import org.json.JSONObject;

public class Bubble {

    // Identity
    private String id;
    // Position
    private double x;
    private double y;
    // Content
    private String text;
    // Visual properties
    private String color;
    private double radius;
    // Metadata
    private String createdBy;
    private long timestamp;

    // CONSTRUCTORS

    /**
     * Full constructor with all properties
     */
    public Bubble(String id, double x, double y, String text, String color, String createdBy) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.text = text;
        this.color = color;
        this.createdBy = createdBy;
        this.radius = 50.0;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Simple constructor with defaults for color and creator
     */
    public Bubble(String id, double x, double y, String text) {
        this(id, x, y, text, "#FFFFFF", "unknown");
    }

    /**
     * Constructor from JSON object (for deserializing from server)
     */
    public Bubble(JSONObject json) {
        this.id = json.getString("id");
        this.x = json.getDouble("x");
        this.y = json.getDouble("y");
        this.text = json.getString("text");
        this.color = json.optString("color", "#FFFFFF");
        this.createdBy = json.optString("createdBy", "unknown");
        this.radius = json.optDouble("radius", 50.0);
        this.timestamp = json.optLong("timestamp", System.currentTimeMillis());
    }

    // GETTERS
    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getText() {
        return text;
    }

    public String getColor() {
        return color;
    }

    public double getRadius() {
        return radius;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getTimestamp() {
        return timestamp;
    }


    // ============================================
    // SETTERS
    // ============================================

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    /**
     * Set position in one call (useful for dragging)
     */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // JSON SERIALIZATION
    /**
     * Convert bubble to JSON string for sending over network
     * Creates a complete JSON object with all properties
     */
    public String toJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "bubble_create");
        json.put("id", id);
        json.put("x", x);
        json.put("y", y);
        json.put("text", text);
        json.put("color", color);
        json.put("radius", radius);
        json.put("createdBy", createdBy);
        json.put("timestamp", timestamp);

        return json.toString();
    }

    /**
     * Create a JSON object for position update only
     * More efficient than sending all data when only position changes
     */
    public String toPositionUpdateJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "bubble_update");
        json.put("id", id);
        json.put("x", x);
        json.put("y", y);

        return json.toString();
    }

    /**
     * Create a JSON object for text update only
     */
    public String toTextUpdateJSON() {
        JSONObject json = new JSONObject();
        json.put("type", "bubble_update");
        json.put("id", id);
        json.put("text", text);

        return json.toString();
    }

    // UTILITY METHODS
    /**
     * Check if a point (x, y) is inside this bubble
     * Useful for detecting clicks on the bubble
     */
    public boolean contains(double pointX, double pointY) {
        double distance = Math.sqrt(
                Math.pow(pointX - this.x, 2) +
                        Math.pow(pointY - this.y, 2)
        );
        return distance <= radius;
    }

    /**
     * Calculate distance from this bubble's center to another point
     */
    public double distanceTo(double pointX, double pointY) {
        return Math.sqrt(
                Math.pow(pointX - this.x, 2) +
                        Math.pow(pointY - this.y, 2)
        );
    }

    /**
     * Calculate distance from this bubble to another bubble
     */
    public double distanceTo(Bubble other) {
        return distanceTo(other.x, other.y);
    }

    /**
     * Check if this bubble overlaps with another bubble
     * Useful for preventing bubbles from being placed too close
     */
    public boolean overlaps(Bubble other) {
        double distance = distanceTo(other);
        return distance < (this.radius + other.radius);
    }

    /**
     * Get the bounds of this bubble (for collision detection)
     * Returns: [minX, minY, maxX, maxY]
     */
    public double[] getBounds() {
        return new double[] {
                x - radius,  // minX
                y - radius,  // minY
                x + radius,  // maxX
                y + radius   // maxY
        };
    }

    /**
     * Create a copy of this bubble
     */
    public Bubble copy() {
        return new Bubble(id, x, y, text, color, createdBy);
    }

    // OBJECT OVERRIDES

    @Override
    public String toString() {
        return String.format("Bubble[id=%s, pos=(%.1f,%.1f), text='%s', color=%s]",
                id, x, y, text, color);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Bubble other = (Bubble) obj;
        return id.equals(other.id);  // Bubbles are equal if they have same ID
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // TESTING

    /**
     * Test the Bubble class independently
     */
    public static void main(String[] args) {
        System.out.println("=== Bubble Class Test ===\n");

        // Test 1: Create bubble
        Bubble bubble1 = new Bubble("b1", 100, 200, "First Idea");
        System.out.println("Created: " + bubble1);

        // Test 2: Convert to JSON
        System.out.println("\nJSON format:");
        System.out.println(bubble1.toJSON());

        // Test 3: Update position
        bubble1.setPosition(150, 250);
        System.out.println("\nAfter moving:");
        System.out.println(bubble1.toPositionUpdateJSON());

        // Test 4: Check if point is inside bubble
        System.out.println("\nContains (150, 250)? " + bubble1.contains(150, 250));
        System.out.println("Contains (300, 300)? " + bubble1.contains(300, 300));

        // Test 5: Check overlap with another bubble
        Bubble bubble2 = new Bubble("b2", 180, 280, "Second Idea");
        System.out.println("\nBubble 1 overlaps Bubble 2? " + bubble1.overlaps(bubble2));
        System.out.println("Distance between bubbles: " + bubble1.distanceTo(bubble2));

        // Test 6: Test bounds
        double[] bounds = bubble1.getBounds();
        System.out.println("\nBubble 1 bounds: [" + bounds[0] + ", " + bounds[1] +
                ", " + bounds[2] + ", " + bounds[3] + "]");

        // Test 7: Test equality
        Bubble bubble1Copy = bubble1.copy();
        System.out.println("\nBubble 1 equals its copy? " + bubble1.equals(bubble1Copy));
        System.out.println("Bubble 1 equals Bubble 2? " + bubble1.equals(bubble2));

        // Test 8: Create from JSON
        System.out.println("\n=== Creating from JSON ===");
        JSONObject json = new JSONObject();
        json.put("id", "b3");
        json.put("x", 300);
        json.put("y", 400);
        json.put("text", "From JSON");
        json.put("color", "#FF5733");

        Bubble bubble3 = new Bubble(json);
        System.out.println("Created from JSON: " + bubble3);
    }
}
