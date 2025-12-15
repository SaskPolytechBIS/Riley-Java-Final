import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * EchoServer with #ftpUpload handling and #ftplist / #ftpget support.
 *
 * Clean version: DEBUG prints removed.
 */
public class EchoServer extends AbstractServer {
    //Class variables *************************************************

    /**
     * The default port to listen on.
     */
    final public static int DEFAULT_PORT = 5555;

    //Constructors ****************************************************
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
     */
    public void handleMessageFromClient(Object msg, ConnectionToClient client) {
        if (msg instanceof Envelope) {
            Envelope env = (Envelope) msg;
            //handle command
            handleCommandFromClient(env, client);
        } else {
            System.out.println("Message received: " + msg + " from " + client);

            // get the name of the room the sending client is in; ensure a default
            String room = (String) client.getInfo("room");
            if (room == null) {
                room = "commons";
                client.setInfo("room", room);
            }

            // if the client has a user ID add it to their messages
            if (client.getInfo("UserId") != null) {
                this.sendToAllClientsInRoom(client.getInfo("UserId") + ": " + msg, room);
            } else {
                this.sendToAllClientsInRoom(msg, room);
            }
        }
    }

    /**
     * Handle Envelope commands from clients.
     */
    public void handleCommandFromClient(Envelope env, ConnectionToClient client) {
        if (env == null || env.getCommand() == null) {
            return;
        }

        // command: setName
        if (env.getCommand().equals("setName")) {
            String userId = (String) env.getData();
            client.setInfo("UserId", userId);
            return;
        }

        // command: join
        if (env.getCommand().equals("join")) {
            String room = (String) env.getData();
            if (room == null) {
                room = "commons";
            }
            client.setInfo("room", room);
            if (client.getInfo("UserId") != null) {
                String UserId = (String) client.getInfo("UserId");
                System.out.println("<" + UserId + " has joined room " + room + ">");
            } else {
                System.out.println("<User has joined room " + room + ">");
            }
            return;
        }

        // command: pm
        if (env.getCommand().equals("pm")) {
            String target = env.getArg();
            String text = (String) env.getData();
            sendToClientByUserId(text, target);
            return;
        }

        // command: who
        if (env.getCommand().equals("who")) {
            String room = (String) client.getInfo("room");
            if (room == null) {
                room = "commons";
            }
            ArrayList<String> clientList = getAllClientsInRoom(room);
            Envelope returnEnv = new Envelope();
            returnEnv.setCommand("who");
            returnEnv.setData(clientList);
            try {
                client.sendToClient(returnEnv);
            } catch (IOException e) {
                System.out.println("Something went wrong when trying to send who return envelope");
                e.printStackTrace();
            }
            return;
        }

        // command: #ftpUpload
        // arg: filename
        // data: byte[]
        if (env.getCommand().equals("#ftpUpload")) {
            String filename = env.getArg();
            Object dataObj = env.getData();

            if (filename == null || dataObj == null || !(dataObj instanceof byte[])) {
                System.out.println("Invalid #ftpUpload envelope received from " + client);
                try {
                    client.sendToClient("Error: invalid upload (missing filename or data).");
                } catch (IOException ignore) {
                }
                return;
            }

            byte[] fileBytes = (byte[]) dataObj;

            // Sanitize filename (strip any path components)
            String safeName = new File(filename).getName();

            // Ensure uploads directory exists
            File dir = new File("uploads");
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    String msg = "Error: Failed to create uploads directory on server.";
                    System.out.println(msg);
                    try {
                        client.sendToClient(msg);
                    } catch (IOException ignore) {
                    }
                    return;
                }
            }

