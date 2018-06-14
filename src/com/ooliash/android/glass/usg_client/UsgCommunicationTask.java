package com.ooliash.android.glass.usg_client;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    private static TextView textView;
    private static byte[] intBuffer = new byte[4];
    private static byte[] dataBuffer = new byte[BUFFER_SIZE];
    private final AudioManager audioManager;
    private Socket socket;
    private ArrayBlockingQueue<String> commandQueue =
            new ArrayBlockingQueue<String>(COMMAND_QUEUE_CAPACITY);

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
        textView = textViews[0];
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
                    command = "GET_PICTURE";
                }
                // Send pull picture command.
                SendString(outputStream,command);

                // Receive picture.
                Bitmap picture = ReceiveBitmap(inputStream);

                // Show picture.
                publishProgress(picture);
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

    private Bitmap ReceiveBitmap(InputStream inputStream) throws IOException {
        int length = ReceiveByteArray(inputStream);
        logd("Received " + length + " bytes.");
        return BitmapFactory.decodeByteArray(dataBuffer, 0, length);
    }

    private int ReceiveByteArray(InputStream inputStream) throws IOException {
        logd("receiving data length...");
        int length = ReceiveInt(inputStream);

        if (length <= 0 || length > BUFFER_SIZE) {
            Log.e("VSR", "data length (" + length + ") out of bounds.");
            throw new IndexOutOfBoundsException("Length: " + length);
        }

        logd("Receiving " + length + " bytes...");
        int received = 0;
        while (received < length) {
            received += inputStream.read(dataBuffer, received, length - received);
            logd("Received " + received + "/" + length);
        }

        return length;
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
        textView.setText(Integer.toString(progressData[0].getAllocationByteCount()));
        BitmapDrawable drawable = new BitmapDrawable(Resources.getSystem(), progressData[0]);
        textView.setBackground(drawable);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }
}