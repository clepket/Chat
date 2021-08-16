package pt.isec.User.Threads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class FileSenderThread extends Thread {
    private String filePath;
    private Socket socket;

    public FileSenderThread(String fp, Socket s) {
        filePath = fp;
        socket = s;
    }

    @Override
    public void run() {
        try {
            OutputStream os = socket.getOutputStream();
            FileInputStream fis = new FileInputStream(new File(filePath));
            byte fileBuffer[] = new byte[5000];
            int nBytes = 0;

            while(fis.available() != 0) {
                nBytes = fis.read(fileBuffer);

                os.write(fileBuffer,0,nBytes);
                os.flush();
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
