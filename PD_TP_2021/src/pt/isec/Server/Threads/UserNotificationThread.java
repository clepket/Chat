package pt.isec.Server.Threads;

import pt.isec.Components.Notification;
import pt.isec.Server.Server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class UserNotificationThread extends Thread {
    private Notification notification = null;
    private Server server; //might not need this
    private Socket socket;
    private boolean end = false;

    public UserNotificationThread(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    public void closeThread() {
        end = true;
    }

    public void setNotification(Notification notification) {
        this.notification = notification;
    }

    @Override
    public void run() {
        try {
            OutputStream os = socket.getOutputStream();

            while (!end) {
                synchronized (this) {
                    wait();
                }

                if (notification == null)
                    continue;

                ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
                ObjectOutputStream objectOS = new ObjectOutputStream(byteArrayOS);
                objectOS.writeObject(notification);

                byte[] buffer = byteArrayOS.toByteArray();
                os.write(buffer);
                os.flush();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {}
        }
        System.out.println("\nUserNotificationThread off");
    }
}
