package com.ooliash.android.glass.usg_client;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

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

    // Available USG server commands.
    private static final String COMMAND_GET_PICTURE = "GET_PICTURE";
    private static final String COMMAND_GET_GAIN = "GET_GAIN";
    private static final String COMMAND_GET_AREA = "GET_IMAGING_RANGE";
    private static final String COMMAND_GET_TX_FREQUENCY = "GET_TX_FREQUENCY";
    private static final String COMMAND_GET_TX_TYPE = "GET_TX_TYPE";
    private static final String COMMAND_GET_FPS = "GET_FPS";
    private static final String COMMAND_FREEZE = "FREEZE";
    private static final String COMMAND_GAIN_UP = "GAIN_UP";
    private static final String COMMAND_GAIN_DOWN = "GAIN_DOWN";
    private static final String COMMAND_AREA_UP = "AREA_UP";
    private static final String COMMAND_AREA_DOWN = "AREA_DOWN";
    private static final String COMMAND_HIDE = "HIDE";
    private static final String COMMAND_SAVE = "SAVE";
    private static final String COMMAND_8BIT_GRAYSCALE = "8BIT_GRAYSCALE";
    
    // Local commands.
    private static final String COMMAND_NETWORK_STATUS = "NETWORK_STATUS";

    // Other fields.
    private final WindowsSocketCommunication communication;
    private final WeakReference<UsgMainActivity> contextWR;

    private ArrayBlockingQueue<String> commandQueue =
            new ArrayBlockingQueue<String>(COMMAND_QUEUE_CAPACITY);

    private Bitmap usgPicture;
    private String networkIndicatorText;

    UsgCommunicationTask(UsgMainActivity context) {
        contextWR = new WeakReference<UsgMainActivity>(context);
        communication = new WindowsSocketCommunication();
    }

    void SendCommand(String command) {
        try {
            commandQueue.put(command);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Cannot put new command to the command queue: "
                    + e.getMessage());
        }
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
                publishProgress(SET_MAIN_TEXT, ""); // clear command from main text
                Log.d(LOG_TAG, "connected...");
                commandQueue.add(COMMAND_GET_GAIN);
                commandQueue.add(COMMAND_GET_AREA);

                audioManager.playSoundEffect(Sounds.SUCCESS);
                while (communication.isConnected() && !isCancelled()) {
                    String command = commandQueue.poll();
                    if (command == null) {
                        command = COMMAND_GET_PICTURE;  // Send pull picture command if queue is empty.
                    } else {
                    }
                    Log.d(LOG_TAG, "Sending " + command + " command");
                    communication.SendString(command);

                    try {
                        if (command == COMMAND_GET_PICTURE) {
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

    private void networkIndicateDataPush() {
        publishProgress(COMMAND_NETWORK_STATUS, "\u25b2"); // black up-pointing small triangle
    }

    private void networkIndicateDataPop() {
        publishProgress(COMMAND_NETWORK_STATUS, "\u25bc"); // black down-pointing small triangle
    }

    private void networkIndicateNoDataTransfer() {
        publishProgress(COMMAND_NETWORK_STATUS, " ");
    }

    @Override
    protected void onProgressUpdate(String... progressData) {
        if (progressData.length <= 0) {
            Log.e(LOG_TAG, "Insufficient parameters for onProgressUpdate()");
            return;
        }

        UsgMainActivity context = contextWR.get();
        TextView currentTextView = context.getCurrentTextView();
        String command = progressData[0];
        if (command == COMMAND_GET_PICTURE) {
            BitmapDrawable drawable = new BitmapDrawable(Resources.getSystem(), usgPicture);
            currentTextView.setBackground(drawable);
        } else if (command == SET_MAIN_TEXT) {
            currentTextView.setText(progressData[1]);
        } else if (command == ERROR_MESSAGE) {
            context.errorMessage(progressData[1]);
        } else if (command == COMMAND_NETWORK_STATUS) {
            context.networkIndicatorTextView.setText(progressData[1]);
        } else if (command == COMMAND_GAIN_UP || command == COMMAND_GAIN_DOWN || command == COMMAND_GET_GAIN) {
            context.gainTextView.setText("\u2195" + progressData[1]);
            currentTextView.setText(""); // clear command from main text
        } else if (command == COMMAND_AREA_UP || command == COMMAND_AREA_DOWN || command == COMMAND_GET_AREA) {
            context.areaTextView.setText("\u2194" + progressData[1]);
            currentTextView.setText(""); // clear command from main text
        } else {
            Log.e(LOG_TAG, "Unknown command for onProgressUpdate(): " + command);
        }
    }
}
