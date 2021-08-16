package pt.isec.Ousider;

import pt.isec.Const;
import pt.isec.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

public class UpdateRMIThread extends Thread {
    private final Outsider outsider;
    private boolean end = false;
    private final ArrayList<String> capacityInfo;

    public UpdateRMIThread(Outsider outsider) {
        this.outsider = outsider;
        capacityInfo = new ArrayList<>();
    }

    @Override
    public void run() {
        DatagramPacket packet;
        MulticastSocket socket = initializeSocket();
        if (socket == null) {
            System.out.println("\nError initializing multicast socket");
            return;
        }

        try {
            //REQUEST A CAPACITY RESPONSE
            capacityInfo.clear();
            InetAddress group = InetAddress.getByName(Const.SERVER_MULTICAST_IP);
            byte[] buffer = Const.SERVER_CAPACITY_REQUEST.getBytes();
            packet = new DatagramPacket(buffer, buffer.length, group, Const.SERVER_MULTICAST_PORT);
            socket.send(packet);

            synchronized (this) { wait(500); }
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        }

        while (!end) {
            try {
                byte[] buf = new byte[Const.BUFFER_SIZE_OBJECT];
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                ArrayList<String> args = Utils.parseString(received, " ");

                switch (processMulticastOptions(args.get(0))) {
                    case 1 -> {
                        capacityInfo.add(args.get(1));
                        System.out.println("\n(Multicast) New RMI server online!");
//                        outsider.updateRMI(capacityInfo);
                        outsider.showCommandPrompt();
                    }
                    case 2 -> capacityInfo.clear();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                System.out.println("\nError during multicast");
            }
        }
    }

    private MulticastSocket initializeSocket() {
        InetAddress group;
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(Const.SERVER_MULTICAST_PORT);
            group = InetAddress.getByName(Const.SERVER_MULTICAST_IP);
            socket.joinGroup(group);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return socket;
    }

    private int processMulticastOptions(String request) {
        if (request.equals(Const.SERVER_CAPACITY_RESPONSE))
            return 1;
        if (request.equals(Const.SERVER_CAPACITY_REQUEST))
            return 2;
        return -1;
    }

    public ArrayList<String> getCapacityInfo() {
        capacityInfo.sort((s1, s2) -> {
            ArrayList<String> c1 = Utils.parseString(s1, "-");
            ArrayList<String> c2 = Utils.parseString(s2, "-");

            if (Integer.parseInt(c1.get(0)) > Integer.parseInt(c2.get(0)))
                return 1;
            else if (Integer.parseInt(c1.get(0)) < Integer.parseInt(c2.get(0)))
                return -1;
            return 0;
        });
        ArrayList<String> tmp = new ArrayList<>();
        for (String s : capacityInfo)
            tmp.add(Utils.parseString(s, "/").get(1));
        return tmp;
    }

    public int getServerPort(int port) {
        for (String s : capacityInfo) {
            if (Utils.parseString(s, "/").get(1).equals(String.valueOf(port)))
                return Integer.parseInt(Utils.parseString(Utils.parseString(s, "/").get(0), ":").get(1));
        }
        return -1;
    }
}
