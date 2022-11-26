package pacote;


import java.io.IOException;
import java.util.*;

public class MenuUI {
    private Scanner sc;

    public MenuUI() {
        this.sc = new Scanner(System.in);
    }

    private void menu() throws IOException, InterruptedException {
        for (String str : new String[] { "1-> Modo cliente;", "2-> Modo router;", "3-> Modo servidor;", "4-> Quit;" })
            System.out.println(str);
        int opcao = Integer.parseInt(sc.next());

        switch(opcao){
            case 1:
                //System.out.println("Escreva o <#portaUDP_de_origem>");
                //int udpOriginPort = Integer.parseInt(sc.next());
                System.out.println("Escreva o <ip_vizinho>:");
                String destino = sc.next();
                //String info[] = sc.next().split(":");
                //String destino = info[0];
                //int tcpDestPort = Integer.parseInt(info[1]);
                Cliente cliente = new Cliente(destino);
                System.out.println("Client started");
                cliente.run();
                break;

            case 2:
                //System.out.println("Escreva o <#portaTCP_de_origem>");
                //int tcpPortRouter = Integer.parseInt(sc.next());
                //System.out.println("Escreva o <#portaUDP_de_origem>");
                //int udpPortRouter = Integer.parseInt(sc.next());

                System.out.println("Escreva para cada vizinho: <ip_vizinho>;");
                String []vizinhosRouter = sc.next().split(";");
                ArrayList<Vizinho> listaVizinhosRouter = new ArrayList<>();

                for(String vizinho: vizinhosRouter){
                    //String []information = vizinho.split(":");
                    Vizinho v = new Vizinho(vizinho);
                    listaVizinhosRouter.add(v);
                }
                Router router = new Router(listaVizinhosRouter);
                System.out.println("Router started");
                router.run();
                break;
            case 3:
                //System.out.println("Escreva o <#portaTCP_de_origem>");
                //int tcpPortServer = Integer.parseInt(sc.next());
                System.out.println("Escreva cada vizinho: <ip_vizinho>;");
                String []vizinhosServer = sc.next().split(";");
                ArrayList<Vizinho> listaVizinhosServer = new ArrayList<>();

                for(String vizinho: vizinhosServer){
                    //String []information = vizinho.split(":");
                    Vizinho v = new Vizinho(vizinho);
                    listaVizinhosServer.add(v);
                }
                Servidor servidor = new Servidor(listaVizinhosServer);
                System.out.println("Server started");
                servidor.run();
                break;
            case 4:
                break;
            default:
                System.out.println("Opção desconhecida");
                menu();
        }

    }

    public void run() throws IOException, InterruptedException {
        System.out.println("Bem-vindo à oNode! Escolha um modo de operação:");
        menu();
        System.out.println("Até breve...");
    }
}