package pt.isec.User.Threads;

import pt.isec.Components.Notification;
import pt.isec.Const;
import pt.isec.User.User;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

public class NotificationThread extends Thread {
    private final Socket notification;
    private final User user;
    private boolean end = false;
    private boolean chat = false;
    private boolean privateChat = false;

    public NotificationThread(User user, Socket socket) {
        this.user = user;
        this.notification = socket;
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
            InputStream is = notification.getInputStream();
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
                            System.out.println("\n" + not.getInfo());
                            user.showChat(not.getInfo());
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
            notification.close();
        } catch (IOException | ClassNotFoundException e) {
            if (!(e instanceof SocketException)) {
                e.printStackTrace();
            }
        }
    }

    private void printNotification() {
        StringBuilder str = new StringBuilder();


        System.out.println(str.toString());
    }
}
