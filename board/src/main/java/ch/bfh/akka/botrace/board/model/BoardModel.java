/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.model;

import akka.actor.typed.ActorRef;
import ch.bfh.akka.botrace.board.actor.Board;
import ch.bfh.akka.botrace.common.Direction;
import ch.bfh.akka.botrace.common.Message;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Model for managing the state of the board. Implements the {@link Board}
 * interface. As called by an actor thread, use {@link javafx.application.Platform#runLater(Runnable)}.
 */
public class BoardModel implements Board {

    private char[][] board;
    private int rows;
    private int cols;
    private Position start;
    private Position end;
    private Map<ActorRef<Message>, Position> playerPosition = new HashMap<>();
    private Map<ActorRef<Message>, String> playerName = new HashMap<>();
    private List<ActorRef<Message>> bots = new ArrayList<>();

    private List<BoardUpdateListener> listeners = new ArrayList<>(); // define listeners

    public void addBoardUpdateListener(BoardUpdateListener listener) {
        listeners.add(listener);
    }

    private void notifyUiListeners() {
        for (BoardUpdateListener listener : listeners) {
            listener.boardUpdated();
        }
    }

    /**
     *
     * @param filepath for different boards 1-5
     *
     */
    public BoardModel(String filepath) throws IOException {
        loadBoard(filepath);
    }


    // Load board from file into 2D array
    private void loadBoard(String filePath) throws IOException {
        List<String> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String row;
            while ((row = br.readLine()) != null) {
                rows.add(row.replaceAll(" ", "")); //
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.rows = rows.size();
        this.cols = rows.get(0).length();

        // Erstellen des 2D-Arrays
        this.board = new char[this.rows][this.cols];

        // Befüllen des Arrays
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.cols; j++) {
                board[i][j] = rows.get(i).charAt(j);
                if(board[i][j] == 'S'){
                    this.start = new Position(i,j);
                }if(board[i][j] == 'E'){
                    this.end = new Position(i,j);
                }
            }
        }
    }

    @Override
    public void registerNewBot(String name, ActorRef<Message> botRef) {
        //Todo: does register need to send a message back to bot?
        playerName.put(botRef, name);
        playerPosition.put(botRef, start);
        bots.add(botRef);

        //notifyUiListeners();  // trigger listener to rerender UI on new bot registration
    }

    @Override
    public boolean playChosenDirection(Direction direction, ActorRef<Message> botRef) {

        //checks if move valid
        if(isMoveValid(botRef, direction)) {
            int row = playerPosition.get(botRef).getRow();
            int col = playerPosition.get(botRef).getCol();

            switch (direction) {
                case N:
                    row -= 1;
                    break;
                case S:
                    row += 1;
                    break;
                case E:
                    col += 1;
                    break;
                case W:
                    col -= 1;
                    break;
                case NE:
                    row -= 1;
                    col += 1;
                    break;
                case NW:
                    row -= 1;
                    col -= 1;
                    break;
                case SE:
                    row += 1;
                    col += 1;
                    break;
                case SW:
                    row += 1;
                    col -= 1;
                    break;
            }
            Position newPosition = new Position(row,col);
            playerPosition.put(botRef,newPosition);

            notifyUiListeners(); // trigger ui listener on new move played


            return true;
        }

        //move not valid
        return false;
    }

    @Override
    public List<Direction> getAvailableDirection(ActorRef<Message> botRef) {

        List<Direction> validDirections = new ArrayList<>();

        for(Direction direction : Direction.values()){
            if(isMoveValid(botRef,direction)){
                validDirections.add(direction);
            }
        }

        return validDirections;
    }



    @Override
    public boolean checkIfBotFinished(ActorRef<Message> botRef) {
         Position botPos = playerPosition.get(botRef);
         if(botPos.col == this.end.col && botPos.row == this.end.row){
             return true;
         }else{
             return false;
         }
    }

    @Override
    public boolean isMoveValid(ActorRef<Message> botRef, Direction direction){

        int row = playerPosition.get(botRef).getRow();
        int col = playerPosition.get(botRef).getCol();

        // no check needed for no move
        if (direction == Direction.X) {
            return true;
        }

        // Get the board dimensions
        int rows = board.length;
        int cols = board[0].length;

        switch (direction) {
            case N:
                if (row - 1 < 0 || board[row - 1][col] == 'X') {
                    return false;
                }else{return true;}

            case Direction.S:
                if (row + 1 >= rows || board[row + 1][col] == 'X') {
                    return false;
                }else{return true;}

            case Direction.E:
                if (col + 1 >= cols || board[row][col + 1] == 'X') {
                    return false;
                }else{return true;}

            case Direction.W:
                if (col - 1 < 0 || board[row][col - 1] == 'X') {
                    return false;
                }else{return true;}

            case Direction.NE:
                if (row - 1 < 0 || col + 1 >= cols || board[row - 1][col + 1] == 'X') {
                    return false;
                }else{return true;}

            case Direction.NW:
                if (row - 1 < 0 || col - 1 < 0 || board[row - 1][col - 1] == 'X') {
                    return false;
                }else{return true;}


            case Direction.SE:
                if (row + 1 >= rows || col + 1 >= cols || board[row + 1][col + 1] == 'X') {
                    return false;
                }else{return true;}

            case Direction.SW:
                if (row + 1 >= rows || col - 1 < 0 || board[row + 1][col - 1] == 'X') {
                    return false;
                }else{return true;}
        }

        return false;
    }

    public void deregister(ActorRef<Message> botRef) {
        playerPosition.remove(botRef);
        playerName.remove(botRef);
        bots.remove(botRef);
    }


    public int getDistanceToTarget(ActorRef<Message> botRef) {

        Position pos = playerPosition.get(botRef);

        int diffRows = Math.abs(pos.row - end.row);
        int diffCols = Math.abs(pos.col - end.col);

        return Math.max(diffCols,diffRows);
    }


    public char[][] getBoard(){

        char[][] boardCopy = new char[board.length][];
        for (int i = 0; i < board.length; i++) {
            boardCopy[i] = board[i].clone();
        }

        //set players
        for(Map.Entry<ActorRef<Message>, Position> entry : playerPosition.entrySet()) {
            Position pos = entry.getValue();
            String player = playerName.get(entry.getKey());


            // Place the player's name on the board (e.g., just the first letter for simplicity)
            if (pos.getRow() >= 0 && pos.getRow() < rows && pos.getCol() >= 0 && pos.getCol() < cols) {
                boardCopy[pos.getRow()][pos.getCol()] = player.charAt(0);
            }
        }

        return boardCopy;
    }


    public Map<ActorRef<Message>, Position> getPlayerPosition() {
        return playerPosition;
    }

    public Map<ActorRef<Message>, String> getPlayerName() {
        return playerName;
    }

    public List<ActorRef<Message>> getBots(){
        return bots;
    }
}
