package sockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class VizSocket {
    private Socket socket;
    private String ip;
    private String vizVersion;

    public VizSocket(String ip){
        this.ip = ip;
    }

    public boolean connect(){
        try {
            socket = new Socket(ip, 6100);
        } catch (IOException e) {
            socket = null;
            vizVersion = null;
            return false;
        }
        return true;
    }

    public void changeCamera(int cameraId){
        sendMessage("send RENDERER*CAMERA" + cameraId + "*EXTERNAL SET 1");
        sendMessage("send EDITOR*5 SET_CAMERA " + cameraId);
    }

    public void close(){
        try{
            socket.close();
        }catch (IOException e){
            System.err.println("Cannot close.");
        }
    }

    private void sendMessage(String data){
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write((data + " \0").getBytes());
            outputStream.flush();

            byte[] buffer = new byte[124];
            InputStream inputStream = socket.getInputStream();
            inputStream.read(buffer);
            if (data.equals("send VERSION")){
                vizVersion = new String(buffer);
            }
        } catch (IOException e) {
            vizVersion = null;
            socket = null;
            System.err.println("Cannot open output stream.");
        }
    }

    public String getIp() {
        return ip;
    }

    public String getVizVersion() {
        if (socket != null) {
            sendMessage("send VERSION");
        }
        return vizVersion;
    }

    //TODO:maybe change the reconnect logic to a more understandable
    public boolean isConnected() {
        boolean isConnected = false;
        if (socket != null) {
            if (socket.isConnected()) {
                sendMessage("send VERSION");
                if (vizVersion != null) {
                    if (vizVersion.equals("")) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        } else {
            isConnected = connect();
        }
        return isConnected;
    }
}