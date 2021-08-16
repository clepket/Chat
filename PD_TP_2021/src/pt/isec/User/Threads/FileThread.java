package pt.isec.User.Threads;

import pt.isec.Components.Notification;
import pt.isec.Components.UserProfile;
import pt.isec.Const;
import pt.isec.User.User;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileThread extends Thread {
    private final User user;
    private final Socket file;
    private String filePath = " ";
    boolean end = false;
    boolean chat = false;
    boolean privateChat = false;

    public FileThread(User user, Socket s) {
        this.user = user;
        this.file = s;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void closeThread() {
        end = true;
        chat = false;
        privateChat = false;
    }

    public void startThread() {
        this.chat = true;
    }

    public void starChatThread() {
        this.privateChat = true;
    }

    public void leaveChat() {
        this.chat = false;
    }

    public void leavePrivateChat() {
        this.privateChat = false;
    }

    @Override
    public void run() {
        try {
            InputStream is = file.getInputStream();
            while (!end) {
                //READING USER OBJECT SERIALIZED
                byte buffer[] = new byte[Const.BUFFER_SIZE_TXT];
                if (is.read(buffer) == -1)
                    continue;

                //RECEIVING OBJECT NOTIFICATION
                Notification not;
                ByteArrayInputStream byteArrayIS = new ByteArrayInputStream(buffer);
                ObjectInputStream objectIS = new ObjectInputStream(byteArrayIS);
                not = (Notification) objectIS.readObject();

                if(chat) {
                    switch (not.getCode()) {
                        case 1 -> {
                            try {
                                System.out.println("Vou enviar o ficheiro");
                                OutputStream os = file.getOutputStream();
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
                        case 2 -> {
                            try {
                                System.out.println("Vou receber o ficheiro");
                                is = file.getInputStream();

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
                        default -> System.out.println("\nUnknown notification sent");
                    }
                    user.showCommandPrompt();
                }
                else if(privateChat) {
                    switch (not.getCode()) {
                        case 1 -> {
                            System.out.println("\n" + not.getInfo());
//                            user.showPrivateChat(not.getInfo(), not.getInfo2());
                        }
                        default -> System.out.println("\nUnknown notification sent");
                    }
                    user.showCommandPrompt();
                }
            }
            file.close();
        } catch (IOException | ClassNotFoundException e) {
            if (!(e instanceof SocketException)) {
                e.printStackTrace();
            }
        }

    }
}
