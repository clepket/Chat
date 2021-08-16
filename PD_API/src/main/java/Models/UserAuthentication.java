package Models;

import java.io.Serializable;

public class UserAuthentication implements Serializable {
    private final String name, username, password;

    public UserAuthentication(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
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
}
