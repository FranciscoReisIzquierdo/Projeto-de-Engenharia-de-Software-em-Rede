package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class RouterWorker implements Runnable{
    private static final int MAX_BYTES = 1024;
    private InetAddress host = InetAddress.getByName("localhost");
    private int tcpPort;
    private int udpPort;
    private ArrayList<Vizinho> vizinhos;
    // Now is an Integer, later change to an InetAddress!!!!!!!
    private ConcurrentHashMap<Integer, DataOutputStream> outputVizinhos = new ConcurrentHashMap<>();
    private Socket connection;
    private DataInputStream input;
    private DataOutputStream output;
    // Now is an Integer, later change to an InetAddress!!!!!!!
    private ConcurrentHashMap<Integer, ArrayList<Rota>> tabelaEncaminhamento;
    private UDPCenter udpCenter;


    public RouterWorker(int tcpPort, int udpPort, ArrayList<Vizinho> vizinhos, ConcurrentHashMap<Integer, DataOutputStream> outputVizinhos, Socket connection, ConcurrentHashMap<Integer, ArrayList<Rota>> tabelaEncaminhamento, UDPCenter udpCenter) throws IOException {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.vizinhos = vizinhos;
        this.outputVizinhos = outputVizinhos;
        this.connection = connection;
        this.input = new DataInputStream(new BufferedInputStream(this.connection.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.connection.getOutputStream()));
        this.tabelaEncaminhamento = tabelaEncaminhamento;
        this.udpCenter = udpCenter;
    }


    @Override
    public void run() {
        boolean running = true;
        while(running) {
            byte[] info = new byte[MAX_BYTES];
            int size;
            Header header = null;
            try {
                size = this.input.read(info);
                header = Header.translate(info, size);
            } catch (Exception e) {
                System.out.println("Something went wrong on RouterWorker class...fix it you twat!");
            }
            System.out.println("Received TCP Packet > " + header.toString() + "\n");

            switch (header.getType()) {
                case 1:
                    buildTabelaEncaminhamento(header);
                    setBestServer();
                    printTabelaEncaminhamento();
                    try {
                        flooding(header);
                    } catch (UnknownHostException e) {
                        System.out.println("Something went wrong on RouterWorker class while flooding the network... fix it u dumb fuck!");
                    }
                    break;

                case 2:
                    request(header);
                    break;

                case 3:
                    running = destroyFlow(header);
                    break;
            }
        }
        try {
            this.connection.shutdownInput();
            this.connection.shutdownOutput();
            this.connection.close();
        } catch (IOException e) {
            System.out.println("Something went wrong while sending type 3 message on class RouterWorker...fix it u dumb fuck!");
        }
    }

    public void buildTabelaEncaminhamento(Header header){
        long delay = new Date().getTime() - header.getTimestamp().getTime();
        Rota rota = null;
        for (Vizinho vizinho : this.vizinhos) {
            if (vizinho.getVizinho().toString().equals(header.getHost()) && vizinho.getTcpPort() == header.getUdpOriginPortOrtcpOriginPort()) {
                rota = new Rota(vizinho, delay, header.getJumps() + 1, new Destino(header.getFont(), header.getUdpFontPortOrtcpFontPort()));
                break;
            }
        }

        if(!this.tabelaEncaminhamento.containsKey(header.getUdpFontPortOrtcpFontPort())){
            ArrayList<Rota> rotas = new ArrayList<>();
            rotas.add(rota);
            this.tabelaEncaminhamento.put(header.getUdpFontPortOrtcpFontPort(), rotas);

        }
        else {
            ArrayList<Rota> listaRotas = this.tabelaEncaminhamento.get(header.getUdpFontPortOrtcpFontPort());
            for (Rota r : listaRotas) {
                if (r.getDelay() > delay + delay * 0.7) {
                    listaRotas.add(listaRotas.indexOf(r), rota);
                    break;
                } else if (r.getDelay() <= delay + delay * 0.7 && r.getDelay() > delay + delay * 0.35 && rota.getJumps() < r.getJumps()) {
                    listaRotas.add(listaRotas.indexOf(r), rota);
                    break;
                }
                listaRotas.add(rota);
                break;
            }
        }
    }

    public void flooding(Header header) throws UnknownHostException {
        if(!this.outputVizinhos.containsKey(header.getUdpOriginPortOrtcpOriginPort())) this.outputVizinhos.put(header.getUdpOriginPortOrtcpOriginPort(), this.output);

        for (Vizinho vizinho : this.vizinhos) {
            if (!vizinho.getVizinho().toString().equals(header.getHost()) || vizinho.getTcpPort() != header.getUdpOriginPortOrtcpOriginPort()) {
                try {
                    Socket connection = new Socket(vizinho.getVizinho(), vizinho.getTcpPort());
                    if(!this.outputVizinhos.containsKey(vizinho.getTcpPort())){
                        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));
                        this.outputVizinhos.put(vizinho.getTcpPort(), out);
                    }

                    Header floading = new Header(1, header.getFont(), header.getUdpFontPortOrtcpFontPort(), this.tcpPort, "/" + this.host.toString().split("/")[1], vizinho.getVizinho().toString(), header.getJumps() + 1, header.getTimestamp());
                    this.outputVizinhos.get(vizinho.getTcpPort()).write(floading.typeMessage());
                    this.outputVizinhos.get(vizinho.getTcpPort()).flush();

                    Thread thread = new Thread(new RouterWorker(this.tcpPort, this.udpPort, this.vizinhos, this.outputVizinhos, connection, this.tabelaEncaminhamento, this.udpCenter));
                    thread.start();

                } catch (IOException e) {
                    System.out.println("The neighbor I am trying to connect is a client that is offline!");
                }
            }
        }
    }

    public void setBestServer(){
        long delay = -1;
        // Also change here the Integer to InetAddress!!!!!!!
        int server = 0;
        for(Map.Entry<Integer, ArrayList<Rota>> set : this.tabelaEncaminhamento.entrySet()) {
            long compareDelay = set.getValue().get(0).getDelay();
            if(compareDelay < delay || delay < 0){
                delay = compareDelay;
                server = set.getKey();
            }
        }
        this.udpCenter.getLock().lock();
        this.udpCenter.setServer(server);
        this.udpCenter.getLock().unlock();
    }

    public void request(Header header){
        Vizinho v = null;
        for (Vizinho vizinho : this.vizinhos) {
            if (vizinho.getVizinho().toString().equals(header.getHost()) && vizinho.getUdpPort() == header.getUdpOriginPortOrtcpOriginPort()) {
                v = vizinho;
                break;
            }
        }
        this.udpCenter.getLock().lock();
        this.udpCenter.getListaStreams().add(v);
        if(this.udpCenter.getUDPClients() == 0){
            Rota bestRota = this.tabelaEncaminhamento.get(this.udpCenter.getServer()).get(0);
            Header request = new Header(2, header.getFont(), header.getUdpFontPortOrtcpFontPort(), this.udpPort, "/" + this.host.toString().split("/")[1], bestRota.getInterfaceSaida().getVizinho().toString(), header.getJumps() + 1, header.getTimestamp());
            try {
                Thread udpWorker= new Thread(new RouterUDPWorker(this.udpCenter, this.udpPort));
                udpWorker.start();
                this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).write(request.typeMessage());
                this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).flush();
                bestRota.setState(true);
            } catch (IOException e) {
                System.out.println("Something went wrong on RouterWorker class reading type 2 message...fix it u dumb fuck!");
                System.out.println(e);
            }
        }
        this.udpCenter.addUDPClient();
        this.udpCenter.getLock().unlock();
    }

    public boolean destroyFlow(Header header){
        this.udpCenter.getLock().lock();
        this.udpCenter.removeUDPClient();
        for(Vizinho vizinho : this.udpCenter.getListaStreams()){
            if(vizinho.getUdpPort() == header.getUdpOriginPortOrtcpOriginPort()){
                this.udpCenter.getListaStreams().remove(vizinho);
                break;
            }
        }

        if(this.udpCenter.getUDPClients() == 0){
            Rota bestRota = this.tabelaEncaminhamento.get(this.udpCenter.getServer()).get(0);
            Header request = new Header(3, header.getFont(), header.getUdpFontPortOrtcpFontPort(), this.udpPort, "/" + this.host.toString().split("/")[1], bestRota.getInterfaceSaida().getVizinho().toString(), header.getJumps() + 1, header.getTimestamp());

            try {
                this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).write(request.typeMessage());
                this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).flush();
            } catch (IOException e) {
                System.out.println("Something went wrong while sending type 3 message on class RouterWorker...fix it u dumb fuck!");
            }
            bestRota.setState(true);
            for(Vizinho v : this.vizinhos){
                // Change here to compare InetAddress!!!!!!!
                if(v.getUdpPort() == header.getUdpFontPortOrtcpFontPort()){
                    System.out.println("Client " + v.getVizinho() + ":" + v.getUdpPort() + " has disconnected\n");
                    this.udpCenter.getLock().unlock();
                    return false;
                }
            }
        }
        this.udpCenter.getLock().unlock();
        return true;
    }

    public void printTabelaEncaminhamento(){
        System.out.println("|-------------------------------------------------------------------------------------------------------------|");
        System.out.println("|          Interface de Saída          |   Delay (ms)   |   Saltos   |          Destino          |   Estado   |");
        System.out.println("|-------------------------------------------------------------------------------------------------------------|");

        for(Map.Entry<Integer, ArrayList<Rota>> set : this.tabelaEncaminhamento.entrySet()){
            for(Rota rota : set.getValue()){
                System.out.println(rota.toString());
                System.out.println("|-------------------------------------------------------------------------------------------------------------|");
            }
        }
    }
}
