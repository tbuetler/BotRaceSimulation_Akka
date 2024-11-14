/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.cli;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import ch.bfh.akka.botrace.board.actor.Board;
import ch.bfh.akka.botrace.board.actor.BoardRoot;
import ch.bfh.akka.botrace.board.actor.ClusterListener;
import ch.bfh.akka.botrace.board.model.BoardUpdateListener;
import ch.bfh.akka.botrace.common.Message;
import ch.bfh.akka.botrace.board.model.BoardModel;
import ch.bfh.akka.botrace.common.boardmessage.StartRaceMessage;
import ch.bfh.akka.botrace.common.boardmessage.PauseMessage;
import ch.bfh.akka.botrace.common.boardmessage.ResumeMessage;
import javafx.application.Platform;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class BoardMain implements BoardUpdateListener {
    /**
     * Entry point for the Board actor system.
     *
     * @param args not used
     */
    private static ActorRef<Message> boardRef;
    private static ActorSystem<Void> board;
    private static Scanner scanner = new Scanner(System.in);

    private static BoardModel boardModel;

    private static boolean gameRunning = false;


    public static void main(String[] args) throws IOException {
        try {
            System.out.println("Select a board file by number:");

            List<String> files = Arrays.asList(getResourceListing(BoardMain.class, "ch/bfh/akka/botrace/board/model/"));

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
        URL resourceUrl = BoardMain.class.getResource("/ch/bfh/akka/botrace/board/model/" + boardChoiceShortcut);
        if (resourceUrl == null) {
            System.out.println("Resource file not found: "+boardChoiceShortcut);
            return;
        }

        String filePath = resourceUrl.getFile();
        boardModel = new BoardModel(filePath);
        boardModel.addBoardUpdateListener(new BoardMain()); // subscribe cli board to BoardModel

        board = ActorSystem.create(rootBehavior(), "ClusterSystem");
        System.out.println("Board Actor System created");
        runCli(); // display application
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
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

    private static void runCli() {
        boolean running = true;
        while (running) {
            if (!gameRunning) {
                System.out.println("\n[1] Start Game\n[2] Exit");
                System.out.print("Select an option: ");
                int choice = scanner.nextInt();
                switch (choice) {
                    case 1:
                        startGame();
                        gameMenu();
                        break;
                    case 2:
                        running = false;
                        System.out.println("Exiting...");
                        terminate();
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        }
    }

    private static void gameMenu() {
        gameRunning = true;
        while (gameRunning) {
            System.out.println("\n[1] Pause Game\n[2] Resume Game\n[3] End Game");
            System.out.print("Select an option: ");
            int choice = scanner.nextInt();
            switch (choice) {
                case 1:
                    pause();
                    displayBoard();
                    break;
                case 2:
                    resume();
                    displayBoard();
                    break;
                case 3:
                    endGame();
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    public static void startGame() {
        System.out.println("Game started");
        boardRef.tell(new StartRaceMessage());
        displayBoard();
    }

    private static void pause() {
        boardRef.tell(new PauseMessage());
        System.out.println("Game paused");
    }

    private static void resume() {
        boardRef.tell(new ResumeMessage());
        System.out.println("Game resumed");
    }

    private static void endGame() {
        System.out.println("Game ended");
        gameRunning = false;
        if (board != null) {
            board.terminate();
        }
        Platform.exit();
    }

    private static void terminate() {
        if (board != null) {
            board.terminate();
        }
    }

    private static void displayBoard() {
        System.out.println("Current Board:");
        char[][] currentBoard = boardModel.getBoard();
        if (currentBoard != null) {
            for (int i = 0; i < currentBoard.length; i++) {
                for (int j = 0; j < currentBoard[i].length; j++) {
                    System.out.print(currentBoard[i][j] + " ");
                }
                System.out.println();
            }
        } else {
            System.out.println("No board data available.");
        }
    }

    @Override
    public void boardUpdated() {
        displayBoard();// Call the display function whenever an update is notified
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        boardRef.tell(new StartRaceMessage());
    }
}
