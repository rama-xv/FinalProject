import org.json.JSONObject;
/**
 * Connection.java
 * Represents a line/arrow connecting two bubbles.
 * Shows relationships between ideas in the brainstorm.
 */
public class Connection {

    // The two bubbles being connected
    private String fromBubbleId;        // Starting bubble ID
    private String toBubbleId;          // Ending bubble ID

    // Visual properties
    private String color;               // Line color (hex code)
    private double thickness;           // Line width in pixels
    private boolean isDirected;         // If true, show arrow (A → B); if false, just line (A — B)

    // Metadata
    private String createdBy;           // Which client created this connection
    private long timestamp;             // When it was created

    // CONSTRUCTORS
    //Full constructor with all properties
    public Connection(String fromBubbleId, String toBubbleId, String color) {
        this.fromBubbleId = fromBubbleId;
        this.toBubbleId = toBubbleId;
        this.color = color;
        this.thickness = thickness;
        this.isDirected = isDirected;
        this.createdBy = createdBy;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Simple constructor with defaults
     * Default: black line, 2px thick, directed (with arrow)
     */
    public Connection(String fromBubbleId, String toBubbleId) {
        this(fromBubbleId, toBubbleId, "#000000");
    }

    /**
     * Constructor from JSON object (for deserializing from server)
     */
    public Connection(JSONObject json) {
        this.fromBubbleId = json.getString("from");
        this.toBubbleId = json.getString("to");
        this.color = json.optString("color", "#000000");
        this.thickness = json.optDouble("thickness", 2.0);
        this.isDirected = json.optBoolean("isDirected", true);
        this.createdBy = json.optString("createdBy", "unknown");
        this.timestamp = json.optLong("timestamp", System.currentTimeMillis());
    }

    // GETTERS


    public String getFromBubbleId() {
        return fromBubbleId;
    }

    public String getToBubbleId() {
        return toBubbleId;
    }

    public String getColor() {
        return color;
    }

    public double getThickness() {
        return thickness;
    }

    public boolean isDirected() {
        return isDirected;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getTimestamp() {
        return timestamp;
    }
    // SETTERS

    public void setColor(String color) {
        this.color = color;
    }

    public void setThickness(double thickness) {
        this.thickness = thickness;
    }

    public void setDirected(boolean directed) {
        this.isDirected = directed;
    }
    // JSON SERIALIZATION

    /**
     * Convert connection to JSON string for sending over network
     */
    public String toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "connection_create");
        json.put("from", fromBubbleId);
        json.put("to", toBubbleId);
        json.put("color", color);
        json.put("thickness", thickness);
        json.put("isDirected", isDirected);
        json.put("createdBy", createdBy);
        json.put("timestamp", timestamp);

        return json.toString();
    }

