package com.company;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Servidor {
    private static final int MAX_BYTES = 1024;
    private int originPort;
    private InetAddress origin;
    private ServerSocket server;
    private UDPCenter udpCenter;
    private ArrayList<Vizinho> listaVizinhos;
    private ArrayList<Thread> threadsVizinhos = new ArrayList<>();


    public Servidor(int originPort, ArrayList<Vizinho> listaVizinhos) throws IOException {
        this.originPort = originPort;
        this.origin = InetAddress.getByName("localhost");
        this.server = new ServerSocket(this.originPort);
        this.udpCenter = new UDPCenter(0, null);
        this.listaVizinhos = listaVizinhos;
    }

    public void run() throws IOException, InterruptedException {

        for (Vizinho v : this.listaVizinhos) {
            Thread thread = new Thread(new VizinhoServerWorker(v.getVizinho(), v.getTcpPort(), v.getUdpPort(), this.originPort, this.udpCenter));
            this.threadsVizinhos.add(thread);
            thread.start();
        }

        for (Thread t : this.threadsVizinhos) t.join();

        /*



        while(true){
            System.out.println("Server waiting for clients to connect.");
            Socket client = this.server.accept();
            DataInputStream input= new DataInputStream(new BufferedInputStream(client.getInputStream()));

            byte[] info = new byte[MAX_BYTES];
            int size = input.read(info);

            Header header = Header.translate(info, size);
            System.out.println("Client " + header.getHost() + " at UDP port #" + header.getUDP_originPort() + " has connected!");
            System.out.println(header.toString());
            try {
                int type = header.getType();
                switch (type) {
                    // Probe packet -> sent by routers only
                    case 1:


                    case 2:
                        // Request packet -> sent by clients only
                        this.udpCenter.getLock().lock();
                        if (this.udpCenter.getUDPClients() == 0) {
                            createStream(header);
                        } else {
                            this.udpCenter.getStream().UDPclients.add(header.getUDP_originPort());
                        }
                        this.udpCenter.addUDPClient();
                        Thread threadClient = new Thread(new ServerWorker(client, this.udpCenter, header.getUDP_originPort(), header.getHost(), input));
                        threadClient.start();
                        this.udpCenter.getLock().unlock();
                }
            }
            catch (Exception e){
                System.out.println(e);
            }
        }*/
    }
}



class UDPCenter{
    private int server;
    private int UDPClients;
    private StreamWorker stream;
    private Lock lock = new ReentrantLock();
    private ArrayList<Vizinho> listaStreams = new ArrayList<>();

    public UDPCenter(int UDPClients, StreamWorker stream){
        this.UDPClients = UDPClients;
        this.stream = stream;
        this.server = -1;
    }

    public int getUDPClients() { return UDPClients; }

    public StreamWorker getStream() { return stream; }

    public Lock getLock() { return lock; }

    public void setUDPClients(int UDPClients) { this.UDPClients = UDPClients; }

    public void setStream(StreamWorker stream) { this.stream = stream; }

    public void addUDPClient(){ this.UDPClients++; }

    public void removeUDPClient(){ this.UDPClients--; }

    public ArrayList<Vizinho> getListaStreams() { return listaStreams; }

    public int getServer() { return server; }

    public void setServer(int server) { this.server = server; }
}



class Header{
    private int type;
    private String font;
    private int udpFontPortOrtcpFontPort;
    private String host;
    private int udpOriginPortOrtcpOriginPort;
    private String dest;
    private int jumps;
    private Timestamp timestamp;

    public Header(int type, String font, int udpFontPortOrtcpFontPort, int udpOriginPortOrtcpOriginPort, String host, String dest, int jumps, Timestamp timestamp){
        this.type = type;
        this.font = font;
        this.udpFontPortOrtcpFontPort = udpFontPortOrtcpFontPort;
        this.host = host;
        this.udpOriginPortOrtcpOriginPort = udpOriginPortOrtcpOriginPort;
        this.dest = dest;
        this.jumps = jumps;
        this.timestamp = timestamp;
    }

    public int getType() { return type; }

    public String getHost() { return host; }

    public int getUDP_originPort() { return udpOriginPortOrtcpOriginPort; }

    public String getDest() { return dest; }

    public int getJumps() { return jumps; }

    public Timestamp getTimestamp() { return timestamp; }

    public String getFont() { return font; }

    public int getUdpFontPortOrtcpFontPort() { return udpFontPortOrtcpFontPort; }

    public int getUdpOriginPortOrtcpOriginPort() { return udpOriginPortOrtcpOriginPort; }

    public byte[] typeMessage() throws IOException {
        byte []header = null;
        switch (this.type){
            case 1:
            case 2:
                if(this.timestamp == null) timestamp = new Timestamp(new Date().getTime());
                header = (this.type + ";" + this.host + ":" + this.udpOriginPortOrtcpOriginPort + ";" + this.dest + ";" + this.jumps + ";" + this.timestamp + ";" + this.font + ":" + this.udpFontPortOrtcpFontPort).getBytes(StandardCharsets.UTF_8);
                break;

            case 3:
                if(this.timestamp == null) timestamp = new Timestamp(new Date().getTime());
                //header = (this.type + ";" + this.host + ":" + RTP_RCV_PORT + ";" + dest + ":" + destPort + ";" + jumps + ";" + timestamp).getBytes(StandardCharsets.UTF_8);
                break;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(header);
        return outputStream.toByteArray();
    }


    public static Header translate(byte []message, int size) throws UnknownHostException {
        byte []data;
        data = Arrays.copyOf(message, size);
        String convert = new String(data);
        String []header = convert.split(";");
        int type = Integer.parseInt(header[0]);

        if(type == 1 || type == 2){
            // type;ipOrig:tcpPort;ipDest;jumps;timestamp

            String host = header[1].split(":")[0];
            int udpOriginPortOrtcpOriginPort = Integer.parseInt(header[1].split(":")[1]);
            String dest = header[2];
            int jumps = Integer.parseInt(header[3]);
            Timestamp timestamp = Timestamp.valueOf(header[4]);
            String font = header[5].split(":")[0];
            int udpFontPortOrtcpFontPort = Integer.parseInt(header[5].split(":")[1]);
            return new Header(type, font, udpFontPortOrtcpFontPort, udpOriginPortOrtcpOriginPort, host, dest, jumps, timestamp);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Header{" +
                "type=" + type +
                ", font='" + font + '\'' +
                ", udpFontPortOrtcpFontPort=" + udpFontPortOrtcpFontPort +
                ", host='" + host + '\'' +
                ", udpOriginPortOrtcpOriginPort=" + udpOriginPortOrtcpOriginPort +
                ", dest='" + dest + '\'' +
                ", jumps=" + jumps +
                ", timestamp=" + timestamp +
                '}';
    }
}
