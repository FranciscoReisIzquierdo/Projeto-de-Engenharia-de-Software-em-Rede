package pacote;


/* Classe que representa cada destino */
class Destino{
    String destino;
    int tcpPort;


    /* Construtor parameterizado */
    public Destino(String destino, int tcpPort){
        this.destino = destino;
        this.tcpPort = tcpPort;
    }


    /* Método toString da classe */
    public String toString() { return destino; }
}
