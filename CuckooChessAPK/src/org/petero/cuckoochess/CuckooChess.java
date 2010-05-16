package org.petero.cuckoochess;

import guibase.ChessController;
import guibase.GUIInterface;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import chess.ChessParseError;
import chess.Move;
import chess.Position;
import chess.TextIO;

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
        playerWhite = true;
        
        Typeface chessFont = Typeface.createFromAsset(getAssets(), "casefont.ttf");
        cb.setFont(chessFont);
        
        ctrl.newGame(playerWhite, ttLogSize, false);
        if (savedInstanceState != null) {
        	String fen = savedInstanceState.getString("fen");
        	if (fen != null) {
        		try {
        			ctrl.setPosition(TextIO.readFEN(fen));
        		} catch (ChessParseError e) {
        			// Just ignore invalid state
        		}
        	}
        }
        
        cb.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
//				System.out.printf("%d %g %g%n", event.getAction(), event.getX(), event.getY());
		        if (ctrl.humansTurn()) {
		            int sq = cb.eventToSquare(event);
		            Move m = cb.mousePressed(sq);
		            if (m != null) {
		                ctrl.humanMove(m);
		            }
		        }
		        return false;
			}
		});
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		String fen = TextIO.toFEN(ctrl.game.pos);
		outState.putString("fen", fen);
	}
    
    // FIXME!!! Need to handle lifecycle events, so that game is not reset when turning phone
    // FIXME!!! Add a menu: Back, forward, new game, quit.
    // FIXME!!! Options menu: Flip board, play black, thinking time, show thinking.
    
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