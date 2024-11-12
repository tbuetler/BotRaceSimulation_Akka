/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.gui;

import akka.actor.ActorSystem;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;

public class BoardMain extends Application {

	@FXML
	private VBox botList;
	@FXML
	private ChoiceBox<Integer> speed;
	@FXML
	private Button start;
	@FXML
	private Button stop;
	@FXML
	private GridPane gamefield;

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
	public void start(Stage primaryStage) throws IOException {
		// TODO Replace the following by a 'real' GUI
		//Label label = new Label("Hello Akka programmer, please complete the GUI for the board");

		// Create the root layout
		BorderPane root = new BorderPane();

		// Create the VBox for bot list and controls
		botList = new VBox();
		botList.setPrefHeight(91.0);
		botList.setPrefWidth(600.0);

		// Create a ChoiceBox for speed
		speed = new ChoiceBox<>();
		speed.getItems().addAll(200, 500, 2000, 5000);
		speed.setValue(speed.getItems().getFirst());
		speed.setPrefWidth(150.0);

		// Create Start and Stop buttons
		start = new Button("Start");
		start.setOnAction(event -> startRace());

		stop = new Button("Stop");
		stop.setOnAction(event -> stopRace());

		// Add the ChoiceBox and buttons to the left side (VBox)
		VBox leftPanel = new VBox(speed, start, stop);
		leftPanel.setPrefWidth(100.0);

		// Create the game field (GridPane)
		gamefield = new GridPane();
		gamefield.setPrefWidth(400.0);
		gamefield.setPrefHeight(200.0);

		// Configure grid column constraints
		ColumnConstraints col1 = new ColumnConstraints();
		col1.setHgrow(javafx.scene.layout.Priority.SOMETIMES);
		col1.setMinWidth(10.0);
		col1.setPrefWidth(100.0);

		ColumnConstraints col2 = new ColumnConstraints();
		col2.setHgrow(javafx.scene.layout.Priority.SOMETIMES);
		col2.setMinWidth(10.0);
		col2.setPrefWidth(100.0);

		gamefield.getColumnConstraints().addAll(col1, col2);

		// Configure grid row constraints
		RowConstraints row1 = new RowConstraints();
		row1.setVgrow(javafx.scene.layout.Priority.SOMETIMES);
		row1.setMinHeight(10.0);
		row1.setPrefHeight(30.0);

		RowConstraints row2 = new RowConstraints();
		row2.setVgrow(javafx.scene.layout.Priority.SOMETIMES);
		row2.setMinHeight(10.0);
		row2.setPrefHeight(30.0);

		gamefield.getRowConstraints().addAll(row1, row2);

		// Set the components into the BorderPane layout
		root.setTop(botList);
		root.setLeft(leftPanel);
		root.setCenter(gamefield);

		// set up the GUI
		Scene scene = new Scene(root, 500, 200);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Board of Group 06");
		primaryStage.show();
	}

	@Override
	public void stop() {
		// TODO Terminate actor system
		Platform.exit();
	}

	public static void main(String[] args) {
		Application.launch(args);
	}

	public void startRace() {
		System.out.println("Race started.\nSpeed: " + speed.getValue());
	}

	public void stopRace() {
		System.out.println("Race stopped.");
	}
}
