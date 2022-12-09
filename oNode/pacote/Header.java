package pacote;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;


/* Classe que representa o cabeçalho com a informação do pacote TCP/IP */
class Header{
    private String id;
    private int type;
    private String font;
    private int tcpPortFont;
    private String host;
    private int tcpPortOrigin;
    private String dest;
    private int jumps;
    private Timestamp timestamp;
    private ArrayList<Integer> tokens;
    //private String client;


    /* Construtor parameterizado */
    public Header(int type, String font, int tcpPortFont, int tcpPortOrigin, String host, String dest, int jumps, Timestamp timestamp, String id){
        this.type = type;
        this.font = font;
        this.tcpPortFont = tcpPortFont;
        this.host = host;
        this.tcpPortOrigin = tcpPortOrigin;
        this.dest = dest;
        this.jumps = jumps;
        this.timestamp = timestamp;
        this.id = id;
        this.tokens = new ArrayList<>();
    }


    /* Método que devolve o tipo do cabeçalho */
    public int getType() { return type; }


    /* Método que devolve o endereço ip de quem reencaminhou o pacote TCP/IP */
    public String getHost() { 
        String result[] = this.host.split("/");
        return result.length > 1 ? result[1] : result[0]; 
    }


    /* Método que devolve o número de saltos que o pacote TCP/IP efetuou */
    public int getJumps() { return jumps; }


    /* Método que devolve o Timestamp do pacote TCP/IP */
    public Timestamp getTimestamp() { return timestamp; }


    /* Método que devolve o endereço ip de quem originou o pacote TCP/IP */
    public String getFont() { return font; }


    /* Método que devolve o número da porta TCP/IP de quem originou o pacote TCP/IP */
    public int getTCPFontPort() { return tcpPortFont; }


    /* Método que devolve o número da porta TCP/IP de quem reencminhou o pacote TCP/IP */
    public int getTCPOriginPort() { return tcpPortOrigin; }


    /* Método que retorna o id do pacote TCP/IP */
    public String getId() { return id; }


    /* Método que retorna o ip de destino do pacote TCP/IP */
    public String getDest() { 
        String result[] = this.dest.split("/");
        return result.length > 1 ? result[1] : result[0]; 
       }


    /* Método que devolve a lista de tokens do pacote TCP/IP */
    public ArrayList<Integer> getTokens(){ return tokens; }


    /* Método que devolve o cliente final a ser servido pela stream */
    //public String getCliente(){ return client; }


    /* Método que define a lista de tokens do pacote TCP/IP */
    public void setTokens(ArrayList<Integer> tokens){ for(Integer token : tokens) this.tokens.add(token); }


    /* Método que define o cliente final a servir a stream */
    //public void setClient(String cliente){ this.client = cliente; }


    /* Método responsável por originar o cabeçalho do pacote TCP/IP com toda a informação necessária */
    public byte[] typeMessage() throws IOException {
        byte []header = null;
        if(this.timestamp == null) this.timestamp = new Timestamp(new Date().getTime());
        if(this.id == null) this.id = timestamp.toString() + font;
        
        String info = this.type + ";" + this.host + ":" + this.tcpPortOrigin + ";" + this.dest + ";" + this.jumps + ";" + this.timestamp + ";" + this.font + ":" + this.tcpPortFont + ";" + this.id;

        for(Integer token : this.tokens) info = info + ";" + token;

        //if(this.type == 2){ info = info + "!" + this.client; }

        header = info.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(header);
        return outputStream.toByteArray();
    }


    /* Método estático responsável por traduzir o array de bytes do pacote TCP/IP que diz respeito ao cabeçalho e originar o cabeçalho humanamente legível */
    public static Header translate(byte []message, int size) throws UnknownHostException {
        byte []data;
        data = Arrays.copyOf(message, size);
        String convert = new String(data);
        String []header = convert.split(";");
        int type = Integer.parseInt(header[0]);

        String host = header[1].split(":")[0];
        int tcpPortOrigin = Integer.parseInt(header[1].split(":")[1]);
        String dest = header[2];
        int jumps = Integer.parseInt(header[3]);
        Timestamp timestamp = Timestamp.valueOf(header[4]);
        String font = header[5].split(":")[0];
        int tcpPortFont = Integer.parseInt(header[5].split(":")[1]);
        String id = header[6];


        if(type == 1){
            ArrayList<Integer> tokens = new ArrayList<>();
            for(int i = 7; i < header.length; i++) tokens.add(Integer.parseInt(header[i]));
            Header pacote = new Header(type, font, tcpPortFont, tcpPortOrigin, host, dest, jumps, timestamp, id);
            pacote.setTokens(tokens);
            return pacote;
        }
        return new Header(type, font, tcpPortFont, tcpPortOrigin, host, dest, jumps, timestamp, id);
    }


    /* Método que verifica se a mensagem já passou pelo host */
    public void addToken(int token){
        this.tokens.add(token);
    }


    /* Método que verifica se o pacote TCP já passou pelo host */
    public boolean checkToken(int token){
        for(Integer tk : this.tokens) if(tk == token) return false;
        return true;
    }


    
    /* Método toString da classe */
    public String toString() {
        String aux[] = this.dest.split("/");
        String destino = aux.length > 1 ? aux[1] : aux[0];

        String info = "Header{" +
                "type=" + type +
                ", font=" + font +
                ", #tcpPort=" + tcpPortFont +
                ", host=" + host +
                ", #tcpPort=" + tcpPortOrigin +
                ", dest=" + destino +
                ", jumps=" + jumps +
                ", timestamp=" + timestamp +
                ", id=" + id +
                '}';
        
                info = info + " Tokens=[";
                if(tokens.size() == 0) info = info + "]";
        for(int i = 0; i< tokens.size(); i++){
            if(i == 0 && i!= tokens.size() - 1) info = info + tokens.get(i) + "; ";
            else if(i == 0 && i== tokens.size() - 1) info = info + tokens.get(i) + " ]";
            else if(i == tokens.size() - 1) info = info + tokens.get(i) + " ]";
            else info = info + tokens.get(i) + "; ";
        }

        return info;
    }
}

