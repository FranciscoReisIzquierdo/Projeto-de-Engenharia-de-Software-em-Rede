package pacote;
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class VizinhoServerWorker implements Runnable{
    private static final int MAX_BYTES = 1024;
    private InetAddress host;
    private InetAddress vizinho;
    private int tcpPortOrigin;
    private int udpPort;
    private int tcpPort;
    private UDPCenter udpCenter;
    private Socket socket;
    private DataOutputStream output;
    private DataInputStream input;


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

    public InetAddress getLocalHost() throws SocketException{
        NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
        Enumeration<InetAddress> inetAddress = networkInterface.getInetAddresses();
        InetAddress currentAddress;
        InetAddress ip = null;
        currentAddress = inetAddress.nextElement();
        while(inetAddress.hasMoreElements())
        {
            currentAddress = inetAddress.nextElement();
            if(currentAddress instanceof Inet4Address && !currentAddress.isLoopbackAddress())
            {
                ip = currentAddress;
                break;
            }
        }
        return ip;
     }


    @Override
    public void run() {
        sentProbe();
        while (this.socket.isConnected()) {
            try {
                byte[] info = new byte[MAX_BYTES];
                int size;
                Header header;
                size = this.input.read(info);
                header = Header.translate(info, size);

                System.out.println("Received TCP Packet > " + header.toString() + "\n");

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
                System.out.println("Something went wrong on VizinhoServerWorker class...fix it, u dumb fuck!\n" + e);
            }
        }
    }


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

    public void createStream(Vizinho vizinho){
        StreamWorker.VideoFileName = "/home/core/Desktop/TP2VSCode/movie.Mjpeg";
        File f = new File(StreamWorker.VideoFileName);
        if (f.exists()) {
            //Create a Main object
            ArrayList<Vizinho> Listvizinho = new ArrayList<>();
            Listvizinho.add(vizinho);
            this.udpCenter.setStream(new StreamWorker(Listvizinho));
            //show GUI: (opcional!)
            //s.pack();
            //s.setVisible(true);
        } else {
            System.out.println("Ficheiro de video não existe: " + StreamWorker.VideoFileName);
        }
    }

    public void destroyStreamFlow(Header header){
        this.udpCenter.getLock().lock();
        this.udpCenter.removeUDPClient();


        for(Vizinho v : this.udpCenter.getListaStreams()){
            if(v.getUdpPort() == header.getUdpOriginPortOrtcpOriginPort()){
                this.udpCenter.getListaStreams().remove(v);
                this.udpCenter.getStream().UDPclients.remove(v);
                break;
            }
        }
        if(this.udpCenter.getUDPClients() == 0) this.udpCenter.setStream(null);
        this.udpCenter.getLock().unlock();
    }

    public void sentProbe(){
        Header header = new Header(1, this.host.toString().split("/")[1], this.tcpPortOrigin, this.tcpPortOrigin, this.host.toString().split("/")[1], this.vizinho.toString(), 0, null, null);
        try {
            this.output.write(header.typeMessage());
            this.output.flush();
        } catch (IOException exception) {
            System.out.println("Something went wrong on VizinhoServerWorker class...fix it, u dumb fuck!");
        }
    }
}