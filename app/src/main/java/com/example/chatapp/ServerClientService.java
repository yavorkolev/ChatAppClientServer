package com.example.chatapp;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerClientService extends Service {
    static Socket socket;
    static final int PORT = 3333;
    DataOutputStream dataOutputStream;

    private final IBinder iBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        ServerClientService getService() {
            return ServerClientService.this;
        }
    }

    public ServerClientService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public void startClientServer() {
        new Thread(() -> {
            try {
                // Ip-xx.xx.xx.xx of the network
                socket = new Socket("xx.xx.xx.xx", PORT);
                MainActivity.dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                MainActivity.receiveThread();
            } catch (IOException e) {
                new Thread(() -> {
                    try {
                        ServerSocket serverSocket = new ServerSocket(PORT);
                        socket = serverSocket.accept();
                        MainActivity.dataInputStream = new DataInputStream(socket.getInputStream());
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        MainActivity.receiveThread();
                    } catch (IOException e1) {
                    }
                }).start();
            }
        }).start();
    }

    public void sendMessage(String message) {
        new Thread(() -> {
            try {
                byte[] keyBytes= ("Mesg").getBytes("UTF-8");
                Log.i("MyTag", "keyBytes.length: "+keyBytes.length);
                for(int i=0;i<keyBytes.length;i++){
                    Log.i("MyTag", "keyBytes[i]: "+keyBytes[i]);
                }
                dataOutputStream.write(keyBytes);
//                //--------------------------------
//                byte[] messageBytes= (message).getBytes();
//                Log.i("MyTag", "messageBytes.length: "+messageBytes.length);
//                for(int i=0;i<messageBytes.length;i++){
//                    Log.i("MyTag", "messageBytes[i]: "+messageBytes[i]);
//                }
//                dataOutputStream.writeInt(messageBytes.length);
//                dataOutputStream.write(messageBytes);
//                Log.i("MyTag", "dataOutputStream.size(): "+dataOutputStream.size());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void sendFile(Uri fileUri) {
        Cursor returnCursor = getApplicationContext().getContentResolver().query(fileUri, new String[]{
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
        }, null, null, null);
        returnCursor.moveToFirst();
        try {
            InputStream inputStream = getApplicationContext().getContentResolver().openInputStream(fileUri);
            Log.i("MyTag", "inputStream: "+inputStream.toString());
            Log.i("MyTag", "inputStream.available(): "+inputStream.available());
            File file = new File(String.valueOf(fileUri));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new Thread(() -> {
            try {
                byte[] messageBytes= ("File"+fileUri).getBytes("UTF-8");
                dataOutputStream.writeInt(messageBytes.length);
                dataOutputStream.write(messageBytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }


}
