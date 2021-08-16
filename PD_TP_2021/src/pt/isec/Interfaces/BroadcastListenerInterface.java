package pt.isec.Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BroadcastListenerInterface extends Remote {

    void broadcastMessage(String msg, String username) throws RemoteException;
    void newLoginAlert(String username, int server) throws RemoteException;
    void newChannelMessageAlert(String msg, String sender, String channel) throws RemoteException;
    void newPrivateMessageAlert(String msg, String sender, String receiver) throws RemoteException;
}
