package pacote;


import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;

class Header{
    private String id;
    private int type;
    private String font;
    private int udpFontPortOrtcpFontPort;
    private String host;
    private int udpOriginPortOrtcpOriginPort;
    private String dest;
    private int jumps;
    private Timestamp timestamp;

    public Header(int type, String font, int udpFontPortOrtcpFontPort, int udpOriginPortOrtcpOriginPort, String host, String dest, int jumps, Timestamp timestamp, String id){
        this.type = type;
        this.font = font;
        this.udpFontPortOrtcpFontPort = udpFontPortOrtcpFontPort;
        this.host = host;
        this.udpOriginPortOrtcpOriginPort = udpOriginPortOrtcpOriginPort;
        this.dest = dest;
        this.jumps = jumps;
        this.timestamp = timestamp;
        this.id = id;
    }

    public int getType() { return type; }

    public String getHost() { 
        String result[] = this.host.split("/");
        return result.length > 1 ? result[1] : result[0]; 
    }

    public int getJumps() { return jumps; }

    public Timestamp getTimestamp() { return timestamp; }

    public String getFont() { return font; }

    public int getUdpFontPortOrtcpFontPort() { return udpFontPortOrtcpFontPort; }

    public int getUdpOriginPortOrtcpOriginPort() { return udpOriginPortOrtcpOriginPort; }

    public String getId() { return id; }

    public byte[] typeMessage() throws IOException {
        byte []header = null;
        switch (this.type){
            case 1:
            case 2:
            case 3:
            case 4:
                if(this.timestamp == null) this.timestamp = new Timestamp(new Date().getTime());
                if(this.id == null) this.id = timestamp.toString() + font;
                header = (this.type + ";" + this.host + ":" + this.udpOriginPortOrtcpOriginPort + ";" + this.dest + ";" + this.jumps + ";" + this.timestamp + ";" + this.font + ":" + this.udpFontPortOrtcpFontPort + ";" + this.id).getBytes(StandardCharsets.UTF_8);
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

        String host = header[1].split(":")[0];
        int udpOriginPortOrtcpOriginPort = Integer.parseInt(header[1].split(":")[1]);
        String dest = header[2];
        int jumps = Integer.parseInt(header[3]);
        Timestamp timestamp = Timestamp.valueOf(header[4]);
        String font = header[5].split(":")[0];
        int udpFontPortOrtcpFontPort = Integer.parseInt(header[5].split(":")[1]);
        String id = header[6];
        return new Header(type, font, udpFontPortOrtcpFontPort, udpOriginPortOrtcpOriginPort, host, dest, jumps, timestamp, id);
    }

    @Override
    public String toString() {
        return "Header{" +
                "type=" + type +
                ", font=" + font +
                ", #tcpPort=" + udpFontPortOrtcpFontPort +
                ", host=" + host +
                ", #tcpPort=" + udpOriginPortOrtcpOriginPort +
                ", dest=" + dest.toString().split("/")[1] +
                ", jumps=" + jumps +
                ", timestamp=" + timestamp +
                ", id=" + id +
                '}';
    }
}

