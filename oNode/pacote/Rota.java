package pacote;


/* Classe que representa uma rota */
class Rota{
    Vizinho interfaceSaida;
    long delay;
    int jumps;
    Destino destino;
    boolean state;


    /* Construtor parameterizado */
    public Rota(Vizinho interfaceSaida, long delay, int jumps, Destino destino){
        this.interfaceSaida = interfaceSaida;
        this.delay = delay;
        this.jumps = jumps;
        this.destino = destino;
        this.state = false;
    }


    /* Método que devolve o ip da interface de saída da rota */
    public Vizinho getInterfaceSaida() { return interfaceSaida; }


    /* Método que devolve o atraso da rota */
    public long getDelay() { return delay; }


    /* Método que devolve o número de saltos da rota */
    public int getJumps() { return jumps; }


    /* Método que devolve o estado da rota */
    public String getState(){ return state ? "Ativa" : "Inativa"; }


    /** Método que devolve o destino */
    public String getDestino(){ return destino.toString(); }


    /* Método que devolve a interface de saída em modo string */
    public String getInterfaceSaidaString(){ 
        String info[] = interfaceSaida.toString().split("/");
        return info.length > 1 ? info[1] : info[0];
    }
    

    /* Método que define o estado da rota */
    public void setState(boolean state) { this.state = state; }


    /* Método que define o atraso da rota */
    public void setDelay(long delay) { this.delay = delay; }

    
    /* Método toString da classe */
    public String toString() {
        return "|      " + interfaceSaida.toString() +
                "     |      " + delay +
                "   |     " + jumps +
                "      | " + destino +
                "   |    " + state +
                "   |";
    }
}
