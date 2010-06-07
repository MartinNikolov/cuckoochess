/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A player that reads input from the keyboard.
 * @author petero
 */
public class HumanPlayer implements Player {
    private String lastCmd = "";
    private BufferedReader in;

    public HumanPlayer() {
        in = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public String getCommand(Position pos, boolean drawOffer, List<Position> history) {
        try {
            ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(pos);
            moves = MoveGen.removeIllegal(pos, moves);
            String color = pos.whiteMove ? "white" : "black";
            System.out.print(String.format("Enter move (%s):", color));
            String moveStr = in.readLine();
            if (moveStr.length() == 0) {
                return lastCmd;
            } else {
                lastCmd = moveStr;
            }
            return moveStr;
        } catch (IOException ex) {
            return "quit";
        }
    }
    
    @Override
    public boolean isHumanPlayer() {
        return true;
    }
    
    @Override
    public void useBook(boolean bookOn) {
    }

    @Override
    public void timeLimit(int minTimeLimit, int maxTimeLimit) {
    }

    @Override
    public void clearTT() {
    }
}
