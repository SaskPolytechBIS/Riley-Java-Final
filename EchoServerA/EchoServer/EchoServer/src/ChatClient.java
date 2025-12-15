
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
        openConnection();
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
            Envelope returnEnv = (Envelope)msg;
            handleCommandFromServer(returnEnv);
        }
        else
        {
            clientUI.display(msg.toString());
        }
    }
    
    public void handleCommandFromServer(Envelope env)
    {
        //command - who
        //arg
        //data - ArrayList<String> - a list of all users in room
        if(env.getCommand().equals("who"))
        {
            ArrayList<String> usersInRoom = (ArrayList<String>)env.getData();
            clientUI.display("---The users in the room are---");
            for(int i = 0; i < usersInRoom.size(); i++)
            {
                clientUI.display(usersInRoom.get(i));
            }
            
        }
    }

    /**
     * This method handles all data coming from the UI
     *
     * @param message The message from the UI.
     */
    public void handleMessageFromClientUI(String message) {

        //if the first character is a # send it to handle client command
        if (message.charAt(0) == '#') {

            handleClientCommand(message);

        } else {
            //send message to server
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

    //Extends the hook method from Abstract client. This method is called at the beginning of
    //the AbstractClient run method
    protected void connectionEstablished()
    {
        System.out.println("Connected to server at "+getHost()+" on port "+getPort());
    }
    
    protected void connectionException(Exception exception) {

        System.out.println("Server has shut down");

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

        //#setPort 5556
        if (message.indexOf("#setPort") == 0) {

            if (isConnected()) {
                clientUI.display("Cannot change port while connected");
            } else {
                //setPort()
                //Integer.parseInt()
                //message.substring(8, message.length()).trim()
                // "5556"
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
            //create envelope to send to server
            Envelope env = new Envelope();
            env.setCommand("setName");
            
            //grab the name from the message and add it as the envelope's data
            String name = message.substring(9, message.length());
            env.setData(name);
            
            //send envelope to server
            try {
                sendToServer(env);
            } catch (IOException e) {
                clientUI.display("Could not send message to server.  Terminating client.......");
                quit();
            }
        }
        
        //ex - #join room1
        if(message.indexOf("#join") == 0)
        {
            //create an envelope to send to server
            Envelope env = new Envelope();
            env.setCommand("join");
            
            //grab the name of the room the user wants to join
            String room = message.substring(6, message.length());
            env.setData(room);
            
            //send envelope to server
            try {
                sendToServer(env);
            } catch (IOException e) {
                clientUI.display("Could not send message to server.  Terminating client.......");
                quit();
            }
        }
        
        //ex - #pm hannah hi hannah
        if(message.indexOf("#pm") == 0)
        {
            //create envelope for command
            Envelope env = new Envelope();
            env.setCommand("pm");
            
            //hannah hi hannah
            String targetAndPM = message.substring(4, message.length());
            
            //hannah
            //this line gets from the first character until the first instance of a space
            String target = targetAndPM.substring(0,targetAndPM.indexOf(" "));
            
            //hi hannah
            //this line gets from the character after the first space till the end of the message
            String pm = targetAndPM.substring(targetAndPM.indexOf(" ") + 1, targetAndPM.length());
            
            env.setArg(target);
            env.setData(pm);
            
            //send envelope to server
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
            
            //send envelope to server
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
