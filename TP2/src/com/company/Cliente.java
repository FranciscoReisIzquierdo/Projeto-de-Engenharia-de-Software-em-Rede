package com.company;

import java.io.*;
import java.net.*;

public class Cliente {
    private DataInputStream input;
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
        this.isOn = true;
        this.stream = new ClientStream(this.socketInfo, this.output, this.host, this.RTP_RCV_PORT, this.dest, this.tcpDestPort);
        while(this.socketInfo.isConnected());
    }
}