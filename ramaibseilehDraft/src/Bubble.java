import org.json.*;

public class Bubble {
    private String id;
    private String text;
    private double x;
    private double y;

    public Bubble(String id, String text, double x, double y) {
        this.id = id;
        this.text = text;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("text", text);
        json.put("x", x);
        json.put("y", y);
        return json;
    }

    @Override
    public String toString() {
        return "Bubble{id='" + id + "', text='" + text + "', x=" + x + ", y=" + y + "}";
    }
}