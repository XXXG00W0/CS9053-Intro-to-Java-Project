package chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import encryption.Encryption;

public class ChatServerModel {

    public final static String HELLO = "HELLO";
    public final static String CONNECTED = "CONNECTED";
	public final static String LOGIN = "LOGIN";
	public final static String REGISTER = "REGISTER";
	public final static String OK = "OK";
	public final static String NO_OK = "NO_OK";
    public final static String MSG = "MSG";
    public final static String DISCONNECT = "DISCONNECT";
    public final static String TEST_ALIVE = "TEST_ALIVE";
	public final static String CHANGE_PSWD = "CHANGE_PSWD";
	public final static String CHANGE_USER = "CHANGE_USER";
    public final static String DELETE = "DELETE";
	public final static String[] ACTION_LIST = {LOGIN, REGISTER, CHANGE_PSWD, CHANGE_USER, DELETE};

    private final static int RUNNING = 1;
    private final static int SHUTDOWN = -1;
    private int status = SHUTDOWN;

    private ServerSocket serverSocket;
    private static final String RSA = "RSA";
    private Key privateKey;
    private int clientCount = 0;
    private ArrayList<ClientHandler> clientList = new ArrayList<>();
    private Hashtable<ClientHandler, Thread> clientThreadTable = new Hashtable<>();
    ExecutorService executor;
    private static final Logger logger = Logger.getLogger(ChatClientView.class.getName());

    // private Connection jdbcConnection;
    // Socket socket;
    private ServerDatabase db;

    public ChatServerModel() {
        try{
            db = new ServerDatabase();
        } catch (SQLException | ClassNotFoundException e) {
            System.exit(0);
        } 

        try {
            privateKey = Encryption.readPrivateKey("keypairs/pkcs8_key");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("problem loading private key: " + e.getMessage());
            System.exit(1);
        }

    }

    public void acceptClient() {
        status = RUNNING;
        try {
            serverSocket = new ServerSocket(9898);
            logger.info("Chat server started at " + new Date() + '\n');
            logger.info("Private key:\n" + privateKey.toString() + '\n');
        } catch (IOException e) {
            logger.severe("Cannote initate serverside socket");
            status = SHUTDOWN;
            return;
        }

        while (status == RUNNING) {
            Socket socket;
            try {
                socket = serverSocket.accept();
                clientCount++;
            } catch (IOException ioe) {
                logger.severe("IOException happens during accepting client");
                continue;
            }

            logger.info("Starting thread for client " + clientCount +
                    " at " + new Date() + '\n');

            InetAddress inetAddress = socket.getInetAddress();
            logger.info("Client " + clientCount + "'s host name is "
                    + inetAddress.getHostName() + "\n");
            logger.info("Client " + clientCount + "'s IP Address is "
                    + inetAddress.getHostAddress() + "\n");

            ClientHandler clientHandler = new ClientHandler(socket, clientCount, this);
            synchronized (clientThreadTable) {
                Thread handlerThread = new Thread(clientHandler);
                clientThreadTable.put(clientHandler, handlerThread);
                handlerThread.start();
            }
        }
    }

    class ClientHandler implements Runnable {
        private int timeout = 5000;
        private Socket socket;
        private int clientNum;
        private ChatServerModel server;
        private Key communicationKey;
        DataInputStream fromClient;
        DataOutputStream toClient;
        private boolean authentication_passed = false;
        private String username;
        

        public ClientHandler(Socket socket, int clientNum, ChatServerModel server) {
            this.socket = socket;
            this.clientNum = clientNum;
            this.server = server;
        }

        @Override
        public void run() {
            Boolean handshakeSuccess = handshake();
            if (!handshakeSuccess) {
                connectionCleanup(fromClient, toClient, socket);
                return;
            }

            // Read and broadcast encrypted message
            while (true) {
                String clientString = receiveMessage(fromClient, communicationKey);
                logger.info("Received from client "+ clientNum+": " + clientString);
                if (clientString == null)break;
                boolean success = parseUserCredential(clientString);
                if (success) continue;
                else if (clientString.equals(DISCONNECT)) {
                    logger.info("Client " + clientNum +" "+this.username+" has left the chat\n");
                    server.broadcast("User <"+this.username+"> has left the chat\n", this);
                    break;
                } else if (clientString.equals(TEST_ALIVE)) {
                    logger.info("Client " + clientNum +" "+this.username+" connection is alive\n");
                } else if (clientString.startsWith(MSG)){
                    logger.info("Client " + clientNum +" "+this.username+"message: " + clientString);
                    server.broadcast(clientString.substring(MSG.length()+1), this);
                }else{
                    logger.warning("Invaid string received: "+clientString);
                }
            }
            connectionCleanup(fromClient, toClient, socket);
        }

