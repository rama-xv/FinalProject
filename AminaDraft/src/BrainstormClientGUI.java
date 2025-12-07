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
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.Optional;

// BrainstormClientGUI
// Current status: Connect, add bubble, clear button
// Add bubble: Adds your ideas
// Clear: Removes bubbles
// Connect: Start network connection


 //Must do:
 // - Connect GUI to model
 // -Connect to networking

public class BrainstormClientGUI extends Application {

   // Creating plain, blank canvas
    private Pane canvas;

    // Counter to give each bubble a unique ID (local for now)
    private int nextBubbleId = 1;

    @Override
    public void start(Stage primaryStage) {
        // Main layout containers

        // BorderPane with:
        //    buttons on top
        //   bubbles in center
        BorderPane root = new BorderPane();

        // Creating canvas space
        canvas = new Pane();
        canvas.setPrefSize(900, 600);

        // Building buttons

        HBox topBar = new HBox(10);         // Creating 10px spacing between buttons
        topBar.setPadding(new Insets(10));  // Creating padding around the bar
        topBar.setAlignment(Pos.CENTER_LEFT);

        Button connectButton   = new Button("Connect");    // for networking later
        Button addBubbleButton = new Button("Add Bubble"); // create idea bubble
        Button clearButton     = new Button("Clear");      // remove all bubbles

        topBar.getChildren().addAll(connectButton, addBubbleButton, clearButton);

        root.setTop(topBar);
        root.setCenter(canvas);

        // Clear button behavior

        clearButton.setOnAction(event -> {
            // Remove all from canvas
            canvas.getChildren().clear();
        });

        // Add Bubble button behavior

        addBubbleButton.setOnAction(event -> {
            // Asking the user for idea text
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Idea Bubble");
            dialog.setHeaderText("Create a new idea bubble");
            dialog.setContentText("Enter bubble text:");

            Optional<String> result = dialog.showAndWait();

            result.ifPresent(text -> {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {

                    int bubbleId = nextBubbleId++;

                    // Circular placement around the center of the canvas

                    // Center of the canvas
                    double centerX = canvas.getWidth() / 2;
                    double centerY = canvas.getHeight() / 2;

                    // If canvas not laid out yet, fall back to default
                    if (centerX == 0 || centerY == 0) {
                        centerX = 450; // half of 900
                        centerY = 300; // half of 600
                    }

                    // Radius of the bubble circle
                    double radius = 200;

                    // How many "slots" around the circle we assume
                    int slots = 6;

                    // Choose an angle based on which bubble number this is.
                    double angleDegrees = (bubbleId - 1) * (360.0 / slots);
                    double angleRadians = Math.toRadians(angleDegrees);

                    // Position on the circle
                    double x = centerX + radius * Math.cos(angleRadians);
                    double y = centerY + radius * Math.sin(angleRadians);

                    // Create the bubble at the computed (x, y) position
                    createBubbleOnCanvas(bubbleId, x, y, trimmed);
                }
            });
        });

        // Connect button

        connectButton.setOnAction(event -> {
            // Networking later.
            System.out.println("Connect button clicked (networking not implemented yet).");
        });

        // Scene set up

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("BrainStorm Bubble - Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // creating bubble

    private void createBubbleOnCanvas(int id, double x, double y, String text) {
        double radius = 60; // bubble size

        // Circle for the bubble
        Circle circle = new Circle(radius);
        // WHITE fill and BLACK outline
        circle.setFill(Color.WHITE);
        circle.setStroke(Color.BLACK);
        circle.setStrokeWidth(2.0);

        // Text inside the bubble
        Text label = new Text(text);

        // centering text
        double textWidth = label.getLayoutBounds().getWidth();
        double textHeight = label.getLayoutBounds().getHeight();
        label.setX(-textWidth / 2);
        label.setY(textHeight / 4);

        // Group the circle and text together so they move as a unit
        Group bubbleGroup = new Group(circle, label);

        // Position the group so that its CENTER is at (x, y)
        bubbleGroup.setLayoutX(x);
        bubbleGroup.setLayoutY(y);

        // Store the bubble's ID
        bubbleGroup.setUserData(id);

        // Bubble can be dragged
        makeBubbleDraggable(bubbleGroup);

        // Add the bubble to the canvas
        canvas.getChildren().add(bubbleGroup);
    }

    private void makeBubbleDraggable(Group bubbleGroup) {
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

                // incomplete still
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}


