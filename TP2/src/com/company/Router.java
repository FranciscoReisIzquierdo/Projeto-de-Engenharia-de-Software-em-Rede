package com.company;

import javax.print.attribute.standard.Destination;
import javax.xml.crypto.Data;
import java.net.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;


public class Router {
    private static final int MAX_BYTES = 1024;
    private ServerSocket serverSocket;
    private int tcpPort;
    private int udpPort;
    private ArrayList<Vizinho> vizinhos;
    // Now is an Integer, later change to an InetAddress!!!!!!!
    private ConcurrentHashMap<Integer, DataOutputStream> outputVizinhos = new ConcurrentHashMap<>();
    // Now is an Integer, later change to an InetAddress!!!!!!!
    private ConcurrentHashMap<Integer, ArrayList<Rota>> tabelaEncaminhamento = new ConcurrentHashMap<>();
    private UDPCenter udpCenter;


    // Tabela de encaminhamento
    //private String tabela;

    public Router(int tcpPort, int udpPort, ArrayList<Vizinho> vizinhos) throws IOException {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.vizinhos = vizinhos;
        this.serverSocket = new ServerSocket(this.tcpPort);
        this.udpCenter = new UDPCenter(0, null);
    }

    public void run() throws IOException {
        while(true) {
            Socket client = this.serverSocket.accept();
            //System.out.println("Router > Client connected!");

            DataInputStream input= new DataInputStream(new BufferedInputStream(client.getInputStream()));

            Thread thread = new Thread(new RouterWorker(this.tcpPort, this.udpPort, this.vizinhos, this.outputVizinhos, client, this.tabelaEncaminhamento, this.udpCenter));
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


class Rota{
    Vizinho interfaceSaida;
    long delay;
    int jumps;
    Destino destino;
    boolean state;

    public Rota(Vizinho interfaceSaida, long delay, int jumps, Destino destino){
        this.interfaceSaida = interfaceSaida;
        this.delay = delay;
        this.jumps = jumps;
        this.destino = destino;
        this.state = false;
    }


    public Vizinho getInterfaceSaida() { return interfaceSaida; }

    public long getDelay() { return delay; }

    public int getJumps() { return jumps; }

    public Destino getDestino() { return destino; }

    public boolean isState() { return state; }

    public void setInterfaceSaida(Vizinho interfaceSaida) { this.interfaceSaida = interfaceSaida; }

    public void setDelay(long delay) { this.delay = delay; }

    public void setJumps(int jumps) { this.jumps = jumps; }

    public void setState(boolean state) { this.state = state; }

    @Override
    public String toString() {
        return "| " + interfaceSaida.toString() +
                "  |      " + delay +
                "        |     " + jumps +
                "      | " + destino +
                "  |   " + state +
                "    |";
    }
}

class Destino{
    String destino;
    int tcpPort;

    public Destino(String destino, int tcpPort){
        this.destino = destino;
        this.tcpPort = tcpPort;
    }

    public String getDestino() { return destino; }

    public int getTcpPort() { return tcpPort; }

    @Override
    public String toString() {
        return destino + ':' +
                "#tcpPort=" + tcpPort;
    }
}
