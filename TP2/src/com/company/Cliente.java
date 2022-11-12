package com.company;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Date;

public class Cliente {
    //private int originPort;
    //private static int RTP_RCV_PORT = 25000;
    //DatagramSocket RTPsocket;
    private DataInputStream input;
    private static final int MAX_BYTES = 1024;
    private InetAddress host;
    int RTP_RCV_PORT; // #udp origin port
    private int tcpDestPort;
    private InetAddress dest;
    private Socket socketInfo;
    private DataOutputStream output;
    private ClientStream stream;
    boolean isOn;


    public Cliente(int udpOriginPort, int tcpDestPort, String dest) throws IOException {
        this.host = InetAddress.getByName("localhost");
        this.RTP_RCV_PORT = udpOriginPort;
        this.tcpDestPort = tcpDestPort;
        this.dest = InetAddress.getByName(dest);
        this.socketInfo = new Socket(this.dest, this.tcpDestPort);
        this.input = new DataInputStream(new BufferedInputStream(this.socketInfo.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.socketInfo.getOutputStream()));
    }

    public void run() throws IOException {
        request();

    }


    public void request() throws IOException {
        Header header = new Header(2, this.host.toString(), this.RTP_RCV_PORT, this.RTP_RCV_PORT, "/" + this.host.toString().split("/")[1], this.dest.toString(), 0, null);
        this.output.write(header.typeMessage());
        this.output.flush();
        System.out.println("Request for stream sent to :" + this.dest + ":" + this.tcpDestPort);
        this.isOn = true;
        this.stream = new ClientStream(this.socketInfo, this.output, this.host, this.RTP_RCV_PORT, this.dest, this.tcpDestPort);
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