import javafx.application.Platform;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.*;

/**
 * MessageHandler.java
 * Processes all incoming/outgoing messages between client, server, and GUI.
 * Acts as the "translator" between JSON messages and Java objects.
 */
public class MessageHandler {

    // Data storage
    private Map<String, Bubble> bubbles;           // All bubbles (key = bubble ID)
    private List<Connection> connections;          // All connections between bubbles

    // References to other components
    private BrainstormClientGUI gui;               // The GUI (Person 3's work)
    private NetworkClient client;                  // The network client

    // Client identification
    private String clientId;                       // Unique ID for this client


    /**
     * Constructor - initializes empty data structures
     */
    public MessageHandler(BrainstormClientGUI gui) {
        this.gui = gui;
        this.bubbles = new HashMap<>();
        this.connections = new ArrayList<>();
        this.clientId = generateClientId();
    }

    /**
     * Set the network client reference (called after NetworkClient is created)
     */
    public void setNetworkClient(NetworkClient client) {
        this.client = client;
    }


    // INCOMING MESSAGE PROCESSING (from server)

    /**
     * Main handler for all incoming messages from server.
     * Parses JSON and routes to appropriate handler method.
     */
    public void handleIncomingMessage(String jsonMessage) {
        try {
            JSONObject json = new JSONObject(jsonMessage);
            String type = json.getString("type");

            // Route based on message type
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

                case "client_id":
                    // Server assigns us a unique client ID
                    this.clientId = json.getString("id");
                    System.out.println("Assigned client ID: " + clientId);
                    break;

                case "initial_state":
                    // Server sends all existing bubbles/connections when we first connect
                    handleInitialState(json);
                    break;

                default:
                    System.out.println("Unknown message type: " + type);
            }

        } catch (JSONException e) {
            System.err.println("ERROR: Failed to parse JSON message: " + jsonMessage);
            System.err.println("Error details: " + e.getMessage());
        }
    }

    /**
     * Handles creation of a new bubble
     */
    private void handleBubbleCreate(JSONObject json) {
        try {
            String id = json.getString("id");
            double x = json.getDouble("x");
            double y = json.getDouble("y");
            String text = json.getString("text");
            String color = json.optString("color", "#FFFFFF");
            String createdBy = json.optString("createdBy", "unknown");

            // Create bubble object
            Bubble bubble = new Bubble(id, x, y, text, color, createdBy);

            // Store in our data structure
            bubbles.put(id, bubble);

            // Update GUI on JavaFX thread (IMPORTANT!)
            Platform.runLater(() -> {
                if (gui != null) {
                    gui.addBubbleToCanvas(bubble);
                }
            });

            System.out.println("Created bubble: " + id + " at (" + x + ", " + y + ")");

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid bubble_create message format");
        }
    }

    /**
     * Handles update to existing bubble (position or text change)
     */
    private void handleBubbleUpdate(JSONObject json) {
        try {
            String id = json.getString("id");

            Bubble bubble = bubbles.get(id);
            if (bubble == null) {
                System.err.println("ERROR: Cannot update non-existent bubble: " + id);
                return;
            }

            // Update fields if present in message
            if (json.has("x")) {
                bubble.setX(json.getDouble("x"));
            }
            if (json.has("y")) {
                bubble.setY(json.getDouble("y"));
            }
            if (json.has("text")) {
                bubble.setText(json.getString("text"));
            }

            // Update GUI
            Platform.runLater(() -> {
                if (gui != null) {
                    gui.updateBubbleOnCanvas(bubble);
                }
            });

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid bubble_update message format");
        }
    }

    /**
     * Handles deletion of a bubble
     */
    private void handleBubbleDelete(JSONObject json) {
        try {
            String id = json.getString("id");

            Bubble bubble = bubbles.remove(id);
            if (bubble == null) {
                System.err.println("WARNING: Tried to delete non-existent bubble: " + id);
                return;
            }

            // Also remove any connections involving this bubble
            connections.removeIf(conn ->
                    conn.getFromBubbleId().equals(id) || conn.getToBubbleId().equals(id)
            );

            // Update GUI
            Platform.runLater(() -> {
                if (gui != null) {
                    gui.removeBubbleFromCanvas(id);
                }
            });

            System.out.println("Deleted bubble: " + id);

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid bubble_delete message format");
        }
    }

    /**
     * Handles creation of a connection between two bubbles
     */
    private void handleConnectionCreate(JSONObject json) {
        try {
            String fromId = json.getString("from");
            String toId = json.getString("to");

            // Verify both bubbles exist
            if (!bubbles.containsKey(fromId) || !bubbles.containsKey(toId)) {
                System.err.println("ERROR: Cannot create connection - bubble(s) don't exist");
                return;
            }

            // Create connection
            Connection conn = new Connection(fromId, toId);
            connections.add(conn);

            // Update GUI
            Platform.runLater(() -> {
                if (gui != null) {
                    gui.addConnectionToCanvas(conn);
                }
            });

            System.out.println("Created connection: " + fromId + " -> " + toId);

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid connection_create message format");
        }
    }

    /**
     * Handles deletion of a connection
     */
    private void handleConnectionDelete(JSONObject json) {
        try {
            String fromId = json.getString("from");
            String toId = json.getString("to");

            // Find and remove connection
            boolean removed = connections.removeIf(conn ->
                    conn.getFromBubbleId().equals(fromId) && conn.getToBubbleId().equals(toId)
            );

            if (!removed) {
                System.err.println("WARNING: Tried to delete non-existent connection");
                return;
            }

            // Update GUI
            Platform.runLater(() -> {
                if (gui != null) {
                    gui.removeConnectionFromCanvas(fromId, toId);
                }
            });

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid connection_delete message format");
        }
    }

    /**
     * Handles initial state message (sent when client first connects)
     * Contains all existing bubbles and connections
     */
    private void handleInitialState(JSONObject json) {
        try {
            // Clear existing data
            bubbles.clear();
            connections.clear();

            // Load bubbles
            if (json.has("bubbles")) {
                var bubblesArray = json.getJSONArray("bubbles");
                for (int i = 0; i < bubblesArray.length(); i++) {
                    JSONObject bubbleJson = bubblesArray.getJSONObject(i);
                    handleBubbleCreate(bubbleJson);
                }
            }

            // Load connections
            if (json.has("connections")) {
                var connectionsArray = json.getJSONArray("connections");
                for (int i = 0; i < connectionsArray.length(); i++) {
                    JSONObject connJson = connectionsArray.getJSONObject(i);
                    handleConnectionCreate(connJson);
                }
            }

            System.out.println("Loaded initial state: " + bubbles.size() +
                    " bubbles, " + connections.size() + " connections");

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid initial_state message format");
        }
    }


    // ============================================
    // OUTGOING ACTIONS (from GUI to server)
    // ============================================

    /**
     * User creates a new bubble in the GUI
     * Called by Person 3's GUI code when user clicks canvas
     */
    public void createBubble(double x, double y, String text) {
        String id = generateBubbleId();
        String color = "#FFFFFF";  // Default white, can be customized later

        // Create bubble locally first (optimistic update)
        Bubble bubble = new Bubble(id, x, y, text, color, clientId);
        bubbles.put(id, bubble);

        // Update GUI immediately (feels more responsive)
        if (gui != null) {
            gui.addBubbleToCanvas(bubble);
        }

        // Send to server to broadcast to other clients
        JSONObject json = new JSONObject();
        json.put("type", "bubble_create");
        json.put("id", id);
        json.put("x", x);
        json.put("y", y);
        json.put("text", text);
        json.put("color", color);
        json.put("createdBy", clientId);

        sendToServer(json);
    }

    /**
     * User updates a bubble (drags it or edits text)
     */
    public void updateBubble(String id, Double newX, Double newY, String newText) {
        Bubble bubble = bubbles.get(id);
        if (bubble == null) {
            System.err.println("ERROR: Cannot update non-existent bubble: " + id);
            return;
        }

        // Update locally
        if (newX != null) bubble.setX(newX);
        if (newY != null) bubble.setY(newY);
        if (newText != null) bubble.setText(newText);

        // Build JSON with only changed fields
        JSONObject json = new JSONObject();
        json.put("type", "bubble_update");
        json.put("id", id);
        if (newX != null) json.put("x", newX);
        if (newY != null) json.put("y", newY);
        if (newText != null) json.put("text", newText);

        sendToServer(json);
    }

    /**
     * User deletes a bubble
     */
    public void deleteBubble(String id) {
        bubbles.remove(id);

        // Remove connections
        connections.removeIf(conn ->
                conn.getFromBubbleId().equals(id) || conn.getToBubbleId().equals(id)
        );

        // Update GUI
        if (gui != null) {
            gui.removeBubbleFromCanvas(id);
        }

        // Send to server
        JSONObject json = new JSONObject();
        json.put("type", "bubble_delete");
        json.put("id", id);

        sendToServer(json);
    }

    /**
     * User creates a connection between two bubbles
     */
    public void createConnection(String fromId, String toId) {
        // Verify both bubbles exist
        if (!bubbles.containsKey(fromId) || !bubbles.containsKey(toId)) {
            System.err.println("ERROR: Cannot create connection - bubble(s) don't exist");
            return;
        }

        // Create locally
        Connection conn = new Connection(fromId, toId);
        connections.add(conn);

        // Update GUI
        if (gui != null) {
            gui.addConnectionToCanvas(conn);
        }

        // Send to server
        JSONObject json = new JSONObject();
        json.put("type", "connection_create");
        json.put("from", fromId);
        json.put("to", toId);

        sendToServer(json);
    }

    /**
     * User deletes a connection
     */
    public void deleteConnection(String fromId, String toId) {
        connections.removeIf(conn ->
                conn.getFromBubbleId().equals(fromId) && conn.getToBubbleId().equals(toId)
        );

        // Update GUI
        if (gui != null) {
            gui.removeConnectionFromCanvas(fromId, toId);
        }

        // Send to server
        JSONObject json = new JSONObject();
        json.put("type", "connection_delete");
        json.put("from", fromId);
        json.put("to", toId);

        sendToServer(json);
    }


    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Helper to send JSON to server
     */
    private void sendToServer(JSONObject json) {
        if (client != null && client.isConnected()) {
            client.sendMessage(json.toString());
        } else {
            System.err.println("ERROR: Cannot send message - not connected to server");
        }
    }

    /**
     * Generate unique bubble ID
     */
    private String generateBubbleId() {
        return "bubble_" + clientId + "_" + System.currentTimeMillis();
    }

    /**
     * Generate unique client ID
     */
    private String generateClientId() {
        return "client_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get all bubbles (for GUI to render)
     */
    public Collection<Bubble> getAllBubbles() {
        return bubbles.values();
    }

    // Get all connections (for GUI to render)
    public List<Connection> getAllConnections() {
        return new ArrayList<>(connections);
    }

    //Get specific bubble by ID

    public Bubble getBubble(String id) {
        return bubbles.get(id);
    }
}