package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
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
    //private ConcurrentHashMap<Integer, ArrayList<Rota>> tabelaEncaminhamento;
    //private ConcurrentHashMap<String, Boolean> idMessages;
    private UDPCenter udpCenter;


    public RouterWorker(int tcpPort, int udpPort, ArrayList<Vizinho> vizinhos, ConcurrentHashMap<Integer, DataOutputStream> outputVizinhos, Socket connection, UDPCenter udpCenter) throws IOException {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.vizinhos = vizinhos;
        this.outputVizinhos = outputVizinhos;
        this.connection = connection;
        this.input = new DataInputStream(new BufferedInputStream(this.connection.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.connection.getOutputStream()));
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
                this.udpCenter.getLock().lock();
                System.out.println("Size of byte array: " + size);
                header = Header.translate(info, size);
            } catch (Exception e) {
                System.out.println("Here > Something went wrong on RouterWorker class...fix it you twat! -> " + e);
                this.udpCenter.getLock().unlock();
            }
            System.out.println("Received TCP Packet > " + header.toString() + "\n");

            switch (header.getType()) {
                case 1:
                    this.udpCenter.getLock().lock();
                    buildTabelaEncaminhamento(header);
                    setBestServer();
                    printTabelaEncaminhamento();
                    if(this.udpCenter.getIdMessages().containsKey(header.getId())){
                        this.udpCenter.getLock().unlock();
                        break;
                    }
                    this.udpCenter.getIdMessages().put(header.getId(), true);
                    this.udpCenter.getLock().unlock();
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
                case 4:
                    running = destroyFlow(header, header.getType());
                    break;
            }
            this.udpCenter.getLock().unlock();
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
        this.udpCenter.getLock().lock();
        if(!this.udpCenter.getTabelaEncaminhamento().containsKey(header.getUdpFontPortOrtcpFontPort())){
            ArrayList<Rota> rotas = new ArrayList<>();
            rotas.add(rota);
            this.udpCenter.getTabelaEncaminhamento().put(header.getUdpFontPortOrtcpFontPort(), rotas);

        }
        else {
            boolean flag = false;
            ArrayList<Rota> listaRotas = this.udpCenter.getTabelaEncaminhamento().get(header.getUdpFontPortOrtcpFontPort());

            for (Rota r : listaRotas) {
                //Rota existente, atualizar delay e reordenar melhor rota para o destino indicado
                if (r.getInterfaceSaida().getVizinho().equals(rota.getInterfaceSaida().getVizinho()) && r.getInterfaceSaida().getTcpPort() == rota.getInterfaceSaida().getTcpPort()) {
                    r.setDelay(delay);
                    flag = true;
                    /*for (int i = 0; i < listaRotas.size(); i++) {
                        for (int j = i + 1; j < listaRotas.size(); j++) {
                            if (listaRotas.get(j).getDelay() < listaRotas.get(i).getDelay())
                                Collections.swap(listaRotas, j, i);

                            else if(listaRotas.get(j).getDelay() == listaRotas.get(i).getDelay() && listaRotas.get(j).getJumps() < listaRotas.get(i).getJumps())
                                Collections.swap(listaRotas, j, i);
                        }
                    }*/
                    break;
                }
            }
            if(!flag){
                for (Rota r : listaRotas) {
                    if (r.getDelay() > delay) {
                        listaRotas.add(listaRotas.indexOf(r), rota);
                        flag = true;
                        break;
                    }
                    else if(r.getDelay() == delay && r.getJumps() > header.getJumps() + 1){
                        listaRotas.add(listaRotas.indexOf(r), rota);
                        flag = true;
                        break;
                    }
                }
            }
            if(!flag) listaRotas.add(rota);
        }
        this.udpCenter.getLock().unlock();
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

                    Header floading = new Header(1, header.getFont(), header.getUdpFontPortOrtcpFontPort(), this.tcpPort, "/" + this.host.toString().split("/")[1], vizinho.getVizinho().toString(), header.getJumps() + 1, header.getTimestamp(), header.getId());
                    this.outputVizinhos.get(vizinho.getTcpPort()).write(floading.typeMessage());
                    this.outputVizinhos.get(vizinho.getTcpPort()).flush();

                    Thread thread = new Thread(new RouterWorker(this.tcpPort, this.udpPort, this.vizinhos, this.outputVizinhos, connection, this.udpCenter));
                    thread.start();

                } catch (IOException e) {
                    System.out.println("The neighbor " + vizinho.getVizinho() + ":" + vizinho.getUdpPort() + ":" + vizinho.getTcpPort() + " I am trying to flood is a client (that maybe is offline)!");
                }
            }
        }
    }

    public void setBestServer(){
        long delay = -1;
        int jumps = -1;
        // Also change here the Integer to InetAddress!!!!!!!
        int server = 0;
        this.udpCenter.getLock().lock();
        for(Map.Entry<Integer, ArrayList<Rota>> set : this.udpCenter.getTabelaEncaminhamento().entrySet()) {
            long compareDelay = set.getValue().get(0).getDelay();
            int compareJumps = set.getValue().get(0).getJumps();
            if (compareDelay < delay || delay < 0) {
                delay = compareDelay;
                server = set.getKey();
            }
            else if(compareDelay == delay && (jumps < 0 || compareJumps < jumps)){
                jumps = compareJumps;
                server = set.getKey();
            }
        }

        //Alterar para InetAdress

        if(server != this.udpCenter.getServer() && this.udpCenter.getServer() != -1 && this.udpCenter.getUDPClients() > 0){
            System.out.println("Entrou!!!!!!!");
            changeFlow();
            this.udpCenter.setServer(server);
            newFlow();
        }
        else if(this.udpCenter.getServer() == -1 || (server != this.udpCenter.getServer() && this.udpCenter.getUDPClients() == 0)) this.udpCenter.setServer(server);
        this.udpCenter.getLock().unlock();
    }


    public void newFlow(){
        this.udpCenter.getLock().lock();
        Rota bestRota = this.udpCenter.getTabelaEncaminhamento().get(this.udpCenter.getServer()).get(0);
        Header request = new Header(2, this.host.toString(), this.udpPort, this.udpPort, "/" + this.host.toString().split("/")[1], bestRota.getInterfaceSaida().getVizinho().toString(), 0, null, null);
        try {
            this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).write(request.typeMessage());
            this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).flush();
            System.out.println("Sent tcp packet > " + request.toString());
            bestRota.setState(true);
        } catch (IOException e) {
            System.out.println("Something went wrong on RouterWorker class reading type 2 message...fix it u dumb fuck!");
        }
        this.udpCenter.getLock().unlock();
    }

    public void changeFlow(){
        this.udpCenter.getLock().lock();
        Rota bestRota = this.udpCenter.getTabelaEncaminhamento().get(this.udpCenter.getServer()).get(0);
        Header request = new Header(4, this.host.toString(), this.udpPort, this.udpPort, "/" + this.host.toString().split("/")[1], bestRota.getInterfaceSaida().getVizinho().toString(), 0, null, null);
        try {
            bestRota.setState(false);
            this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).write(request.typeMessage());
            this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).flush();
            System.out.println("Sent tcp packet > " + request.toString());
        } catch (IOException e) {
            System.out.println("Something went wrong while sending type 3 message on class RouterWorker...fix it u dumb fuck!");
        }
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
            //Alterar para InetAddress
            Rota bestRota = this.udpCenter.getTabelaEncaminhamento().get(this.udpCenter.getServer()).get(0);
            Header request = new Header(2, header.getFont(), header.getUdpFontPortOrtcpFontPort(), this.udpPort, "/" + this.host.toString().split("/")[1], bestRota.getInterfaceSaida().getVizinho().toString(), header.getJumps() + 1, header.getTimestamp(), header.getId());
            try {
                Thread udpWorker= new Thread(new RouterUDPWorker(this.udpCenter, this.udpPort));
                udpWorker.start();
                this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).write(request.typeMessage());
                this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).flush();
                System.out.println("Sent TCP packet > " + request.toString() + "\n");
                bestRota.setState(true);
            } catch (IOException e) {
                System.out.println("Something went wrong on RouterWorker class reading type 2 message...fix it u dumb fuck!");
                System.out.println(e);
            }
        }
        this.udpCenter.addUDPClient();
        this.udpCenter.getLock().unlock();
    }

    public boolean destroyFlow(Header header, int type){
        this.udpCenter.getLock().lock();
        this.udpCenter.removeUDPClient();
        for(Vizinho vizinho : this.udpCenter.getListaStreams()){
            if(vizinho.getUdpPort() == header.getUdpOriginPortOrtcpOriginPort()){
                this.udpCenter.getListaStreams().remove(vizinho);
                break;
            }
        }

        if(this.udpCenter.getUDPClients() == 0){
            Rota bestRota = this.udpCenter.getTabelaEncaminhamento().get(this.udpCenter.getServer()).get(0);
            Header request = new Header(4, header.getFont(), header.getUdpFontPortOrtcpFontPort(), this.udpPort, "/" + this.host.toString().split("/")[1], bestRota.getInterfaceSaida().getVizinho().toString(), header.getJumps() + 1, header.getTimestamp(), header.getId());

            try {
                this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).write(request.typeMessage());
                this.outputVizinhos.get(bestRota.getInterfaceSaida().getTcpPort()).flush();
                System.out.println("Sent tcp packet > " + request.toString());
            } catch (IOException e) {
                System.out.println("Something went wrong while sending type 3 message on class RouterWorker...fix it u dumb fuck!");
            }
            bestRota.setState(false);
            for(Vizinho v : this.vizinhos){
                // Change here to compare InetAddress!!!!!!!
                if(v.getUdpPort() == header.getUdpFontPortOrtcpFontPort()){
                    System.out.println("Client " + v.getVizinho() + ":" + v.getUdpPort() + " has disconnected\n");
                    if(type == 3) {
                        this.udpCenter.getLock().unlock();
                        return false;
                    }
                }
            }
        }
        else if(this.udpCenter.getUDPClients() > 0){
            for(Vizinho v : this.vizinhos){
                // Change here to compare InetAddress!!!!!!!
                if(v.getUdpPort() == header.getUdpFontPortOrtcpFontPort()){
                    System.out.println("Client " + v.getVizinho() + ":" + v.getUdpPort() + " has disconnected\n");
                    if(type == 3) {
                        this.udpCenter.getLock().unlock();
                        return false;
                    }
                }
            }
        }
        this.udpCenter.getLock().unlock();
        return true;
    }

    public void printTabelaEncaminhamento(){
        System.out.println("|-----------------------------------------------------------------------------------------------------------------|");
        System.out.println("|          Interface de Saída          |   Delay (ms)   |   Saltos   |          Destino          |    Estado      |");
        System.out.println("|-----------------------------------------------------------------------------------------------------------------|");
        this.udpCenter.getLock().lock();
        for(Map.Entry<Integer, ArrayList<Rota>> set : this.udpCenter.getTabelaEncaminhamento().entrySet()){
            for(Rota rota : set.getValue()){
                System.out.println(rota.toString());
                System.out.println("|----------------------------------------------------------------------------------------------------------------|");
            }
        }
        System.out.println("Best server: " + this.udpCenter.getServer());
        this.udpCenter.getLock().unlock();
    }
}
