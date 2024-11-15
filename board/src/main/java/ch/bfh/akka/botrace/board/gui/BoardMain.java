package ch.bfh.akka.botrace.board.gui;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import ch.bfh.akka.botrace.board.actor.BoardRoot;
import ch.bfh.akka.botrace.board.actor.ClusterListener;
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BoardMain extends Application {
    private static ActorRef<Message> boardRef;

    // is used to manage the state of the board. It Provides methods to read the board file and update the state of the board
    private static BoardModel boardModel;
    // uses the ActorSystem to create and manage actors. It send messages to the BoardRoot actor to control the game.
    private ActorSystem<Void> actorSystem;
    private ChoiceBox<Integer> speed;

    private static Scanner scanner = new Scanner(System.in);

    /**
     * Initialize the actor system.
     */
    @Override
    public void init() throws IOException {
        try {
            System.out.println("Select a board file by number:");

            List<String> files = Arrays.asList(getResourceListing(ch.bfh.akka.botrace.board.cli.BoardMain.class, "ch/bfh/akka/botrace/board/model/"));

            if (files.isEmpty()) {
                System.out.println("No boards found");
                return;
            }

            for (int i = 0; i < files.size(); i++) {
                System.out.printf("[%d] %s\n", i + 1, files.get(i));
            }

            System.out.print("Choose your board: ");

            int fileIndex = scanner.nextInt() - 1;
            if (fileIndex < 0 || fileIndex >= files.size()) {
                System.out.println("Invalid selection.");
                return;
            }

            String boardChoiceShortcut = files.get(fileIndex);
            URL resourceUrl = ch.bfh.akka.botrace.board.cli.BoardMain.class.getResource("/ch/bfh/akka/botrace/board/model/" + boardChoiceShortcut);
            if (resourceUrl == null) {
                System.out.println("Resource file not found: "+boardChoiceShortcut);
                return;
            }

            String filePath = resourceUrl.getFile();
            boardModel = new BoardModel(filePath);
            boardModel.addBoardUpdateListener(new ch.bfh.akka.botrace.board.cli.BoardMain()); // subscribe cli board to BoardModel

            actorSystem = ActorSystem.create(rootBehavior(), "ClusterSystem");
            System.out.println("Board Actor System created");
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the two actors {@link ClusterListener} and {@link BoardRoot}.
     *
     * @return a void behavior
     */
    private static Behavior<Void> rootBehavior() {
        return Behaviors.setup(context -> {

            context.spawn(ClusterListener.create(),"ClusterListener");

            boardRef = context.spawn(BoardRoot.create(boardModel), "BoardRoot");

            context.getLog().info("BoardRoot with BoardModel created");

            return Behaviors.empty();
        });
    }

    /**
     * Copied code: Source -> https://www.uofr.net/~greg/java/get-resource-listing.html
     * List directory contents for a resource folder. Not recursive.
     * This is basically a brute-force implementation.
     * Works for regular files and also JARs.
     *
     * @author Greg Briggs
     * @param clazz Any java class that lives in the same place as the resources you want.
     * @param path Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @throws URISyntaxException
     * @throws IOException
     */
    static String[] getResourceListing(Class clazz, String path) throws URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            /* A file path: easy enough */
            return new File(dirURL.toURI()).list((dir, name) -> name.endsWith(".txt")); // filter to only include .txt files
        }

        if (dirURL == null) {
            /*
             * In case of a jar file, we can't actually find a directory.
             * Have to assume the same jar as clazz.
             */
            String me = clazz.getName().replace(".", "/")+".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        if (dirURL.getProtocol().equals("jar")) {
            /* A JAR path */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
            while(entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path) && name.endsWith(".txt")) { //filter according to the path
                    String entry = name.substring(path.length());
                    int checkSubdir = entry.indexOf("/");
                    if (checkSubdir >= 0) {
                        // if it is a subdirectory, we just return the directory name
                        entry = entry.substring(0, checkSubdir);
                    }
                    result.add(entry);
                }
            }
            return result.toArray(new String[result.size()]);
        }

        throw new UnsupportedOperationException("Cannot list files for URL "+dirURL);
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
            boardRef.tell(new SetupMessage(selectedSpeed));
        });

        playRace.setOnAction(event -> {
            boardRef.tell(new StartMessage());
        });

        pauseRace.setOnAction(event -> {
            boardRef.tell(new PauseMessage());
        });

        resumeRace.setOnAction(event -> {
            boardRef.tell(new ResumeMessage());
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