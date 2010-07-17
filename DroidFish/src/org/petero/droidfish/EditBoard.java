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

public class EditBoard extends Activity {
	private ChessBoard cb;
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
		ChessBoard oldCB = cb;
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
		cb = (ChessBoard)findViewById(R.id.eb_chessboard);
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
        cb.setEditMode(true);
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
		Position pos = new Position(cb.pos);
		int piece = Piece.EMPTY;
		if (m.from >= 0) {
			piece = pos.getPiece(m.from);
		} else {
			piece = -(m.from + 2);
		}
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
			String fen = TextIO.toFEN(cb.pos);
			setResult(RESULT_OK, (new Intent()).setAction(fen));
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
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
	static final int FIELDS_DIALOG = 1; 

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case EDIT_DIALOG: {
			final CharSequence[] items = {
					"White King", "White Queen", "White Rook", "White Bishop", "White Knight", "White Pawn",
					"Black King", "Black Queen", "Black Rook", "Black Bishop", "Black Knight", "Black Pawn",
					"Empty square",
					"Clear Board", "Initial position", "Edit fields","Copy Position", "Paste Position"
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Edit Board");
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
					try {
						switch (item) {
						case 0:  addPiece(Piece.WKING);   break;
						case 1:  addPiece(Piece.WQUEEN);  break;
						case 2:  addPiece(Piece.WROOK);   break;
						case 3:  addPiece(Piece.WBISHOP); break;
						case 4:  addPiece(Piece.WKNIGHT); break;
						case 5:  addPiece(Piece.WPAWN);   break;
						case 6:  addPiece(Piece.BKING);   break;
						case 7:  addPiece(Piece.BQUEEN);  break;
						case 8:  addPiece(Piece.BROOK);   break;
						case 9:  addPiece(Piece.BBISHOP); break;
						case 10: addPiece(Piece.BKNIGHT); break;
						case 11: addPiece(Piece.BPAWN);   break;
						case 12: addPiece(Piece.EMPTY);   break;
						case 13: { // Clear board
							Position pos = new Position();
							cb.setPosition(pos);
							cb.setSelection(-1);
							checkValid();
							break;
						}
						case 14: { // Set initial position
							Position pos = TextIO.readFEN(TextIO.startPosFEN);
							cb.setPosition(pos);
							cb.setSelection(-1);
							checkValid();
							break;
						}
						case 15: // Edit castling flags, en passant and move counters
							showDialog(FIELDS_DIALOG);
							checkValid();
							break;
						case 16: { // Copy position
							String fen = TextIO.toFEN(cb.pos) + "\n";
							ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
							clipboard.setText(fen);
							break;
						}
						case 17: { // Paste position
							ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
							if (clipboard.hasText()) {
								String fen = clipboard.getText().toString();
								Position pos = TextIO.readFEN(fen);
								cb.setPosition(pos);
								checkValid();
							}
							break;
						}
						}
					} catch (ChessParseError e) {
					}
			    }
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		case FIELDS_DIALOG: {
			final CharSequence[] items = {
					"White to move", "White king castle", "White queen castle",
					"Black king castle", "Black queen castle"
			};
			boolean[] checkedItems = {
					cb.pos.whiteMove, cb.pos.h1Castle(), cb.pos.a1Castle(),
					cb.pos.h8Castle(), cb.pos.a8Castle()
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Edit Fields");
			builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					Position pos = new Position(cb.pos);
					boolean a1Castle = pos.a1Castle();
					boolean h1Castle = pos.h1Castle();
					boolean a8Castle = pos.a8Castle();
					boolean h8Castle = pos.h8Castle();
					switch (which) {
					case 0: pos.whiteMove = isChecked; break;
					case 1: h1Castle = isChecked; break;
					case 2: a1Castle = isChecked; break;
					case 3: h8Castle = isChecked; break;
					case 4: a8Castle = isChecked; break;
					}
					int castleMask = 0;
					if (a1Castle) castleMask |= 1 << Position.A1_CASTLE;
					if (h1Castle) castleMask |= 1 << Position.H1_CASTLE;
					if (a8Castle) castleMask |= 1 << Position.A8_CASTLE;
					if (h8Castle) castleMask |= 1 << Position.H8_CASTLE;
					pos.setCastleMask(castleMask);
					cb.setPosition(pos);
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		}
		return null;
	}

	private final void addPiece(int piece) {
		if (cb.selectedSquare >= 0) {
			Move m = new Move(-piece - 2, cb.selectedSquare, Piece.EMPTY);
			doMove(m);
		}
	}
}
