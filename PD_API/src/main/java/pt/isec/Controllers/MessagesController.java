package pt.isec.Controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.rmi.RemoteException;

import static pt.isec.PD_API.PdApiApplication.remoteServer;

@RestController
public class MessagesController {

    @GetMapping("last-messages")
    public String lastMessages(@RequestParam(value="n",required=false,defaultValue = "50") int n) {
        try {
            String messages = remoteServer.getLastServerMessages(n);
            return messages;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return "Error getting messages";
    }
}
