package pacote;

class Rota{
    Vizinho interfaceSaida;
    long delay;
    int jumps;
    Destino destino;
    boolean state;

    public Rota(Vizinho interfaceSaida, long delay, int jumps, Destino destino){
        this.interfaceSaida = interfaceSaida;
        this.delay = delay;
        this.jumps = jumps;
        this.destino = destino;
        this.state = false;
    }


    public Vizinho getInterfaceSaida() { return interfaceSaida; }

    public long getDelay() { return delay; }

    public int getJumps() { return jumps; }

    public void setState(boolean state) { this.state = state; }

    public void setDelay(long delay) { this.delay = delay; }

    @Override
    public String toString() {
        return "|      " + interfaceSaida.toString() +
                "     |      " + delay +
                "   |     " + jumps +
                "      | " + destino +
                "   |    " + state +
                "   |";
    }
}
