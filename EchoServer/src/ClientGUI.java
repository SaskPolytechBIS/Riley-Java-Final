/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.IOException;

/**
 *
 * @author Instructor
 */
public class ClientGUI extends JFrame implements ChatIF {

    /**
     * The instance of the client that handles the communication for the GUI
     */
    ChatClient client;
    
    /**
     * The default port to connect on.
     */
    final public static int DEFAULT_PORT = 5555;
    
    /**
     * JFrame Buttons.
     */
    private JButton logoffB = new JButton("Logoff");
    private JButton loginB = new JButton("Login");
    private JButton sendB = new JButton("Send");
    private JButton quitB = new JButton("Quit");

    /**
     * JFrame Text Fields.
     */
    private JTextField portTxF = new JTextField("5555");
    private JTextField hostTxF = new JTextField("127.0.0.1");
    private JTextField userIdTxF = new JTextField("");
    private JTextField messageTxF = new JTextField("");

    /**
     * JFrame Labels.
     */
    private JLabel portLB = new JLabel("Port: ", JLabel.RIGHT);
    private JLabel hostLB = new JLabel("Host: ", JLabel.RIGHT);
    private JLabel userIdLB = new JLabel("UserId: ", JLabel.RIGHT);
    private JLabel messageLB = new JLabel("Message: ", JLabel.RIGHT);

    /**
     * The main text area where messages are displayed
     */
    private JTextArea messageList = new JTextArea();

    public static void main(String[] args)
    {
        String host = "localhost";
        int port = DEFAULT_PORT;  //The port number
        
        //create and display the GUI using the default values for host and port as placeholders
        ClientGUI chat = new ClientGUI(host, port);
    }
    
    public ClientGUI(String host, int port) {

        //create the GUI Window
        super("Simple Chat GUI");
        setSize(300, 400);

        //setup the JFrame layout
        setLayout(new BorderLayout(5, 5));

        //Because only one item can go in each area of a JFrame layout, we create
        //the JPanel Bottom and add our elements into that, then add that to the
        //JFrame layout
        JPanel bottom = new JPanel();
        add("Center", messageList);
        add("South", bottom);

        //Set the Layout of the buttons to be a grid
        bottom.setLayout(new GridLayout(6, 2, 5, 5));
        bottom.add(hostLB);             bottom.add(hostTxF);
        
        bottom.add(portLB);             bottom.add(portTxF);
        
        bottom.add(userIdLB);           bottom.add(userIdTxF);
        
        bottom.add(messageLB);          bottom.add(messageTxF);
        
        bottom.add(loginB);             bottom.add(sendB);
       
        bottom.add(logoffB);            bottom.add(quitB);
        

        loginB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String userId = userIdTxF.getText();
                
                if(userId.equals(""))
                {
                   display("You must enter a User Id to log in");
                }
                else
                {
                    display("Logging in as "+ userIdTxF.getText());
                    send("#login");
                    send("#setName "+userIdTxF.getText());
                }
                
            }
        });
        
        sendB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send(messageTxF.getText());
            }
        });
        
        logoffB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                display("Logging off server");
                send("#logoff");
            }
        });
        
        quitB.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send("#quit");
            }
        });

        //create ChatClient to handle messages
        try {
            client = new ChatClient(host, port, this);
        } catch (IOException exception) {
            System.out.println("Error: Can't setup connection!!!!"
                    + " Terminating client.");
            System.exit(1);
        }
        
        //Display the window to the user. This should be the last step
        setVisible(true);
    }
    
    /**
     * Send a text message to the ChatClient to process 
     */
    public void send(String message)
    {
        client.handleMessageFromClientUI(message);
    }
    
    /**
     * Displays information to the user using the messageList JTextArea
     */
    public void display(String message) {
        //add the "\n" so the messages dont stack on a single line
        messageList.insert(message+"\n", 0);
    }
}
