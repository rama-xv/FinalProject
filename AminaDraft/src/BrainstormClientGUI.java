import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Group;
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

// BrainstormClientGUI
// Current status:
// bubble graph complete
//buttons complete

public class BrainstormClientGUI extends Application {

    // Plain canvas where we draw bubbles and lines
    private Pane canvas;

    // Counter to give each idea bubble a unique ID (local for now)
    private int nextBubbleId = 1;

    // Bubbles tracked by ID
    private final Map<Integer, BubbleData> bubbleMap = new HashMap<>();

    // Center bubble visual nodes + text
    private Group centerBubbleGroup;
    private Text centerLabel;

    // Center position
    private double centerX = 450;
    private double centerY = 300;

    // The main idea text default
    private String mainIdeaText = "Main Idea";

    // Single idea bubble
    private static class BubbleData {
        int id;
        double x;
        double y;
        String text;
        Group view;  // node for the bubble itself
        Line line;   // connection line to the center bubble

        BubbleData(int id, double x, double y, String text, Group view, Line line) {
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
        // Main layout containers

        //   top - buttons
        //   center - bubbles in page
        BorderPane root = new BorderPane();

        // Creating canvas space
        canvas = new Pane();
        canvas.setPrefSize(900, 600);

        // Initial center bubble
        createCenterBubble();

        // Building buttons

        HBox topBar = new HBox(10);         // 10px spacing between buttons
        topBar.setPadding(new Insets(10));  // padding around the bar
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button connectButton     = new Button("Connect");      // for networking later
        Button mainIdeaButton    = new Button("Set Main Idea");// change center bubble text
        Button addBubbleButton   = new Button("Add Bubble");   // create idea bubble
        Button clearButton       = new Button("Clear");        // remove all idea bubbles

        topBar.getChildren().addAll(connectButton, mainIdeaButton, addBubbleButton, clearButton);

        root.setTop(topBar);
        root.setCenter(canvas);

        // When using clear button

        clearButton.setOnAction(event -> {
            // Remove everything, bubbles and lines, from canvas
            canvas.getChildren().clear();

            // Clear simple model so GUI and model stay in sync
            bubbleMap.clear();
            nextBubbleId = 1; // reseting IDs for idea bubbles

            // Recreate the center bubble using the current mainIdeaText
            createCenterBubble();
        });

        // When pressing "Create main" button

        mainIdeaButton.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog(mainIdeaText);
            dialog.setTitle("Set Main Idea");
            dialog.setHeaderText("Enter the main idea for your brainstorm");
            dialog.setContentText("Main idea:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(text -> {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    // Updating main idea text
                    mainIdeaText = trimmed;

                    // Updating the label inside the main bubble visually
                    if (centerLabel != null) {
                        centerLabel.setText(mainIdeaText);

                        // Re-center the label text based on new width/height
                        double textWidth = centerLabel.getLayoutBounds().getWidth();
                        double textHeight = centerLabel.getLayoutBounds().getHeight();
                        centerLabel.setX(-textWidth / 2);
                        centerLabel.setY(textHeight / 4);
                    }


                }
            });
        });

        // When pressing "add bubble"

        addBubbleButton.setOnAction(event -> {
            // Ask the user for idea text
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Idea Bubble");
            dialog.setHeaderText("Create a new idea bubble");
            dialog.setContentText("Enter bubble text:");

            Optional<String> result = dialog.showAndWait();

            result.ifPresent(text -> {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {

                    int bubbleId = nextBubbleId++;

                    // Placing  around the center bubble

                    // Radius of the idea bubble circle
                    double radius = 200;

                    // How many ideas per main bubble (ADD OPTION TO CHOOSE NUMBER)
                    int slots = 8;

                    // Choosinf an angle based on which bubble number this is.
                    double angleDegrees = (bubbleId - 1) * (360.0 / slots);
                    double angleRadians = Math.toRadians(angleDegrees);

                    // Position on the circle, relative to center
                    double x = centerX + radius * Math.cos(angleRadians);
                    double y = centerY + radius * Math.sin(angleRadians);

                    createIdeaBubbleOnCanvas(bubbleId, x, y, trimmed);
                }
            });
        });

        // Connect button

        connectButton.setOnAction(event -> {
            System.out.println("Connect button clicked (networking not implemented yet).");
        });

        // Scene set up

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("BrainStorm Bubble - Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // When pressing Center bubble button

    // not draggable
    private void createCenterBubble() {
        double radius = 70; // center bubble size

        Circle circle = new Circle(radius);
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(2.5);

        // Create the label for the center bubble and store
        centerLabel = new Text(mainIdeaText);
        double textWidth = centerLabel.getLayoutBounds().getWidth();
        double textHeight = centerLabel.getLayoutBounds().getHeight();
        centerLabel.setX(-textWidth / 2);
        centerLabel.setY(textHeight / 4);

        centerBubbleGroup = new Group(circle, centerLabel);

        // Place center bubble at the center logical (centerX, centerY)
        centerBubbleGroup.setLayoutX(centerX);
        centerBubbleGroup.setLayoutY(centerY);

        canvas.getChildren().add(centerBubbleGroup);
    }

    // Idea bubble
    private void createIdeaBubbleOnCanvas(int id, double x, double y, String text) {
        double radius = 60; // idea bubble size

        // Circle for the bubble
        Circle circle = new Circle(radius);
        // WHITE fill and BLACK outline
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(2.0);

        // Text inside the bubble
        Text label = new Text(text);

        // Centering text in the middle of the circle estimate
        double textWidth = label.getLayoutBounds().getWidth();
        double textHeight = label.getLayoutBounds().getHeight();
        label.setX(-textWidth / 2);
        label.setY(textHeight / 4);

        // Group the circle and text together so they move as a unit
        Group bubbleGroup = new Group(circle, label);

        // Position the group so that its CENTER is at (x, y)
        bubbleGroup.setLayoutX(x);
        bubbleGroup.setLayoutY(y);

        // Store the bubble's ID on the view node so we can look up its model later
        bubbleGroup.setUserData(id);

        // Connection line from idea to main bubble

        Line connectionLine = new Line();
        connectionLine.setStroke(Color.BLACK);
        connectionLine.setStrokeWidth(1.5);

        // Line starts at the logical center (centerX, centerY)
        connectionLine.setStartX(centerX);
        connectionLine.setStartY(centerY);

        // Line ends at the bubble's position
        connectionLine.setEndX(x);
        connectionLine.setEndY(y);

        // Add line first so bubble draws on top of it
        canvas.getChildren().add(connectionLine);

        // Make the bubble draggable
        makeIdeaBubbleDraggable(bubbleGroup, connectionLine);

        // Add the bubble to the canvas (on top of the line)
        canvas.getChildren().add(bubbleGroup);

        //  GUI to Model
        BubbleData data = new BubbleData(id, x, y, text, bubbleGroup, connectionLine);
        bubbleMap.put(id, data);

        // Notify user on new bubble
        sendAddBubbleToServer(data);
    }

 // Can be dragged
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

                // Move the visual node
                bubbleGroup.setLayoutX(newX);
                bubbleGroup.setLayoutY(newY);

                // Updating the line's end point
                line.setEndX(newX);
                line.setEndY(newY);

                // Update model when bubble is moved
                Object userData = bubbleGroup.getUserData();
                if (userData instanceof Integer) {
                    int id = (Integer) userData;
                    BubbleData data = bubbleMap.get(id);
                    if (data != null) {
                        data.x = newX;
                        data.y = newY;

                        // Tell server that bubble moved
                        sendMoveBubbleToServer(data);
                    }
                }
            }
        });
    }

    // more work

    private void sendAddBubbleToServer(BubbleData bubble) {
        // In progress
        System.out.println("ADD bubble -> id=" + bubble.id + ", text=" + bubble.text +
                ", x=" + bubble.x + ", y=" + bubble.y);
    }

    private void sendMoveBubbleToServer(BubbleData bubble) {
        // In progress
        System.out.println("MOVE bubble -> id=" + bubble.id +
                ", x=" + bubble.x + ", y=" + bubble.y);
    }

    public static void main(String[] args) {
        launch(args);
    }
}


