// Remove or comment out this import:
// import javafx.application.Platform;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.*;

/**
 * MessageHandler.java
 * Processes all incoming/outgoing messages between client, server, and GUI.
 */
public class MessageHandler {

    // Data storage
    private Map<String, Bubble> bubbles;
    private List<Connection> connections;

    // References to other components
    // Commented out temporarily since GUI doesn't exist yet
    // private BrainstormClientGUI gui;
    private Object gui;  // Placeholder for now

    private NetworkClient client;
    private String clientId;


    /**
     * Constructor - for now accepts null since GUI doesn't exist
     */
    public MessageHandler(Object gui) {
        this.gui = gui;
        this.bubbles = new HashMap<>();
        this.connections = new ArrayList<>();
        this.clientId = generateClientId();
    }

    public void setNetworkClient(NetworkClient client) {
        this.client = client;
    }
    // INCOMING MESSAGE PROCESSING
    public void handleIncomingMessage(String jsonMessage) {
        try {
            JSONObject json = new JSONObject(jsonMessage);
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
                case "client_id":
                    this.clientId = json.getString("id");
                    System.out.println("Assigned client ID: " + clientId);
                    break;
                case "initial_state":
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

    private void handleBubbleCreate(JSONObject json) {
        try {
            String id = json.getString("id");
            double x = json.getDouble("x");
            double y = json.getDouble("y");
            String text = json.getString("text");
            String color = json.optString("color", "#FFFFFF");
            String createdBy = json.optString("createdBy", "unknown");

            Bubble bubble = new Bubble(id, x, y, text, color, createdBy);
            bubbles.put(id, bubble);

            // GUI update removed temporarily - will add back when Person 3 is done
            // Platform.runLater(() -> {
            //     if (gui != null) {
            //         gui.addBubbleToCanvas(bubble);
            //     }
            // });

            System.out.println("Created bubble: " + id + " at (" + x + ", " + y + ")");

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid bubble_create message format");
        }
    }

    private void handleBubbleUpdate(JSONObject json) {
        try {
            String id = json.getString("id");
            Bubble bubble = bubbles.get(id);

            if (bubble == null) {
                System.err.println("ERROR: Cannot update non-existent bubble: " + id);
                return;
            }

            if (json.has("x")) bubble.setX(json.getDouble("x"));
            if (json.has("y")) bubble.setY(json.getDouble("y"));
            if (json.has("text")) bubble.setText(json.getString("text"));

            // GUI update removed temporarily
            System.out.println("Updated bubble: " + id);

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid bubble_update message format");
        }
    }

    private void handleBubbleDelete(JSONObject json) {
        try {
            String id = json.getString("id");
            Bubble bubble = bubbles.remove(id);

            if (bubble == null) {
                System.err.println("WARNING: Tried to delete non-existent bubble: " + id);
                return;
            }

            connections.removeIf(conn ->
                    conn.getFromBubbleId().equals(id) || conn.getToBubbleId().equals(id)
            );

            System.out.println("Deleted bubble: " + id);

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid bubble_delete message format");
        }
    }

    private void handleConnectionCreate(JSONObject json) {
        try {
            String fromId = json.getString("from");
            String toId = json.getString("to");

            if (!bubbles.containsKey(fromId) || !bubbles.containsKey(toId)) {
                System.err.println("ERROR: Cannot create connection - bubble(s) don't exist");
                return;
            }

            Connection conn = new Connection(fromId, toId);
            connections.add(conn);

            System.out.println("Created connection: " + fromId + " -> " + toId);

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid connection_create message format");
        }
    }

    private void handleConnectionDelete(JSONObject json) {
        try {
            String fromId = json.getString("from");
            String toId = json.getString("to");

            boolean removed = connections.removeIf(conn ->
                    conn.getFromBubbleId().equals(fromId) && conn.getToBubbleId().equals(toId)
            );

            if (removed) {
                System.out.println("Deleted connection: " + fromId + " -> " + toId);
            }

        } catch (JSONException e) {
            System.err.println("ERROR: Invalid connection_delete message format");
        }
    }

    private void handleInitialState(JSONObject json) {
        try {
            bubbles.clear();
            connections.clear();

            if (json.has("bubbles")) {
                JSONArray bubblesArray = json.getJSONArray("bubbles");
                for (int i = 0; i < bubblesArray.length(); i++) {
                    JSONObject bubbleJson = bubblesArray.getJSONObject(i);
                    handleBubbleCreate(bubbleJson);
                }
            }

            if (json.has("connections")) {
                JSONArray connectionsArray = json.getJSONArray("connections");
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

    // OUTGOING ACTIONS

    public void createBubble(double x, double y, String text) {
        String id = generateBubbleId();
        String color = "#FFFFFF";

        Bubble bubble = new Bubble(id, x, y, text, color, clientId);
        bubbles.put(id, bubble);

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

    public void updateBubble(String id, Double newX, Double newY, String newText) {
        Bubble bubble = bubbles.get(id);
        if (bubble == null) {
            System.err.println("ERROR: Cannot update non-existent bubble: " + id);
            return;
        }

        if (newX != null) bubble.setX(newX);
        if (newY != null) bubble.setY(newY);
        if (newText != null) bubble.setText(newText);

        JSONObject json = new JSONObject();
        json.put("type", "bubble_update");
        json.put("id", id);
        if (newX != null) json.put("x", newX);
        if (newY != null) json.put("y", newY);
        if (newText != null) json.put("text", newText);

        sendToServer(json);
    }

    public void deleteBubble(String id) {
        bubbles.remove(id);
        connections.removeIf(conn ->
                conn.getFromBubbleId().equals(id) || conn.getToBubbleId().equals(id)
        );

        JSONObject json = new JSONObject();
        json.put("type", "bubble_delete");
        json.put("id", id);

        sendToServer(json);
    }

    public void createConnection(String fromId, String toId) {
        if (!bubbles.containsKey(fromId) || !bubbles.containsKey(toId)) {
            System.err.println("ERROR: Cannot create connection - bubble(s) don't exist");
            return;
        }

        Connection conn = new Connection(fromId, toId);
        connections.add(conn);

        JSONObject json = new JSONObject();
        json.put("type", "connection_create");
        json.put("from", fromId);
        json.put("to", toId);

        sendToServer(json);
    }

    public void deleteConnection(String fromId, String toId) {
        connections.removeIf(conn ->
                conn.getFromBubbleId().equals(fromId) && conn.getToBubbleId().equals(toId)
        );

        JSONObject json = new JSONObject();
        json.put("type", "connection_delete");
        json.put("from", fromId);
        json.put("to", toId);

        sendToServer(json);
    }
    // UTILITY METHODS
    private void sendToServer(JSONObject json) {
        if (client != null && client.isConnected()) {
            client.sendMessage(json.toString());
        } else {
            System.err.println("ERROR: Cannot send message - not connected to server");
        }
    }

    private String generateBubbleId() {
        return "bubble_" + clientId + "_" + System.currentTimeMillis();
    }

    private String generateClientId() {
        return "client_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public Collection<Bubble> getAllBubbles() {
        return bubbles.values();
    }

    public List<Connection> getAllConnections() {
        return new ArrayList<>(connections);
    }

    public Bubble getBubble(String id) {
        return bubbles.get(id);
    }
    // TESTING

    public static void main(String[] args) {
        System.out.println("=== MessageHandler Test ===\n");

        MessageHandler handler = new MessageHandler(null);

        // Test handling incoming message
        String testMessage = "{\"type\":\"bubble_create\",\"id\":\"b1\",\"x\":100,\"y\":200,\"text\":\"Test Bubble\"}";
        handler.handleIncomingMessage(testMessage);

        System.out.println("\nTotal bubbles: " + handler.getAllBubbles().size());

        // Test creating bubble
        handler.createBubble(300, 400, "Another bubble");
        System.out.println("Total bubbles after create: " + handler.getAllBubbles().size());

        System.out.println("\n MessageHandler working correctly!");
    }
}