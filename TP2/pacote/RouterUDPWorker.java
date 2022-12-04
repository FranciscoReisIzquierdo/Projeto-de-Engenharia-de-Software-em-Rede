package pacote;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;



public class RouterUDPWorker implements Runnable{
    private UDPCenter udpCenter;
    private int udpPort;
    private DatagramSocket datagramSocket;
    DatagramPacket rcvdp;
    byte[] cBuf;


    public RouterUDPWorker(UDPCenter udpCenter, int udpPort) throws SocketException {
        this.udpCenter = udpCenter;
        this.udpPort = udpPort;
        this.datagramSocket = new DatagramSocket(this.udpPort);
    }


    @Override
    public void run() {
        this.udpCenter.getLock().lock();
        streamingPackets();
        try{
            this.udpCenter.getLock().unlock();
        } catch(Exception e){System.out.println("Try to unlock a lock that isn't mine!");}
        this.datagramSocket.close();
    }

    public void streamingPackets(){
        while(this.udpCenter.getUDPClients() > 0){
            this.udpCenter.getLock().unlock();
            this.cBuf = new byte[15000];
            this.rcvdp = new DatagramPacket(cBuf, cBuf.length);
            try {
                this.datagramSocket.setSoTimeout(2000);
                this.datagramSocket.receive(this.rcvdp);
                //System.out.println("Received udp packet");
                this.udpCenter.getLock().lock();
                //System.out.println("Will send udp packet to list of clients, size -> " + this.udpCenter.getListaStreams().size() + " Cliente -> " + this.udpCenter.getListaStreams().get(0).toString());
                ArrayList<Vizinho> listaClientes = this.udpCenter.getListaStreams();
                for(Vizinho vizinho : listaClientes){
                    //System.out.println("Sending udp packets to: " + vizinho.getVizinho());
                    int packet_length = this.rcvdp.getLength();
                    DatagramPacket packet = new DatagramPacket(this.rcvdp.getData(), packet_length, vizinho.getVizinho(), 50000);
                    this.datagramSocket.send(packet);
                }
            }
            catch (SocketTimeoutException e) {
                break;
            }
            catch (IOException e ) {
                System.out.println("Something went wrong on RouterUDPWorker class...fix it u dumb fuck!");
                break;
            }
        }
    }
}