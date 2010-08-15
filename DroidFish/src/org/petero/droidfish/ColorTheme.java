package org.petero.droidfish;

import android.content.SharedPreferences;
import android.graphics.Color;

public class ColorTheme {
	private int colorTable[];

	final static int DARK_SQUARE = 0;
	final static int BRIGHT_SQUARE = 1;
	final static int SELECTED_SQUARE = 2;
	final static int CURSOR_SQUARE = 3;
	final static int DARK_PIECE = 4;
	final static int BRIGHT_PIECE = 5;
	final static int CURRENT_MOVE = 6;
	final static int ARROW_0 = 7;

	final static int numArrows = 6;

	private static ColorTheme inst = null;

	/** Get singleton instance. */
	static final ColorTheme instance() {
		if (inst == null)
			inst = new ColorTheme();
		return inst;
	}
	
	private ColorTheme() {
		colorTable = new int[ARROW_0 + numArrows];
	}
	
	final void readColors(SharedPreferences settings) {
		setColorTable(DARK_SQUARE,     settings, "darkSquare",     "#FF808080");
		setColorTable(BRIGHT_SQUARE,   settings, "brightSquare",   "#FFBEBE5A");
		setColorTable(SELECTED_SQUARE, settings, "selectedSquare", "#FFFF0000");
		setColorTable(CURSOR_SQUARE,   settings, "cursorSquare",   "#FF00FF00");
		setColorTable(DARK_PIECE,      settings, "darkPiece",      "#FF000000");
		setColorTable(BRIGHT_PIECE,    settings, "brightPiece",    "#FFFFFFFF");
		setColorTable(CURRENT_MOVE,    settings, "currentMove",    "#FF888888");

		setColorTable(ARROW_0 + 0, settings, "arrow0", "#A01F1FFF");
		setColorTable(ARROW_0 + 1, settings, "arrow1", "#A0FF1F1F");
		setColorTable(ARROW_0 + 2, settings, "arrow2", "#501F1FFF");
		setColorTable(ARROW_0 + 3, settings, "arrow3", "#50FF1F1F");
		setColorTable(ARROW_0 + 4, settings, "arrow4", "#1E1F1FFF");
		setColorTable(ARROW_0 + 5, settings, "arrow5", "#28FF1F1F");
	}

	private void setColorTable(int colorType, SharedPreferences settings,
							   String prefName, String defaultColor) {
		prefName = "color_" + prefName;
		String colorString = settings.getString(prefName, defaultColor);
		try {
			colorTable[colorType] = Color.parseColor(colorString);
		} catch (IllegalArgumentException e) {
			colorTable[colorType] = 0;
		}
	}

	final int getColor(int colorType) {
		return colorTable[colorType];
	}
}
