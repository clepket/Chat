package Observer;

import java.rmi.RemoteException;

public interface ObserverRMI extends java.rmi.Remote {
    public void notifyNewOperation(String description) throws RemoteException;
}
