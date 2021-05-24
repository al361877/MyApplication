package com.e.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.*;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.util.ArrayList;

import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private static File mediaStorageDir;
    private static File mediaFile;
    //User Interface Variables
    Button stopButton;
    Button connectButton;
    Button diconnectButton;
    Button forwardButton;
    Button backwardButton;
    Button turnLeftForwardButton;
    Button turnRightForwardButton;
    Button listaButton;
    TextView texto;
    BluetoothAdapter bluetooth;
    public static Boolean bluetoothActive = false;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    BroadcastReceiver discoveryResult;
    BluetoothSocket btScocket;
    OutputStream outputStream;
    InputStream inputStream;
    Button directButton;
    CameraPreview mCameraPreview;
    Camera mCamera;
    String readResource;

    int velocidad=1;
    FrameLayout cameraPreviewFrameLayout;
    Camera.PictureCallback mPicture ;
    private Handler handlerNetworkExecutorResult;
    private NetworkExecutor networkExecutor;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetooth=BluetoothAdapter.getDefaultAdapter();
        stopButton = (Button) findViewById(R.id.stop);
        connectButton = (Button) findViewById(R.id.connect);
        diconnectButton = (Button) findViewById(R.id.disconnect);
        forwardButton = (Button) findViewById(R.id.forward);
        backwardButton= (Button) findViewById(R.id.back);
        turnLeftForwardButton= (Button) findViewById(R.id.left);
        turnRightForwardButton= (Button) findViewById(R.id.right);
        directButton= (Button) findViewById (R.id.direct);
        texto = (TextView) findViewById(R.id.texto);
        mCamera = getCameraInstance();
        mCameraPreview = new CameraPreview(this, mCamera);
        cameraPreviewFrameLayout = (FrameLayout) findViewById(R.id.cameraPreviewFrameLayout);
        cameraPreviewFrameLayout.addView(mCameraPreview);
        checkBTPermissions();
        discoveryResult = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Guardamos el nombre del dispositivo descubierto
                String remoteDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                //Guardamos el objeto Java del dispositivo descubierto, para poderconectar
                BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Leemos la intensidad de la radio con respecto a este dispositivobluetooth
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                //Guardamos el dispositivo encontrado en la lista
                deviceList.add(remoteDevice);
                //Mostramos el evento en el Log.
                Log.d("MyFirstApp", "Discovered " + remoteDeviceName);
                Log.d("MyFirstApp", "RSSI " + rssi + "dBm");


            }
        };
        mPicture = new android.hardware.Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
                        byte[] resized = resizeImage(data);
                        File pictureFile = getOutputMediaFile();
                        if (pictureFile == null) {
                            return;
                        }
                        try {
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(resized);
                            fos.close();
                        } catch (Exception e) {
                            Log.e("onPictureTaken", "ERROR:" + e);
                        }
                    }
                };
        handlerNetworkExecutorResult = new Handler() {
            @SuppressLint("LongLogTag")
            @Override
            public void handleMessage(Message msg) {
                Log.d("handlerNetworkExecutorRes", (String) msg.obj);
                if (msg != null) {
                    if (msg.obj.equals("FORWARD")) {
                        forward();
                    } else if (msg.obj.equals("BACKWARD")) {
                        back();
                    } else if (msg.obj.equals("LEFT")) {
                        left();
                    } else if (msg.obj.equals("RIGHT")) {
                        right();
                    } else if (msg.obj.equals("CAMERA")) {
                        captureCamera();
                    }
                }
            }
        };
        readResource=readResourceTextFile();
        networkExecutor = new NetworkExecutor(readResource,handlerNetworkExecutorResult);
        networkExecutor.start();

    }

    public void onClickConexionDirecta(View view){
//        BluetoothDevice device=bluetooth.getRemoteDevice("BC:2D:EF:45:4A:D8");
        BluetoothDevice device=bluetooth.getRemoteDevice("08:3A:F2:A9:36:CE");
        connect(device);
    }

    public void onClickConnectButton(View view){
        texto.setText("Connect pressed");
        //comprueba si el usuario tiene activado desde la seccion de preferecias del dispositivo.
        //en caso positivo, consultamos, a modo de ejemplo, la direccion MAC del adaptador bluetooth y el nombre, mostrandolo por pantalla con el objeto toast.
        if (bluetooth.isEnabled()){
            bluetoothActive=true;
            String address=bluetooth.getAddress();
            String name = bluetooth.getName();
            //Mostramos los datos en la pantalla
            Toast.makeText(getApplicationContext(),"Bluetooth ENABLE: "+name+""+address,Toast.LENGTH_SHORT).show();
            startDiscovery();
            Toast.makeText(getApplicationContext(),"CONETADISIMO",Toast.LENGTH_SHORT).show();
        }else{
            bluetoothActive=false;
            Toast.makeText(getApplicationContext(),"Bluetooth NOT enable",Toast.LENGTH_SHORT).show();
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
        }

    }
    public void onClickDisButton(View view){
        texto.setText("Dis pressed");
        bluetooth.disable();
        Toast.makeText(getApplicationContext(),"Bluetooth disable",Toast.LENGTH_SHORT).show();
    }
