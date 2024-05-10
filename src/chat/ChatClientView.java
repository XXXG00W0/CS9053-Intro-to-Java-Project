package chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.*;

public class ChatClientView extends JFrame {

    private final static Dimension SCREEN_RESOLUTION = Toolkit.getDefaultToolkit().getScreenSize();
    private final static int SCREEN_WIDTH = SCREEN_RESOLUTION.width;
    private final static int SCREEN_HEIGHT = SCREEN_RESOLUTION.height;
    private int width = 400;
    private int height = 400;

    private Color backgroundColor = new Color(255, 255, 255);
    private Color buttonPressedColor = new Color(223, 225, 229);
    private Color buttonBaseColor = new Color(242, 243, 245);
    private Color buttonHoverColor = new Color(215, 217, 220);
    private Color warningTextColor = new Color(255, 0, 0);
    private Font defualtFont = new Font("Helvetica", Font.PLAIN, 14);
    private Font itallicFont = new Font("Helvetica", Font.ITALIC, 14);
    private Font boldFont = new Font("Helvetica", Font.BOLD, 14);

    private WelcomeFrame welcomeFrame;

    private static final Logger logger = Logger.getLogger(ChatClientView.class.getName());

    public ChatClientView() {
        super("Chat client");
//        connectItem = new JMenuItem("Connect");
//        connectItem.setFont(defualtFont);
        // connectItem.addActionListener();
        exitItem = new JMenuItem("Exit");
        exitItem.setFont(defualtFont);
        fileMenu = new JMenu("File");
        fileMenu.setFont(defualtFont);
//        fileMenu.add(connectItem);
        fileMenu.add(exitItem);

        settingItem = new JMenuItem("Manage Account");
        settingItem.setFont(defualtFont);
        deleteAccountItem = new JMenuItem("Delete account");
        deleteAccountItem.setFont(defualtFont);
        deleteAccountItem.setForeground(warningTextColor);

        settingMenu = new JMenu("Setting");
        settingMenu.setFont(defualtFont);
        settingMenu.add(settingItem);

        menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(settingMenu);
        this.setJMenuBar(menuBar);

        textField = new JTextField(1);
        textField.setFont(defualtFont);
        // textField.addActionListener(new textFieldListener());

        textArea = new JTextArea(10, 10);
        textArea.setFont(defualtFont);
        textArea.setEditable(false);
        // textArea.addActionListener(new textAreaListener());
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(scrollPane, BorderLayout.CENTER);
        this.add(textField, BorderLayout.SOUTH);
        setSize(width, height);
        this.setLocation((SCREEN_WIDTH - width) / 2, (SCREEN_HEIGHT - height) / 2);

        welcomeFrame = createWelcomeFrame();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void initiateWelcomeFrame() {
        welcomeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        welcomeFrame.setVisible(true);
    }

    public void closeWelcomeFrame() {
        welcomeFrame.dispose();
    }

    private WelcomeFrame createWelcomeFrame() {
        return new WelcomeFrame();
    }

    public String getUsername() {
        return welcomeFrame.getUsername();
    }

    public String getPassword() {
        return welcomeFrame.getPassword();
    }

    public void addTextFieldListener(ActionListener listener) {
        textField.addActionListener(listener);
    }

    public void addConnectItemListener(ActionListener listener) {
        connectItem.addActionListener(listener);
    }

    public void addSettingItemListener(ActionListener listener) {
        settingItem.addActionListener(listener);
    }

    public void addExitItemListener(ActionListener listener) {
        exitItem.addActionListener(listener);
    }

    public void addWelcomeLoginListener(ActionListener listener) {
        welcomeFrame.addLoginListener(listener);
    }

    public void addWelcomeRegisterListener(ActionListener listener) {
        welcomeFrame.addRegisterListener(listener);
    }

    public void appendTextArea(String string) {
        if (string.isBlank()) {
            logger.warning("[appendTextArea] String is blank");
        } else if (string.charAt(string.length() - 1) == '\n') {
            textArea.append(string);
        } else {
            textArea.append(string + '\n');
        }
    }

    public void clearTextArea() {
        textArea.setText("");;
    }

    public void clearTextField() {
        textField.setText("");
    }

    public String getTextField() {
        return textField.getText();
    }

    private JTextArea textArea;
    private JTextField textField;
    private JScrollPane scrollPane;
    private JMenuBar menuBar;
    private JMenu fileMenu;
    private JMenu settingMenu;
    private JMenuItem connectItem;
    private JMenuItem exitItem;
    private JMenuItem settingItem;
    private JMenuItem deleteAccountItem;
    private JMenuItem userNameItem;

    public static void main(String[] args) {
        ChatClientView chatClientView = new ChatClientView();
        chatClientView.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatClientView.setVisible(true);
    }
}
