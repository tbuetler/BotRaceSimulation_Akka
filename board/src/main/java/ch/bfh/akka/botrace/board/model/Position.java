package ch.bfh.akka.botrace.board.model;

public class Position {
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
