package chat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.*;
import javax.crypto.*;
import javax.swing.*;

import encryption.Encryption;

public class ChatClient extends JFrame {

	private JTextArea textArea;
	private JTextField textField;
	private JScrollPane scrollPane;
	private Socket socket = null;
	private JMenuBar menuBar;
	private JMenu menu;

	private final static String HELLO = "HELLO";
	private final static String CONNECTED = "CONNECTED";
	private final static String DISCONNECT = "DISCONNECT";
	private final static String TEST_ALIVE = "TEST_ALIVE";

	private DataOutputStream toServer = null;
	private DataInputStream fromServer = null;
	private Thread receiverThread;

	private int timeout = 5000;
	private static final String RSA = "RSA";
	private static final String SERVER_PUBLIC_KEY = "MIGeMA0GCSqGSIb3DQEBAQUAA4GMADCBiAKBgGk9wUQ4G9PChyL5SUkCyuHjTNOglEy5h4KEi0xpgjxi/UbIH27NXLXOr94JP1N5pa1BbaVSxlvpuCDF0jF9jlZw5IbBg1OW2R1zUACK+NrUIAYHWtagG7KB/YcyNXHOZ6Icv2lXXd7MbIao3ShrUVXo3u+5BJFCEibd8a/JD/KpAgMBAAE=";
	private PublicKey serverPublicKey;
	private Key communicationKey;
	private byte[] ASEKeySeed;

	public ChatClient() {
		super("Chat Client");
		try {
			serverPublicKey = Encryption.readPublicKey(SERVER_PUBLIC_KEY);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("error getting server public key: " + e.getMessage());
		}
		ASEKeySeed = Encryption.generateSeed();
		communicationKey = Encryption.generateAESKey(ASEKeySeed);
		JMenuItem connectItem = new JMenuItem("Connect");
		connectItem.addActionListener(new ConnectionListener());
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener((e) -> {
			try {
				receiverThread.interrupt();
				connectionCleanup();
				textArea.append("connection closed");
			} catch (Exception e1) {
				System.err.println("error");
			} finally {
				System.exit(0);
			}
		});
		menu = new JMenu("File");
		menu.add(connectItem);
		menu.add(exitItem);
		menuBar = new JMenuBar();
		menuBar.add(menu);
		this.setJMenuBar(menuBar);
		// pack();

		textField = new JTextField(1);
		textField.addActionListener(new textFieldListener());
		textArea = new JTextArea(10, 10);
		textArea.setEditable(false);
		// textArea.addActionListener(new textAreaListener());
		textArea.setRows(1);
		scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(textField, BorderLayout.SOUTH);
		setSize(400, 400);
	}

	class ConnectionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				// Check if there is an active connection
				if (testAlive() == true){return;}
				// No active connection, then start new connection
				textArea.append("Initiating connection to server...\n");
				socket = new Socket("localhost", 9898);

				textArea.append("connected\n");

				fromServer = new DataInputStream(socket.getInputStream());
				toServer = new DataOutputStream(socket.getOutputStream());

				// Handshake procedure
				textArea.append("Initiating handshake...\n");
				socket.setSoTimeout(timeout); // set timeout during handshake
				// Send HELLO
				toServer.writeUTF(HELLO);
				toServer.flush();

				// TimeUnit.SECONDS.sleep(6);

				// Wait CONNECTED
				String replyString = fromServer.readUTF();
				textArea.append("Receive from server: " + replyString + '\n');

				// If not receive string CONNECTED
				if (!replyString.equals(CONNECTED)) {
					textArea.append("Illegal handshake procedure: " + replyString + '\n');
					connectionCleanup();
					return;
				}
				// Send ASE key seed
				byte[] encryptedASEKeySeed = Encryption.pkEncrypt(serverPublicKey, ASEKeySeed);
				// toServer.writeInt(-1);
				toServer.writeInt(encryptedASEKeySeed.length);
				toServer.write(encryptedASEKeySeed);
				toServer.flush();

				socket.setSoTimeout(0);// return to no-timeout
				// Handshake Successful
				textArea.append("Handshake successful\n");
				// Start thread to receive message from server
				receiverThread = new Thread(() -> receiveFromServer());
				receiverThread.start();
			} catch (SocketException se) {
				se.printStackTrace();
				textArea.append("Connection Failed\n");
				connectionCleanup();
			} catch (SocketTimeoutException ste) {
				ste.printStackTrace();
				textArea.append("Handshake timeout after " + timeout / 1000 + " seconds");
				connectionCleanup();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				textArea.append("IOException\n");
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
					| BadPaddingException de) {
				textArea.append("Handshake encryption failed\n");
				de.printStackTrace();
				// } catch (InterruptedException e1) {
				// e1.printStackTrace();
			}
		}
	}

	class textFieldListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				// fromServer = new DataInputStream(socket.getInputStream());
				// toServer = new DataOutputStream(socket.getOutputStream());

				String text = textField.getText().trim();
				if (text.length() != 0) {
					sendMessage(toServer, communicationKey, text + '\n');
					textArea.append(text + '\n');
					toServer.flush();
					textField.setText("");
				} else {
					textArea.append("Cannot send empty text!\n");
				}

			} catch (IOException ex) {
				textArea.append(ex.toString() + '\n');
			}
		}
	}

	private void receiveFromServer() {
		while (!Thread.currentThread().isInterrupted()) {
			String text = receiveMessage(fromServer, communicationKey);
			textArea.append(text);
			if (text.equals(DISCONNECT)) {
				connectionCleanup();
				return;
			}
		}
	}

	private void connectionCleanup() {
		try {
			toServer.writeUTF(DISCONNECT);
			toServer.flush();
			fromServer.close();
			toServer.close();
			socket.close();
			System.out.println("Client Cleanup complete");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("IOException happened when trying to clean up");
			textArea.append("IOException happened when trying to clean up");
		};
	}

	public Boolean testAlive(){
		if(socket == null){
			return false;
		}
		else{
			try{
				toServer.writeUTF(TEST_ALIVE);
				toServer.flush();
				textArea.append("Already connected\n");
				return true;
			}catch(IOException ioe){
				// textArea.append("Already connected");
				textArea.append("Connection is dropped");
				return false;
			}
		}
	}

	public void sendMessage(DataOutputStream outStream, Key key, String message) {
		try {
			if (socket.isClosed()){
				textArea.append("Connection is closed, cannot send message\n");
				return;
			}
			String encryptedMessage = Encryption.encrypt(key, message);
			outStream.writeUTF(encryptedMessage);
			outStream.flush();
		} catch (IOException ioe) {
			textArea.append("Cannot send message with stream: " + outStream + '\n');
		} catch (Exception e) {
			textArea.append("Cannot encrypt message with key: " + key + '\n');
		}
	}

	public String receiveMessage(DataInputStream inStream, Key key) {
		String decodedString = null;
		try {
			String encryptedMessage = inStream.readUTF();
			// if plain text == DISCONNECT
			if (encryptedMessage.equals(DISCONNECT)) {
				decodedString = encryptedMessage;
			} else {
				decodedString = Encryption.decrypt(key, encryptedMessage);
			}
		} catch (IOException ioe) {
			textArea.append("Cannot receive message with inStream: " + inStream + '\n');
		} catch (Exception e) {
			textArea.append("Cannot decrypt message with key: " + key + '\n');
		}
		return decodedString;
	}

	public static void main(String[] args) {
		WelcomeFrame welcomeFrame = new WelcomeFrame();
		ChatClient chatClient = new ChatClient();
		chatClient.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		chatClient.setVisible(true);
	}
}
