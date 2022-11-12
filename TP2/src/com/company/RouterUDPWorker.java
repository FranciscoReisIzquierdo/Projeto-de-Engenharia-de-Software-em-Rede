package com.company;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
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
        while(this.udpCenter.getUDPClients() > 0){
            this.udpCenter.getLock().unlock();
            this.cBuf = new byte[15000];
            this.rcvdp = new DatagramPacket(cBuf, cBuf.length);
            try {
                this.datagramSocket.receive(this.rcvdp);
                //System.out.println("Packet udp received!!!!!!!");
                this.udpCenter.getLock().lock();
                //System.out.println("Size of vizinhos: " + this.udpCenter.getListaStreams().size());
                for(Vizinho vizinho : this.udpCenter.getListaStreams()){
                    //System.out.println("Vizinho at #udpPort: " + vizinho.getUdpPort());

                    int packet_length = this.rcvdp.getLength();

                    DatagramPacket packet = new DatagramPacket(this.rcvdp.getData(), packet_length, vizinho.getVizinho(), vizinho.getUdpPort());
                    this.datagramSocket.send(packet);
                }
            } catch (IOException e) {
                System.out.println("Something went wrong on RouterUDPWorker class...fix it u dumb fuck!");
            }
        }
    }
}
