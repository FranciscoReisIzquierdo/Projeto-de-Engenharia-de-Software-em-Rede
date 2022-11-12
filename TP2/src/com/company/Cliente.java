package com.company;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Date;

public class Cliente {
    private static final int MAX_BYTES = 1024;
    private InetAddress host;
    //private int originPort;
    //private static int RTP_RCV_PORT = 25000;
    int RTP_RCV_PORT;
    private int destPort;
    private InetAddress dest;
    private Socket socketInfo;
    //DatagramSocket RTPsocket;
    private DataInputStream input;
    private DataOutputStream output;
    private ClientStream stream;
    boolean isOn;


    public Cliente(int UDP_origin_Port, int destPort, String dest) throws IOException {
        this.host = InetAddress.getByName("localhost");
        this.RTP_RCV_PORT = UDP_origin_Port;
        //this.originPort = originPort;
        this.destPort = destPort;
        this.dest = InetAddress.getByName(dest);
        this.socketInfo = new Socket(this.dest, this.destPort);
        this.input = new DataInputStream(new BufferedInputStream(this.socketInfo.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.socketInfo.getOutputStream()));
    }

    // Tipo | Quem (host:porta) | O que |
    public void request() throws IOException {
        this.output.write(typeMessage(2, null, this.host, this.RTP_RCV_PORT, this.dest, this.destPort, 0, null));
        this.output.flush();
        this.isOn = true;
        this.stream = new ClientStream(this.socketInfo, this.output, this.host, this.RTP_RCV_PORT, this.dest, this.destPort);
        while(this.socketInfo.isConnected());
    }

    public static void disconnect(Socket client, DataOutputStream output, InetAddress host, int RTP_RCV_PORT, InetAddress dest, int destPort) throws IOException {
        output.write(typeMessage(3, null, host, RTP_RCV_PORT, dest, destPort, 0, null));
        output.flush();
        client.close();
        System.exit(0);
    }


    public static byte[] typeMessage(int type, String message, InetAddress host, int RTP_RCV_PORT, InetAddress dest, int destPort, int jumps, Timestamp timestamp) throws IOException {
        if(timestamp == null) timestamp = new Timestamp(new Date().getTime());
        byte[]header = (type + ";" + host + ":" + RTP_RCV_PORT + ";" + dest + ":" + destPort + ";" + jumps + ";" + timestamp).getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(header);
        if(message!= null) {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            outputStream.write(data);
        }
        return outputStream.toByteArray();
    }
}