/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.petero.droidfish.gamelogic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Scanner;

import org.petero.droidfish.GUIInterface;
import org.petero.droidfish.GameMode;
import org.petero.droidfish.engine.ComputerPlayer;
import org.petero.droidfish.gamelogic.Game.GameState;

/**
 * The glue between the chess engine and the GUI.
 * @author petero
 */
public class ChessController {
    private ComputerPlayer computerPlayer = null;
    private String bookFileName = "";
    private Game game;
    private GUIInterface gui;
    private GameMode gameMode;
    private Thread computerThread;
    private Thread analysisThread;

    private int timeControl;
	private int movesPerSession;
	private int timeIncrement;
    
    // Search statistics
    private String thinkingPV;

    class SearchListener implements org.petero.droidfish.gamelogic.SearchListener {
        private int currDepth = 0;
        private int currMoveNr = 0;
        private String currMove = "";
        private int currNodes = 0;
        private int currNps = 0;
        private int currTime = 0;

        private int pvDepth = 0;
        private int pvScore = 0;
        private boolean pvIsMate = false;
        private boolean pvUpperBound = false;
        private boolean pvLowerBound = false;
        private String bookInfo = "";
        private String pvStr = "";

        private final void setSearchInfo() {
            StringBuilder buf = new StringBuilder();
            buf.append(String.format("%n[%d] ", pvDepth));
            if (pvUpperBound) {
                buf.append("<=");
            } else if (pvLowerBound) {
                buf.append(">=");
            }
            if (pvIsMate) {
                buf.append(String.format("m%d", pvScore));
            } else {
                buf.append(String.format("%.2f", pvScore / 100.0));
            }
            buf.append(pvStr);
            buf.append("\n");
            buf.append(String.format("d:%d %d:%s t:%.2f n:%d nps:%d", currDepth,
                    currMoveNr, currMove, currTime / 1000.0, currNodes, currNps));
            if (bookInfo.length() > 0) {
            	buf.append("\n");
            	buf.append(bookInfo);
            }
            final String newPV = buf.toString();
			final SearchStatus localSS = ss;
            gui.runOnUIThread(new Runnable() {
                public void run() {
                	if (!localSS.searchResultWanted)
                		return;
                    thinkingPV = newPV;
                    setThinkingPV();
                }
            });
        }

        public void notifyDepth(int depth) {
            currDepth = depth;
            setSearchInfo();
        }

        public void notifyCurrMove(Move m, int moveNr) {
            currMove = TextIO.moveToString(new Position(game.pos), m, false);
            currMoveNr = moveNr;
            setSearchInfo();
        }

        public void notifyPV(int depth, int score, int time, int nodes, int nps, boolean isMate,
                boolean upperBound, boolean lowerBound, ArrayList<Move> pv) {
            pvDepth = depth;
            pvScore = score;
            currTime = time;
            currNodes = nodes;
            currNps = nps;
            pvIsMate = isMate;
            pvUpperBound = upperBound;
            pvLowerBound = lowerBound;

            StringBuilder buf = new StringBuilder();
            Position pos = new Position(game.pos);
            UndoInfo ui = new UndoInfo();
            for (Move m : pv) {
                buf.append(String.format(" %s", TextIO.moveToString(pos, m, false)));
                pos.makeMove(m, ui);
            }
            pvStr = buf.toString();
            setSearchInfo();
        }

        public void notifyStats(int nodes, int nps, int time) {
            currNodes = nodes;
            currNps = nps;
            currTime = time;
            setSearchInfo();
        }

		@Override
		public void notifyBookInfo(String bookInfo) {
			this.bookInfo = bookInfo;
			setSearchInfo();
		}
    }
    SearchListener listener;
    
    public ChessController(GUIInterface gui) {
        this.gui = gui;
        listener = new SearchListener();
        thinkingPV = "";
    }

