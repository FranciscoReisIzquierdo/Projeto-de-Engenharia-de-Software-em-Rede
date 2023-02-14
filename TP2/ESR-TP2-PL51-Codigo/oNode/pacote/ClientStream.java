package pacote;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;


/* Classe responsável por entregar pacotes UDP da stream ao cliente */
public class ClientStream{
    private int RTP_RCV_PORT;
    private DatagramSocket RTPsocket;
    JFrame f = new JFrame("Cliente de Testes");
    JButton setupButton = new JButton("Setup");
    JButton playButton = new JButton("Play");
    JButton pauseButton = new JButton("Pause");
    JButton tearButton = new JButton("Teardown");
    JPanel mainPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JLabel iconLabel = new JLabel();
    ImageIcon icon;
    DatagramPacket rcvdp;
    Timer cTimer;
    byte[] cBuf;
    InetAddress host;
    Socket client;
    DataOutputStream output;
    InetAddress dest;
    int destPort;


    /* Construtor parameterizado */
    public ClientStream(Socket client, DataOutputStream output, InetAddress host, int UDP_origin_Port, InetAddress dest, int destPort){
        this.RTP_RCV_PORT = UDP_origin_Port;
        this.host = host;
        this.client = client;
        this.output = output;
        this.dest = dest;
        this.destPort = destPort;
        f.addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { System.exit(0); }});

        buttonPanel.setLayout(new GridLayout(1,0));
        buttonPanel.add(setupButton);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(tearButton);

        playButton.addActionListener(new playButtonListener());
        tearButton.addActionListener(new tearButtonListener());

        iconLabel.setIcon(null);

        mainPanel.setLayout(null);
        mainPanel.add(iconLabel);
        mainPanel.add(buttonPanel);
        iconLabel.setBounds(0,0,380,280);
        buttonPanel.setBounds(0,280,380,50);

        f.getContentPane().add(mainPanel, BorderLayout.CENTER);
        f.setSize(new Dimension(390,370));
        f.setVisible(true);

        cTimer = new Timer(20, new clientTimerListener());
        cTimer.setInitialDelay(0);
        cTimer.setCoalesce(true);
        cBuf = new byte[15000];

        try {
            RTPsocket = new DatagramSocket(RTP_RCV_PORT);
            RTPsocket.setSoTimeout(5000);
        } catch (SocketException e) {
            System.out.println("Cliente: erro no socket: " + e.getMessage());
        }
    }


    /* Handler para o botão Play */
    class playButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){
            System.out.println("Play Button pressed !");
            cTimer.start();
        }
    }


    /* Handler para o botão Teardown */
    class tearButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e){

            System.out.println("Teardown Button pressed !");
            //stop the timer
            cTimer.stop();
            try {
                Header header = new Header(3, host.toString(), RTP_RCV_PORT, RTP_RCV_PORT, host.toString(), dest.toString(), 0, null, null);
                output.write(header.typeMessage());
                output.flush();
                client.close();
                System.out.println("Até breve! Esperamos que tenha disfrutado dos serviços de streamming da oNode!");
                System.exit(0);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    /* Handler para o timer */
    class clientTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            rcvdp = new DatagramPacket(cBuf, cBuf.length);
            try{
                RTPsocket.receive(rcvdp);
                RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
                rtp_packet.printheader();

                int payload_length = rtp_packet.getpayload_length();
                byte [] payload = new byte[payload_length];
                rtp_packet.getpayload(payload);

                Toolkit toolkit = Toolkit.getDefaultToolkit();
                Image image = toolkit.createImage(payload, 0, payload_length);

                icon = new ImageIcon(image);
                iconLabel.setIcon(icon);
            }
            catch (InterruptedIOException iioe){ System.out.println("Nothing to read"); }
            catch (IOException ioe) { System.out.println("Exception caught: "+ioe); }
        }
    }
}