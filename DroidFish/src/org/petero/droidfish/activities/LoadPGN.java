package org.petero.droidfish.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.petero.droidfish.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class LoadPGN extends ListActivity {
	private static final class GameInfo {
		String event = "";
		String site = "";
		String date = "";
		String round = "";
		String white = "";
		String black = "";
		String result = "";
		long startPos;
		long endPos;

		public String toString() {
    		StringBuilder info = new StringBuilder(128);
    		info.append(white);
    		info.append(" - ");
    		info.append(black);
    		if (date.length() > 0) {
    			info.append(' ');
    			info.append(date);
    		}
    		if (round.length() > 0) {
    			info.append(' ');
	    		info.append(round);
    		}
    		if (event.length() > 0) {
    			info.append(' ');
    			info.append(event);
    		}
    		if (site.length() > 0) {
    			info.append(' ');
    			info.append(site);
    		}
    		info.append(' ');
    		info.append(result);
    		return info.toString();
		}
	}

	static ArrayList<GameInfo> gamesInFile = new ArrayList<GameInfo>();
	static boolean cacheValid = false;
	String fileName;
	ProgressDialog progress;
	GameInfo giToDelete = null;
	ArrayAdapter<GameInfo> aa = null;
	EditText filterText = null;

	SharedPreferences settings;
	int defaultItem = 0;
	String lastSearchString = "";
	String lastFileName = "";
	long lastModTime = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

		if (savedInstanceState != null) {
			defaultItem = savedInstanceState.getInt("defaultItem");
			lastSearchString = savedInstanceState.getString("lastSearchString");
			if (lastSearchString == null) lastSearchString = "";
			lastFileName = savedInstanceState.getString("lastFileName");
			if (lastFileName == null) lastFileName = "";
			lastModTime = savedInstanceState.getLong("lastModTime");
		} else {
			defaultItem = settings.getInt("defaultItem", 0);
			lastSearchString = settings.getString("lastSearchString", "");
			lastFileName = settings.getString("lastFileName", "");
			lastModTime = settings.getLong("lastModTime", 0);
		}

		Intent i = getIntent();
		fileName = i.getAction();
		showDialog(PROGRESS_DIALOG);
		final LoadPGN lpgn = this;
		new Thread(new Runnable() {
			public void run() {
				readFile();
				runOnUiThread(new Runnable() {
					public void run() {
						lpgn.showList();
					}
				});
			}
		}).start();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("defaultItem", defaultItem);
		outState.putString("lastSearchString", lastSearchString);
		outState.putString("lastFileName", lastFileName);
		outState.putLong("lastModTime", lastModTime);
	}

	@Override
	protected void onPause() {
		Editor editor = settings.edit();
		editor.putInt("defaultItem", defaultItem);
		editor.putString("lastSearchString", lastSearchString);
		editor.putString("lastFileName", lastFileName);
		editor.putLong("lastModTime", lastModTime);
		editor.commit();
		super.onPause();
	}

	private final void showList() {
		progress.dismiss();
		setContentView(R.layout.select_game);
		aa = new ArrayAdapter<GameInfo>(this, R.layout.select_game_list_item, gamesInFile);
		setListAdapter(aa);
		ListView lv = getListView();
		lv.setSelectionFromTop(defaultItem, 0);
		lv.setFastScrollEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
				defaultItem = pos;
				sendBackResult(aa.getItem(pos));
			}
		});
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
				giToDelete = aa.getItem(pos);
				removeDialog(DELETE_GAME_DIALOG);
				showDialog(DELETE_GAME_DIALOG);
				return true;
			}
		});

