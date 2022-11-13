package com.company;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;



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
        this.datagramSocket.close();
    }

    public void streamingPackets(){
        while(this.udpCenter.getUDPClients() > 0){
            this.udpCenter.getLock().unlock();
            this.cBuf = new byte[15000];
            this.rcvdp = new DatagramPacket(cBuf, cBuf.length);
            try {
                this.datagramSocket.setSoTimeout(1000);
                this.datagramSocket.receive(this.rcvdp);
                this.udpCenter.getLock().lock();

                for(Vizinho vizinho : this.udpCenter.getListaStreams()){
                    int packet_length = this.rcvdp.getLength();
                    DatagramPacket packet = new DatagramPacket(this.rcvdp.getData(), packet_length, vizinho.getVizinho(), vizinho.getUdpPort());
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
