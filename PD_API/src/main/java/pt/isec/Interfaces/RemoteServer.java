package pt.isec.Interfaces;

import java.rmi.RemoteException;

public interface RemoteServer extends java.rmi.Remote {
    boolean isLoginValid(String username, String password) throws RemoteException;
    void sendBroadcastMessage(String msg, String username) throws RemoteException;
    void sendUserProfile(String name, String username, String password, int port) throws RemoteException;
    String getLastServerMessages(int num) throws RemoteException;
    void shutdownUser(String username) throws RemoteException;
}
