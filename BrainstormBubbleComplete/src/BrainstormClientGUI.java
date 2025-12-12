import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * BrainStorm Bubble - Enhanced GUI with Colors
 */
public class BrainstormClientGUI extends Application {

    private Pane canvas;
    private final Map<String, BubbleData> bubbleMap = new HashMap<>();

    private Group centerBubbleGroup;
    private Text centerLabel;
    private final double centerX = 450;
    private final double centerY = 300;

    private String mainIdeaText = "Main Idea";
    private NetworkClient networkClient;
    private MessageHandler messageHandler;

    private Label statusLabel;

    // Color palette for bubbles
    private final Color[] BUBBLE_COLORS = {
            Color.web("#FF6B6B"), // Coral Red
            Color.web("#4ECDC4"), // Turquoise
            Color.web("#45B7D1"), // Sky Blue
            Color.web("#FFA07A"), // Light Salmon
            Color.web("#98D8C8"), // Mint
            Color.web("#F7DC6F"), // Yellow
            Color.web("#BB8FCE"), // Lavender
            Color.web("#85C1E2")  // Baby Blue
    };
    private int colorIndex = 0;

    private static class BubbleData {
        String id;
        double x;
        double y;
        String text;
        Color color;
        Group view;
        Line line;

        BubbleData(String id, double x, double y, String text, Color color, Group view, Line line) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.view = view;
            this.line = line;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f7fa;");

        // Canvas
        canvas = new Pane();
        canvas.setPrefSize(900, 600);
        canvas.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 10;");

        // Add some padding around canvas
        StackPane canvasWrapper = new StackPane(canvas);
        canvasWrapper.setPadding(new Insets(20));

        createCenterBubble();

        // Top toolbar with better styling
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #667eea; -fx-background-radius: 0 0 10 10;");

        Button connectButton = createStyledButton("Connect", "#4CAF50");
        Button mainIdeaButton = createStyledButton("Main Idea", "#2196F3");
        Button addBubbleButton = createStyledButton("Add Bubble", "#FF9800");
        Button clearButton = createStyledButton("Clear", "#F44336");

        topBar.getChildren().addAll(connectButton, mainIdeaButton, addBubbleButton, clearButton);

        // Status bar
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #d0d0d0; -fx-border-width: 1 0 0 0;");

        Circle statusCircle = new Circle(6);
        statusCircle.setFill(Color.RED);

        statusLabel = new Label("Disconnected");
        statusLabel.setFont(Font.font("Arial", 12));

        statusBar.getChildren().addAll(statusCircle, statusLabel);

        root.setTop(topBar);
        root.setCenter(canvasWrapper);
        root.setBottom(statusBar);

        // Button Actions
        clearButton.setOnAction(event -> {
            canvas.getChildren().clear();
            bubbleMap.clear();
            colorIndex = 0;
            createCenterBubble();

            if (networkClient != null && networkClient.isConnected() && messageHandler != null) {
                messageHandler.clearAll();
            }
        });

