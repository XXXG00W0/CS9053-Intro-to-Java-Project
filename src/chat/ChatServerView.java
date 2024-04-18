package chat;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;

import javax.swing.*;

public class ChatServerView extends JFrame {

    JTextArea textArea;
	JMenuBar menuBar;
	JMenu menu;
    JMenuItem exitItem;

    public ChatServerView(){
        super("Chat Server");

        menu = new JMenu("File");
		menu = new JMenu("File");
		exitItem = new JMenuItem("Exit");
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
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void addExitItemListener(ActionListener exitListner){
        exitItem.addActionListener(exitListner);
    }
    
    public void appendTextArea(String message){
        textArea.append(message);
    }
}
