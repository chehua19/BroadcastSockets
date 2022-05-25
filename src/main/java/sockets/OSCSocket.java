package sockets;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.transport.udp.OSCPortOut;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

public class OSCSocket {
    private OSCPortOut sender;

    private String ip;
    private int port;

    public OSCSocket(String ip, int port) {
        this.ip = ip;
        this.port = port;
        try {
            sender =  new OSCPortOut(InetAddress.getByName(ip), port);
        } catch (IOException e) {
            System.out.println("OSCSocket creation error");
        }
    }

    public void sendMess(String message) throws IOException {
        OSCMessage msg = new OSCMessage(message, new ArrayList<>());
        try {
            sender.send(msg);
        } catch (Exception e) {
            System.out.println("Error.");
        }
    }

    public OSCPortOut getSender() {
        return sender;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}