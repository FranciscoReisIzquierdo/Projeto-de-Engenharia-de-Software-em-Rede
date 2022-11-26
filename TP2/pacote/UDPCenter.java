package pacote;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class UDPCenter{
    private InetAddress server;
    private int UDPClients;
    private StreamWorker stream;
    private Lock lock = new ReentrantLock();
    private ArrayList<Vizinho> listaStreams = new ArrayList<>();
    private ConcurrentHashMap<String, Boolean> idMessages = new ConcurrentHashMap<>();
    private ConcurrentHashMap<InetAddress, ArrayList<Rota>> tabelaEncaminhamento = new ConcurrentHashMap<>();

    public UDPCenter(int UDPClients, StreamWorker stream){
        this.UDPClients = UDPClients;
        this.stream = stream;
        this.server = null;
    }

    public int getUDPClients() { return UDPClients; }

    public StreamWorker getStream() { return stream; }

    public Lock getLock() { return lock; }

    public void setStream(StreamWorker stream) { this.stream = stream; }

    public void addUDPClient(){ this.UDPClients++; }

    public void removeUDPClient(){ this.UDPClients--; }

    public ArrayList<Vizinho> getListaStreams() { return listaStreams; }

    public InetAddress getServer() { return server; }

    public void setServer(InetAddress server) { this.server = server; }

    public ConcurrentHashMap<String, Boolean> getIdMessages() { return idMessages; }

    public ConcurrentHashMap<InetAddress, ArrayList<Rota>> getTabelaEncaminhamento() { return tabelaEncaminhamento; }
}
