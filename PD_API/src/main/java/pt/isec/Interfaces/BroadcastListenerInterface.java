package pt.isec.Interfaces;

import java.rmi.RemoteException;

public interface BroadcastListenerInterface extends java.rmi.Remote {
    void broadcastMessage(String msg, String username) throws RemoteException;
    void newLoginAlert(String username, int server) throws RemoteException;
}
