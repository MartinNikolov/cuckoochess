package org.petero.droidfish;

public class GameMode {
	public final boolean playerWhite;
	public final boolean analysisMode;

	GameMode(int modeNr) {
		switch (modeNr) {
		case 1: default: // Player white
			playerWhite = true;
			analysisMode = false;
			break;
		case 2: // Player black
			playerWhite = false;
			analysisMode = false;
			break;
		case 3: // Two players
			// FIXME!!!
			playerWhite = true;
			analysisMode = false;
			break;
		case 4: // Analysis mode
			playerWhite = true;
			analysisMode = true;
			break;
		}
	}
}
