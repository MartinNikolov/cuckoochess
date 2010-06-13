package org.petero.cuckoochess;

import java.util.ArrayList;
import java.util.List;

import guibase.ChessController;
import guibase.GUIInterface;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ScrollView;
import android.widget.TextView;
import chess.Move;
import chess.Position;

public class CuckooChess extends Activity implements GUIInterface {
	ChessBoard cb;
	ChessController ctrl;
	boolean mShowThinking;
	int mTimeLimit;
	boolean playerWhite;
	static final int ttLogSize = 16; // Use 2^ttLogSize hash entries.
	
	TextView status;
	ScrollView moveListScroll;
	TextView moveList;
	TextView thinking;
	
	SharedPreferences settings;

	private void readPrefs() {
        mShowThinking = settings.getBoolean("showThinking", false);
        mTimeLimit = settings.getInt("timeLimit", 5000);
        playerWhite = settings.getBoolean("playerWhite", true);
        boolean boardFlipped = settings.getBoolean("boardFlipped", false);
        cb.setFlipped(boardFlipped);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				readPrefs();
			}
		});
        
        setContentView(R.layout.main);
        status = (TextView)findViewById(R.id.status);
        moveListScroll = (ScrollView)findViewById(R.id.scrollView);
        moveList = (TextView)findViewById(R.id.moveList);
        thinking = (TextView)findViewById(R.id.thinking);
		cb = (ChessBoard)findViewById(R.id.chessboard);
        ctrl = new ChessController(this);
        readPrefs();
        
        Typeface chessFont = Typeface.createFromAsset(getAssets(), "casefont.ttf");
        cb.setFont(chessFont);
        cb.setFocusable(true);
        cb.requestFocus();
        cb.setClickable(true);
        
        ctrl.newGame(playerWhite, ttLogSize, false);
        if (savedInstanceState != null) {
        	String fen = savedInstanceState.getString("startFEN");
        	if (fen == null) {
        		fen = "";
        	}
        	String moves = savedInstanceState.getString("moves");
        	if (moves == null) {
        		moves = "";
        	}
        	List<String> posHistStr = new ArrayList<String>();
        	posHistStr.add(fen);
        	posHistStr.add(moves);
        	ctrl.setPosHistory(posHistStr);
        }
        
        cb.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
//				System.out.printf("%d %g %g%n", event.getAction(), event.getX(), event.getY());
		        if (ctrl.humansTurn() && (event.getAction() == MotionEvent.ACTION_DOWN)) {
		            int sq = cb.eventToSquare(event);
		            Move m = cb.mousePressed(sq);
		            if (m != null) {
		                ctrl.humanMove(m);
		            }
		            return true;
		        }
		        return false;
			}
		});
        
        cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
        	public void onTrackballEvent(MotionEvent event) {
		        if (ctrl.humansTurn()) {
		        	Move m = cb.handleTrackballEvent(event);
		        	if (m != null) {
		        		ctrl.humanMove(m);
		        	}
		        }
        	}
        });
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		List<String> posHistStr = ctrl.getPosHistory();
		outState.putString("startFEN", posHistStr.get(0));
		outState.putString("moves", posHistStr.get(1));
	}
	
	@Override
	protected void onDestroy() {
		ctrl.newGame(true, ttLogSize, false);
		super.onDestroy();
	}

	static final int MENU_NEW_GAME = 0;
	static final int MENU_QUIT = 1;
	static final int MENU_UNDO = 2;
	static final int MENU_REDO = 3;
	static final int MENU_PREFS = 4;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// FIXME!!! Define menu in xml file.
		menu.add(0, MENU_NEW_GAME, 0, "New Game");
		menu.add(0, MENU_QUIT, 0, "Quit");
		menu.add(0, MENU_UNDO, 0, "Undo");
		menu.add(0, MENU_REDO, 0, "Redo");
		menu.add(0, MENU_PREFS, 0, "Settings");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_NEW_GAME:
	        ctrl.newGame(playerWhite, ttLogSize, false);
			return true;
		case MENU_QUIT:
			finish();
			return true;
		case MENU_UNDO:
			ctrl.takeBackMove();
			return true;
		case MENU_REDO:
			ctrl.redoMove();
			return true;
		case MENU_PREFS:
		{
			Intent i = new Intent(CuckooChess.this, Preferences.class);
			startActivityForResult(i, 0);
			return true;
		}
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			readPrefs();
		}
	}

	// FIXME!!! Settings menu: thinking time
	// FIXME!!! Redo history is lost when flipping phone.
	// FIXME!!! Implement "switch sides".
	// FIXME!!! Implement "edit board" (And/or copy/paste FEN)
	// FIXME!!! Show "Thinking..." string when computer is thinking.
    
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
		moveListScroll.fullScroll(ScrollView.FOCUS_DOWN);
	}
	
	@Override
	public void setThinkingString(String str) {
		thinking.setText(str);
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