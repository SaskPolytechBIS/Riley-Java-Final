import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;

/**
 * Client GUI with FTP UI elements enabled:
 * - File List combo (populated from server FTPLIST response)
 * - "File List" button that requests the server file list (#ftplist)
 * - "Download" button that requests a file from the server (#ftpget)
 *
 * All previous comments and upload behavior preserved.
 */
public class ClientGUI extends JFrame implements ChatIF {

    private static final long serialVersionUID = 1L;

    ChatClient client;
    final public static int DEFAULT_PORT = 5555;

    // Buttons
    private JButton userListB = new JButton("User List");
    private JButton ftpListB = new JButton("File List");   // enabled
    private JButton downloadB = new JButton("Download");   // enabled

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

    // File list combo for remote files (used by FTP File List / Download)
    // This was previously commented out; now enabled and wired to server FTPLIST responses.
    private JComboBox<String> fileListCombo = new JComboBox<>();

    private File selectedFile = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI("localhost", DEFAULT_PORT));
    }

    public ClientGUI(String host, int port) {
        super("Simple Chat GUI");

        // --- Simple visual defaults (small & safe) ---
        UIManager.put("defaultFont", new Font("SansSerif", Font.PLAIN, 13));
        Color panelBg = new Color(0xF4F6F8);    // light neutral background
        Color messageBg = Color.WHITE;
        Color buttonBg = new Color(0xEAF0F7);   // soft blue
        Color buttonFg = new Color(0x0B57A5);   // deep blue text

        // Use a single vertical box layout for the content pane
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        main.setBackground(panelBg);
        getContentPane().add(main);

        // 1) Top: message area with a fixed maximum height so it doesn't expand
        messageList.setEditable(false);
        messageList.setLineWrap(true);
        messageList.setWrapStyleWord(true);
        messageList.setBackground(messageBg);
        messageList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        messageScroll.setPreferredSize(new Dimension(520, 320));
        messageScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320)); // prevents vertical expansion
        main.add(messageScroll);
        main.add(Box.createRigidArea(new Dimension(0, 8)));

        // -------------------------------------------------------------
        // New layout: bottom area composed of two columns:
        // - leftColumn: stacked fields (Host, Port, UserId, Message) + fileListCombo
        // - rightColumn: vertically stacked buttons (includes FTP buttons)
        // -------------------------------------------------------------

        // Left column (fields) using GridBagLayout (keeps existing spacing)
        JPanel leftColumn = new JPanel(new GridBagLayout());
        leftColumn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        leftColumn.setBackground(panelBg);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        leftColumn.add(hostLB, c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1.0;
        leftColumn.add(hostTxF, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.0;
        leftColumn.add(portLB, c);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        leftColumn.add(portTxF, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.0;
        leftColumn.add(userIdLB, c);
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1.0;
        leftColumn.add(userIdTxF, c);

        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0.0;
        leftColumn.add(messageLB, c);
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1.0;
        leftColumn.add(messageTxF, c);

        // Files combo row (FTP) - enabled
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 0.0;
        leftColumn.add(new JLabel("Files:", JLabel.RIGHT), c);
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 1.0;
        fileListCombo.setPrototypeDisplayValue("selected-file-name.txt"); // makes combo a reasonable width
        leftColumn.add(fileListCombo, c);

        // Right column: vertically stacked buttons with consistent styling
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(true);
        rightColumn.setBackground(panelBg);
        rightColumn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        Font btnFont = new Font("SansSerif", Font.BOLD, 13);
        // Order: User List, PM, Send, File List, Download, Save, Login, Logoff, Browse, Quit
        JButton[] btns = new JButton[] {
                userListB, pmB, sendB, ftpListB, downloadB, saveB, loginB, logoffB, browseB, quitB
        };
        for (JButton b : btns) {
            b.setBackground(buttonBg);
            b.setForeground(buttonFg);
            b.setFocusPainted(false);
            b.setFont(btnFont);
            b.setMaximumSize(new Dimension(200, 36));
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xC8D7E6)),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            rightColumn.add(b);
            rightColumn.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        // Combine into bottomPanel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(true);
        bottomPanel.setBackground(panelBg);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 10, 6));
        bottomPanel.add(leftColumn, BorderLayout.CENTER);
        bottomPanel.add(rightColumn, BorderLayout.EAST);

        main.add(bottomPanel);

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

        sendB.addActionListener(e -> {
            send(messageTxF.getText());
            // optional: clear the input after sending
            messageTxF.setText("");
        });

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

        // ftpListB listener (request server file list) - enabled
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

        // downloadB listener (request server to send file) - enabled
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

        userListB.addActionListener(e -> send("#who"));

        // PM button: show a small two-field dialog (Target, Message), validate, then send
        pmB.addActionListener(e -> {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Target (username):"), gbc);
            gbc.gridx = 1;
            JTextField targetField = new JTextField(12);
            panel.add(targetField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            panel.add(new JLabel("Message:"), gbc);
            gbc.gridx = 1;
            JTextField msgField = new JTextField(18);
            panel.add(msgField, gbc);

            int result = JOptionPane.showConfirmDialog(
                    ClientGUI.this,
                    panel,
                    "Private Message",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String target = targetField.getText();
                String text = msgField.getText();
                if (target == null) target = "";
                if (text == null) text = "";

                target = target.trim();
                text = text.trim();

                if (target.length() == 0) {
                    display("PM cancelled: target is empty. Usage: specify username.");
                    return;
                }
                if (text.length() == 0) {
                    display("PM cancelled: message is empty. Usage: enter a message.");
                    return;
                }

                send("#pm " + target + " " + text);
            } else {
                display("PM cancelled");
            }
        });

        pmB.setEnabled(true); // ensure enabled

        // create ChatClient to handle messages
        try {
            client = new ChatClient(host, port, this);
        } catch (IOException exception) {
            System.out.println("Error: Can't setup connection!!!! Terminating client.");
            System.exit(1);
        }

        // Final window settings
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(840, 640)); // wide enough for combo + buttons
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // send a text message command to ChatClient's handler
    public void send(String message) {
        client.handleMessageFromClientUI(message);
    }

    // display text in the message area (most recent at top)
    // If FTPLIST response arrives it will populate the fileListCombo.
    public void display(String message) {
        if (message == null) return;

        // FTPLIST parsing: format is "FTPLIST:name1,name2,..." (as produced by ChatClient)
        if (message.startsWith("FTPLIST:")) {
            String listString = message.substring("FTPLIST:".length());
            String[] items = listString.isEmpty() ? new String[0] : listString.split(",", -1);
            SwingUtilities.invokeLater(() -> {
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                for (String s : items) {
                    if (!s.isEmpty()) model.addElement(s);
                }
                fileListCombo.setModel(model);
                if (model.getSize() > 0) {
                    fileListCombo.setSelectedIndex(0);
                    downloadB.setEnabled(true);
                } else {
                    downloadB.setEnabled(false);
                }
                messageList.insert("File list updated (" + model.getSize() + " files)\n", 0);
            });
            return;
        }

        messageList.insert(message + "\n", 0);
    }
}