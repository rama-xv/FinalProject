import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

/**
 * BrainstormServerGUI - Server-side GUI that displays the current state of the brainstorming board.
 * This is a read-only view that shows all bubbles and connections as clients create/modify them.
 */
public class BrainstormServerGUI extends Application {

    private Pane canvas;
    private final Map<String, BubbleView> bubbleViews = new HashMap<>();
    private Label statusLabel;
    private Label clientCountLabel;

    private Group centerBubbleGroup;
    private Text centerLabel;
    private final double centerX = 450;
    private final double centerY = 300;
    private String mainIdeaText = "Main Idea";

    private BrainstormServer server;
    private Thread serverThread;

    private static class BubbleView {
        String id;
        double x;
        double y;
        String text;
        Group view;
        Line line;

        BubbleView(String id, double x, double y, String text, Group view, Line line) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.text = text;
            this.view = view;
            this.line = line;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // Create canvas for bubbles
        canvas = new Pane();
        canvas.setPrefSize(900, 600);
        canvas.setStyle("-fx-background-color: #f0f0f0;");

        createCenterBubble();

        // Top bar with status info
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Server Status: Starting...");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

        clientCountLabel = new Label("Connected Clients: 0");
        clientCountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");

        Button clearButton = new Button("Clear All");
        clearButton.setOnAction(e -> clearAllBubbles());

        topBar.getChildren().addAll(statusLabel, clientCountLabel, clearButton);

        // Title
        Label titleLabel = new Label("BrainStorm Server - Live View");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10;");
        titleLabel.setAlignment(Pos.CENTER);

        VBox topSection = new VBox(5);
        topSection.setAlignment(Pos.CENTER);
        topSection.getChildren().addAll(titleLabel, topBar);

        root.setTop(topSection);
        root.setCenter(canvas);

        Scene scene = new Scene(root, 900, 700);
        primaryStage.setTitle("BrainStorm Server");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start the server in a background thread
        startServer();

        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            if (server != null) {
                server.shutdown();
            }
            Platform.exit();
            System.exit(0);
        });
    }

    private void createCenterBubble() {
        double radius = 70;

        Circle circle = new Circle(radius);
        circle.setFill(Color.LIGHTBLUE);
        circle.setStroke(Color.DARKBLUE);
        circle.setStrokeWidth(3);

        centerLabel = new Text(mainIdeaText);
        centerLabel.setStyle("-fx-font-weight: bold;");
        double textWidth = centerLabel.getLayoutBounds().getWidth();
        double textHeight = centerLabel.getLayoutBounds().getHeight();
        centerLabel.setX(-textWidth / 2);
        centerLabel.setY(textHeight / 4);

        centerBubbleGroup = new Group(circle, centerLabel);
        centerBubbleGroup.setLayoutX(centerX);
        centerBubbleGroup.setLayoutY(centerY);

        canvas.getChildren().add(centerBubbleGroup);
    }

    private void startServer() {
        server = new BrainstormServer();
        server.setServerGUI(this);

        serverThread = new Thread(() -> {
            server.start();
        });
        serverThread.setDaemon(true);
        serverThread.start();

        Platform.runLater(() -> {
            statusLabel.setText("Server Status: Running on port 8080");
            statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
        });
    }

    // Called by the server when a bubble is created
    public void onBubbleCreated(Bubble bubble) {
        Platform.runLater(() -> {
            createBubbleOnCanvas(bubble.getId(), bubble.getX(), bubble.getY(), bubble.getText());
        });
    }

    // Called by the server when a bubble is updated
    public void onBubbleUpdated(Bubble bubble) {
        Platform.runLater(() -> {
            updateBubbleOnCanvas(bubble.getId(), bubble.getX(), bubble.getY(), bubble.getText());
        });
    }

    // Called by the server when a bubble is deleted
    public void onBubbleDeleted(String bubbleId) {
        Platform.runLater(() -> {
            removeBubbleFromCanvas(bubbleId);
        });
    }

    // Called by the server when client count changes
    public void onClientCountChanged(int count) {
        Platform.runLater(() -> {
            clientCountLabel.setText("Connected Clients: " + count);
        });
    }

    // Called by the server when main idea is updated
    public void onMainIdeaUpdated(String mainIdea) {
        Platform.runLater(() -> {
            mainIdeaText = mainIdea;
            if (centerLabel != null) {
                centerLabel.setText(mainIdeaText);
                double textWidth = centerLabel.getLayoutBounds().getWidth();
                double textHeight = centerLabel.getLayoutBounds().getHeight();
                centerLabel.setX(-textWidth / 2);
                centerLabel.setY(textHeight / 4);
            }
        });
    }

    // Called by the server when clear all is triggered
    public void onClearAll() {
        Platform.runLater(() -> {
            for (BubbleView view : bubbleViews.values()) {
                canvas.getChildren().remove(view.line);
                canvas.getChildren().remove(view.view);
            }
            bubbleViews.clear();
        });
    }

    private void createBubbleOnCanvas(String id, double x, double y, String text) {
        if (bubbleViews.containsKey(id)) {
            updateBubbleOnCanvas(id, x, y, text);
            return;
        }

        double radius = 60;
        Circle circle = new Circle(radius);
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(2.0);

        Text label = new Text(text);
        double textWidth = label.getLayoutBounds().getWidth();
        double textHeight = label.getLayoutBounds().getHeight();
        label.setX(-textWidth / 2);
        label.setY(textHeight / 4);

        Group bubbleGroup = new Group(circle, label);
        bubbleGroup.setLayoutX(x);
        bubbleGroup.setLayoutY(y);

        // Connection line to center
        Line connectionLine = new Line();
        connectionLine.setStroke(Color.GRAY);
        connectionLine.setStrokeWidth(1.5);
        connectionLine.setStartX(centerX);
        connectionLine.setStartY(centerY);
        connectionLine.setEndX(x);
        connectionLine.setEndY(y);

        canvas.getChildren().add(connectionLine);
        canvas.getChildren().add(bubbleGroup);

        BubbleView view = new BubbleView(id, x, y, text, bubbleGroup, connectionLine);
        bubbleViews.put(id, view);
    }

    private void updateBubbleOnCanvas(String id, double x, double y, String text) {
        BubbleView view = bubbleViews.get(id);
        if (view == null) {
            createBubbleOnCanvas(id, x, y, text);
            return;
        }

        view.x = x;
        view.y = y;
        view.text = text;

        view.view.setLayoutX(x);
        view.view.setLayoutY(y);
        view.line.setEndX(x);
        view.line.setEndY(y);

        // Update text
        for (Node node : view.view.getChildren()) {
            if (node instanceof Text) {
                Text label = (Text) node;
                label.setText(text);
                double textWidth = label.getLayoutBounds().getWidth();
                double textHeight = label.getLayoutBounds().getHeight();
                label.setX(-textWidth / 2);
                label.setY(textHeight / 4);
            }
        }
    }

    private void removeBubbleFromCanvas(String id) {
        BubbleView view = bubbleViews.remove(id);
        if (view != null) {
            canvas.getChildren().remove(view.line);
            canvas.getChildren().remove(view.view);
        }
    }

    private void clearAllBubbles() {
        for (BubbleView view : bubbleViews.values()) {
            canvas.getChildren().remove(view.line);
            canvas.getChildren().remove(view.view);
        }
        bubbleViews.clear();

        // Also clear server state and notify clients
        if (server != null) {
            server.clearAllBubbles();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
