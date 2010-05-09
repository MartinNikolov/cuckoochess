package org.petero.cuckoochess;

import chess.ChessParseError;
import chess.Position;
import chess.TextIO;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class CuckooChess extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        try {
			Position pos = TextIO.readFEN(TextIO.startPosFEN);
			String s = TextIO.asciiBoard(pos);
			TextView tv = (TextView)this.findViewById(R.id.textview);
			tv.setText(s);
		} catch (ChessParseError e) {
			e.printStackTrace();
		}
    }
}