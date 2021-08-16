package pt.isec.Components;

import java.io.Serializable;
import java.util.*;

public class Channel implements Serializable {
    private final ArrayList<String> users;
    private String name, password, description;
    private final String creator;
    private final ArrayList<Message> messages;
    private Integer nMessages;

    public Channel(String name, String password, String creator) {
        this.name = name;
        this.password = password;
        this.creator = creator;
        this.users = new ArrayList<>();
        this.messages = new ArrayList<>();
        this.nMessages = 0;

        users.add(creator);
    }

    public Channel(String name, String password, String creator, String description) {
        this.name = name;
        this.password = password;
        this.description = description;
        this.creator = creator;
        this.users = new ArrayList<>();
        this.messages = new ArrayList<>();
        this.nMessages = 0;

        users.add(creator);
    }

    public boolean addUser(String newUser) {
        for (String user : users) {
            if (user.equals(newUser))
                return false;
        }

        users.add(newUser);
        return true;
    }

    public boolean removeUser(String username) {
        return users.remove(username);
    }

    public ArrayList<String> getUsers() {
        return users;
    }

    public Message newMessage(String msg, String username) {
        Message message = new Message(msg, username, name, ++nMessages);
        messages.add(message);
        return message;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getCreator() {
        return creator;
    }

    public boolean checkPassword(String guess) {
        return password.equals(guess);
    }

    public boolean checkCreator(UserProfile user) {
        return creator.equals(user.getUsername());
    }

    public boolean checkCreator(String username) {
        return creator.equals(username);
    }

    public String getStats() {
        return "Number of users: " + users.size() + " | Number of Messages: " + nMessages;
    }

    public boolean containsUser(String username) {
        return users.contains(username);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Channel channel = (Channel) o;
        return Objects.equals(name, channel.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Name: " + name + " | Creator: " + creator + " | Password: " + password + " | Num. Messages: " + nMessages;
    }

    public String showChat() {
        StringBuilder chat = new StringBuilder("Channel: " + name + "\n");

        for (Message msg : messages)
            chat.append("(").append(msg.getSender()).append(") - ").append(msg.getMsg()).append("\n");

        return chat.toString();
    }

    public String getAllUsers() {
        StringBuilder s = new StringBuilder("Channel Users: ");

        for (String u : users)
            s.append("- ").append(u).append("\n");

        return s.toString();
    }

    public String printLastNMessages(int num) {
        StringBuilder s = new StringBuilder("Last ");
        s.append(num).append(" messages from channel ").append(name).append("\n");
        ListIterator<Message> it = messages.listIterator(messages.size());

        while (it.hasPrevious() && (num--) > 0) {
            s.append(it.previous().getMsg()).append("\n");
        }
        return s.toString();
    }
}