//		lv.setTextFilterEnabled(true);
		filterText = (EditText)findViewById(R.id.select_game_filter);
		filterText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) { }
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				aa.getFilter().filter(s);
				lastSearchString = s.toString();
			}
		});
		filterText.setText(lastSearchString);
		lv.requestFocus();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	final static int PROGRESS_DIALOG = 0;
	final static int DELETE_GAME_DIALOG = 1;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			progress = new ProgressDialog(this);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setTitle(R.string.reading_pgn_file);
			progress.setMessage(getString(R.string.please_wait));
			progress.setCancelable(false);
			return progress;
		case DELETE_GAME_DIALOG: {
			final GameInfo gi = giToDelete;
			giToDelete = null;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Delete game?");
			String msg = gi.toString();
			builder.setMessage(msg);
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					deleteGame(gi);
					dialog.cancel();
				}
			});
			builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		default:
			return null;
		}
	}

	static private final class BufferedRandomAccessFileReader {
		RandomAccessFile f;
		byte[] buffer = new byte[8192];
		long bufStartFilePos = 0;
		int bufLen = 0;
		int bufPos = 0;

		BufferedRandomAccessFileReader(String fileName) throws FileNotFoundException {
			f = new RandomAccessFile(fileName, "r");
		}
		final long length() throws IOException {
			return f.length();
		}
		final long getFilePointer() throws IOException {
			return bufStartFilePos + bufPos;
		}
		final void close() throws IOException {
			f.close();
		}

		private final int EOF = -1024;

		final String readLine() throws IOException {
			// First handle the common case where the next line is entirely 
			// contained in the buffer
			for (int i = bufPos; i < bufLen; i++) {
				byte b = buffer[i];
				if ((b == '\n') || (b == '\r')) {
					String line = new String(buffer, bufPos, i - bufPos);
					for ( ; i < bufLen; i++) {
						b = buffer[i];
						if ((b != '\n') && (b != '\r')) {
							bufPos = i;
							return line;
						}
					}
					break;
				}
			}

			// Generic case
			byte[] lineBuf = new byte[8192];
			int lineLen = 0;
			int b;
			while (true) {
				b = getByte();
				if (b == '\n' || b == '\r' || b == EOF)
					break;
				lineBuf[lineLen++] = (byte)b;
				if (lineLen >= lineBuf.length)
					break;
			}
			while (true) {
				b = getByte();
				if ((b != '\n') && (b != '\r')) {
					if (b != EOF)
						bufPos--;
					break;
				}
			}
			if ((b == EOF) && (lineLen == 0))
				return null;
			else
				return new String(lineBuf, 0, lineLen);
		}
		
		private final int getByte() throws IOException {
			if (bufPos >= bufLen) {
				bufStartFilePos = f.getFilePointer();
				bufLen = f.read(buffer);
				bufPos = 0;
				if (bufLen <= 0)
					return EOF;
			}
			return buffer[bufPos++];
		}
	}
	
	private final void readFile() {
		if (!fileName.equals(lastFileName))
			defaultItem = 0;
		long modTime = new File(fileName).lastModified();
		if (cacheValid && (modTime == lastModTime) && fileName.equals(lastFileName))
			return;
		lastModTime = modTime;
		lastFileName = fileName;
		try {
			int percent = -1;
			gamesInFile.clear();
			BufferedRandomAccessFileReader f = new BufferedRandomAccessFileReader(fileName);
			long fileLen = f.length();
			GameInfo gi = null;
			GameInfo prevGi = new GameInfo();
			boolean inHeader = false;
			long filePos = 0;
			while (true) {
				filePos = f.getFilePointer();
				String line = f.readLine();
				if (line == null)
					break; // EOF
				int len = line.length();
				if (len == 0)
					continue;
				boolean isHeader = line.charAt(0) == '[';
				if (isHeader) {
					if (!line.contains("\"")) // Try to avoid some false positives
						isHeader = false;
				}
				if (isHeader) {
					if (!inHeader) { // Start of game
						inHeader = true;
						if (gi != null) {
							gi.endPos = filePos;
							gamesInFile.add(gi);
							final int newPercent = (int)(filePos * 100 / fileLen);
							if (newPercent > percent) {
								percent =  newPercent;
								runOnUiThread(new Runnable() {
									public void run() {
										progress.setProgress(newPercent);
									}
								});
							}
							prevGi = gi;
						}
						gi = new GameInfo();
						gi.startPos = filePos;
						gi.endPos = -1;
					}
					if (line.startsWith("[Event ")) {
						gi.event = line.substring(8, len - 2);
						if (gi.event.equals("?")) gi.event = "";
						else if (gi.event.equals(prevGi.event)) gi.event = prevGi.event;
					} else if (line.startsWith("[Site ")) {
						gi.site = line.substring(7, len - 2);
						if (gi.site.equals("?")) gi.site = "";
						else if (gi.site.equals(prevGi.site)) gi.site = prevGi.site;
					} else if (line.startsWith("[Date ")) {
						gi.date = line.substring(7, len - 2);
						if (gi.date.equals("?")) gi.date = "";
						else if (gi.date.equals(prevGi.date)) gi.date = prevGi.date;
					} else if (line.startsWith("[Round ")) {
						gi.round = line.substring(8, len - 2);
						if (gi.round.equals("?")) gi.round = "";
						else if (gi.round.equals(prevGi.round)) gi.round = prevGi.round;
					} else if (line.startsWith("[White ")) {
						gi.white = line.substring(8, len - 2);
						if (gi.white.equals(prevGi.white)) gi.white = prevGi.white;
					} else if (line.startsWith("[Black ")) {
						gi.black = line.substring(8, len - 2);
						if (gi.black.equals(prevGi.black)) gi.black = prevGi.black;
					} else if (line.startsWith("[Result ")) {
						gi.result = line.substring(9, len - 2);
						if (gi.result.equals("1-0")) gi.result = "1-0";
						else if (gi.result.equals("0-1")) gi.result = "0-1";
						else if ((gi.result.equals("1/2-1/2")) || (gi.result.equals("1/2"))) gi.result = "1/2-1/2";
						else gi.result = "*";
					}
				} else {
					inHeader = false;
				}
			}
			if (gi != null) {
				gi.endPos = filePos;
				gamesInFile.add(gi);
			}
			f.close();
		} catch (IOException e) {
		}
		cacheValid = true;
	}

	private final void sendBackResult(GameInfo gi) {
		try {
			RandomAccessFile f = new RandomAccessFile(fileName, "r");
			byte[] pgnData = new byte[(int) (gi.endPos - gi.startPos)];
			f.seek(gi.startPos);
			f.readFully(pgnData);
			f.close();
			String result = new String(pgnData);
			setResult(RESULT_OK, (new Intent()).setAction(result));
			finish();
		} catch (IOException e) {
		}
		setResult(RESULT_CANCELED);
		finish();
	}

	private final void deleteGame(GameInfo gi) {
		try {
			File origFile = new File(fileName);
			File tmpFile = new File(fileName + ".tmp_delete");
			RandomAccessFile fileReader = new RandomAccessFile(origFile, "r");
			RandomAccessFile fileWriter = new RandomAccessFile(tmpFile, "rw");
			copyData(fileReader, fileWriter, gi.startPos);
			fileReader.seek(gi.endPos);
			copyData(fileReader, fileWriter, fileReader.length() - gi.endPos);
			fileReader.close();
			fileWriter.close();
			tmpFile.renameTo(origFile);

			// Update gamesInFile and listview adapter (aa)
			gamesInFile.remove(gi);
			final int nGames = gamesInFile.size();
			final long delta = gi.endPos - gi.startPos;
			for (int i = 0; i < nGames; i++) {
				GameInfo tmpGi = gamesInFile.get(i);
				if (tmpGi.startPos > gi.startPos) {
					tmpGi.startPos -= delta;
					tmpGi.endPos -= delta;
				}
			}
			boolean androidIsBuggy = true;
			if (!androidIsBuggy) {
				aa.remove(gi);
				aa.notifyDataSetChanged();
			} else {
				ListView lv = getListView();
				int pos = lv.pointToPosition(0,0);
				aa = new ArrayAdapter<GameInfo>(this, R.layout.select_game_list_item, gamesInFile);
				setListAdapter(aa);
				String s = filterText.getText().toString();
				aa.getFilter().filter(s);
				lv.setSelection(pos);
			}
			// Update lastModTime, since current change has already been handled
			long modTime = new File(fileName).lastModified();
			lastModTime = modTime;
		} catch (IOException e) {
			String msg = "Failed to delete game";
			Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	private final static void copyData(RandomAccessFile fileReader,
									   RandomAccessFile fileWriter,
									   long nBytes) throws IOException {
		byte[] buffer = new byte[8192];
		while (nBytes > 0) {
			int nRead = fileReader.read(buffer, 0, Math.min(buffer.length, (int)nBytes));
			if (nRead > 0) {
				fileWriter.write(buffer, 0, nRead);
				nBytes -= nRead;
			}
		}
	}
}
