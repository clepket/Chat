package pt.isec.Server.Threads;


import pt.isec.Const;
import pt.isec.Server.Server;

import java.io.IOException;
import java.net.*;

public class HeartbeatThread extends Thread {
    private final Server server;
    private MulticastSocket socket;
    private MulticastSocket pingSocket;
    private String MSG = Const.ALIVE;

    public HeartbeatThread(Server server) {
        this.server = server;
    }

    public void shutdownWarning() {
        MSG = Const.SERVER_LOGOUT;
    }

    @Override
    public void run() {
        InetAddress group;
        try {
            socket = new MulticastSocket(server.getHeartbeatPort());
            pingSocket = new MulticastSocket(Const.SERVER_PING_PORT);

            group = InetAddress.getByName(Const.SERVER_MULTICAST_IP);
            socket.joinGroup(group); //missing network interface - don't need it
            pingSocket.joinGroup(group); //missing network interface - don't need it

            while (true) {
                socket.setSoTimeout(1500); //every second, server send signs of being alive

                byte[] buf = MSG.getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group, server.getHeartbeatPort());
                socket.send(packet);

                /*buf = String.valueOf(server.getServerPortUDP()).getBytes();
                packet = new DatagramPacket(buf, buf.length, group, Const.SERVER_PING_PORT);
                pingSocket.send(packet);*/
            }
        }
        catch (IOException exception) {
            socket.close();
            exception.printStackTrace(); //remove later
        }
        System.out.println("\nHeartbeatThread off");
    }
}
