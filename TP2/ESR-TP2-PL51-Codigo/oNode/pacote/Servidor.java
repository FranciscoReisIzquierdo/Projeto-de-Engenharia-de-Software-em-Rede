package pacote;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/* Classe que representa o servidor */
public class Servidor {
    private InetAddress origin;
    private int originPort;
    private ServerSocket server;
    private UDPCenter udpCenter;
    private ArrayList<Vizinho> listaVizinhos;
    private ArrayList<Thread> threadsVizinhos = new ArrayList<>();
    private ConcurrentHashMap<InetAddress, DataOutputStream> outputsVizinhos = new ConcurrentHashMap<>();


    /* Construtor parameterizado */
    public Servidor(ArrayList<Vizinho> listaVizinhos) throws IOException {
        this.origin = getLocalHost();
        this.originPort = 25000;
        this.server = new ServerSocket(this.originPort);
        this.udpCenter = new UDPCenter(0, null);
        this.listaVizinhos = listaVizinhos;
    }


     /* Método que devolve o endereço ip da interface eth0 do host */
    public InetAddress getLocalHost() throws SocketException{
        NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
        Enumeration<InetAddress> inetAddress = networkInterface.getInetAddresses();
        InetAddress currentAddress;
        InetAddress ip = null;
        currentAddress = inetAddress.nextElement();
        while(inetAddress.hasMoreElements()){
            currentAddress = inetAddress.nextElement();
            
            if(currentAddress instanceof Inet4Address && !currentAddress.isLoopbackAddress()){
                ip = currentAddress;
                break;
            }
        }
        return ip;
     }


     /* Método que recebe pedidos TCP/IP em modo servidor */
    public void run() throws IOException, InterruptedException {

        for (Vizinho v : this.listaVizinhos) {
            Socket socket = new Socket(v.getVizinho(), v.getTcpPort());
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            this.outputsVizinhos.put(v.getVizinho(), output);
            Thread thread = new Thread(new VizinhoServerWorker(v.getVizinho(), v.getTcpPort(), v.getUdpPort(), this.originPort, this.udpCenter, output, socket));
            this.threadsVizinhos.add(thread);
            thread.start();
        }
        synchronized (this) {
            while (true) {
                this.wait(15000);
                for (Vizinho v : this.listaVizinhos) {
                    sentProbe(v, this.outputsVizinhos.get(v.getVizinho()));
                }
            }
        }
    }


    /* Método responsável por enviar pacotes de prova TCP durante intervalos de tempo */
    public void sentProbe(Vizinho vizinho, DataOutputStream outputStream){
        Header header = new Header(1, this.origin.toString().split("/")[1], this.originPort, this.originPort, this.origin.toString().split("/")[1], vizinho.getVizinho().toString(), 0, null, null);
        header.setTokens(header.getTokens());
        header.addToken(hash(this.origin.toString().split("/")[1].getBytes()));
        try {
            synchronized (this){ this.wait(15000); }

            outputStream.write(header.typeMessage());
            outputStream.flush();
        } catch (IOException | InterruptedException exception) {
            System.out.println("Algo correu mal ao enviar o pacote TCP de prova!");
        }
    }


     /* Método que cria o token do nodo */
     public static int hash(byte[] str) {
        int hash = 0;
        for (int i = 0; i < str.length && str[i]!= '\0'; i++) {
            hash = str[i] + ((hash << 5) - hash);
        }
        return hash;
    }
}