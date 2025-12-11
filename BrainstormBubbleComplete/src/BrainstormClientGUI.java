import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// Bubble brainstorm system with main idea bubble and multiple sub-bubbles

public class BrainstormClientGUI extends Application {

    private Pane canvas; // space for our bubble diagram
    private int nextBubbleIdForLayout = 1;
    private final Map<String, BubbleData> bubbleMap = new HashMap<>();


    private Group centerBubbleGroup;
    private Text centerLabel;
    private final double centerX = 450; // assumed center coordinates
    private final double centerY = 300;


    private String mainIdeaText = "Main Idea"; // default main bubble text
    private NetworkClient networkClient;
    private MessageHandler messageHandler;

    private static class BubbleData { // holds individual data for each bubble/idea
        String id;
        double x;
        double y;
        String text;
        Group view;
        Line line;

        BubbleData(String id, double x, double y, String text, Group view, Line line) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.text = text;
            this.view = view;
            this.line = line;
        }
    }

     // Creating Canvas size
    @Override
    public void start(Stage primaryStage) { // building UI
        BorderPane root = new BorderPane();

        canvas = new Pane();
        canvas.setPrefSize(900, 600); // assumed size

        createCenterBubble(); // builds main bubble

        HBox topBar = new HBox(10); // bar for buttons
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button connectButton   = new Button("Connect");
        Button mainIdeaButton  = new Button("Set Main Idea");
        Button addBubbleButton = new Button("Add Bubble");
        Button clearButton     = new Button("Clear");

        topBar.getChildren().addAll(connectButton, mainIdeaButton, addBubbleButton, clearButton);

        root.setTop(topBar);
        root.setCenter(canvas);

        // Clear Button
        clearButton.setOnAction(event -> {
            // Removes and resets canvas to default form
            canvas.getChildren().clear();
            bubbleMap.clear();
            nextBubbleIdForLayout = 1;

            createCenterBubble();

            // Send clear command to server
            if (networkClient != null && networkClient.isConnected() && messageHandler != null) {
                messageHandler.clearAll();
            }
        });

        // main idea/bubble
        mainIdeaButton.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog(mainIdeaText);
            dialog.setTitle("Set Main Idea");
            dialog.setHeaderText("Enter the main idea for your brainstorm");
            dialog.setContentText("Main idea:");

            Optional<String> result = dialog.showAndWait(); //edit text if user input given
            result.ifPresent(text -> {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    mainIdeaText = trimmed;

                    if (centerLabel != null) {
                        centerLabel.setText(mainIdeaText);

                        // Adjusting text label to fit in assumed center
                        double textWidth = centerLabel.getLayoutBounds().getWidth();
                        double textHeight = centerLabel.getLayoutBounds().getHeight();
                        centerLabel.setX(-textWidth / 2);
                        centerLabel.setY(textHeight / 4);
                    }

                    // Send main idea update to server
                    if (networkClient != null && networkClient.isConnected() && messageHandler != null) {
                        messageHandler.updateMainIdea(trimmed);
                    }
                }
            });
        });

        //Add Bubble button
        addBubbleButton.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Idea Bubble");
            dialog.setHeaderText("Create a new idea bubble");
            dialog.setContentText("Enter your idea text:");
            // user option to add sub-idea text (idea bubbles)

            Optional<String> result = dialog.showAndWait();

            result.ifPresent(text -> {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {

                    // Positioning new bubbles based on current bubble count
                    int currentBubbleCount = bubbleMap.size();
                    double radius = 200; // distance from center
                    int slots = 12;       // maximum sub-ideas around main idea

                    double angleDegrees = currentBubbleCount * (360.0 / slots);
                    double angleRadians = Math.toRadians(angleDegrees);

                    double x = centerX + radius * Math.cos(angleRadians);
                    double y = centerY + radius * Math.sin(angleRadians);

                    // When connected to a server, MessageHandler works on creating bubble, if not then locally stored and produced.
                    if (networkClient != null && networkClient.isConnected() && messageHandler != null) {
                        // Sending JSON to server
                        messageHandler.createBubble(x, y, trimmed);
                    } else {

                        // When no network involved
                        String localId = "local_" + System.currentTimeMillis();
                        createIdeaBubbleOnCanvas(localId, x, y, trimmed);
                    }
                }
            });
        });

        // Connect button
        connectButton.setOnAction(event -> {

            if (networkClient != null && networkClient.isConnected()) {
                System.out.println("You are already connected to server.");
                return;
            }

           // Message handler work, connection attempts
            messageHandler = new MessageHandler(this);
            networkClient = new NetworkClient("localhost", 8080, messageHandler);
            messageHandler.setNetworkClient(networkClient);

            boolean ok = networkClient.connect();
            if (ok) {
                System.out.println("Connected to server");


            } else {
                System.out.println(" Has failed to connect to the server.");
            }
        });

        // Completing set up
        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("BrainStorm Bubble - Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Main bubble set up design
    private void createCenterBubble() {
        double radius = 70; //  size

        Circle circle = new Circle(radius);
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(2.5);

        // label
        centerLabel = new Text(mainIdeaText);
        double textWidth = centerLabel.getLayoutBounds().getWidth();
        double textHeight = centerLabel.getLayoutBounds().getHeight();
        centerLabel.setX(-textWidth / 2);
        centerLabel.setY(textHeight / 4);

        centerBubbleGroup = new Group(circle, centerLabel);

        // Ideal center
        centerBubbleGroup.setLayoutX(centerX);
        centerBubbleGroup.setLayoutY(centerY);

        canvas.getChildren().add(centerBubbleGroup);
    }

    // sub bubbles design

    private void createIdeaBubbleOnCanvas(String id, double x, double y, String text) {
        double radius = 60; // size
        Circle circle = new Circle(radius);
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(2.0);

        // enter text
        Text label = new Text(text);
        double textWidth = label.getLayoutBounds().getWidth();
        double textHeight = label.getLayoutBounds().getHeight();
        label.setX(-textWidth / 2);
        label.setY(textHeight / 4);

        Group bubbleGroup = new Group(circle, label);
        bubbleGroup.setLayoutX(x);
        bubbleGroup.setLayoutY(y);

        // String ID
        bubbleGroup.setUserData(id);

        // Connecting line
        Line connectionLine = new Line();
        connectionLine.setStroke(Color.BLACK);
        connectionLine.setStrokeWidth(1.5);
        connectionLine.setStartX(centerX);
        connectionLine.setStartY(centerY);
        connectionLine.setEndX(x);
        connectionLine.setEndY(y);


        canvas.getChildren().add(connectionLine);
        makeIdeaBubbleDraggable(bubbleGroup, connectionLine);
        canvas.getChildren().add(bubbleGroup);


        BubbleData data = new BubbleData(id, x, y, text, bubbleGroup, connectionLine);
        bubbleMap.put(id, data);
    }


    private void makeIdeaBubbleDraggable(Group bubbleGroup, Line line) {
        final Point2D[] dragOffset = new Point2D[1];

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

                Object userData = bubbleGroup.getUserData(); // searching user data
                if (userData instanceof String) {
                    String id = (String) userData;
                    BubbleData data = bubbleMap.get(id);
                    if (data != null) {
                        data.x = newX;
                        data.y = newY;


                        if (networkClient != null && networkClient.isConnected() && messageHandler != null) {
                            messageHandler.updateBubble(id, newX, newY, null); // means that users must update
                        }
                    }
                }
            }
        });
    }


    public void onNetworkBubbleCreated(Bubble bubble) { // retrieving ID
        createIdeaBubbleOnCanvas(
                bubble.getId(),
                bubble.getX(),
                bubble.getY(),
                bubble.getText()
        );
    }


    public void onNetworkBubbleUpdated(Bubble bubble) {
        String id = bubble.getId();
        BubbleData data = bubbleMap.get(id);

        if (data == null) {
            createIdeaBubbleOnCanvas(id, bubble.getX(), bubble.getY(), bubble.getText());
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

        // Updating text on bubble
        for (Node node : group.getChildren()) {
            if (node instanceof Text) {
                Text label = (Text) node;
                label.setText(data.text);
                double textWidth = label.getLayoutBounds().getWidth();
                double textHeight = label.getLayoutBounds().getHeight();
                label.setX(-textWidth / 2);
                label.setY(textHeight / 4);
            }
        }
    }


    public void onNetworkBubbleDeleted(String id) {
        BubbleData data = bubbleMap.remove(id);
        if (data == null) {
            return;
        }

        canvas.getChildren().remove(data.line);
        canvas.getChildren().remove(data.view);
    }


    public void onNetworkResetAllBubbles() {
        canvas.getChildren().clear();
        bubbleMap.clear();
        nextBubbleIdForLayout = 1;
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

