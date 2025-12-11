import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
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

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send welcome message with client ID
            JSONObject welcome = new JSONObject();
            welcome.put("type", "client_id");
            welcome.put("id", clientId);
            sendMessage(welcome.toString());

            // Send current canvas state
            JSONObject initialState = new JSONObject();
            initialState.put("type", "initial_state");
            initialState.put("bubbles", server.getCanvasState().toJSON().getJSONArray("bubbles"));
            initialState.put("connections", server.getCanvasState().toJSON().getJSONArray("connections"));
            sendMessage(initialState.toString());

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
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");

            switch (type) {
                case "bubble_create":
                    handleBubbleCreate(json);
                    break;
                case "bubble_update":
                    handleBubbleUpdate(json);
                    break;
                case "bubble_delete":
                    handleBubbleDelete(json);
                    break;
                case "connection_create":
                    handleConnectionCreate(json);
                    break;
                case "connection_delete":
                    handleConnectionDelete(json);
                    break;
                case "main_idea_update":
                    handleMainIdeaUpdate(json);
                    break;
                case "clear_all":
                    handleClearAll(json);
                    break;
                default:
                    System.err.println("Unknown message type: " + type);
            }
        } catch (Exception e) {
            System.err.println("Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleBubbleCreate(JSONObject json) {
        String id = json.getString("id");
        String text = json.getString("text");
        double x = json.getDouble("x");
        double y = json.getDouble("y");
        String color = json.optString("color", "#FFFFFF");
        String createdBy = json.optString("createdBy", clientId);

        Bubble bubble = new Bubble(id, x, y, text, color, createdBy);
        server.getCanvasState().addBubble(bubble);

        // Notify server GUI
        server.notifyBubbleCreated(bubble);

        // Broadcast to all clients except the sender
        server.broadcast(json.toString(), this);
    }

    private void handleBubbleUpdate(JSONObject json) {
        String id = json.getString("id");
        String text = json.optString("text", null);
        double x = json.optDouble("x", 0);
        double y = json.optDouble("y", 0);

        server.getCanvasState().updateBubble(id, text, x, y);

        // Notify server GUI
        Bubble bubble = server.getCanvasState().getBubble(id);
        if (bubble != null) {
            server.notifyBubbleUpdated(bubble);
        }

        // Broadcast to all clients except the sender
        server.broadcast(json.toString(), this);
    }

    private void handleBubbleDelete(JSONObject json) {
        String id = json.getString("id");
        server.getCanvasState().deleteBubble(id);

        // Notify server GUI
        server.notifyBubbleDeleted(id);

        // Broadcast to all clients except the sender
        server.broadcast(json.toString(), this);
    }

    private void handleConnectionCreate(JSONObject json) {
        String from = json.getString("from");
        String to = json.getString("to");

        Connection connection = new Connection(from, to);
        server.getCanvasState().addConnection(connection);

        // Broadcast to all clients except the sender
        server.broadcast(json.toString(), this);
    }

    private void handleConnectionDelete(JSONObject json) {
        String from = json.getString("from");
        String to = json.getString("to");

        server.getCanvasState().deleteConnection(from, to);

        // Broadcast to all clients except the sender
        server.broadcast(json.toString(), this);
    }

    private void handleMainIdeaUpdate(JSONObject json) {
        String text = json.getString("text");

        // Store in server's canvas state
        server.getCanvasState().setMainIdea(text);

        // Notify server GUI
        server.notifyMainIdeaUpdated(text);

        // Broadcast to all clients except the sender
        server.broadcast(json.toString(), this);
    }

    private void handleClearAll(JSONObject json) {
        // Clear server's canvas state
        server.clearAllBubbles();

        // Notify server GUI
        server.notifyClearAll();

        // Broadcast to all clients except the sender
        server.broadcast(json.toString(), this);
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
