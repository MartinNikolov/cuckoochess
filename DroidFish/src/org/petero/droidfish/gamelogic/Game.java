/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.petero.droidfish.gamelogic;

import java.util.ArrayList;
import java.util.List;

import org.petero.droidfish.PGNOptions;
import org.petero.droidfish.engine.ComputerPlayer;
import org.petero.droidfish.gamelogic.GameTree.Node;

/**
 *
 * @author petero
 */
public class Game {
    boolean pendingDrawOffer;
    GameState drawState; // FIXME Move to tree.playerAction
    GameTree tree;
    private String drawStateMoveStr; // Move required to claim DRAW_REP or DRAW_50  FIXME!!! Move to tree.playerAction
    private GameState resignState;  // FIXME!!! Move to tree.playerAction
    private ComputerPlayer computerPlayer;
    TimeControl timeController;
    private boolean gamePaused;

    public Game(ComputerPlayer computerPlayer, int timeControl, int movesPerSession, int timeIncrement) {
        this.computerPlayer = computerPlayer;
        tree = new GameTree();
        timeController = new TimeControl();
        timeController.setTimeControl(timeControl, movesPerSession, timeIncrement);
        gamePaused = false;
        handleCommand("new");
    }

    public final void setComputerPlayer(ComputerPlayer computerPlayer) {
    	this.computerPlayer = computerPlayer;
	}

	public final void setGamePaused(boolean gamePaused) {
		if (gamePaused != this.gamePaused) {
			this.gamePaused = gamePaused;
	        updateTimeControl(false);
		}
	}

	final void setPos(Position pos) {
		tree.setStartPos(new Position(pos));
        updateTimeControl(false);
	}

    public final String getPGN(PGNOptions options) {
    	String pgnResultString = getPGNResultString();
    	return tree.toPGN(pgnResultString, options);
    }

	final boolean readPGN(String pgn, PGNOptions options) throws ChessParseError {
		boolean ret = tree.readPGN(pgn, options);
		if (ret)
			updateTimeControl(false);
		return ret;
	}
	
	final Position currPos() {
		return tree.currentPos;
	}

	/**
     * Update the game state according to move/command string from a player.
     * @param str The move or command to process.
     * @return True if str was understood, false otherwise.
     */
    public final boolean processString(String str) {
        if (handleCommand(str)) {
        	updateTimeControl(true);
            return true;
        }
        if (getGameState() != GameState.ALIVE) {
            return false;
        }

        Move m = TextIO.UCIstringToMove(str);
        if (m != null) {
            ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(currPos());
            moves = MoveGen.removeIllegal(currPos(), moves);
            boolean legal = false;
            for (int i = 0; i < moves.size(); i++) {
            	if (m.equals(moves.get(i))) {
            		legal = true;
            		break;
            	}
            }
            if (!legal)
            	m = null;
        }
        if (m == null) {
            m = TextIO.stringToMove(currPos(), str);
        }
        if (m == null) {
            return false;
        }

        List<Move> varMoves = tree.variations();
        boolean movePresent = false;
        int varNo;
        for (varNo = 0; varNo < varMoves.size(); varNo++) {
        	if (varMoves.get(varNo).equals(m)) {
        		movePresent = true;
        		break;
        	}
        }
        if (!movePresent) {
        	String moveStr = TextIO.moveToUCIString(m);
        	String playerAction = pendingDrawOffer ? "draw offer" : "";
        	varNo = tree.addMove(moveStr, playerAction, 0, "", "");
        }
        tree.reorderVariation(varNo, 0);
        tree.goForward(0);
        int remaining = timeController.moveMade(System.currentTimeMillis());
        tree.setRemainingTime(remaining);
        updateTimeControl(true);
    	pendingDrawOffer = false;
        return true;
    }

