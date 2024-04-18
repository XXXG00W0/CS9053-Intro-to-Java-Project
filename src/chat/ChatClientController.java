package chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;

public class ChatClientController implements Runnable {

    private ChatClientModel model;
    private ChatClientView view;

    private Thread senderThread;
    private Thread receiverThread;

    private LoginListener loginListener;

    private static final Logger logger = Logger.getLogger(ChatClientView.class.getName());

    public ChatClientController(ChatClientModel model, ChatClientView view) {
        this.model = model;
        this.view = view;

        view.addExitItemListener(new ExitListener());

        loginListener = new LoginListener();
        view.addWelcomeLoginListener(loginListener);
        view.addTextFieldListener(new textFieldListener());

        view.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                model.connectionCleanup();
            }
        });

        Timer heartBeatTimer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    model.sendMessage(ChatClientModel.TEST_ALIVE, true);
                    logger.info("Heartbeat message sent");
                } catch (IOException | GeneralSecurityException ex) {
                    logger.severe("Failed to send heartbeat, assuming disconnection " + ex.toString());
                    view.appendTextArea("It seems that you have gone offline, better check your connection.");
                    ((Timer) e.getSource()).stop();
                    model.connectionCleanup();
                    // Ask user to re-login
                    loginListener.login();
                    loginListener.showCountdownPrompt();
                }
            }
        });
        // heartBeatTimer.start();
    }

    private void receiveFromServer() {
        while (!Thread.currentThread().isInterrupted()) {
            String text = model.receiveMessage(true);
            // put
            if (text.equals(ChatClientModel.DISCONNECT)) {
                model.connectionCleanup();
                view.appendTextArea("Server disconnected\n");
                break;
            }
            if (model.getStatus() != ChatClientModel.CERTIFICATED) {
                view.appendTextArea("Welcome\n");
                logger.info(text);
            }
            view.appendTextArea(text);
        }
    }

    class textFieldListener implements ActionListener {

        private String text;

        @Override
        public void actionPerformed(ActionEvent e) {
            // fromServer = new DataInputStream(socket.getInputStream());
            // toServer = new DataOutputStream(socket.getOutputStream());
            try {
                text = view.getTextField().trim();
                if (text.length() != 0) {
                    model.sendMessage(text + '\n', true);
                    view.appendTextArea(text + '\n');
                    view.clearTextField();
                } else {
                    view.appendTextArea("Cannot send empty text!\n");
                }
            } catch (IOException ioe) {
                view.appendTextArea("Cannot send string with string " + text + '\n');
            } catch (GeneralSecurityException de) {
                view.appendTextArea("Cannot encrypt string\n");
            }
        }
    }

    private class ExitListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e){
            try{
                receiverThread.interrupt();
                model.connectionCleanup();
                logger.info("connection closed");
            }finally{
                view.dispose();
                System.exit(0);
            }
        }
    }

    public class LoginListener implements ActionListener {
        JOptionPane pane;
        JDialog dialog;
        JLabel countdownLabel = new JLabel();
        int count = 3;
        String message = "";

        // String message = model.getStatus() == CERTIFICATED ? "Login
        // Successful!" : "Login Failed...";

        public LoginListener() {
            pane = new JOptionPane(countdownLabel, JOptionPane.INFORMATION_MESSAGE);
            countdownLabel.setText(message + " (" + count + ")");
            dialog = pane.createDialog(null, "Login prompt");
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setModal(false); // set dialog to non modal
        }

        public void login() {
            if (model.getStatus() == ChatClientModel.NOT_CONNECTED) {
                model.handshake();
            }
            if (model.getStatus() == ChatClientModel.HANDSHAKE_FAILED) {
                message = "Handshake failed";
                model.setStatus(ChatClientModel.NOT_CONNECTED);
            } else if (model.getStatus() == ChatClientModel.HANDSHAKE_OK) {
                // proceed to certification stage
                model.login(view.getUsername(), view.getPassword());
                if (model.getStatus() == ChatClientModel.INCORRECT_CERTIFICATION) {
                    message = "Incorrect username / password";
                } else if (model.getStatus() == ChatClientModel.CERTIFICATED) {
                    message = "Connected!";
                    // dispose welcome window after logged in
                    view.closeWelcomeFrame();
                } else {
                    message = "Illegal Status: " + model.getStatus() +'\n';
                    logger.severe(message);
                }
            } else {
                message = "Illegal Status: " + model.getStatus() +'\n';
                logger.severe(message);
            }
        }

        public void showCountdownPrompt() {
            // countdownLabel.setText(message + " (" + count + ")");
            countdownLabel.setText(message);
            dialog.setVisible(true);
            // set timer that update countdown label every 1000 ms / 1 sec
            // Timer timer = new Timer(1000, new ActionListener() {
            //     int countdown = count;

            //     @Override
            //     public void actionPerformed(ActionEvent e) {
            //         if (countdown > 0) {
            //             countdown--;
            //             updateCountdownLabel(countdownLabel, countdown);
            //         } else {
            //             ((Timer) e.getSource()).stop();
            //             dialog.dispose();
            //         }
            //     }
            // });
            // timer.start();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            login();
            showCountdownPrompt();
        }

        private void updateCountdownLabel(JLabel label, int seconds) {
            label.setText(message + " (" + seconds + ")");
        }
    }

    @Override
    public void run() {

        boolean handshakeStatus = model.handshake();
        if (!handshakeStatus) {
            logger.severe("Handshake failed");
        }

        view.setVisible(true);
        view.initiateWelcomeFrame();
        receiverThread = new Thread(() -> receiveFromServer());
        receiverThread.start();

    }

    public static void main(String[] args) {
        ChatClientView chatClientView = new ChatClientView();
        ChatClientModel chatClientModel = new ChatClientModel();
        ChatClientController chatClientController = new ChatClientController(chatClientModel, chatClientView);
        Thread controllerThread = new Thread(chatClientController);
        controllerThread.start();
    }

}
