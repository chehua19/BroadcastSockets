package sockets;

import content.Signal;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class AtemSocket {
    //https://github.com/applest/node-applest-atem/blob/master/src/atem.coffee

    private DatagramSocket socket;
    private InetAddress address;
    private static String NAME;

    private final ArrayList<Signal> SIGNALS = new ArrayList<>();
    private int AUXs;

    private String ip;

    private byte connectionState;
    private int localPacketId;

    private byte[] sessionId;
    private byte[] remotePacketId;

    private byte[] commandHello = new byte[]{
            0x10, 0x14, 0x53, (byte)0xAB,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x3A, 0x00, 0x00,
            0x01, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
    };

    private byte[] commandHelloAnswer = new byte[]{
            (byte)0x80, 0x0C, 0x53, (byte)0xAB,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x03, 0x00, 0x00
    };

    public AtemSocket(String ip){
        this.ip = ip;
        localPacketId = 1;

        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName(ip);
        } catch (SocketException e) {
            System.err.println("Cannot create socket.");
        } catch (UnknownHostException e) {
            System.err.println("Unknown address.");
        }

    }

    public void connect(){
        sendPacket(commandHello);


        byte[] receive = new byte[1420];
        try {
            socket.receive(new DatagramPacket(receive, receive.length));
            receivePacket(receive);
        } catch (IOException e) {
            System.out.println("try to connect to atem exception");
            e.printStackTrace();
        }
        connectionState = ConnectionState.SynSend.getCode();
    }

    public void takeCut(){
        sendMessage("DCut", new byte[]{0x00, (byte) 0xef, (byte) 0xbf, 0x5f});
    }

    public void changeProgramCamera(int camera){
        // for m/e 2 [0x01, 0x75, (byte) (camera >> 8), (byte) (camera & 0xFF)] ;
        // for m/e 1 [0x00, 0x00, (byte) (camera >> 8), (byte) (camera & 0xFF)]

        sendMessage("CPgI", new byte[]{0x00, 0x00, (byte) (camera >> 8), (byte) (camera & 0xFF)});
    }

    public void changeProgramCameraMe2(int camera){
        sendMessage("CPgI", new byte[]{0x01, 0x75, (byte) (camera >> 8), (byte) (camera & 0xFF)});
    }

    public void changePreviewCamera(int camera){
        sendMessage("CPvI", new byte[]{0x00, 0x00, (byte) (camera >> 8), (byte) (camera & 0xFF)});
    }

    public void changePreviewCameraMe2(int camera){
        sendMessage("CPvI", new byte[]{0x01, 0x75, (byte) (camera >> 8), (byte) (camera & 0xFF)});
    }

    // aux number start from 0
    public void changeAuxInput(int aux, int camera){
        sendMessage("CAuS", new byte[]{0x01, (byte) aux, (byte) (camera >> 8), (byte) (camera & 0xFF)});
    }

    private void sendPacket(byte[] packet){
        DatagramPacket outputPacket = new DatagramPacket(packet, packet.length, address, 9910);
        try {
            socket.send(outputPacket);

            byte[] receive = new byte[1422];
            try {
                socket.receive(new DatagramPacket(receive, receive.length));
                receivePacket(receive);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("cannot send.");
        }
    }


    private void receivePacket(byte[] message){
        int length = ((message[0] & 0x07) << 8) | message[1];
        byte flags = (byte) (message[0] >> 3);

        sessionId = new byte[]{message[2], message[3]};
        remotePacketId = new byte[]{message[10], message[11]};

        // when receive connect flag
        if ((flags & AtemSocket.PacketFlag.Connect.getCode()) != 0 && (flags & AtemSocket.PacketFlag.Repeat.getCode()) == 0){
            sendPacket(commandHelloAnswer);
        }
        /*
        // get all atem data
        if ((flags & PacketFlag.Sync.getCode()) != 0 && length > 12){
            byte[] dataBytes = Arrays.copyOfRange(message, 12, message.length);

            int dataLength = dataBytes[0] | dataBytes[1];
            String dataName = new String(Arrays.copyOfRange(dataBytes, 2, 8));
            System.out.println(dataName + " " + dataLength);

            if (dataName.equals("InPr")){
                byte[] info = Arrays.copyOfRange(dataBytes, 9, dataBytes.length);
                System.out.println(dataBytes[8]);
                System.out.println("name: " + new String(info));
            }
        }
        */
        if ((flags & PacketFlag.Sync.getCode()) != 0 && length == 12 && (connectionState == ConnectionState.Established.getCode())){
            connectionState = ConnectionState.Established.getCode();
        }

        if ((flags & PacketFlag.Sync.getCode()) != 0 && (connectionState == ConnectionState.Established.getCode())){
            byte[] ack = new byte[]{(byte) 0x80, 0x0C, sessionId[0], sessionId[1], remotePacketId[0], sessionId[1], 0x41};
            sendPacket(ack);
        }

        // return config
        byte[] dataBytes = Arrays.copyOfRange(message, 12, message.length);
        parseCommand(dataBytes);
    }

    private void parseCommand(byte[] dataBytes){
        int dataLength = dataBytes[0] + (dataBytes[1] & 0xFF);
        String dataName = new String(Arrays.copyOfRange(dataBytes, 4, 8));

        if (dataLength < 4) return;
        setStatus(dataName, Arrays.copyOfRange(dataBytes, 8, dataLength));

        if (dataBytes.length > dataLength){
            parseCommand(Arrays.copyOfRange(dataBytes, dataLength, dataBytes.length));
        }
    }

    private void setStatus(String name, byte[] buffer){
        switch (name){
            case "_pin":
                NAME = new String(Arrays.copyOfRange(buffer, 0, 33));
                break;
            case "_top":
                AUXs = buffer[3];
                break;
            case "InPr":
                int videoSource = (buffer[0] << 8) + buffer[1];
                String longName = toStringArray(Arrays.copyOfRange(buffer, 2,21));
                Signal signal = new Signal(videoSource, longName);
                SIGNALS.add(signal);
                break;
            default:
                break;
        }
    }

    private String toStringArray(byte[] arr){
        StringBuilder sb = new StringBuilder();
        for (byte ch: arr) {
            if (ch == 0) break;
            sb.append((char) ch);
        }

        return sb.toString();
    }

    private void sendMessage(String command, byte[] payload){
        byte[] buffer = new byte[20 + payload.length];
        buffer[0] = (byte) (((20 + payload.length)/256) | 0x08);
        buffer[1] = (byte) ((20 + payload.length) % 256);
        buffer[2] = sessionId[0];
        buffer[3] = sessionId[1];
        buffer[10] = (byte) (localPacketId/256);
        buffer[11] = (byte) (localPacketId%256);
        buffer[12] = (byte) ((8 + payload.length)/256);
        buffer[13] = (byte) ((8 + payload.length)%256);
        buffer[16] = (byte) command.charAt(0);
        buffer[17] = (byte) command.charAt(1);
        buffer[18] = (byte) command.charAt(2);
        buffer[19] = (byte) command.charAt(3);

        for (int i = 0; i < payload.length; i++) {
            buffer[20+i] = payload[i];
        }
        sendPacket(buffer);
        localPacketId++;
    }

    private enum PacketFlag{
        Sync(0x01),
        Connect(0x02),
        Repeat(0x04);

        private byte code;

        PacketFlag(int code) {
            this.code = (byte)code;
        }

        public byte getCode(){
            return code;
        }

    }

    private enum ConnectionState {
        SynSend(0x02),
        Established(0x04);

        private byte code;

        ConnectionState(int code) {
            this.code = (byte)code;
        }

        public byte getCode(){
            return code;
        }
    }

    public boolean isConnected() {
        try {
            if (connectionState == 0) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void setConnectionState (byte connectionState) {
        this.connectionState = connectionState;
    }

    public static String getNAME() {
        return NAME;
    }

    public ArrayList<Signal> getSIGNALS() {
        return SIGNALS;
    }

    public int getAUXs() {
        return AUXs;
    }

    public String getIp() {
        return ip;
    }
}