	private final void updateTimeControl(boolean discardElapsed) {
		int move = currPos().fullMoveCounter;
		boolean wtm = currPos().whiteMove;
		if (discardElapsed || (move != timeController.currentMove) || (wtm != timeController.whiteToMove)) {
			int initialTime = timeController.getInitialTime();
			int whiteBaseTime = tree.getRemainingTime(true, initialTime);
			int blackBaseTime = tree.getRemainingTime(false, initialTime);
			timeController.setCurrentMove(move, wtm, whiteBaseTime, blackBaseTime);
		}
		long now = System.currentTimeMillis();
		if (gamePaused || (getGameState() != GameState.ALIVE)) {
			timeController.stopTimer(now);
		} else {
			timeController.startTimer(now);
		}
	}

    public final String getGameStateString() {
        switch (getGameState()) {
            case ALIVE:
                return "";
            case WHITE_MATE:
                return "Game over, white mates!";
            case BLACK_MATE:
                return "Game over, black mates!";
            case WHITE_STALEMATE:
            case BLACK_STALEMATE:
                return "Game over, draw by stalemate!";
            case DRAW_REP:
            {
            	String ret = "Game over, draw by repetition!";
            	if ((drawStateMoveStr != null) && (drawStateMoveStr.length() > 0)) {
            		ret = ret + " [" + drawStateMoveStr + "]";
            	}
            	return ret;
            }
            case DRAW_50:
            {
                String ret = "Game over, draw by 50 move rule!";
            	if ((drawStateMoveStr != null) && (drawStateMoveStr.length() > 0)) {
            		ret = ret + " [" + drawStateMoveStr + "]";  
            	}
            	return ret;
            }
            case DRAW_NO_MATE:
                return "Game over, draw by impossibility of mate!";
            case DRAW_AGREE:
                return "Game over, draw by agreement!";
            case RESIGN_WHITE:
                return "Game over, white resigns!";
            case RESIGN_BLACK:
                return "Game over, black resigns!";
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Get the last played move, or null if no moves played yet.
     */
    public final Move getLastMove() {
    	return tree.currentNode.move;
    }
    
    /** Return true if there is a move to redo. */
    public final boolean canRedoMove() {
    	int nVar = tree.variations().size();
    	return nVar > 0;
    }

    public static enum GameState {
        ALIVE,
        WHITE_MATE,         // White mates
        BLACK_MATE,         // Black mates
        WHITE_STALEMATE,    // White is stalemated
        BLACK_STALEMATE,    // Black is stalemated
        DRAW_REP,           // Draw by 3-fold repetition
        DRAW_50,            // Draw by 50 move rule
        DRAW_NO_MATE,       // Draw by impossibility of check mate
        DRAW_AGREE,         // Draw by agreement
        RESIGN_WHITE,       // White resigns
        RESIGN_BLACK        // Black resigns
    }

    /**
     * Get the current state (draw, mate, ongoing, etc) of the game.
     */
    public final GameState getGameState() {
    	Position pos = currPos();
        ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(pos);
        moves = MoveGen.removeIllegal(pos, moves);
        if (moves.size() == 0) {
            if (MoveGen.inCheck(pos)) {
                return pos.whiteMove ? GameState.BLACK_MATE : GameState.WHITE_MATE;
            } else {
                return pos.whiteMove ? GameState.WHITE_STALEMATE : GameState.BLACK_STALEMATE;
            }
        }
        if (insufficientMaterial()) {
            return GameState.DRAW_NO_MATE;
        }
        if (resignState != GameState.ALIVE) {
            return resignState;
        }
        return drawState;
    }

    /**
     * Check if a draw offer is available.
     * @return True if the current player has the option to accept a draw offer.
     */
    public final boolean haveDrawOffer() {
    	return tree.currentNode.playerAction.equals("draw offer");
    }

    /**
     * Handle a special command.
     * @param moveStr  The command to handle
     * @return  True if command handled, false otherwise.
     */
    private final boolean handleCommand(String moveStr) {
        if (moveStr.equals("new")) {
        	tree = new GameTree();
            pendingDrawOffer = false;
            drawState = GameState.ALIVE;
            resignState = GameState.ALIVE;
            if (computerPlayer != null)
            	computerPlayer.clearTT();
            timeController.reset();
            updateTimeControl(true);
            return true;
        } else if (moveStr.equals("undo")) {
        	Move m = tree.currentNode.move;
            if (m != null) {
            	tree.goBack();
                pendingDrawOffer = false;
                drawState = GameState.ALIVE;
                resignState = GameState.ALIVE;
                updateTimeControl(true);
                return true;
            }
            return true;
        } else if (moveStr.equals("redo")) {
        	if (canRedoMove()) {
        		tree.goForward(-1);
                pendingDrawOffer = false;
                updateTimeControl(true);
                return true;
            }
            return true;
        } else if (moveStr.startsWith("draw ")) {
            if (getGameState() == GameState.ALIVE) {
                String drawCmd = moveStr.substring(moveStr.indexOf(" ") + 1);
                return handleDrawCmd(drawCmd);
            } else {
                return true;
            }
        } else if (moveStr.equals("resign")) {
            if (getGameState()== GameState.ALIVE) {
                resignState = currPos().whiteMove ? GameState.RESIGN_WHITE : GameState.RESIGN_BLACK;
                return true;
            } else {
                return true;
            }
        } else if (moveStr.startsWith("setpos ")) {
            try {
                String fen = moveStr.substring(moveStr.indexOf(" ") + 1);
                Position newPos = TextIO.readFEN(fen);
                handleCommand("new");
                setPos(newPos);
                updateTimeControl(true);
            } catch (ChessParseError ex) {
            }
            return true;
        } else {
            return false;
        }
    }

    public final String getMoveListString() {
        StringBuilder ret = new StringBuilder(2048);

        Pair<List<Node>, Integer> ml = tree.getMoveList();
        List<GameTree.Node> moveList = ml.first;
        final int numMovesPlayed = ml.second;
        Position pos = new Position(tree.startPos);

        // Print all moves
        int size = moveList.size();
        boolean haveRedoPart = false;
        if (!pos.whiteMove && (size > 0)) {
        	ret.append(String.format("%d... ", pos.fullMoveCounter));
        }
        UndoInfo ui = new UndoInfo();
        GameTree.Node n = tree.rootNode;
        for (int i = 0; i < size; i++) {
        	n = moveList.get(i);
        	if (i == numMovesPlayed) {
        		ret.append("{ ");
        		haveRedoPart = true;
        	}
            if (pos.whiteMove) {
            	ret.append(pos.fullMoveCounter);
            	ret.append('.');
            	ret.append(' ');
            }
            ret.append(n.moveStr);
            ret.append(' ');
            pos.makeMove(n.move, ui);
        }
        String gameResult = getPGNResultString();
        if (!gameResult.equals("*")) {
        	ret.append(gameResult);
        }
        if (haveRedoPart)
        	ret.append("}");
        return ret.toString();
    }

    public final String getPGNResultString() {
        String gameResult = "*";
        switch (getGameState()) {
            case ALIVE:
                break;
            case WHITE_MATE:
            case RESIGN_BLACK:
                gameResult = "1-0";
                break;
            case BLACK_MATE:
            case RESIGN_WHITE:
                gameResult = "0-1";
                break;
            case WHITE_STALEMATE:
            case BLACK_STALEMATE:
            case DRAW_REP:
            case DRAW_50:
            case DRAW_NO_MATE:
            case DRAW_AGREE:
                gameResult = "1/2-1/2";
                break;
        }
        return gameResult;
    }

    /**
     * Return the last zeroing position and a list of moves
     * to go from that position to the current position.
     */
    public final Pair<Position, ArrayList<Move>> getUCIHistory() {
    	Pair<List<Node>, Integer> ml = tree.getMoveList();
        List<Node> moveList = ml.first;
        Position pos = new Position(tree.startPos);
        ArrayList<Move> mList = new ArrayList<Move>();
        Position currPos = new Position(pos);
        UndoInfo ui = new UndoInfo();
        int nMoves = ml.second;
        for (int i = 0; i < nMoves; i++) {
        	Node n = moveList.get(i);
        	mList.add(n.move);
        	currPos.makeMove(n.move, ui);
        	if (currPos.halfMoveClock == 0) {
        		pos = new Position(currPos);
        		mList.clear();
        	}
        }
        return new Pair<Position, ArrayList<Move>>(pos, mList);
    }

    private final boolean handleDrawCmd(String drawCmd) {
    	Position pos = tree.currentPos;
        if (drawCmd.startsWith("rep") || drawCmd.startsWith("50")) {
            boolean rep = drawCmd.startsWith("rep");
            Move m = null;
            String ms = drawCmd.substring(drawCmd.indexOf(" ") + 1);
            if (ms.length() > 0) {
                m = TextIO.stringToMove(pos, ms);
            }
            boolean valid;
            if (rep) {
                valid = false;
                UndoInfo ui = new UndoInfo();
                int repetitions = 0;
                Position posToCompare = new Position(tree.currentPos);
                if (m != null) {
                    posToCompare.makeMove(m, ui);
                    repetitions = 1;
                }
                Pair<List<Node>, Integer> ml = tree.getMoveList();
                List<Node> moveList = ml.first;
                Position tmpPos = new Position(tree.startPos);
                if (tmpPos.drawRuleEquals(posToCompare))
                	repetitions++;
                int nMoves = ml.second;
                for (int i = 0; i < nMoves; i++) {
                	Node n = moveList.get(i);
                	tmpPos.makeMove(n.move, ui);
                	TextIO.fixupEPSquare(tmpPos);
                    if (tmpPos.drawRuleEquals(posToCompare))
                    	repetitions++;
                }
                if (repetitions >= 3)
                    valid = true;
            } else {
                Position tmpPos = new Position(pos);
                if (m != null) {
                    UndoInfo ui = new UndoInfo();
                    tmpPos.makeMove(m, ui);
                }
                valid = tmpPos.halfMoveClock >= 100;
            }
            if (valid) {
                drawState = rep ? GameState.DRAW_REP : GameState.DRAW_50;
                drawStateMoveStr = null;
                if (m != null) {
                	drawStateMoveStr = TextIO.moveToString(pos, m, false);
                }
            } else {
                pendingDrawOffer = true;
                if (m != null) {
                    processString(ms);
                }
            }
            return true;
        } else if (drawCmd.startsWith("offer ")) {
            pendingDrawOffer = true;
            String ms = drawCmd.substring(drawCmd.indexOf(" ") + 1);
            if (TextIO.stringToMove(pos, ms) != null) {
                processString(ms);
            }
            return true;
        } else if (drawCmd.equals("accept")) {
            if (haveDrawOffer()) {
                drawState = GameState.DRAW_AGREE;
            }
            return true;
        } else {
            return false;
        }
    }

    private final boolean insufficientMaterial() {
    	Position pos = currPos();
        if (pos.nPieces(Piece.WQUEEN) > 0) return false;
        if (pos.nPieces(Piece.WROOK)  > 0) return false;
        if (pos.nPieces(Piece.WPAWN)  > 0) return false;
        if (pos.nPieces(Piece.BQUEEN) > 0) return false;
        if (pos.nPieces(Piece.BROOK)  > 0) return false;
        if (pos.nPieces(Piece.BPAWN)  > 0) return false;
        int wb = pos.nPieces(Piece.WBISHOP);
        int wn = pos.nPieces(Piece.WKNIGHT);
        int bb = pos.nPieces(Piece.BBISHOP);
        int bn = pos.nPieces(Piece.BKNIGHT);
        if (wb + wn + bb + bn <= 1) {
            return true;    // King + bishop/knight vs king is draw
        }
        if (wn + bn == 0) {
            // Only bishops. If they are all on the same color, the position is a draw.
            boolean bSquare = false;
            boolean wSquare = false;
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    int p = pos.getPiece(Position.getSquare(x, y));
                    if ((p == Piece.BBISHOP) || (p == Piece.WBISHOP)) {
                        if (Position.darkSquare(x, y)) {
                            bSquare = true;
                        } else {
                            wSquare = true;
                        }
                    }
                }
            }
            if (!bSquare || !wSquare) {
                return true;
            }
        }
        return false;
    }
}
