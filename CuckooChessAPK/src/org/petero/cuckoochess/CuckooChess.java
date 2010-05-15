package org.petero.cuckoochess;

import guibase.ChessController;
import guibase.GUIInterface;
import chess.Position;
import android.app.Activity;
import android.os.Bundle;
//import android.widget.EditText;
import android.widget.TextView;

public class CuckooChess extends Activity implements GUIInterface {
	ChessBoard cb;
	ChessController ctrl;
	boolean mShowThinking;
	int mTimeLimit;
	boolean playerWhite;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		TextView tv = (TextView)this.findViewById(R.id.textview);
        cb = new ChessBoard(tv);
        ctrl = new ChessController(this);
        mShowThinking = true;
        mTimeLimit = 5000;
        playerWhite = true;
        ctrl.newGame(playerWhite);
        
//		EditText cmd = (EditText)findViewById(R.id.cmd);
    }

	@Override
	public void setPosition(Position pos) {
		cb.setPosition(pos);
	}

	@Override
	public void setSelection(int sq) {
		cb.setSelection(sq);
	}

	@Override
	public void setStatusString(String str) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setMoveListString(String str) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int timeLimit() {
		return mTimeLimit;
	}

	@Override
	public boolean showThinking() {
		return mShowThinking;
	}


	@Override
	public int getPromotePiece() {
		return 0; // FIXME!!! Handle under-promotion. (How? No modal dialogs?)
	}

	@Override
	public void runOnUIThread(Runnable runnable) {
		runOnUiThread(runnable);
	}
}