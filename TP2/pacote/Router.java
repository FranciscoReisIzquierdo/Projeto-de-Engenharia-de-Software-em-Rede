package pacote;

import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;



public class Router {
    //private static final int MAX_BYTES = 1024;
    private ServerSocket serverSocket;
    private int tcpPort;
    private int udpPort;
    private ArrayList<Vizinho> vizinhos;
    // Now is an Integer, later change to an InetAddress!!!!!!!
    private ConcurrentHashMap<InetAddress, DataOutputStream> outputVizinhos = new ConcurrentHashMap<>();
    // Now is an Integer, later change to an InetAddress!!!!!!!
    private UDPCenter udpCenter;

    public Router(ArrayList<Vizinho> vizinhos) throws IOException {
        this.tcpPort = 25000;
        this.udpPort = 50000;
        this.vizinhos = vizinhos;
        this.serverSocket = new ServerSocket(this.tcpPort);
        this.udpCenter = new UDPCenter(0, null);
    }


    public void run() throws IOException {
        while(true) {
            Socket client = this.serverSocket.accept();
            Thread thread = new Thread(new RouterWorker(this.vizinhos, this.outputVizinhos, client, this.udpCenter));
            thread.start();
        }
    }
}
