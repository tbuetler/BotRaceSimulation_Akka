/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
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
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class BoardMain extends Application {

	// Deklariert die Hauptkomponenten der GUI
	private TableView<String> botTable; // Tabelle für die Bot-Informationen
	private ChoiceBox<Integer> speed; // Wahlbox zur Auswahl der Geschwindigkeit
	private Button setupRace, playRace, pauseRace, resumeRace, terminateRace; // Steuerungsbuttons
	private GridPane gamefield; // Spielbereich als Raster für Bots und Hindernisse
	private ActorSystem<Message> actorSystem; // Referenz zur Board Actor System

    /**
     * Initialize the actor system.
     */
    @Override
    public void init() {
        // TODO Initialize the board actor system, keep a reference to it.
        actorSystem = ActorSystem.create(BoardRoot.create(new BoardModel()), "BoardActorSystem");
	}

    /**
     * Constructs the GUI and shows it to the user.
     * @param primaryStage the primary stage for this application.
     */
    @Override
	public void start(Stage primaryStage) {
		// Erzeugt ein BorderPane als Hauptlayout
		BorderPane root = new BorderPane();

		// Oberer Bereich - Bot-Tabelle
		botTable = createBotTable();
		root.setTop(botTable);

		// Unterer Bereich - Steuerungsbuttons
		HBox controlButtons = createControlButtons();
		root.setBottom(controlButtons);

		// Zentraler Bereich - Spielfeld
		gamefield = createGameField();
		root.setCenter(gamefield);

		// Initialisiert die Szene und zeigt sie an
		Scene scene = new Scene(root, 800, 600);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Board of Group 06");
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
	 * Erstellt eine HBox mit Steuerungsbuttons für das Rennen.
	 * @return Eine HBox mit Buttons zum Steuern des Rennens.
	 */
	private HBox createControlButtons() {
		HBox controlBox = new HBox(10); // HBox mit 10 Pixeln Abstand zwischen den Buttons

		// Erzeugt die Steuerungsbuttons und fügt sie zur HBox hinzu
		setupRace = new Button("Setup Race");
		playRace = new Button("Play Race");
		pauseRace = new Button("Pause Race");
		resumeRace = new Button("Resume Race");
		terminateRace = new Button("Terminate Race");

		setupRace.setOnAction(event -> {
			// TODO Implement the setup race action
		});

		playRace.setOnAction(event -> {
			// TODO Implement the play race action
		});

		pauseRace.setOnAction(event -> {
			// TODO Implement the pause race action
		});

		resumeRace.setOnAction(event -> {
			// TODO Implement the resume race action
		});

		terminateRace.setOnAction(event -> {
			stop();
		});

		controlBox.getChildren().addAll(setupRace, playRace, pauseRace, resumeRace, terminateRace);
		controlBox.setAlignment(Pos.CENTER); // Zentriert die Buttons
		controlBox.setPrefHeight(50); // Setzt die Höhe des Steuerungsbereichs

		return controlBox;
	}

	/**
	 * Erstellt das Spielfeld als Raster für Bots und Hindernisse.
	 * @return Ein GridPane, das das Spielfeld repräsentiert.
	 */
	private GridPane createGameField() {
		GridPane grid = new GridPane();
		grid.setPrefSize(600, 400); // Setzt die Größe des Spielfeldes
		grid.setStyle("-fx-background-color: white;"); // Setzt den Hintergrund auf weiß

		// Fügt Wände (graue Rechtecke) hinzu
		for (int i = 1; i < 4; i++) {
			for (int j = 1; j < 3; j++) {
				Pane wall = new Pane();
				wall.setStyle("-fx-background-color: gray;"); // Graue Farbe für die Wände
				wall.setPrefSize(60, 60); // Setzt die Größe der Wände
				grid.add(wall, i, j); // Fügt die Wand an eine Position im Raster hinzu
			}
		}

		// TODO: Hier können die Bots hinzugefügt werden. Diese dienen nur als Beispiel
		// Fügt Bots (farbige Kreise) an verschiedene Positionen hinzu
		Circle bot1 = createBotCircle(Color.BLUE, "Bot 1");
		Circle bot2 = createBotCircle(Color.RED, "Bot 2");
		Circle bot3 = createBotCircle(Color.GREEN, "Bot 3");
		Circle bot4 = createBotCircle(Color.YELLOW, "Bot 4");

		grid.add(bot1, 2, 5); // Positioniert Bot 1 im Raster
		grid.add(bot2, 3, 3); // Positioniert Bot 2 im Raster
		grid.add(bot3, 5, 2); // Positioniert Bot 3 im Raster
		grid.add(bot4, 6, 4); // Positioniert Bot 4 im Raster

		return grid;
	}

	/**
	 * Erzeugt einen farbigen Kreis für einen Bot.
	 * @param color Die Farbe des Kreises (Bots).
	 * @param botName Der Name des Bots für die Identifikation.
	 * @return Ein Kreis, der den Bot repräsentiert.
	 */
	private Circle createBotCircle(Color color, String botName) {
		Circle bot = new Circle(20, color); // Erzeugt einen Kreis mit 20 Pixeln Radius und der angegebenen Farbe
		bot.setUserData(botName); // Speichert den Namen des Bots in den User-Daten
		Tooltip.install(bot, new Tooltip(botName)); // Tooltip zeigt den Namen des Bots an, wenn man über den Kreis fährt
		return bot;
	}

	/**
	 * Beendet die Anwendung und schließt die Plattform.
	 */
	@Override
	public void stop() {
		if (actorSystem != null) {
			actorSystem.terminate();
		}
		Platform.exit(); // Schließt die JavaFX-Anwendung
	}

	/**
	 * Hauptmethode zum Starten der Anwendung.
	 * @param args Standard-Argumente für die Main-Methode
	 */
	public static void main(String[] args) {
		launch(args); // Startet die JavaFX-Anwendung
	}
}
