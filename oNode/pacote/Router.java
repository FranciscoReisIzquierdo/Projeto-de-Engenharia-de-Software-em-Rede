package pacote;

import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;


/* Classe que representa um router */
public class Router {
    private ServerSocket serverSocket;
    private int tcpPort;
    private int udpPort;
    private ArrayList<Vizinho> vizinhos;
    private ConcurrentHashMap<InetAddress, DataOutputStream> outputVizinhos = new ConcurrentHashMap<>();
    private UDPCenter udpCenter;


    /* Construtor parameterizado */
    public Router(ArrayList<Vizinho> vizinhos) throws IOException {
        this.tcpPort = 25000;
        this.udpPort = 50000;
        this.vizinhos = vizinhos;
        this.serverSocket = new ServerSocket(this.tcpPort);
        this.udpCenter = new UDPCenter(0, null);
    }


    /* Método que recebe pedidos TCP/IP em modo servidor */
    public void run() throws IOException {
        while(true) {
            Socket client = this.serverSocket.accept();
            Thread thread = new Thread(new RouterWorker(this.vizinhos, this.outputVizinhos, client, this.udpCenter));
            thread.start();
        }
    }
}
