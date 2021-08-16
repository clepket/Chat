package pt.isec.Server.Threads;


import pt.isec.Components.*;
import pt.isec.Const;
import pt.isec.Server.Server;
import pt.isec.Utils;


import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;


public class UserHandlerThread extends java.lang.Thread {
//    private final int loginCode;
    private boolean end = false;
    private final Server server;
    private final Socket socket;
    private UserProfile profile;

    public UserHandlerThread(Server server, Socket socket/*, int loginCode*/) {
        this.server = server;
        this.socket = socket;
//        this.loginCode = loginCode;
    }

    public void closeThread() {
        end = true;
    }

    @Override
    public void run() {
        try {
            System.out.println();
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            /*os.write(String.valueOf(loginCode).getBytes());
            os.flush();*/

            synchronized (server) {
                server.notify();
            }

            int res;
            do {
                res = loginUser(os, is);
            } while (res == 0);

            createProfile(is);
            if (res == 1) { //NEW USER
                System.out.println("\n\nUser '" + profile.getUsername() + "' connected ");
            }
            else if (res == 2){ //OLD USER
                System.out.println("\n\nUser '" + profile.getUsername() + "' reconnected");
            }
            else { //USER RECONNECT
                System.out.println("\n\nUser '" + profile.getUsername() + "' was saved");
            }
            server.showCommandPrompt();

            while (!end) {
                UserMessages um = deserialize(is);
                Channel c;
                PrivateChat pc;
                UserProfile up;

                switch (um.getMessageType()) {
                    case SHUTDOWN -> {
                        end = true;
                        System.out.println("\n\nUser '" + profile.getUsername() + "' logged out"); //todo this line is not printed
                        server.manageUserState(profile, false);
                    }
                    case CREATE_CHANNEL -> { //done
                        if (server.findChannel(um.getChannelName()) == null) {
                            Channel channel;
                            if (um.getDescription().isEmpty())
                                channel = new Channel(um.getChannelName(), um.getChannelPassword(), um.getUser().getUsername());
                            else
                                channel = new Channel(um.getChannelName(), um.getChannelPassword(), um.getUser().getUsername(), um.getDescription());
                            server.createChannel(channel);
                            os.write(Const.CHANNEL_CREATE_SUCCESS.getBytes());
                            server.multicast(channel, Const.MulticastType.NEW_CHANNEL_MULTICAST); //MULTICAST HERE

                            System.out.println("\n\nUser '" + profile.getUsername() + "' created channel '" + channel.getName() + "'");
                        }
                        else {
                            os.write(Const.CHANNEL_CREATE_FAILED.getBytes());

                            System.out.println("\n\nUser '" + profile.getUsername() + "' tried to create a channel that already exists");
                        }
                        os.flush();
                    }
                    case CHECK_CHANNEL_PASS -> {
                        c = server.findChannel(um.getChannelName());
                        if (c == null)
                            os.write(Const.NO_CHANNEL.getBytes());
                        else {
                            if (c.checkPassword(um.getChannelPassword()) && c.checkCreator(um.getUser()))
                                os.write(Const.PASSWORD_CORRECT.getBytes());
                            else
                                os.write(Const.PASSWORD_INCORRECT.getBytes());
                        }
                        os.flush();
                    }
                    case EDIT_CHANNEL -> {
                        c = server.findChannel(um.getChannelName());
                        if (c.checkCreator(um.getUser())) {
                            String oldName = "";
                            if (!um.getNewName().equals("-")) {
                                oldName = um.getChannelName();
                                c.setName(um.getNewName());
                            }
                            if (!um.getNewName().equals("-"))
                                c.setPassword(um.getNewPass());
                            if (!um.getNewName().equals("-"))
                                c.setDescription(um.getDescription());

                            System.out.println("\n\nUser '" + um.getUser().getUsername() + "' made changes to channel " + um.getChannelName());
                            os.write(Const.CHANNEL_EDIT_SUCCESS.getBytes());
                            server.editChannel(c, oldName);
                            server.multicast(c, Const.MulticastType.EDIT_CHANNEL_MULTICAST);
                            server.multicast(oldName);
                        }
                        else
                            os.write(Const.CHANNEL_EDIT_FAILED.getBytes());
                        os.flush();
                    }
                    case DELETE_CHANNEL -> {
                        c = server.findChannel(um.getChannelName());
                        if (c.checkCreator(um.getUser())) {
                            System.out.println("\n\nChannel '" + c.getName() + "' deleted by creator");

                            os.write(Const.CHANNEL_DELETE_SUCCESS.getBytes());
                            server.deleteChannel(c);
                            server.multicast(c, Const.MulticastType.REMOVE_CHANNEL_MULTICAST); //MULTICAST HERE
                        }
                        else
                            os.write(Const.CHANNEL_DELETE_FAILED.getBytes());
                    }
                    case ACCESS_CHANNEL -> {
                        c = server.findChannel(um.getChannelName());
                        if (c.addUser(um.getUser().getUsername())) {
                            os.write(Const.CHANNEL_ACCESS_SUCCESS.getBytes());
                            System.out.println("\n\nUser '" + um.getUser().getUsername() + "' accessed channel '" + c.getName() + "'");
//                            server.addUserToChannel(c, profile);
                            server.multicast(c, Const.MulticastType.ACCESS_CHANNEL_MULTICAST); //MULTICAST HERE
                        }
                        else
                            os.write(Const.CHANNEL_ACCESS_SUCCESS.getBytes());
                        os.flush();
                    }
                    case LEAVE_CHANNEL -> {
                        c = server.findChannel(um.getChannelName());
                        if(c.getUsers().contains(um.getUser().getUsername())) {
                            if(c.removeUser(um.getUser().getUsername())) {
                                os.write(Const.CHANNEL_LEAVE_SUCCESS.getBytes());
                                System.out.println("\n\nUser '" + um.getUser().getUsername() + "' left channel '" + c.getName() + "'");
                                server.multicast(c, Const.MulticastType.ACCESS_CHANNEL_MULTICAST); //MULTICAST HERE
                            }
                            else
                                os.write(Const.CHANNEL_LEAVE_FAILED.getBytes());
                        }
                        else
                            os.write(Const.CHANNEL_LEAVE_IMPOSSIBLE.getBytes());
                        os.flush();
                    }
                    case SHOW_CHAT -> {
                        c = server.findChannel(um.getChannelName());
                        sendChannel(c);
                    }
                    case SHOW_ALL_USERS -> {
                        c = server.findChannel(um.getChannelName());
                        sendUsers(c);
                    }
                    /*case SHOW_FILES -> {
                        sendFiles(um.getUser(), socket);
                    }*/
                    case CREATE_PRIVATE_CHAT -> {
                        pc = server.findChat(um.getChannelName(), um.getReceiver());
                        up = server.findUser(um.getReceiver());
                        if (pc == null) {
                            if (up == null)
                                os.write(Const.CREATE_CHAT_FAILED.getBytes());
                            /*else {
                                PrivateChat newChat = new PrivateChat(um.getUser(), up);
                                server.addPrivateChat(newChat);
                                server.multicast(newChat, Const.MulticastType.NEW_PRIVATE_CHAT_MULTICAST); //MULTICAST HERE
                                os.write(Const.CREATE_CHAT_SUCCESS.getBytes());
                            }*/
                        }
                        else
                            os.write(Const.PRIVATE_CHAT_EXISTS.getBytes());
                        os.flush();
                    }
                    case SHOW_PRIVATE_CHAT -> {
                        pc = server.findChat(um.getChannelName(), um.getReceiver());
                        sendPrivateChat(pc);
                    }
                    case SEND_DIRECT_MESSAGE -> {
                        up = server.findUser(um.getReceiver());
                        System.out.println(um.getChannelName() + " / " + um.getReceiver());
                        pc = server.findChat(um.getChannelName(), um.getReceiver());
                        if(pc == null) {
                            Notification notification = new Notification(0);
                            sendNotificationToUser(notification);
                        }
                        else {
                            //pc = new PrivateChat(um.getUser(), up);
                            if (um.getMessage().equals("exitchat")) {
                                Notification notification = new Notification(2);
                                sendNotificationToUser(notification);
                            } else {
//                                pc.newMessage(um.getMessage(), um.getUser().getUsername());
                                Notification notification = new Notification(1, um.getChannelName(), um.getReceiver());
                                sendNotificationToUser(notification);
//                                server.notifyReceiver(notification, up.getLoginCode());
                                server.multicast(pc, Const.MulticastType.EDIT_PRIVATE_CHAT_MULTICAST); //MULTICAST HERE
                            }
                        }
                    }
                    case SEND_CHANNEL_MESSAGE -> {
                        c = server.findChannel(um.getChannelName());
                        if (c == null) {
                            Notification notification = new Notification(0);
                            sendNotificationToUser(notification);
                        }
                        else {
                            if(um.getMessage().equals("exitchat")) {
                                Notification notification = new Notification(2);
                                sendNotificationToUser(notification);
                            }
                            else if(um.getMessage().equals("sendfile")) {
                                Notification notification = new Notification(3);
                                sendNotificationToUser(notification);
                            }
                            else {
                                c.newMessage(um.getMessage(), um.getUser().getUsername());
                                Notification notification = new Notification(1, c.getName());
                                sendNotificationToUser(notification);
//                                server.notifyUsers(notification, profile.getLoginCode());
                                server.multicast(c, Const.MulticastType.EDIT_CHANNEL_MULTICAST); //MULTICAST HERE
                            }
                        }
                    }
                    default -> System.out.println("\nInvalid option");
                }

                server.showMenu();
            }
        }
        catch (IOException | ClassNotFoundException exception) {
            if (exception instanceof SocketException)
                server.manageUserState(profile, false);
            else
                exception.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }

//        server.deleteUserThreads(profile.getLoginCode());
        if (profile != null) {
            server.multicast(profile, Const.MulticastType.USER_LOGOUT_MULTICAST); //logout outside of this server
            server.manageUserState(profile, false); //logout on this server
            System.out.println("\nUserHandlerThread '" + profile.getUsername() + "' off");
        }
    }