	public final void setBookFileName(String bookFileName) {
		if (!this.bookFileName.equals(bookFileName)) {
			this.bookFileName = bookFileName;
			if (computerPlayer != null) {
				computerPlayer.setBookFileName(bookFileName);
				if (analysisThread != null) {
					stopAnalysis();
					startAnalysis();
				}
				updateBookHints();
			}
		}
	}
	
	public final void updateBookHints() {
		if (gameMode != null) {
			boolean analysis = gameMode.analysisMode();
			boolean computersTurn = !gameMode.humansTurn(game.pos.whiteMove);
			thinkingPV = "";
			if (!analysis && !computersTurn && gui.showBookHints()) {
				thinkingPV = computerPlayer.getBookHints(game.pos);
			}
			setThinkingPV();
		}
	}

    private final static class SearchStatus {
    	boolean searchResultWanted = true;
    }
    SearchStatus ss = new SearchStatus();
    
    public final void newGame(GameMode gameMode) {
        ss.searchResultWanted = false;
        stopComputerThinking();
        stopAnalysis();
        this.gameMode = gameMode;
        if (computerPlayer == null) {
        	computerPlayer = new ComputerPlayer();
        	computerPlayer.setListener(listener);
        	computerPlayer.setBookFileName(bookFileName);
        }
       	game = new Game(computerPlayer, timeControl, movesPerSession, timeIncrement);
       	updateGamePaused();
    }

    public final void startGame() {
        updateComputeThreads(true);
        setSelection(); 
        updateGUI();
       	updateGamePaused();
    }
    
    private boolean guiPaused = false;
    public final void setGuiPaused(boolean paused) {
    	guiPaused = paused;
    	updateGamePaused();
    }

    private final void updateGamePaused() {
    	if (game != null) {
    		boolean gamePaused = gameMode.analysisMode() || (humansTurn() && guiPaused);
    		game.setGamePaused(gamePaused);
    		updateRemainingTime();
    	}
    }
    
    private final void updateComputeThreads(boolean clearPV) {
    	boolean analysis = gameMode.analysisMode();
    	boolean computersTurn = !gameMode.humansTurn(game.pos.whiteMove);
    	if (!analysis)
    		stopAnalysis();
    	if (!computersTurn)
    		stopComputerThinking();
    	if (clearPV) {
			thinkingPV = "";
    		if (!analysis && !computersTurn && gui.showBookHints()) {
    			thinkingPV = computerPlayer.getBookHints(game.pos);
    		}
    	}
        if (analysis)
        	startAnalysis();
        if (computersTurn)
            startComputerThinking();
    }

    /** Set game mode. */
	public final void setGameMode(GameMode newMode) {
		if (!gameMode.equals(newMode)) {
			if (newMode.humansTurn(game.pos.whiteMove))
				ss.searchResultWanted = false;
			gameMode = newMode;
			updateGamePaused();
			updateComputeThreads(true);
			updateGUI();
		}
	}

	public final void setPosHistory(List<String> posHistStr) {
		try {
			String fen = posHistStr.get(0);
			Position pos = TextIO.readFEN(fen);
			game.processString("new");
			game.setPos(pos);
			String[] strArr = posHistStr.get(1).split(" ");
			final int arrLen = strArr.length;
			for (int i = 0; i < arrLen; i++) {
				game.processString(strArr[i]);
			}
			int numUndo = Integer.parseInt(posHistStr.get(2));
			for (int i = 0; i < numUndo; i++) {
				game.processString("undo");
			}
			String clockState = posHistStr.get(3);
			game.timeController.restoreState(clockState);
		} catch (ChessParseError e) {
			// Just ignore invalid positions
		}
    }
    
    public final List<String> getPosHistory() {
    	List<String> ret = game.getPosHistory();
    	ret.add(game.timeController.saveState());
    	return ret;
    }
    
    public final String getFEN() {
    	return TextIO.toFEN(game.pos);
    }
    
