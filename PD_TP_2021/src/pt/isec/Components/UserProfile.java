package pt.isec.Components;

import java.io.Serializable;
import java.util.Objects;

public class UserProfile implements Serializable {
    private final String name, username, password;
    private int server;
    private boolean isLogged = false;

    public UserProfile(String name, String username, String password, int server) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.server = server;
    }

    public UserProfile(String name, String username, String password, int server, boolean state) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.server = server;
        this.isLogged = state;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public void setServer(int server) {
        this.server = server;
    }

    public int getServer() {
        return server;
    }

    public boolean isLogged() {
        return isLogged;
    }

    public void login() {
        isLogged = true;
    }

    public void logout() {
        isLogged = false;
    }
    @Override
    public String toString() {
        return "Name: " + name + " | Username: " + username + " | Password: " + password + " | Server: " + server + " | Online: " + isLogged;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        return (username.equals(that.username));
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