        private boolean parseUserCredential(String rawString){
            /* Login string should be in the form of LOGIN <username> <password> */
            String[] stringList = rawString.split(" ");
            String action = stringList[0];
            boolean isAction = Arrays.stream(ACTION_LIST).anyMatch(e -> e.equals(action));
            if (!isAction){
                logger.warning("\""+rawString +"\" contains invalid action keyword");
                return false;
            }
            
            if (stringList.length >= 0 & stringList.length < 3){ // 0 1 2
                logger.warning("\""+rawString +"\" is not a valid raw string for user credential");
                sendMessage(toClient, communicationKey, action+" "+NO_OK);
                return false;
            }
            String username  = stringList[1];
            String password  = stringList[2];
            User user = new User(username, password);
            boolean success = false;
            if (action.equals(LOGIN)){
                success = handleLogin(user);
            }else if (action.equals(REGISTER)){
                success = handleRegister(user);
            } else if (action.equals(DELETE)){
                success = handleDelete(user);
            }else if (action.equals(CHANGE_PSWD) & stringList.length == 4){
                success = handleChangePassword(user, stringList[3]);
            }else if (action.equals(CHANGE_USER) & stringList.length == 4){
                success = handleChangeUsername(user, stringList[3]);
            }

            if (success){
                sendMessage(toClient, communicationKey, action+" "+OK);
            }else{
                sendMessage(toClient, communicationKey, action+" "+NO_OK);
            }
            // Parse 
            return true;
        }

        private boolean handleRegister(User user){
            if (!(user instanceof User)){
                return false;
            }
            String username  = user.getUsername();
            String password  = user.getPassword();
            return db.createUser(username, password);
        }
        
        private boolean handleLogin(User user) {
            if(authentication_passed){
                logger.info("Already logged in");
                return true;
            }
            if ( !(user instanceof User)){
                return false;
            }
            String username  = user.getUsername();
            String password  = user.getPassword();
            if (db.checkUsernameAndPassword(username, password)){
                authentication_passed = true;
                logger.info("Login with username "+username+" and password "+password);
                this.username = username;
                return true;
            }else{
                authentication_passed = false;
                logger.info("Login failed with username "+username+" and password "+password);
                return false;
            }  
        }

        private boolean handleDelete(User user){
            String username  = user.getUsername();
            String password  = user.getPassword();
            if (db.checkUsernameAndPassword(username, password)){
                logger.info("Delete account with username "+username+" and password "+password);
                this.username = "";
                db.deleteUser(username, password);
                authentication_passed = false;
                return true;
            }else{
                logger.info("Login failed with username "+username+" and password "+password);
                return false;
            }
        }

        private boolean handleChangePassword(User user, String newPassword){
            String username  = user.getUsername();
            String password  = user.getPassword();
            boolean res = db.updatePassword(username, password, newPassword);
            logger.info("User "+username+" Change password from "+password+" to "+newPassword);
            logger.info("Action successful? "+res);               
            return res;
        }

        private boolean handleChangeUsername(User user, String newUsername){
            String username  = user.getUsername();
            String password  = user.getPassword();
            boolean res = db.updateUsername(username, password, newUsername);
            logger.info("User "+username+" change to "+newUsername+" with password "+password);
            logger.info("Action successful? "+res);
            if (res)
                this.username = username;              
            return res;
        }

