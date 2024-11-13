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
import ch.bfh.akka.botrace.common.Message;
import ch.bfh.akka.botrace.board.model.BoardModel;
import ch.bfh.akka.botrace.common.boardmessage.StartRaceMessage;
import ch.bfh.akka.botrace.common.boardmessage.PauseMessage;
import ch.bfh.akka.botrace.common.boardmessage.ResumeMessage;
import ch.bfh.akka.botrace.common.boardmessage.StartMessage;
import javafx.application.Platform;

import java.io.IOException;
import java.util.Scanner;

public class BoardMain {
    /**
     * Entry point for the Board actor system.
     * @param args not used
     */
    private static ActorRef<Message> boardRef;
    private static ActorSystem<Message> board;
    //private static ActorSystem<Void> board;
    private static boolean loggedIn = false;
    private static Scanner scanner = new Scanner(System.in);

    private static BoardModel boardModel;

    private static boolean gameRunning = false;


    public static void main(String[] args) throws IOException {
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
        boardModel = new BoardModel("/Users/martin/BFH/SW2/java-06/board/target/classes/ch/bfh/akka/botrace/board/model/"+boardChoiceShortcut);
        //boardModel = new BoardModel("C:\\Users\\gil\\IdeaProjects\\java-06\\board\\src\\main\\resources\\ch\\bfh\\akka\\botrace\\board\\model\\"+boardChoiceShortcut);

        board = ActorSystem.create(BoardRoot.create(boardModel), "BoardActorSystem");
        //board = ActorSystem.create(rootBehavior(), "ClusterSystem");
        board.log().info("Board Actor System created");
        runCli(); // display application
    }

    /**
     * Creates the two actors {@link ClusterListener} and {@link BoardRoot}.
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
                    System.out.println("Starting the game...");
                    boardRef.tell(new StartRaceMessage());
                    displayBoard();
                    break;
                case 2:
                    resume();
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
        board.tell(new StartMessage());
        updateBoard(boardModel.displayBoard());
    }

    private static void pause() {
        board.tell(new PauseMessage());
        System.out.println("Game paused");
    }

    private static void resume() {
        board.tell(new ResumeMessage());
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
}
