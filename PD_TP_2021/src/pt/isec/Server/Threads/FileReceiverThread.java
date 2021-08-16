package pt.isec.Server.Threads;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileReceiverThread extends Thread {
    private String filePath;
    private Socket socket;

    public FileReceiverThread(String fp, Socket s) {
        filePath = fp;
        socket = s;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();

            Path p = Paths.get(filePath);
            String fileName = p.getFileName().toString();

            FileOutputStream fos = new FileOutputStream(fileName);
            byte fileBuffer[] = new byte[5000];
            int nBytes = 0;

            while(true) {
                nBytes = is.read(fileBuffer);

                if(nBytes == -1) {
                    break;
                }

                fos.write(fileBuffer);
                fos.flush();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
