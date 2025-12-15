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
            Envelope env = (Envelope) msg;
            //handle command
            handleCommandFromClient(env, client);
        }
        else
        {
            System.out.println("Message received: " + msg + " from " + client);
            
            
            //get the name of the room the sending client is in
            String room = (String)client.getInfo("room");
            
            //if the client has a user ID add it to their messages
            if(client.getInfo("UserId") != null)
            {
                this.sendToAllClientsInRoom(client.getInfo("UserId") + ": " +msg, room);
            }
            else
            {
                this.sendToAllClientsInRoom(msg,room);
            }
        }
    }
    
    public void handleCommandFromClient(Envelope env, ConnectionToClient client)
    {
        //command: setName
        //arg: 
        //data: String - new name for client
        if(env.getCommand().equals("setName"))
        {
            String userId = (String) env.getData();
            client.setInfo("UserId", userId);
        }
        
        //command: join
        //arg:
        //data: String - name for room the user wants to join
        if(env.getCommand().equals("join"))
        {
            String room = (String)env.getData();
            client.setInfo("room", room);
            if(client.getInfo("UserId") != null)
            {
                String UserId = (String)client.getInfo("UserId");
                System.out.println("<"+UserId+" has joined room "+room+">");
            }
            else
            {
                System.out.println("<User has joined room "+room+">");
            }
        }
        
        //command: pm
        //arg: target for the pm
        //data: String - text of the pm
        if(env.getCommand().equals("pm"))
        {
            String target = env.getArg();
            String text = (String)env.getData();
            
            sendToClientByUserId(text,target);
        }
        
        if(env.getCommand().equals("who"))
        {
            //find out what room the person who sent the command is in
            String room = (String)client.getInfo("room");
            
            //find all users in the same room and store in an ArrayList<String>
            ArrayList<String> clientList = getAllClientsInRoom(room);
            
            //create an envelope with an Id of "who"
            //add the arraylist as the data
            Envelope returnEnv = new Envelope();
            returnEnv.setCommand("who");
            returnEnv.setData(clientList);
            
            //use the sendToClient function from ConnectionToClient to send the envelope
            //back to the client who sent the command
            try{
                client.sendToClient(returnEnv);
            }
            catch(Exception e)
            {
                System.out.println("Something went wrong when trying to send who return envelope");
                e.printStackTrace();
            }
        }
    }

    public ArrayList<String> getAllClientsInRoom(String room)
    {
        ArrayList<String> result = new ArrayList<String>();
        
        //get array of all clients
        Thread[] clientThreadList = getClientConnections();

        //loop through all clients
        for (int i = 0; i < clientThreadList.length; i++) {
            //cast the client thread as a connectionToClient object
            ConnectionToClient currClient = ((ConnectionToClient) clientThreadList[i]);
            
            //before we add client to list, make sure it is in the specified room
            if(room.equals(currClient.getInfo("room")))
            {
                //check if the currClient has a UserId
                if(currClient.getInfo("UserId")!= null)
                {
                    //add user id to the list
                    result.add( (String)currClient.getInfo("UserId") );
                }
            }
        }
        return result;
    }
    
    /**
     * Send message to all clients in specified room
     * @param msg - The message to send
     * @param room - The room to send to
     */
    public void sendToAllClientsInRoom(Object msg, String room) {
        //get array of all clients
        Thread[] clientThreadList = getClientConnections();

        //loop through all clients
        for (int i = 0; i < clientThreadList.length; i++) {
            //cast the client thread as a connectionToClient object
            ConnectionToClient currClient = ((ConnectionToClient) clientThreadList[i]);
            
            //before we send to a client, make sure it is in the specified room
            if(room.equals(currClient.getInfo("room")))
            {
                try {
                    //send message to client
                    currClient.sendToClient(msg);
                } catch (Exception ex) {
                }
            }
        }
    }
    
    public void sendToClientByUserId(Object msg, String target) {
        //get array of all clients
        Thread[] clientThreadList = getClientConnections();

        //loop through all clients
        for (int i = 0; i < clientThreadList.length; i++) {
            //cast the client thread as a connectionToClient object
            ConnectionToClient currClient = ((ConnectionToClient) clientThreadList[i]);
            
            //make sure the client has a user id before trying to read it
            if(currClient.getInfo("UserId") != null)
            {
                //before we send to a client, make sure it is in the specified room
                if(target.equals(currClient.getInfo("UserId")))
                {
                    try {
                        //send message to client
                        currClient.sendToClient(msg);
                    } catch (Exception ex) {
                    }
                }
            }
        }
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
            port = Integer.parseInt(args[0]);
        }
        catch(ArrayIndexOutOfBoundsException aioobe)
        {
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

        System.out.println("<Client Connected:" + client + ". Placing them in room commons>");
        client.setInfo("room", "commons");

    }
    
    synchronized protected void clientException(
            ConnectionToClient client, Throwable exception) {
        System.out.println("<Client has disconnected>");
    }
}
//End of EchoServer class
