package com.ooliash.android.glass.usg_client;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.google.android.glass.media.Sounds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

public class UsgCommunicationTask extends AsyncTask<TextView, Bitmap, Void> {
    private static final int PORT_NUMBER = 9050;
    private static final int COMMAND_QUEUE_CAPACITY = 10;
    private static final int BUFFER_SIZE = 128*1024;
    private static final String LOG_TAG = "USG";

    private static TextView publishingTextView;
    private static byte[] intBuffer = new byte[4];
    private static byte[] dataBuffer = new byte[BUFFER_SIZE];
    private final AudioManager audioManager;
    private Socket socket;
    private ArrayBlockingQueue<String> commandQueue =
            new ArrayBlockingQueue<String>(COMMAND_QUEUE_CAPACITY);
    private int transferred = 0;
    private TextView networkIndicatorTextView;
    private int networkIndicatorColor = Color.WHITE;
    private String publishString = null;

    public UsgCommunicationTask(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    private void logd(String text) {
        Log.d(LOG_TAG, text);
    }

    public void SendCommand(String command) {
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
     * @param textViews The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected Void doInBackground(TextView... textViews) {
        publishingTextView = textViews[0];
        networkIndicatorTextView = textViews[1];
        try {
            logd("creating Socket");
            socket = new Socket("192.168.1.100", PORT_NUMBER);
            audioManager.playSoundEffect(Sounds.SUCCESS);
            logd("connected...");

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            while (socket.isConnected() && !isCancelled()) {
                String command = commandQueue.poll();
                if (command == null) {
                    command = "GET_PICTURE";    // Send pull picture command if queue is empty.
                }
//                Available commands:
//                "GET_PICTURE"
//                "FREEZE"
//                "GAIN_UP"
//                "GAIN_DOWN"
//                "AREA_UP"
//                "AREA_DOWN"
//                "HIDE"
//                "SAVE"
//                "8BIT_GRAYSCALE"
//                "GET_GAIN"
//                "GET_TX_FREQUENCY"
//                "GET_TX_TYPE"
//                "GET_IMAGING_RANGE"
//                "GET_FPS"

                logd("Sending " + command + " command");
                SendString(outputStream,command);

                if (command == "GET_PICTURE") {
                    // Receive picture.
                    Bitmap picture = ReceiveBitmap(inputStream);

                    // Show picture.
                    publishProgress(picture);
                }
                if (command == "GAIN_UP" || command == "GAIN_DOWN"
                        || command == "AREA_UP" || command == "AREA_DOWN") {
                    publishString = ReceiveString(inputStream);
                    publishProgress();
                }
            }

            socket.close();
        } catch (IOException e) {
            logd(e.getMessage());
        }

        return null;
    }

    private void SendString(OutputStream outputStream, String text) throws IOException {
        byte[] bytesToSend = text.getBytes();
        outputStream.write(IntToByteArray(bytesToSend.length));
        outputStream.write(bytesToSend);
    }

    private String ReceiveString(InputStream inputStream) throws IOException {
        int length = ReceiveByteArray(inputStream);
        return new String(dataBuffer, 0, length);
    }

    private Bitmap ReceiveBitmap(InputStream inputStream) throws IOException {
        int length = ReceiveByteArray(inputStream);
        logd("Received " + length + " bytes.");
        return BitmapFactory.decodeByteArray(dataBuffer, 0, length);
    }

    private int ReceiveByteArray(InputStream inputStream) throws IOException {
        networkIndicateStarted();
//        logd("receiving data length...");
        int length = ReceiveInt(inputStream);

        if (length <= 0 || length > BUFFER_SIZE) {
            Log.e("VSR", "data length (" + length + ") out of bounds.");
            throw new IndexOutOfBoundsException("Length: " + length);
        }

        logd("Receiving " + length + " bytes...");
        int received = 0;
        while (received < length) {
            received += inputStream.read(dataBuffer, received, length - received);
//            logd("Received " + received + "/" + length);
        }
        networkIndicateStopped();

        return length;
    }

    private void networkIndicateStarted() {
        networkIndicatorColor = Color.RED;
        publishProgress();
    }

    private void networkIndicateStopped() {
        networkIndicatorColor = Color.BLACK;
        publishProgress();
    }

    private int ReceiveInt(InputStream inputStream) throws IOException {
        inputStream.read(intBuffer, 0, 4);
        return (((intBuffer[3] & 0xFF) << 24)
                | ((intBuffer[2] & 0xFF) << 16)
                | ((intBuffer[1] & 0xFF) << 8)
                | (intBuffer[0] & 0xFF));
    }

    private byte[] IntToByteArray(int inputInt) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte)(inputInt & 0xFF);
        byteArray[1] = (byte)((inputInt >> 8) & 0xFF);
        byteArray[2] = (byte)((inputInt >> 16) & 0xFF);
        byteArray[3] = (byte)((inputInt >> 24) & 0xFF);
        return byteArray;
    }

    @Override
    protected void onProgressUpdate(Bitmap... progressData) {
        if (progressData.length == 0) {
            if (publishString != null) {
                publishingTextView.setText(publishString);
                publishString = null;
            }
            networkIndicatorTextView.setTextColor(networkIndicatorColor);
            return;
        }
        transferred += progressData[0].getAllocationByteCount();
//        publishingTextView.setText(Integer.toString(transferred));
        BitmapDrawable drawable = new BitmapDrawable(Resources.getSystem(), progressData[0]);
        publishingTextView.setBackground(drawable);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }
}
