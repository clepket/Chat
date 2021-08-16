package pt.isec.Controllers;

import Models.UserAuthentication;
import org.springframework.web.bind.annotation.*;
import pt.isec.Security.Token;
import pt.isec.Security.User;

import java.rmi.RemoteException;

import static pt.isec.PD_API.PdApiApplication.remoteServer;

@RestController
@RequestMapping("user")
public class UserController {

    @PostMapping("login")
    public User login(@RequestBody UserAuthentication user)
    {
        User loggedUser = null;
        try {
            if(remoteServer.isLoginValid(user.getUsername(), user.getPassword())) {
                UserAuthentication u = user;
                loggedUser = new User();
                loggedUser.setUsername(u.getUsername());
                loggedUser.setPassword(u.getPassword());
                String token = Token.getNewToken(loggedUser.getUsername());
                loggedUser.setToken(token);
                remoteServer.sendUserProfile(u.getName(), loggedUser.getUsername(), loggedUser.getPassword(), 0);
            }

        } catch (RemoteException ex) {
            System.out.println("\nError during login: "+ex);
        }

        return loggedUser;
    }

    @PostMapping("broadcast")
    public String broadcast(@RequestBody String message, @RequestParam(value="username",required=true) String username) {
        try {
            remoteServer.sendBroadcastMessage(message, username);
            return "" + message + " sent as broadcast";
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping("logout")
    public String logout(@RequestParam(value="username", required = true) String username) {
        try {
            remoteServer.shutdownUser(username);
            return "User '" + username + "' logged out successfully";
        } catch (RemoteException e) {
            System.out.println("\nError during logout: " + e);
        }
        return "Something went wrong with logout";
    }
}
