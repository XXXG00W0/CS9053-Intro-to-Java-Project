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
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;

import encryption.Encryption;

import java.security.*;

public class ChatServer extends JFrame implements Runnable {

	JTextArea textArea;
	Socket socket;
	JMenuBar menuBar;
	JMenu menu;

	private final static String HELLO = "HELLO";
	private final static String CONNECTED = "CONNECTED";
	private final static String DISCONNECT = "DISCONNECT";
	private final static String TEST_ALIVE = "TEST_ALIVE";

	private ServerSocket serverSocket;
	private static final String RSA = "RSA";
	private Key privateKey;
	private int clientCount = 0;
	ArrayList<ClientHandler> clientList = new ArrayList<>();
	ExecutorService executor;

	private static final Logger logger = Logger.getLogger(ChatClientView.class.getName());

	public ChatServer() {

		super("Chat Server");

		try {
			privateKey = Encryption.readPrivateKey("keypairs/pkcs8_key");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("problem loading private key: " + e.getMessage());
			System.exit(1);
		}

		menu = new JMenu("File");
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener((e) -> {
			try {
				for (ClientHandler handler : clientList) {
					handler.connectionCleanup(handler.fromClient, handler.toClient, handler.socket);
					executor.shutdownNow();
				}
				serverSocket.close();
				logger.info("Server shutdown");
			} catch (IOException ioe) {
				logger.info("Encounter IOException while server shutdown");
			} finally {
				System.exit(0);
			}
		});
		menu.add(exitItem);
		menuBar = new JMenuBar();
		menuBar.add(menu);
		this.setJMenuBar(menuBar);

		textArea = new JTextArea(10, 10);
		textArea.setEditable(false);
		JScrollPane sp = new JScrollPane(textArea);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.add(sp, BorderLayout.CENTER);
		setSize(400, 400);
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		try {
			serverSocket = new ServerSocket(9898);
			textArea.append("Chat server started at " + new Date() + '\n');
			textArea.append("Private key:\n" + privateKey.toString() + '\n');
			executor = Executors.newFixedThreadPool(128);

			while (true) {
				Socket socket = serverSocket.accept();
				clientCount++;

				textArea.append("Starting thread for client " + clientCount +
						" at " + new Date() + '\n');

				InetAddress inetAddress = socket.getInetAddress();
				textArea.append("Client " + clientCount + "'s host name is "
						+ inetAddress.getHostName() + "\n");
				textArea.append("Client " + clientCount + "'s IP Address is "
						+ inetAddress.getHostAddress() + "\n");

				ClientHandler clientHandler = new ClientHandler(socket, clientCount, this);
				clientList.add(clientHandler);
				executor.execute(clientHandler);

				// new Thread(clientHandler).start();
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			// socket.close();
		}
	}

	class ClientHandler implements Runnable {
		private int timeout = 5000;
		private Socket socket;
		private int clientNum;
		private ChatServer server;
		private Key communicationKey;
		DataInputStream fromClient;
		DataOutputStream toClient;

		public ClientHandler(Socket socket, int clientNum, ChatServer server) {
			this.socket = socket;
			this.clientNum = clientNum;
			this.server = server;
		}

		public void run() {
			try {
				fromClient = new DataInputStream(socket.getInputStream());
				toClient = new DataOutputStream(socket.getOutputStream());
				textArea.append("Waiting for client handshake...\n");

				socket.setSoTimeout(0); // set timeout during handshake

				// Read HELLO
				String helloString = fromClient.readUTF();
				textArea.append("[Handshake] Receive from client" + clientNum + ": " + helloString + '\n');
				if (!helloString.equals("HELLO")) {
					textArea.append("[Handshake] Illegal handshake procedure: " + helloString + '\n');
					connectionCleanup(fromClient, toClient, socket);
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
					textArea.append(
							"[Handshake] Receive from client" + clientNum + " w/ ASE key seed: " + ASEKeySeed + '\n');
				} else {
					textArea.append("[Handshake] Illegal ASE key seed length: " + length + '\n');
					connectionCleanup(fromClient, toClient, socket);
				}

				socket.setSoTimeout(0);// return to no-timeout
				textArea.append("Handshake successful\n");

				// Read and broadcast encrypted message
				while (true) {
					String clientString = receiveMessage(fromClient, communicationKey);
					logger.info("Received " + clientString);
					if (clientString == null) {

					} else if (clientString.equals(DISCONNECT)) {
						textArea.append("Client " + clientNum + " has left the chat\n");
						server.broadcast("Client " + clientNum + " has left the chat\n", this);
						break;
					} else if (clientString.equals(TEST_ALIVE)) {
						textArea.append("Client " + clientNum + " connection is alive\n");
					} else {
						textArea.append("Client " + clientNum + ": " + clientString);
						server.broadcast(clientString, this);
					}
				}
			} catch (EOFException eofe) {
				eofe.printStackTrace();
			} catch (SocketTimeoutException ste) {
				ste.printStackTrace();
				textArea.append("Handshake timeout after " + timeout / 1000 + " seconds");
			} catch (SocketException se) {
				se.printStackTrace();
				textArea.append("Connection reset with client " + clientNum + '\n');
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
					| BadPaddingException e) {
				textArea.append("Decryption error with client " + clientNum + '\n');
				e.printStackTrace();
			} finally {
				connectionCleanup(fromClient, toClient, socket);
			}
		}

		public void connectionCleanup(DataInputStream inputStream, DataOutputStream outputStream, Socket socket) {

			synchronized (server.clientList) {
				server.clientList.remove(this);
			}
			try {
				outputStream.writeUTF("DISCONNECT");
				outputStream.flush();
				inputStream.close();
				outputStream.close();
				socket.close();
				System.out.println("Server Cleanup complete");
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("IOException happened when trying to clean up");
				textArea.append("IOException happened when trying to clean up");
			}
			;
		}

	}

	public void broadcast(String message, ClientHandler thisHandler) {
		for (ClientHandler handler : this.clientList) {
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
			textArea.append("Cannot send message with stream: " + outStream + '\n');
		} catch (GeneralSecurityException e) {
			textArea.append("Cannot encrypt message with key: " + key + '\n');
		}
	}

	public String receiveMessage(DataInputStream inStream, Key key) {
		String decodedString = null;
		try {
			String encryptedMessage = inStream.readUTF();
			if (encryptedMessage.equals(TEST_ALIVE)) {
				return encryptedMessage;
			} else if (encryptedMessage.equals(DISCONNECT)) {
				return encryptedMessage;
			}
			decodedString = Encryption.decrypt(key, encryptedMessage);
		} catch (IOException ioe) {
			textArea.append("Cannot receive message with inStream: " + inStream + '\n');
		} catch (GeneralSecurityException e) {
			textArea.append("Cannot decrypt message with key: " + key + '\n');
		}
		return decodedString;
	}

	public static void main(String[] args) {
		ChatServer chatServer = new ChatServer();
		chatServer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		chatServer.setVisible(true);
	}

}
