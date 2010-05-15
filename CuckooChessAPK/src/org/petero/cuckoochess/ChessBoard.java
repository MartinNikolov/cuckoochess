package org.petero.cuckoochess;

import android.widget.TextView;
import chess.Position;
import chess.TextIO;

public class ChessBoard {
	TextView tv;
	private Position pos;
	private int selectedSquare;
	
	ChessBoard(TextView tv) {
		this.tv = tv;
		pos = new Position();
		selectedSquare = -1;
	}

	void setPosition(Position pos) {
		this.pos = pos;
		update();
	}

	void setSelection(int sq) {
		selectedSquare = sq;
		update();
	}
	
	private void update() {
		String s = TextIO.asciiBoard(pos);
		tv.setText(s);
	}
}
