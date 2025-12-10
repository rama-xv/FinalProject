import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CanvasState {
    private Map<String, Bubble> bubbles;
    private Map<String, Connection> connections;

    public CanvasState() {
        this.bubbles = new ConcurrentHashMap<>();
        this.connections = new ConcurrentHashMap<>();
    }

    // Bubble methods
    public synchronized void addBubble(Bubble bubble) {
        bubbles.put(bubble.getId(), bubble);
    }

    public synchronized void updateBubble(String id, String text, double x, double y) {
        Bubble bubble = bubbles.get(id);
        if (bubble != null) {
            if (text != null) {
                bubble.setText(text);
            }
            bubble.setX(x);
            bubble.setY(y);
        }
    }

    public synchronized void deleteBubble(String id) {
        bubbles.remove(id);

        // Remove all connections associated with this bubble
        List<String> toRemove = new ArrayList<>();
        for (Connection conn : connections.values()) {
            if (conn.getFromBubbleId().equals(id) || conn.getToBubbleId().equals(id)) {
                String connId = conn.getFromBubbleId() + "-" + conn.getToBubbleId();
                toRemove.add(connId);
            }
        }
        for (String connId : toRemove) {
            connections.remove(connId);
        }
    }

    // Connection methods
    public synchronized void addConnection(Connection connection) {
        String id = connection.getFromBubbleId() + "-" + connection.getToBubbleId();
        connections.put(id, connection);
    }

    public synchronized void deleteConnection(String fromId, String toId) {
        String id = fromId + "-" + toId;
        connections.remove(id);
    }

    public synchronized Bubble getBubble(String id) {
        return bubbles.get(id);
    }

    public synchronized Connection getConnection(String id) {
        return connections.get(id);
    }

    public synchronized Collection<Bubble> getAllBubbles() {
        return new ArrayList<>(bubbles.values());
    }

    public synchronized Collection<Connection> getAllConnections() {
        return new ArrayList<>(connections.values());
    }

    public synchronized JSONObject toJSON() {
        JSONObject json = new JSONObject();

        JSONArray bubblesArray = new JSONArray();
        for (Bubble bubble : bubbles.values()) {
            bubblesArray.put(new JSONObject(bubble.toJSON()));
        }
        json.put("bubbles", bubblesArray);

        JSONArray connectionsArray = new JSONArray();
        for (Connection connection : connections.values()) {
            connectionsArray.put(new JSONObject(connection.toJSON()));
        }
        json.put("connections", connectionsArray);

        return json;
    }
}