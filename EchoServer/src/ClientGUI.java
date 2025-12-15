import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;

/**
 * Cleaned-up Client GUI with FTP UI elements commented out (instead of removed)
 *
 * Notes:
 * - The File List combo, File List button and Download button are commented out
 * - Each commented block is labeled and includes a brief reason and re-enable note
 * - Upload (Save) logic remains active (sends "#ftpUpload") because server still expects it
 */
public class ClientGUI extends JFrame implements ChatIF {

    private static final long serialVersionUID = 1L;

    ChatClient client;
    final public static int DEFAULT_PORT = 5555;

    // Buttons
    private JButton userListB = new JButton("User List");
    // COMMENTED OUT - FTP File List button
    // Reason: removing optional FTP UI to simplify GUI for current build
    // private JButton ftpListB = new JButton("File List");

    // COMMENTED OUT - FTP Download button
    // Reason: removing optional FTP UI to simplify GUI for current build
    // private JButton downloadB = new JButton("Download");

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

    // COMMENTED OUT - combo for remote files (used by FTP File List / Download)
    // Reason: not needed for current simplified GUI; left as comment for easy restore
    // add ftpListB/downloadB to the button panel and uncomment the listeners
    // private JComboBox<String> fileListCombo = new JComboBox<>()

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

        // COMMENTED OUT - Files combo row (FTP)
        // Reason: removed optional FTP list UI to simplify GUI
        /*
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 0.0;
        fieldsPanel.add(new JLabel("Files:", JLabel.RIGHT), c);
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 1.0;
        fieldsPanel.add(fileListCombo, c);
        */

        main.add(fieldsPanel);
        main.add(Box.createRigidArea(new Dimension(0, 6)));

        // 3) Separator line
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        main.add(sep);
        main.add(Box.createRigidArea(new Dimension(0, 8)));

        // 4) Bottom: buttons panel - 4 rows x 2 columns (clean)
        JPanel buttonPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        // order: User List / Send, PM / Save, Login / Logoff, Browse / Quit
        buttonPanel.add(userListB);  buttonPanel.add(sendB);

        // COMMENTED OUT - FTP File List button position
        // Reason: this used to provide file listing from server. Commented out to simplify UI
        // buttonPanel.add(ftpListB)

        buttonPanel.add(pmB);        buttonPanel.add(saveB);

        // COMMENTED OUT - FTP Download button position
        // Reason: this used to trigger file download from server. Commented out to simplify UI
        // buttonPanel.add(downloadB)

        buttonPanel.add(loginB);     buttonPanel.add(logoffB);
        buttonPanel.add(browseB);    buttonPanel.add(quitB);

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

        // Upload (send #ftpUpload)
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
                env.setCommand("#ftpUpload");            // command agreed earlier
                env.setArg(selectedFile.getName());      // filename only
                env.setData(fileBytes);                  // file contents as byte[]
                client.sendToServer(env);
                display("Sent file to server: " + selectedFile.getName());
            } catch (IOException ex) {
                display("Error reading or sending file: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        // COMMENTED OUT - ftpListB listener (request server file list)
        // Reason: optional FTP UI removed for simplicity
        /*
        ftpListB.addActionListener(e -> {
            if (client == null || !client.isConnected()) {
                display("You must login/connect before requesting file list.");
                return;
            }
            try {
                Envelope env = new Envelope();
                env.setCommand("#ftplist");
                client.sendToServer(env);
                display("Requested file list from server...");
            } catch (IOException ex) {
                display("Error requesting file list: " + ex.getMessage());
            }
        });
        */

        // COMMENTED OUT - downloadB listener (request server to send file)
        // Reason: optional FTP UI removed for simplicity
        /*
        downloadB.addActionListener(e -> {
            if (client == null || !client.isConnected()) {
                display("You must login/connect before downloading.");
                return;
            }
            String filename = (String) fileListCombo.getSelectedItem();
            if (filename == null || filename.isEmpty()) {
                display("No file selected in the list.");
                return;
            }
            try {
                Envelope env = new Envelope();
                env.setCommand("#ftpget");
                env.setArg(filename);
                client.sendToServer(env);
                display("Requested download for: " + filename);
            } catch (IOException ex) {
                display("Error requesting download: " + ex.getMessage());
            }
        });
        */

        userListB.addActionListener(e -> send("#who"));

        pmB.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(ClientGUI.this,
                    "Enter 'target message' (e.g. alice Hi):", "Private Message", JOptionPane.PLAIN_MESSAGE);
            if (input != null && !input.trim().isEmpty()) {
                send("#pm " + input.trim());
            }
        });

        // create ChatClient to handle messages
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
    // NOTE: previously this method handled FTPLIST:... special messages to populate the file combo
    // That logic has been commented out to remove FTP UI behavior while preserving the code for easy restore
    public void display(String message) {
        if (message == null) return;

        // COMMENTED OUT - FTPLIST parsing
        // Reason: removed FTP UI from GUI
        /*
        if (message.startsWith("FTPLIST:")) {
            String listString = message.substring("FTPLIST:".length());
            String[] items = listString.isEmpty() ? new String[0] : listString.split(",", -1);
            SwingUtilities.invokeLater(() -> {
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                for (String s : items) {
                    if (!s.isEmpty()) model.addElement(s);
                }
                fileListCombo.setModel(model);
                if (model.getSize() > 0) fileListCombo.setSelectedIndex(0);
                messageList.insert("File list updated (" + model.getSize() + " files)\n", 0);
            });
            return;
        }
        */

        messageList.insert(message + "\n", 0);
    }
}