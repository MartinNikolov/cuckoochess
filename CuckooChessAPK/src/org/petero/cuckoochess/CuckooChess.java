package org.petero.cuckoochess;

import guibase.ChessController;
import guibase.GUIInterface;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import chess.Position;

public class CuckooChess extends Activity implements GUIInterface {
	ChessBoard cb;
	ChessController ctrl;
	boolean mShowThinking;
	int mTimeLimit;
	boolean playerWhite;
	static final int ttLogSize = 15; // Use 2^15 hash entries.
	
	TextView status;
	TextView moveList;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        status = (TextView)findViewById(R.id.status);
        moveList = (TextView)findViewById(R.id.moveList);
		cb = (ChessBoard)findViewById(R.id.chessboard);
        ctrl = new ChessController(this);
        mShowThinking = true;
        mTimeLimit = 5000;
        playerWhite = false;
        
        ctrl.newGame(playerWhite, ttLogSize, false);

		final EditText cmd = (EditText)findViewById(R.id.cmd);
        cmd.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
					(keyCode == KeyEvent.KEYCODE_ENTER)) {
					String str = cmd.getText().toString();
					ctrl.processString(str);
					cmd.setText("");
					return true;
				}
				return false;
			}
        });
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
		status.setText(str);
	}

	@Override
	public void setMoveListString(String str) {
		moveList.setText(str);
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