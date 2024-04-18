package chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import encryption.Encryption;

public class ChatClientModel implements Runnable {

	public final static String HELLO = "HELLO";
	public final static String CONNECTED = "CONNECTED";
	public final static String DISCONNECT = "DISCONNECT";
	public final static String TEST_ALIVE = "TEST_ALIVE";

	private DataOutputStream toServer = null;
	private DataInputStream fromServer = null;
	private Socket socket = null;
	private Thread sender;
	private Thread receiver;

	public final static int QUITTING = -3;
	public final static int NOT_CONNECTED = 0;
	public final static int HANDSHAKE_FAILED = -1;
	public final static int HANDSHAKE_OK = 1;
	public final static int INCORRECT_CERTIFICATION = -2;
	public final static int CERTIFICATED = 2;
	public final static ArrayList<Integer> statusList = new ArrayList<>(Arrays.asList(QUITTING, NOT_CONNECTED,
			HANDSHAKE_FAILED, HANDSHAKE_OK, INCORRECT_CERTIFICATION, CERTIFICATED));
	private int status = NOT_CONNECTED;

	private int timeout = 5000;
	private static final String RSA = "RSA";
	private static final String SERVER_PUBLIC_KEY = "MIGeMA0GCSqGSIb3DQEBAQUAA4GMADCBiAKBgGk9wUQ4G9PChyL5SUkCyuHjTNOglEy5h4KEi0xpgjxi/UbIH27NXLXOr94JP1N5pa1BbaVSxlvpuCDF0jF9jlZw5IbBg1OW2R1zUACK+NrUIAYHWtagG7KB/YcyNXHOZ6Icv2lXXd7MbIao3ShrUVXo3u+5BJFCEibd8a/JD/KpAgMBAAE=";
	private PublicKey serverPublicKey;
	private Key communicationKey;
	private byte[] ASEKeySeed;

	private static final Logger logger = Logger.getLogger(ChatClientView.class.getName());

	public ChatClientModel() {
		try {
			serverPublicKey = Encryption.readPublicKey(SERVER_PUBLIC_KEY);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("error getting server public key: " + e.getMessage());
		}
		ASEKeySeed = Encryption.generateSeed();
		communicationKey = Encryption.generateAESKey(ASEKeySeed);
	}

	@Override
	public void run() {

		// while(!Thread.currentThread().isInterrupted()){

		// // Handshake
		// boolean handshakeResult = handshake();
		// if(!handshakeResult){
		// logger.severe("Handshake failed");
		// Thread.currentThread().interrupt();
		// }
		// }

	}

	public boolean handshake() {
		try {
			status = NOT_CONNECTED;
			logger.info("Initiating connection to server...\n");
			socket = new Socket("localhost", 9898);
			fromServer = new DataInputStream(socket.getInputStream());
			toServer = new DataOutputStream(socket.getOutputStream());
			logger.info("Initiating handshake...\n");
			socket.setSoTimeout(0);

			// Send HELLO
			sendString(toServer, null, HELLO);
			// Wait CONNECTED
			String replyString = fromServer.readUTF();
			logger.info("Receive from server: " + replyString + '\n');

			// If not receive string CONNECTED
			if (!replyString.equals(CONNECTED)) {
				logger.info("Illegal handshake procedure: " + replyString + '\n');
				status = HANDSHAKE_FAILED;
				connectionCleanup();
				return false;
			}

			// Send ASE key seed
			byte[] encryptedASEKeySeed = Encryption.pkEncrypt(serverPublicKey, ASEKeySeed);
			sendInt(toServer, encryptedASEKeySeed.length);
			sendByte(toServer, null, encryptedASEKeySeed);

			socket.setSoTimeout(0);
			logger.info("Handshake successful\n");
			status = HANDSHAKE_OK;
			return true;

		} catch (UnknownHostException e) {
			logger.severe(e.toString() + " Unknown host: " + socket.getChannel());
			System.err.println(e.getStackTrace());
		} catch (IOException e) {
			System.err.println(e.getStackTrace());
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException de) {
			logger.warning(de.toString() + " Cannot encrypt seed with key: " + serverPublicKey + '\n');
		} catch (Exception e) {
			logger.severe("Unknown exception: " + e.toString());
			e.printStackTrace();
		}
		status = HANDSHAKE_FAILED;
		return false;
	}

