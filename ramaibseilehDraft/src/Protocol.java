import org.json.*;
public class Protocol {
    public static final String WELCOME = "WELCOME";
    public static final String FULL_STATE = "FULL_STATE";
    public static final String ADD_BUBBLE = "ADD_BUBBLE";
    public static final String UPDATE_BUBBLE = "UPDATE_BUBBLE";
    public static final String DELETE_BUBBLE = "DELETE_BUBBLE";
    public static final String ADD_CONNECTION = "ADD_CONNECTION";
    public static final String DELETE_CONNECTION = "DELETE_CONNECTION";
    public static final String REQUEST_STATE = "REQUEST_STATE";

    /**
     * Creates a welcome message with client ID
     * Format: {"type":"WELCOME","data":{"clientId":"CLIENT_123"}}
     */
    public static String createWelcomeMessage(String clientId) {
        JSONObject json = new JSONObject();
        json.put("type", WELCOME);

        JSONObject data = new JSONObject();
        data.put("clientId", clientId);
        json.put("data", data);

        return json.toString();
    }

    /**
     * Creates a full state message with all bubbles and connections
     * Format: {"type":"FULL_STATE","data":{"bubbles":[...],"connections":[...]}}
     */
    public static String createFullStateMessage(CanvasState state) {
        JSONObject json = new JSONObject();
        json.put("type", FULL_STATE);
        json.put("data", state.toJSON());
        return json.toString();
    }

    /**
     * Creates an add bubble message
     * Format: {"type":"ADD_BUBBLE","data":{"id":"B1","text":"Idea","x":100,"y":200}}
     */
    public static String createAddBubbleMessage(String id, String text, double x, double y) {
        JSONObject json = new JSONObject();
        json.put("type", ADD_BUBBLE);

        JSONObject data = new JSONObject();
        data.put("id", id);
        data.put("text", text);
        data.put("x", x);
        data.put("y", y);
        json.put("data", data);

        return json.toString();
    }

    /**
     * Creates an update bubble message
     * Format: {"type":"UPDATE_BUBBLE","data":{"id":"B1","text":"New Text","x":150,"y":250}}
     */
    public static String createUpdateBubbleMessage(String id, String text, double x, double y) {
        JSONObject json = new JSONObject();
        json.put("type", UPDATE_BUBBLE);

        JSONObject data = new JSONObject();
        data.put("id", id);
        if (text != null) data.put("text", text);
        data.put("x", x);
        data.put("y", y);
        json.put("data", data);

        return json.toString();
    }

    /**
     * Creates a delete bubble message
     * Format: {"type":"DELETE_BUBBLE","data":{"id":"B1"}}
     */
    public static String createDeleteBubbleMessage(String id) {
        JSONObject json = new JSONObject();
        json.put("type", DELETE_BUBBLE);

        JSONObject data = new JSONObject();
        data.put("id", id);
        json.put("data", data);

        return json.toString();
    }

    /**
     * Creates an add connection message
     * Format: {"type":"ADD_CONNECTION","data":{"id":"C1","from":"B1","to":"B2"}}
     */
    public static String createAddConnectionMessage(String id, String from, String to) {
        JSONObject json = new JSONObject();
        json.put("type", ADD_CONNECTION);

        JSONObject data = new JSONObject();
        data.put("id", id);
        data.put("from", from);
        data.put("to", to);
        json.put("data", data);

        return json.toString();
    }

    /**
     * Creates a delete connection message
     * Format: {"type":"DELETE_CONNECTION","data":{"id":"C1"}}
     */
    public static String createDeleteConnectionMessage(String id) {
        JSONObject json = new JSONObject();
        json.put("type", DELETE_CONNECTION);

        JSONObject data = new JSONObject();
        data.put("id", id);
        json.put("data", data);

        return json.toString();
    }

    /**
     * Creates a request state message
     * Format: {"type":"REQUEST_STATE","data":{}}
     */
    public static String createRequestStateMessage() {
        JSONObject json = new JSONObject();
        json.put("type", REQUEST_STATE);
        json.put("data", new JSONObject());
        return json.toString();
    }

    /**
     * Parse message type from JSON string
     */
    public static String getMessageType(String jsonMessage) {
        try {
            JSONObject json = new JSONObject(jsonMessage);
            return json.getString("type");
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Parse data object from JSON string
     */
    public static JSONObject getMessageData(String jsonMessage) {
        try {
            JSONObject json = new JSONObject(jsonMessage);
            return json.getJSONObject("data");
        } catch (JSONException e) {
            return null;
        }
    }

}
