package chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.logging.Logger;

import encryption.Encryption;

public class ChatServerController implements Runnable {

    private ChatServerModel model;
    private ChatServerView view;

    private final static String HELLO = "HELLO";
	private final static String CONNECTED = "CONNECTED";
	private final static String DISCONNECT = "DISCONNECT";
	private final static String TEST_ALIVE = "TEST_ALIVE";

	private static final Logger logger = Logger.getLogger(ChatClientView.class.getName());

    public ChatServerController(ChatServerView view, ChatServerModel model){
        this.model = model;
        this.view = view;
        view.addExitItemListener(new ExitListener());

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
				return encryptedMessage;
			} else if (encryptedMessage.equals(DISCONNECT)) {
				return encryptedMessage;
			}
			decodedString = Encryption.decrypt(key, encryptedMessage);
		} catch (IOException ioe) {
			logger.warning("Cannot receive message with inStream: " + inStream + '\n');
		} catch (GeneralSecurityException e) {
			logger.warning("Cannot decrypt message with key: " + key + '\n');
		}
		return decodedString;
	}

    @Override
    public void run(){
        view.setVisible(true);
        model.acceptClient();
    }
    
    class ExitListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            model.shutdownCleanup();
        }

    }

    public static void main(String[] args) {
        ChatServerView view = new ChatServerView();
        ChatServerModel model = new ChatServerModel();
        ChatServerController controller = new ChatServerController(view, model);
        Thread controllerThread = new Thread(controller);
        controllerThread.start();
    }

}
