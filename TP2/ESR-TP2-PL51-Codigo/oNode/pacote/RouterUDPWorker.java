package pacote;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;


/* Método que é responsável por entregar os pacotes UDP da stream ao nodo seguinte da melhor rota */
public class RouterUDPWorker implements Runnable{
    private UDPCenter udpCenter;
    private int udpPort;
    private DatagramSocket datagramSocket;
    DatagramPacket rcvdp;
    byte[] cBuf;


    /* Construtor parameterizado */
    public RouterUDPWorker(UDPCenter udpCenter, int udpPort) throws SocketException {
        this.udpCenter = udpCenter;
        this.udpPort = udpPort;
        this.datagramSocket = new DatagramSocket(this.udpPort);
    }


    /* Método run da thread */
    public void run() {
        this.udpCenter.getLock().lock();
        streamingPackets();
        try{
            this.udpCenter.getLock().unlock();
        } catch(Exception e){}
        this.datagramSocket.close();
    }


    /* Método que entrega os pacotes UDP da stream a cada cliente da lista de clientes */
    public void streamingPackets(){
        while(this.udpCenter.getUDPClients() > 0){

            this.udpCenter.getLock().unlock();
            this.cBuf = new byte[15000];
            this.rcvdp = new DatagramPacket(cBuf, cBuf.length);

            try {
                this.datagramSocket.setSoTimeout(2000);
                this.datagramSocket.receive(this.rcvdp);
                this.udpCenter.getLock().lock();

                ArrayList<Vizinho> listaClientes = this.udpCenter.getListaStreams();
                for(Vizinho vizinho : listaClientes){
                    int packet_length = this.rcvdp.getLength();
                    DatagramPacket packet = new DatagramPacket(this.rcvdp.getData(), packet_length, vizinho.getVizinho(), 50000);
                    this.datagramSocket.send(packet);
                }
            }
            catch (SocketTimeoutException e) { break; }
            catch (IOException e ) {
                System.out.println("Something went wrong on RouterUDPWorker class...fix it u dumb fuck!");
                break;
            }
        }
    }
}