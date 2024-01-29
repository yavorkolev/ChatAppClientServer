package com.example.chatapp;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
//    Readme:
//    ChatAppClientServer Java Android Studio Android Application example/project
//    First started application is Server the next ones are clients it's completed. Must set the IP address of the network of the server, at the moment is hard coded into the coded
    static TextView tvReceivedMsg;
    Button btnSendMsg, btnSelectFile, btnSendFile;
    EditText etEditMsg;
    ServerClientService serverClientService;
    boolean isBound = false;
    private Uri fileUri;
    static DataInputStream dataInputStream;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSendMsg = findViewById(R.id.btnSendMsg);
        tvReceivedMsg = findViewById(R.id.tvReceivedMsg);
        etEditMsg = findViewById(R.id.etEditMsg);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSendFile = findViewById(R.id.btnSendFile);
        setStoragePermissions();
        try {
            Log.i("MyTag", "IP: " + getLocalIpAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(this, ServerClientService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void setStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            btnSelectFile.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(getApplicationContext(), "Storage Permissions are Denied", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendMessage(View view) {
        serverClientService.sendMessage(etEditMsg.getText().toString());

    }

    public void sendFile(View view) {
        btnSendFile.setVisibility(View.GONE);
        btnSelectFile.setVisibility(View.VISIBLE);
        serverClientService.sendFile(fileUri);
    }

    public void selectFile(View view) {
        Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        chooseFile = Intent.createChooser(chooseFile, "Choose a file");
        someActivityResultLauncher.launch(chooseFile);
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            btnSelectFile.setVisibility(View.GONE);
                            btnSendFile.setVisibility(View.VISIBLE);
                            fileUri = data.getData();
                            Toast.makeText(getApplicationContext(), "You select a file", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            ServerClientService.LocalBinder binder = (ServerClientService.LocalBinder) service;
            serverClientService = binder.getService();
            isBound = true;
            serverClientService.startClientServer();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };

    public static void updateUi(DataInputStream dataInputStream) {
        int bytes = 0;
        File output = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + "newFile.txt");
        long size = 0;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(output);
            size = dataInputStream.readLong();
            byte[] buffer = new byte[4 * 1024];
            while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                fileOutputStream.write(buffer, 0, bytes);
                size -= bytes;
            }
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //tvReceivedMsg.setText(inputStreamToString(inputStream));
    }

    private static String inputStreamToString(DataInputStream inputStream) {
        int length = 0;
        byte[] data;
        String str;
        try {
            length = inputStream.readInt();
            data = new byte[length];
            inputStream.readFully(data);
            str = new String(data, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return str;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager =
                (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(
                ByteBuffer.allocate(4).order(
                        ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    public static Thread receiveThread() {
        while (true) {
            try {
                if (dataInputStream.available() > 0) {
                    int length= 0;
                    byte[] dataKey;
                    byte[] dataValue;
                    String strKey , strValue = null;
                    try {
                        length = dataInputStream.readInt();
                        Log.i("MyTag", "length:"+length);
                        dataKey=new byte[4];
                        dataInputStream.readFully(dataKey);
                        for (byte by : dataKey) {
                            // Print the character
                            System.out.print((char)by);
                            Log.i("MyTag", "(char)by:"+(char)by);
                        }
                        //strKey=new String(dataKey,"UTF-8");
                        //Log.i("MyTag", "strKey: "+strKey);
//                        if("Mesg".equals(strKey)){
//                            Log.i("MyTag", "Mesg-str: "+strKey);
//                            dataValue=new byte[length-4];
//                            dataInputStream.readFully(dataValue);
//                            strValue=new String(dataValue,"UTF-8");
//                            Log.i("MyTag", "Mesg-str: "+strValue);
//                            tvReceivedMsg.setText(strValue);
//                        } else if("File".equals(strKey)){
//                            Log.i("MyTag", "File-str: "+strKey);
//                            dataValue=new byte[length-4];
//                            dataInputStream.readFully(dataValue);
//                            strValue=new String(dataValue,"UTF-8");
//                            Log.i("MyTag", "File-str: "+strValue);
//                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}