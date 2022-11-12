package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class VizinhoServerWorker implements Runnable{
    private static final int MAX_BYTES = 1024;
    private InetAddress host = InetAddress.getByName("localhost");
    private InetAddress vizinho;
    private int tcpPortOrigin;
    private int udpPort;
    private int tcpPort;
    private UDPCenter udpCenter;
    private Socket socket;
    private DataOutputStream output;
    private DataInputStream input;


    public VizinhoServerWorker(InetAddress vizinho, int tcpPort, int udpPort, int tcpPortOrigin, UDPCenter udpCenter) throws IOException {
        this.vizinho = vizinho;
        this.tcpPortOrigin = tcpPortOrigin;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.udpCenter = udpCenter;
        this.socket = new Socket(this.vizinho, this.tcpPort);
        this.input= new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
    }


    @Override
    public void run() {
        Header header = new Header(1, "/" + this.host.toString().split("/")[1], this.tcpPortOrigin, this.tcpPortOrigin, "/" + this.host.toString().split("/")[1], this.vizinho.toString(), 0, null);
        try {
            this.output.write(header.typeMessage());
            this.output.flush();
            System.out.println("Message sent to: " + this.vizinho + ":#(tcpPort)" + this.tcpPort);
            while(this.socket.isConnected()){
                byte[] info = new byte[MAX_BYTES];
                int size = this.input.read(info);
                // check the type message you cunt!
            }
        } catch (IOException e) {
            System.out.println("Something went wrong on VizinhoServerWorker class...fix it, u dumb fuck!");
        }
    }
}
