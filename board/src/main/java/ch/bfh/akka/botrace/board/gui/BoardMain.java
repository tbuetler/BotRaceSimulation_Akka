package ch.bfh.akka.botrace.board.gui;

import akka.actor.typed.ActorSystem;
import ch.bfh.akka.botrace.board.actor.BoardRoot;
import ch.bfh.akka.botrace.board.model.BoardModel;
import ch.bfh.akka.botrace.common.Message;
import ch.bfh.akka.botrace.common.boardmessage.PauseMessage;
import ch.bfh.akka.botrace.common.boardmessage.ResumeMessage;
import ch.bfh.akka.botrace.common.boardmessage.SetupMessage;
import ch.bfh.akka.botrace.common.boardmessage.StartMessage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import java.io.IOException;

public class BoardMain extends Application {

	// is used to manage the state of the board. It Provides methods to read the board file and update the state of the board
	private static BoardModel boardModel;
	// uses the ActorSystem to create and manage actors. It send messages to the BoardRoot actor to control the game.
	private ActorSystem<Message> actorSystem;
	private ChoiceBox<Integer> speed;

	/**
	 * Initialize the actor system.
	 */
	@Override
	public void init() throws IOException {
		// Initialisiert das BoardModel mit der Dateipfad für das Board
		String boardChoiceShortcut = "";
		int boardChoice = 1; // Please choose your board
		boardChoiceShortcut = switch (boardChoice) {
			case 1 -> "board1.txt";
			case 2 -> "board2.txt";
			case 3 -> "board3.txt";
			case 4 -> "board4.txt";
			case 5 -> "board5.txt";
			default -> "board1.txt";
		};
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
		TableView<String> botTable = createBotTable();
		topBox.getChildren().add(botTable);

		// Unterer Bereich: Steuerungsbuttons
		HBox controlButtons = createControlButtons();
		root.setBottom(controlButtons);

		// Zentraler Bereich: Spielfeld
		GridPane gamefield = createGameField();
		root.setCenter(gamefield);

		// Das Layout dem Hauptfenster hinzufügen
		root.setTop(topBox);


		// Szene erstellen und anzeigen
		Scene scene = new Scene(root, 1200, 800);
		primaryStage.setScene(scene);
		primaryStage.setTitle("BotRace Board");
		primaryStage.show();
	}
	
	/**
	 * Erzeugt die Tabelle für die Anzeige der Bot-Informationen.
	 * @return Eine TableView mit Spalten für Bot-Name, Actor-Referenz, Position, Distanz und Ping-Status.
	 */
	private TableView<String> createBotTable() {
		TableView<String> table = new TableView<>();
		table.setPrefHeight(150); // Setzt die Höhe der Tabelle

		// Erzeugt und konfiguriert die einzelnen Spalten
		TableColumn<String, String> botNameCol = new TableColumn<>("Bot Name");
		botNameCol.setPrefWidth(100);

		TableColumn<String, String> actorRefCol = new TableColumn<>("Actor Ref");
		actorRefCol.setPrefWidth(250);

		TableColumn<String, String> posCol = new TableColumn<>("Pos");
		posCol.setPrefWidth(100);

		TableColumn<String, String> distCol = new TableColumn<>("Dist");
		distCol.setPrefWidth(75);

		TableColumn<String, String> pingCol = new TableColumn<>("Ping Status");
		pingCol.setPrefWidth(75);

		// Fügt die Spalten zur Tabelle hinzu
		table.getColumns().addAll(botNameCol, actorRefCol, posCol, distCol, pingCol);
		return table;
	}

	/**
	 * Erstellt das Spielfeld für das Board, basierend auf den Daten im `BoardModel`.
	 * Jede Zelle im Spielfeld wird als Rechteck dargestellt, dessen Farbe je nach Inhalt variiert:
	 * - Rote Zellen ('X') stellen Hindernisse dar.
	 * - Grüne Zellen ('S') repräsentieren den Startpunkt.
	 * - Blaue Zellen ('E') markieren das Ziel.
	 * - Weiße Zellen ('_') stellen leere Felder dar.
	 * Die Zellen werden in einem `GridPane` angeordnet, um das Spielfeld darzustellen.
	 *
	 * @return Ein `GridPane`-Objekt, das das visualisierte Spielfeld enthält.
	 */
	private GridPane createGameField() {
		// Erstellen des GridPane für das Spielfeld
		GridPane gridPane = new GridPane();
		gridPane.setHgap(5); // Horizontaler Abstand zwischen den Zellen
		gridPane.setVgap(5); // Vertikaler Abstand zwischen den Zellen
		gridPane.setAlignment(Pos.CENTER); // Das GridPane wird im Zentrum des Fensters ausgerichtet

		// Dimensionen des Spielfeldes anpassen (Basierend auf den Board-Daten)
		char[][] currentBoard = boardModel.getBoard();
		if (currentBoard != null) {
			for (int i = 0; i < currentBoard.length; i++) {
				for (int j = 0; j < currentBoard[i].length; j++) {
					// Erstellen eines Rechtecks für jede Zelle
					Rectangle cell = new Rectangle(30, 30);
					switch (currentBoard[i][j]) {
						case 'X':
							cell.setFill(Color.RED); // Hindernis
							break;
						case 'S':
							cell.setFill(Color.GREEN); // start
							break;
						case 'E':
							cell.setFill(Color.BLUE); // Ziel
							break;
						case '_':
							cell.setFill(Color.WHITE); // leeres Feld
							break;
						default:
							cell.setFill(Color.BLACK); // Wand
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

		speed = new ChoiceBox<>();
		speed.getItems().addAll(200, 500, 2000, 5000);
		speed.setValue(200); // Default value

		Button playRace = new Button("Play Race");
		Button pauseRace = new Button("Pause Race");
		Button resumeRace = new Button("Resume Race");
		Button terminateRace = new Button("Terminate Race");

		speed.setOnAction(event -> {
			Integer selectedSpeed = speed.getValue();
			actorSystem.tell(new SetupMessage(selectedSpeed));
		});

		playRace.setOnAction(event -> {
			actorSystem.tell(new StartMessage());
		});

		pauseRace.setOnAction(event -> {
			actorSystem.tell(new PauseMessage());
		});

		resumeRace.setOnAction(event -> {
			actorSystem.tell(new ResumeMessage());
		});

		terminateRace.setOnAction(event -> {;
			if (actorSystem != null) {
				actorSystem.terminate();
			}
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
