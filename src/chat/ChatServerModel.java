package chat;

import java.awt.BorderLayout;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import encryption.Encryption;

public class ChatServerModel {

    private final static String HELLO = "HELLO";
    private final static String CONNECTED = "CONNECTED";
    private final static String DISCONNECT = "DISCONNECT";
    private final static String TEST_ALIVE = "TEST_ALIVE";

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

    // Socket socket;

    public ChatServerModel() {

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
                if (clientString == null) {
                    break;
                } else if (clientString.equals(DISCONNECT)) {
                    logger.info("Client " + clientNum + " has left the chat\n");
                    server.broadcast("Client " + clientNum + " has left the chat\n", this);
                    break;
                } else if (clientString.equals(TEST_ALIVE)) {
                    logger.info("Client " + clientNum + " connection is alive\n");
                } else {
                    // logger.info("Client " + clientNum + ": " + clientString);
                    server.broadcast(clientString, this);
                }
            }
            connectionCleanup(fromClient, toClient, socket);
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
                        "Client " + thisHandler.clientNum + ": " + message);
            }
        }
    }

    public void sendMessage(DataOutputStream outStream, Key key, String message) {
        try {
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
        } catch (GeneralSecurityException e) {
            logger.warning("Cannot decrypt message with key: " + key + '\n');
        }
        return decodedString;
    }

}
