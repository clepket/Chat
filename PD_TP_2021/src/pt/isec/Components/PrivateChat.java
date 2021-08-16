package pt.isec.Components;

import java.io.Serializable;
import java.util.*;

public class PrivateChat implements Serializable {
    private final String name;
    private final ArrayList<String> users;
    private ArrayList<Message> messages;
    private int nMessages;

    public PrivateChat(String sender, String receiver) {
        this.name = sender + " / " + receiver;
        this.messages = new ArrayList<>();
        this.nMessages = 0;
        users = new ArrayList<>();
        users.add(sender);
        users.add(receiver);
    }

    public Message newMessage(String msg, String username, String receiver) {
        Message message = new Message(msg, username, receiver, ++nMessages);
        messages.add(message);
        return message;
    }

    public void replaceMessages(PrivateChat chat) {
        messages = chat.messages;
    }

    public String getName() {
        return name;
    }

    public boolean containsUser(String username) { //todo use this
        return users.contains(username);
    }

    /**
     * Index is 1 or 2
     * @param index
     * @return
     */
    public String getUser(int index) {
        return users.get(index);
    }

    public String show() {
        StringBuilder chat = new StringBuilder("Private Chat: " + name + "\n");

        for (Message msg : messages)
            chat.append("(").append(msg.getSender()).append(") - ").append(msg.getMsg()).append("\n");

        return chat.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrivateChat that = (PrivateChat) o;
        return name.equals(that.name) && users.equals(that.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, users);
    }

    @Override
    public String toString() {
        return name + " (" + nMessages + " Messages)";
    }

    public String printLastNMessages(int num) {
        StringBuilder s = new StringBuilder("Last ");
        s.append(num).append(" messages from chat ").append(name).append("\n");
        ListIterator<Message> it = messages.listIterator(messages.size());

        while (it.hasPrevious() && (num--) > 0) {
            s.append(it.previous().getMsg()).append("\n");
        }
        return s.toString();
    }
}
