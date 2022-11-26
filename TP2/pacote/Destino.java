package pacote;

class Destino{
    String destino;
    int tcpPort;

    public Destino(String destino, int tcpPort){
        this.destino = destino;
        this.tcpPort = tcpPort;
    }

    @Override
    public String toString() {
        return destino;
    }
}
