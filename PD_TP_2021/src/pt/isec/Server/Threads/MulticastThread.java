package pt.isec.Server.Threads;

import pt.isec.Components.*;
import pt.isec.Const;
import pt.isec.Server.Server;
import pt.isec.Utils;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * This class is the receiver of every multicast made by other servers
 */
public class MulticastThread extends Thread {
    private final Server server;
    private final MulticastSocket socket;
    private final InetAddress group;
    private boolean end = false;
    private final ArrayList<String> capacityInfo;

    public MulticastThread(Server server, MulticastSocket multicastSocket, InetAddress group) {
        this.server = server;
        this.socket = multicastSocket;
        this.group = group;
        capacityInfo = new ArrayList<>();
    }

    public void closeThread() {
        end = true;
    }

    @Override
    public void run() {
        byte[] cap;
        DatagramPacket multicast;

        try {
            synchronized (this) {
                wait(1000);
            }

            cap = (Const.NEW_SERVER + " " + server.getServerIP() + ":" + server.getServerPortUDP()).getBytes();
            multicast = new DatagramPacket(cap, cap.length, group, Const.SERVER_MULTICAST_PORT);
            socket.send(multicast);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        while (!end) {
            try {
                byte[] buf = new byte[Const.BUFFER_SIZE_OBJECT];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                ArrayList<String> args = Utils.parseString(received, " ");
                switch (processMulticastOptions(args.get(0))) {
                    case 0 -> { // ##### NEW_SERVER #####
                        ArrayList<String> adr = Utils.parseString(args.get(1), ":");
                        if (Integer.parseInt(adr.get(1)) != server.getServerPortUDP()) {
                            //send all relevant info to other server through UDP unicast socket
                            ArrayList<UserProfile> users = server.getUsers();
                            ArrayList<Channel> channels = server.getChannels();
                            ArrayList<PrivateChat> privateChats = server.getPrivateChats();

                            DatagramSocket udp = new DatagramSocket();
                            byte[] update = createUpdateMessage(users.isEmpty(), channels.isEmpty(), privateChats.isEmpty()).getBytes();
                            DatagramPacket p = new DatagramPacket(update, update.length, InetAddress.getByName(adr.get(0)), Integer.parseInt(adr.get(1)));
                            udp.send(p);

                            ByteArrayOutputStream baOS = new ByteArrayOutputStream();
                            ObjectOutputStream oOS = new ObjectOutputStream(baOS);
                            if (!users.isEmpty())
                                oOS.writeUnshared(users);
                            if (!channels.isEmpty())
                                oOS.writeUnshared(channels);
                            if (!privateChats.isEmpty())
                                oOS.writeUnshared(privateChats);
                            oOS.flush();
                            baOS.flush();

                            byte[] objects = baOS.toByteArray();
                            p = new DatagramPacket(objects, objects.length, new InetSocketAddress(adr.get(0), Integer.parseInt(adr.get(1))));
                            udp.send(p);

                            oOS.close();
                            baOS.close();
                        }
                    }
                    case 1 -> { // ##### SERVER_CAPACITY_REQUEST #####
                        //clear the List
                        capacityInfo.clear();
                        cap = (Const.SERVER_CAPACITY_RESPONSE + " " + server.getServerCapacity() + "-" + server.getServerIP() + ":" + server.getServerPortUDP() + "/" + server.getRMIPort()).getBytes();
                        multicast = new DatagramPacket(cap, cap.length, group, Const.SERVER_MULTICAST_PORT);
                        socket.send(multicast);

                        System.out.println("\n(Multicast) Capacity request received!");
                        server.showCommandPrompt();
                    }
                    case 2 -> // ##### SERVER_CAPACITY_RESPONSE #####
                            capacityInfo.add(args.get(1));
                    case 3 -> { // ##### USER_RECONNECTION_REQUEST #####
                        byte[] address = (Const.USER_RECONNECTION_OFFER + " " + server.getServerIP() + ":" + server.getServerPortUDP()).getBytes();
                        DatagramPacket offer = new DatagramPacket(address, address.length, group, Const.SERVER_MULTICAST_PORT);
                        socket.send(offer);
                    }
                    case 4 -> {} // ##### USER_RECONNECTION_OFFER #####
                    case 5 -> { // ##### INFORMATION_MULTICAST #####
                        if (Integer.parseInt(args.get(1)) != server.getServerPortUDP()) {
                            byte[] buffer = new byte[Const.BUFFER_SIZE_OBJECT];
                            DatagramPacket multicastMsg = new DatagramPacket(buffer, buffer.length);
                            socket.receive(multicastMsg);
                            ByteArrayInputStream baIS = new ByteArrayInputStream(multicastMsg.getData(), 0, multicastMsg.getLength());
                            ObjectInputStream oIS = new ObjectInputStream(baIS);
                            Const.MulticastType msg = (Const.MulticastType) oIS.readObject();

                            switch (msg) {
                                case NEW_USER_MULTICAST -> {
                                    UserProfile newUser = (UserProfile) oIS.readObject();
                                    System.out.println("\n(Multicast) New user '" + newUser.getUsername() + "' connected to server " + args.get(1));
//                                    server.addOnlineUser(newUser);
                                    server.addOnlineUser(newUser.getName(), newUser.getUsername(), newUser.getPassword(), newUser.getServer());
                                }
                                case USER_LOGOUT_MULTICAST -> {
                                    UserProfile removedUser = (UserProfile) oIS.readObject();
                                    System.out.println("\n(Multicast) User '" + removedUser.getUsername() + "' logged out in server " + args.get(1));
                                    server.logoutUser(removedUser);
                                }
                                case NEW_CHANNEL_MULTICAST -> {
                                    Channel newChannel = (Channel) oIS.readObject();
                                    System.out.println("\n(Multicast) New channel '" + newChannel.getName() + "' created by '" + newChannel.getCreator() + "' in server " + args.get(1));
                                    server.createChannel(newChannel);
                                }
                                case EDIT_CHANNEL_MULTICAST -> {
                                    Channel editChannel = (Channel) oIS.readObject();
                                    System.out.println("\n(Multicast) Channel '" + editChannel.getName() + "' edited by '" + editChannel.getCreator() + "' in server " + args.get(1));

                                    byte[] oldName = new byte[Const.BUFFER_SIZE_OBJECT];
                                    DatagramPacket p = new DatagramPacket(oldName, oldName.length);
                                    socket.receive(p);
                                    String oldNameStr = new String(p.getData(), 0, p.getLength());
                                    server.editChannel(editChannel, oldNameStr.trim());
                                }
                                case ACCESS_CHANNEL_MULTICAST -> {
                                    Channel editChannel = (Channel) oIS.readObject();
                                    System.out.println("\n(Multicast) Channel '" + editChannel.getName() + "' edited by '" + editChannel.getCreator() + "' in server " + args.get(1));
                                    server.editChannel(editChannel, "");
                                }
                                case REMOVE_CHANNEL_MULTICAST -> {
                                    Channel removedChannel = (Channel) oIS.readObject();
                                    System.out.println("\n(Multicast) Channel '" + removedChannel.getName() + "' was deleted in server " + args.get(1));
                                    server.deleteChannel(removedChannel);
                                }
                                case NEW_PRIVATE_CHAT_MULTICAST -> {
                                    PrivateChat privateChat = (PrivateChat) oIS.readObject();
                                    System.out.println("\n(Multicast) Private '" + privateChat.getName() + "' was created in server " + args.get(1));
                                    server.addPrivateChat(privateChat);
                                }
                                case EDIT_PRIVATE_CHAT_MULTICAST -> {
                                    PrivateChat editedChat = (PrivateChat) oIS.readObject();
                                    System.out.println("\n(Multicast) Private '" + editedChat.getName() + "' was edited in server " + args.get(1));
                                    PrivateChat chat = server.findChat(editedChat.getUser(0), editedChat.getUser(1));
                                    chat.replaceMessages(editedChat);
                                }
                            }
                            server.showCommandPrompt();
                        }
                    }
                    /*case 6 -> {
                        byte[] buffer = server.getServers().getBytes();
                        DatagramPacket response = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
                        socket.send(response);
                    }*/
                }
            } catch (IOException | ClassNotFoundException exception) {
                exception.printStackTrace();
                System.out.println("\nError during multicast");
            }
        }
        try {
            socket.leaveGroup(group);
        } catch (IOException exception) {
            exception.printStackTrace();
        } finally {
            socket.close();
        }


        System.out.println("\nMulticastThread off");
    }

    private String createUpdateMessage(boolean emptyUsers, boolean emptyChannels, boolean emptyChats) {
        StringBuilder str = new StringBuilder(Const.UPDATE_INCOMING);
        if (emptyUsers)
            str.append(" ").append("NO");
        else
            str.append(" ").append("YES");
        if (emptyChannels)
            str.append(" ").append("NO");
        else
            str.append(" ").append("YES");
        if (emptyChats)
            str.append(" ").append("NO");
        else
            str.append(" ").append("YES");
//        System.out.println("update message " + str.toString());
        return str.toString();
    }

    /**
     * Return the array with the capacity information of all servers ordered.
     * @return Ordered Array of information
     */
    public ArrayList<String> getCapacityInfo() {
        capacityInfo.sort((s1, s2) -> {
            ArrayList<String> c1 = Utils.parseString(s1, "-");
            ArrayList<String> c2 = Utils.parseString(s2, "-");

            if (Integer.parseInt(c1.get(0)) > Integer.parseInt(c2.get(0)))
                return 1;
            else if (Integer.parseInt(c1.get(0)) < Integer.parseInt(c2.get(0)))
                return -1;
            return 0;
        });
        return capacityInfo;
    }

    private int processMulticastOptions(String request) {
        if (request.equals(Const.NEW_SERVER))
            return 0;
        if (request.equals(Const.SERVER_CAPACITY_REQUEST))
            return 1;
        if (request.equals(Const.SERVER_CAPACITY_RESPONSE))
            return 2;
        if (request.equals(Const.USER_RECONNECTION_REQUEST))
            return 3;
        if (request.equals(Const.USER_RECONNECTION_OFFER))
            return 4;
        if (request.equals(Const.INFORMATION_MULTICAST))
            return 5;
        if (request.equals(Const.OUTSIDER_LOGIN))
            return 6;
        return -1;
    }

    private int processMulticastInfo(String type) {
        if (type.equals(Const.NEW_USER))
            return 1;
        if (type.equals(Const.NEW_CHANNEL))
            return 2;
        if (type.equals(Const.NEW_MESSAGE))
            return 3;
        if (type.equals(Const.REMOVE_USER))
            return 4;
        if (type.equals(Const.REMOVE_CHANNEL))
            return 5;
        return 0;
    }
}
