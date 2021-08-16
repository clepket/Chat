package pt.isec.Server.Threads;


import pt.isec.Components.Channel;
import pt.isec.Components.PrivateChat;
import pt.isec.Components.UserProfile;
import pt.isec.Const;
import pt.isec.Server.Server;
import pt.isec.Utils;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ServerThread extends Thread {
    private final String CONNECTION_APPROVED = "YES";

    private int serverPortUDP;
    private final Server server;
    private boolean end = false;

    public ServerThread(Server server) {
        this.serverPortUDP = server.getServerPortUDP();
        this.server = server;
    }

    public void closeThread() {
        end = true;
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        DatagramPacket packet;
        byte[] buffer;

        while (!end) {
            try {
                socket = new DatagramSocket(serverPortUDP);
                server.setServerPortAndIP(serverPortUDP, InetAddress.getLocalHost().getHostAddress());
                break;
            }
            catch (IOException e) {
                if (e instanceof BindException)
                    serverPortUDP++;
                else
                    e.printStackTrace();
            }
        }

        while (!end) {
            try {
                socket.setSoTimeout(5000);
                packet = new DatagramPacket(new byte[Const.BUFFER_SIZE_TXT], Const.BUFFER_SIZE_TXT);
                //NEW CLIENT MESSAGE RECEIVED
                socket.receive(packet);
                String str = new String(packet.getData(), 0, packet.getLength());
                ArrayList<String> args = Utils.parseString(str, " ");
                if (args.get(0).equals(Const.NEW_CON_STR)) {
                    int port = server.checkServerSpace();

                    boolean connect = (port == server.getServerPortUDP());

                    String answer = createAnswer(connect);
                    buffer = answer.getBytes();
                    packet = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                    //Send answer to user
                    socket.send(packet);
                }
                else if (args.get(0).equals(Const.UPDATE_INCOMING)) {
                    while (true) {
                        try {
                            DatagramPacket objects = new DatagramPacket(new byte[500000], 500000);
                            socket.receive(objects);

                            ByteArrayInputStream baIS = new ByteArrayInputStream(objects.getData(), 0, objects.getLength());
                            ObjectInputStream oIS = new ObjectInputStream(baIS);

                            //THIS CAN BE UNSTABLE FOR SEVERAL SERVERS WITH CHANNELS ETC
                            if (args.get(1).equals(CONNECTION_APPROVED))
                                server.updateUsers( (ArrayList<UserProfile>) oIS.readUnshared() );
                            if (args.get(2).equals(CONNECTION_APPROVED))
                                server.updateChannels( (ArrayList<Channel>) oIS.readUnshared() );
                            if (args.get(3).equals(CONNECTION_APPROVED))
                                server.updatePrivateChats( (ArrayList<PrivateChat>) oIS.readUnshared() );

                            System.out.println("\nServer Updated");
                            server.showCommandPrompt();
                            break;
                        } catch (StreamCorruptedException ignored) {}
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                if (e instanceof SocketTimeoutException)
                    continue;
                e.printStackTrace();
            }
        }

        if (socket != null)
            socket.close();
        System.out.println("\nUserListenerThread off");
    }

    public String createAnswer(boolean connect) {
        StringBuilder builder = new StringBuilder();

        if (connect) {
            builder.append(CONNECTION_APPROVED).append(" ");
            builder.append(server.getHeartbeatInfo()).append(" ");
            builder.append(server.getRMIPort());
        }
        else {
            String CONNECTION_DENIED = "NO";
            builder.append(CONNECTION_DENIED).append(" ");
            builder.append(server.getServers());
        }

        return builder.toString();
    }
}
