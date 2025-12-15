import java.io.*;
import java.util.ArrayList;

/**
 * This class overrides some of the methods defined in the abstract superclass
 * in order to give more functionality to the client.
 */
public class ChatClient extends AbstractClient {
    //Instance variables **********************************************

    /**
     * The interface type variable. It allows the implementation of the display
     * method in the client.
     */
    ChatIF clientUI;

    //Constructors ****************************************************
    /**
     * Constructs an instance of the chat client.
     *
     * @param host The server to connect to.
     * @param port The port number to connect on.
     * @param clientUI The interface type variable.
     */
    public ChatClient(String host, int port, ChatIF clientUI)
            throws IOException {
        super(host, port); //Call the superclass constructor
        this.clientUI = clientUI;
        //openConnection();
    }

    //Instance methods ************************************************
    /**
     * This method handles all data that comes in from the server.
     *
     * @param msg The message from the server.
     */
    public void handleMessageFromServer(Object msg) {
        if(msg instanceof Envelope)
        {
            Envelope env = (Envelope)msg;
            handleCommandFromServer(env);
        }
        else
        {
            clientUI.display(msg.toString());
        }
    }

    /**
     * Handles command envelopes received from server.
     */
    public void handleCommandFromServer(Envelope env)
    {
        if (env == null || env.getCommand() == null) {
            return;
        }

        String cmd = env.getCommand();

        // 'who' response (existing)
        if(cmd.equals("who"))
        {
            ArrayList<String> clients = (ArrayList<String>)env.getData();

            System.out.println("--- Printing out all clients on The List ---");

            //loop through the array list
            for(int i = 0; i < clients.size(); i++)
            {
                //for an array list .get(i) works like [i] for an array
                System.out.println(clients.get(i));
            }
            return;
        }

        // ftplist response: data is ArrayList<String>
        if (cmd.equals("ftplist")) {
            @SuppressWarnings("unchecked")
            ArrayList<String> files = (ArrayList<String>) env.getData();
            String joined = "";
            if (files != null && !files.isEmpty()) {
                joined = String.join(",", files);
            }
            // GUI will parse messages starting with "FTPLIST:"
            clientUI.display("FTPLIST:" + joined);
            return;
        }

        // ftpget response: arg = filename, data = byte[]
        if (cmd.equals("#ftpget")) {
            String filename = env.getArg();
            Object dataObj = env.getData();
            if (filename == null || dataObj == null || !(dataObj instanceof byte[])) {
                clientUI.display("Error: invalid ftpget response from server.");
                return;
            }
            byte[] bytes = (byte[]) dataObj;

            // write to downloads directory
            try {
                File dir = new File("downloads");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File out = new File(dir, filename);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(bytes);
                }
                clientUI.display("Downloaded file to downloads/" + filename);
            } catch (IOException e) {
                clientUI.display("Error saving downloaded file: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }

        // other envelopes - keep previous behavior for known commands handled elsewhere
        // you can add more handling here if server sends other Envelope commands
    }

    /**
     * This method handles all data coming from the UI
     *
     * @param message The message from the UI.
     */
    public void handleMessageFromClientUI(String message) {

        //if first character is #, handle it as a command
        if (message.charAt(0) == '#') {

            handleClientCommand(message);

        } else {
            try {
                sendToServer(message);
            } catch (IOException e) {
                clientUI.display("Could not send message to server.  Terminating client.......");
                quit();
            }
        }
    }

    /**
     * This method terminates the client.
     */
    public void quit() {
        try {
            closeConnection();
        } catch (IOException e) {
        }
        System.exit(0);
    }

    public void connectionClosed() {

        System.out.println("Connection closed");

    }

    protected void connectionException(Exception exception) {

        System.out.println("Server has shut down");

    }

    /**
     * Extends the hook method from AbstractClient. Prints out a message when 
     * client successfully connects to server
     */
    protected void connectionEstablished(){
        System.out.println("Connected to server at "+ getHost() + " on port "+getPort());
    }
    
    public void handleClientCommand(String message) {

        if (message.equals("#quit")) {
            clientUI.display("Shutting Down Client");
            quit();

        }

        if (message.equals("#logoff")) {
            clientUI.display("Disconnecting from server");
            try {
                closeConnection();
            } catch (IOException e) {
            };

        }

        //#setHost localhost
        if (message.indexOf("#setHost") == 0) {

            if (isConnected()) {
                clientUI.display("Cannot change host while connected");
            } else {
                setHost(message.substring(9, message.length()));
            }

        }

        if (message.indexOf("#setPort") == 0) {

            if (isConnected()) {
                clientUI.display("Cannot change port while connected");
            } else {
                //setPort()
                //Integer.parseInt()
                //message.substring(9, message.length())
                //#setPort 5556
                //"5556"
                
                //String portNum = message.substring(8,message.length()).trim();
                //portNum = portNum.trim();
                setPort(Integer.parseInt(message.substring(9, message.length())));
            }

        }

        if (message.equals("#login")) {

            if (isConnected()) {
                clientUI.display("already connected");
            } else {
                try {
                    openConnection();
                } catch (IOException e) {
                    clientUI.display("failed to connect to server.");
                }
            }
        }
        
        //#setName Mike
        if(message.indexOf("#setName") == 0)
        {
            //create an envelope
            Envelope env = new Envelope();
            //set the command
            env.setCommand("setName");
            
            //grab the name from the message and add it as the envelopes data
            String name = message.substring(9, message.length());
            env.setData(name);
            
            //try sending the envelope to the server
            try {
                sendToServer(env);
            } catch (IOException e) {
                clientUI.display("Could not send message to server.  Terminating client.......");
                quit();
            }
        }
        
        //#join room1
        if(message.indexOf("#join") == 0)
        {
            //make envelope to send to server
            Envelope env = new Envelope();
            env.setCommand("join");
            
            //get the room name
            String room = message.substring(6,message.length());
            env.setData(room);
            
            //try sending the envelope to the server
            try {
                sendToServer(env);
            } catch (IOException e) {
                clientUI.display("Could not send message to server.  Terminating client.......");
                quit();
            }
        }
        
        //#pm mike hi mike!
        if(message.indexOf("#pm") == 0)
        {
            Envelope env = new Envelope();
            env.setCommand("pm");
            
            //hannah hi hannah!
            String targetAndText = message.substring(4, message.length());
            
            //hann
            String target = targetAndText.substring(0, targetAndText.indexOf(" "));
            
            //h hi hannah!
            //this line is storing the text of the pm
            //targetAndText is the original message without #pm
            //substring lets us get part of a string
            //indexOf(" ") lets us get the position of the first space
            //we add +1 so we dont include the space in the text
            String text = targetAndText.substring(targetAndText.indexOf(" ")+1, targetAndText.length());
            
            env.setArg(target);
            env.setData(text);
            
            //try sending the envelope to the server
            try {
                sendToServer(env);
            } catch (IOException e) {
                clientUI.display("Could not send message to server.  Terminating client.......");
                quit();
            }
                      
        }
        
        if(message.equals("#who"))
        {
            Envelope env = new Envelope();
            env.setCommand("who");
            
            //try sending the envelope to the server
            try {
                sendToServer(env);
            } catch (IOException e) {
                clientUI.display("Could not send message to server.  Terminating client.......");
                quit();
            }
        }
    }

}
//End of ChatClient class