import java.io.*;
import java.net.*;

/**
 * NetworkClient.java:Manages the connection between this client and the BrainStorm server.
 * Responsibilities:
 * 1. Connect to server using Socket
 * 2. Send json messages to server (bubble creation, updates, etc.)
 * 3. Listen for incoming messages from server (in background thread)
 * 4. Handle disconnection and reconnection
 */
public class NetworkClient {

    private Socket socket;//Socket - the actual network connection to the server.
    private PrintWriter out;    //PrintWriter - used to SEND text messages to the server(It writes to the socket's output stream.)
    private BufferedReader in;    //BufferedReader - used to RECEIVE text messages from the server(It reads from the socket's input stream.)
    private String serverAddress;    //The server's address
    private int serverPort;    //The port number the server is listening on(Must match the port Person 1's server uses)
    private MessageHandler messageHandler;    //processes incoming messages from the server, then pass it to this handler.
    private boolean isConnected;    //Flag to track if we're currently connected to the server.
    private Thread listenerThread;    // Thread that continuously listens for incoming messages

    /**
     Constructor:
     * Creates a new NetworkClient
     * @param serverAddress The server's IP or hostname
     * @param serverPort The server's port number
     * @param messageHandler The handler that processes incoming messages
     */
    public NetworkClient(String serverAddress, int serverPort, MessageHandler messageHandler) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.messageHandler = messageHandler;
        this.isConnected = false;
    }
    /*Connection Methods:
     * Connects to the BrainStorm server.
     * This creates the Socket and sets up input/output streams.
     *
     * Steps:
     * 1. Create Socket to server
     * 2. Get output stream $ input streams
     * 3. Start listening thread (to receive messages continuously)

     * return true if connection successful, false otherwise
     */
    public boolean connect() {
        try {
            // 1: Create socket connection to server
            System.out.println("Attempting to connect to " + serverAddress + ":" + serverPort);
            socket = new Socket(serverAddress, serverPort);

            // 2: Set up OUTPUT stream (for sending messages TO server)
            out = new PrintWriter(socket.getOutputStream(), true);

            // Step 3: Set up INPUT stream (for receiving messages FROM server)
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));// BufferedReader reads text line-by-line

            // Mark as connected
            isConnected = true;
            System.out.println("Connected to server successfully!");

            // 4: Start background thread to listen for incoming messages
            startListening();

            return true;

        } catch (UnknownHostException e) {
            // This happens if the server address is invalid
            System.err.println("ERROR: Unknown host " + serverAddress);
            System.err.println("Make sure the server address is correct!");
            return false;

        } catch (IOException e) {
            // This happens if connection fails
            System.err.println("ERROR: Could not connect to server at " + serverAddress + ":" + serverPort);
            System.err.println("Make sure is right!");
            return false;
        }
    }

    //Disconnects from the server cleanly, this closes all streams and the socket to avoid resource leaks

    public void disconnect() {
        try {
            // Mark as disconnected first (stops the listener thread)
            isConnected = false;

            System.out.println("Disconnecting from server...");

            // Close the streams and socket, close streams before socket
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            System.out.println("Disconnected successfully");

        } catch (IOException e) {
            System.err.println("ERROR: Problem during disconnect: " + e.getMessage());
        }
    }

    /*
     * Attempts to reconnect to the server.
     * Useful if connection is lost unexpectedly.
     *
     * @param maxAttempts How many times to try reconnecting
     * @return true if reconnection successful, false otherwise
     */
    public boolean reconnect(int maxAttempts) {
        System.out.println("Attempting to reconnect...");
        // First, clean up the old connection
        disconnect();

        // Try to reconnect multiple times
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            System.out.println("Reconnection attempt " + attempt + "/" + maxAttempts);
            if (connect()) {
                System.out.println("Reconnected successfully!");
                return true;
            }

            // If not last attempt, wait before trying again
            if (attempt < maxAttempts) {
                try {
                    System.out.println("Waiting 2 seconds before next attempt...");
                    Thread.sleep(2000);  // Wait 2 seconds
                } catch (InterruptedException e) {
                    // If interrupted, stop trying
                    System.out.println("Reconnection interrupted");
                    return false;
                }
            }
        }
        System.err.println("Failed to reconnect after " + maxAttempts + " attempts");
        return false;
    }

    // SENDING MESSAGES
    /**
     * Sends a message to the server.
     * The message should be in JSON format (created by your data models).
     *
     * @param message The message to send
     * @return true if sent successfully, false if not connected
     */
    public boolean sendMessage(String message) {
        // Check if connected first
        if (!isConnected || out == null) {
            System.err.println("ERROR: Cannot send message: not connected to server!");
            return false;
        }

        try {
            // Send the message
            out.println(message);

            // Check if there was an error writing
            if (out.checkError()) {
                System.err.println("ERROR: Failed to send message");
                // Try to reconnect
                isConnected = false;
                reconnect(3);
                return false;
            }
            System.out.println(" Sent to server: " + message);
            return true;

        } catch (Exception e) {
            System.err.println("ERROR: Exception while sending message: " + e.getMessage());
            isConnected = false;
            return false;
        }
    }

    // RECEIVING MESSAGES
    /**
     * Starts a background thread that continuously listens for messages from the server.
     * The thread runs this logic in a loop:
     * 1. Wait for message from server
     * 2. When message arrives, pass it to MessageHandler
     * 3. Repeat until disconnected
     */
    private void startListening() {
        // Create a new thread for listening
        listenerThread = new Thread(() -> {
            System.out.println("Started listening for server messages...");
            try {
                String message;
                // Keep reading messages while connected
                while (isConnected && (message = in.readLine()) != null) {

                    System.out.println("Received from server: " + message);

                    // Pass the message to the handler for processing to parse it and update the GUI
                    if (messageHandler != null)
                        messageHandler.handleIncomingMessage(message);

                }
                // If we get here, connection was lost
                System.out.println("Connection to server lost");
                isConnected = false;

            } catch (IOException e) {
                // This happens if reading fails (connection lost, server crashed, etc.)
                if (isConnected) {
                    System.err.println("ERROR: Lost connection to server");
                    isConnected = false;
                    // Try to reconnect automatically
                    System.out.println("Attempting automatic reconnection...");
                    reconnect(3);
                }
            }
        });

        // Mark thread as daemon (it will stop when main program stops)
        listenerThread.setDaemon(true);
        // Start the thread running
        listenerThread.start();
    }
    // UTILITY METHODS
    /**
     * Checks if currently connected to server.
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return isConnected && socket != null && !socket.isClosed();
    }

    /**
     * Gets the server address this client is configured to connect to.
     * @return server address as string
     */
    public String getServerAddress() {
        return serverAddress;
    }

    /**
     * Gets the server port this client is configured to connect to.
     * @return server port number
     */
    public int getServerPort() {
        return serverPort;
    }

    // MAIN METHOD (FOR TESTING)
    /**
     * Main method for testing the NetworkClient independently.
     * test client before the GUI is ready!
     */
    public static void main(String[] args) {
        System.out.println("=== NetworkClient Test ===");

        // Create a simple test message handler
        MessageHandler testHandler = new MessageHandler(null) {
            @Override
            public void handleIncomingMessage(String message) {
                System.out.println("TEST HANDLER: Received message: " + message);
            }
        };

        // Create the client
        NetworkClient client = new NetworkClient("localhost", 8080, testHandler);

        // Try to connect
        if (client.connect()) {
            System.out.println("Connection successful! Ready to send messages.");

            // Wait a moment for initial messages
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }

            // Send a VALID test message using JSON
            try {
                org.json.JSONObject testMsg = new org.json.JSONObject();
                testMsg.put("type", "bubble_create");
                testMsg.put("id", "test_bubble_1");
                testMsg.put("x", 100.0);
                testMsg.put("y", 200.0);
                testMsg.put("text", "Test from NetworkClient!");
                testMsg.put("color", "#FF5733");

                client.sendMessage(testMsg.toString());

            } catch (Exception e) {
                System.err.println("Error creating test message: " + e.getMessage());
            }

            // Keep the program running to receive messages
            System.out.println("Listening for messages... Press Ctrl+C to exit");
            try {
                Thread.sleep(30000);  // Wait 30 seconds
            } catch (InterruptedException e) {
                System.out.println("Test interrupted");
            }

            // Disconnect when done
            client.disconnect();

        } else {
            System.err.println("Connection failed. Check that the server is running!");
        }
    }
}