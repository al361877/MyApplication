package com.e.myapplication;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

public class NetworkExecutor extends Thread {

    private static File mediaStorageDir;
    private static File mediaFile;
    final public int CODE_OK = 200;
    final public int CODE_BADREQUEST = 400;
    final public int CODE_FORBIDDEN = 403;
    final public int CODE_NOTFOUND = 404;
    final public int CODE_INTERNALSERVERERROR = 500;
    final public int CODE_NOTIMPLEMENTED = 501;
    String fileStr;
    Handler handlerNetworkExecutorResult;

    NetworkExecutor(String getSource, Handler handlerNetworkExecutorResult){
        this.fileStr=getSource;
        this.handlerNetworkExecutorResult=handlerNetworkExecutorResult;

    }
    public void run() {
        //creamos el socket cliente
        Socket scliente = null;

        //creamos el servidor socket
        ServerSocket unSocket = null;

        while (true) {
            try {
                unSocket = new ServerSocket(8083); //Creamos el puerto
                scliente = unSocket.accept(); //Aceptando conexiones del navegador Web
//                System.setProperty("line.separator", "\r\n");

                //Creamos los objetos para leer y escribir en el socket
                BufferedReader in = new BufferedReader(new InputStreamReader(scliente.getInputStream()));
                PrintStream out = new PrintStream(new BufferedOutputStream(scliente.getOutputStream()));

                //Leemos el comando que ha sido enviado por el servidor web
                // Ejemplo de comando: GET /index.html HTTP\1.0
                String cadena = in.readLine();
                StringTokenizer st = new StringTokenizer(cadena);
                String commandString = st.nextToken().toUpperCase();
                if (commandString.equals("GET")) {
                    String urlObjectString = st.nextToken();
                    Log.v("urlObjectString", urlObjectString);
                    if (urlObjectString.toUpperCase().startsWith("/INDEX.HTML") || urlObjectString.toUpperCase().equals("/INDEX.HTM") || urlObjectString.equals("/")) {
                        String headerStr = getHTTP_Header(CODE_OK, "text/html", fileStr.length());
                        out.print(headerStr);
                        out.println(fileStr);
                        out.flush();
                    }
                    if (urlObjectString.toUpperCase().startsWith("/FORWARD")) {
                        showDisplayMessage("FORWARD");
                        String headerStr = getHTTP_Header(CODE_OK, "text/html", fileStr.length());
                        out.print(headerStr);
                        out.println(fileStr);
                        out.flush();
                    }
                    if (urlObjectString.toUpperCase().startsWith("/BACKWARD")) {
                        showDisplayMessage("BACKWARD");
                        String headerStr = getHTTP_Header(CODE_OK, "text/html", fileStr.length());
                        out.print(headerStr);
                        out.println(fileStr);
                        out.flush();
                    }
                    if (urlObjectString.toUpperCase().startsWith("/LEFT")) {
                        showDisplayMessage("LEFT");
                        String headerStr = getHTTP_Header(CODE_OK, "text/html", fileStr.length());
                        out.print(headerStr);
                        out.println(fileStr);
                        out.flush();
                    }
                    if (urlObjectString.toUpperCase().startsWith("/RIGHT")) {
                        showDisplayMessage("RIGHT");
                        String headerStr = getHTTP_Header(CODE_OK, "text/html", fileStr.length());
                        out.print(headerStr);
                        out.println(fileStr);
                        out.flush();
                    }
                    if (urlObjectString.toUpperCase().startsWith("/STOP")) {
                        showDisplayMessage("STOP");
                        String headerStr = getHTTP_Header(CODE_OK, "text/html", fileStr.length());
                        out.print(headerStr);
                        out.println(fileStr);
                        out.flush();
                    }
                    if (urlObjectString.toUpperCase().startsWith("/CAMERA.JPG") || urlObjectString.toUpperCase().startsWith("/CAMERA.")) {
                        showDisplayMessage("CAMERA");
                        File cameraFile = getOutputMediaFile();
                        FileInputStream fis = null;
                        boolean exist = true;
                        try {
                            fis = new FileInputStream(cameraFile);
                        } catch (FileNotFoundException e) {
                            exist = false;
                        }
                        if (exist) {
                            String headerStr = getHTTP_Header(CODE_OK,"image/jpeg", (int) cameraFile.length());
                            out.print(headerStr);
                            byte[] buffer = new byte[4096];
                            int n;
                            while ((n = fis.read(buffer)) > 0) { // enviar archivo
                                out.write(buffer, 0, n);
                            }
                            out.flush();
                            out.close();
                        }

                    }
                }

                unSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private String getHTTP_Header(int headerStatusCode, String headerContentType, int headerFileLength) {
        String result = getHTTP_HeaderStatus(headerStatusCode) + "\r\n" +getHTTP_HeaderContentLength(headerFileLength)+ getHTTP_HeaderContentType(headerContentType)+ "\r\n";
        return result;
    }
    //trata los errores
    private String getHTTP_HeaderStatus(int headerStatusCode){
        String result = "";
        switch (headerStatusCode) {
            case CODE_OK:
                result = "200 OK"; break;
            case CODE_BADREQUEST:
                result = "400 Bad Request"; break;
            case CODE_FORBIDDEN:
                result = "403 Forbidden"; break;
            case CODE_NOTFOUND:
                result = "404 Not Found"; break;
            case CODE_INTERNALSERVERERROR:
                result = "500 Internal Server Error"; break;
            case CODE_NOTIMPLEMENTED:
                result = "501 Not Implemented"; break;
        }
        return ("HTTP/1.0 "+result);
    }
    private String getHTTP_HeaderContentLength(int headerFileLength){
        return "Content-Length: " + headerFileLength + "\r\n";
    }
    private String getHTTP_HeaderContentType(String headerContentType){
        return "Content-Type: "+headerContentType+"\r\n";
    }

    public void showDisplayMessage(String displayMessage) {
        Message msg = new Message();
        msg.arg1 = 0;
        msg.obj = displayMessage.replaceAll("_", " ");
        handlerNetworkExecutorResult.sendMessage(msg);
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
}