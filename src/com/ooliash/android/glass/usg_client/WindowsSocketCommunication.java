package com.ooliash.android.glass.usg_client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.StrictMode;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class WindowsSocketCommunication {
    // Constants.
    private static final String LOG_TAG = "USG";
    private static final int PORT_NUMBER = 9050;
    private static final int BROADCAST_PORT_NUMBER = 9049;
    private static final int SOCKET_TIMEOUT = 3000;
    private static final int BUFFER_SIZE = 128*1024;

    // Data transfer variables.
    private static byte[] intBuffer = new byte[4];
    private static byte[] dataBuffer = new byte[BUFFER_SIZE];
    private Socket socket;

    // Other variables.
    private InetAddress serverAddress;   // "192.168.1.100"
    private InputStream inputStream;
    private OutputStream outputStream;

    /**
     * Connects to USG server. Restarts connection if it's already connected.
     * @throws IOException
     */
    void connectToUsgServer() {
        if (isConnected()) {
            Log.e(LOG_TAG, "I'm already connected to USG. Disconnecting...");
            disconnectFromUsgServer();
        }

        boolean done = false;
        do {
            findUsgServerAddress();

            Log.d(LOG_TAG, "creating Socket");
            try {
                socket = new Socket();
                socket.setSoTimeout(SOCKET_TIMEOUT);
                socket.connect(new InetSocketAddress(serverAddress, PORT_NUMBER), SOCKET_TIMEOUT);

                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                done = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (!done);
    }

    /**
     * Checks socket connection status.
     * @return True if socket is connected, false otherwise.
     */
    boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    /**
     * Shutdowns {@link #inputStream} and {@link #outputStream} and closes {@link #socket}.
     * Nullifies all of them.
     */
    void disconnectFromUsgServer() {
        inputStream = null;
        outputStream = null;
        try {
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
            socket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends String through the {@link #outputStream}.
     * @param text String to send.
     * @throws IOException
     */
    void SendString(String text) throws IOException {
        byte[] bytesToSend = text.getBytes();
//        networkIndicateDataPush();
        outputStream.write(IntToByteArray(bytesToSend.length));
        outputStream.write(bytesToSend);
//        networkIndicateNoDataTransfer();
    }

    /**
     * Receives String from the {@link #inputStream}.
     * @return The string received.
     * @throws IOException
     */
    String ReceiveString() throws IOException {
        int length = ReceiveByteArray();
        return new String(dataBuffer, 0, length);
    }

    /**
     * Receives Bitmap from the {@link #inputStream}.
     * @return The Bitmap received.
     * @throws IOException
     */
    Bitmap ReceiveBitmap() throws IOException {
        int length = ReceiveByteArray();
//        Log.d(LOG_TAG, "Received " + length + " bytes.");
        return BitmapFactory.decodeByteArray(dataBuffer, 0, length);
    }

    /*
    ============== PRIVATE METHODS ================
     */

    /**
     * Broadcasts "Looking for USG server".
     */
    private void sendBroadcast() {
        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            //Open a random port to send the package
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(SOCKET_TIMEOUT);
            byte[] sendData = "LF_PJATK_USG_SERVER".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                    sendData,
                    sendData.length,
                    InetAddress.getByName("255.255.255.255"),
                    BROADCAST_PORT_NUMBER);

            String messageStr = null;
            byte[] recvBuf = new byte[100];
            DatagramPacket receivedPacket = new DatagramPacket(recvBuf, recvBuf.length);
            do {
                socket.send(sendPacket);
                Log.d(LOG_TAG, "Broadcast packet sent to: 255.255.255.255");

                //Wait for a response
                try {
                    socket.receive(receivedPacket);
                } catch (InterruptedIOException ex) {
                    Log.d(LOG_TAG, "No broadcast response...");
                    continue;
                }
                messageStr = new String(receivedPacket.getData()).trim();
                Log.d(LOG_TAG,
                        "Received response from " + receivedPacket.getAddress().getHostAddress()
                                + ": " + messageStr);
            } while (messageStr == null || !messageStr.equals("PJATK_USG_SERVER_ACK"));

            serverAddress = receivedPacket.getAddress();
            socket.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException: " + e.getMessage());
        }
    }

//    InetAddress getBroadcastAddress() throws IOException {
//        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
//        DhcpInfo dhcp = wifi.getDhcpInfo();
//        // handle null somehow
//
//        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
//        byte[] quads = new byte[4];
//        for (int k = 0; k < 4; k++)
//            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
//        return InetAddress.getByAddress(quads);
//    }

    /**
     * Finds USG server using UDP broadcast "looking for USG server".
     */
    private void findUsgServerAddress() {
        serverAddress = null;
        while (serverAddress == null) {
            Log.d(LOG_TAG, "Broadcast looking for USG server...");
            sendBroadcast();
        }
    }

    /**
     * Receives byte array from the {@link #inputStream}.
     * @return The byte array received.
     * @throws IOException
     */
    private int ReceiveByteArray() throws IOException {
//        networkIndicateDataPop();
//        logd("receiving data length...");
        int length = ReceiveInt();

        if (length == 0) {
            throw new UsgCommandExecutionException(ReceiveString());
        }

        if (length < 0 || length > BUFFER_SIZE) {
            Log.e(LOG_TAG, "data length (" + length + ") out of bounds.");
            throw new IndexOutOfBoundsException("Length: " + length);
        }

//        Log.d(LOG_TAG, "Receiving " + length + " bytes...");
        int received = 0;
        while (received < length) {
            received += inputStream.read(dataBuffer, received, length - received);
//            logd("Received " + received + "/" + length);
        }
//        networkIndicateNoDataTransfer();

        return length;
    }

    /**
     * Receives integer from {@link #inputStream}.
     * @return The integer received.
     * @throws IOException
     */
    private int ReceiveInt() throws IOException {
//        networkIndicateDataPop();
        int count = inputStream.read(intBuffer, 0, 4);
        if (count != 4) {
            throw new IOException("Couldn't read 4 bytes [read " + count + "]");
        }
//        networkIndicateNoDataTransfer();
        return (((intBuffer[3] & 0xFF) << 24)
                | ((intBuffer[2] & 0xFF) << 16)
                | ((intBuffer[1] & 0xFF) << 8)
                | (intBuffer[0] & 0xFF));
    }

    /**
     * Converts integer to byte array so it can be sent through socket to .NET client.
     * @param inputInt Integer to convert.
     * @return Byte array containing integer.
     */
    private byte[] IntToByteArray(int inputInt) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte)(inputInt & 0xFF);
        byteArray[1] = (byte)((inputInt >> 8) & 0xFF);
        byteArray[2] = (byte)((inputInt >> 16) & 0xFF);
        byteArray[3] = (byte)((inputInt >> 24) & 0xFF);
        return byteArray;
    }
}
