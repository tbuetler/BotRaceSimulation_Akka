/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class BoardMain extends Application {

    /**
     * Initialize the actor system.
     */
    @Override
    public void init() {
        // TODO Initialize the board actor system, keep a reference to it.
    }

    /**
     * Constructs the GUI and shows it to the user.
     * @param primaryStage the primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        // TODO Replace the following by a 'real' GUI
        Label label = new Label("Hello Akka programmer, please complete the GUI for the board");
        StackPane pane = new StackPane(label);
        Scene scene = new Scene(pane, 500, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Board of Group xyz");
        primaryStage.show();
    }

    @Override
    public void stop() {
        // TODO Terminate actor system
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
