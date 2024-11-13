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

import java.io.IOException;
import java.util.Scanner;

public class BoardMain {
    /**
     * Entry point for the Board actor system.
     * @param args not used
     */
    private static ActorSystem<Void> board;
    private static ActorRef<Message> boardRef;
    private static boolean loggedIn = false;
    private static Scanner scanner = new Scanner(System.in);

    private static BoardModel boardModel;

    public static void main(String[] args) throws IOException {
        // Create the board Akka system with initial actors.
        boardModel = new BoardModel("C:\\Users\\gil\\IdeaProjects\\java-06\\board\\src\\main\\resources\\ch\\bfh\\akka\\botrace\\board\\model\\board1.txt");

        board = ActorSystem.create(rootBehavior(), "ClusterSystem");
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
        while (true) {
            if (!loggedIn) {
                System.out.println("\n[1] Login\n[2] Register\n[3] Exit");
                System.out.print("Select an option: ");
                int choice = scanner.nextInt();
                switch (choice) {
                    case 1:
                        login();
                        break;
                    case 2:
                        register();
                        break;
                    case 3:
                        System.out.println("Exiting...");
                        board.terminate();
                        return;
                    default:
                        System.out.println("Invalid option.. Please try again.");
                }
            } else {
                gameMenu();
            }
        }
    }

    private static void login() {
        System.out.print("Enter username: ");
        String username = scanner.next();
        loggedIn = true;
        System.out.println("Logged in as " + username);
    }

    private static void register() {
        System.out.print("Register new username: ");
        String username = scanner.next();
        loggedIn = true;
        // logic to register new user

        System.out.println("Registered and logged in as " + username);
    }

    private static void gameMenu() {
        while (true) {
            System.out.println("\n[1] Start Game\n[2] End Game\n[3] Logout\n[4] Deregister");
            System.out.print("Select an option: ");
            int choice = scanner.nextInt();
            switch (choice) {
                case 1:
                    System.out.println("Starting the game...");
                    boardRef.tell(new StartRaceMessage());
                    displayBoard();
                    break;
                case 2:
                    System.out.println("Ending the game...");
                    return;
                case 3:
                    logout();
                    return;
                case 4:
                    deregister();
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
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


    private static void deregister() {
        loggedIn = false;
        System.out.println("Deregistered and logged out.");
    }

    private static void logout() {
        loggedIn = false;
        System.out.println("Logged out.");
    }
}
