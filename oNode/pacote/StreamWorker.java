package pacote;

import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;


/* Classe responsável por criar os pacote UDP para criar a stream */
public class StreamWorker extends JFrame implements ActionListener{
    JLabel label;
    DatagramPacket senddp; 
    DatagramSocket RTPsocket;
    InetAddress ClientIPAddr;
    ArrayList<Vizinho> UDPclients = new ArrayList<>();
    static String VideoFileName;
    int imagenb = 0;
    VideoStream video;
    static int MJPEG_TYPE = 26;
    static int FRAME_PERIOD = 100;
    static int VIDEO_LENGTH = 500;
    Timer sTimer;
    byte[] sBuf;


    /* Construtor parameterizado */
    public StreamWorker(ArrayList<Vizinho> vizinho) {
        super("StreamWorker");

        sTimer = new Timer(FRAME_PERIOD, this);
        sTimer.setInitialDelay(0);
        sTimer.setCoalesce(true);
        sBuf = new byte[15000];
        this.UDPclients = vizinho;

        try {
            RTPsocket = new DatagramSocket();
            ClientIPAddr = InetAddress.getByName("127.0.0.1");

            video = new VideoStream(VideoFileName);

        } 
        catch (SocketException e) { System.out.println("Servidor: erro no socket: " + e.getMessage()); } 
        catch (Exception e) { System.out.println("Servidor: erro no video: " + e.getMessage()); }
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                sTimer.stop();
                System.exit(0);
            }});

        label = new JLabel("Send frame #        ", JLabel.CENTER);
        getContentPane().add(label, BorderLayout.CENTER);
        sTimer.start();
    }

    
    /* Handler para o tempo */
    public void actionPerformed(ActionEvent e) {

        if (imagenb < VIDEO_LENGTH){
            imagenb++;
            try {
                int image_length = video.getnextframe(sBuf);
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD, sBuf, image_length);
                int packet_length = rtp_packet.getlength();
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);

                for(Vizinho vizinho: this.UDPclients){
                    senddp = new DatagramPacket(packet_bits, packet_length, vizinho.getVizinho(), 50000);
                    RTPsocket.send(senddp);
                }

                rtp_packet.printheader();
            }
            catch(Exception ex){
                System.out.println("Exception caught: "+ex);
                System.exit(0);
            }
        }
        else{
            sTimer.stop();
            new StreamWorker(this.UDPclients);
        }
    }
}