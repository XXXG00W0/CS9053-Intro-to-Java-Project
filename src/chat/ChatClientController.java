package chat;

// import sqlite-jdbc-3.34.0.jar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.attribute.AclFileAttributeView;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.sql.rowset.spi.SyncFactoryException;
import javax.swing.*;

public class ChatClientController implements Runnable {

    private ChatClientModel model;
    private ChatClientView view;
    private SettingFrame settingFrame;

    private Thread senderThread;
    private Thread receiverThread;

    private String username;

    private LoginListener loginListener;
    private RegisterListener registerListener;

    // private final static String LOGIN = "LOGIN";
    private static final Logger logger = Logger.getLogger(ChatClientView.class.getName());

    public ChatClientController(ChatClientModel model, ChatClientView view) {
        this.model = model;
        this.view = view;

        view.addExitItemListener(new ExitListener());

        loginListener = new LoginListener();
        view.addWelcomeLoginListener(loginListener);
        registerListener = new RegisterListener();
        view.addWelcomeRegisterListener(registerListener);
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
        settingFrame = new SettingFrame();
        settingFrame.setConfirmListner(new SettingConfirmListener());
        settingFrame.setCancelListner(new SettingCancelListener());
        view.addSettingItemListener(new openSettingPanelListener());

        // heartBeatTimer.start();
    }

