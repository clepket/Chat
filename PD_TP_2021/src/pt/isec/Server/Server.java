package pt.isec.Server;

import pt.isec.Components.*;
import pt.isec.Const;
import pt.isec.Interfaces.BroadcastListenerInterface;
import pt.isec.Server.Threads.*;
import pt.isec.Utils;

import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.util.*;

public class Server {
    private int serverPortUDP, heartbeatPort;
    private String serverIP;
    private File serverFile = null;
    private final ServerRMI rmi;
    private final HashMap<String, BroadcastListenerInterface> observers;

    private final ArrayList<UserProfile> users;
    private final ArrayList<Channel> channels;
    private final ArrayList<PrivateChat> privateChats;
    private final ArrayList<Message> lastMessages;

    private ServerThread listenerThread = null;
    private MulticastThread multicastThread = null;
    private HeartbeatThread heartbeatThread = null;
    private MulticastSocket multicastSocket = null;

    private boolean UDPUpdated = false;
    private boolean end = false;
    private int numberUsers = 0;

    private final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.run();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public Server() throws RemoteException {
        this.serverPortUDP = Const.SERVER_PORT_UDP;
        this.users = new ArrayList<>();
        this.channels = new ArrayList<>();
        this.privateChats = new ArrayList<>();
        this.lastMessages = new ArrayList<>();
        this.rmi = new ServerRMI(this);
        this.observers = new HashMap<>();
    }

    public String getServerIP() {
        return serverIP;
    }

    public int getServerPortUDP() {
        return serverPortUDP;
    }

    public void setServerPortAndIP(int UDPPort, String ip) {
        if (!UDPUpdated) {
            this.serverIP = ip;
            this.serverPortUDP = UDPPort;
            UDPUpdated = true;
        }
    }

    private void setHeartbeatPort(int port) {
        this.heartbeatPort = port;
    }

    public int getHeartbeatPort() {
        return heartbeatPort;
    }

    public int getRMIPort() {
        return rmi.getRMIPort();
    }

    public ArrayList<UserProfile> getUsers() {
        return users;
    }

    /**
     * Server run function - Initiates server.
     */
    public void run() {
        createServerFile();
        readServerInfo();
//        launchHeartbeatThread(); //todo uncomment this
        launchServerThread();
        launchMulticastThread();

        ArrayList<String> cmdArgs;
        while (!end) {
            if (UDPUpdated)
                showMenu();
            cmdArgs = Utils.parseString(sc.nextLine(), " ");

            switch (processInputs(cmdArgs.get(0))) {
                case 0 -> shutdownServer();
                case 1 -> list(cmdArgs);
                case 2 -> listChannelMsg(cmdArgs);
                case 3 -> listUserMsg(cmdArgs);
                case 4 -> showStats(cmdArgs);
                case 5 -> showLastServerMessages(cmdArgs);
                default -> System.out.println("Invalid command");
            }
        }

        System.out.println("\nWaiting for threads to finish");
        listenerThread.closeThread();
        System.out.println("\nServer is shutting down");
    }

    private void shutdownServer() {
        end = true;
        heartbeatThread.shutdownWarning();
    }

    /**
     * Processes input commands.
     * @param str - command entered by the user.
     * @return options for switch
     */
    private int processInputs(String str) {
        if (str.equals(Const.CMD_SHUTDOWN))
            return 0;
        if (str.equals(Const.CMD_LIST))
            return 1;
        if (str.equals(Const.CMD_LIST_CH_MSG))
            return 2;
        if (str.equals(Const.CMD_LIST_USER_MSG))
            return 3;
        if (str.equals(Const.CMD_STATS))
            return 4;
        if (str.equals(Const.CMD_LIST_SERVER_MSG))
            return 5;
        return -1;
    }

