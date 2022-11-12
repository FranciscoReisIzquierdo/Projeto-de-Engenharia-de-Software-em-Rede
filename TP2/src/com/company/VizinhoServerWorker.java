package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

public class VizinhoServerWorker implements Runnable{
    private static final int MAX_BYTES = 1024;
    private InetAddress host = InetAddress.getByName("localhost");
    private InetAddress vizinho;
    private int tcpPortOrigin;
    private int udpPort;
    private int tcpPort;
    private UDPCenter udpCenter;
    private Socket socket;
    private DataOutputStream output;
    private DataInputStream input;


    public VizinhoServerWorker(InetAddress vizinho, int tcpPort, int udpPort, int tcpPortOrigin, UDPCenter udpCenter) throws IOException {
        this.vizinho = vizinho;
        this.tcpPortOrigin = tcpPortOrigin;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.udpCenter = udpCenter;
        this.socket = new Socket(this.vizinho, this.tcpPort);
        this.input= new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
    }


    @Override
    public void run() {
        sentProbe();
        while (this.socket.isConnected()) {
            try {
                byte[] info = new byte[MAX_BYTES];
                int size = 0;
                Header header = null;
                size = this.input.read(info);
                header = Header.translate(info, size);

                System.out.println("Message type " + header.getType() + " received from " + header.getHost() + ":(#tcpPort)" + header.getUdpOriginPortOrtcpOriginPort() + " jumps = " + header.getJumps() + " writer is " + header.getFont() + ":" + header.getUdpFontPortOrtcpFontPort());
                switch (header.getType()) {
                    case 2:
                        Vizinho vizinho = new Vizinho(this.vizinho.toString().split("/")[1], this.tcpPort, this.udpPort);
                        this.udpCenter.getLock().lock();
                        if (this.udpCenter.getUDPClients() == 0) {
                            createStream(header, vizinho);
                        } else {
                            this.udpCenter.getStream().UDPclients.add(vizinho);
                        }
                        this.udpCenter.addUDPClient();
                        this.udpCenter.getLock().unlock();
                        break;

                    case 3:

                        break;
                }
            } catch (IOException e) {
                System.out.println("Something went wrong on VizinhoServerWorker class...fix it, u dumb fuck!");
                System.out.println(e);
            }
        }
    }

    public void createStream(Header header, Vizinho vizinho){
        StreamWorker.VideoFileName = "C:\\Users\\franc\\OneDrive\\Ambiente de Trabalho\\4Ano1Semestre\\ESR\\TP2\\src\\com\\company\\movie.Mjpeg";
        File f = new File(StreamWorker.VideoFileName);
        if (f.exists()) {
            //Create a Main object
            this.udpCenter.setStream(new StreamWorker(vizinho));
            //show GUI: (opcional!)
            //s.pack();
            //s.setVisible(true);
        } else {
            System.out.println("Ficheiro de video não existe: " + StreamWorker.VideoFileName);
        }
    }


    public void sentProbe(){
        Header header = new Header(1, "/" + this.host.toString().split("/")[1], this.tcpPortOrigin, this.tcpPortOrigin, "/" + this.host.toString().split("/")[1], this.vizinho.toString(), 0, null);
        try {
            this.output.write(header.typeMessage());
            this.output.flush();
            System.out.println("Message sent to: " + this.vizinho + ":#(tcpPort)" + this.tcpPort);
        } catch (IOException exception) {
            System.out.println("Something went wrong on VizinhoServerWorker class...fix it, u dumb fuck!");
        }
    }
}
