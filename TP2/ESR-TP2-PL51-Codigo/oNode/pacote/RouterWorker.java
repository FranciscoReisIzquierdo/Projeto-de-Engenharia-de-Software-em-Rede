package pacote;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.*;
import java.util.Enumeration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/* Classe que trata de todos os pacotes TCP/IP */
public class RouterWorker implements Runnable{
    private static final int MAX_BYTES = 1024;
    private InetAddress host = getLocalHost();
    private int tcpPort;
    private int udpPort;
    private ArrayList<Vizinho> vizinhos;
    private ConcurrentHashMap<InetAddress, DataOutputStream> outputVizinhos = new ConcurrentHashMap<>();
    private Socket connection;
    private DataInputStream input;
    private DataOutputStream output;
    private UDPCenter udpCenter;
    private int token;
    private ArrayList<Integer> tokensVizinhos;


    /* Construtor parameterizado */
    public RouterWorker(ArrayList<Vizinho> vizinhos, ConcurrentHashMap<InetAddress, DataOutputStream> outputVizinhos, Socket connection, UDPCenter udpCenter) throws IOException {
        this.tcpPort = 25000;
        this.udpPort = 50000;
        this.vizinhos = vizinhos;
        this.outputVizinhos = outputVizinhos;
        this.connection = connection;
        this.input = new DataInputStream(new BufferedInputStream(this.connection.getInputStream()));
        this.output = new DataOutputStream(new BufferedOutputStream(this.connection.getOutputStream()));
        this.udpCenter = udpCenter;
        this.token = hash(this.host.toString().split("/")[1].getBytes());
        this.tokensVizinhos = new ArrayList<>();
        for(Vizinho v : this.vizinhos) this.tokensVizinhos.add(hash(v.getVizinho().toString().split("/")[1].getBytes()));
    }


     /* Método que devolve o endereço ip da interface eth0 do host */
    public InetAddress getLocalHost() throws SocketException{

        NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
        Enumeration<InetAddress> inetAddress = networkInterface.getInetAddresses();
        InetAddress currentAddress;
        InetAddress ip = null;
        currentAddress = inetAddress.nextElement();

        while(inetAddress.hasMoreElements()){
            currentAddress = inetAddress.nextElement();
            if(currentAddress instanceof Inet4Address && !currentAddress.isLoopbackAddress()){
                ip = currentAddress;
                break;
            }
        }
        return ip;
     }


    /* Método run da thread que lida com os pacotes TCP/IP consoante o tipo de cabeçalho */
    public void run() {

        boolean running = true;
        while(running) {
            byte[] info = new byte[MAX_BYTES];
            int size;
            Header header = null;
            try {
                size = this.input.read(info);
                this.udpCenter.getLock().lock();
                header = Header.translate(info, size);
                System.out.println("\nPacote TCP recebido > " + header.toString() + "\n");
                
            } catch (Exception e) {
                System.out.println("Algo correu mal a ler o pacote TCP > Pacote vazio!" + e);
            }
            
            switch (header.getType()) {
                case 1:

                    try { buildTabelaEncaminhamento(header); } 
                    
                    catch (UnknownHostException e1) {
                        System.out.println("Algo correu mal ao construir a tabela de encaminhamento!");
                        this.udpCenter.getLock().unlock();
                    }

                    /*if(this.udpCenter.getIdMessages().containsKey(header.getId())){
                        this.udpCenter.getLock().unlock();
                        break;
                    }

                    this.udpCenter.getIdMessages().put(header.getId(), true);*/
                    try {
                        flooding(header);
                        setBestServer();
                        printTabelaEncaminhamento();
                        this.udpCenter.getLock().unlock();
                    } catch (UnknownHostException e) {
                        System.out.println("Algo correu mal ao realizar o flooding do pacote TCP!");
                        this.udpCenter.getLock().unlock();
                    }
                    break;

                case 2:

                    request(header);
                    this.udpCenter.getLock().unlock();
                    break;

                case 3:
                case 4:

                    running = destroyFlow(header, header.getType());
                    this.udpCenter.getLock().unlock();
                    break;

            }
        }

        try {
            this.connection.shutdownInput();
            this.connection.shutdownOutput();
            this.connection.close();
        } catch (IOException e) { System.out.println("Algo correu mal ao fechar a conexão com o vizinho!"); }
    }