//se conecta al bluetooth
    protected void connect(BluetoothDevice device){
        try{
            btScocket=device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            btScocket.connect();
            Log.d("connect","Client connected");
            inputStream=btScocket.getInputStream();
            outputStream=btScocket.getOutputStream();

        }catch (Exception e) {
            Log.e("Error: conncet",">>",e);
        }
    }

        public void onClickListaButton(View view){

            if (bluetooth.isEnabled()){
                bluetoothActive=true;
                String lista="";
                for(BluetoothDevice dispositivo: deviceList){
                    lista=lista+"\n"+dispositivo.getName();
                }
                Toast.makeText(getApplicationContext(),"La lista es:"+lista+"",Toast.LENGTH_SHORT).show();
            }else{
                bluetoothActive=false;
                Toast.makeText(getApplicationContext(),"Bluetooth NOT enable",Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);
            }




    }

    public void onClickRightButton(View view){
        try{
            String tmpStr=right();
            byte bytes[]=tmpStr.getBytes();
            if (outputStream != null)outputStream.write(bytes);
            if(outputStream != null) outputStream.flush();
            Log.d("forward","Envio el string correctamente");
        }catch(Exception e){
            Log.e("forward","ERROR"+e);
        }

    }
    String forward(){
        return "w"+velocidad++; //primero te da el valor y luego lo suma
    }
    String left(){
        return "a5";
    }
    String stop(){
        return "0";
    }
    String back(){
        return "s"+velocidad++; //primero te da el valor y luego lo suma
    }
    String right(){
        return "d5";
    }


    public void onClickForwardButton(View view){
        texto.setText("Forward pressed");
        try{
            String tmpStr=forward();
            byte bytes[]=tmpStr.getBytes();
            if (outputStream != null)outputStream.write(bytes);
            if(outputStream != null) outputStream.flush();
        }catch(Exception e){
            Log.e("forward","ERROR"+e);
        }

    }

    public void onClickLeftButton(View view){
        try{
            String tmpStr=left();
            byte bytes[]=tmpStr.getBytes();
            if (outputStream != null)outputStream.write(bytes);
            if(outputStream != null) outputStream.flush();
        }catch(Exception e){
            Log.e("forward","ERROR"+e);
        }

    }

    public void onClickStopButton(View view){
        try{
            String tmpStr=stop();
            byte bytes[]=tmpStr.getBytes();
            if (outputStream != null)outputStream.write(bytes);
            if(outputStream != null) outputStream.flush();
        }catch(Exception e){
            Log.e("forward","ERROR"+e);
        }

    }

    public void onClickBackButton(View view){
        texto.setText("Forward pressed");
        try{
            String tmpStr=back();
            velocidad++;
            byte bytes[]=tmpStr.getBytes();
            if (outputStream != null)outputStream.write(bytes);
            if(outputStream != null) outputStream.flush();
        }catch(Exception e){
            Log.e("forward","ERROR"+e);
        }
    }


    //Este metodo sirve para conocer la opcion elegida por el user.
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) //Bluetooth permission request code
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "User Enabled Bluetooth", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "User Did not enable Bluetooth", Toast.LENGTH_SHORT).show();
            }
    }
    private void startDiscovery(){
        checkBTPermissions();
        if (bluetoothActive){
            //Borramos la lista de dispositivos anterior
            deviceList.clear();
            //Activamos un Intent Android que avise cuando se encuentre un dispositivo
            //NOTA: <<discoveryResult>> es una clase <<callback>> que describiremos enel siguiente paso
            registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            //Ponemos el adaptador bluetooth en modo <<Discovery>>
            bluetooth.startDiscovery();
        }
    }



    public void checkBTPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    if (ContextCompat.checkSelfPermission(getBaseContext(),Manifest.permission.ACCESS_COARSE_LOCATION) !=PackageManager.PERMISSION_GRANTED) {
                        this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
                    }
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    break;
            }
            switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CAMERA)) {
                case PackageManager.PERMISSION_DENIED:
                    if (ContextCompat.checkSelfPermission(getBaseContext(),Manifest.permission.CAMERA) !=PackageManager.PERMISSION_GRANTED) {
                        this.requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.CAMERA}, 100);
                    }
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    break;
            }
        }

    }

    private android.hardware.Camera getCameraInstance() {
        android.hardware.Camera camera = null;
        try {
            camera = android.hardware.Camera.open(0);
        } catch (Exception e) {
            // cannot get camera or does not exist
            Log.d("getCameraInstance", "ERROR" + e);
        }
        return camera;
    }


    public void captureCamera(){
        if (mCamera!=null) {
            mCamera.takePicture(null, null, mPicture);
        }
    }
    byte[] resizeImage(byte[] input) {
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(input, 0, input.length);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 80, 107,
                true);
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, blob);
        return blob.toByteArray();
    }
    private static File getOutputMediaFile() {
        if (mediaStorageDir == null){
            mediaStorageDir = new
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "MyCameraApp");
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory");
                    return null;
                }
            }
        }
        if (mediaFile==null) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG.jpg");
        }
        return mediaFile;
    }
    public String readResourceTextFile() {
        String fileStr = "";
            InputStream is = getResources().openRawResource(R.raw.index);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String readLine = null;
        try {
            while ((readLine = br.readLine()) != null) {
                fileStr = fileStr + readLine + "\r\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileStr;
    }

}



