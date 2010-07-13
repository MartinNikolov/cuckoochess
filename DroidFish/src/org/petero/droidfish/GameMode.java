package org.petero.droidfish;

public class GameMode {
	private final boolean playerWhite;
	private final boolean playerBlack;
	private final boolean analysisMode;

	GameMode(int modeNr) {
		switch (modeNr) {
		case 1: default: // Player white
			playerWhite = true;
			playerBlack = false;
			analysisMode = false;
			break;
		case 2: // Player black
			playerWhite = false;
			playerBlack = true;
			analysisMode = false;
			break;
		case 3: // Two players
			playerWhite = true;
			playerBlack = true;
			analysisMode = false;
			break;
		case 4: // Analysis mode
			playerWhite = true;
			playerBlack = true;
			analysisMode = true;
			break;
		}
	}

	public final boolean playerWhite() {
		return playerWhite;
	}
	public final boolean playerBlack() {
		return playerBlack;
	}
	public final boolean analysisMode() {
		return analysisMode;
	}
	public boolean humansTurn(boolean whiteMove) {
        return (whiteMove ? playerWhite : playerBlack) || analysisMode;
	}
}
