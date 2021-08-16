package pt.isec.Server.Threads;

import pt.isec.Server.Server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UserFileThread extends Thread {
    private File file = null;
    private Server server;
    private Socket socket;
    private String filePath = " ";
    private boolean end = false;
    private boolean send = false;

    public UserFileThread(Server server, Socket s) {
        this.server = server;
        socket = s;
    }

    public void closeThread() {
        end = true;
    }

    public void setSend(boolean b) {
        this.send = b;
    }

    public void setFilePath(boolean send, String fp) {
        this.send = send;
        this.filePath = fp;
    }

    @Override
    public void run() {
        try {
            while(!end) {
                synchronized (this) {
                    wait();
                }
            }

            if(send) {
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
            else {
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
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
