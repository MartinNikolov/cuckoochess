/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.petero.droidfish.gamelogic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.petero.droidfish.PGNOptions;
import org.petero.droidfish.engine.ComputerPlayer;
import org.petero.droidfish.gamelogic.GameTree.Node;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.LeadingMarginSpan;

/**
 *
 * @author petero
 */
public class Game {
    boolean pendingDrawOffer;
    GameTree tree;
    private ComputerPlayer computerPlayer;
    TimeControl timeController;
    private boolean gamePaused;

    PgnScreenText gameTextListener;

    public Game(ComputerPlayer computerPlayer, int timeControl, int movesPerSession, int timeIncrement) {
        this.computerPlayer = computerPlayer;
        gameTextListener = new PgnScreenText();
        tree = new GameTree(gameTextListener);
        timeController = new TimeControl();
        timeController.setTimeControl(timeControl, movesPerSession, timeIncrement);
        gamePaused = false;
        newGame();
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
        if (getGameState() != GameState.ALIVE)
            return false;
		if (str.startsWith("draw ")) {
			String drawCmd = str.substring(str.indexOf(" ") + 1);
			handleDrawCmd(drawCmd);
	    	return true;
		} else if (str.equals("resign")) {
			addToGameTree(new Move(0, 0, 0), "resign");
			return true;
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

        addToGameTree(m, pendingDrawOffer ? "draw offer" : "");
        return true;
    }
    
    private final void addToGameTree(Move m, String playerAction) {
    	if (m.equals(new Move(0, 0, 0))) { // Don't create more than one null move at a node
    		List<Move> varMoves = tree.variations();
    		for (int i = varMoves.size() - 1; i >= 0; i--) {
            	if (varMoves.get(i).equals(m)) {
            		tree.deleteVariation(i);
            	}
    		}
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
        	varNo = tree.addMove(moveStr, playerAction, 0, "", "");
        }
        tree.reorderVariation(varNo, 0);
        tree.goForward(0);
        int remaining = timeController.moveMade(System.currentTimeMillis());
        tree.setRemainingTime(remaining);
        updateTimeControl(true);
    	pendingDrawOffer = false;
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
            	String drawInfo = tree.getGameStateInfo();
            	if (drawInfo.length() > 0) {
            		ret = ret + " [" + drawInfo+ "]";
            	}
            	return ret;
            }
            case DRAW_50:
            {
                String ret = "Game over, draw by 50 move rule!";
            	String drawInfo = tree.getGameStateInfo();
            	if (drawInfo.length() > 0) {
            		ret = ret + " [" + drawInfo + "]";  
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
    
    public final int numVariations() {
    	if (tree.currentNode == tree.rootNode)
    		return 1;
    	tree.goBack();
    	int nChildren = tree.variations().size();
    	tree.goForward(-1);
    	return nChildren;
    }

    public final void changeVariation(int delta) {
    	if (tree.currentNode == tree.rootNode)
    		return;
    	tree.goBack();
    	int defChild = tree.currentNode.defaultChild;
    	int nChildren = tree.variations().size();
    	int newChild = defChild + delta;
    	newChild = Math.max(newChild, 0);
    	newChild = Math.min(newChild, nChildren - 1);
    	tree.goForward(newChild);
        pendingDrawOffer = false;
        updateTimeControl(true);
    }

    public final void removeVariation() {
    	if (numVariations() <= 1)
    		return;
    	tree.goBack();
    	int defChild = tree.currentNode.defaultChild;
    	tree.deleteVariation(defChild);
    	tree.goForward(-1);
        pendingDrawOffer = false;
        updateTimeControl(true);
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
    	return tree.getGameState();
    }

    /**
     * Check if a draw offer is available.
     * @return True if the current player has the option to accept a draw offer.
     */
    public final boolean haveDrawOffer() {
    	return tree.currentNode.playerAction.equals("draw offer");
    }

    public final void undoMove() {
    	Move m = tree.currentNode.move;
    	if (m != null) {
    		tree.goBack();
    		pendingDrawOffer = false;
    		updateTimeControl(true);
    	}
    }

    public final void redoMove() {
    	if (canRedoMove()) {
    		tree.goForward(-1);
            pendingDrawOffer = false;
            updateTimeControl(true);
        }
    }

    public final void newGame() {
    	tree = new GameTree(gameTextListener);
        if (computerPlayer != null)
        	computerPlayer.clearTT();
        timeController.reset();
        pendingDrawOffer = false;
        updateTimeControl(true);
    }


	/** PngTokenReceiver implementation that renders PGN data for screen display. */
	static class PgnScreenText implements PgnToken.PgnTokenReceiver {
		private SpannableStringBuilder sb = new SpannableStringBuilder();
		private int prevType = PgnToken.EOF;
		int nestLevel = 0;
		boolean col0 = true;
		Node currNode = null;
		final int indentStep = 15;
		boolean inMainLine = true;
		boolean upToDate = false;

		private static class NodeInfo {
			int l0, l1;
			NodeInfo(int ls, int le) {
				l0 = ls;
				l1 = le;
			}
		}
		HashMap<Node, NodeInfo> nodeToCharPos;
		
		PgnScreenText() {
			nodeToCharPos = new HashMap<Node, NodeInfo>();
		}

		public Pair<SpannableStringBuilder, Boolean> getData() {
			return new Pair<SpannableStringBuilder, Boolean>(sb, inMainLine);
		}

		int paraStart = 0;
		int paraIndent = 0;
		private final void newLine() {
			if (!col0) {
				if (paraIndent > 0) {
					int paraEnd = sb.length();
					int indent = paraIndent * indentStep;
					sb.setSpan(new LeadingMarginSpan.Standard(indent), paraStart, paraEnd,
							   Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				sb.append('\n');
				paraStart = sb.length();
				paraIndent = nestLevel;
			}
			col0 = true;
		}

		boolean pendingNewLine = false;

		public void processToken(Node node, int type, String token) {
			if (	(prevType == PgnToken.RIGHT_BRACKET) &&
					(type != PgnToken.LEFT_BRACKET))  {
				// End of header. Just drop header lines
				sb = new SpannableStringBuilder();
			}
			if (pendingNewLine) {
				if (type != PgnToken.RIGHT_PAREN) {
					newLine();
					pendingNewLine = false;
				}
			}
			switch (type) {
			case PgnToken.STRING:
				break;
			case PgnToken.INTEGER:
				if (	(prevType != PgnToken.LEFT_PAREN) &&
						(prevType != PgnToken.RIGHT_BRACKET) && !col0)
					sb.append(' ');
				sb.append(token);
				col0 = false;
				break;
			case PgnToken.PERIOD:		 sb.append('.');   col0 = false; break;
			case PgnToken.ASTERISK:		 sb.append(" *");  col0 = false; break;
			case PgnToken.LEFT_BRACKET:  sb.append('[');   col0 = false; break;
			case PgnToken.RIGHT_BRACKET: sb.append("]\n"); col0 = false; break;
			case PgnToken.LEFT_PAREN:
				nestLevel++;
				if (col0)
					paraIndent++;
				newLine();
				sb.append('(');
				col0 = false;
				break;
			case PgnToken.RIGHT_PAREN:
				sb.append(')');
				nestLevel--;
				pendingNewLine = true;
				break;
			case PgnToken.NAG:
				sb.append(Node.nagStr(Integer.parseInt(token)));
				col0 = false;
				break;
			case PgnToken.SYMBOL: {
				if ((prevType != PgnToken.RIGHT_BRACKET) && !col0)
					sb.append(' ');
				int l0 = sb.length();
				sb.append(token);
				int l1 = sb.length();
				nodeToCharPos.put(node, new NodeInfo(l0, l1));
				if (node == currNode) {
					inMainLine = (nestLevel == 0);
				}
				col0 = false;
				break;
			}
			case PgnToken.COMMENT:
				if (prevType == PgnToken.RIGHT_BRACKET) {
				} else if (nestLevel == 0) {
					nestLevel++;
					newLine();
					nestLevel--;
				} else {
					if ((prevType != PgnToken.LEFT_PAREN) && !col0) {
						sb.append(' ');
					}
				}
				sb.append(token.trim());
				col0 = false;
				if (nestLevel == 0)
					newLine();
				break;
			}
			prevType = type;
		}

		@Override
		public void clear() {
			upToDate = false;
			nodeToCharPos.clear();
		}

		BackgroundColorSpan bgSpan = new BackgroundColorSpan(0xff888888);

		@Override
		public void setCurrent(Node node) {
			sb.removeSpan(bgSpan);
			NodeInfo ni = nodeToCharPos.get(node);
			if (ni != null) {
				bgSpan = new BackgroundColorSpan(0xff888888);
				sb.setSpan(bgSpan, ni.l0, ni.l1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			currNode = node;
		}
	}

	private PGNOptions currViewOptions = null;

	public final Pair<SpannableStringBuilder, Boolean> getMoveListString(PGNOptions options) {
		boolean update = currViewOptions == null;
		if (!update) {
			update |= options.view.comments != currViewOptions.view.comments;
			update |= options.view.variations != currViewOptions.view.variations;
			update |= options.view.nag != currViewOptions.view.nag;
		}
		update |= !gameTextListener.upToDate;
		currViewOptions = new PGNOptions(options);
		if (update) {
			PGNOptions tmpOptions = new PGNOptions();
			tmpOptions.exp.variations   = currViewOptions.view.variations;
			tmpOptions.exp.comments     = currViewOptions.view.comments;
			tmpOptions.exp.nag          = currViewOptions.view.nag;
			tmpOptions.exp.playerAction = false;
			tmpOptions.exp.clockInfo    = false;
			gameTextListener.clear();
			tree.pgnTreeWalker(tmpOptions, gameTextListener);
			gameTextListener.upToDate = true;
		}
        gameTextListener.setCurrent(tree.currentNode);
        return gameTextListener.getData();
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

    private final void handleDrawCmd(String drawCmd) {
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
            	String playerAction = rep ? "draw rep" : "draw 50";
                if (m != null)
                	playerAction += " " + TextIO.moveToString(pos, m, false);
            	addToGameTree(new Move(0, 0, 0), playerAction);
            } else {
                pendingDrawOffer = true;
                if (m != null) {
                    processString(ms);
                }
            }
        } else if (drawCmd.startsWith("offer ")) {
            pendingDrawOffer = true;
            String ms = drawCmd.substring(drawCmd.indexOf(" ") + 1);
            if (TextIO.stringToMove(pos, ms) != null) {
                processString(ms);
            }
        } else if (drawCmd.equals("accept")) {
            if (haveDrawOffer())
            	addToGameTree(new Move(0, 0, 0), "draw accept");
        }
    }
}