    /**
     * Create JSON for deleting this connection
     */
    public String toDeleteJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "connection_delete");
        json.put("from", fromBubbleId);
        json.put("to", toBubbleId);

        return json.toString();
    }

    // UTILITY METHODS

    /**
     * Check if this connection involves a specific bubble
     * Useful for finding all connections when deleting a bubble
     */
    public boolean involves(String bubbleId) {
        return fromBubbleId.equals(bubbleId) || toBubbleId.equals(bubbleId);
    }

    /**
     * Check if this connection involves either of two bubbles
     */
    public boolean involves(String bubbleId1, String bubbleId2) {
        return involves(bubbleId1) || involves(bubbleId2);
    }

    /**
     * Get the other bubble ID in this connection
     * If you give it the "from" bubble, it returns the "to" bubble, and vice versa
     */
    public String getOtherBubble(String bubbleId) {
        if (fromBubbleId.equals(bubbleId)) {
            return toBubbleId;
        } else if (toBubbleId.equals(bubbleId)) {
            return fromBubbleId;
        }
        return null;  // Given bubble is not part of this connection
    }

    /**
     * Check if this is a self-connection (bubble connected to itself)
     */
    public boolean isSelfConnection() {
        return fromBubbleId.equals(toBubbleId);
    }

    /**
     * Reverse the direction of this connection
     * Changes A → B to B → A
     */
    public void reverse() {
        String temp = fromBubbleId;
        fromBubbleId = toBubbleId;
        toBubbleId = temp;
    }

    /**
     * Create a reversed copy of this connection
     */
    public Connection reversed() {
        return new Connection(toBubbleId, fromBubbleId, color);
    }

    /**
     * Create a copy of this connection
     */
    public Connection copy() {
        return new Connection(fromBubbleId, toBubbleId, color);
    }

    /**
     * Check if this connection is identical to another (same bubbles, same direction)
     */
    public boolean matches(Connection other) {
        return this.fromBubbleId.equals(other.fromBubbleId) &&
                this.toBubbleId.equals(other.toBubbleId);
    }

    /**
     * Check if this connection is the same as another, ignoring direction
     * A → B is equivalent to B → A
     */
    public boolean matchesUndirected(Connection other) {
        return matches(other) ||
                (this.fromBubbleId.equals(other.toBubbleId) &&
                        this.toBubbleId.equals(other.fromBubbleId));
    }

    // OBJECT OVERRIDES
    @Override
    public String toString() {
        String arrow = isDirected ? "→" : "—";
        return String.format("Connection[%s %s %s, color=%s, thickness=%.1f]",
                fromBubbleId, arrow, toBubbleId, color, thickness);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Connection other = (Connection) obj;
        // Connections are equal if they connect the same bubbles in the same direction
        return fromBubbleId.equals(other.fromBubbleId) &&
                toBubbleId.equals(other.toBubbleId);
    }

    @Override
    public int hashCode() {
        return fromBubbleId.hashCode() * 31 + toBubbleId.hashCode();
    }


    // TESTING
    /**
     * Test the Connection class independently
     */
    public static void main(String[] args) {
        System.out.println("=== Connection Class Test ===\n");

        // Test 1: Create connection
        Connection conn1 = new Connection("bubble1", "bubble2");
        System.out.println("Created: " + conn1);

        // Test 2: Convert to JSON
        System.out.println("\nJSON format:");
        System.out.println(conn1.toJSON());

        // Test 3: Check if involves bubble
        System.out.println("\nInvolves 'bubble1'? " + conn1.involves("bubble1"));
        System.out.println("Involves 'bubble3'? " + conn1.involves("bubble3"));

        // Test 4: Get other bubble
        System.out.println("\nFrom 'bubble1', other bubble is: " +
                conn1.getOtherBubble("bubble1"));
        System.out.println("From 'bubble2', other bubble is: " +
                conn1.getOtherBubble("bubble2"));

        // Test 5: Check self-connection
        Connection selfConn = new Connection("bubble1", "bubble1");
        System.out.println("\nIs self-connection? " + selfConn.isSelfConnection());

        // Test 6: Reverse connection
        System.out.println("\nOriginal: " + conn1);
        Connection reversed = conn1.reversed();
        System.out.println("Reversed: " + reversed);

        // Test 7: Match comparisons
        Connection conn2 = new Connection("bubble1", "bubble2");
        Connection conn3 = new Connection("bubble2", "bubble1");

        System.out.println("\nConn1 matches Conn2? " + conn1.matches(conn2));
        System.out.println("Conn1 matches Conn3 (directed)? " + conn1.matches(conn3));
        System.out.println("Conn1 matches Conn3 (undirected)? " +
                conn1.matchesUndirected(conn3));

        // Test 8: Styled connection
        Connection styledConn = new Connection(
                "bubble1", "bubble2", "#FF5733"
        );
        System.out.println("\nStyled connection: " + styledConn);
        System.out.println("JSON: " + styledConn.toJSON());

        // Test 9: Create from JSON
        System.out.println("\n=== Creating from JSON ===");
        JSONObject json = new JSONObject();
        json.put("from", "bubble_a");
        json.put("to", "bubble_b");
        json.put("color", "#00FF00");
        json.put("thickness", 4.0);
        json.put("isDirected", true);

        Connection conn4 = new Connection(json);
        System.out.println("Created from JSON: " + conn4);

        // Test 10: Delete JSON
        System.out.println("\nDelete JSON:");
        System.out.println(conn1.toDeleteJSON());
    }
}