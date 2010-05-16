/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import java.util.ArrayList;
import java.util.List;

/**
 * A computer algorithm player.
 * @author petero
 */
public class ComputerPlayer implements Player {
    public static String engineName = "CuckooChess 1.04";

    int minTimeMillis;
    int maxTimeMillis;
    int maxDepth;
    int maxNodes;
    public boolean verbose;
    TranspositionTable tt;
    Book book;
    boolean bookEnabled;
    Search currentSearch;

    public ComputerPlayer() {
        minTimeMillis = 10000;
        maxTimeMillis = 10000;
        maxDepth = 100;
        maxNodes = -1;
        verbose = true;
        setTTLogSize(15);
        book = new Book(verbose);
        bookEnabled = true;
    }

	public void setTTLogSize(int logSize) {
		tt = new TranspositionTable(logSize);
	}
    
    Search.Listener listener;
    public void setListener(Search.Listener listener) {
        this.listener = listener;
    }

    @Override
    public String getCommand(Position pos, boolean drawOffer, List<Position> history) {
        // Create a search object
        ArrayList<Long> posHashList = new ArrayList<Long>();
        for (Position p : history) {
            posHashList.add(p.zobristHash());
        }
        tt.nextGeneration();
        Search sc = new Search(pos, posHashList, tt);

        // Determine all legal moves
        ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(pos);
        moves = MoveGen.removeIllegal(pos, moves);
        sc.scoreMoveList(moves, false, 0);

        // Test for "game over"
        if (moves.size() <= 0) {
            // Switch sides so that the human can decide what to do next.
            return "swap";
        }

        if (bookEnabled) {
            Move bookMove = book.getBookMove(pos);
            if (bookMove != null) {
                System.out.printf("Book moves: %s\n", book.getAllBookMoves(pos));
                return TextIO.moveToString(pos, bookMove, false);
            }
        }
        
        // Find best move using iterative deepening
        currentSearch = sc;
        sc.setListener(listener);
        Move bestM = sc.iterativeDeepening(moves, minTimeMillis, maxTimeMillis, maxDepth, maxNodes, verbose);
        currentSearch = null;
//        tt.printStats();
        String strMove = TextIO.moveToString(pos, bestM, false);

        // Claim draw if appropriate
        if (bestM.score <= 0) {
            if (sc.canClaimDraw50(pos)) {
                strMove = "draw 50";
            } else if (sc.canClaimDrawRep(pos, posHashList, posHashList.size())) {
                strMove = "draw rep";
            } else {
                posHashList.add(pos.zobristHash());
                UndoInfo ui = new UndoInfo();
                pos.makeMove(bestM, ui);
                if (sc.canClaimDraw50(pos)) {
                    strMove = "draw 50 " + strMove;
                } else if (sc.canClaimDrawRep(pos, posHashList, posHashList.size())) {
                    strMove = "draw rep " + strMove;
                }
            }
        }
        return strMove;
    }

    @Override
    public boolean isHumanPlayer() {
        return false;
    }

    @Override
    public void useBook(boolean bookOn) {
        bookEnabled = bookOn;
    }

    @Override
    public void timeLimit(int minTimeLimit, int maxTimeLimit) {
        minTimeMillis = minTimeLimit;
        maxTimeMillis = maxTimeLimit;
        if (currentSearch != null) {
            currentSearch.timeLimit(minTimeLimit, maxTimeLimit);
        }
    }

    @Override
    public void clearTT() {
        tt.clear();
    }

    /** Search a position and return the best move and score. Used for test suite processing. */
    public TwoReturnValues<Move, String> searchPosition(Position pos, int maxTimeMillis) {
        // Create a search object
        ArrayList<Long> posHashList = new ArrayList<Long>();
        tt.nextGeneration();
        Search sc = new Search(pos, posHashList, tt);
        
        // Determine all legal moves
        ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(pos);
        moves = MoveGen.removeIllegal(pos, moves);
        sc.scoreMoveList(moves, false, 0);

        // Find best move using iterative deepening
        Move bestM = sc.iterativeDeepening(moves, maxTimeMillis, maxTimeMillis, -1, -1, false);

        // Extract PV
        String PV = TextIO.moveToString(pos, bestM, false) + " ";
        UndoInfo ui = new UndoInfo();
        pos.makeMove(bestM, ui);
        PV += tt.extractPV(pos);
        pos.unMakeMove(bestM, ui);

//        tt.printStats();

        // Return best move and PV
        return new TwoReturnValues<Move, String>(bestM, PV);
    }


    // FIXME!!! Test LDS in quiesce (for checks and/or SEE<0 captures)
    // FIXME!!! Test Botvinnik-Markoff extension
    // FIXME!!! Implement pawn hash table
    // FIXME!!! Should a repeated position have a different hash key, to avoid repetition draw problems?
}
