package org.petero.droidfish;

import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Piece;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Chess board widget suitable for edit mode.
 * @author petero
 */
public class ChessBoardEdit extends ChessBoard {
	public ChessBoardEdit(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	Move mousePressed(int sq) {
		if (sq < 0)
			return null;
    	cursorVisible = false;

        if (selectedSquare >= 0) {
            if (sq != selectedSquare) {
            	Move m = new Move(selectedSquare, sq, Piece.EMPTY);
            	setSelection(sq);
            	return m;
            }
            setSelection(-1);
        } else {
        	setSelection(sq);
        }
        return null;
    }
}
