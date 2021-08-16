package pt.isec.User;

import pt.isec.Components.*;
import pt.isec.Const;
import pt.isec.Interfaces.RemoteServer;
import pt.isec.User.Threads.StethoscopeThread;
import pt.isec.Utils;

import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class User {
    private StethoscopeThread heartbeatThread = null;
    private UserProfile profile;
    private String serverIP;
    private int serverPortUDP, serverHeartbeat, RMIPort;
    private boolean end = false;
    private RemoteServer remoteServer;
    private BroadcastListener listener;

    private final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("IP: ");
        String ip = sc.nextLine();
        System.out.print("Port: ");
        String port = sc.nextLine();

        try {
            User user = new User(ip.trim(), port.trim());
            user.run();
        } catch (RemoteException e) {
            System.err.println("\nError loading RMI Server");
        }

        System.exit(0);
    }

    public User(String ip, String port) throws RemoteException {
        this.serverIP = ip;
        try {
            this.serverPortUDP = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            this.serverPortUDP = Const.SERVER_PORT_UDP;
        }
    }

    public UserProfile getUserProfile() {
        return profile;
    }

    public void run() {
        serverUDPConnection();
        connectToServer(1);
//        launchStethoscopeThread(); //todo uncomment this
        startMenu();
        mainMenu();
    }

    private void serverUDPConnection() {
        String connectionIP = serverIP;
        int connectionPort = serverPortUDP;

        try {
            DatagramSocket socket;
            DatagramPacket packet;

            while (true) {
                socket = new DatagramSocket(); //try to connect to this port
                byte[] buffer = Const.NEW_CON_STR.getBytes();
                packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(connectionIP), connectionPort);
                System.out.println("\nConnecting to server: " + connectionIP + ":" + connectionPort);
                //SEND REQUEST TO CONNECT
                socket.send(packet);

                //RECEIVE & PROCESS ANSWER
                packet = new DatagramPacket(new byte[Const.BUFFER_SIZE_TXT], Const.BUFFER_SIZE_TXT);
                socket.setSoTimeout(2500);
                socket.receive(packet);
                String str = new String(packet.getData(), 0, packet.getLength());
                ArrayList<String> args = Utils.parseString(str, " ");

                //CONNECTION APPROVED
                if (args.get(0).equals(Const.CONNECTION_APPROVED)) {
                    System.out.println("Connection Approved!");
                    serverIP = connectionIP; //Update server IP
                    serverPortUDP = connectionPort;
                    serverHeartbeat = Integer.parseInt(args.get(1)); //Get server heartbeat port
                    RMIPort = Integer.parseInt(args.get(2));
                    connectToRMIServer();
                    break;
                }
                //NEW CONNECTION SUGGESTED
                else {
                    System.out.println("New Connection Suggested!");
                    printIPs(args);

                    ArrayList<String> newCon = Utils.parseString(args.get(1), ":");
                    connectionIP = newCon.get(0);
                    connectionPort = Integer.parseInt(Utils.parseString(newCon.get(1), "/").get(0));
                }
            }
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                System.out.println("Server " + connectionIP + ":" + connectionPort + " not online");
                shutdownUser(-1);
            }
            e.printStackTrace();
        }
    }

    private void printIPs(ArrayList<String> ips) {
        System.out.print("Next - ");
        for (int i = 1; i < ips.size(); i++)
            System.out.println(ips.get(i));
        System.out.println();
    }

    private void serverUDPReconnect(String address, String port) {
        try {
            while (true) {
                DatagramSocket socket = new DatagramSocket();
                byte[] buffer = Const.NEW_CON_STR.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(address), Integer.parseInt(port));
                socket.send(packet);

                //RECEIVE & PROCESS ANSWER
                packet = new DatagramPacket(new byte[Const.BUFFER_SIZE_TXT], Const.BUFFER_SIZE_TXT);
                socket.receive(packet);
                String str = new String(packet.getData(), 0, packet.getLength());
                ArrayList<String> args = Utils.parseString(str, " ");

                //CONNECTION APPROVED
                if (args.get(0).equals(Const.CONNECTION_APPROVED)) {
                    serverIP = address; //Update server IP
                    serverHeartbeat = Integer.parseInt(args.get(1)); //Get server heartbeat port
                    RMIPort = Integer.parseInt(args.get(2));
                    connectToRMIServer();
                    break;
                }
                //NEW CONNECTION SUGGESTED
                else {
                    ArrayList<String> newCon = Utils.parseString(args.get(1), ":");
                    address = newCon.get(0);
                    port = newCon.get(1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        connectToServer(2);
        sendProfileToServer();
    }

    private void connectToRMIServer() {
        try {
            Registry r = LocateRegistry.getRegistry(RMIPort);
            Remote remote = r.lookup("rmi_server");
            remoteServer = (RemoteServer) remote;
        } catch (RemoteException | NotBoundException e) {
            shutdownUser(3);
        }
    }

    private void connectToServer(int flag) {
        if (flag == 1)
            System.out.println("Server connection established");
        else {
            System.out.println("\nServer connection reestablished");
            showCommandPrompt();
        }
    }

    private void launchStethoscopeThread() {
        heartbeatThread = new StethoscopeThread(this, serverHeartbeat);
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private void startMenu() {
        while (!end) {
            showLoginOptions();
            switch (processMenuOptions(sc.nextLine())) {
                case 1 -> {
                    if (!login())
                        continue;
                }
                case 2 -> {
                    if (!newUser())
                        continue;
                }
                default -> {
                    System.out.println("Invalid Option");
                    continue;
                }
            }
            end = true;
        }

        sendProfileToServer();
    }

    private void showMainMenu() {
        System.out.println("\n##### User: " + profile.getUsername() + " | Server: " + serverPortUDP + " | Server RMI: " + RMIPort + " #####\n");
        System.out.println("0 - Exit");
        System.out.println("1 - Channel Options");
        System.out.println("2 - Chat Options");
        System.out.print("3 - Broadcast message");
        showCommandPrompt();
    }

    public void mainMenu() {
        end = false;
        while(!end) {
            showMainMenu();
            switch (processMenuOptions(sc.nextLine())) {
                case 0 -> end = true;
                case 1 -> channelOptions();
                case 2 -> chatOptions();
                case 3 -> sendBroadcastMessage();
                default -> System.out.println("\nInvalid Option");
            }
        }
        shutdownUser(1);
    }

    private void channelOptionsMenu() {
        System.out.println("\n##### User: " + profile.getUsername() + "\tTCP Port: " + serverPortUDP + " #####\n");
        System.out.println("0 - Exit");
        System.out.println("1 - Create channel");
        System.out.println("2 - Edit channel");
        System.out.println("3 - Delete channel");
        System.out.println("4 - Subscribe channel");
        System.out.println("5 - Leave channel");
        System.out.print("6 - Enter channel");
        showCommandPrompt();
    }

    private void channelOptions() {
        while (true) {
            try {
                System.out.println("\n##### Channel Options #####");
                channelOptionsMenu();
                switch (processMenuOptions(sc.nextLine())) {
                    case 0 -> {
                        return;
                    }
                    case 1 -> createChannel();
                    case 2 -> editChannel();
                    case 3 -> deleteChannel();
                    case 4 -> subscribeChannel();
                    case 5 -> leaveChannel();
                    case 6 -> enterChannel();
                    default -> System.out.println("\nInvalid Option");
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void createChannel() throws RemoteException {
        String channelName, pass, description;

        System.out.println("\n##### Creating channel #####");
        System.out.println("'exit' to cancel channel creation");
        do {
            System.out.print("\nChannel name: ");
            channelName = sc.nextLine();
            if (isExit(channelName))
                return;
        } while (channelName.isEmpty() || channelName.isBlank() || remoteServer.channelExists(channelName));

        do {
            System.out.print("Channel password: ");
            pass = sc.nextLine();
            if (isExit(pass))
                return;
        } while (pass.isBlank() || pass.isEmpty());

        System.out.print("Channel description: ");
        description = sc.nextLine();

        Channel channel = remoteServer.createChannel(new UserMessages(channelName, pass, profile, description));
        if (channel != null)
            System.out.println("\nChannel '" + channelName + "' created with success");
        else
            System.out.println("\nError creating channel " + channelName);
    }

    private void editChannel() throws RemoteException {
        String channelName, newName, pass, newPass, description;

        System.out.println("\n##### Editing channel #####");
        System.out.println("'-' to not edit the parameter, 'exit' to cancel creation at any moment");
        do {
            if (!showOwnedChannels())
                return;
            System.out.print("\nChannel name: ");
            channelName = sc.nextLine();
            if (isExit(channelName))
                return;
        } while (channelName.isBlank() || channelName.isEmpty() || !remoteServer.isChannelOwner(channelName, profile.getUsername()));

        int res;
        do {
            System.out.print("Password: ");
            pass = sc.nextLine();
            if (isExit(pass))
                return;

            res = checkChannelPassword(channelName, pass);
            if (res == -1) {
                System.out.println("\nChannel does not exist");
                return;
            }
            else if (res == 2)
                System.out.println("\nWrong Password");
        } while (res == 2);

        System.out.print("\n=== New Credentials === ");
        do {
            System.out.print("\nChannel new name: ");
            newName = sc.nextLine();
            if (isExit(newName))
                return;
        } while (newName.isBlank() || newName.isEmpty());

        do {
            System.out.print("New password: ");
            newPass = sc.nextLine();
            if (isExit(newPass))
                return;

            if (pass.equals(newPass)) {
                System.out.println("The old and new passwords are the same");
                continue;
            }
        } while (newPass.isEmpty() || newPass.isBlank());

        System.out.print("Channel new description: ");
        description = sc.nextLine();

        String answer = remoteServer.editChannel(new UserMessages(channelName, pass, profile, description, null, null, newName, newPass, null));
        if (answer.equals(Const.CHANNEL_EDIT_SUCCESS)) {
            if (newName.equals("-"))
                System.out.println("\nChannel '" + channelName + "' edited successfully");
            else
                System.out.println("\nChannel '" + channelName + "' is now called '" + newName + "'");
            return;
        }
        System.out.println("\nError editing channel '" + channelName + "'");
    }

    private void deleteChannel() throws RemoteException {
        String channelName, pass;

        System.out.println("\n##### Deleting channel #####");
        System.out.println("'exit' to cancel delete at any moment");
        do {
            if (!showOwnedChannels())
                return;
            System.out.print("\nChannel to delete: ");
            channelName = sc.nextLine();
            if (isExit(channelName))
                return;
        } while (channelName.isEmpty() || channelName.isBlank() || !remoteServer.channelExists(channelName));

        int res;
        do {
            System.out.print("Channel password: ");
            pass = sc.nextLine();
            if (isExit(pass))
                return;

            res = checkChannelPassword(channelName, pass);
            if (res == -1) {
                System.out.println("\nChannel does not exist");
                return;
            }
            else if (res == 2)
                System.out.println("\nWrong Password");
        } while (res == 2);

        String answer = remoteServer.deleteChannel(new UserMessages(channelName, pass, profile, null));
        if (answer.equals(Const.CHANNEL_DELETE_SUCCESS)) {
            System.out.println("\nChannel '" + channelName + "' successfully deleted");
            return;
        }
        System.out.println("\nError deleting channel '" + channelName + "'");
    }

    public void subscribeChannel() throws RemoteException { //what is this supposed to do?
        String channelName, pass;

        System.out.println("\n##### Subscribe Channel #####");
        System.out.println("'exit' to cancel subscription at any moment");

        do {
            if (!showChannels()) {
                System.out.println("There are not channels to subscribe");
                return;
            }
            System.out.print("\nChannel to subscribe: ");
            channelName = sc.nextLine();
            if (isExit(channelName))
                return;
        } while (channelName.isEmpty() || channelName.isBlank() || !remoteServer.channelExists(channelName));

        int res;
        do {
            System.out.print("Password: ");
            pass = sc.nextLine();
            if (isExit(pass))
                return;

            res = checkChannelPassword(channelName, pass);
            if (res == -1) {
                System.out.println("\nChannel does not exist");
                return;
            }
            else if (res == 2)
                System.out.println("\nWrong Password");
        } while (res == 2);

        Channel channel = remoteServer.subscribeChannel(channelName, pass, profile.getUsername());
        if (channel != null)
            System.out.println("\nChannel '" + channelName + "' subscribed with successfully");
        else
            System.out.println("\nAlready subscribed to channel '" + channelName + "'");
    }

    public void leaveChannel() throws RemoteException {
        String channelName, pass;

        System.out.println("\n##### Accessing channel #####");
        System.out.println("'exit' to exit at any moment");
        do {
            System.out.print("Channel to access: ");
            channelName = sc.nextLine();
            if (isExit(channelName))
                return;
        } while (channelName.isEmpty() || channelName.isBlank() || !remoteServer.channelExists(channelName));

        int res;
        do {
            System.out.print("Password: ");
            pass = sc.nextLine();
            if (isExit(channelName))
                return;
            res = checkChannelPassword(channelName, pass);
            if (res == -1) {
                System.out.println("\nChannel does not exist");
                return;
            }
            else if (res == 2)
                System.out.println("\nWrong Password");
        } while (res == 2);

        Channel channel = remoteServer.leaveChannel(channelName, pass, profile.getUsername());
        if (channel != null)
            System.out.println("\nChannel '" + channelName + "' left with success");
        else
            System.out.println("\nError leaving channel '" + channelName + "'");
    }

    private void enterChannel() throws IOException, ClassNotFoundException {
        String channel;
        do {
            if (!showMyChannels()) {
                System.out.println("No channels available");
                return;
            }
            System.out.print("\nChannel: ");
            channel = sc.nextLine();
        } while (channel.isEmpty() || channel.isBlank() || !remoteServer.channelExists(channel));

        do {
            showChat(channel);
        } while(sendChannelMessage(channel));
    }

    private boolean showChannels() throws RemoteException {
        ArrayList<Channel> channels = remoteServer.getChannels();
        if (channels.isEmpty())
            return false;

        System.out.println();
        for (Channel ch : channels)
            System.out.println("- " + ch);
        return true;
    }

    private boolean showMyChannels() throws RemoteException {
        ArrayList<Channel> channels = remoteServer.getUserChannels(profile.getUsername());
        if (channels.isEmpty())
            return false;

        System.out.println();
        for (Channel ch : channels)
            System.out.println("- " + ch);
        return true;
    }

    private boolean showOwnedChannels() throws RemoteException {
        ArrayList<Channel> channels = remoteServer.getOwnedChannels(profile.getUsername());
        if (channels.isEmpty())
            return false;

        System.out.println();
        for (Channel ch : channels)
            System.out.println("- " + ch);
        return true;
    }

    private int checkChannelPassword(String channelName, String password) throws RemoteException {
        return switch (remoteServer.checkChannelPassword(profile.getUsername(), channelName.trim(), password.trim())) {
            case Const.PASSWORD_CORRECT -> 1;
            case Const.NO_CHANNEL -> -1;
            case Const.PASSWORD_INCORRECT -> 2;
            default -> 0;
        };
    }

    public void sendBroadcastMessage() {
        String str;
        do {
            System.out.print("Message to broadcast: ");
            str = sc.nextLine();
        } while (str.isEmpty() || str.isBlank());

        try {
            remoteServer.sendBroadcastMessage(str, profile.getUsername());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean sendChannelMessage(String channelName) throws IOException {
        System.out.println("\n(- to exit)");

        String message;
        do {
            System.out.print("Message: ");
            message = sc.nextLine();
        } while (message.isEmpty() || message.isBlank());

        if(message.equals("-"))
            return false;

        if(!remoteServer.sendChannelMessage(channelName, profile.getUsername(), message)) {
            System.out.println("\nFailed to connect to " + channelName);
            return false;
        }
        return true;
    }

    public void showChat(String channelName) throws RemoteException {
        Channel c = remoteServer.getChannelInfo(channelName);
        System.out.println("\n" + c.showChat());
    }

    private void chatOptionsMenu() {
        System.out.println("\n##### User: " + profile.getUsername() + "\tTCP Port: " + serverPortUDP + " #####\n");
        System.out.println("0 - Exit");
        System.out.println("1 - Create private chat");
        System.out.println("2 - See my chats");
        System.out.print("3 - Enter chat");
        showCommandPrompt();
    }

    private void chatOptions() {
        while (true) {
            try {
                System.out.println("\n##### Chat Options #####");
                chatOptionsMenu();
                switch (processMenuOptions(sc.nextLine())) {
                    case 0 -> {
                        return;
                    }
                    case 1 -> createPrivateChat();
                    case 2 -> showMyPrivateChats();
                    case 3 -> enterPrivateChat();
                    default -> System.out.println("\nInvalid Option");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void createPrivateChat() {
        try {
            System.out.println("\n##### Creating chat #####");
            System.out.println("'-' to exit chat creation");

            String receiver;
            printAllUsers();
            do {
                System.out.print("\nReceiver: ");
                receiver = sc.nextLine();
            } while (receiver.isEmpty() || receiver.isBlank());

            if(receiver.equals("-"))
                return;
            if (receiver.equals(profile.getUsername())) {
                System.out.println("Cannot create a chat with yourself");
                return;
            }

            if (remoteServer.getPrivateChatInfo(profile.getUsername(), receiver.trim()) == null) {
                PrivateChat chat = remoteServer.createPrivateChat(new UserMessages(receiver, profile.getUsername()));
                if (chat != null)
                    System.out.println("\nChat created with success");
                else
                    System.out.println("\nChat creating failure");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void printAllUsers() throws RemoteException {
        ArrayList<String> users = remoteServer.getAllUsers();
        System.out.println("\nAll users available");
        for (String s : users) {
            if (!s.equals(profile.getUsername()))
                System.out.println("- " + s);
        }
    }

    private boolean showMyPrivateChats() throws RemoteException {
        ArrayList<PrivateChat> chats = remoteServer.getUserPrivateChats(profile.getUsername());
        if (!chats.isEmpty()) {
            System.out.println("\nPrivate Chats Available");
            System.out.println("0 - exit");
            int i = 1;
            for (PrivateChat chat : chats)
                System.out.println(i++ + " - " + chat);
            return true;
        }
        System.out.println("You currently have no chats");
        return false;
    }

    public void enterPrivateChat() throws RemoteException {
        String chat;
        int c = 0;
        do {
            if (!showMyPrivateChats())
                return;

            System.out.print("Chat: ");
            chat = sc.nextLine();

            try {
                c = Integer.parseInt(chat);
                if (c == 0)
                    return;
            } catch (NumberFormatException e) {
                System.out.println("Invalid option");
                chat = "";
            }
        } while (chat.isEmpty() || chat.isBlank());

        do {
            showPrivateChat(c);
        } while (sendDirectMessage(chat));
    }

    public boolean sendDirectMessage(String receiver) throws RemoteException {
        System.out.println("\n(- to exit)");

        String msg;
        do {
            System.out.print("Message: ");
            msg = sc.nextLine();
        } while (msg.isEmpty() || msg.isBlank());

        if(msg.equals("-"))
            return false;

        PrivateChat chat = remoteServer.getUserPrivateChats(profile.getUsername()).get(Integer.parseInt(receiver)-1);

        if (!remoteServer.sendDirectMessage(chat.getUser(1), profile.getUsername(), msg)) {
            System.out.println("\nFailed to connect to " + receiver);
            return false;
        }
        return true;
    }

    public void showPrivateChat(int index) throws RemoteException {
        ArrayList<PrivateChat> chats = remoteServer.getUserPrivateChats(profile.getUsername());
        System.out.println();
        int i = 1;
        for (PrivateChat chat : chats) {
            if (index == i++)
                System.out.println(chat.show());
        }
    }

    private boolean login() {
        String username, pass;
        try {
            do {
                System.out.println("'exit' to return to start menu");
                System.out.print("\nInsert your username: ");
                username = sc.nextLine();
                if (username.equals("exit"))
                    return false;

                System.out.print("Insert a password: ");
                pass = sc.nextLine();
            } while (!remoteServer.isLoginValid(username, pass));
            profile = remoteServer.updateUserProfile(username);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean newUser() {
        System.out.println("'exit' to return to start menu");

        String name;
        do {
            System.out.print("\nInsert you name: ");
            name = sc.nextLine();
        } while (name.isEmpty() || name.isBlank());

        if (isExit(name))
            return false;

        String username = Const.ERROR;
        try {
            do {
                System.out.print("Insert your username: ");
                username = sc.nextLine();
                if (isExit(username))
                    return false;
            } while (!remoteServer.isUsernameValid(username));
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        String pass1, pass2;
        while (true) {
            System.out.print("Insert a password: ");
            pass1 = sc.nextLine();
            System.out.print("Confirm password: ");
            pass2 = sc.nextLine();

            if (!pass1.equals(pass2) && !pass1.isEmpty() && !pass1.isBlank())
                System.out.println("Passwords don't match\n");
            else
                break;
        }
        profile = new UserProfile(name, username, pass1, serverPortUDP);
        return true;
    }

    private boolean isExit(String str) {
        return str.trim().equals("exit");
    }

    private void sendProfileToServer() {
        try {
            profile.setServer(serverPortUDP);
            remoteServer.sendUserProfile(profile.getName(), profile.getUsername(), profile.getPassword(), profile.getServer());
        } catch (RemoteException e) {
            e.printStackTrace();
            shutdownUser(-1);
        }
        registerObserver();
    }

    private void showLoginOptions() {
        System.out.println("\n1 - Login");
        System.out.print("2 - New User");
        showCommandPrompt();
    }

    public void showCommandPrompt() {
        System.out.print("\nCommand > ");
    }

    private int processMenuOptions(String str) {
        str = str.trim();
        if (str.equals("0")) return 0;
        if (str.equals("1")) return 1;
        if (str.equals("2")) return 2;
        if (str.equals("3")) return 3;
        if (str.equals("4")) return 4;
        if (str.equals("5")) return 5;
        if (str.equals("6")) return 6;
        if (str.equals("7")) return 7;
        return -1;
    }

    private void registerObserver() {
        try {
            listener = new BroadcastListener(this);
            remoteServer.addObserver(profile.getUsername(), listener);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void reconnectServer() {
        try {
            DatagramSocket failSocket = new DatagramSocket();
            InetAddress group = InetAddress.getByName(Const.SERVER_MULTICAST_IP);
            byte[] buf = Const.USER_RECONNECTION_REQUEST.getBytes();

            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, Const.SERVER_MULTICAST_PORT);
            failSocket.send(packet);
            failSocket.close();

            //Waiting for a response
            MulticastSocket multicastSocket = new MulticastSocket(Const.SERVER_MULTICAST_PORT);
            multicastSocket.setSoTimeout(3000); //3 seconds to answer
            group = InetAddress.getByName(Const.SERVER_MULTICAST_IP);
            multicastSocket.joinGroup(group);

            buf = new byte[Const.BUFFER_SIZE_TXT];
            packet = new DatagramPacket(buf, buf.length);
            multicastSocket.receive(packet);

            String offer = new String(packet.getData(), 0, packet.getLength());
            multicastSocket.leaveGroup(group);
            multicastSocket.close();

            ArrayList<String> args = Utils.parseString(offer, " ");
            if (args.get(0).equals(Const.USER_RECONNECTION_OFFER)) {
                ArrayList<String> conInfo = Utils.parseString(args.get(1), ":");
                serverUDPReconnect(conInfo.get(0), conInfo.get(1));
            }
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException)
                shutdownUser(2);
            else
                e.printStackTrace();
        }
    }

    private void shutdownUser(int status) {
        switch (status) {
            case -1 -> {
                System.out.println("\nERROR - shutting down now");
                try {
                    remoteServer.removeObserver(profile.getUsername(), listener);
                } catch (RemoteException | NullPointerException ignore) {}
                System.exit(status);
            }
            case 1 -> {
                try {
                    warnServerOfShutdown();
                    if (heartbeatThread != null)
                        heartbeatThread.closeThread();
                    this.remoteServer = null;
                    this.listener = null;
                    System.out.println("\nUser shutting down");
                } catch (IOException e) {
                    System.out.println("Error during user shutdown. Exit status " + status);
                }
            }
            case 2 -> {
                System.out.println("\nServer connection lost.");
                System.out.println("No other servers responded");
                System.exit(status);
            }
            case 3 -> {
                System.out.println("\nError connecting to RMI Server. User will shutdown.");
                System.exit(status);
            }
            default -> System.out.println("\nInvalid shutdown code");
        }
    }

    private void warnServerOfShutdown() throws IOException {
        remoteServer.removeObserver(profile.getUsername(), listener);
        remoteServer.shutdownUser(profile.getUsername());
    }
}
