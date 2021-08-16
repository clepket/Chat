package pt.isec.User;

import pt.isec.Interfaces.BroadcastListenerInterface;
import pt.isec.Ousider.Outsider;
import pt.isec.User.User;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class BroadcastListener extends UnicastRemoteObject implements BroadcastListenerInterface {
    private final User user;
    private final Outsider outsider;

    public BroadcastListener(User user) throws RemoteException {
        this.user = user;
        this.outsider = null;
    }

    public BroadcastListener(Outsider outsider) throws RemoteException {
        this.user = null;
        this.outsider = outsider;
    }

    @Override
    public void broadcastMessage(String msg, String username) throws RemoteException {
        System.out.println("\n\n'" + username + "' Broadcast Message > " + msg);
        showPrompt();
    }

    @Override
    public void newLoginAlert(String username, int server) throws RemoteException {
        System.out.println("\n\nNew user '" + username + "' in server " + server);
        showPrompt();
    }

    @Override
    public void newChannelMessageAlert(String msg, String sender, String channel) throws RemoteException {
        System.out.println("\n\nNew message sent by user '" + sender + "' to channel '" + channel + "'");
        System.out.println("> " + msg);
        showPrompt();
    }

    @Override
    public void newPrivateMessageAlert(String msg, String sender, String receiver) throws RemoteException {
        if (user != null)
            System.out.println("\n\nNew message sent by user '" + sender);
        else
            System.out.println("\n\nNew message sent by user '" + sender + "' to '" + receiver + "'");
        System.out.println("> " + msg);
        showPrompt();
    }

    private void showPrompt() {
        if (user != null)
            user.showCommandPrompt();
        else if (outsider != null)
            outsider.showCommandPrompt();
    }
}