    /** Convert current game to PGN format. */
    public final String getPGN() {
    	StringBuilder pgn = new StringBuilder();
    	List<String> posHist = getPosHistory();
    	String fen = posHist.get(0);
        String moves = game.getMoveListString();
        if (game.getGameState() == GameState.ALIVE)
        	moves += " *";
    	int year, month, day;
    	{
    		Calendar now = GregorianCalendar.getInstance();
    		year = now.get(Calendar.YEAR);
    		month = now.get(Calendar.MONTH) + 1;
    		day = now.get(Calendar.DAY_OF_MONTH);
    	}
    	pgn.append(String.format("[Date \"%04d.%02d.%02d\"]%n", year, month, day));
    	String engine = ComputerPlayer.engineName;
    	String white = gameMode.playerWhite() ? "Player" : engine;
    	String black = gameMode.playerBlack() ? "Player" : engine;
    	pgn.append(String.format("[White \"%s\"]%n", white));
    	pgn.append(String.format("[Black \"%s\"]%n", black));
    	pgn.append(String.format("[Result \"%s\"]%n", game.getPGNResultString()));
    	if (!fen.equals(TextIO.startPosFEN)) {
    		pgn.append(String.format("[FEN \"%s\"]%n", fen));
    		pgn.append("[SetUp \"1\"]\n");
    	}
    	pgn.append("\n");
		String[] strArr = moves.split(" ");
    	int currLineLength = 0;
		final int arrLen = strArr.length;
		for (int i = 0; i < arrLen; i++) {
			String move = strArr[i].trim();
			int moveLen = move.length();
			if (moveLen > 0) {
				if (currLineLength + 1 + moveLen >= 80) {
					pgn.append("\n");
					pgn.append(move);
					currLineLength = moveLen;
				} else {
					if (currLineLength > 0) {
						pgn.append(" ");
						currLineLength++;
					}
					pgn.append(move);
					currLineLength += moveLen;
				}
			}
		}
    	pgn.append("\n\n");
    	return pgn.toString();
    }

    public final void setFENOrPGN(String fenPgn) throws ChessParseError {
       	Game newGame = new Game(null, timeControl, movesPerSession, timeIncrement);
    	try {
    		Position pos = TextIO.readFEN(fenPgn);
    		newGame.setPos(pos);
    	} catch (ChessParseError e) {
    		// Try read as PGN instead
    		if (!setPGN(newGame, fenPgn)) {
    			throw e;
    		}
    	}
    	ss.searchResultWanted = false;
    	game = newGame;
    	game.setComputerPlayer(computerPlayer);
    	updateGamePaused();
    	stopAnalysis();
    	stopComputerThinking();
    	computerPlayer.clearTT();
		updateComputeThreads(true);
		gui.setSelection(-1);
		updateGUI();
    }

