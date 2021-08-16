package pt.isec;

public abstract class Const {

    //COMMANDS
    public static final String CMD_LIST = "list";
    public static final String CMD_LIST_CH_MSG = "chmsg";
    public static final String CMD_LIST_USER_MSG = "usermsg";
    public static final String CMD_LIST_SERVER_MSG = "lastmsg";
    public static final String CMD_STATS = "stats";
    public static final String CMD_SHUTDOWN = "shutdown";

    public static final int SERVER_CAPACITY = 10; //this number is temporary
    public static final String SERVER_PORT_FILE = "serverPorts.txt";

    public static final String RMI_SERVER = "rmi_server";
    public static final String RMI_OBSERVER = "RMI_OBSERVER";
    public static final String ERROR = "ERROR";
    public static final String ALIVE = "IM_ALIVE";
    public static final String SERVER_LOGOUT = "SERVER_LOGOUT";
    public static final String NEW_USER_REQUEST = "NEW_USER";
    public static final String USERNAME_APPROVED = "USERNAME_APPROVED";
    public static final String USERNAME_DECLINED = "USERNAME_DECLINED";
    public static final String LOGIN_REQUEST = "LOGIN";
    public static final String RECONNECT_LOGIN = "RECONNECT_LOGIN";
    public static final String SERVER_PING = "SERVER_PING";
    public static final String INFORMATION_MULTICAST = "INFO_MULTICAST_INCOMING";
    public static final String SERVER_CAPACITY_REQUEST = "CAPACITY_REQUEST";
    public static final String SERVER_CAPACITY_RESPONSE = "CAPACITY_RESPONSE";
    public static final String USER_RECONNECTION_REQUEST = "USER_RECONNECT_REQUEST";
    public static final String USER_RECONNECTION_OFFER = "USER_RECONNECT_OFFER";
    public static final String CONNECTION_APPROVED = "YES";
    public static final String NEW_CON_STR = "CONNECT";
    public static final String NEW_SERVER = "NEW_SERVER";
    public static final String UPDATE_INCOMING = "SERVER_UPDATE_INCOMING";
    public static final String OUTSIDER_LOGIN = "OUTSIDER_LOGIN";

    public static final int BUFFER_SIZE_TXT = 256;
    public static final int BUFFER_SIZE_OBJECT = 6000;
    public static final int BUFFER_SIZE_FILE = 5000;

    //OBJECTS FOR MULTICAST
    public static final String NEW_USER = "NEW_USER";
    public static final String REMOVE_USER = "REMOVE_USER";
    public static final String NEW_CHANNEL = "NEW_CHANNEL";
    public static final String REMOVE_CHANNEL = "REMOVE_CHANNEL";
    public static final String NEW_MESSAGE = "NEW_MESSAGE";
//    public static final String NEW = "CONNECT";

    //PORTS
    public static final int SERVER_PORT_UDP = 5000;
    public static final int OUTSIDER_PORT_UDP = 6000;
    public static final int SERVER_HEARTBEAT_PORT = 2000;
    public static final int SERVER_MULTICAST_PORT = 4000;
    public static final int SERVER_PING_PORT = 3000;

    public static final String SERVER_MULTICAST_IP = "230.0.0.0";

    //SERVER RESPONSES
    public static final String CHANNEL_CREATE_SUCCESS = "CHANNEL_CREATE_SUCCESS";
    public static final String CHANNEL_CREATE_FAILED = "CHANNEL_CREATE_FAILED";
    public static final String CHANNEL_EDIT_SUCCESS = "CHANNEL_EDIT_SUCCESS";
    public static final String CHANNEL_EDIT_FAILED = "CHANNEL_EDIT_FAILED";
    public static final String CHANNEL_DELETE_SUCCESS = "CHANNEL_DELETE_SUCCESS";
    public static final String CHANNEL_DELETE_FAILED = "CHANNEL_DELETE_FAILED";
    public static final String CHANNEL_ACCESS_SUCCESS = "CHANNEL_ACCESS_SUCCESS";
    public static final String CHANNEL_ACCESS_FAILED = "CHANNEL_ACCESS_FAILED";
    public static final String CHANNEL_LEAVE_SUCCESS = "CHANNEL_LEAVE_SUCCESS";
    public static final String CHANNEL_LEAVE_FAILED = "CHANNEL_LEAVE_FAILED";
    public static final String CHANNEL_LEAVE_IMPOSSIBLE = "CHANNEL_LEAVE_IMPOSSIBLE";
    public static final String NO_CHANNEL = "NO_CHANNEL";
    public static final String PASSWORD_CORRECT = "PASSWORD_CORRECT";
    public static final String PASSWORD_INCORRECT = "PASSWORD_INCORRECT";
    public static final String NO_PRIVATE_CHAT = "NO_PRIVATE_CHAT";
    public static final String CREATE_CHAT_SUCCESS = "CREATE_CHAT_SUCCESS";
    public static final String PRIVATE_CHAT_EXISTS = "PRIVATE_CHAT_EXISTS";
    public static final String CREATE_CHAT_FAILED = "CREATE_CHAT_FAILED";


    public enum MulticastType {
        NEW_USER_MULTICAST,
        USER_LOGOUT_MULTICAST,
        NEW_CHANNEL_MULTICAST,
        REMOVE_CHANNEL_MULTICAST,
        EDIT_CHANNEL_MULTICAST,
        ACCESS_CHANNEL_MULTICAST,
        NEW_PRIVATE_CHAT_MULTICAST,
        EDIT_PRIVATE_CHAT_MULTICAST
    }

    public enum MessageType {
        CREATE_CHANNEL,
        CHECK_CHANNEL_PASS,
        EDIT_CHANNEL,
        DELETE_CHANNEL,
        ACCESS_CHANNEL,
        LEAVE_CHANNEL,
        SHOW_CHAT,
        SEND_DIRECT_MESSAGE,
        SEND_CHANNEL_MESSAGE,
        SEND_DIRECT_FILE,
        SEND_FILE,
        SHUTDOWN,
        SHOW_ALL_USERS,
        CREATE_PRIVATE_CHAT,
        SHOW_PRIVATE_CHAT
    }

}