	public boolean login(String username, String pswd) {
		// to-do verify username pswd combination via database connection
		status = CERTIFICATED;
		return true;
	}

	public void connectionCleanup() {
		try {
			sendString(toServer, null, DISCONNECT);
			fromServer.close();
			toServer.close();
			socket.close();
			System.out.println("Client Cleanup complete");
		} catch (IOException e) {
			e.printStackTrace();
			logger.warning("IOException happened when trying to clean up");
		} catch (GeneralSecurityException de) {
			logger.warning(de.toString() + " happened when trying to clean up");
		}
		;
	}

	public Boolean testAlive() {
		if (socket == null) {
			return false;
		} else {
			try {
				toServer.writeUTF(TEST_ALIVE);
				toServer.flush();
				logger.info("Already connected\n");
				return true;
			} catch (IOException ioe) {
				// logger.info("Already connected");
				logger.info("Connection is dropped");
				return false;
			}
		}
	}

	public void sendString(DataOutputStream outStream, Key key, String message)
			throws IOException, GeneralSecurityException {
		try {
			if (key != null) {
				message = Encryption.encrypt(key, message);
			}
			outStream.writeUTF(message);
			outStream.flush();
		} catch (IOException ioe) {
			logger.warning(
					"[sendString] Cannot send string with stream: " + outStream + " or string " + message + '\n');
			throw ioe;
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | InvalidAlgorithmParameterException de) {
			logger.warning("[sendString] " + de.toString() + " Cannot encrypt string with key: " + key + '\n');
			throw de;
		}
	}

	public void sendByte(DataOutputStream outStream, Key key, byte[] message)
			throws IOException, GeneralSecurityException {
		try {
			if (key != null) {
				message = Encryption.pkEncrypt(key, message);
			}
			outStream.write(message);
			outStream.flush();
		} catch (IOException ioe) {
			logger.warning("Cannot send byte[] with stream: " + outStream + " or message " + message + '\n');
			throw ioe;
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException de) {
			logger.warning(de.toString() + " Cannot encrypt byte[] with key: " + key + '\n');
			throw de;
		}
	}

	public void sendInt(DataOutputStream outStream, int integer) throws IOException {
		try {
			outStream.writeInt(integer);
			outStream.flush();
		} catch (IOException ioe) {
			logger.warning("Cannot send integer with stream: " + outStream + " or integer " + integer + '\n');
			throw ioe;
		}
	}

	public void sendMessage(String message, Boolean encrypt)
			throws IOException, GeneralSecurityException {
		Key key = encrypt ? communicationKey : null;
		sendString(toServer, key, message);
	}

	public String receiveMessage(Boolean encrypt) {
		Key key = encrypt ? communicationKey : null;
		return receiveMessage(fromServer, key);
	}

	private String receiveMessage(DataInputStream inStream, Key key) {
		String decodedString = null;
		try {
			String encryptedMessage = inStream.readUTF();
			if (key == null) {
				decodedString = encryptedMessage;
			} else if (encryptedMessage.equals(DISCONNECT)) {
				decodedString = encryptedMessage;
			} else {
				decodedString = Encryption.decrypt(key, encryptedMessage);
			}
		} catch (IOException ioe) {
			logger.warning("Cannot receive message with inStream: " + inStream + '\n');
		} catch (GeneralSecurityException de) {
			logger.warning(de.toString() + " Cannot decrypt message with key: " + key + '\n');
		}
		return decodedString;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status){
		if (statusList.contains(status)){
			this.status = status;
		}else{
			logger.warning("Cannot set status: "+status);
		}

	}

}
