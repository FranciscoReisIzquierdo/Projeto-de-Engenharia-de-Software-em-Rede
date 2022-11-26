package pacote;
import java.net.InetAddress;
import java.net.UnknownHostException;

class Vizinho{
    private InetAddress vizinho;
    private int tcpPort;
    private int udpPort;

    public Vizinho(String vizinho) throws UnknownHostException {
        this.vizinho = InetAddress.getByName(vizinho);
        this.tcpPort = 25000;
        this.udpPort = 50000;
    }

    public InetAddress getVizinho() { return this.vizinho; }

    public int getTcpPort() { return this.tcpPort; }

    public int getUdpPort() { return this.udpPort; }

    @Override
    public String toString() {
        return vizinho.toString().split("/")[1];
    }
}
