import java.util.ArrayList;


public class EchoServer extends AbstractServer {
    //Class variables *************************************************

    /**
     * The default port to listen on.
     */
    final public static int DEFAULT_PORT = 5555;

    //Constructors ****************************************************
    /**
     * Constructs an instance of the echo server.
     *
     * @param port The port number to connect on.
     */
    public EchoServer(int port) {

        super(port);

        try {
            this.listen(); //Start listening for connections
        } catch (Exception ex) {
            System.out.println("ERROR - Could not listen for clients!");
        }

    }

    //Instance methods ************************************************
    /**
     * This method handles any messages received from the client.
     *
     * @param msg The message received from the client.
     * @param client The connection from which the message originated.
     */
    public void handleMessageFromClient(Object msg, ConnectionToClient client) {
        if(msg instanceof Envelope)
        {
            //handle as command
            Envelope env = (Envelope) msg;
            handleCommandFromClient(env, client);
        }
        else
        {
            //handle as message
            System.out.println("Message received: " + msg + " from " + client);
            
            String room = (String)client.getInfo("room");
            
            //if the client has a user id
            if(client.getInfo("UserId") != null)
            {
                this.sendToAllClientsInRoom(client.getInfo("UserId") + ": " + msg, room);
            }
            else
            {
                this.sendToAllClientsInRoom(msg, room);
            }
        }
    }
    
    public void handleCommandFromClient(Envelope env, ConnectionToClient client)
    {
        //command - "setName"
        //arg - 
        //data - String - new userId for the client
        if(env.getCommand().equals("setName"))
        {
            //set the UserId of the client using the hashmap
            client.setInfo("UserId", env.getData());
        }
        
        //command - "join"
        //arg - 
        //data - String - room the user wants to join
        if(env.getCommand().equals("join"))
        {
            client.setInfo("room", env.getData());
        }
        
        //command - "pm"
        //arg - target of the pm
        //data - String - message for the target
        if(env.getCommand().equals("pm"))
        {
            String target = env.getArg();
            String pm = (String)env.getData();
            
            sendToTarget(pm, target);
        }
        
        //command - "who"
        //arg - 
        //data - 
        if(env.getCommand().equals("who"))
        {
            //create a new envelope
            Envelope returnEnv = new Envelope();
            returnEnv.setCommand("who");
            
            //get an ArrayList of all clients in the same room
            ArrayList<String> usersInRoom = getAllClientsInRoom((String)client.getInfo("room"));
            returnEnv.setData(usersInRoom);
            
            //send envelope back to client
            try
            {
                client.sendToClient(returnEnv);
            }
            catch(Exception e)
            {
                
            }
        }
    }

    /**
     * Sends message to all clients in the room specified
     * @param msg The message to be sent
     * @param room The room we are sending a message to
     */
    public void sendToAllClientsInRoom(Object msg, String room) {
        Thread[] clientThreadList = getClientConnections();

        for (int i = 0; i < clientThreadList.length; i++) {
            //cast current item as ConnectionToClient
            ConnectionToClient currClient = (ConnectionToClient) clientThreadList[i];
            
            //check which room this client is in
            if(currClient.getInfo("room").equals(room))
            {
                //only send messages if the room is correct
                try {                
                    currClient.sendToClient(msg);
                } catch (Exception ex) {
                }
            }
        }
    }
    
    public void sendToTarget(Object msg, String targetId) {
        Thread[] clientThreadList = getClientConnections();

        for (int i = 0; i < clientThreadList.length; i++) {
            //cast current item as ConnectionToClient
            ConnectionToClient currClient = (ConnectionToClient) clientThreadList[i];
            
            if(currClient.getInfo("UserId") != null)
            {
                //check which room this client is in
                if(currClient.getInfo("UserId").equals(targetId))
                {
                    //only send messages if the room is correct
                    try {                
                        currClient.sendToClient(msg);
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }
    
    /**
     * Loops through all connected clients and adds their username to the list if
     * they are in the specified room
     * @param room room name of the room we are searching
     * @return 
     */
    public ArrayList<String> getAllClientsInRoom(String room)
    {
        ArrayList<String> result = new ArrayList<String>();
        
        //loop through all clients
        Thread[] clientThreadList = getClientConnections();

        for (int i = 0; i < clientThreadList.length; i++) {
            //cast current item as ConnectionToClient
            ConnectionToClient currClient = (ConnectionToClient) clientThreadList[i];
            
            //check which room this client is in
            if(currClient.getInfo("room").equals(room))
            {
                //if room name matches, add to list
                if(currClient.getInfo("UserId") != null)
                {
                    String UserId = (String)currClient.getInfo("UserId");
                    result.add(UserId);
                }
            }
        }
        
        //return list
        return result;
    }
    
    /**
     * This method overrides the one in the superclass. Called when the server
     * starts listening for connections.
     */
    protected void serverStarted() {
        System.out.println("Server listening for connections on port " + getPort());
    }

    /**
     * This method overrides the one in the superclass. Called when the server
     * stops listening for connections.
     */
    protected void serverStopped() {
        System.out.println("Server has stopped listening for connections.");
    }

    //Class methods ***************************************************
    /**
     * This method is responsible for the creation of the server instance (there
     * is no UI in this phase).
     *
     * @param args[0] The port number to listen on. Defaults to 5555 if no
     * argument is entered.
     */
    public static void main(String[] args) {
        int port = 0; //Port to listen on

        try
        {
            //try to grab info from the arguments
            port = Integer.parseInt(args[0]);
        }
        catch(ArrayIndexOutOfBoundsException aioobe)
        {
            //if there are no arguments, use default values
            port = DEFAULT_PORT; //Set port to 5555
        }
        
        EchoServer sv = new EchoServer(port);

        try {
            sv.listen(); //Start listening for connections
        } catch (Exception ex) {
            System.out.println("ERROR - Could not listen for clients!");
        }

    }

    protected void clientConnected(ConnectionToClient client) {

        System.out.println("<Client Has Connected:" + client + ". Placing in room commons>");
        client.setInfo("room", "commons");
    }
    
    synchronized protected void clientException(
            ConnectionToClient client, Throwable exception)
    {
        System.out.println("<Client Has Disconnected>");
    }
}
//End of EchoServer class
