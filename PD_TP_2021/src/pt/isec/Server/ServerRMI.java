package pt.isec.Server;

import pt.isec.Components.*;
import pt.isec.Const;
import pt.isec.Interfaces.BroadcastListenerInterface;
import pt.isec.Interfaces.RemoteServer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ServerRMI extends UnicastRemoteObject implements RemoteServer {
    private final Server server;
    private final int port;

    public ServerRMI(Server server) throws RemoteException {
        this.server = server;
        int port = Registry.REGISTRY_PORT;

        while (true) {
            try {
                Registry registry = LocateRegistry.createRegistry(port);
                registry.rebind(Const.RMI_SERVER, this);
                System.out.print("RMI Server is running");
                break;
            } catch (ExportException e) {
                port++;
            }
        }
        this.port = port;
    }

    public int getRMIPort() {
        return port;
    }

    @Override
    public void addObserver(String username, BroadcastListenerInterface observer) {
        server.addObserver(username, observer);
        server.showCommandPrompt();
    }

    @Override
    public void removeObserver(String username, BroadcastListenerInterface observer) {
        server.removeObserver(username);
    }

    @Override
    public void sendBroadcastMessage(String msg, String username) throws RemoteException {
        server.broadcastMessage(msg, username);
        server.showCommandPrompt();
    }

    @Override
    public String getLastServerMessages(int num) throws RemoteException {
        return server.getLastServerMessages(num);
    }

    @Override
    public boolean isLoginValid(String username, String password) throws RemoteException {
        return (server.doesLoginExist(username, password) != null);
    }

    @Override
    public boolean isUsernameValid(String username) throws RemoteException {
        return server.isUsernameValid(username);
    }

    @Override
    public void sendUserProfile(String name, String username, String password, int port) throws RemoteException {
        server.addOnlineUser(name, username, password, port);
        server.newLoginAlert(username);
    }

    @Override
    public UserProfile updateUserProfile(String username) throws RemoteException {
        UserProfile up = server.findUser(username);
        up.login();
        return up;
    }

    @Override
    public void addOfflineUser(String name, String username, String password, int port) throws RemoteException {
        server.addOnlineUser(name, username, password, port);
        server.logoutUser(server.findUser(username));
    }

    @Override
    public void shutdownUser(String username) throws RemoteException {
        server.logoutUser(server.findUser(username));
        server.multicast(server.findUser(username), Const.MulticastType.USER_LOGOUT_MULTICAST);
    }

    //CHANNELS

    @Override
    public String checkChannelPassword(String username, String channel, String password) throws RemoteException {
        Channel c = server.findChannel(channel);
        if (c == null)
            return Const.NO_CHANNEL;
        else {
            if (c.checkPassword(password))
                return Const.PASSWORD_CORRECT;
            else
                return Const.PASSWORD_INCORRECT;
        }
    }

    @Override
    public boolean isChannelOwner(String channelName, String username) throws RemoteException {
        return server.findChannel(channelName).checkCreator(username);
    }

    @Override
    public Channel createChannel(UserMessages msg) throws RemoteException {
        if (server.findChannel(msg.getChannelName()) == null) {
            Channel channel;
            if (msg.getDescription().isEmpty())
                channel = new Channel(msg.getChannelName(), msg.getChannelPassword(), msg.getUser().getUsername());
            else
                channel = new Channel(msg.getChannelName(), msg.getChannelPassword(), msg.getUser().getUsername(), msg.getDescription());
            server.createChannel(channel);
            server.multicast(channel, Const.MulticastType.NEW_CHANNEL_MULTICAST); //MULTICAST HERE

            server.showCommandPrompt();
            return channel;
        }
        else {
            System.out.println("\n\nUser '" + msg.getUser().getUsername() + "' tried to create a channel that already exists");
            server.showCommandPrompt();
            return null;
        }
    }

    @Override
    public String editChannel(UserMessages msg) throws RemoteException {
        Channel c = server.findChannel(msg.getChannelName());
        if (c.checkCreator(msg.getUser())) {
            String oldName = "";
            if (!msg.getNewName().equals("-")) {
                oldName = msg.getChannelName();
                c.setName(msg.getNewName());
            }
            if (!msg.getNewName().equals("-"))
                c.setPassword(msg.getNewPass());
            if (!msg.getNewName().equals("-"))
                c.setDescription(msg.getDescription());

            server.editChannel(c, oldName);
            server.multicast(c, Const.MulticastType.EDIT_CHANNEL_MULTICAST);
            server.multicast(oldName);
            server.showCommandPrompt();
            return Const.CHANNEL_EDIT_SUCCESS;
        }
        else
            return Const.CHANNEL_EDIT_FAILED;
    }

    @Override
    public String deleteChannel(UserMessages msg) throws RemoteException {
        Channel c = server.findChannel(msg.getChannelName());
        if (c.checkCreator(msg.getUser())) {
            server.deleteChannel(c);
            server.multicast(c, Const.MulticastType.REMOVE_CHANNEL_MULTICAST); //MULTICAST HERE
            server.showCommandPrompt();
            return Const.CHANNEL_DELETE_SUCCESS;
        }
        return Const.CHANNEL_DELETE_FAILED;
    }

    @Override
    public Channel subscribeChannel(String channelName, String password, String username) throws RemoteException {
        Channel c = server.findChannel(channelName);
        if (c.addUser(username)) {
            server.addUserToChannel(c, username);
            server.multicast(c, Const.MulticastType.ACCESS_CHANNEL_MULTICAST); //MULTICAST HERE
            server.showCommandPrompt();
            return c;
        }
        return null;
    }

    @Override
    public Channel leaveChannel(String channelName, String password, String username) throws RemoteException {
        Channel c = server.findChannel(channelName);
        if (c.getUsers().contains(username)) {
            if (c.removeUser(username)) {
                System.out.println("\n\nUser '" + username + "' left channel '" + c.getName() + "'");
                server.multicast(c, Const.MulticastType.ACCESS_CHANNEL_MULTICAST); //MULTICAST HERE
                server.showCommandPrompt();
                return c;
            }
        }
        return null;
    }

    @Override
    public Channel getChannelInfo(String channel) throws RemoteException {
        return server.findChannel(channel);
    }

    @Override
    public ArrayList<Channel> getUserChannels(String username) throws RemoteException {
        return server.getUserChannels(username);
    }

    @Override
    public ArrayList<Channel> getChannels() throws RemoteException {
        return server.getChannels();
    }

    @Override
    public ArrayList<Channel> getOwnedChannels(String username) throws RemoteException {
        return server.getUserOwnedChannels(username);
    }

    @Override
    public boolean channelExists(String channelName) throws RemoteException {
        return (server.findChannel(channelName) != null);
    }

    @Override
    public boolean sendChannelMessage(String channelName, String sender, String msg) throws RemoteException {
        Channel c = server.findChannel(channelName);
        if (c == null)
            return false;
        else {
            server.addLastMessage(c.newMessage(msg, sender));
            server.multicast(c, Const.MulticastType.EDIT_CHANNEL_MULTICAST); //MULTICAST HERE
            server.newChannelMessageAlert(c, sender, msg);
            return true;
        }
    }

    //PRIVATE CHATS

    @Override
    public PrivateChat createPrivateChat(UserMessages msg) throws RemoteException {
        PrivateChat pc = server.findChat(msg.getSender(), msg.getReceiver());
        UserProfile receiver = server.findUser(msg.getReceiver());
        if (pc == null && receiver != null) {
            PrivateChat newChat = new PrivateChat(msg.getSender(), msg.getReceiver());
            server.addPrivateChat(newChat);
            server.multicast(newChat, Const.MulticastType.NEW_PRIVATE_CHAT_MULTICAST); //MULTICAST HERE
            return newChat;
        }
        return null;
    }

    @Override
    public PrivateChat getPrivateChatInfo(String name, String receiver) throws RemoteException {
        return server.findChat(name, receiver);
    }

    @Override
    public ArrayList<String> getAllUsers() throws RemoteException {
        return server.getUsernames();
    }

    @Override
    public ArrayList<PrivateChat> getUserPrivateChats(String username) throws RemoteException {
        return server.getUserPrivateChats(username);
    }

    @Override
    public boolean sendDirectMessage(String receiver, String sender, String msg) throws RemoteException {
        PrivateChat pc = server.findChat(sender, receiver);
        if (pc == null)
            return false;

        server.addLastMessage(pc.newMessage(msg, sender, receiver));
        server.multicast(pc, Const.MulticastType.EDIT_PRIVATE_CHAT_MULTICAST); //MULTICAST HERE
        server.newPrivateMessageAlert(receiver, sender, msg);
        return true;
    }
}