    private void receiveFromServer() {
        while (!Thread.currentThread().isInterrupted()) {
            String text = model.receiveMessage(true);
            logger.info("Receive from server: " + text);
            // put
            if (text.equals(ChatClientModel.DISCONNECT)) {
                model.connectionCleanup();
                view.appendTextArea("Server disconnected\n");
                break;
            }String[] stringList = text.split(" ");
            String action = stringList[0];
            boolean isAction = Arrays.stream(ChatClientModel.ACTION_LIST).anyMatch(e -> e.equals(action));
            if (isAction) {
                try {
                    logger.info("Put server reply: " + text);
                    model.messageQueue.put(text);
                } catch (InterruptedException ie) {
                    logger.warning("Putting message " + text + "with InterruptedException:\n" + ie.getMessage());
                }
            } else if (model.getStatus() == ChatClientModel.CERTIFICATED) {
                // view.appendTextArea("Welcome\n");
                view.appendTextArea(text);
                logger.info(text);
            }
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
                    // message start with MSG
                    model.sendMessage(ChatClientModel.MSG + " " + text + '\n', true);
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

    private class ExitListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                receiverThread.interrupt();
                model.connectionCleanup();
                logger.info("connection closed");
            } finally {
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
            if (model.getStatus() == ChatClientModel.NOT_CONNECTED) {
                message = "Handshake failed";
            } else if (model.getStatus() == ChatClientModel.HANDSHAKE_OK) {
                logger.info("Handshake ok then login");
                // proceed to certification stage
                String username = view.getUsername();
                String password = view.getPassword();
                if (username.equals("") | password.equals("")) {
                    message = "Empty username or password";
                } else if (model.login(username, password)) {
                    message = "Connected!";
                    model.setStatus(ChatClientModel.CERTIFICATED);
                    model.setUsername(username);
                    // dispose welcome window after logged in
                    view.closeWelcomeFrame();
                } else {
                    message = "Incorrect username / password";
                }
            } else {
                message = "Illegal Status: " + model.getStatus() + '\n';
                logger.severe(message);
            }
        }

        public void showCountdownPrompt() {
            // countdownLabel.setText(message + " (" + count + ")");
            countdownLabel.setText(message);
            dialog.setVisible(true);
            // set timer that update countdown label every 1000 ms / 1 sec
            // Timer timer = new Timer(1000, new ActionListener() {
            // int countdown = count;

            // @Override
            // public void actionPerformed(ActionEvent e) {
            // if (countdown > 0) {
            // countdown--;
            // updateCountdownLabel(countdownLabel, countdown);
            // } else {
            // ((Timer) e.getSource()).stop();
            // dialog.dispose();
            // }
            // }
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

    public class RegisterListener implements ActionListener {
        JOptionPane pane;
        JDialog dialog;
        JLabel countdownLabel = new JLabel();
        int count = 3;
        String message = "";

        // String message = model.getStatus() == CERTIFICATED ? "Login
        // Successful!" : "Login Failed...";

        public RegisterListener() {
            pane = new JOptionPane(countdownLabel, JOptionPane.INFORMATION_MESSAGE);
            countdownLabel.setText(message + " (" + count + ")");
            dialog = pane.createDialog(null, "Register prompt");
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setModal(false); // set dialog to non modal
        }

        public void register() {
            if (model.getStatus() == ChatClientModel.NOT_CONNECTED) {
                model.handshake();
            }
            if (model.getStatus() == ChatClientModel.NOT_CONNECTED) {
                message = "Handshake failed";
            } else if (model.getStatus() == ChatClientModel.HANDSHAKE_OK) {
                // proceed to register stage
                logger.info("Handshake ok then register");
                String username = view.getUsername();
                String password = view.getPassword();
                if (username.equals("") | password.equals("")) {
                    message = "Empty username or password";
                } else if (!model.register(username, password)) {
                    message = "Unable to register, consider a different username";
                } else {
                    message = "Register successful, please procceed to login";
                }
            } else {
                message = "Illegal Status: " + model.getStatus() + '\n';
                logger.severe(message);
            }
        }

        public void showCountdownPrompt() {
            // countdownLabel.setText(message + " (" + count + ")");
            countdownLabel.setText(message);
            dialog.pack();
            dialog.setVisible(true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            register();
            showCountdownPrompt();
        }

        private void updateCountdownLabel(JLabel label, int seconds) {
            label.setText(message + " (" + seconds + ")");
        }
    }

    public class openSettingPanelListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            settingFrame.setUsername(model.getUsername());
            settingFrame.setVisible(true);
        }
    }

    public class SettingConfirmListener implements ActionListener {

        JOptionPane pane;
        JDialog dialog;
        JLabel countdownLabel = new JLabel();
        String message = "";

        public SettingConfirmListener() {

            pane = new JOptionPane(countdownLabel, JOptionPane.INFORMATION_MESSAGE);
            countdownLabel.setText(message);
            dialog = pane.createDialog(null, "Setting prompt");
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setModal(false); // set dialog to non modal
        }

        @Override
        public void actionPerformed(ActionEvent e){
            String username = settingFrame.getUsername();
            String password = settingFrame.getPassword();
            String option = settingFrame.getSelectedOption();
            if (model.getUsername().equals(username)){
                if (option.equals("password")){
                    String newPassword = settingFrame.getNewPassword();
                    if(model.changePassword(username, password, newPassword)){
                        message = "Change password successful";
                    }else{
                        message = "Change password failed";
                    }
                } else if (option.equals("username")){
                    String newUsername = settingFrame.getNewUsername();
                    if(model.changeUsername(username, newUsername, password)){
                        model.setUsername(newUsername);
                        settingFrame.setUsername(newUsername);
                        message = "Change username successful";
                    }else{
                        message = "Change username failed";
                    }
                }else if (option.equals("delete")){
                    if(model.deleteAccount(username, password)){
                        model.setUsername("");
                        settingFrame.dispose();
                        view.initiateWelcomeFrame();
                        model.setStatus(ChatClientModel.HANDSHAKE_OK);
                        message = "Account deleted";
                    }else{
                        message = "Invalid username or password";
                    }

                }else{
                    message = "Invalid option: "+ option;
                }
            }else{
                message = "Invalid username";
            }
            
           
            showPrompt();
        }

        public void showPrompt() {
            // countdownLabel.setText(message + " (" + count + ")");
            countdownLabel.setText(message);
            dialog.pack();
            dialog.setVisible(true);
        }
    }

    public class SettingCancelListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            settingFrame.dispose();
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
