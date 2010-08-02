package org.petero.droidfish.gamelogic;

/** A token in a PGN data stream. Used by the PGN parser. */
public class PgnToken {
	// These are tokens according to the PGN spec
	static final int STRING = 0;
	static final int INTEGER = 1;
	static final int PERIOD = 2;
	static final int ASTERISK = 3;
	static final int LEFT_BRACKET = 4;
	static final int RIGHT_BRACKET = 5;
	static final int LEFT_PAREN = 6;
	static final int RIGHT_PAREN = 7;
	static final int NAG = 8;
	static final int SYMBOL = 9;

	// These are not tokens according to the PGN spec, but the parser
	// extracts these anyway for convenience.
	static final int COMMENT = 10;
	static final int EOF = 11;

	// Actual token data
	int type;
	String token;

	PgnToken(int type, String token) {
		this.type = type;
		this.token = token;
	}
	
	public interface PgnTokenReceiver {
		public void processToken(GameTree.Node node, int type, String token);
	};
}
