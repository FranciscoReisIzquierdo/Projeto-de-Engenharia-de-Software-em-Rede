package pacote;

import java.net.InetAddress;
import java.net.UnknownHostException;


/* Classe que representa um nodo vizinho ao servidor */
class Vizinho{
    private InetAddress vizinho;
    private int tcpPort;
    private int udpPort;


    /* Construtor parameterizado */
    public Vizinho(String vizinho) throws UnknownHostException {
        this.vizinho = InetAddress.getByName(vizinho);
        this.tcpPort = 25000;
        this.udpPort = 50000;
    }


    /* Método que devolve o endereço ip nodo vizinho */
    public InetAddress getVizinho() { return this.vizinho; }


    /* Método que devolve o número da porta TCP do nodo vizinho */
    public int getTcpPort() { return this.tcpPort; }


    /* Método que devolve o número da porta UDP do nodo vizinho */
    public int getUdpPort() { return this.udpPort; }


    /* Método toString da classe */
    public String toString() { return vizinho.toString().split("/")[1]; }
}
