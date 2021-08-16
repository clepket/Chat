package pt.isec.Server.Threads;

import pt.isec.Const;
import pt.isec.Server.Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;

public class PingReceiverThread extends Thread {
    private Server server;
    private HashMap<String, Boolean> serverPings;

    public PingReceiverThread(Server server) {
        this.server = server;
        this.serverPings = new HashMap<>();
    }

    @Override
    public void run() {
        while (true) {
            try {
                MulticastSocket socket = new MulticastSocket(Const.SERVER_PING_PORT);
                socket.setSoTimeout(1000);
                serverPings = new HashMap<>();

                while (true) {
                    byte[] buf = new byte[Const.BUFFER_SIZE_TXT];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    String port = new String(packet.getData(), 0, packet.getLength());
                    System.out.print(port + "\n");
                    if (serverPings.containsKey(port))
                        serverPings.replace(port, true);
                    else
                        serverPings.put(port, true);
                }
            } catch (IOException e) {
                if (e instanceof SocketTimeoutException)
                    checkServerPings();
                else {
                    e.printStackTrace();
                    System.out.println("\nPingReceiverThread off");
                }

            }
        }
    }

    private void checkServerPings() {
        for (String port: serverPings.keySet()) {
            if (!serverPings.get(port)) {
                System.out.println("\nServer with port " + port + " went down");
                serverPings.remove(port);
            }
        }
    }
}
