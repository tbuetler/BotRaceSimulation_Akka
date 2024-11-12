/*
 * Special Week 2 (BTI5205), © 2024 Berner Fachhochschule
 */
package ch.bfh.akka.botrace.board.model;

import akka.actor.Actor;
import akka.actor.typed.ActorRef;
import ch.bfh.akka.botrace.board.actor.Board;
import ch.bfh.akka.botrace.common.Direction;
import ch.bfh.akka.botrace.common.Message;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    //directional offsets for 8 possible moves
    private static final int[][] DIRECTIONS = {
            {-1, 0}, // N
            {1, 0},  // S
            {0, -1}, // W
            {0, 1},  // E
            {-1, -1}, // NE
            {-1, 1},  // NW
            {1, -1},  // SW
            {1, 1}    // SE
    };

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
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        int row = 0;

        while ((line = br.readLine()) != null) {
            if (board == null) {
                rows = line.length() / 2;
                cols = rows;
                board = new char[rows][cols];
            }
            for (int col = 0; col < line.length(); col += 2) {
                board[row][col / 2] = line.charAt(col);
            }
            row++;
        }
        br.close();

        //saving start and end
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == 'S') {
                    this.start = new Position(i, j);
                } else if (board[i][j] == 'E') {
                    this.end = new Position(i, j);
                }
            }
        }
    }

    @Override
    public void registerNewBot(String name, ActorRef<Message> botRef) {
        playerName.put(botRef, name);
        playerPosition.put(botRef, start);
    }

    @Override
    public boolean playChosenDirection(Direction direction, ActorRef<Message> botRef) {

        return false;
    }

    @Override
    public List<Direction> getAvailableDirection(ActorRef<Message> botRef) {
        List<Direction> directions = new ArrayList<>();

        return directions;
    }



    @Override
    public boolean checkIfBotFinished(ActorRef<Message> botRef) {
        if(playerPosition.get(botRef).equals(end)) {
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean isMoveValid(ActorRef<Message> botRef, Direction direction){
        // no check needed for no move
        if(direction == Direction.X){
            return true;
        }

        Position pos = playerPosition.get(botRef);

        switch(direction) {

        }



        return false;
    }


    // Inner class for position
    private static class Position {
        int row, col;
        Position(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public int getCol() {
            return col;
        }
        public int getRow() {
            return row;
        }
    }


}