    private final boolean setPGN(Game newGame, String pgn) throws ChessParseError {
    	boolean anythingParsed = false;
    	// First pass, remove comments
    	{
    		StringBuilder out = new StringBuilder();
    		Scanner sc = new Scanner(pgn);
    		sc.useDelimiter("");
    		while (sc.hasNext()) {
    			String c = sc.next();
    			if (c.equals("{")) {
    				sc.skip("[^}]*\\}");
    			} else if (c.equals(";")) {
    				sc.skip("[^\n]*\n");
    			} else {
    				out.append(c);
    			}
    		}
    		pgn = out.toString();
    	}

    	// Parse tag section
    	Position pos = TextIO.readFEN(TextIO.startPosFEN);
    	Scanner sc = new Scanner(pgn);
    	sc.useDelimiter("\\s+");
    	while (sc.hasNext("\\[.*")) {
    		anythingParsed = true;
    		String tagName = sc.next();
    		if (tagName.length() > 1) {
    			tagName = tagName.substring(1);
    		} else {
    			tagName = sc.next();
    		}
    		String tagValue = sc.findWithinHorizon(".*\\]", 0);
    		tagValue = tagValue.trim();
    		if (tagValue.charAt(0) == '"')
    			tagValue = tagValue.substring(1);
    		if (tagValue.charAt(tagValue.length()-1) == ']')
    			tagValue = tagValue.substring(0, tagValue.length() - 1);
    		if (tagValue.charAt(tagValue.length()-1) == '"')
    			tagValue = tagValue.substring(0, tagValue.length() - 1);
    		if (tagName.equals("FEN")) {
    			pos = TextIO.readFEN(tagValue);
    		}
    	}
    	newGame.setPos(pos);

    	// Handle (ignore) recursive annotation variations
    	{
    		StringBuilder out = new StringBuilder();
    		sc.useDelimiter("");
    		int level = 0;
    		while (sc.hasNext()) {
    			String c = sc.next();
    			if (c.equals("(")) {
    				level++;
    			} else if (c.equals(")")) {
    				level--;
    			} else if (level == 0) {
    				out.append(c);
    			}
    		}
    		pgn = out.toString();
    	}

    	// Parse move text section
    	sc = new Scanner(pgn);
    	sc.useDelimiter("\\s+");
    	while (sc.hasNext()) {
    		String strMove = sc.next();
    		strMove = strMove.replaceFirst("\\$?[0-9]*\\.*([^?!]*)[?!]*", "$1");
    		if (strMove.length() == 0) continue;
    		Move m = TextIO.stringToMove(newGame.pos, strMove);
    		if (m == null)
    			break;
    		newGame.processString(strMove);
    		anythingParsed = true;
    	}
    	return anythingParsed;
    }

    /** True if human's turn to make a move. (True in analysis mode.) */
    public final boolean humansTurn() {
    	return gameMode.humansTurn(game.pos.whiteMove);
    }

    /** Return true if computer player is using CPU power. */
    public final boolean computerBusy() {
    	if (game.getGameState() != GameState.ALIVE)
    		return false;
    	return gameMode.analysisMode() || !humansTurn();
    }

    private final void undoMoveNoUpdate() {
    	if (game.getLastMove() != null) {
    		ss.searchResultWanted = false;
    		game.processString("undo");
    		if (!humansTurn()) {
    			if (game.getLastMove() != null) {
    				game.processString("undo");
    				if (!humansTurn()) {
    					game.processString("redo");
    				}
    			} else {
    				// Don't undo first white move if playing black vs computer,
    				// because that would cause computer to immediately make
    				// a new move and the whole redo history will be lost.
    				game.processString("redo");
    			}
    		}
    	}
    }
    
    public final void undoMove() {
    	if (game.getLastMove() != null) {
        	undoMoveNoUpdate();
    		stopAnalysis();
			stopComputerThinking();
    		updateComputeThreads(true);
    		setSelection();
    		updateGUI();
    	}
    }

    private final void redoMoveNoUpdate() {
    	if (game.canRedoMove()) {
    		ss.searchResultWanted = false;
    		game.processString("redo");
    		if (!humansTurn() && game.canRedoMove()) {
    			game.processString("redo");
    			if (!humansTurn())
    				game.processString("undo");
    		}
    	}
    }

    public final boolean canRedoMove() {
    	return game.canRedoMove();
    }
    
    public final void redoMove() {
    	if (canRedoMove()) {
    		redoMoveNoUpdate();
    		stopAnalysis();
			stopComputerThinking();
    		updateComputeThreads(true);
    		setSelection();
    		updateGUI();
    	}
    }

	public final void gotoMove(int moveNr) {
		boolean needUpdate = false;
		while (game.pos.fullMoveCounter > moveNr) { // Go backward
			int before = game.currentMove;
			undoMoveNoUpdate();
			int after = game.currentMove;
			if (after >= before)
				break;
			needUpdate = true;
		}
		while (game.pos.fullMoveCounter < moveNr) { // Go forward
			int before = game.currentMove;
			redoMoveNoUpdate();
			int after = game.currentMove;
			if (after <= before)
				break;
			needUpdate = true;
		}
		if (needUpdate) {
    		stopAnalysis();
			stopComputerThinking();
    		updateComputeThreads(true);
    		setSelection();
    		updateGUI();
		}
	}


