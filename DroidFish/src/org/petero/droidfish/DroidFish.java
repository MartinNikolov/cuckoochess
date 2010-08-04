package org.petero.droidfish;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.petero.droidfish.gamelogic.ChessController;
import org.petero.droidfish.gamelogic.ChessParseError;
import org.petero.droidfish.gamelogic.Move;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.gamelogic.TextIO;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class DroidFish extends Activity implements GUIInterface {
	// FIXME!!! Computer clock should stop if phone turned off (computer stops thinking if unplugged)
	// FIXME!!! book.txt (and test classes) should not be included in apk

	// FIXME!!! Current position in game should be visible: TextView.bringPointIntoView()

	// FIXME!!! PGN view option: Show comments
	// FIXME!!! PGN view option: Show NAGs
	// FIXME!!! PGN view option: Show variations (recursion depth?)
	// FIXME!!! PGN view option: game continuation (for training)
	// FIXME!!! PGN view option: Promote played variations to mainline (default true)
	// FIXME!!! Implement "revert to mainline": Go back, set default to follow mainline back/forward from point.
	// FIXME!!! Command to go to next/previous move in PGN export order.

	// FIXME!!! Handle more move formats in PGN import. 0-0, long form, extra characters in short form
	// FIXME!!! PGN standard says = sign shall be used in promotions, e8=Q
	// FIXME!!! Remove invalid playerActions in PGN import (should be done in verifyChildren)

	// FIXME!!! Implement "limit strength" option
	// FIXME!!! Implement PGN database support (and FEN?)
	// FIXME!!! Implement pondering (permanent brain)
	// FIXME!!! Implement multi-variation analysis mode
	// FIXME!!! Save analysis (analyze mode and computer thinking mode) as PGN comments and/or variation

	// FIXME!!! Add support all time controls defined by the PGN standard
	// FIXME!!! How to handle hour-glass time control?
	// FIXME!!! What should happen if you change time controls in the middle of a game?

	private ChessBoard cb;
	private ChessController ctrl = null;
	private boolean mShowThinking;
	private boolean mShowBookHints;
	private int maxNumArrows;
	private GameMode gameMode;
	private boolean boardFlipped;
	private boolean autoSwapSides;

	private TextView status;
	private ScrollView moveListScroll;
	private TextView moveList;
	private TextView thinking;
	private TextView whiteClock, blackClock;

	SharedPreferences settings;

	private float scrollSensitivity;
	private boolean soundEnabled;
	private MediaPlayer moveSound;

	private final String bookDir = "DroidFish";
	private String currentBookFile = "";
	private PGNOptions pgnOptions = new PGNOptions();

	private long lastVisibleMillis; // Time when GUI became invisible. 0 if currently visible.
	private long lastComputationMillis; // Time when engine last showed that it was computing.

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
				readPrefs();
				ctrl.setGameMode(gameMode);
			}
		});

        initUI(true);

        ctrl = new ChessController(this);
        ctrl.newGame(new GameMode(GameMode.TWO_PLAYERS));
        readPrefs();
        ctrl.newGame(gameMode);
        {
    		byte[] data = null;
        	if (savedInstanceState != null) {
        		data = savedInstanceState.getByteArray("gameState");
        	} else {
        		String dataStr = settings.getString("gameState", null);
        		if (dataStr != null)
        			data = strToByteArr(dataStr);
        	}
        	if (data != null)
        		ctrl.fromByteArray(data);
        }
    	ctrl.setGuiPaused(true);
    	ctrl.setGuiPaused(false);
        ctrl.startGame();
    }
    
    private final byte[] strToByteArr(String str) {
    	int nBytes = str.length() / 2;
    	byte[] ret = new byte[nBytes];
    	for (int i = 0; i < nBytes; i++) {
    		int c1 = str.charAt(i * 2) - 'A';
    		int c2 = str.charAt(i * 2 + 1) - 'A';
    		ret[i] = (byte)(c1 * 16 + c2);
    	}
    	return ret;
    }
    
    private final String byteArrToString(byte[] data) {
    	StringBuilder ret = new StringBuilder(32768);
    	int nBytes = data.length;
    	for (int i = 0; i < nBytes; i++) {
    		int b = data[i]; if (b < 0) b += 256;
    		char c1 = (char)('A' + (b / 16));
    		char c2 = (char)('A' + (b & 15));
    		ret.append(c1);
    		ret.append(c2);
    	}
    	return ret.toString();
    }
    
	private SpannableStringBuilder moveListStr = new SpannableStringBuilder();
	private boolean inMainLine = true;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ChessBoard oldCB = cb;
		String statusStr = status.getText().toString();
        initUI(false);
        readPrefs();
        cb.cursorX = oldCB.cursorX;
        cb.cursorY = oldCB.cursorY;
        cb.cursorVisible = oldCB.cursorVisible;
		cb.setPosition(oldCB.pos);
		cb.setFlipped(oldCB.flipped);
        setSelection(oldCB.selectedSquare);
        setStatusString(statusStr);
        setMoveListString(moveListStr, inMainLine);
		updateThinkingInfo();
	}

	private final void initUI(boolean initTitle) {
		if (initTitle)
			requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        if (initTitle) {
        	getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);
    		whiteClock = (TextView)findViewById(R.id.white_clock);
    		blackClock = (TextView)findViewById(R.id.black_clock);
        }
        status = (TextView)findViewById(R.id.status);
        moveListScroll = (ScrollView)findViewById(R.id.scrollView);
        moveList = (TextView)findViewById(R.id.moveList);
        thinking = (TextView)findViewById(R.id.thinking);
		status.setFocusable(false);
		moveListScroll.setFocusable(false);
		moveList.setFocusable(false);
		thinking.setFocusable(false);

		cb = (ChessBoard)findViewById(R.id.chessboard);
        cb.setFocusable(true);
        cb.requestFocus();
        cb.setClickable(true);
        
        final GestureDetector gd = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
        	private float scrollX = 0;
        	private float scrollY = 0;
        	public boolean onDown(MotionEvent e) {
        		scrollX = 0;
        		scrollY = 0;
        		return false;
        	}
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				cb.cancelLongPress();
				if (scrollSensitivity > 0) {
					scrollX += distanceX;
					scrollY += distanceY;
					float scrollUnit = cb.sqSize * scrollSensitivity;
					if (Math.abs(scrollX) >= Math.abs(scrollY)) {
						// Undo/redo
						int nRedo = 0, nUndo = 0;
						while (scrollX > scrollUnit) {
							nRedo++;
							scrollX -= scrollUnit;
						}
						while (scrollX < -scrollUnit) {
							nUndo++;
							scrollX += scrollUnit;
						}
						if (nUndo + nRedo > 0)
							scrollY = 0;
						if (nRedo + nUndo > 1) {
							boolean analysis = gameMode.analysisMode();
							boolean human = gameMode.playerWhite() || gameMode.playerBlack();
							if (analysis || !human)
								ctrl.setGameMode(new GameMode(GameMode.TWO_PLAYERS));
						}
						for (int i = 0; i < nRedo; i++) ctrl.redoMove();
						for (int i = 0; i < nUndo; i++) ctrl.undoMove();
						ctrl.setGameMode(gameMode);
					} else {
						// Next/previous variation
						int varDelta = 0;
						while (scrollY > scrollUnit) {
							varDelta++;
							scrollY -= scrollUnit;
						}
						while (scrollY < -scrollUnit) {
							varDelta--;
							scrollY += scrollUnit;
						}
						if (varDelta != 0)
							scrollX = 0;
						ctrl.changeVariation(varDelta);
					}
				}
				return true;
			}
			public boolean onSingleTapUp(MotionEvent e) {
				cb.cancelLongPress();
				handleClick(e);
	        	return true;
			}
			public boolean onDoubleTapEvent(MotionEvent e) {
				if (e.getAction() == MotionEvent.ACTION_UP)
					handleClick(e);
				return true;
			}
			private final void handleClick(MotionEvent e) {
		        if (ctrl.humansTurn()) {
		        	int sq = cb.eventToSquare(e);
		        	Move m = cb.mousePressed(sq);
		        	if (m != null)
		        		ctrl.makeHumanMove(m);
		        }
			}
        });
        cb.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				return gd.onTouchEvent(event);
			}
		});
        cb.setOnTrackballListener(new ChessBoard.OnTrackballListener() {
        	public void onTrackballEvent(MotionEvent event) {
		        if (ctrl.humansTurn()) {
		        	Move m = cb.handleTrackballEvent(event);
		        	if (m != null) {
		        		ctrl.makeHumanMove(m);
		        	}
		        }
        	}
        });
        cb.setOnLongClickListener(new OnLongClickListener() {
			public boolean onLongClick(View v) {
				removeDialog(CLIPBOARD_DIALOG);
				showDialog(CLIPBOARD_DIALOG);
				return true;
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (ctrl != null) {
			byte[] data = ctrl.toByteArray();
			outState.putByteArray("gameState", data);
		}
	}

	@Override
	protected void onResume() {
		lastVisibleMillis = 0;
		if (ctrl != null) {
			ctrl.setGuiPaused(false);
		}
		updateNotification();
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (ctrl != null) {
			ctrl.setGuiPaused(true);
			byte[] data = ctrl.toByteArray();
			Editor editor = settings.edit();
			String dataStr = byteArrToString(data);
			editor.putString("gameState", dataStr);
			editor.commit();
		}
		lastVisibleMillis = System.currentTimeMillis();
		updateNotification();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (ctrl != null) {
			ctrl.shutdownEngine();
		}
        setNotification(false);
		super.onDestroy();
	}

	private final void readPrefs() {
		String tmp = settings.getString("gameMode", "1");
        int modeNr = Integer.parseInt(tmp);
        gameMode = new GameMode(modeNr);
        boardFlipped = settings.getBoolean("boardFlipped", false);
        autoSwapSides = settings.getBoolean("autoSwapSides", false);
        setBoardFlip();

        mShowThinking = settings.getBoolean("showThinking", false);
        tmp = settings.getString("thinkingArrows", "2");
        maxNumArrows = Integer.parseInt(tmp);
        mShowBookHints = settings.getBoolean("bookHints", false);

        tmp = settings.getString("timeControl", "300000");
		int timeControl = Integer.parseInt(tmp);
		tmp = settings.getString("movesPerSession", "60");
		int movesPerSession = Integer.parseInt(tmp);
		tmp = settings.getString("timeIncrement", "0");
		int timeIncrement = Integer.parseInt(tmp);
        ctrl.setTimeLimit(timeControl, movesPerSession, timeIncrement);


        tmp = settings.getString("scrollSensitivity", "2");
        scrollSensitivity = Float.parseFloat(tmp);

        tmp = settings.getString("fontSize", "12");
        int fontSize = Integer.parseInt(tmp);
        status.setTextSize(fontSize);
        moveList.setTextSize(fontSize);
        thinking.setTextSize(fontSize);
        soundEnabled = settings.getBoolean("soundEnabled", false);

        String bookFile = settings.getString("bookFile", "");
        setBookFile(bookFile);
        ctrl.updateBookHints();
		updateThinkingInfo();

		pgnOptions.imp.variations   = settings.getBoolean("importVariations",   true);
		pgnOptions.imp.comments     = settings.getBoolean("importComments",     true);
		pgnOptions.imp.nag          = settings.getBoolean("importNAG", 		  	true);
		pgnOptions.exp.variations   = settings.getBoolean("exportVariations",   true);
		pgnOptions.exp.comments     = settings.getBoolean("exportComments",     true);
		pgnOptions.exp.nag          = settings.getBoolean("exportNAG",          true);
		pgnOptions.exp.playerAction = settings.getBoolean("exportPlayerAction", false);
		pgnOptions.exp.clockInfo    = settings.getBoolean("exportTime",         false);
	}

	private final void setBookFile(String bookFile) {
		currentBookFile = bookFile;
		if (bookFile.length() > 0) {
			File extDir = Environment.getExternalStorageDirectory();
			String sep = File.separator;
			bookFile = extDir.getAbsolutePath() + sep + bookDir + sep + bookFile;
		}
        ctrl.setBookFileName(bookFile);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		return true;
	}
	
	static private final int RESULT_EDITBOARD = 0;
	static private final int RESULT_SETTINGS = 1;
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_new_game:
			if (autoSwapSides && (gameMode.playerWhite() != gameMode.playerBlack())) {
				int gameModeType;
				if (gameMode.playerWhite()) {
					gameModeType = GameMode.PLAYER_BLACK;
				} else {
					gameModeType = GameMode.PLAYER_WHITE;
				}
				Editor editor = settings.edit();
				String gameModeStr = String.format("%d", gameModeType);
				editor.putString("gameMode", gameModeStr);
				editor.commit();
				gameMode = new GameMode(gameModeType);
			}
	        ctrl.newGame(gameMode);
	        ctrl.startGame();
			return true;
		case R.id.item_editboard: {
			Intent i = new Intent(DroidFish.this, EditBoard.class);
			i.setAction(ctrl.getFEN());
			startActivityForResult(i, RESULT_EDITBOARD);
			return true;
		}
		case R.id.item_settings: {
			Intent i = new Intent(DroidFish.this, Preferences.class);
			startActivityForResult(i, RESULT_SETTINGS);
			return true;
		}
		case R.id.item_undo:
			ctrl.undoMove();
			return true;
		case R.id.item_redo:
			ctrl.redoMove();
			return true;
		case R.id.item_goto_move: {
			showDialog(SELECT_MOVE_DIALOG);
			return true;
		}
		case R.id.item_force_move: {
			ctrl.stopSearch();
			return true;
		}
		case R.id.item_draw: {
			if (ctrl.humansTurn()) {
				if (!ctrl.claimDrawIfPossible()) {
					Toast.makeText(getApplicationContext(), R.string.offer_draw, Toast.LENGTH_SHORT).show();
				}
			}
			return true;
		}
		case R.id.item_resign: {
			if (ctrl.humansTurn()) {
				ctrl.resignGame();
			}
			return true;
		}
		case R.id.select_book:
			removeDialog(SELECT_BOOK_DIALOG);
			showDialog(SELECT_BOOK_DIALOG);
			return true;
		case R.id.item_about:
        	showDialog(ABOUT_DIALOG);
        	return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case RESULT_SETTINGS:
			readPrefs();
			ctrl.setGameMode(gameMode);
			break;
		case RESULT_EDITBOARD:
			if (resultCode == RESULT_OK) {
				try {
					String fen = data.getAction();
					ctrl.setFENOrPGN(fen, pgnOptions);
				} catch (ChessParseError e) {
				}
			}
			break;
		}
	}

	private final void setBoardFlip() {
		boolean flipped = boardFlipped;
		if (autoSwapSides) {
			if (gameMode.analysisMode()) {
				flipped = !cb.pos.whiteMove;
			} else if (gameMode.playerWhite() && gameMode.playerBlack()) {
				flipped = !cb.pos.whiteMove;
			} else if (gameMode.playerWhite()) {
				flipped = false;
			} else if (gameMode.playerBlack()) {
				flipped = true;
			} else { // two computers
				flipped = !cb.pos.whiteMove;
			}
		}
		cb.setFlipped(flipped);
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
	public void setMoveListString(SpannableStringBuilder str, boolean inMainLine) {
		moveListStr = str;
		this.inMainLine = inMainLine;
		moveList.setText(moveListStr);
		if (!ctrl.canRedoMove() && inMainLine)
			moveListScroll.fullScroll(ScrollView.FOCUS_DOWN);
	}

	@Override
	public void setPosition(Position pos, String variantInfo, List<Move> variantMoves) {
		variantStr = variantInfo;
		this.variantMoves = variantMoves;
		cb.setPosition(pos);
		setBoardFlip();
		updateThinkingInfo();
	}

	private String thinkingStr = "";
	private String bookInfoStr = "";
	private String variantStr = "";
	private List<Move> pvMoves = null;
	private List<Move> bookMoves = null;
	private List<Move> variantMoves = null;

	@Override
	public void setThinkingInfo(String pvStr, String bookInfo, List<Move> pvMoves, List<Move> bookMoves) {
		thinkingStr = pvStr;
		bookInfoStr = bookInfo;
		this.pvMoves = pvMoves;
		this.bookMoves = bookMoves;
		updateThinkingInfo();

		if (ctrl.computerBusy()) {
			lastComputationMillis = System.currentTimeMillis();
		} else {
			lastComputationMillis = 0;
		}
		updateNotification();
	}

	private final void updateThinkingInfo() {
		boolean thinkingEmpty = true;
		{
			String s = "";
			if (mShowThinking || gameMode.analysisMode()) {
				s = thinkingStr;
			}
			thinking.setText(s, TextView.BufferType.SPANNABLE);
			if (s.length() > 0) thinkingEmpty = false;
		}
		if (mShowBookHints && (bookInfoStr.length() > 0)) {
			String s = "";
			if (!thinkingEmpty)
				s += "<br>";
			s += "<b>Book:</b>" + bookInfoStr;
			thinking.append(Html.fromHtml(s));
			thinkingEmpty = false;
		}
		if (variantStr.indexOf(' ') >= 0) {
			String s = "";
			if (!thinkingEmpty)
				s += "<br>";
			s += "<b>Var:</b> " + variantStr;
			thinking.append(Html.fromHtml(s));
		}

		List<Move> hints = null;
		if (mShowThinking || gameMode.analysisMode())
			hints = pvMoves;
		if ((hints == null) && mShowBookHints)
			hints = bookMoves;
		if ((variantMoves != null) && variantMoves.size() > 1) {
			hints = variantMoves;
		}
		if ((hints != null) && (hints.size() > maxNumArrows)) {
			hints = hints.subList(0, maxNumArrows);
		}
		cb.setMoveHints(hints);
	}

	static final int PROMOTE_DIALOG = 0; 
	static final int CLIPBOARD_DIALOG = 1;
	static final int ABOUT_DIALOG = 2;
	static final int SELECT_MOVE_DIALOG = 3;
	static final int SELECT_BOOK_DIALOG = 4;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROMOTE_DIALOG: {
			final CharSequence[] items = {
				getString(R.string.queen), getString(R.string.rook),
				getString(R.string.bishop), getString(R.string.knight)
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.promote_pawn_to);
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
	        		ctrl.reportPromotePiece(item);
			    }
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		case CLIPBOARD_DIALOG: {
			List<CharSequence> lst = new ArrayList<CharSequence>();
			lst.add(getString(R.string.copy_game));
			lst.add(getString(R.string.copy_position));
			lst.add(getString(R.string.paste));
			if (ctrl.humansTurn() && (ctrl.numVariations() > 1)) {
				lst.add(getString(R.string.remove_variation));
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.tools_menu);
			builder.setItems(lst.toArray(new CharSequence[3]), new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
					switch (item) {
					case 0: {
						String pgn = ctrl.getPGN(pgnOptions);
						ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
						clipboard.setText(pgn);
						break;
					}
					case 1: {
						String fen = ctrl.getFEN() + "\n";
						ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
						clipboard.setText(fen);
						break;
					}
					case 2: {
						ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
						if (clipboard.hasText()) {
							String fenPgn = clipboard.getText().toString();
							try {
								ctrl.setFENOrPGN(fenPgn, pgnOptions);
							} catch (ChessParseError e) {
								Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
							}
						}
						break;
					}
					case 3:
						ctrl.removeVariation();
						break;
					}
			    }
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		case ABOUT_DIALOG: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.app_name).setMessage(R.string.about_info);
			AlertDialog alert = builder.create();
			return alert;
		}
		case SELECT_MOVE_DIALOG: {
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.select_move_number);
			dialog.setTitle(R.string.goto_move);
			final EditText moveNrView = (EditText)dialog.findViewById(R.id.selmove_number);
			Button ok = (Button)dialog.findViewById(R.id.selmove_ok);
			Button cancel = (Button)dialog.findViewById(R.id.selmove_cancel);
			moveNrView.setText("1");
			final Runnable gotoMove = new Runnable() {
				public void run() {
					try {
				        int moveNr = Integer.parseInt(moveNrView.getText().toString());
				        ctrl.gotoMove(moveNr);
						dialog.cancel();
					} catch (NumberFormatException nfe) {
						Toast.makeText(getApplicationContext(), R.string.invalid_number_format, Toast.LENGTH_SHORT).show();
					}
				}
			};
			moveNrView.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
						gotoMove.run();
						return true;
					}
					return false;
				}
	        });
			ok.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					gotoMove.run();
				}
			});
			cancel.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					dialog.cancel();
				}
			});
			return dialog;
		}
		case SELECT_BOOK_DIALOG: {
        	File extDir = Environment.getExternalStorageDirectory();
        	String sep = File.separator;
        	File dir = new File(extDir.getAbsolutePath() + sep + bookDir);
        	String[] files = dir.list();
        	if (files == null)
        		files = new String[0];
        	Arrays.sort(files, String.CASE_INSENSITIVE_ORDER);
        	final int numFiles = files.length;
        	CharSequence[] items = new CharSequence[numFiles + 1];
        	for (int i = 0; i < numFiles; i++)
        		items[i] = files[i];
        	items[numFiles] = getString(R.string.internal_book);
        	final CharSequence[] finalItems = items;
			int defaultItem = numFiles;
			for (int i = 0; i < numFiles; i++) {
				if (currentBookFile.equals(items[i])) {
					defaultItem = i;
					break;
				}
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.select_opening_book_file);
			builder.setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					Editor editor = settings.edit();
					String bookFile = "";
					if (item < numFiles)
						bookFile = finalItems[item].toString();
					editor.putString("bookFile", bookFile);
					editor.commit();
					setBookFile(bookFile);
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		}
		return null;
	}

	@Override
	public void requestPromotePiece() {
		runOnUIThread(new Runnable() {
            public void run() {
            	showDialog(PROMOTE_DIALOG);
            }
		});
	}

	@Override
	public void reportInvalidMove(Move m) {
		String msg = String.format("Invalid move %s-%s", TextIO.squareToString(m.from), TextIO.squareToString(m.to));
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void computerMoveMade() {
		if (soundEnabled) {
			if (moveSound != null)
				moveSound.release();
			moveSound = MediaPlayer.create(this, R.raw.movesound);
			moveSound.start();
		}
	}

	@Override
	public void runOnUIThread(Runnable runnable) {
		runOnUiThread(runnable);
	}

	/** Decide if user should be warned about heavy CPU usage. */
	private final void updateNotification() {
		boolean warn = false;
		if (lastVisibleMillis != 0) { // GUI not visible
			warn = lastComputationMillis >= lastVisibleMillis + 90000;
		}
		setNotification(warn);
	}

	private boolean notificationActive = false;

	/** Set/clear the "heavy CPU usage" notification. */
	private final void setNotification(boolean show) {
		if (notificationActive == show)
			return;
		notificationActive = show;
		final int cpuUsage = 1;
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager)getSystemService(ns);
		if (show) {
			int icon = R.drawable.icon;
			CharSequence tickerText = "Heavy CPU usage";
			long when = System.currentTimeMillis();
			Notification notification = new Notification(icon, tickerText, when);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;

			Context context = getApplicationContext();
			CharSequence contentTitle = "Background processing";
			CharSequence contentText = "DroidFish is using a lot of CPU power";
			Intent notificationIntent = new Intent(this, CPUWarning.class);

			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

			mNotificationManager.notify(cpuUsage, notification);
		} else {
			mNotificationManager.cancel(cpuUsage);
		}
	}

	private final String timeToString(long time) {
		int secs = (int)Math.floor((time + 999) / 1000.0);
		boolean neg = false;
		if (secs < 0) {
			neg = true;
			secs = -secs;
		}
		int mins = secs / 60;
		secs -= mins * 60;
		StringBuilder ret = new StringBuilder();
		if (neg) ret.append('-');
		ret.append(mins);
		ret.append(':');
		if (secs < 10) ret.append('0');
		ret.append(secs);
		return ret.toString();
	}

	private Handler handlerTimer = new Handler();
	private Runnable r = new Runnable() {
		public void run() {
			ctrl.updateRemainingTime();
		}
	};
	
	@Override
	public void setRemainingTime(long wTime, long bTime, long nextUpdate) {
		whiteClock.setText("White: " + timeToString(wTime));
		blackClock.setText("Black: " + timeToString(bTime));
		handlerTimer.removeCallbacks(r);
		if (nextUpdate > 0) {
			handlerTimer.postDelayed(r, nextUpdate);
		}
	}
}
