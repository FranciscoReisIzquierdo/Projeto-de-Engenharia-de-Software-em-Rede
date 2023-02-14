package pacote;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/* Classe que faz toda a gestão dos pacotes UDP de stream e clientes a servir */
class UDPCenter{
    private InetAddress server;
    private int UDPClients;
    private StreamWorker stream;
    private Lock lock = new ReentrantLock();
    private ArrayList<Vizinho> listaStreams = new ArrayList<>();
    private ConcurrentHashMap<String, Boolean> idMessages = new ConcurrentHashMap<>();
    private ConcurrentHashMap<InetAddress, ArrayList<Rota>> tabelaEncaminhamento = new ConcurrentHashMap<>();
    //private ConcurrentHashMap<String, Boolean> clientes = new ConcurrentHashMap<>();
    //private ConcurrentHashMap<String, InetAddress> vizinhoServeClient = new ConcurrentHashMap<>();


    /* Construtor parameterizado */
    public UDPCenter(int UDPClients, StreamWorker stream){
        this.UDPClients = UDPClients;
        this.stream = stream;
        this.server = null;
    }


    /* Método que devolve o número de clientes a servir */
    public int getUDPClients() { return UDPClients; }


    /* Método que devolve a thread responsável por realizar o envio de pacotes UDP da stream  */
    public StreamWorker getStream() { return stream; }


    /* Método que devolve o cadeado */
    public Lock getLock() { return lock; }


    /* Método que define a thread responsável por realizar o envio de pacotes UDP da stream */
    public void setStream(StreamWorker stream) { this.stream = stream; }


    /* Método que incrementa o número de clientes a servir */
    public void addUDPClient(){ this.UDPClients++; }


    /* Método que decrementa o número de clientes a servir */
    public void removeUDPClient(){ this.UDPClients--; }


    /* Método que devolve a lista de clientes que estão a ser servidor pela stream */
    public ArrayList<Vizinho> getListaStreams() { return listaStreams; }


    /* Método que devolve qual o servidor que está a fornecer os pacotes UDP da stream */
    public InetAddress getServer() { return server; }


    /* Método que devolve o map dos vizinhos que servem um determinado cliente final */
    //public ConcurrentHashMap<String, InetAddress> getVizinhoServeCliente(){ return vizinhoServeClient; }


    /* Método de define qual o servidor que está a fornecer os pacotes UDP da stream */
    public void setServer(InetAddress server) { this.server = server; }


    /* Método que devolve o map das mensagens TCP recebidas de modo a haver o flooding controlado */
    public ConcurrentHashMap<String, Boolean> getIdMessages() { return idMessages; }

    

    /* Método que devolve para cada servidor, a lista de rotas associadas ao mesmo */
    public ConcurrentHashMap<InetAddress, ArrayList<Rota>> getTabelaEncaminhamento() { return tabelaEncaminhamento; }


    /* Método que devolve o map de clientes finais que estão a ser servidos */
    //public ConcurrentHashMap<String, Boolean> getClientes(){ return clientes; }
}
