package guibase;

import chess.Position;

public interface GUIInterface {

	/** Update the displayed board position. */
	public void setPosition(Position pos);

	/** Mark square i as selected. Set to -1 to clear selection. */
	public void setSelection(int sq);

	/** Set the status text. */
	public void setStatusString(String str);

	/** Update the list of moves. */
	public void setMoveListString(String str);

	/** Get the current time limit. */
	public int timeLimit();

	/** Return true if "show thinking" is enabled. */
	public boolean showThinking();

	/** Ask for which piece to promote a pawn to. Should be implemented as a modal dialog. */
	public int getPromotePiece();

	/** Run code on the GUI thread. */
	public void runOnUIThread(Runnable runnable);
}
