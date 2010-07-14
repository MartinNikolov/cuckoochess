package org.petero.droidfish;

public class GameMode {
	private final boolean playerWhite;
	private final boolean playerBlack;
	private final boolean analysisMode;

	public GameMode(int modeNr) {
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
		case 5: // Computer vs Computer
			playerWhite = false;
			playerBlack = false;
			analysisMode = false;
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

	@Override
	public boolean equals(Object o) {
		if ((o == null) || (o.getClass() != this.getClass()))
			return false;
		GameMode other = (GameMode)o;
		if (playerWhite != other.playerWhite)
			return false;
		if (playerBlack != other.playerBlack)
			return false;
		if (analysisMode != other.analysisMode)
			return false;
		return true;
	}
}
