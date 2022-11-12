package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class RouterWorker implements Runnable{
    private static final int MAX_BYTES = 1024;
    private InetAddress host = InetAddress.getByName("localhost");
    private int tcpPort;
    private int udpPort;
    private ArrayList<Vizinho> vizinhos;
    private ConcurrentHashMap<Vizinho, DataOutputStream> outputVizinhos = new ConcurrentHashMap<>();
    private Socket connection;
    private DataInputStream input;
    private DataOutputStream output;


    public RouterWorker(int tcpPort, int udpPort, ArrayList<Vizinho> vizinhos, ConcurrentHashMap<Vizinho, DataOutputStream> outputVizinhos, Socket connection) throws IOException {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.vizinhos = vizinhos;
        this.outputVizinhos = outputVizinhos;
        this.connection = connection;
        this.input = new DataInputStream(new BufferedInputStream(this.connection.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.connection.getOutputStream()));
    }


    @Override
    public void run() {
        while(this.connection.isConnected()) {
            byte[] info = new byte[MAX_BYTES];
            int size = 0;
            Header header = null;
            try {
                size = this.input.read(info);
                header = Header.translate(info, size);
            } catch (Exception e) {
                System.out.println("Something went wrong on RouterWorker class...fix it you twat!");
            }
            System.out.println("Message type " + header.getType() + " received from " + header.getHost() + ":(#tcpPort)" + header.getTcpPort() + " jumps = " + header.getJumps() + " writer is " + header.getFont() + ":" + header.getTcpFont());
            switch (header.getType()) {
                case 1:
                    // Tabela de encaminhamento

                    for (Vizinho vizinho : this.vizinhos) {
                        if (!vizinho.getVizinho().toString().equals(header.getHost()) || vizinho.getTcpPort() != header.getTcpPort()) {
                            try {
                                Socket connection = new Socket(vizinho.getVizinho(), vizinho.getTcpPort());
                                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));
                                this.outputVizinhos.put(vizinho, out);

                                Header floading = new Header(1, header.getFont(), header.getTcpFont(), this.tcpPort, "/" + this.host.toString().split("/")[1], vizinho.getVizinho().toString(), header.getJumps() + 1, header.getTimestamp());
                                System.out.println("Message sent to " + vizinho.getVizinho() + ":" + vizinho.getTcpPort() + "-> " + floading.toString());
                                out.write(floading.typeMessage());
                                out.flush();

                                Thread thread = new Thread(new RouterWorker(this.tcpPort, this.udpPort, this.vizinhos, this.outputVizinhos, connection));
                                thread.start();

                            } catch (IOException e) {
                                System.out.println("The neighbor I am trying to connect is a client that is offline!");
                            }
                        }
                    }
                    break;

                case 2:

                    break;
            }
        }
    }
}
