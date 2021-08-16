package pt.isec.Interfaces;

import pt.isec.Components.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RemoteServer extends Remote {

    void addObserver(String username, BroadcastListenerInterface observer) throws RemoteException;
    void removeObserver(String username, BroadcastListenerInterface observer) throws RemoteException;
    void sendBroadcastMessage(String msg, String username) throws RemoteException;
    String getLastServerMessages(int num) throws RemoteException;

    boolean isLoginValid(String username, String password) throws RemoteException;
    boolean isUsernameValid(String username) throws RemoteException;
    void sendUserProfile(String name, String username, String password, int port) throws RemoteException;
    UserProfile updateUserProfile(String username) throws RemoteException;
    void addOfflineUser(String name, String username, String password, int port) throws RemoteException;
    void shutdownUser(String username) throws RemoteException;

    //METHODS CHANNEL
    boolean sendChannelMessage(String channelName, String sender, String msg) throws RemoteException;
    Channel createChannel(UserMessages msg) throws RemoteException;
    String editChannel(UserMessages msg) throws RemoteException;
    String deleteChannel(UserMessages msg) throws RemoteException;
    Channel subscribeChannel(String channelName, String password, String username) throws RemoteException;
    Channel leaveChannel(String channelName, String password, String username) throws RemoteException;
    Channel getChannelInfo(String channel) throws RemoteException;
    ArrayList<Channel> getUserChannels(String username) throws RemoteException;
    ArrayList<Channel> getOwnedChannels(String username) throws RemoteException;
    ArrayList<Channel> getChannels() throws RemoteException;
    boolean isChannelOwner(String channelName, String username) throws RemoteException;
    String checkChannelPassword(String username, String channel, String password) throws RemoteException;
    boolean channelExists(String channelName) throws RemoteException;

    //METHODS PRIVATE CHAT
    boolean sendDirectMessage(String receiver, String sender, String msg) throws RemoteException;
    PrivateChat createPrivateChat(UserMessages msg) throws RemoteException;
    PrivateChat getPrivateChatInfo(String name, String receiver) throws RemoteException;
    ArrayList<String> getAllUsers() throws RemoteException;
    ArrayList<PrivateChat> getUserPrivateChats(String username) throws RemoteException;
}
