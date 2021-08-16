package pt.isec.User.Threads;

import pt.isec.Const;
import pt.isec.User.User;

import java.io.IOException;
import java.net.*;

public class StethoscopeThread extends Thread {
    private MulticastSocket socket = null;
    private int heartbeatPort;
    private User user;
    private boolean end = false;

    public StethoscopeThread(User user, int port) {
        this.user = user;
        this.heartbeatPort = port;
    }

    public void closeThread() {
        end = true;
    }

    @Override
    public void run() {
        InetAddress group = null;
        try {
            socket = new MulticastSocket(heartbeatPort);
            group = InetAddress.getByName(Const.SERVER_MULTICAST_IP);
            socket.joinGroup(group);

            while (!end) {
                socket.setSoTimeout(2500); //2.5 second timeout for the heartbeat to fail.

                byte[] buf = new byte[Const.BUFFER_SIZE_TXT];
//                byte[] buf = Const.ALIVE.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                if (msg.equals(Const.SERVER_LOGOUT)) {
                    System.out.println("\n\nServer has logged out");
                    end = true;
                    user.reconnectServer();
                }
            }
        }
        catch (IOException exception) {
            if (exception instanceof SocketTimeoutException)
                user.reconnectServer();
            else
                exception.printStackTrace();
        } finally {
            try {
                socket.leaveGroup(group);
            } catch (IOException exception) {
                exception.printStackTrace();
            } finally {
                socket.close();
            }
        }
    }
}
