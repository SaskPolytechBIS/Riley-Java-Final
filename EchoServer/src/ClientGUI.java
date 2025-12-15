
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;

/**
 * Client GUI laid out vertically so: - top: message area (fixed height) -
 * middle: stacked Host/Port/UserId/Message fields - separator - bottom: 4x2
 * button grid
 */
public class ClientGUI extends JFrame implements ChatIF {

    private static final long serialVersionUID = 1L;

    ChatClient client;
    final public static int DEFAULT_PORT = 5555;

    // Buttons
    private JButton userListB = new JButton("User List");
    private JButton pmB = new JButton("PM");
    private JButton sendB = new JButton("Send");
    private JButton loginB = new JButton("Login");
    private JButton browseB = new JButton("Browse");
    private JButton saveB = new JButton("Save");
    private JButton logoffB = new JButton("Logoff");
    private JButton quitB = new JButton("Quit");

    // Fields and labels
    private JTextField hostTxF = new JTextField("localhost", 14);
    private JTextField portTxF = new JTextField("5555", 8);
    private JTextField userIdTxF = new JTextField("", 14);
    private JTextField messageTxF = new JTextField("", 14);

    private JLabel hostLB = new JLabel("Host:", JLabel.RIGHT);
    private JLabel portLB = new JLabel("Port:", JLabel.RIGHT);
    private JLabel userIdLB = new JLabel("User Id:", JLabel.RIGHT);
    private JLabel messageLB = new JLabel("Message:", JLabel.RIGHT);

    // Message area
    private JTextArea messageList = new JTextArea();
    private JScrollPane messageScroll = new JScrollPane(messageList);

    private File selectedFile = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI("localhost", DEFAULT_PORT));
    }

    public ClientGUI(String host, int port) {
        super("Simple Chat GUI");

        // Use a single vertical box layout for the content pane
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        getContentPane().add(main);

        // 1) Top: message area with a fixed maximum height so it doesn't expand
        messageList.setEditable(false);
        messageList.setLineWrap(true);
        messageList.setWrapStyleWord(true);
        messageScroll.setPreferredSize(new Dimension(380, 300));
        messageScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300)); // prevents vertical expansion
        main.add(messageScroll);
        main.add(Box.createRigidArea(new Dimension(0, 8)));

        // 2) Middle: stacked labels/fields panel (4 rows x 2 columns)
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        fieldsPanel.add(hostLB, c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1.0;
        fieldsPanel.add(hostTxF, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        fieldsPanel.add(portLB, c);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        fieldsPanel.add(portTxF, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.0;
        fieldsPanel.add(userIdLB, c);
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1.0;
        fieldsPanel.add(userIdTxF, c);

        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0.0;
        fieldsPanel.add(messageLB, c);
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1.0;
        fieldsPanel.add(messageTxF, c);

        main.add(fieldsPanel);
        main.add(Box.createRigidArea(new Dimension(0, 6)));

        // 3) Separator line
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        main.add(sep);
        main.add(Box.createRigidArea(new Dimension(0, 8)));

        // 4) Bottom: buttons panel - 4 rows x 2 columns
        JPanel buttonPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        // order chosen to match screenshot feel
        buttonPanel.add(userListB);
        buttonPanel.add(sendB);
        buttonPanel.add(pmB);
        buttonPanel.add(saveB);
        buttonPanel.add(loginB);
        buttonPanel.add(logoffB);
        buttonPanel.add(browseB);
        buttonPanel.add(quitB);

        main.add(buttonPanel);

        // --- Actions ---
        loginB.addActionListener(e -> {
            String userId = userIdTxF.getText().trim();
            if (userId.equals("")) {
                display("You must enter a User Id to log in");
            } else {
                display("Logging in as " + userId);
                send("#login");
                send("#setName " + userId);
            }
        });

        sendB.addActionListener(e -> send(messageTxF.getText()));

        logoffB.addActionListener(e -> {
            display("Logging off server");
            send("#logoff");
        });

        quitB.addActionListener(e -> send("#quit"));

        browseB.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showOpenDialog(ClientGUI.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedFile = chooser.getSelectedFile();
                display("Selected file: " + selectedFile.getName());
            } else {
                display("File selection cancelled");
            }
        });

        saveB.addActionListener(e -> {
            if (selectedFile == null) {
                display("No file selected. Click Browse first.");
                return;
            }
            if (client == null || !client.isConnected()) {
                display("You must login/connect before sending a file.");
                return;
            }
            try {
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                Envelope env = new Envelope();
                env.setCommand("#ftpUpload");            // <-- required command
                env.setArg(selectedFile.getName());      // filename only (no path)
                env.setData(fileBytes);                  // file contents as byte[]
                client.sendToServer(env);
                display("Sent file to server: " + selectedFile.getName());
            } catch (IOException ex) {
                display("Error reading or sending file: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        userListB.addActionListener(e -> send("#who"));

        pmB.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(ClientGUI.this,
                    "Enter 'target message' (e.g. alice Hi):", "Private Message", JOptionPane.PLAIN_MESSAGE);
            if (input != null && !input.trim().isEmpty()) {
                send("#pm " + input.trim());
            }
        });

        // create ChatClient to handle messages (existing code)
        try {
            client = new ChatClient(host, port, this);
        } catch (IOException exception) {
            System.out.println("Error: Can't setup connection!!!! Terminating client.");
            System.exit(1);
        }

        // Final window settings
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(420, 720)); // reasonably large
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // send a text message command to ChatClient's handler
    public void send(String message) {
        client.handleMessageFromClientUI(message);
    }

    // display text in the message area (most recent at top)
    public void display(String message) {
        messageList.insert(message + "\n", 0);
    }
}
