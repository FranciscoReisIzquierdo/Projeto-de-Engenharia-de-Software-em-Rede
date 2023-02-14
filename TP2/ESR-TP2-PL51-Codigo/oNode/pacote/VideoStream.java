package pacote;

import java.io.*;


/* Classe que representa o video da stream */
public class VideoStream {
    FileInputStream fis;
    int frame_nb;


    /* Construtor parameterizado */
    public VideoStream(String filename) throws Exception{
        fis = new FileInputStream(filename);
        frame_nb = 0;
    }


    /* Método que devolve o próximo frame do video */
    public int getnextframe(byte[] frame) throws Exception{
        int length = 0;
        String length_string;
        byte[] frame_length = new byte[5];
        fis.read(frame_length,0,5);
        length_string = new String(frame_length);
        length = Integer.parseInt(length_string);

        return(fis.read(frame,0,length));
    }
}