    private void sendChannel(Channel c) throws IOException {
        OutputStream os = socket.getOutputStream();
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        ObjectOutputStream objectOS = new ObjectOutputStream(byteArrayOS);
        objectOS.writeObject(c);

        byte[] buffer = byteArrayOS.toByteArray();
        os.write(buffer);
        os.flush();
    }

    private void sendUsers(Channel c) throws IOException {
        OutputStream os = socket.getOutputStream();
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        ObjectOutputStream objectOS = new ObjectOutputStream(byteArrayOS);
        objectOS.writeObject(c.getUsers());

        byte[] buffer = byteArrayOS.toByteArray();
        os.write(buffer);
        os.flush();
    }

    private void sendPrivateChat(PrivateChat pc) throws IOException {
        OutputStream os = socket.getOutputStream();
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        ObjectOutputStream objectOS = new ObjectOutputStream(byteArrayOS);
        objectOS.writeObject(pc);

        byte[] buffer = byteArrayOS.toByteArray();
        os.write(buffer);
        os.flush();
    }

    private synchronized int loginUser(OutputStream os, InputStream is) throws IOException {
        int res = 0;

        byte[] buffer = new byte[256];
        int nBytes = is.read(buffer);
        String question = new String(buffer, 0, nBytes);

        ArrayList<String> args = Utils.parseString(question, " ");
        StringBuilder answer = new StringBuilder();
        switch (processLoginMessages(args.get(0))) {
            case 1 -> { // #### NEW_USER_REQUEST ####
                if (server.isUsernameValid(args.get(1))) {
                    res = 1;
                    answer.append(Const.USERNAME_APPROVED);
                }
                else
                    answer.append(Const.USERNAME_DECLINED);
            }
            case 2 -> { // #### LOGIN_REQUEST ####
                profile = server.doesLoginExist(args.get(1), args.get(2));
                if (profile != null) {
                    res = 2;
                    sendObjectToUser(profile, os);
                    server.manageUserState(profile, true);
                } else
                    sendObjectToUser(null, os);
            }
            case 3 -> { return 3; } // #### RECONNECT_LOGIN ####
            default -> System.out.println("\n");
        }
        os.write(answer.toString().getBytes());
        os.flush();

        return res;
    }