            File out = new File(dir, safeName);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(fileBytes);
                System.out.println("Saved uploaded file " + out.getAbsolutePath() + " from " + client);
                try {
                    client.sendToClient("Upload successful: " + out.getName());
                } catch (IOException ignore) {
                }
            } catch (IOException e) {
                System.out.println("Error saving uploaded file from " + client + ": " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient("Error saving file: " + e.getMessage());
                } catch (IOException ignore) {
                }
            }
            return;
        }

        // NEW: #ftplist - return list of filenames in uploads/
        if (env.getCommand().equals("#ftplist")) {
            File dir = new File("uploads");
            ArrayList<String> list = new ArrayList<>();
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            list.add(f.getName());
                        }
                    }
                }
            }
            Envelope returnEnv = new Envelope();
            returnEnv.setCommand("ftplist"); // response command
            returnEnv.setData(list);
            try {
                client.sendToClient(returnEnv);
            } catch (IOException e) {
                System.out.println("Failed to send ftplist to " + client);
                e.printStackTrace();
            }
            return;
        }

        // NEW: #ftpget - send the requested file (if present) back to requesting client
        if (env.getCommand().equals("#ftpget")) {
            String filename = env.getArg();
            if (filename == null) {
                try {
                    client.sendToClient("Error: missing filename for ftpget.");
                } catch (IOException ignore) {
                }
                return;
            }
            String safeName = new File(filename).getName();
            File f = new File("uploads", safeName);
            if (!f.exists() || !f.isFile()) {
                try {
                    client.sendToClient("Error: file not found: " + safeName);
                } catch (IOException ignore) {
                }
                return;
            }
            try {
                byte[] data = Files.readAllBytes(f.toPath());
                Envelope returnEnv = new Envelope();
                returnEnv.setCommand("#ftpget"); // response command with file bytes
                returnEnv.setArg(safeName);
                returnEnv.setData(data);
                client.sendToClient(returnEnv);
            } catch (IOException e) {
                System.out.println("Error reading file for ftpget: " + e.getMessage());
                e.printStackTrace();
                try {
                    client.sendToClient("Error: could not read file: " + e.getMessage());
                } catch (IOException ignore) {
                }
            }
            return;
        }
    }

    public ArrayList<String> getAllClientsInRoom(String room) {
        ArrayList<String> result = new ArrayList<String>();

        if (room == null) {
            return result;
        }

        //get array of all clients
        Thread[] clientThreadList = getClientConnections();
        if (clientThreadList == null) {
            return result;
        }

        //loop through all clients
        for (Thread t : clientThreadList) {
            if (!(t instanceof ConnectionToClient)) {
                continue;
            }
            ConnectionToClient currClient = (ConnectionToClient) t;
            Object clientRoom = currClient.getInfo("room");
            if (clientRoom != null && room.equals(clientRoom)) {
                if (currClient.getInfo("UserId") != null) {
                    result.add((String) currClient.getInfo("UserId"));
                }
            }
        }
        return result;
    }

    /**
     * Send message to all clients in specified room
     *
     * @param msg - The message to send
     * @param room - The room to send to
     */
    public void sendToAllClientsInRoom(Object msg, String room) {
        if (room == null) {
            return;
        }

        //get array of all clients
        Thread[] clientThreadList = getClientConnections();
        if (clientThreadList == null) {
            return;
        }

        //loop through all clients
        for (int i = 0; i < clientThreadList.length; i++) {
            if (!(clientThreadList[i] instanceof ConnectionToClient)) {
                continue;
            }
            ConnectionToClient currClient = ((ConnectionToClient) clientThreadList[i]);

            Object clientRoom = currClient.getInfo("room");
            if (clientRoom != null && room.equals(clientRoom)) {
                try {
                    //send message to client
                    currClient.sendToClient(msg);
                } catch (Exception ex) {
                    System.out.println("Failed to send to client " + currClient + ": " + ex.getMessage());
                }
            }
        }
    }

    public void sendToClientByUserId(Object msg, String target) {
        if (target == null) {
            return;
        }

        //get array of all clients
        Thread[] clientThreadList = getClientConnections();
        if (clientThreadList == null) {
            return;
        }

        //loop through all clients
        for (int i = 0; i < clientThreadList.length; i++) {
            if (!(clientThreadList[i] instanceof ConnectionToClient)) {
                continue;
            }
            ConnectionToClient currClient = ((ConnectionToClient) clientThreadList[i]);

            Object uid = currClient.getInfo("UserId");
            if (uid != null && target.equals(uid)) {
                try {
                    currClient.sendToClient(msg);
                } catch (Exception ex) {
                    System.out.println("Failed to send pm to " + target + ": " + ex.getMessage());
                }
            }
        }
    }

    protected void serverStarted() {
        System.out.println("Server listening for connections on port " + getPort());
    }

    protected void serverStopped() {
        System.out.println("Server has stopped listening for connections.");
    }

    //Class methods ***************************************************
    public static void main(String[] args) {
        int port = 0; //Port to listen on

        try {
            port = Integer.parseInt(args[0]);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
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