import org.json.*;

public class Connection {
    private String id;
    private String from;
    private String to;

    public Connection(String id, String from, String to) {
        this.id = id;
        this.from = from;
        this.to = to;
    }

    public String getId() {
        return id;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("from", from);
        json.put("to", to);
        return json;
    }

    @Override
    public String toString() {
        return "Connection{id='" + id + "', from='" + from + "', to='" + to + "'}";
    }
}