    private int processLoginMessages(String msg) {
        if (msg.equals(Const.NEW_USER_REQUEST))
            return 1;
        if (msg.equals(Const.LOGIN_REQUEST))
            return 2;
        if (msg.equals(Const.RECONNECT_LOGIN))
            return 3;
        return 0;
    }

    private void createProfile(InputStream is) throws ClassNotFoundException, IOException {
        //READING USER OBJECT SERIALIZED
        byte[] buffer = new byte[Const.BUFFER_SIZE_TXT];
        if (is.read(buffer) == -1) {
            System.out.println("\nError occurred during createProfile");
            end = true;
            return;
        }

        //RECEIVING OBJECT USER THAT CONNECTED
        ByteArrayInputStream byteArrayIS = new ByteArrayInputStream(buffer);
        ObjectInputStream objectIS = new ObjectInputStream(byteArrayIS);
        UserProfile newUser = (UserProfile) objectIS.readObject();

        server.multicast(newUser, Const.MulticastType.NEW_USER_MULTICAST);
//        server.addOnlineUser(newUser);
        profile = newUser;
    }

    private void sendObjectToUser(Object obj, OutputStream os) {
        try {
            ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
            ObjectOutputStream objectOS = new ObjectOutputStream(byteArrayOS);
            objectOS.writeObject(obj);

            byte[] objBuf = byteArrayOS.toByteArray();
            os.write(objBuf);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private UserMessages deserialize(InputStream is) throws IOException, ClassNotFoundException {
        byte[] buffer = new byte[Const.BUFFER_SIZE_OBJECT];
        if (is.read(buffer) == -1) {
            System.out.println("\nAn error occurred during deserializing");
            return null;
        }
        ByteArrayInputStream byteArrayIS = new ByteArrayInputStream(buffer);
        ObjectInputStream objectIS = new ObjectInputStream(byteArrayIS);
        return (UserMessages) objectIS.readUnshared();
    }

    private void sendNotificationToUser(Notification notification) throws IOException {
        OutputStream os = socket.getOutputStream();

        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        ObjectOutputStream objectOS = new ObjectOutputStream(byteArrayOS);
        objectOS.writeObject(notification);

        byte[] buffer = byteArrayOS.toByteArray();
        os.write(buffer);
        os.flush();
    }
}
