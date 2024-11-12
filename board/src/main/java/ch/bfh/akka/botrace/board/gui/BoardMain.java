/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.gui;

import akka.actor.ActorSystem;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
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

	private ActorSystem actorSystem;

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

		// Load the FXML file
		FXMLLoader loader = new FXMLLoader(getClass().getResource("BoardMain.fxml"));
		if (loader.getLocation() == null) {
			throw new IllegalStateException("FXML file not found");
		}
		Parent root = loader.load();


		// set up the GUI
		Scene scene = new Scene(root, 500, 200);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Board of Group 06");
		primaryStage.show();

		// set up the event handlers
		start.setOnAction(event -> startRace());
		stop.setOnAction(event -> stopRace());
	}

	@Override
	public void stop() {
		// TODO Terminate actor system
		actorSystem.terminate();
		Platform.exit();
	}

	public static void main(String[] args) {
		Application.launch(args);
	}

	public void startRace() {
		System.out.println("Race started.");
	}

	public void stopRace() {
		System.out.println("Race stopped.");
	}
}
