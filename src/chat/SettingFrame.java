package chat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class SettingFrame extends JFrame {

    private JPanel gridPanel;

    private JPanel usernamePanel;
    private JLabel usernameLabel;
    private JTextField usernameField;

    private JPanel newUsernamePanel;
    private JLabel newUsernameLabel;
    private JTextField newUsernameField;

    private JPanel pswdPanel;
    private JLabel pswdLabel;
    private JTextField pswdField;
    
    private JPanel newPswdPanel;
    private JLabel newPswdLabel;
    private JTextField newPswdField;

    private JPanel controlPanel;
    private JButton confirmButton;
    private JButton cancelButton;

    private JPanel radioPanel;
    private JRadioButton pswdButton;
    private JRadioButton userButton;
    private JRadioButton deleteButton;
    private ButtonGroup buttonGroup;

    private String seletedOption = "";

    private final static Dimension SCREEN_RESOLUTION = Toolkit.getDefaultToolkit().getScreenSize();
    private final static int SCREEN_WIDTH = SCREEN_RESOLUTION.width;
    private final static int SCREEN_HEIGHT = SCREEN_RESOLUTION.height;
    private int width = 300;
    private int height = 400;

    private Color backgroundColor = new Color(255, 255, 255);
    private Color buttonPressedColor = new Color(223, 225, 229);
    private Color buttonBaseColor = new Color(242, 243, 245);
    private Color buttonHoverColor = new Color(215, 217, 220);
    private Font defualtFont = new Font("Helvetica", Font.PLAIN, 14);
    private Font itallicFont = new Font("Helvetica", Font.ITALIC, 14);

    public SettingFrame() {
        super("Settings");
        this.setLayout(new BorderLayout());
        usernameField = new JTextField(10);
        usernameField.setFont(defualtFont);
        newUsernameField = new JTextField(10);
        newUsernameField.setFont(defualtFont);
        pswdField = new JTextField(10);
        pswdField.setFont(defualtFont);
        newPswdField = new JTextField(10);
        newPswdField.setFont(defualtFont);
        usernameLabel = new JLabel("username");
        usernameLabel.setFont(defualtFont);
        newUsernameLabel = new JLabel("New username");
        newUsernameLabel.setFont(defualtFont);
        pswdLabel = new JLabel("password");
        pswdLabel.setFont(defualtFont);
        newPswdLabel = new JLabel("New password");
        newPswdLabel.setFont(defualtFont);

        pswdButton = new JRadioButton("Password");
        pswdButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED){
                paintNewPasswordPanel();
                seletedOption = "password";
            }
        });
        userButton = new JRadioButton("Username");
        userButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED){
                paintNewUsernamePanel();
                seletedOption = "username";
            }
        });
        deleteButton = new JRadioButton("Delete Account");
        deleteButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED){
                paintDeleteUserPanel();
                seletedOption = "delete";
            }
        });
        buttonGroup = new ButtonGroup();
        buttonGroup.add(pswdButton);
        buttonGroup.add(userButton);
        buttonGroup.add(deleteButton);

        confirmButton = new JButton("Confirm") {

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
        confirmButton.setFont(defualtFont);
        // confirmButton.addActionListener(new LoginListener(true));

        cancelButton = new JButton("Cancel") {

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
        cancelButton.setFont(itallicFont);

        gridPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        gridPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // row 1
        usernamePanel = new JPanel(new FlowLayout());
        usernamePanel.setBackground(backgroundColor);
        usernamePanel.add(usernameLabel);
        usernamePanel.add(usernameField);
        gridPanel.add(usernamePanel);
        
        // row 2
        pswdPanel = new JPanel(new FlowLayout());
        pswdPanel.setBackground(backgroundColor);
        pswdPanel.add(pswdLabel);
        pswdPanel.add(pswdField);
        gridPanel.add(pswdPanel);

        // row 3
        radioPanel = new JPanel(new FlowLayout());
        radioPanel.add(pswdButton);
        radioPanel.add(userButton);
        radioPanel.add(deleteButton);
        gridPanel.add(radioPanel);

        // row 4
        newUsernamePanel = new JPanel(new FlowLayout());
        newUsernamePanel.setBackground(backgroundColor);
        newUsernamePanel.add(newUsernameLabel);
        newUsernamePanel.add(newUsernameField);
        // gridPanel.add(newUsernamePanel);

        // row 4
        newPswdPanel = new JPanel(new FlowLayout());
        newPswdPanel.setBackground(backgroundColor);
        newPswdPanel.add(newPswdLabel);
        newPswdPanel.add(newPswdField);
        // gridPanel.add(newPswdPanel);

        // row 5
        controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(backgroundColor);
        controlPanel.add(cancelButton);
        controlPanel.add(confirmButton);
        // gridPanel.add(controlPanel);

        this.add(gridPanel);
        this.pack();
        // this.setSize(width, height);
        this.setLocation((SCREEN_WIDTH - width) / 2, (SCREEN_HEIGHT - height) / 2);
    }

    public void paintNewPasswordPanel(){
        /* Print new password label and text field */
        this.remove(gridPanel);
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(5, 1, 10, 10));
        gridPanel.add(usernamePanel);
        gridPanel.add(pswdPanel);
        gridPanel.add(radioPanel);
        gridPanel.add(newPswdPanel);
        gridPanel.add(controlPanel);

        this.add(gridPanel);
        this.revalidate();
        this.repaint();
        this.pack();
    }

    public void paintNewUsernamePanel(){
        /* Print new username label and text field */
        this.remove(gridPanel);
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(5, 1, 10, 10));
        gridPanel.add(usernamePanel);
        gridPanel.add(pswdPanel);
        gridPanel.add(radioPanel);
        gridPanel.add(newUsernamePanel);
        gridPanel.add(controlPanel);

        this.add(gridPanel);
        this.revalidate();
        this.repaint();
        this.pack();
    }

    public void paintDeleteUserPanel(){
        /* Print new username label and text field */
        this.remove(gridPanel);
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(5, 1, 10, 10));
        gridPanel.add(usernamePanel);
        gridPanel.add(pswdPanel);
        gridPanel.add(radioPanel);
        gridPanel.add(controlPanel);

        this.add(gridPanel);
        this.revalidate();
        this.repaint();
        this.pack();
    }


    public String getPassword(){
        return pswdField.getText();
    }

    public String getNewPassword(){
        return newPswdField.getText();
    }

    public String getUsername(){
        return usernameField.getText();
    }

    public String getNewUsername(){
        return newUsernameField.getText();
    }

    public void setConfirmListner(ActionListener ac){
        this.confirmButton.addActionListener(ac);
    }

    public void setCancelListner(ActionListener ac){
        this.cancelButton.addActionListener(ac);
    }

    public String getSelectedOption(){
        return seletedOption;
    }

    public void setUsername(String username){
        this.usernameField.setText(username);
    }
}
