package pt.isec.Components;

import pt.isec.Const;

import java.io.Serializable;

public class UserMessages implements Serializable {
    private Const.MessageType messageType;
    private String channelName = "";
    private String channelPassword = "";
    private UserProfile user = null;
    private String sender = "";
    private String receiver = "";
    private String description = "";
    private String message = "";
    private String newName = "";
    private String newPass = "";
//    private String filePath = "";

    private String user1 = "", user2 = "", msg = "";

    public UserMessages(String channelN, String pass, UserProfile u, String desc, String r, String m, String nn, String np, String fp) {
        channelName = channelN;
        channelPassword = pass;
        user = u;
        description = desc;
        receiver = r;
        message = m;
        newName = nn;
        newPass = np;
//        filePath = fp;
    }

    /**
     * This constructor is to handle the channel
     */
    public UserMessages(String name, String pass, UserProfile u, String desc) {
        channelName = name;
        channelPassword = pass;
        user = u;
        description = desc;
    }

    /**
     * This constructor is to handle the private chat
     */
    public UserMessages(String receiver, String sender) {
        this.receiver = receiver;
        this.sender = sender;
    }

    public UserMessages(Const.MessageType msg, String receiver, String sender, String message) {
        if (msg == Const.MessageType.SEND_DIRECT_MESSAGE)
            this.receiver = receiver;
        else
            this.channelName = receiver;
        this.sender = sender;
        this.message = message;
    }

    public String getUser1() {
        return user1;
    }

    public String getUser2() {
        return user2;
    }

    public Const.MessageType getMessageType() {
        return messageType;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getChannelPassword() {
        return channelPassword;
    }

    public UserProfile getUser() {
        return user;
    }

    public String getDescription() {
        return description;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public String getNewName() {
        return newName;
    }

    public String getNewPass() {
        return newPass;
    }

    public void setUser(UserProfile user) {
        this.user = user;
    }
}
