package pt.isec.Ousider;

import pt.isec.User.BroadcastListener;
import pt.isec.Components.UserProfile;
import pt.isec.Const;
import pt.isec.Interfaces.RemoteServer;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Outsider {
    private final HashMap<Integer, RemoteServer> remotes;
    private final ArrayList<Integer> observers;
    private BroadcastListener listener;
    private static int NEXT_ID = 0;
    private final String id;
    private UpdateRMIThread thread;
    private boolean end = false;
    private final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        Outsider outsider = new Outsider();
        outsider.run();
        System.exit(0);
    }

    public Outsider() {
        observers = new ArrayList<>();
        remotes = new HashMap<>();
        id = Const.RMI_OBSERVER + "/" + NEXT_ID++;
        try {
            listener = new BroadcastListener(this);
        }
        catch (RemoteException e) {
            System.out.println("\nError creating outsider");
            System.exit(-1);
        }
    }

    private void run() {
        launchRMIThread();
        mainMenu();
    }

    private void launchRMIThread() {
        thread = new UpdateRMIThread(this);
        thread.setDaemon(true);
        thread.start();
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
        return -1;
    }

    private void showMainMenu() {
        System.out.println("\n##### Outsider " + id + " #####\n");
        System.out.println("0 - Shutdown");
        System.out.println("1 - Add Server Observer");
        System.out.println("2 - Remove Server Observer");
        System.out.println("3 - Show Available Servers");
        System.out.println("4 - Show Registered Servers");
        System.out.println("5 - Login User");
        System.out.print("6 - Broadcast Message");
        showCommandPrompt();
    }

    private void mainMenu() {
        while (!end) {
            showMainMenu();
            switch (processMenuOptions(sc.nextLine())) {
                case 0 -> end = true;
                case 1 -> addServerObserver();
                case 2 -> removeServerObserver();
                case 3 -> showAvailableServer();
                case 4 -> showRegisteredServers();
                case 5 -> createNewUser();
                case 6 -> broadcastMessage();
                default -> System.out.println("\nInvalid Option");
            }
        }
        shutdownOutsider();
        System.out.println("\nOutsider Shutting Down");
    }

    private int checkServer(int code) {
        int port;
        while (true) {
            if (code == 1)
                if (!showAvailableServer())
                    return 0;
            else
                if (!showRegisteredServers())
                    return 0;

            System.out.print("> ");
            String opt = sc.nextLine();

            if (isExit(opt)) {
                System.out.println("\nAborted");
                return 0;
            }

            try {
                port = Integer.parseInt(opt);
                if (checkServerPort(opt))
                    return port;
                System.out.println("\nInvalid port inserted");
            } catch (NumberFormatException e) {
                System.out.println("\nInvalid number format");
            }
        }
    }

    private void createNewUser() {
        if (observers.isEmpty()) {
            System.out.println("No server RMI servers registered yet");
            return;
        }

        System.out.println("\n##### Login New User #####\n");
        System.out.println("(0 to exit)");

        int port = checkServer(2);
        if (port == 0)
            return;

        try {
            UserProfile profile = newUser(port);
            if (profile == null) {
                System.out.println("\nUser creation aborted");
                return;
            }
            remotes.get(port).addOfflineUser(profile.getName(), profile.getUsername(), profile.getPassword(), profile.getServer());
        } catch (RemoteException e) {
            System.out.println("\nError creating user");
        }
    }

    private UserProfile newUser(int port) throws RemoteException {
        String name;
        do {
            System.out.print("\nInsert you name: ");
            name = sc.nextLine();
        } while (name.isEmpty() || name.isBlank());

        if (isExit(name))
            return null;

        String username = Const.ERROR;
        try {
            do {
                System.out.print("Insert your username: ");
                username = sc.nextLine();
                if (isExit(username))
                    return null;
            } while (!remotes.get(port).isUsernameValid(username));
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
        return new UserProfile(name, username, pass1, thread.getServerPort(port));
    }

    private void addServerObserver() {
        System.out.println("\n##### Add Server Observer #####");
        System.out.println("(0 to exit)\n");

        int port = checkServer(1);
        if (port == 0)
            return;

        RemoteServer remote = connectRMI(port);
        remotes.put(port, remote);
        try {
            remotes.get(port).addObserver(id, listener);
            observers.add(port);
        } catch (RemoteException | NullPointerException e) {
            System.out.println("\nError adding observer to server RMI " + port);
            remotes.remove(port);
        }
    }

    private void removeServerObserver() {
        System.out.println("\n##### Remove Server Observer #####");
        System.out.println("(0 to exit)\n");

        int port = checkServer(2);
        if (port == 0)
            return;

        try {
            remotes.get(port).removeObserver(id, listener);
            remotes.remove(port);
            observers.remove(port);
        } catch (RemoteException | NullPointerException e) {
            System.out.println("\nError removing observer of server RMI " + port);
        }
    }

    private void broadcastMessage() {
        System.out.println("\n##### Broadcast Message #####");
        System.out.println("(0 to exit)\n");

        int port = checkServer(2);
        if (port == 0)
            return;

        String str;
        do {
            System.out.print("Message to broadcast: ");
            str = sc.nextLine();
        } while (str.isEmpty() || str.isBlank());

        try {
            remotes.get(port).sendBroadcastMessage(str, id);
        } catch (RemoteException | NullPointerException e) {
            System.out.println("\nError sending broadcast message to server RMI " + port);
        }
    }

    private RemoteServer connectRMI(int port) {
        Registry r;
        Remote remote = null;
        try {
            r = LocateRegistry.getRegistry(port);
            remote = r.lookup("rmi_server");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            System.out.println("\nError connecting to RMI Server");
        }
        return (RemoteServer) remote;
    }

    private boolean isExit(String str) {
        return str.trim().equals("0");
    }

    private boolean showAvailableServer() {
        ArrayList<String> servers = thread.getCapacityInfo();
        if (servers.isEmpty()) {
            System.out.println("No servers available");
            return false;
        }

        System.out.println("Servers Available:");
        for (String s: servers)
            System.out.println(s);
        return true;
    }

    private boolean showRegisteredServers() {
        if (observers.isEmpty()) {
            System.out.println("No servers registered");
            return false;
        }

        System.out.println("Servers Registered:");
        for (int port : observers)
            System.out.println("Server RMI - " + port);
        return true;
    }

    private boolean checkServerPort(String port) {
        return thread.getCapacityInfo().contains(port);
    }

    private void shutdownOutsider() {
        for (int i : remotes.keySet()) {
            try {
                remotes.get(i).removeObserver(id, listener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
