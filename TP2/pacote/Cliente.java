package pacote;
import java.io.*;
import java.net.*;
import java.util.Enumeration;

public class Cliente {
    private DataInputStream input;
    private InetAddress host;
    int RTP_RCV_PORT; // #udp origin port
    private int tcpDestPort;
    private InetAddress dest;
    private Socket socketInfo;
    private DataOutputStream output;
    private ClientStream stream;
    boolean isOn;


    public Cliente(String dest) throws IOException {
        this.host = getLocalHost();
        this.RTP_RCV_PORT = 50000;
        this.tcpDestPort = 25000;
        this.dest = InetAddress.getByName(dest);
        this.socketInfo = new Socket(this.dest, this.tcpDestPort);
        this.input = new DataInputStream(new BufferedInputStream(this.socketInfo.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.socketInfo.getOutputStream()));
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

    public void run() throws IOException {
        request();
    }


    public void request() throws IOException {
        Header header = new Header(2, this.host.toString().split("/")[1], this.RTP_RCV_PORT, this.RTP_RCV_PORT, "/" + this.host.toString().split("/")[1], this.dest.toString(), 0, null, null);
        this.output.write(header.typeMessage());
        this.output.flush();
        this.isOn = true;
        this.stream = new ClientStream(this.socketInfo, this.output, this.host, this.RTP_RCV_PORT, this.dest, this.tcpDestPort);
        while(this.socketInfo.isConnected());
    }
}