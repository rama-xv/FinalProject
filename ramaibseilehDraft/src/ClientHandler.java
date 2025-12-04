import java.io.*;
import java.net.*;
import org.json.*;

public class ClientHandler implements Runnable{
    private Socket socket;
    private BrainstormServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String clientId;
    private boolean connected;

    public ClientHandler(Socket socket, BrainstormServer server) {
        this.socket = socket;
        this.server = server;
        this.clientId = "CLIENT_" + System.currentTimeMillis();
        this.connected = true;
    }



    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send welcome message with client ID
            sendMessage(Protocol.createWelcomeMessage(clientId));

            // Send current canvas state
            sendMessage(Protocol.createFullStateMessage(server.getCanvasState()));

            // Listen for messages from client
            String message;
            while (connected && (message = in.readLine()) != null) {
                handleMessage(message);
            }
        } catch (IOException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleMessage(String message) {
        try {
            String type = Protocol.getMessageType(message);
            JSONObject data = Protocol.getMessageData(message);

            if (type == null || data == null) {
                return;
            }

            switch (type) {
                case Protocol.ADD_BUBBLE:
                    handleAddBubble(data);
                    break;
                case Protocol.UPDATE_BUBBLE:
                    handleUpdateBubble(data);
                    break;
                case Protocol.DELETE_BUBBLE:
                    handleDeleteBubble(data);
                    break;
                case Protocol.ADD_CONNECTION:
                    handleAddConnection(data);
                    break;
                case Protocol.DELETE_CONNECTION:
                    handleDeleteConnection(data);
                    break;
                case Protocol.REQUEST_STATE:
                    sendMessage(Protocol.createFullStateMessage(server.getCanvasState()));
                    break;
                default:
                    System.err.println("Unknown message type: " + type);
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
        }
    }

    private void handleAddBubble(JSONObject data) {
        String id = data.getString("id");
        String text = data.getString("text");
        double x = data.getDouble("x");
        double y = data.getDouble("y");

        Bubble bubble = new Bubble(id, text, x, y);
        server.getCanvasState().addBubble(bubble);

        // Broadcast to all other clients
        server.broadcast(Protocol.createAddBubbleMessage(id, text, x, y), this);
    }

    private void handleUpdateBubble(JSONObject data) {
        String id = data.getString("id");
        String text = data.optString("text", null);
        double x = data.getDouble("x");
        double y = data.getDouble("y");

        server.getCanvasState().updateBubble(id, text, x, y);

        // Broadcast to all other clients
        server.broadcast(Protocol.createUpdateBubbleMessage(id, text, x, y), this);
    }

    private void handleDeleteBubble(JSONObject data) {
        String id = data.getString("id");
        server.getCanvasState().deleteBubble(id);

        // Broadcast to all other clients
        server.broadcast(Protocol.createDeleteBubbleMessage(id), this);
    }

    private void handleAddConnection(JSONObject data) {
        String id = data.getString("id");
        String from = data.getString("from");
        String to = data.getString("to");

        Connection connection = new Connection(id, from, to);
        server.getCanvasState().addConnection(connection);

        // Broadcast to all other clients
        server.broadcast(Protocol.createAddConnectionMessage(id, from, to), this);
    }

    private void handleDeleteConnection(JSONObject data) {
        String id = data.getString("id");
        server.getCanvasState().deleteConnection(id);

        // Broadcast to all other clients
        server.broadcast(Protocol.createDeleteConnectionMessage(id), this);
    }

    public void sendMessage(String message) {
        if (out != null && connected) {
            out.println(message);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        connected = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
        server.removeClient(this);
    }

    public String getClientId() {
        return clientId;
    }
}