        mainIdeaButton.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog(mainIdeaText);
            dialog.setTitle("Set Main Idea");
            dialog.setHeaderText("Enter the main idea for your brainstorm");
            dialog.setContentText("Main idea:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(text -> {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    mainIdeaText = trimmed;

                    if (centerLabel != null) {
                        centerLabel.setText(mainIdeaText);
                        double textWidth = centerLabel.getLayoutBounds().getWidth();
                        double textHeight = centerLabel.getLayoutBounds().getHeight();
                        centerLabel.setX(-textWidth / 2);
                        centerLabel.setY(textHeight / 4);
                    }

                    if (networkClient != null && networkClient.isConnected() && messageHandler != null) {
                        messageHandler.updateMainIdea(trimmed);
                    }
                }
            });
        });

        addBubbleButton.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Idea Bubble");
            dialog.setHeaderText("Create a new idea bubble");
            dialog.setContentText("Enter your idea:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(text -> {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    int currentBubbleCount = bubbleMap.size();
                    double radius = 200;
                    int slots = 12;

                    double angleDegrees = currentBubbleCount * (360.0 / slots);
                    double angleRadians = Math.toRadians(angleDegrees);

                    double x = centerX + radius * Math.cos(angleRadians);
                    double y = centerY + radius * Math.sin(angleRadians);

                    if (networkClient != null && networkClient.isConnected() && messageHandler != null) {
                        messageHandler.createBubble(x, y, trimmed);
                    } else {
                        String localId = "local_" + System.currentTimeMillis();
                        Color bubbleColor = BUBBLE_COLORS[colorIndex % BUBBLE_COLORS.length];
                        colorIndex++;
                        createIdeaBubbleOnCanvas(localId, x, y, trimmed, bubbleColor);
                    }
                }
            });
        });

        connectButton.setOnAction(event -> {
            if (networkClient != null && networkClient.isConnected()) {
                System.out.println("Already connected.");
                return;
            }

            messageHandler = new MessageHandler(this);
            networkClient = new NetworkClient("localhost", 8080, messageHandler);
            messageHandler.setNetworkClient(networkClient);

            boolean ok = networkClient.connect();
            if (ok) {
                System.out.println("Connected to server");
                statusCircle.setFill(Color.LIMEGREEN);
                statusLabel.setText("Connected");
            } else {
                System.out.println("Failed to connect");
                statusCircle.setFill(Color.RED);
                statusLabel.setText("Connection Failed");
            }
        });

        Scene scene = new Scene(root, 900, 700);
        primaryStage.setTitle("BrainStorm Bubble - Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Create styled buttons
    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 13px; " +
                        "-fx-padding: 8 20; " +
                        "-fx-background-radius: 5; " +
                        "-fx-cursor: hand;"
        );

        // Hover effect
        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + "-fx-opacity: 0.85;");
        });
        button.setOnMouseExited(e -> {
            button.setStyle(button.getStyle().replace("-fx-opacity: 0.85;", ""));
        });

        return button;
    }

    // Create center bubble with nice styling
    private void createCenterBubble() {
        double radius = 70;

        Circle circle = new Circle(radius);
        circle.setFill(Color.web("#667eea")); // Purple gradient color
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(3);

        // Add shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(10);
        circle.setEffect(shadow);

        centerLabel = new Text(mainIdeaText);
        centerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        centerLabel.setFill(Color.WHITE);

        double textWidth = centerLabel.getLayoutBounds().getWidth();
        double textHeight = centerLabel.getLayoutBounds().getHeight();
        centerLabel.setX(-textWidth / 2);
        centerLabel.setY(textHeight / 4);

        centerBubbleGroup = new Group(circle, centerLabel);
        centerBubbleGroup.setLayoutX(centerX);
        centerBubbleGroup.setLayoutY(centerY);

        canvas.getChildren().add(centerBubbleGroup);
    }

    // Create idea bubble with colors
    private void createIdeaBubbleOnCanvas(String id, double x, double y, String text, Color color) {
        double radius = 60;

        Circle circle = new Circle(radius);
        circle.setFill(color);
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2.5);

        // Add shadow
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        shadow.setRadius(8);
        circle.setEffect(shadow);

        // Truncate text if too long
        String displayText = text.length() > 15 ? text.substring(0, 12) + "..." : text;

        Text label = new Text(displayText);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        label.setFill(Color.WHITE);

        double textWidth = label.getLayoutBounds().getWidth();
        double textHeight = label.getLayoutBounds().getHeight();
        label.setX(-textWidth / 2);
        label.setY(textHeight / 4);

        Group bubbleGroup = new Group(circle, label);
        bubbleGroup.setLayoutX(x);
        bubbleGroup.setLayoutY(y);
        bubbleGroup.setUserData(id);

        // Connection line with same color
        Line connectionLine = new Line();
        connectionLine.setStroke(color);
        connectionLine.setStrokeWidth(2);
        connectionLine.setStartX(centerX);
        connectionLine.setStartY(centerY);
        connectionLine.setEndX(x);
        connectionLine.setEndY(y);
        connectionLine.setOpacity(0.7);

        canvas.getChildren().add(connectionLine);
        makeIdeaBubbleDraggable(bubbleGroup, connectionLine);
        canvas.getChildren().add(bubbleGroup);

        BubbleData data = new BubbleData(id, x, y, text, color, bubbleGroup, connectionLine);
        bubbleMap.put(id, data);

        // Simple entrance animation
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), bubbleGroup);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(1);
        scale.setToY(1);
        scale.play();
    }

    private void makeIdeaBubbleDraggable(Group bubbleGroup, Line line) {
        final Point2D[] dragOffset = new Point2D[1];

        // Hover effect
        bubbleGroup.setOnMouseEntered(e -> {
            bubbleGroup.setScaleX(1.1);
            bubbleGroup.setScaleY(1.1);
            bubbleGroup.setCursor(javafx.scene.Cursor.HAND);
        });

        bubbleGroup.setOnMouseExited(e -> {
            bubbleGroup.setScaleX(1.0);
            bubbleGroup.setScaleY(1.0);
            bubbleGroup.setCursor(javafx.scene.Cursor.DEFAULT);
        });

        bubbleGroup.setOnMousePressed(event -> {
            event.consume();
            dragOffset[0] = new Point2D(
                    event.getSceneX() - bubbleGroup.getLayoutX(),
                    event.getSceneY() - bubbleGroup.getLayoutY()
            );
        });

        bubbleGroup.setOnMouseDragged(event -> {
            event.consume();

            if (dragOffset[0] != null) {
                double newX = event.getSceneX() - dragOffset[0].getX();
                double newY = event.getSceneY() - dragOffset[0].getY();

                bubbleGroup.setLayoutX(newX);
                bubbleGroup.setLayoutY(newY);

                line.setEndX(newX);
                line.setEndY(newY);

                Object userData = bubbleGroup.getUserData();
                if (userData instanceof String) {
                    String id = (String) userData;
                    BubbleData data = bubbleMap.get(id);
                    if (data != null) {
                        data.x = newX;
                        data.y = newY;

                        if (networkClient != null && networkClient.isConnected() && messageHandler != null) {
                            messageHandler.updateBubble(id, newX, newY, null);
                        }
                    }
                }
            }
        });
    }

    // Network callbacks
    public void onNetworkBubbleCreated(Bubble bubble) {
        Color bubbleColor = BUBBLE_COLORS[colorIndex % BUBBLE_COLORS.length];
        colorIndex++;

        createIdeaBubbleOnCanvas(
                bubble.getId(),
                bubble.getX(),
                bubble.getY(),
                bubble.getText(),
                bubbleColor
        );
    }

    public void onNetworkBubbleUpdated(Bubble bubble) {
        String id = bubble.getId();
        BubbleData data = bubbleMap.get(id);

        if (data == null) {
            onNetworkBubbleCreated(bubble);
            return;
        }

        data.x = bubble.getX();
        data.y = bubble.getY();
        data.text = bubble.getText();

        Group group = data.view;
        group.setLayoutX(data.x);
        group.setLayoutY(data.y);
        data.line.setEndX(data.x);
        data.line.setEndY(data.y);

        for (Node node : group.getChildren()) {
            if (node instanceof Text) {
                Text label = (Text) node;
                String displayText = data.text.length() > 15 ?
                        data.text.substring(0, 12) + "..." :
                        data.text;
                label.setText(displayText);
                double textWidth = label.getLayoutBounds().getWidth();
                double textHeight = label.getLayoutBounds().getHeight();
                label.setX(-textWidth / 2);
                label.setY(textHeight / 4);
            }
        }
    }

    public void onNetworkBubbleDeleted(String id) {
        BubbleData data = bubbleMap.remove(id);
        if (data == null) return;

        canvas.getChildren().remove(data.line);
        canvas.getChildren().remove(data.view);
    }

    public void onNetworkResetAllBubbles() {
        canvas.getChildren().clear();
        bubbleMap.clear();
        colorIndex = 0;
        createCenterBubble();
    }

    public void onMainIdeaUpdated(String newMainIdea) {
        mainIdeaText = newMainIdea;
        if (centerLabel != null) {
            centerLabel.setText(mainIdeaText);
            double textWidth = centerLabel.getLayoutBounds().getWidth();
            double textHeight = centerLabel.getLayoutBounds().getHeight();
            centerLabel.setX(-textWidth / 2);
            centerLabel.setY(textHeight / 4);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}