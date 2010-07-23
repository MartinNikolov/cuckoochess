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
	// FIXME!!! User defined time controls
	// FIXME!!! Computer clock should stop if phone turned off (computer stops thinking if unplugged)

	// FIXME!!! Include draw claim in save/restore state
	// FIXME!!! Implement fully standard-compliant PGN parser
	// FIXME!!! Try to parse redo info in PGN import
	// FIXME!!! Save analysis (analyze mode and computer thinking mode) as PGN comments
	// FIXME!!! Redo moves should be displayed in grey on screen

	// FIXME!!! Implement PGN database support (and FEN?)
	// FIXME!!! Implement support for PGN comments
	// FIXME!!! Implement support for PGN variants

	// FIXME!!! book.txt (and test classes) should not be included in apk

	// FIXME!!! Implement pondering (permanent brain)
	// FIXME!!! Implement multi-variation analysis mode
	// FIXME!!! Implement "limit strength" option


	private ChessBoard cb;
	private ChessController ctrl = null;
	private boolean mShowThinking;
	private boolean mShowBookHints;
	private int mTimeLimit;
	private GameMode gameMode;
	private boolean autoSwapSides;

	private TextView status;
	private ScrollView moveListScroll;
	private TextView moveList;
	private TextView thinking;
	private TextView whiteClock, blackClock;

	SharedPreferences settings;

	private boolean soundEnabled;
	private MediaPlayer moveSound;

	private final String bookDir = "DroidFish";
	private String currentBookFile = "";

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
        readPrefs();
        ctrl.newGame(gameMode);
        {
        	String fen = "";
        	String moves = "";
        	String numUndo = "0";
        	String clockState = "";
    		String tmp;
        	if (savedInstanceState != null) {
        		tmp = savedInstanceState.getString("startFEN");
        		if (tmp != null) fen = tmp;
        		tmp = savedInstanceState.getString("moves");
        		if (tmp != null) moves = tmp;
        		tmp = savedInstanceState.getString("numUndo");
        		if (tmp != null) numUndo = tmp;
        		tmp = savedInstanceState.getString("clockState");
        		if (tmp != null) clockState = tmp;
        	} else {
        		tmp = settings.getString("startFEN", null);
        		if (tmp != null) fen = tmp;
        		tmp = settings.getString("moves", null);
        		if (tmp != null) moves = tmp;
        		tmp = settings.getString("numUndo", null);
        		if (tmp != null) numUndo = tmp;
        		tmp = settings.getString("clockState", null);
        		if (tmp != null) clockState = tmp;
        	}
        	List<String> posHistStr = new ArrayList<String>();
        	posHistStr.add(fen);
        	posHistStr.add(moves);
        	posHistStr.add(numUndo);
        	posHistStr.add(clockState);
        	ctrl.setPosHistory(posHistStr);
        }
    	ctrl.setGuiPaused(true);
    	ctrl.setGuiPaused(false);
        ctrl.startGame();
    }

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		ChessBoard oldCB = cb;
		String statusStr = status.getText().toString();
		String moveListStr = moveList.getText().toString();
		String thinkingStr = thinking.getText().toString();
        initUI(false);
        readPrefs();
        cb.cursorX = oldCB.cursorX;
        cb.cursorY = oldCB.cursorY;
        cb.cursorVisible = oldCB.cursorVisible;
        setPosition(oldCB.pos);
        setSelection(oldCB.selectedSquare);
        setStatusString(statusStr);
        setMoveListString(moveListStr);
        setThinkingString(thinkingStr);
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
        cb.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
		        if (ctrl.humansTurn() && (event.getAction() == MotionEvent.ACTION_UP)) {
		            int sq = cb.eventToSquare(event);
		            Move m = cb.mousePressed(sq);
		            if (m != null) {
		                ctrl.makeHumanMove(m);
		            }
		            return false;
		        }
		        return false;
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
			@Override
			public boolean onLongClick(View v) {
				showDialog(CLIPBOARD_DIALOG);
				return true;
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (ctrl != null) {
			List<String> posHistStr = ctrl.getPosHistory();
			outState.putString("startFEN", posHistStr.get(0));
			outState.putString("moves", posHistStr.get(1));
			outState.putString("numUndo", posHistStr.get(2));
			outState.putString("clockState", posHistStr.get(3));
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
			List<String> posHistStr = ctrl.getPosHistory();
			Editor editor = settings.edit();
			editor.putString("startFEN", posHistStr.get(0));
			editor.putString("moves", posHistStr.get(1));
			editor.putString("numUndo", posHistStr.get(2));
			editor.putString("clockState", posHistStr.get(3));
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

	private void readPrefs() {
		String gameModeStr = settings.getString("gameMode", "1");
        int modeNr = Integer.parseInt(gameModeStr);
        autoSwapSides = settings.getBoolean("autoSwapSides", false);
        gameMode = new GameMode(modeNr);
        mShowThinking = settings.getBoolean("showThinking", false);
        mShowBookHints = settings.getBoolean("bookHints", false);
        String timeLimitStr = settings.getString("timeLimit", "5000");
        mTimeLimit = Integer.parseInt(timeLimitStr);
        boolean boardFlipped = settings.getBoolean("boardFlipped", false);
        soundEnabled = settings.getBoolean("soundEnabled", false);
        cb.setFlipped(boardFlipped);
        ctrl.setTimeLimit();
        String fontSizeStr = settings.getString("fontSize", "12");
        int fontSize = Integer.parseInt(fontSizeStr);
        status.setTextSize(fontSize);
        moveList.setTextSize(fontSize);
        thinking.setTextSize(fontSize);
        String bookFile = settings.getString("bookFile", "");
        setBookFile(bookFile);
        ctrl.updateBookHints();
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
				boolean boardFlipped;
				int gameModeType;
				if (gameMode.playerWhite()) {
					gameModeType = GameMode.PLAYER_BLACK;
					boardFlipped = true;
				} else {
					gameModeType = GameMode.PLAYER_WHITE;
					boardFlipped = false;
				}
				Editor editor = settings.edit();
				editor.putBoolean("boardFlipped", boardFlipped);
				String gameModeStr = String.format("%d", gameModeType);
				editor.putString("gameMode", gameModeStr);
				editor.commit();
				gameMode = new GameMode(gameModeType);
		        cb.setFlipped(boardFlipped);
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
		case R.id.item_draw: {
			if (ctrl.humansTurn()) {
				if (!ctrl.claimDrawIfPossible()) {
					Toast.makeText(getApplicationContext(), R.string.draw_claim_not_valid, Toast.LENGTH_SHORT).show();
				}
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
					ctrl.setFENOrPGN(fen);
				} catch (ChessParseError e) {
				}
			}
			break;
		}
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
		if (!ctrl.canRedoMove())
			moveListScroll.fullScroll(ScrollView.FOCUS_DOWN);
	}
	
	@Override
	public void setThinkingString(String str) {
		thinking.setText(str);
		if (ctrl.computerBusy()) {
			lastComputationMillis = System.currentTimeMillis();
		} else {
			lastComputationMillis = 0;
		}
		updateNotification();
	}

	@Override
	public int timeLimit() {
		return mTimeLimit;
	}

	@Override
	public boolean showThinking() {
		return mShowThinking || gameMode.analysisMode() || (mShowBookHints && ctrl.humansTurn());
	}

	@Override
	public boolean showBookHints() {
		return mShowBookHints;
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
			final CharSequence[] items = {
				getString(R.string.copy_game), getString(R.string.copy_position), getString(R.string.paste)
			};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.clipboard);
			builder.setItems(items, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int item) {
					switch (item) {
					case 0: {
						String pgn = ctrl.getPGN();
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
								ctrl.setFENOrPGN(fenPgn);
							} catch (ChessParseError e) {
								Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
							}
						}
						break;
					}
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
	public final void updateNotification() {
		boolean warn = false;
		if (lastVisibleMillis != 0) { // GUI not visible
			warn = lastComputationMillis >= lastVisibleMillis + 90000;
		}
		setNotification(warn);
	}

	private boolean notificationActive = false;

	/** Set/clear the "heavy CPU usage" notification. */
	public final void setNotification(boolean show) {
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

	private String timeToString(long time) {
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