    public final void makeHumanMove(Move m) {
        if (humansTurn()) {
            if (doMove(m)) {
            	ss.searchResultWanted = false;
                stopAnalysis();
    			stopComputerThinking();
                updateComputeThreads(true);
                updateGUI();
            } else {
                gui.setSelection(-1);
            }
        }
    }

    Move promoteMove;
    public final void reportPromotePiece(int choice) {
    	final boolean white = game.pos.whiteMove;
    	int promoteTo;
        switch (choice) {
            case 1:
                promoteTo = white ? Piece.WROOK : Piece.BROOK;
                break;
            case 2:
                promoteTo = white ? Piece.WBISHOP : Piece.BBISHOP;
                break;
            case 3:
                promoteTo = white ? Piece.WKNIGHT : Piece.BKNIGHT;
                break;
            default:
                promoteTo = white ? Piece.WQUEEN : Piece.BQUEEN;
                break;
        }
        promoteMove.promoteTo = promoteTo;
        Move m = promoteMove;
        promoteMove = null;
        makeHumanMove(m);
    }

    /**
     * Move a piece from one square to another.
     * @return True if the move was legal, false otherwise.
     */
    final private boolean doMove(Move move) {
        Position pos = game.pos;
        ArrayList<Move> moves = new MoveGen().pseudoLegalMoves(pos);
        moves = MoveGen.removeIllegal(pos, moves);
        int promoteTo = move.promoteTo;
        for (Move m : moves) {
            if ((m.from == move.from) && (m.to == move.to)) {
                if ((m.promoteTo != Piece.EMPTY) && (promoteTo == Piece.EMPTY)) {
                	promoteMove = m;
                	gui.requestPromotePiece();
                	return false;
                }
                if (m.promoteTo == promoteTo) {
                    String strMove = TextIO.moveToString(pos, m, false);
                    game.processString(strMove);
                    return true;
                }
            }
        }
    	gui.reportInvalidMove(move);
        return false;
    }

    final private void updateGUI() {
        String str = game.pos.whiteMove ? "White's move" : "Black's move";
        if (computerThread != null) str += " (thinking)";
        if (analysisThread != null) str += " (analyzing)";
        if (game.getGameState() != Game.GameState.ALIVE) {
            str = game.getGameStateString();
        }
        gui.setStatusString(str);
        gui.setMoveListString(game.getMoveListString());
        setThinkingPV();
        gui.setPosition(game.pos);
        updateRemainingTime();
    }

    final public void updateRemainingTime() {
        // Update remaining time
        long now = System.currentTimeMillis();
        long wTime = game.timeController.getRemainingTime(true, now);
        long bTime = game.timeController.getRemainingTime(false, now);
        long nextUpdate = 0;
        if (game.timeController.clockRunning()) {
        	long t = game.pos.whiteMove ? wTime : bTime;
        	nextUpdate = (t % 1000);
        	if (nextUpdate < 0) nextUpdate += 1000;
        	nextUpdate += 1;
        }
        gui.setRemainingTime(wTime, bTime, nextUpdate);
    }

    private final void setThinkingPV() {
    	String str = "";
    	if (gui.showThinking()) {
            str = thinkingPV;
        }
        gui.setThinkingString(str);
    }

    final private void setSelection() {
        Move m = game.getLastMove();
        int sq = (m != null) ? m.to : -1;
        gui.setSelection(sq);
    }

