/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class EvaluateTest {

    public EvaluateTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of evalPos method, of class Evaluate.
     */
    @Test
    public void testEvalPos() throws ChessParseError {
        System.out.println("evalPos");
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        UndoInfo ui = new UndoInfo();
        pos.makeMove(TextIO.stringToMove(pos, "e4"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "e5"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "Nf3"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "Nc6"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "Bb5"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "Nge7"), ui);
        assertTrue(moveScore(pos, "O-O") > 0);      // Castling is good
        assertTrue(moveScore(pos, "Ke2") < 0);      // Losing right to castle is bad
        assertTrue(moveScore(pos, "Kf1") < 0);
        assertTrue(moveScore(pos, "Rg1") < 0);
        assertTrue(moveScore(pos, "Rf1") < 0);

        pos = TextIO.readFEN("8/8/8/1r3k2/4pP2/4P3/8/4K2R w K - 0 1");
        assertEquals(true, pos.h1Castle());
        int cs1 = evalWhite(pos);
        pos.setCastleMask(pos.getCastleMask() & ~(1 << Position.H1_CASTLE));
        assertEquals(false, pos.h1Castle());
        int cs2 = evalWhite(pos);
        assertTrue(cs2 >= cs1);    // No bonus for useless castle right

        // Test rook open file bonus
        pos = TextIO.readFEN("r4rk1/1pp1qppp/3b1n2/4p3/2B1P1b1/1QN2N2/PP3PPP/R3R1K1 w - - 0 1");
        int ms1 = moveScore(pos, "Red1");
        int ms2 = moveScore(pos, "Rec1");
        int ms3 = moveScore(pos, "Rac1");
        int ms4 = moveScore(pos, "Rad1");
        assertTrue(ms1 > 0);        // Good to have rook on open file
        assertTrue(ms2 > 0);        // Good to have rook on half-open file
        assertTrue(ms1 > ms2);      // Open file better than half-open file
        assertTrue(ms3 > 0);
        assertTrue(ms4 > 0);
        assertTrue(ms4 > ms1);
        assertTrue(ms3 > ms2);
        
        pos = TextIO.readFEN("r3kb1r/p3pp1p/bpPq1np1/4N3/2pP4/2N1PQ2/P1PB1PPP/R3K2R b KQkq - 0 12");
        assertTrue(moveScore(pos, "O-O-O") > 0);    // Black long castle is bad for black
        pos.makeMove(TextIO.stringToMove(pos, "O-O-O"), ui);
        assertTrue(moveScore(pos, "O-O") > 0);      // White short castle is good for white
        
        pos = TextIO.readFEN("8/3k4/2p5/1pp5/1P1P4/3K4/8/8 w - - 0 1");
        int sc1 = moveScore(pos, "bxc5");
        int sc2 = moveScore(pos, "dxc5");
        assertTrue(sc1 < sc2);      // Don't give opponent a passed pawn.
        
        pos = TextIO.readFEN("8/pp1bk3/8/8/8/8/PPPBK3/8 w - - 0 1");
        sc1 = evalWhite(pos);
        pos.setPiece(Position.getSquare(3, 1), Piece.EMPTY);
        pos.setPiece(Position.getSquare(3, 0), Piece.WBISHOP);
        sc2 = evalWhite(pos);
        assertTrue(sc2 > sc1);      // Easier to win if bishops on same color
        
        // Test bishop mobility
        pos = TextIO.readFEN("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3");
        sc1 = moveScore(pos, "Bd3");
        sc2 = moveScore(pos, "Bc4");
        assertTrue(sc2 > sc1);
    }

    /**
     * Test of pieceSquareEval method, of class Evaluate.
     */
    @Test
    public void testPieceSquareEval() throws ChessParseError {
        System.out.println("pieceSquareEval");
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        int score = evalWhite(pos);
        assertEquals(0, score);    // Should be zero, by symmetry
        UndoInfo ui = new UndoInfo();
        pos.makeMove(TextIO.stringToMove(pos, "e4"), ui);
        score = evalWhite(pos);
        System.out.printf("score:%d%n", score);
        assertTrue(score > 0);     // Centralizing a pawn is a good thing
        pos.makeMove(TextIO.stringToMove(pos, "e5"), ui);
        score = evalWhite(pos);
        assertEquals(0, score);    // Should be zero, by symmetry
        assertTrue(moveScore(pos, "Nf3") > 0);      // Developing knight is good        
        pos.makeMove(TextIO.stringToMove(pos, "Nf3"), ui);
        assertTrue(moveScore(pos, "Nc6") < 0);      // Developing knight is good        
        pos.makeMove(TextIO.stringToMove(pos, "Nc6"), ui);
        assertTrue(moveScore(pos, "Bb5") > 0);      // Developing bishop is good
        pos.makeMove(TextIO.stringToMove(pos, "Bb5"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "Nge7"), ui);
        assertTrue(moveScore(pos, "Qe2") > 0);      // Queen away from edge is good
        score = evalWhite(pos);
        pos.makeMove(TextIO.stringToMove(pos, "Bxc6"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "Nxc6"), ui);
        int score2 = evalWhite(pos);
        assertTrue(score2 < score);                 // Bishop worth more than knight in this case
        
        pos = TextIO.readFEN("5k2/1b2nppp/pqn5/1pp1p3/4P3/1BP1BN2/PP2QPPP/3R2K1 w - - 0 1");
        assertTrue(moveScore(pos, "Rd7") > 0);      // Rook on 7:th rank is good
        assertTrue(moveScore(pos, "Rd8") <= 0);     // Rook on 8:th rank not particularly good
        pos.setPiece(TextIO.getSquare("a1"), Piece.WROOK);
        assertTrue(moveScore(pos, "Rac1") > 0);     // Rook on c-f files considered good
    }

    /**
     * Test of tradeBonus method, of class Evaluate.
     */
    @Test
    public void testTradeBonus() throws ChessParseError {
        System.out.println("tradeBonus");
        String fen = "8/5k2/6r1/2p1p3/3p4/2P2N2/3PPP2/4K1R1 w - - 0 1";
        Position pos = TextIO.readFEN(fen);
        int score1 = evalWhite(pos);
        UndoInfo ui = new UndoInfo();
        pos.makeMove(TextIO.stringToMove(pos, "Rxg6"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "Kxg6"), ui);
        int score2 = evalWhite(pos);
        assertTrue(score2 > score1);    // White ahead, trading pieces is good
        
        pos = TextIO.readFEN(fen);
        pos.makeMove(TextIO.stringToMove(pos, "cxd4"), ui);
        pos.makeMove(TextIO.stringToMove(pos, "cxd4"), ui);
        score2 = evalWhite(pos);
        assertTrue(score2 < score1);    // White ahead, trading pawns is bad
    }

    /**
     * Test of material method, of class Evaluate.
     */
    @Test
    public void testMaterial() throws ChessParseError {
        System.out.println("material");
        Evaluate eval= new Evaluate();
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        assertEquals(0, eval.material(pos));
        
        final int p = Evaluate.pieceValue[Piece.WPAWN];
        final int q = Evaluate.pieceValue[Piece.WQUEEN];
        assertTrue(p != 0);
        assertTrue(q != 0);
        assertTrue(q > p);
        
        UndoInfo ui = new UndoInfo();
        pos.makeMove(TextIO.stringToMove(pos, "e4"), ui);
        assertEquals(0, eval.material(pos));
        pos.makeMove(TextIO.stringToMove(pos, "d5"), ui);
        assertEquals(0, eval.material(pos));
        pos.makeMove(TextIO.stringToMove(pos, "exd5"), ui);
        assertEquals(p, eval.material(pos));
        pos.makeMove(TextIO.stringToMove(pos, "Qxd5"), ui);
        assertEquals(0, eval.material(pos));
        pos.makeMove(TextIO.stringToMove(pos, "Nc3"), ui);
        assertEquals(0, eval.material(pos));
        pos.makeMove(TextIO.stringToMove(pos, "Qxd2"), ui);
        assertEquals(-p, eval.material(pos));
        pos.makeMove(TextIO.stringToMove(pos, "Qxd2"), ui);
        assertEquals(-p+q, eval.material(pos));
    }

    /**
     * Test of kingSafety method, of class Evaluate.
     */
    @Test
    public void testKingSafety() throws ChessParseError {
        System.out.println("kingSafety");
        Position pos = TextIO.readFEN("r3kb1r/p1p1pppp/b2q1n2/4N3/3P4/2N1PQ2/P2B1PPP/R3R1K1 w kq - 0 1");
        int s1 = evalWhite(pos);
        pos.setPiece(Position.getSquare(6,6), Piece.EMPTY);
        pos.setPiece(Position.getSquare(1,6), Piece.BPAWN);
        int s2 = evalWhite(pos);
        assertTrue(s2 < s1);    // Half-open g-file is bad for white

        // FIXME!!! Should not play Kf1: rnbqk1nr/pppp1ppp/8/8/1bBpP3/8/PPP2PPP/RNBQK1NR w KQkq - 2 4
    }

    /**
     * Test of endGameEval method, of class Evaluate.
     */
    @Test
    public void testEndGameEval() throws ChessParseError {
        System.out.println("endGameEval");
        Position pos = new Position();
        pos.setPiece(Position.getSquare(4, 1), Piece.WKING);
        pos.setPiece(Position.getSquare(4, 6), Piece.BKING);
        int score = evalWhite(pos);
        assertEquals(0, score);

        pos.setPiece(Position.getSquare(3, 1), Piece.WBISHOP);
        score = evalWhite(pos);
        assertTrue(Math.abs(score) < 50);   // Insufficient material to mate

        pos.setPiece(Position.getSquare(3, 1), Piece.WKNIGHT);
        score = evalWhite(pos);
        assertTrue(Math.abs(score) < 50);   // Insufficient material to mate

        pos.setPiece(Position.getSquare(3, 1), Piece.WROOK);
        score = evalWhite(pos);
        final int rV = Evaluate.pieceValue[Piece.WROOK];
        assertTrue(Math.abs(score) > rV + 100);   // Enough material to force mate
        
        pos.setPiece(Position.getSquare(3, 6), Piece.BBISHOP);
        score = evalWhite(pos);
        final int bV = Evaluate.pieceValue[Piece.WBISHOP];
        assertTrue(score >= 0);
        assertTrue(score < rV - bV);   // Insufficient excess material to mate
        
        pos.setPiece(Position.getSquare(5, 6), Piece.BROOK);
        score = evalWhite(pos);
        assertTrue(score <= 0);
        assertTrue(-score < bV);
        
        pos.setPiece(Position.getSquare(2, 6), Piece.BBISHOP);
        score = evalWhite(pos);
        assertTrue(-score > bV * 2 + 100);
        
        // KrpKn is win for white
        pos = TextIO.readFEN("8/3bk3/8/8/8/3P4/3RK3/8 w - - 0 1");
        score = evalWhite(pos);
        final int pV = Evaluate.pieceValue[Piece.WPAWN];
        assertTrue(score > rV + pV - bV - 100);
        
        // KNNK is a draw
        pos = TextIO.readFEN("8/8/4k3/8/8/3NK3/3N4/8 w - - 0 1");
        score = evalWhite(pos);
        assertTrue(Math.abs(score) < 50);
        
        pos = TextIO.readFEN("8/8/3k4/8/8/3NK3/2B5/8 b - - 0 1");
        score = evalWhite(pos);
        final int nV = Evaluate.pieceValue[Piece.WKNIGHT];
        assertTrue(score > bV + nV + 150);  // KBNK is won, should have a bonus
        score = moveScore(pos, "Kc6");
        assertTrue(score > 0);      // Black king going into wrong corner, good for white
        score = moveScore(pos, "Ke6");
        assertTrue(score < 0);      // Black king going away from wrong corner, good for black
        
        // KRN vs KR is generally drawn
        pos = TextIO.readFEN("rk/p/8/8/8/8/NKR/8 w - - 0 1");
        score = evalWhite(pos);
        assertTrue(score < nV - 2 * pV);
    }

    /** Return static evaluation score for white, regardless of whose turn it is to move. */
    private final int evalWhite(Position pos) {
        Evaluate eval = new Evaluate();
        int ret = eval.evalPos(pos);
        if (!pos.isWhiteMove()) {
            ret = -ret;
        }
        return ret;
    }

    /** Compute change in eval score for white after making "moveStr" in position "pos". */
    private final int moveScore(Position pos, String moveStr) {
        int score1 = evalWhite(pos);
        Position tmpPos = new Position(pos);
        UndoInfo ui = new UndoInfo();
        tmpPos.makeMove(TextIO.stringToMove(tmpPos, moveStr), ui);
        int score2 = evalWhite(tmpPos);
        return score2 - score1;
    }
}