/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.petero.droidfish.gamelogic;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class GameTest {

    public GameTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of haveDrawOffer method, of class Game.
     */
    @Test
    public void testHaveDrawOffer() {
        System.out.println("haveDrawOffer");
        Game game = new Game(null);
        assertEquals(false, game.haveDrawOffer());

        boolean res = game.processString("e4");
        assertEquals(true, res);
        assertEquals(false, game.haveDrawOffer());

        res = game.processString("draw offer e5");
        assertEquals(true, res);
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Draw offer does not imply draw
        assertEquals(Piece.BPAWN, game.pos.getPiece(Position.getSquare(4, 4))); // e5 move made

        res = game.processString("draw offer Nf3");
        assertEquals(true, res);
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Draw offer does not imply draw
        assertEquals(Piece.WKNIGHT, game.pos.getPiece(Position.getSquare(5, 2))); // Nf3 move made

        res = game.processString("Nc6");
        assertEquals(true, res);
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        assertEquals(Piece.BKNIGHT, game.pos.getPiece(Position.getSquare(2, 5))); // Nc6 move made
        
        res = game.processString("draw offer Bb5");
        assertEquals(true, res);
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        assertEquals(Piece.WBISHOP, game.pos.getPiece(Position.getSquare(1, 4))); // Bb5 move made
        
        res = game.processString("draw accept");
        assertEquals(true, res);
        assertEquals(Game.GameState.DRAW_AGREE, game.getGameState());    // Draw by agreement

        res = game.processString("undo");
        assertEquals(true, res);
        assertEquals(Piece.EMPTY, game.pos.getPiece(Position.getSquare(1, 4))); // Bb5 move undone
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        res = game.processString("undo");
        assertEquals(true, res);
        assertEquals(Piece.EMPTY, game.pos.getPiece(Position.getSquare(2, 5))); // Nc6 move undone
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        
        res = game.processString("redo");
        assertEquals(true, res);
        assertEquals(Piece.BKNIGHT, game.pos.getPiece(Position.getSquare(2, 5))); // Nc6 move redone
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        res = game.processString("redo");
        assertEquals(true, res);
        assertEquals(Piece.WBISHOP, game.pos.getPiece(Position.getSquare(1, 4))); // Bb5 move redone
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        res = game.processString("redo");
        assertEquals(true, res);
        assertEquals(true, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Can't redo draw accept
        
        // Test draw offer in connection with invalid move
        res = game.processString("new");
        assertEquals(true, res);
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        
        res = game.processString("draw offer e5");
        assertEquals(true, res);
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.pos));   // Move invalid, not executed
        res = game.processString("e4");
        assertEquals(true, res);
        assertEquals(true, game.haveDrawOffer());   // Previous draw offer still valid
        assertEquals(Piece.WPAWN, game.pos.getPiece(Position.getSquare(4, 3))); // e4 move made

        // Undo/redo shall clear "pendingDrawOffer".
        game.processString("new");
        game.processString("e4");
        game.processString("draw offer e4");       // Invalid black move
        assertEquals(true, game.pendingDrawOffer);
        game.processString("undo");
        game.processString("redo");
        game.processString("e5");
        assertEquals(true,game.pos.whiteMove);
        assertEquals(false, game.haveDrawOffer());
    }
    
    /**
     * Test of draw by 50 move rule, of class Game.
     */
    @Test
    public void testDraw50() {
        System.out.println("draw50");
        Game game = new Game(null);
        assertEquals(false, game.haveDrawOffer());
        boolean res = game.processString("draw 50");
        assertEquals(true, res);
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Draw claim invalid
        res = game.processString("e4");
        assertEquals(true, game.haveDrawOffer());   // Invalid claim converted to draw offer
        
        String cmd = "setpos 8/4k3/8/P7/8/8/8/1N2K2R w K - 99 83";
        res = game.processString(cmd);
        assertEquals(true, res);
        res = game.processString("draw 50");
        assertEquals(Game.GameState.ALIVE, game.getGameState());      // Draw claim invalid

        game.processString(cmd);
        game.processString("draw 50 Nc3");
        assertEquals(Game.GameState.DRAW_50, game.getGameState());    // Draw claim valid
        assertEquals("Game over, draw by 50 move rule! [Nc3]", game.getGameStateString());

        game.processString(cmd);
        game.processString("draw 50 a6");
        assertEquals(Game.GameState.ALIVE, game.getGameState());      // Pawn move resets counter
        assertEquals(Piece.WPAWN, game.pos.getPiece(Position.getSquare(0, 5))); // Move a6 made
        
        game.processString(cmd);
        game.processString("draw 50 O-O");
        assertEquals(Game.GameState.DRAW_50, game.getGameState());    // Castling doesn't reset counter
        
        game.processString(cmd);
        game.processString("draw 50 Kf2");
        assertEquals(Game.GameState.DRAW_50, game.getGameState());    // Loss of castling right doesn't reset counter
        
        game.processString(cmd);
        game.processString("draw 50 Ke3");
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Ke3 is invalid
        assertEquals(true,game.pos.whiteMove);
        game.processString("a6");
        assertEquals(true, game.haveDrawOffer());   // Previous invalid claim converted to offer
        game.processString("draw 50");
        assertEquals(Game.GameState.ALIVE, game.getGameState());  // 50 move counter reset.
        res = game.processString("draw accept");
        assertEquals(true, res);
        assertEquals(Game.GameState.DRAW_AGREE, game.getGameState()); // Can accept previous implicit offer
        
        cmd = "setpos 3k4/R7/3K4/8/8/8/8/8 w - - 99 78";
        game.processString(cmd);
        game.processString("Ra8");
        assertEquals(Game.GameState.WHITE_MATE, game.getGameState());
        game.processString("draw 50");
        assertEquals(Game.GameState.WHITE_MATE, game.getGameState()); // Can't claim draw when game over
        assertEquals(Game.GameState.ALIVE, game.drawState);
    }

    /**
     * Test of draw by repetition, of class Game.
     */
    @Test
    public void testDrawRep() {
        System.out.println("drawRep");
        Game game = new Game(null);
        assertEquals(false, game.haveDrawOffer());
        game.processString("Nc3");
        game.processString("Nc6");
        game.processString("Nb1");
        game.processString("Nb8");
        game.processString("Nf3");
        game.processString("Nf6");
        game.processString("Ng1");
        assertEquals(false, game.haveDrawOffer());
        game.processString("draw rep");
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Claim not valid, one more move needed
        game.processString("draw rep Nc6");
        assertEquals(Game.GameState.ALIVE, game.getGameState());    // Claim not valid, wrong move claimed
        assertEquals(Piece.BKNIGHT, game.pos.getPiece(Position.getSquare(2, 5)));   // Move Nc6 made
        assertEquals(true, game.haveDrawOffer());
        game.processString("undo");
        assertEquals(false, game.haveDrawOffer());
        assertEquals(Piece.EMPTY, game.pos.getPiece(Position.getSquare(2, 5)));
        game.processString("draw rep Ng8");
        assertEquals(Game.GameState.DRAW_REP, game.getGameState());
        assertEquals(Piece.EMPTY, game.pos.getPiece(Position.getSquare(6, 7))); // Ng8 not played
        
        // Test draw by repetition when a "potential ep square but not real ep square" position is present.
        game.processString("new");
        game.processString("e4");   // e3 is not a real epSquare here
        game.processString("Nf6");
        game.processString("Nf3");
        game.processString("Ng8");
        game.processString("Ng1");
        game.processString("Nf6");
        game.processString("Nf3");
        game.processString("Ng8");
        game.processString("draw rep Ng1");
        assertEquals(Game.GameState.DRAW_REP, game.getGameState());

        // Now check the case when e3 *is* an epSquare
        game.processString("new");
        game.processString("Nf3");
        game.processString("d5");
        game.processString("Ng1");
        game.processString("d4");
        game.processString("e4");   // Here e3 is a real epSquare
        game.processString("Nf6");
        game.processString("Nf3");
        game.processString("Ng8");
        game.processString("Ng1");
        game.processString("Nf6");
        game.processString("Nf3");
        game.processString("Ng8");
        game.processString("draw rep Ng1");
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        
        // EP capture not valid because it would leave the king in check. Therefore
        // the position has been repeated three times at the end of the move sequence.
        game.processString("setpos 4k2n/8/8/8/4p3/8/3P4/3KR2N w - - 0 1");
        game.processString("d4");
        game.processString("Ng6");
        game.processString("Ng3");
        game.processString("Nh8");
        game.processString("Nh1");
        game.processString("Ng6");
        game.processString("Ng3");
        game.processString("Nh8");
        game.processString("draw rep Nh1");
//        assertEquals(Game.GameState.DRAW_REP, game.getGameState()); // FIXME!!! Test fails
    }

    /**
     * Test of resign command, of class Game.
     */
    @Test
    public void testResign() {
        System.out.println("resign");
        Game game = new Game(null);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("f3");
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("resign");
        assertEquals(Game.GameState.RESIGN_BLACK, game.getGameState());
        game.processString("undo");
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("f3");
        game.processString("e5");
        game.processString("resign");
        assertEquals(Game.GameState.RESIGN_WHITE, game.getGameState());
        game.processString("undo");
        game.processString("e5");
        game.processString("g4");
        game.processString("Qh4");
        assertEquals(Game.GameState.BLACK_MATE, game.getGameState());
        game.processString("resign");
        assertEquals(Game.GameState.BLACK_MATE, game.getGameState());   // Can't resign after game over
    }
    
    /**
     * Test of processString method, of class Game.
     */
    @Test
    public void testProcessString() throws ChessParseError {
        System.out.println("processString");
        Game game = new Game(null);
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.pos));
        boolean res = game.processString("Nf3");
        assertEquals(true, res);
        assertEquals(1, game.pos.halfMoveClock);
        assertEquals(1, game.pos.fullMoveCounter);
        res = game.processString("d5");
        assertEquals(true, res);
        assertEquals(0, game.pos.halfMoveClock);
        assertEquals(2, game.pos.fullMoveCounter);

        res = game.processString("undo");
        assertEquals(true, res);
        assertEquals(1, game.pos.halfMoveClock);
        assertEquals(1, game.pos.fullMoveCounter);
        res = game.processString("undo");
        assertEquals(true, res);
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.pos));
        res = game.processString("undo");
        assertEquals(true, res);
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.pos));

        res = game.processString("redo");
        assertEquals(true, res);
        assertEquals(1, game.pos.halfMoveClock);
        assertEquals(1, game.pos.fullMoveCounter);
        res = game.processString("redo");
        assertEquals(true, res);
        assertEquals(0, game.pos.halfMoveClock);
        assertEquals(2, game.pos.fullMoveCounter);
        res = game.processString("redo");
        assertEquals(true, res);
        assertEquals(0, game.pos.halfMoveClock);
        assertEquals(2, game.pos.fullMoveCounter);

        res = game.processString("new");
        assertEquals(true, res);
        assertEquals(TextIO.startPosFEN, TextIO.toFEN(game.pos));
        
        String fen = "8/8/8/4k3/8/8/2p5/5K2 b - - 47 68";
        Position pos = TextIO.readFEN(fen);
        res = game.processString("setpos " + fen);
        assertEquals(true, res);
        assertEquals(pos, game.pos);
        
        res = game.processString("junk");
        assertEquals(false, res);
    }

    /**
     * Test of getGameState method, of class Game.
     */
    @Test
    public void testGetGameState() {
        System.out.println("getGameState");
        Game game = new Game(null);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("f3");
        game.processString("e5");
        game.processString("g4");
        game.processString("Qh4");
        assertEquals(Game.GameState.BLACK_MATE, game.getGameState());
        
        game.processString("setpos 5k2/5P2/5K2/8/8/8/8/8 b - - 0 1");
        assertEquals(Game.GameState.BLACK_STALEMATE, game.getGameState());
    }

    /**
     * Test of insufficientMaterial method, of class Game.
     */
    @Test
    public void testInsufficientMaterial() {
        System.out.println("insufficientMaterial");
        Game game = new Game(null);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.processString("setpos 4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());
        final int a1 = Position.getSquare(0, 0);
        game.pos.setPiece(a1, Piece.WROOK);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.pos.setPiece(a1, Piece.BQUEEN);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.pos.setPiece(a1, Piece.WPAWN);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.pos.setPiece(a1, Piece.BKNIGHT);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());
        game.pos.setPiece(a1, Piece.WBISHOP);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());

        final int c1 = Position.getSquare(2, 0);
        game.pos.setPiece(c1, Piece.WKNIGHT);
        assertEquals(Game.GameState.ALIVE, game.getGameState());
        game.pos.setPiece(c1, Piece.BBISHOP);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());
        game.pos.setPiece(c1, Piece.WBISHOP);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());

        final int b2 = Position.getSquare(1, 1);
        game.pos.setPiece(b2, Piece.WBISHOP);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());
        game.pos.setPiece(b2, Piece.BBISHOP);
        assertEquals(Game.GameState.DRAW_NO_MATE, game.getGameState());

        final int b3 = Position.getSquare(1, 2);
        game.pos.setPiece(b3, Piece.WBISHOP);
        assertEquals(Game.GameState.ALIVE, game.getGameState());

        // Can't force mate with KNNK, but still not an automatic draw.
        game.processString("setpos 8/8/8/8/8/8/8/K3nnk1 w - - 0 1");
        assertEquals(Game.GameState.ALIVE, game.getGameState());
    }
}