package pacote;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.net.UnknownHostException;
import java.util.ArrayList;


/* Classe que trata de todos os pacote TCP no servidor */
public class VizinhoServerWorker implements Runnable{
    private static final int MAX_BYTES = 1024;
    private InetAddress host;
    private InetAddress vizinho;
    private int tcpPortOrigin;
    private int tcpPort;
    private int udpPort;
    private UDPCenter udpCenter;
    private Socket socket;
    private DataOutputStream output;
    private DataInputStream input;



    /* Construtor parameterizado */
    public VizinhoServerWorker(InetAddress vizinho, int tcpPort, int udpPort, int tcpPortOrigin, UDPCenter udpCenter, DataOutputStream output, Socket socket) throws IOException {
        this.host = getLocalHost();
        this.vizinho = vizinho;
        this.tcpPortOrigin = 25000;
        this.tcpPort = 25000;
        this.udpPort = 50000;
        this.udpCenter = udpCenter;
        this.socket = socket;
        this.input= new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        this.output = output;
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


    /* Método run da thread */
    public void run() {

        sentProbe();

        while (this.socket.isConnected()) {
            try {
                byte[] info = new byte[MAX_BYTES];
                int size;
                Header header;
                size = this.input.read(info);
                header = Header.translate(info, size);

                System.out.println("\nReceived TCP packet > " + header.toString());

                switch (header.getType()) {
                    case 2:

                        createStreamFlow();
                        break;

                    case 3:
                    case 4:
                        destroyStreamFlow(header);
                        break;
                }
            } catch(Exception e) {
                System.out.println("Algo correu mal ao ler os pacotes TCP no servidor!");
            }
        }
    }


    /* Método responsável por criar um fluxo de stream a fornecer a um dado cliente */
    public void createStreamFlow() throws UnknownHostException {
        Vizinho vizinho = new Vizinho(this.vizinho.toString().split("/")[1]);
        this.udpCenter.getLock().lock();
        if (this.udpCenter.getUDPClients() == 0) {
            createStream(vizinho);
        }
        this.udpCenter.getListaStreams().add(vizinho);
        this.udpCenter.addUDPClient();
        this.udpCenter.getLock().unlock();
    }


    /* Método responsável por criar a stream */
    public void createStream(Vizinho vizinho){
        StreamWorker.VideoFileName = "/home/core/Desktop/TP2VSCode/movie.Mjpeg";
        File f = new File(StreamWorker.VideoFileName);
        if (f.exists()) {
            ArrayList<Vizinho> Listvizinho = new ArrayList<>();
            Listvizinho.add(vizinho);
            this.udpCenter.setStream(new StreamWorker(Listvizinho));

        } else { System.out.println("Ficheiro de video não existe: " + StreamWorker.VideoFileName);}
    }


    /* Método responsável por destruir o fluxo de stream */
    public void destroyStreamFlow(Header header){
        this.udpCenter.getLock().lock();
        this.udpCenter.removeUDPClient();


        for(Vizinho v : this.udpCenter.getListaStreams()){
            if(v.getUdpPort() == header.getTCPOriginPort()){
                this.udpCenter.getListaStreams().remove(v);
                this.udpCenter.getStream().UDPclients.remove(v);
                break;
            }
        }

        if(this.udpCenter.getUDPClients() == 0) this.udpCenter.setStream(null);
        this.udpCenter.getLock().unlock();
    }


    /* Método que envia um pacote TCP de prova assim que o servidor é iniciado */
    public void sentProbe(){
        Header header = new Header(1, this.host.toString().split("/")[1], this.tcpPortOrigin, this.tcpPortOrigin, this.host.toString().split("/")[1], this.vizinho.toString(), 0, null, null);
        header.setTokens(header.getTokens());
        header.addToken(hash(this.host.toString().split("/")[1].getBytes()));
        try {
            this.output.write(header.typeMessage());
            this.output.flush();
        } catch (IOException exception) {
            System.out.println("Algo correu mal ao enviar o pacote de prova!");
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