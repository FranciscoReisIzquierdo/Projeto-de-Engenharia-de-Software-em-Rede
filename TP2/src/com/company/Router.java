package com.company;

import javax.xml.crypto.Data;
import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;


public class Router {
    private static final int MAX_BYTES = 1024;
    private ServerSocket serverSocket;
    private int tcpPort;
    private int udpPort;
    private ArrayList<Vizinho> vizinhos;
    private ConcurrentHashMap<Vizinho, DataOutputStream> outputVizinhos = new ConcurrentHashMap<>();


    // Tabela de encaminhamento
    //private String tabela;

    public Router(int tcpPort, int udpPort, ArrayList<Vizinho> vizinhos) throws IOException {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.vizinhos = vizinhos;
        this.serverSocket = new ServerSocket(this.tcpPort);
    }

    public void run() throws IOException {
        while(true) {
            Socket client = this.serverSocket.accept();
            System.out.println("Router > Client connected!");

            DataInputStream input= new DataInputStream(new BufferedInputStream(client.getInputStream()));

            Thread thread = new Thread(new RouterWorker(this.tcpPort, this.udpPort, this.vizinhos, this.outputVizinhos, client));
            thread.start();


            /*DataInputStream input = new DataInputStream(new BufferedInputStream(client.getInputStream()));

            byte[] info = new byte[MAX_BYTES];
            int size = input.read(info);

            Header header = Header.translate(info, size);
            System.out.println("Message received from " + header.getHost() + " at port #" + header.getUDP_originPort());

            int type = header.getType();
            switch (type) {
                // Probe packet -> sent by routers only
                case 1:


                case 2:

            }*/

        }
    }

}