    private final synchronized void startComputerThinking() {
    	if (analysisThread != null) return;
    	if (game.getGameState() != GameState.ALIVE) return;
    	if (computerThread == null) {
    		ss = new SearchStatus();
			final TwoReturnValues<Position, ArrayList<Move>> ph = game.getUCIHistory();
			final Game g = game;
			final boolean haveDrawOffer = g.haveDrawOffer();
			final Position currPos = new Position(g.pos);
			long now = System.currentTimeMillis();
			final int wTime = game.timeController.getRemainingTime(true, now);
			final int bTime = game.timeController.getRemainingTime(false, now);
			final int inc = game.timeController.getIncrement();
			final int movesToGo = game.timeController.getMovesToTC();
    		computerThread = new Thread(new Runnable() {
    			public void run() {
    				final String cmd = computerPlayer.doSearch(ph.first, ph.second, currPos, haveDrawOffer,
    														   wTime, bTime, inc, movesToGo);
    				final SearchStatus localSS = ss;
    				gui.runOnUIThread(new Runnable() {
    					public void run() {
    						if (!localSS.searchResultWanted)
    							return;
    						g.processString(cmd);
    						updateGamePaused();
    						gui.computerMoveMade();
    						thinkingPV = "";
    						stopComputerThinking();
    						stopAnalysis(); // To force analysis to restart for new position
    						updateComputeThreads(true);
    						setSelection();
    						updateGUI();
    					}
    				});
    			}
    		});
    		thinkingPV = "";
    		computerThread.start();
    		updateGUI();
        }
    }

    private final synchronized void stopComputerThinking() {
        if (computerThread != null) {
            computerPlayer.stopSearch();
            try {
                computerThread.join();
            } catch (InterruptedException ex) {
                System.out.printf("Could not stop computer thread%n");
            }
            computerThread = null;
            updateGUI();
        }
    }

    private final synchronized void startAnalysis() {
    	if (gameMode.analysisMode()) {
    		if (computerThread != null) return;
            if (analysisThread == null) {
        		ss = new SearchStatus();
    			final TwoReturnValues<Position, ArrayList<Move>> ph = game.getUCIHistory();
    			final boolean haveDrawOffer = game.haveDrawOffer();
    			final Position currPos = new Position(game.pos);
            	analysisThread = new Thread(new Runnable() {
            		public void run() {
            			computerPlayer.analyze(ph.first, ph.second, currPos, haveDrawOffer);
            		}
            	});
            	thinkingPV = "";
                analysisThread.start();
                updateGUI();
            }
        }
    }
    private final synchronized void stopAnalysis() {
        if (analysisThread != null) {
            computerPlayer.stopSearch();
            try {
                analysisThread.join();
            } catch (InterruptedException ex) {
                System.out.printf("Could not stop analysis thread%n");
            }
            analysisThread = null;
            thinkingPV = "";
            updateGUI();
        }
    }

    public final synchronized void setTimeLimit(int time, int moves, int inc) {
    	timeControl = time;
    	movesPerSession = moves;
    	timeIncrement = inc;
    	if (game != null)
    		game.timeController.setTimeControl(timeControl, movesPerSession, timeIncrement);
    }

    public final void shutdownEngine() {
    	gameMode = new GameMode(GameMode.TWO_PLAYERS);
    	stopComputerThinking();
    	stopAnalysis();
    	computerPlayer.shutdownEngine();
    }

	/** Help human to claim a draw by trying to find and execute a valid draw claim. */
    public final boolean claimDrawIfPossible() {
    	if (!findValidDrawClaim())
    		return false;
    	updateGUI();
    	return true;
    }

    private final boolean findValidDrawClaim() {
    	if (game.getGameState() != GameState.ALIVE) return true;
		game.processString("draw accept");
		if (game.getGameState() != GameState.ALIVE) return true;
		game.processString("draw rep");
		if (game.getGameState() != GameState.ALIVE) return true;
		game.processString("draw 50");
		if (game.getGameState() != GameState.ALIVE) return true;
        return false;
	}
}
