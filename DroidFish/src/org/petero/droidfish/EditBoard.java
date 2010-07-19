package org.petero.droidfish;

import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class EditBoard extends Activity {
	// FIXME!!! Requires tall screen.
	private ChessBoardEdit cb;
	private TextView status;
	private Button okButton;
	private Button cancelButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initUI();
		
		Intent i = getIntent();
		Position pos;
		try {
			pos = TextIO.readFEN(i.getAction());
			cb.setPosition(pos);
		} catch (ChessParseError e) {
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ChessBoardEdit oldCB = cb;
		String statusStr = status.getText().toString();
        initUI();
        cb.cursorX = oldCB.cursorX;
        cb.cursorY = oldCB.cursorY;
        cb.cursorVisible = oldCB.cursorVisible;
        cb.setPosition(oldCB.pos);
        cb.setSelection(oldCB.selectedSquare);
        status.setText(statusStr);
	}

	private final void initUI() {
		setContentView(R.layout.editboard);
		cb = (ChessBoardEdit)findViewById(R.id.eb_chessboard);
        status = (TextView)findViewById(R.id.eb_status);
		okButton = (Button)findViewById(R.id.eb_ok);
		cancelButton = (Button)findViewById(R.id.eb_cancel);

		okButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				sendBackResult();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		status.setFocusable(false);
        cb.setFocusable(true);
        cb.requestFocus();
        cb.setClickable(true);
        cb.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
		        if (event.getAction() == MotionEvent.ACTION_UP) {
		            int sq = cb.eventToSquare(event);
		            Move m = cb.mousePressed(sq);
		            if (m != null) {
		                doMove(m);
		            }
		            return false;
		        }
		        return false;
			}
		});
        cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
        	public void onTrackballEvent(MotionEvent event) {
        		Move m = cb.handleTrackballEvent(event);
        		if (m != null) {
        			doMove(m);
        		}
        	}
        });
        cb.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				showDialog(EDIT_DIALOG);
				return true;
			}
		});
	}
	
	private void doMove(Move m) {
		if (m.to < 0) {
			if ((m.from < 0) || (cb.pos.getPiece(m.from) == Piece.EMPTY)) {
				cb.setSelection(m.to);
				return;
			}
		}
		Position pos = new Position(cb.pos);
		int piece = Piece.EMPTY;
		if (m.from >= 0) {
			piece = pos.getPiece(m.from);
		} else {
			piece = -(m.from + 2);
		}
		if (m.to >= 0)
			pos.setPiece(m.to, piece);
		if (m.from >= 0)
			pos.setPiece(m.from, Piece.EMPTY);
		cb.setPosition(pos);
		cb.setSelection(-1);
		checkValid();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			sendBackResult();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private final void sendBackResult() {
		if (checkValid()) {
			setPosFields();
			String fen = TextIO.toFEN(cb.pos);
			setResult(RESULT_OK, (new Intent()).setAction(fen));
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
	}

	private final void setPosFields() {
		setEPFile(getEPFile()); // To handle sideToMove change
		TextIO.fixupEPSquare(cb.pos);
		TextIO.removeBogusCastleFlags(cb.pos);
	}
	
	private final int getEPFile() {
		int epSquare = cb.pos.getEpSquare();
		if (epSquare < 0) return 8;
		return Position.getX(epSquare);
	}
	
	private final void setEPFile(int epFile) {
		int epSquare = -1;
		if ((epFile >= 0) && (epFile < 8)) {
			int epRank = cb.pos.whiteMove ? 5 : 2;
			epSquare = Position.getSquare(epFile, epRank);
		}
		cb.pos.setEpSquare(epSquare);
	}

	/** Test if a position is valid. */
	private final boolean checkValid() {
		try {
			String fen = TextIO.toFEN(cb.pos);
			TextIO.readFEN(fen);
			status.setText("");
			return true;
		} catch (ChessParseError e) {
			status.setText(e.getMessage());
		}
		return false;
	}

	static final int EDIT_DIALOG = 0; 
	static final int SIDE_DIALOG = 1; 
	static final int CASTLE_DIALOG = 2; 
	static final int EP_DIALOG = 3;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case EDIT_DIALOG: {
			final CharSequence[] items = {
					"Side to Move",
					"Clear Board", "Initial position", 
					"Castling Flags", "En Passant File", "Move Counters",
					"Copy Position", "Paste Position"
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Edit Board");
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	switch (item) {
			    	case 0: // Edit side to move
			    		showDialog(SIDE_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 1: { // Clear board
			    		Position pos = new Position();
			    		cb.setPosition(pos);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	}
			    	case 2: { // Set initial position
			    		try {
			    			Position pos = TextIO.readFEN(TextIO.startPosFEN);
			    			cb.setPosition(pos);
			    			cb.setSelection(-1);
			    			checkValid();
			    		} catch (ChessParseError e) {
			    		}
			    		break;
			    	}
			    	case 3: // Edit castling flags
			    		showDialog(CASTLE_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 4: // Edit en passant file
			    		showDialog(EP_DIALOG);
			    		cb.setSelection(-1);
			    		checkValid();
			    		break;
			    	case 5: // Edit move counters
			    		break;
			    	case 6: { // Copy position
						setPosFields();
			    		String fen = TextIO.toFEN(cb.pos) + "\n";
			    		ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			    		clipboard.setText(fen);
			    		cb.setSelection(-1);
			    		break;
			    	}
			    	case 7: { // Paste position
			    		ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
			    		if (clipboard.hasText()) {
			    			String fen = clipboard.getText().toString();
			    			try {
			    				Position pos = TextIO.readFEN(fen);
			    				cb.setPosition(pos);
			    			} catch (ChessParseError e) {
			    				if (e.pos != null)
					    			cb.setPosition(e.pos);
								Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
			    			}
			    			cb.setSelection(-1);
			    			checkValid();
			    		}
			    		break;
			    	}
			    	}
			    }
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		case SIDE_DIALOG: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Select side to move first")
			       .setPositiveButton("White", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   cb.pos.setWhiteMove(true);
			        	   checkValid();
			        	   dialog.cancel();
			           }
			       })
			       .setNegativeButton("Black", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   cb.pos.setWhiteMove(false);
			        	   checkValid();
			        	   dialog.cancel();
			           }
			       });
			AlertDialog alert = builder.create();
			return alert;
		}
		case CASTLE_DIALOG: {
			final CharSequence[] items = {
					"White king castle", "White queen castle",
					"Black king castle", "Black queen castle"
			};
			boolean[] checkedItems = {
					cb.pos.h1Castle(), cb.pos.a1Castle(), // FIXME!!! Init every time
					cb.pos.h8Castle(), cb.pos.a8Castle()
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Castling Flags");
			builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					Position pos = new Position(cb.pos);
					boolean a1Castle = pos.a1Castle();
					boolean h1Castle = pos.h1Castle();
					boolean a8Castle = pos.a8Castle();
					boolean h8Castle = pos.h8Castle();
					switch (which) {
					case 0: h1Castle = isChecked; break;
					case 1: a1Castle = isChecked; break;
					case 2: h8Castle = isChecked; break;
					case 3: a8Castle = isChecked; break;
					}
					int castleMask = 0;
					if (a1Castle) castleMask |= 1 << Position.A1_CASTLE;
					if (h1Castle) castleMask |= 1 << Position.H1_CASTLE;
					if (a8Castle) castleMask |= 1 << Position.A8_CASTLE;
					if (h8Castle) castleMask |= 1 << Position.H8_CASTLE;
					pos.setCastleMask(castleMask);
					cb.setPosition(pos);
					checkValid();
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		case EP_DIALOG: { // FIXME!!! Init every time
			final CharSequence[] items = {
					"A", "B", "C", "D", "E", "F", "G", "H", "None"
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Select En Passant File");
			builder.setSingleChoiceItems(items, getEPFile(), new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
			    	setEPFile(item);
			    }
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		}
		return null;
	}
}
