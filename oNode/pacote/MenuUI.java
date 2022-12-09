package pacote;

import java.io.IOException;
import java.util.*;


/* Classe que representa o Menu de interação em cada nodo da topologia */
public class MenuUI {
    private Scanner sc;


    /* Construtor */
    public MenuUI() { this.sc = new Scanner(System.in); }

    /* Método que inicializa o UI do menu */
    private void menu() throws IOException, InterruptedException {
        for (String str : new String[] { "1-> Modo cliente;", "2-> Modo router;", "3-> Modo servidor;", "4-> Quit;" }) System.out.println(str);
        
        System.out.println("Digite o número do modo da opção desejada:");
        int opcao = -1;
        while(true){
            try{
                opcao = Integer.parseInt(sc.next());
                break;
            } catch(Exception e){ System.out.println("Formato de input inválido! Digite o número do modo da opção desejada:"); }
        }

        boolean validate = false;
        switch(opcao){
            case 1:

                String destino = null;
                while(!validate){
                    System.out.println("Escreva o endereço ip do nodo vizinho:");
                    destino = sc.next();
                    validate = checkInput(destino);
                    if(!validate) System.out.println("Formato de ip inválido!");
                }
                Cliente cliente = new Cliente(destino);
                System.out.println("Client started!");
                cliente.run();
                break;

            case 2:
                
            String []vizinhosRouter = null;
                while(!validate){
                    System.out.println("Escreva para cada vizinho: <ip_vizinho>;");
                    vizinhosRouter = sc.next().split(";");
                    int index = 1;
                    for(String ip: vizinhosRouter){
                        if(!checkInput(ip)){
                            vizinhosRouter = null;
                            validate = false;
                            System.out.println("Formato de ip do vizinho " + index + " inválido!");
                            break;
                        }
                        else validate = true;
                        index++;
                    }
                }

                ArrayList<Vizinho> listaVizinhosRouter = new ArrayList<>();
                for(String vizinho: vizinhosRouter){
                    Vizinho v = new Vizinho(vizinho);
                    listaVizinhosRouter.add(v);
                }
                Router router = new Router(listaVizinhosRouter);
                System.out.println("Router started");
                router.run();
                break;

            case 3:
                
                String []vizinhosServer = null;
                while(!validate){
                    System.out.println("Escreva para cada vizinho: <ip_vizinho>;");
                    vizinhosServer = sc.next().split(";");
                    int index = 1;
                    for(String ip: vizinhosServer){
                        if(!checkInput(ip)){
                            vizinhosServer = null;
                            validate = false;
                            System.out.println("Formato de ip do vizinho " + index + " inválido!");
                            break;
                        }
                        else validate = true;
                        index++;
                    }
                }
                
                ArrayList<Vizinho> listaVizinhosServer = new ArrayList<>();
                for(String vizinho: vizinhosServer){
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


    /* Método que verifica a integridade do input do nodo */
    boolean checkInput(String destino){
        String digits[] = destino.split(".");
        for(String digito : digits){
            if((digito == null || !digito.matches("[0-9]+"))) return false;
        }
        return true;
    }

    /* Método que invoca o menu */
    public void run() throws IOException, InterruptedException {
        System.out.println("Bem-vindo à oNode! Escolha um modo de operação:");
        menu();
        System.out.println("Até breve...");
    }
}