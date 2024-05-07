package chat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WelcomeFrame extends JFrame implements Runnable {

    private JTextField userNameField;
    private JTextField pswdField;
    private JLabel userNameLabel;
    private JLabel pswdLabel;
    private JLabel retryLabel;
    private JButton loginButton;
    private JButton registerButton;

    private final static Dimension SCREEN_RESOLUTION = Toolkit.getDefaultToolkit().getScreenSize();
    private final static int SCREEN_WIDTH = SCREEN_RESOLUTION.width;
    private final static int SCREEN_HEIGHT = SCREEN_RESOLUTION.height;
    private int width = 300;
    private int height = 200;

    private Color backgroundColor = new Color(255, 255, 255);
    private Color buttonPressedColor = new Color(223, 225, 229);
    private Color buttonBaseColor = new Color(242, 243, 245);
    private Color buttonHoverColor = new Color(215, 217, 220);
    private Font defualtFont = new Font("Helvetica", Font.PLAIN, 14);
    private Font itallicFont = new Font("Helvetica", Font.ITALIC, 14);

    public WelcomeFrame() {
        super("Start chatting");
        this.setLayout(new BorderLayout());
        userNameField = new JTextField(10);
        userNameField.setFont(defualtFont);
        pswdField = new JTextField(10);
        pswdField.setFont(defualtFont);
        userNameLabel = new JLabel("Username");
        userNameLabel.setFont(defualtFont);
        pswdLabel = new JLabel("Password");
        pswdLabel.setFont(defualtFont);
        retryLabel = new JLabel("Connection failed, please retry");
        retryLabel.setFont(defualtFont);
        retryLabel.setSize(getPreferredSize());

        loginButton = new JButton("Login") {

            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isPressed()) {
                    g.setColor(buttonPressedColor);
                } else if (getModel().isRollover()) {
                    g.setColor(buttonHoverColor);
                } else {
                    g.setColor(buttonBaseColor);
                }
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        loginButton.setFont(defualtFont);
        // loginButton.addActionListener(new LoginListener(true));

        registerButton = new JButton("New user?") {

            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isPressed()) {
                    g.setColor(buttonPressedColor);
                } else if (getModel().isRollover()) {
                    g.setColor(buttonHoverColor);
                } else {
                    g.setColor(buttonBaseColor);
                }
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        registerButton.setFont(itallicFont);

        JPanel gridPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        gridPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel userPanel = new JPanel(new FlowLayout());
        userPanel.setBackground(backgroundColor);
        userPanel.add(userNameLabel);
        userPanel.add(userNameField);
        gridPanel.add(userPanel);

        JPanel pswdPanel = new JPanel(new FlowLayout());
        pswdPanel.setBackground(backgroundColor);
        pswdPanel.add(pswdLabel);
        pswdPanel.add(pswdField);
        gridPanel.add(pswdPanel);

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(backgroundColor);
        controlPanel.add(registerButton);
        controlPanel.add(loginButton);
        gridPanel.add(controlPanel);

        this.add(gridPanel);
        this.setSize(width, height);
        this.setLocation((SCREEN_WIDTH - width) / 2, (SCREEN_HEIGHT - height) / 2);
    }

    public void addLoginListener(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public void addRegisterListener(ActionListener listener) {
        registerButton.addActionListener(listener);
    }

    public String getUsername() {
        return userNameField.getText().trim();
    }

    public String getPassword() {
        return pswdField.getText();
    }

    // class LoginListener implements ActionListener{

    // @Override
    // public void actionPerformed(ActionEvent e) {

    // }
    // }

    class RegisterListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

        }

    }

    public static void main(String[] args) {
        WelcomeFrame welcomeFrame = new WelcomeFrame();
        welcomeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        welcomeFrame.setVisible(true);
        welcomeFrame.setResizable(false);
    }

    @Override
    public void run() {

    }

}
