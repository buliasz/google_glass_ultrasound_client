package com.ooliash.android.glass.usg_client;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.glass.media.Sounds;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.util.concurrent.ArrayBlockingQueue;

class UsgCommunicationTask extends AsyncTask<Void, String, Void> {
    // Constants.
    private static final String LOG_TAG = "USG";
    private static final int COMMAND_QUEUE_CAPACITY = 10;
    private static final String SET_MAIN_TEXT = "SET_MAIN_TEXT";
    private static final String ERROR_MESSAGE = "ERROR_MESSAGE";


    // Local commands.
//    private static final String COMMAND_NETWORK_STATUS = "NETWORK_STATUS";

    // Other fields.
    private final WindowsSocketCommunication communication;
    private final WeakReference<UsgSessionActivity> contextWR;

    ArrayBlockingQueue<String> commandQueue =
            new ArrayBlockingQueue<String>(COMMAND_QUEUE_CAPACITY);

    private Bitmap usgPicture;
    private boolean isConnected;
//    private String networkIndicatorText;

    UsgCommunicationTask(UsgSessionActivity context) {
        contextWR = new WeakReference<UsgSessionActivity>(context);
        communication = new WindowsSocketCommunication();
    }

    boolean isConnected() {
        return isConnected;
    }

    byte[] getLastUsgPictureBytes() {
        return communication.getLastPictureBytes();
    }

    /**
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params No params.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected Void doInBackground(Void... params) {
        AudioManager audioManager = contextWR.get().audioManager;
        while (!isCancelled()) {
            try {
                communication.connectToUsgServer();
                isConnected = true;
                publishProgress(SET_MAIN_TEXT, ""); // clear command from main text

                Log.d(LOG_TAG, "connected...");
                commandQueue.add(Command.GET_PICTURE);
                commandQueue.add(Command.GET_GAIN);
                commandQueue.add(Command.GET_AREA);

                while (communication.isConnected() && !isCancelled()) {
                    String command = commandQueue.poll();
                    if (command == null) {
                        command = Command.GET_PICTURE;  // Send pull picture command if queue is empty.
                    }
                    Log.d(LOG_TAG, "Sending " + command + " command");
                    communication.SendString(command);

                    try {
                        if (command == Command.GET_PICTURE) {
                            // Receive picture.
                            usgPicture = communication.ReceiveBitmap();
                            // Show picture.
                            publishProgress(command);
                        } else {
                            publishProgress(command, communication.ReceiveString());
                        }
                    } catch (SocketTimeoutException e) {
                        Log.e(LOG_TAG, "Couldn't receive response for '" + command
                                + "' command. Restarting connection.");
                        communication.connectToUsgServer();
                        commandQueue.add(command);
                    } catch (UsgCommandExecutionException e) {
                        audioManager.playSoundEffect(Sounds.ERROR);
                        ErrorMessage(e.getMessage());
                    }
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
                e.printStackTrace();
            } finally {
                communication.disconnectFromUsgServer();
            }
        }
        return null;
    }

    private void ErrorMessage(String message) {
        Log.e(LOG_TAG, message);
        publishProgress(ERROR_MESSAGE, message); // clear command from main text
    }

    @Override
    protected void onProgressUpdate(String... progressData) {
        if (progressData.length <= 0) {
            Log.e(LOG_TAG, "Insufficient parameters for onProgressUpdate()");
            return;
        }

        UsgSessionActivity context = contextWR.get();
        String command = progressData[0];
        if (command == Command.GET_PICTURE) {
            BitmapDrawable drawable = new BitmapDrawable(Resources.getSystem(), usgPicture);
            context.textView.setBackground(drawable);
        } else if (command == SET_MAIN_TEXT) {
            context.textView.setText(progressData[1]);
        } else if (command == ERROR_MESSAGE) {
            context.errorMessage(progressData[1]);
        } else if (command == Command.GAIN_UP || command == Command.GAIN_DOWN || command == Command.GET_GAIN) {
            context.gainTextView.setText("\u2195" + progressData[1]);
            context.textView.setText(""); // clear command from main text
        } else if (command == Command.AREA_UP || command == Command.AREA_DOWN || command == Command.GET_AREA) {
            context.areaTextView.setText("\u2194" + progressData[1]);
            context.textView.setText(""); // clear command from main text
        } else if (progressData.length > 1){
            context.normalMessage(progressData[1]); // clear command from main text
        } else {
            Log.e(LOG_TAG, "Unknown command for onProgressUpdate(): " + command);
        }
    }
}