    /**
     * Creates server file. This file is used in recognizing the existence of a server by any User.
     */
    private void createServerFile() {
        serverFile = new File(Const.SERVER_PORT_FILE);

        try {
            if (serverFile.createNewFile())
                System.out.print("\nServer file created");
            else
                System.out.print("\nServer file updated");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read server file to determine heartbeat port.
     */
    private void readServerInfo() {
        try {
            Scanner reader = new Scanner(serverFile);
            String data = "";
            while (reader.hasNextLine())
                data = reader.nextLine();

            if (data.isEmpty()) {
                setHeartbeatPort(Const.SERVER_HEARTBEAT_PORT);
                reader.close();
                writeServerInfo();
                return;
            }

            ArrayList<String> args = Utils.parseString(data, " ");
            int HB_Port = Integer.parseInt(args.get(1));
            setHeartbeatPort(++HB_Port);

            reader.close();
            writeServerInfo();
        } catch (FileNotFoundException | NumberFormatException e) {
            System.out.println("No file found");
            e.printStackTrace();
        }
    }

    /**
     * Write server heartbeat port into a file. Others severs will read this file to determine their heartbeat port.
     */
    private void writeServerInfo() {
        FileWriter writer;
        try {
            writer  = new FileWriter(serverFile, true);

            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            bufferedWriter.write("Server_HB_Port " + heartbeatPort + "\n");
            bufferedWriter.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Launch thread that will listen to users trying to connect.
     */
    private void launchHeartbeatThread() {
        heartbeatThread = new HeartbeatThread(this);
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    /**
     * Launch thread that will listen to users trying to connect.
     */
    private void launchServerThread() { //this will launch a thread that listens for users
        listenerThread = new ServerThread(this);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void launchMulticastThread() {
        InetAddress group;
        try {
            multicastSocket = new MulticastSocket(Const.SERVER_MULTICAST_PORT);
            group = InetAddress.getByName(Const.SERVER_MULTICAST_IP);
            multicastSocket.joinGroup(group);

            multicastThread = new MulticastThread(this, multicastSocket, group);
            multicastThread.setDaemon(true);
            multicastThread.start();
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Multicast object.
     * @param obj Object to multicast.
     * @param multicastType Type of object multicast it is.
     */
    public void multicast(Object obj, Const.MulticastType multicastType) {
        try {
            InetAddress group = InetAddress.getByName(Const.SERVER_MULTICAST_IP);
            DatagramSocket socket = new DatagramSocket();

            byte[] multicast = (Const.INFORMATION_MULTICAST + " " + serverPortUDP).getBytes();
            DatagramPacket inform = new DatagramPacket(multicast, multicast.length, group, Const.SERVER_MULTICAST_PORT);
            socket.send(inform);

            ByteArrayOutputStream bAOS = new ByteArrayOutputStream();
            ObjectOutputStream oOS = new ObjectOutputStream(bAOS);
            oOS.writeObject(multicastType);
            oOS.writeObject(obj);
            oOS.flush();

            byte[] buffer = bAOS.toByteArray();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, Const.SERVER_MULTICAST_PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Multicast String.
     * @param str String to multicast
     */
    public void multicast(String str) {
        try {
            InetAddress group = InetAddress.getByName(Const.SERVER_MULTICAST_IP);
            DatagramSocket socket = new DatagramSocket();
            byte[] multicast = str.getBytes();
            DatagramPacket inform = new DatagramPacket(multicast, multicast.length, group, Const.SERVER_MULTICAST_PORT);
            socket.send(inform);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showMenu() {
        if (UDPUpdated)
            System.out.print("\n\n##### Server - " + serverIP + " : " + serverPortUDP + " | RMI port: " + rmi.getRMIPort() + " #####");
        else
            System.out.print("\n\n##### Server #####");
        System.out.println("\n\n       list [all, users, channels] - List system elements");
        System.out.println("          chmsg [channel] [numMsg] - List messages in channel user");
        System.out.println("usermsg [users1] [users2] [numMsg] - List messages between users");
        System.out.println("                   stats [channel] - Show stats of channel");
        System.out.println("                     lastmsg [num] - List last num messages in server");
        System.out.print("                          shutdown - Shutdown server");
        showCommandPrompt();
    }

    public void showCommandPrompt() {
        System.out.print("\nCommand > ");
    }

    private void list(ArrayList<String> cmdArgs) {
        if (cmdArgs.size() == 2) {
            switch (cmdArgs.get(1)) {
                case "all" -> listAll();
                case "users" -> listUsers();
                case "channels" -> listChannels();
                default -> System.out.println("\nUnknown argument parameter");
            }
        }
        else
            System.out.println("\nInvalid number of arguments");
    }

    private void listAll() {
        listUsers();
        listChannels();
    }

    private void listChannels() {
        if (!channels.isEmpty()) {
            System.out.println("\n\nChannels:");
            for (Channel ch : channels)
                System.out.println(ch);
        }
        else
            System.out.println("\nNo channels");
    }

    private void listUsers() {

        if (!users.isEmpty()) {
            System.out.println("\nUsers:");
            for (UserProfile up: users)
                System.out.println(up);
        }
        else
            System.out.println("\nNo users");
    }

    /**
     * Lists messages from channel.
     * @param cmdArgs Command arguments of user.
     */
    public void listChannelMsg(ArrayList<String> cmdArgs) { //Don't print. Need to configure UI
        try {
            if (cmdArgs.size() == 3) {
                String ch =  cmdArgs.get(1);
                int num = Integer.parseInt(cmdArgs.get(2));

                for (Channel channel: channels) {
                    if (channel.getName().equals(ch))
                        System.out.println(channel.printLastNMessages(num));
                }
            }
            else
                System.out.println("\nWrong number of arguments");
        } catch (NumberFormatException e) {
            System.out.println("\nWrong format of number argument");
        }
    }

    /**
     * Lists messages from private chat.
     * @param cmdArgs Command arguments of user.
     */
    public void listUserMsg(ArrayList<String> cmdArgs) {
        try {
            if (cmdArgs.size() == 3) {
                String pc = cmdArgs.get(1);
                int num = Integer.parseInt(cmdArgs.get(2));

                for (PrivateChat privateChat: privateChats) {
                    if (privateChat.getName().equals(pc))
                        System.out.println(privateChat.printLastNMessages(num));
                }
            }
            else
                System.out.println("\nWrong number of arguments");
        } catch (NumberFormatException e) {
            System.out.println("\nWrong format of number argument");
        }
    }

    private void showLastServerMessages(ArrayList<String> cmdArgs) {
        try {
            if (cmdArgs.size() == 2) {
                int num = Integer.parseInt(cmdArgs.get(1));
                System.out.println(getLastServerMessages(num));
            }
            else
                System.out.println("\nWrong number of arguments");
        } catch (NumberFormatException e) {
            System.out.println("\nWrong format of number argument");
        }
    }

    /**
     * Show channel stats.
     * @param cmdArgs - command arguments of user.
     */
    public void showStats(ArrayList<String> cmdArgs) {
        if (cmdArgs.size() == 2) {
            for (Channel ch: channels) {
                if (ch.getName().equals(cmdArgs.get(1)))
                    System.out.println(ch.getStats());
            }
        }
        else
            System.out.println("\nInvalid number of arguments");
    }

    public void addObserver(String username, BroadcastListenerInterface observer) {
        if (!observers.containsKey(username))
            observers.put(username, observer);
        else
            observers.replace(username, observer);
        System.out.println("\n'" + username + "' registered as observer");
    }

    public void removeObserver(String username) {
        observers.remove(username);
        System.out.println("\n'" + username + "' unregistered as observer");
    }

    public void broadcastMessage(String msg, String username) throws RemoteException {
        System.out.println("\nMessage broadcast from user '" + username + "'");
        for (String s : observers.keySet())
            observers.get(s).broadcastMessage(msg, username);
    }

    public void newLoginAlert(String username) {
        for (String s : observers.keySet()) {
            try {
                observers.get(s).newLoginAlert(username, serverPortUDP);
            } catch (RemoteException ignored) {}
        }
    }

    public void newChannelMessageAlert(Channel channel, String username, String msg) {
        for (String user : channel.getUsers()) {
            if (observers.containsKey(username) && !user.equals(username)) {
                try {
                    observers.get(user).newChannelMessageAlert(msg, username, channel.getName());
                } catch (RemoteException ignored) {}
            }
        }
        try {
            checkForOutsiderObserver(channel, username, msg);
        } catch (RemoteException ignored) {}
    }

    public void newPrivateMessageAlert(String receiver, String sender, String msg) {
        if (observers.containsKey(receiver)) {
            try {
                observers.get(receiver).newPrivateMessageAlert(msg, sender, receiver);
            } catch (RemoteException ignored) {}
        }
        try {
            checkForOutsiderObserver(receiver, sender, msg);
        } catch (RemoteException ignored) {}
    }

    private void checkForOutsiderObserver(String receiver, String sender, String msg) throws RemoteException {
        ArrayList<String> obs;
        for (String s : observers.keySet()) {
            obs = Utils.parseString(s, "/");
            if (obs.get(0).equals(Const.RMI_OBSERVER))
                observers.get(s).newPrivateMessageAlert(msg, sender, receiver);
        }
    }

    private void checkForOutsiderObserver(Channel channel, String username, String msg) throws RemoteException {
        ArrayList<String> obs;
        for (String s : observers.keySet()) {
            obs = Utils.parseString(s, "/");
            if (obs.get(0).equals(Const.RMI_OBSERVER))
                observers.get(s).newChannelMessageAlert(msg, username, channel.getName());
        }
    }

    public void logoutUser(UserProfile profile) {
        for (UserProfile user : users) {
            if (user.equals(profile)) {
                System.out.println("\nUser '" + profile.getUsername() + "' logged out");
                user.logout();
                multicast(profile, Const.MulticastType.NEW_USER_MULTICAST);
                showCommandPrompt();
            }
        }
    }

    public void manageUserState(UserProfile profile, boolean status) {
        for (UserProfile user: users) {
            if (user.equals(profile)) {
                if (!status)
                    user.logout();
                else
                    user.login();
                return;
            }
        }
    }

    public void addOnlineUser(String name, String username, String password, int port) {
        numberUsers++;
        UserProfile user = findUser(username);
        if (user != null) {
            user.login();
            System.out.println("\nUser '" + user.getUsername() + "' login ");
        }
        else {
            UserProfile profile = new UserProfile(name, username, password, port);
            if (checkUserServer(profile)) {
                profile.login();
                users.add(profile);
                System.out.println("\nNew user '" + profile.getUsername() + "'");
            }
            else
                System.out.println("\nUser '" + profile.getUsername() + "' was saved");
            user = profile;
        }
        multicast(user, Const.MulticastType.NEW_USER_MULTICAST);
        showCommandPrompt();
    }

    private boolean checkUserServer(UserProfile profile) {
        return (profile.getServer() == serverPortUDP);
    }

    public boolean isUsernameValid(String username) {
        if (username.contains(Const.RMI_OBSERVER))
            return false;

        for (UserProfile user : users) {
            if (user.getUsername().equals(username))
                return false;
        }
        return true;
    }

    public UserProfile doesLoginExist(String username, String password) {
        for (UserProfile user : users) {
            if (user.getUsername().equals(username) && user.checkPassword(password) && !user.isLogged())
                return user;
        }
        return null;
    }

    public int checkServerSpace() {
        multicastServerOccupationRequest();
        return multicastOccupationResultProcess();
    }

    private void multicastServerOccupationRequest() {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(Const.SERVER_MULTICAST_IP);
            byte[] buffer = Const.SERVER_CAPACITY_REQUEST.getBytes();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, Const.SERVER_MULTICAST_PORT);
            socket.send(packet);

            synchronized (this) { wait(500); }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int multicastOccupationResultProcess() {
        ArrayList<String> s = multicastThread.getCapacityInfo();
        if (s.size() >= 1) {
            String topPort = Utils.parseString(Utils.parseString(s.get(0), "-").get(1), ":").get(1);
            topPort = Utils.parseString(topPort, "/").get(0);
            if (Integer.parseInt(Utils.parseString(s.get(0), "-").get(0)) <= (Const.SERVER_CAPACITY/2))
                return Integer.parseInt(topPort);
        }
        return serverPortUDP;
    }

    public String getServers() {
        StringBuilder str = new StringBuilder();
        ArrayList<String> servers = multicastThread.getCapacityInfo();
        for (String s: servers) {
            str.append(Utils.parseString(s, "-").get(1)).append(" ");
        }
        return str.toString();
    }

    public String getHeartbeatInfo() {
        return String.valueOf(heartbeatPort);
    }

    public void createChannel(Channel channel) {
        this.channels.add(channel);
        System.out.println("\n\nUser '" + channel.getCreator() + "' created channel '" + channel.getName() + "'");
    }

    public void editChannel(Channel channel, String oldName) {
        System.out.println("\n\nUser '" + channel.getCreator() + "' made changes to channel " + channel.getName());
        if (!oldName.isEmpty()) {
            for (Channel ch: channels) {
                if (ch.getName().equals(oldName)) {
                    channels.remove(findChannel(oldName));
                    channels.add(channel);
                }
            }
        }
        else {
            for (Channel ch: channels) {
                if (ch.equals(channel)) {
                    channels.remove(channel);
                    channels.add(channel);
                }
            }
        }
    }

    public void addUserToChannel(Channel c, String username) {
        System.out.println("\n\nUser '" + username + "' subscribed channel '" + c.getName() + "'");
        c.addUser(username);
    }

    public void deleteChannel(Channel channel) {
        System.out.println("\n\nChannel '" + channel.getName() + "' deleted by creator '" + channel.getCreator() + "'");
        this.channels.remove(channel);
    }

    public ArrayList<Channel> getChannels() {
        return channels;
    }

    public Channel findChannel(String channelName) {
        for (Channel ch : channels)
            if(ch.getName().equals(channelName))
                return ch;
        return null;
    }

    public UserProfile findUser(String username) {
        for(UserProfile userProfile : users)
            if(userProfile.getUsername().equals(username))
                return userProfile;
        return null;
    }

    public PrivateChat findChat(String sender, String receiver) {
        for(PrivateChat pc : privateChats)
            if (pc.containsUser(sender) && pc.containsUser(receiver))
                return pc;
        return null;
    }

    public ArrayList<PrivateChat> getUserPrivateChats(String username) {
        ArrayList<PrivateChat> chats = new ArrayList<>();
        for (PrivateChat chat : privateChats) {
            if (chat.containsUser(username))
                chats.add(chat);
        }
        return chats;
    }

    public ArrayList<Channel> getUserChannels(String username) {
        ArrayList<Channel> userCh = new ArrayList<>();
        for (Channel c : channels) {
            if (c.containsUser(username))
                userCh.add(c);
        }
        return userCh;
    }

    public ArrayList<Channel> getUserOwnedChannels(String username) {
        ArrayList<Channel> userCh = new ArrayList<>();
        for (Channel c : channels) {
            if (c.checkCreator(username))
                userCh.add(c);
        }
        return userCh;
    }

    public ArrayList<String> getUsernames() {
        ArrayList<String> usernames = new ArrayList<>();
        for (UserProfile user : users)
            usernames.add(user.getUsername());
        return usernames;
    }

    public ArrayList<PrivateChat> getPrivateChats() { return privateChats; }

    public void addPrivateChat(PrivateChat privateChat) { this.privateChats.add(privateChat); }

    public String getServerCapacity() {
        return String.valueOf(numberUsers);
    }

    public void updateUsers(ArrayList<UserProfile> users) {
        if (users != null)
            this.users.addAll(users);
    }

    public void updateChannels(ArrayList<Channel> channels) {
        if (channels != null)
            this.channels.addAll(channels);
    }

    public void updatePrivateChats(ArrayList<PrivateChat> privateChats) {
        if (privateChats != null)
            this.privateChats.addAll(privateChats);
    }

    public void addLastMessage(Message msg) {
        lastMessages.add(msg);
    }

    public String getLastServerMessages(int num) {
        StringBuilder s = new StringBuilder("Last ");
        s.append(num).append(" messages from server ").append(serverPortUDP).append("\n");
        ListIterator<Message> it = lastMessages.listIterator(lastMessages.size());

        while (it.hasPrevious() && (num-- > 0)) {
            Message msg = it.previous();
            s.append("\nFrom: ").append(msg.getSender()).append(" to ").append(msg.getReceiver()).append("\tMsg: ").append(msg.getMsg());
        }
        return s.toString();
    }
}