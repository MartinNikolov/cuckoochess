package guibase;

import static org.junit.Assert.*;

import org.junit.Test;

import chess.ChessParseError;
import chess.Piece;
import chess.TextIO;

public class ChessControllerTest {

	@Test
	public final void testSetPGN() throws ChessParseError {
		ChessController ctrl = new ChessController(null);
		ctrl.newGame(true, 8, false);
		ctrl.setPGN("[FEN \"k/8/8/8/8/8/KP/8 w\"]\n");
		assertEquals(TextIO.getSquare("a2"), ctrl.game.pos.getKingSq(true));
		assertEquals(TextIO.getSquare("a8"), ctrl.game.pos.getKingSq(false));
		
		ctrl.setPGN("1.e4 e5 2. Nf3!!! $6 Nc6?? Bb5!!? a6?! * Ba4");
		assertEquals(Piece.BPAWN, ctrl.game.pos.getPiece(TextIO.getSquare("a6")));
		assertEquals(Piece.WBISHOP, ctrl.game.pos.getPiece(TextIO.getSquare("b5")));
		assertEquals(Piece.EMPTY, ctrl.game.pos.getPiece(TextIO.getSquare("a4")));

		ctrl.setPGN("[FEN \"r1bq1rk1/pp3ppp/2n1pn2/6B1/1bBP4/2N2N2/PPQ2PPP/R3K2R w KQ - 1 10\"]\n");
		assertEquals(10, ctrl.game.pos.fullMoveCounter);
	}
}
