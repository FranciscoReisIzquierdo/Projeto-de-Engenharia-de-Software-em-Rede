package pacote;
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {
    private InetAddress origin;
    private int originPort;
    private ServerSocket server;
    private UDPCenter udpCenter;
    private ArrayList<Vizinho> listaVizinhos;
    private ArrayList<Thread> threadsVizinhos = new ArrayList<>();
    //Alterar para InetAddress
    private ConcurrentHashMap<InetAddress, DataOutputStream> outputsVizinhos = new ConcurrentHashMap<>();


    public Servidor(ArrayList<Vizinho> listaVizinhos) throws IOException {
        this.origin = getLocalHost();
        this.originPort = 25000;
        this.server = new ServerSocket(this.originPort);
        this.udpCenter = new UDPCenter(0, null);
        this.listaVizinhos = listaVizinhos;
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


    public void run() throws IOException, InterruptedException {

        for (Vizinho v : this.listaVizinhos) {
            Socket socket = new Socket(v.getVizinho(), v.getTcpPort());
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            //Alterar para InetAddress
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


    public void sentProbe(Vizinho vizinho, DataOutputStream outputStream){
        Header header = new Header(1, this.origin.toString().split("/")[1], this.originPort, this.originPort, this.origin.toString().split("/")[1], vizinho.getVizinho().toString(), 0, null, null);
        try {
            synchronized (this){
               this.wait(15000);
            }
            outputStream.write(header.typeMessage());
            outputStream.flush();
            System.out.println("Sent tcp packet > " + header.toString());
        } catch (IOException | InterruptedException exception) {
            System.out.println("Something went wrong on VizinhoServerWorker class...fix it, u dumb fuck!");
        }
    }
}