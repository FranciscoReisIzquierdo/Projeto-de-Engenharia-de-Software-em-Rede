package com.company;

import java.io.*;
import java.net.*;

public class ServerWorker implements Runnable {
    private static final int MAX_BYTES = 1024;
    private Socket socket;
    private UDPCenter udpCenter;
    private int destPort;
    private String host;
    private DataInputStream input;

    public ServerWorker(Socket client,UDPCenter udpCenter, int destPort, String host, DataInputStream input) throws IOException {
        this.socket = client;
        this.udpCenter = udpCenter;
        this.destPort = destPort;
        this.host = host;
        this.input = input;
    }


    @Override
    public void run() {
        while(this.socket.isConnected()){
            byte[] info = new byte[MAX_BYTES];
            int size = 0;
            try {
                size = input.read(info);
                Header header = Header.translate(info, size);
                if(header.getType() == 3) break;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        this.udpCenter.getLock().lock();
        this.udpCenter.removeUDPClient();
        this.udpCenter.getStream().UDPclients.remove((Object) this.destPort);
        if(this.udpCenter.getUDPClients() == 0) this.udpCenter.setStream(null);
        this.udpCenter.getLock().unlock();
        disconnect();
    }

    public void disconnect(){
        System.out.println("Client " + this.host + " at port #" + this.destPort + " has disconnected!");
    }

}
