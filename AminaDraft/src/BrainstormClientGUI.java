import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class BrainstormClientGUI extends Application {

    private Pane canvas;  // this will be our drawing area for bubbles

    @Override
    public void start(Stage primaryStage) {
        // For now, just a blank canvas
        canvas = new Pane();

        Scene scene = new Scene(canvas, 900, 600);
        primaryStage.setTitle("BrainStorm Bubble - Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