        public boolean handshake() {
            try {
                fromClient = new DataInputStream(socket.getInputStream());
                toClient = new DataOutputStream(socket.getOutputStream());
                logger.info("Waiting for client handshake...\n");
                socket.setSoTimeout(0); // set timeout during handshake

                // Read HELLO
                String helloString = fromClient.readUTF();
                logger.info("[Handshake] Receive from client" + clientNum + ": " + helloString + '\n');
                if (!helloString.equals("HELLO")) {
                    logger.info("[Handshake] Illegal handshake procedure: " + helloString + '\n');

                    return false;
                }
                // TimeUnit.SECONDS.sleep(6);
                // Send CONNECTED
                toClient.writeUTF("CONNECTED");
                toClient.flush();

                int length = fromClient.readInt();
                if (length > 0) {
                    byte[] encryptedASEKeySeed = new byte[length];
                    fromClient.readFully(encryptedASEKeySeed);
                    byte[] ASEKeySeed = Encryption.pkDecrypt(privateKey, encryptedASEKeySeed);
                    communicationKey = Encryption.generateAESKey(ASEKeySeed);
                    logger.info(
                            "[Handshake] Receive from client" + clientNum + " w/ ASE key seed: " + ASEKeySeed + '\n');
                } else {
                    logger.info("[Handshake] Illegal ASE key seed length: " + length + '\n');
                    return false;
                }

                socket.setSoTimeout(0);// return to no-timeout
                logger.info("Handshake successful\n");
                return true;

            } catch (SocketTimeoutException ste) {
                ste.printStackTrace();
                logger.severe("Handshake timeout after " + timeout / 1000 + " seconds");
            } catch (SocketException se) {
                se.printStackTrace();
                logger.severe("Connection reset with client " + clientNum + '\n');
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (GeneralSecurityException e) {
                logger.severe("Decryption error with client " + clientNum + '\n');
                e.printStackTrace();
            }
            return false;
        }

        public void connectionCleanup(DataInputStream inputStream, DataOutputStream outputStream, Socket socket) {

            logger.info("Server Cleanup start with client no" + clientNum);
            synchronized (server.clientThreadTable) {
                server.clientThreadTable.remove(this);
            }
            try {
                outputStream.writeUTF("DISCONNECT");
                outputStream.flush();
                inputStream.close();
                outputStream.close();
                socket.close();
                logger.info("Server Cleanup complete with client no" + clientNum);
            } catch (IOException e) {
                e.printStackTrace();
                logger.warning("IOException happened when trying to clean up");
            }
            ;
        }
    }

    public void shutdownCleanup() {
        /* Disconnect all connections and shutdown server */
        logger.info("Server shutdown in progress");
        status = SHUTDOWN;
        synchronized (clientThreadTable) {
            for (Entry<ClientHandler, Thread> entry : clientThreadTable.entrySet()) {
                ClientHandler handler = entry.getKey();
                Thread handlerThread = entry.getValue();
                handler.connectionCleanup(handler.fromClient, handler.toClient, handler.socket);
                handlerThread.interrupt();
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.info("Encounter IOException while server shutdown");
        }
        logger.info("Server shutdown");
        System.exit(0);
    }

    public void broadcast(String message, ClientHandler thisHandler) {
        logger.info("Broadcast client "+thisHandler.clientNum+" message: "+message);
        for (Entry<ClientHandler, Thread> entry : clientThreadTable.entrySet()) {
            ClientHandler handler = entry.getKey();
            if (!handler.equals(thisHandler)) {
                sendMessage(handler.toClient, handler.communicationKey,
                        "User <" + thisHandler.username + ">: " + message);
            }
        }
    }

    public void sendMessage(DataOutputStream outStream, Key key, String message) {
        try {
            logger.info("Send message to client: "+message);
            String encryptedMessage = Encryption.encrypt(key, message);
            outStream.writeUTF(encryptedMessage);
            outStream.flush();
        } catch (IOException ioe) {
            logger.warning("Cannot send message with stream: " + outStream + '\n');
        } catch (GeneralSecurityException e) {
            logger.warning("Cannot encrypt message with key: " + key + '\n');
        }
    }

    public String receiveMessage(DataInputStream inStream, Key key) {
        String decodedString = null;
        try {
            String encryptedMessage = inStream.readUTF();
            if (encryptedMessage.equals(TEST_ALIVE)) {
                logger.info("Client sends test alive");
                return encryptedMessage;
            } else if (encryptedMessage.equals(DISCONNECT)) {
                logger.info("Client sends disconnect");
                return encryptedMessage;
            }
            decodedString = Encryption.decrypt(key, encryptedMessage);
            logger.info("Client sends:");
        } catch (IOException ioe) {
            logger.warning("Cannot receive message with inStream: " + inStream + '\n');
            // throw(ioe);
        } catch (GeneralSecurityException e) {
            logger.warning("Cannot decrypt message with key: " + key + '\n');
        }
        return decodedString;
    }

}