    /* Método que verifica se algum vizinho ainda não recebeu a mensagem de floading */
    public boolean checkFloadVizinhoToken(Header header, int tk){
        for(Integer token : header.getTokens()){
            if(token == tk) return true;
        }
        return false;
    }


    /* Método responsável por construir a tabela de encaminhamento do nodo */
    public void buildTabelaEncaminhamento(Header header) throws UnknownHostException{

        long delay = new Date().getTime() - header.getTimestamp().getTime();
        Rota rota = null;
        for (Vizinho vizinho : this.vizinhos) {
            if (vizinho.getVizinho().toString().split("/")[1].equals(header.getHost())) {
                rota = new Rota(vizinho, delay, header.getJumps() + 1, new Destino(header.getFont(), header.getTCPFontPort()));
                break;
            }
        }

        if(!this.udpCenter.getTabelaEncaminhamento().containsKey(InetAddress.getByName(header.getFont()))){
            ArrayList<Rota> rotas = new ArrayList<>();
            rotas.add(rota);
            this.udpCenter.getTabelaEncaminhamento().put(InetAddress.getByName(header.getFont()), rotas);
        }

        else {
            boolean flag = false;
            ArrayList<Rota> listaRotas = this.udpCenter.getTabelaEncaminhamento().get(InetAddress.getByName(header.getFont()));

            for (Rota r : listaRotas) {
                if (r.getInterfaceSaida().getVizinho().toString().split("/")[1].equals(rota.getInterfaceSaida().getVizinho().toString().split("/")[1])) {
                    r.setDelay(delay);
                    flag = true;
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
    }


    /* Método responsável por realizar o flooding controlado do pacote TCP/IP recebido */
    public void flooding(Header header) throws UnknownHostException {

        if(!this.outputVizinhos.containsKey(InetAddress.getByName(header.getHost()))) this.outputVizinhos.put(InetAddress.getByName(header.getHost()), this.output);
        for (Vizinho vizinho : this.vizinhos) {
            if (!vizinho.getVizinho().toString().split("/")[1].equals(header.getHost()) && 
                    !checkFloadVizinhoToken(header, hash(vizinho.getVizinho().toString().split("/")[1].getBytes())) &&
                        !this.udpCenter.getTabelaEncaminhamento().containsKey(vizinho.getVizinho())) {

                if(!this.outputVizinhos.containsKey(vizinho.getVizinho())){
                    try { 
                        Socket connection = new Socket(vizinho.getVizinho(), vizinho.getTcpPort()); 
                        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(connection.getOutputStream()));
                        this.outputVizinhos.put(vizinho.getVizinho(), out);

                        Header floading = new Header(1, header.getFont(), header.getTCPFontPort(), this.tcpPort, this.host.toString().split("/")[1], vizinho.getVizinho().toString(), header.getJumps() + 1, header.getTimestamp(), header.getId());
                        floading.setTokens(header.getTokens());
                        floading.addToken(this.token);
                        
                        this.outputVizinhos.get(vizinho.getVizinho()).write(floading.typeMessage());
                        this.outputVizinhos.get(vizinho.getVizinho()).flush();

                        Thread thread = new Thread(new RouterWorker(this.vizinhos, this.outputVizinhos, connection, this.udpCenter));
                        thread.start();
                    }
                    catch (IOException e) { System.out.println("O vizinho " + vizinho.getVizinho() + ":" + vizinho.getUdpPort() + ":" + vizinho.getTcpPort() + " está offline!"); }
                }
                else{
                    Header floading = new Header(1, header.getFont(), header.getTCPFontPort(), this.tcpPort, this.host.toString().split("/")[1], vizinho.getVizinho().toString(), header.getJumps() + 1, header.getTimestamp(), header.getId());
                        floading.setTokens(header.getTokens());
                        floading.addToken(this.token);
                        
                        try {
                            this.outputVizinhos.get(vizinho.getVizinho()).write(floading.typeMessage());
                            this.outputVizinhos.get(vizinho.getVizinho()).flush();
                        } 
                        catch (IOException e) { System.out.println("O vizinho " + vizinho.getVizinho() + ":" + vizinho.getUdpPort() + ":" + vizinho.getTcpPort() + " está offline!"); }
                }
            }
        }
    }


    /* Método que define o melhor servidor */
    public void setBestServer(){

        long delay = -1;
        int jumps = -1;
        InetAddress server = null;
        for(Map.Entry<InetAddress, ArrayList<Rota>> set : this.udpCenter.getTabelaEncaminhamento().entrySet()) {
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

        if(this.udpCenter.getServer() == null || (server != this.udpCenter.getServer() && this.udpCenter.getUDPClients() == 0)) this.udpCenter.setServer(server);
        
        else if(server != this.udpCenter.getServer() && this.udpCenter.getServer() != null && this.udpCenter.getUDPClients() > 0){
            
            boolean flag = false;
            for(Vizinho v : this.udpCenter.getListaStreams()){
                if(v.getVizinho().toString().split("/")[1].equals(this.udpCenter.getTabelaEncaminhamento().get(server).get(0).getInterfaceSaida().getVizinho().toString().split("/")[1])){
                    this.udpCenter.removeUDPClient();

                    for(Vizinho vizinho : this.udpCenter.getListaStreams()){
                        if(vizinho.getVizinho().toString().split("/")[1].equals(v.getVizinho().toString().split("/")[1])){
                            this.udpCenter.getListaStreams().remove(vizinho);
                            flag = true;
                            break;
                        }
                    }
                    if(flag) break;
                }
            }


            if(!this.udpCenter.getTabelaEncaminhamento().get(server).get(0).getInterfaceSaida().getVizinho().toString().split("/")[1].
            equals(this.udpCenter.getTabelaEncaminhamento().get(this.udpCenter.getServer()).get(0).getInterfaceSaida().getVizinho().toString().split("/")[1])){
            
                try {
                    synchronized (this){ this.wait(10); }
                    changeFlow();
                    this.udpCenter.setServer(server);
                    if(this.udpCenter.getUDPClients() > 0) newFlow();
        
                } catch (InterruptedException exception) {
                    System.out.println("Algo correu mal ao enviar esperar para mudar rota e pedir novo fluxo!");
                }
            }
            else this.udpCenter.setServer(server);
        }
    }


    /* Método que realiza um novo pedido de stream pela nova melhor rota */
    public void newFlow(){

        Rota bestRota = this.udpCenter.getTabelaEncaminhamento().get(this.udpCenter.getServer()).get(0);
        Header request = new Header(2, this.host.toString(), this.udpPort, this.udpPort, this.host.toString().split("/")[1], this.udpCenter.getServer().toString().split("/")[1], 0, null, null);
        
        try {
            this.outputVizinhos.get(bestRota.getInterfaceSaida().getVizinho()).write(request.typeMessage());
            this.outputVizinhos.get(bestRota.getInterfaceSaida().getVizinho()).flush();
            bestRota.setState(true);
        } catch (IOException e) {
            System.out.println("Algo correu mal ao enviar o pacote TCP de pedido de stream ao mudar de rota!");
        }
    }


    /* Método responsável por alterar o fluxo de pacotes UDP da stream para o melhor fluxo */
    public void changeFlow(){

        Rota bestRota = this.udpCenter.getTabelaEncaminhamento().get(this.udpCenter.getServer()).get(0);
        Header request = new Header(4, this.host.toString(), this.udpPort, this.udpPort, this.host.toString().split("/")[1], bestRota.getInterfaceSaida().getVizinho().toString(), 0, null, null);
        try {
            bestRota.setState(false);
            this.outputVizinhos.get(bestRota.getInterfaceSaida().getVizinho()).write(request.typeMessage());
            this.outputVizinhos.get(bestRota.getInterfaceSaida().getVizinho()).flush();
        } catch (IOException e) {
            System.out.println("Algo correu mal ao enviar o pacote TCP para mudar a melhor rota!");
        }
    }


    /* Método responsável por realizar um pedido pela stream */
    public void request(Header header){

        Vizinho v = null;
        for (Vizinho vizinho : this.vizinhos) {
            if (vizinho.getVizinho().toString().split("/")[1].equals(header.getHost())) {
                v = vizinho;
                break;
            }
        }

        this.udpCenter.getListaStreams().add(v);
        if(this.udpCenter.getUDPClients() == 0){

            Rota bestRota = null;
            String dest = null;
            if(this.host.toString().split("/")[1].equals(header.getDest()) || header.getDest().equals(this.udpCenter.getServer().toString().split("/")[1])){
                bestRota = this.udpCenter.getTabelaEncaminhamento().get(this.udpCenter.getServer()).get(0);
                dest = this.udpCenter.getServer().toString().split("/")[1];
            }
            else{ 
                try { 
                    bestRota = this.udpCenter.getTabelaEncaminhamento().get(InetAddress.getByName(header.getDest())).get(0); 
                    dest = header.getDest();
                } 
                catch (UnknownHostException e1) { System.out.print("Host de destino desconhecido!"); }
            }
            
            Header request = new Header(2, header.getFont(), header.getTCPFontPort(), this.udpPort, this.host.toString().split("/")[1], dest, header.getJumps() + 1, header.getTimestamp(), header.getId());
            try {
                Thread udpWorker= new Thread(new RouterUDPWorker(this.udpCenter, this.udpPort));
                udpWorker.start();
                this.outputVizinhos.get(bestRota.getInterfaceSaida().getVizinho()).write(request.typeMessage());
                this.outputVizinhos.get(bestRota.getInterfaceSaida().getVizinho()).flush();
                bestRota.setState(true);
            } 
            catch (IOException e) {
                System.out.println("Algo correu mal ao enviar o pacote TCP de pedido por stream!");
                System.out.println(e);
            }
        }
        this.udpCenter.addUDPClient();
    }


    /* Método responsável por destruir um fluxo de pacotes UDP da stream */
    public boolean destroyFlow(Header header, int type){

        if(type == 4){
            boolean flag = false;
            for(Vizinho vizinho : this.udpCenter.getListaStreams()){
                if(vizinho.getVizinho().toString().split("/")[1].equals(header.getHost())){
                    flag = true;
                    break;
                }
            }
            if(!flag) return true;
        }

        if(this.udpCenter.getUDPClients() > 0){ 
            this.udpCenter.removeUDPClient();
            for(Vizinho vizinho : this.udpCenter.getListaStreams()){
                if(vizinho.getVizinho().toString().split("/")[1].equals(header.getHost())){
                    this.udpCenter.getListaStreams().remove(vizinho);
                    break;
                }
            }

            if(this.udpCenter.getUDPClients() == 0){
                Rota bestRota = this.udpCenter.getTabelaEncaminhamento().get(this.udpCenter.getServer()).get(0);
                Header request = new Header(4, header.getFont(), header.getTCPFontPort(), this.udpPort, this.host.toString().split("/")[1], bestRota.getInterfaceSaida().getVizinho().toString(), header.getJumps() + 1, header.getTimestamp(), header.getId());

                try {
                    this.outputVizinhos.get(bestRota.getInterfaceSaida().getVizinho()).write(request.typeMessage());
                    this.outputVizinhos.get(bestRota.getInterfaceSaida().getVizinho()).flush();

                } catch (IOException e) {
                    System.out.println("Algo correu mal ao enviar o pacote TCP para destruir o fluxo de stream!");
                }
                bestRota.setState(false);
                for(Vizinho v : this.vizinhos){
                    if(v.getVizinho().toString().split("/")[1].equals(header.getFont().split("/")[1])){
                        if(type == 3) {
                            System.out.println("Client " + v.getVizinho() + ":" + v.getUdpPort() + " has disconnected!\n");
                            return false;
                        }
                    }
                }
            }
            else if(this.udpCenter.getUDPClients() > 0){
                for(Vizinho v : this.vizinhos){
                    if(v.getVizinho().toString().split("/")[1].equals(header.getFont().split("/")[1])){
                        if(type == 3) {
                            System.out.println("Client " + v.getVizinho() + ":" + v.getUdpPort() + " has disconnected!\n");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }


    /* Método que imprime a tabela de encaminhamento do nodo */
    public void printTabelaEncaminhamento(){

        TableGenerator tableGenerator = new TableGenerator();

        List<String> headersList = new ArrayList<>(); 
        headersList.add("Interface de Saída");
        headersList.add("Atraso (ms)");
        headersList.add("Saltos");
        headersList.add("Servidor");
        headersList.add("Estado");

        List<List<String>> rowsList = new ArrayList<>();
        for(Map.Entry<InetAddress, ArrayList<Rota>> set : this.udpCenter.getTabelaEncaminhamento().entrySet()){
            int index = 0;
            for(Rota rota : set.getValue()){
                List<String> row = new ArrayList<>();
                if(this.udpCenter.getServer().toString().split("/")[1].equals(set.getKey().toString().split("/")[1]) && index == 0) row.add("*" + rota.getInterfaceSaidaString());
                else row.add(rota.getInterfaceSaidaString());
                row.add(String.valueOf(rota.getDelay()));
                row.add(String.valueOf(rota.getJumps()));

                if(this.udpCenter.getServer().toString().split("/")[1].equals(set.getKey().toString().split("/")[1])) row.add("*" + set.getKey().toString().split("/")[1]);
                else row.add(set.getKey().toString().split("/")[1]);
                row.add(rota.getState());
                rowsList.add(row);
                index++;
            }
        }
        System.out.print(tableGenerator.generateTable(headersList, rowsList));
        String builder = null;
        builder = "+------------------------------+\n";
        int length = builder.length() - 3;
        builder += "| * -> Melhor rota/servidor    |\n";
        String tokenString = "| Token = " + this.token;
        int lengthToken = tokenString.length() - 3;
        builder += tokenString;
        for(int i = 0; i< length - lengthToken - 2; i++) builder+= " ";
        
        builder += "|\n+------------------------------+\n";
        System.out.print(builder);
    }


    /* Método que cria o token do nodo */
    public static int hash(byte[] str) {
        int hash = 0;
        for (int i = 0; i < str.length && str[i]!= '\0'; i++) {
            hash = str[i] + ((hash << 5) - hash);
        }
        return hash;
    }


/* Classe de suporte a tabela de encaminhamento */
public class TableGenerator {

    private int PADDING_SIZE = 2;
    private String NEW_LINE = "\n";
    private String TABLE_JOINT_SYMBOL = "+";
    private String TABLE_V_SPLIT_SYMBOL = "|";
    private String TABLE_H_SPLIT_SYMBOL = "-";

    public String generateTable(List<String> headersList, List<List<String>> rowsList,int... overRiddenHeaderHeight){
        StringBuilder stringBuilder = new StringBuilder();

        int rowHeight = overRiddenHeaderHeight.length > 0 ? overRiddenHeaderHeight[0] : 1; 

        Map<Integer,Integer> columnMaxWidthMapping = getMaximumWidhtofTable(headersList, rowsList);

        stringBuilder.append(NEW_LINE);
        stringBuilder.append(NEW_LINE);
        createRowLine(stringBuilder, headersList.size(), columnMaxWidthMapping);
        stringBuilder.append(NEW_LINE);


        for (int headerIndex = 0; headerIndex < headersList.size(); headerIndex++) {
            fillCell(stringBuilder, headersList.get(headerIndex), headerIndex, columnMaxWidthMapping);
        }

        stringBuilder.append(NEW_LINE);

        createRowLine(stringBuilder, headersList.size(), columnMaxWidthMapping);


        for (List<String> row : rowsList) {

            for (int i = 0; i < rowHeight; i++) {
                stringBuilder.append(NEW_LINE);
            }

            for (int cellIndex = 0; cellIndex < row.size(); cellIndex++) {
                fillCell(stringBuilder, row.get(cellIndex), cellIndex, columnMaxWidthMapping);
            }

        }

        stringBuilder.append(NEW_LINE);
        createRowLine(stringBuilder, headersList.size(), columnMaxWidthMapping);
        stringBuilder.append(NEW_LINE);
        stringBuilder.append(NEW_LINE);

        return stringBuilder.toString();
    }

    private void fillSpace(StringBuilder stringBuilder, int length)
    {
        for (int i = 0; i < length; i++) {
            stringBuilder.append(" ");
        }
    }

    private void createRowLine(StringBuilder stringBuilder,int headersListSize, Map<Integer,Integer> columnMaxWidthMapping)
    {
        for (int i = 0; i < headersListSize; i++) {
            if(i == 0)
            {
                stringBuilder.append(TABLE_JOINT_SYMBOL);   
            }

            for (int j = 0; j < columnMaxWidthMapping.get(i) + PADDING_SIZE * 2 ; j++) {
                stringBuilder.append(TABLE_H_SPLIT_SYMBOL);
            }
            stringBuilder.append(TABLE_JOINT_SYMBOL);
        }
    }


    private Map<Integer,Integer> getMaximumWidhtofTable(List<String> headersList, List<List<String>> rowsList)
    {
        Map<Integer,Integer> columnMaxWidthMapping = new HashMap<>();

        for (int columnIndex = 0; columnIndex < headersList.size(); columnIndex++) {
            columnMaxWidthMapping.put(columnIndex, 0);
        }

        for (int columnIndex = 0; columnIndex < headersList.size(); columnIndex++) {

            if(headersList.get(columnIndex).length() > columnMaxWidthMapping.get(columnIndex))
            {
                columnMaxWidthMapping.put(columnIndex, headersList.get(columnIndex).length());
            }
        }


        for (List<String> row : rowsList) {

            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {

                if(row.get(columnIndex).length() > columnMaxWidthMapping.get(columnIndex))
                {
                    columnMaxWidthMapping.put(columnIndex, row.get(columnIndex).length());
                }
            }
        }

        for (int columnIndex = 0; columnIndex < headersList.size(); columnIndex++) {

            if(columnMaxWidthMapping.get(columnIndex) % 2 != 0)
            {
                columnMaxWidthMapping.put(columnIndex, columnMaxWidthMapping.get(columnIndex) + 1);
            }
        }


        return columnMaxWidthMapping;
    }

    private int getOptimumCellPadding(int cellIndex,int datalength,Map<Integer,Integer> columnMaxWidthMapping,int cellPaddingSize)
    {
        if(datalength % 2 != 0)
        {
            datalength++;
        }

        if(datalength < columnMaxWidthMapping.get(cellIndex))
        {
            cellPaddingSize = cellPaddingSize + (columnMaxWidthMapping.get(cellIndex) - datalength) / 2;
        }

        return cellPaddingSize;
    }

    private void fillCell(StringBuilder stringBuilder,String cell,int cellIndex,Map<Integer,Integer> columnMaxWidthMapping)
    {

        int cellPaddingSize = getOptimumCellPadding(cellIndex, cell.length(), columnMaxWidthMapping, PADDING_SIZE);

        if(cellIndex == 0)
        {
            stringBuilder.append(TABLE_V_SPLIT_SYMBOL); 
        }

        fillSpace(stringBuilder, cellPaddingSize);
        stringBuilder.append(cell);
        if(cell.length() % 2 != 0)
        {
            stringBuilder.append(" ");
        }

        fillSpace(stringBuilder, cellPaddingSize);

        stringBuilder.append(TABLE_V_SPLIT_SYMBOL); 

    }
}
}