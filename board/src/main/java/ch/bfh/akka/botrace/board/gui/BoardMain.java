package ch.bfh.akka.botrace.board.gui;

import akka.actor.typed.ActorSystem;
import ch.bfh.akka.botrace.board.actor.BoardRoot;
import ch.bfh.akka.botrace.board.model.BoardModel;
import ch.bfh.akka.botrace.common.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import scala.Int;

import java.io.IOException;

public class BoardMain extends Application {

	private static BoardModel boardModel;
	private ActorSystem<Message> actorSystem;
	private GridPane gamefield; // Spielfeld
	private Label statusLabel;
	private ChoiceBox<Integer> speed;

	/**
	 * Initialize the actor system.
	 */
	@Override
	public void init() throws IOException {
		// Initialisiert das BoardModel mit der Dateipfad für das Board
		String boardChoiceShortcut = "";
		int boardChoice = 1; // Please choose your board
		switch(boardChoice) {
			case 1:
				boardChoiceShortcut = "board1.txt";
			case 2:
				boardChoiceShortcut = "board2.txt";
			case 3:
				boardChoiceShortcut = "board3.txt";
			case 4:
				boardChoiceShortcut = "board4.txt";
			case 5:
				boardChoiceShortcut = "board5.txt";

		}
		boardModel = new BoardModel("/home/tim/Documents/BFH/SpecialWeek/SpecialWeek_2/java-06/board/src/main/resources/ch/bfh/akka/botrace/board/model/" + boardChoiceShortcut);
		actorSystem = ActorSystem.create(BoardRoot.create(boardModel), "BoardActorSystem");
	}

	/**
	 * Konstruktion und Anzeige der GUI.
	 * @param primaryStage Hauptfenster der Anwendung.
	 */
	@Override
	public void start(Stage primaryStage) {
		// Layout des Fensters
		BorderPane root = new BorderPane();

		// Oberer Bereich: Status-Label
		VBox topBox = new VBox(10);
		topBox.setAlignment(Pos.CENTER);

		// Unterer Bereich: Steuerungsbuttons
		HBox controlButtons = createControlButtons();

		// Zentraler Bereich: Spielfeld
		gamefield = createGameField();
		root.setCenter(gamefield);

		// Das Layout dem Hauptfenster hinzufügen
		root.setTop(topBox);
		root.setBottom(controlButtons);

		// Szene erstellen und anzeigen
		Scene scene = new Scene(root, 800, 600);
		primaryStage.setScene(scene);
		primaryStage.setTitle("BotRace Board");
		primaryStage.show();
	}

	private GridPane createGameField() {
		// Erstellen des GridPane für das Spielfeld
		GridPane gridPane = new GridPane();
		gridPane.setHgap(2); // Horizontaler Abstand zwischen den Zellen
		gridPane.setVgap(2); // Vertikaler Abstand zwischen den Zellen
		gridPane.setAlignment(Pos.CENTER); // Das GridPane wird im Zentrum des Fensters ausgerichtet

		// Dimensionen des Spielfeldes anpassen (Basierend auf den Board-Daten)
		char[][] currentBoard = boardModel.getBoard();
		if (currentBoard != null) {
			for (int i = 0; i < currentBoard.length; i++) {
				for (int j = 0; j < currentBoard[i].length; j++) {
					// Erstellen eines Rechtecks für jede Zelle
					Rectangle cell = new Rectangle(30, 30);
					if (currentBoard[i][j] == 'X') {
						cell.setFill(Color.RED); // Beispiel: 'X' für einen Bot
					} else {
						cell.setFill(Color.LIGHTGRAY); // Beispiel: leeres Feld
					}
					// Zelle im GridPane an der entsprechenden Position hinzufügen
					gridPane.add(cell, j, i);
				}
			}
		}

		return gridPane;
	}

	/**
	 * Steuerungsbuttons für das Spiel erstellen.
	 * @return HBox mit Steuerungsbuttons.
	 */
	private HBox createControlButtons() {
		HBox controlBox = new HBox(10);

		speed = new ChoiceBox();
		speed.getItems().addAll(200, 500, 2000, 5000);
		speed.setValue(200); // Default value

		Button playRace = new Button("Play Race");
		Button pauseRace = new Button("Pause Race");
		Button resumeRace = new Button("Resume Race");
		Button terminateRace = new Button("Terminate Race");

		speed.setOnAction(event -> {
			// TODO Implement setup race action
			Integer selectedSpeed = speed.getValue();
			System.out.println("Selected speed: " + selectedSpeed);
		});

		playRace.setOnAction(event -> {
			// TODO Implement play race action
		});

		pauseRace.setOnAction(event -> {
			// TODO Implement pause race action
		});

		resumeRace.setOnAction(event -> {
			// TODO Implement resume race action
		});

		terminateRace.setOnAction(event -> {
			// TODO Implement terminate race action
			Platform.exit();
		});

		controlBox.getChildren().addAll(speed, playRace, pauseRace, resumeRace, terminateRace);
		controlBox.setAlignment(Pos.CENTER);
		return controlBox;
	}

	public static void main(String[] args) {
		launch(args);
	}
}
