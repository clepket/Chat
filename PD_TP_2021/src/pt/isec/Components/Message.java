package pt.isec.Components;

import java.io.Serializable;
import java.util.Objects;

public class Message implements Serializable {
    private final String msg;
    private final String sender, receiver;
    private final int id;

    public Message(String msg, String username, String receiver, int id) {
        this.msg = msg;
        this.sender = username;
        this.receiver = receiver;
        this.id = id;
    }

    public String getMsg() {
        return msg;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return id == message.id && sender.equals(message.sender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, id);
    }
}
