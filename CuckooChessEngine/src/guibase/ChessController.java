/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package guibase;

import chess.ChessParseError;
import chess.ComputerPlayer;
import chess.Game;
import chess.HumanPlayer;
import chess.Move;
import chess.MoveGen;
import chess.Piece;
import chess.Player;
import chess.Position;
import chess.Search;
import chess.TextIO;
import chess.UndoInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * The glue between the chess engine and the GUI.
 * @author petero
 */
public class ChessController {
    Player humanPlayer;
    ComputerPlayer computerPlayer;
    Game game;
    GUIInterface gui;
    boolean humanIsWhite;
    Thread computerThread;

    // Search statistics
    String thinkingPV;

    class SearchListener implements Search.Listener {
        int currDepth = 0;
        int currMoveNr = 0;
        String currMove = "";
        int currNodes = 0;
        int currNps = 0;
        int currTime = 0;

        int pvDepth = 0;
        int pvScore = 0;
        int pvTime = 0;
        int pvNodes = 0;
        boolean pvIsMate = false;
        boolean pvUpperBound = false;
        boolean pvLowerBound = false;
        String pvStr = "";

        private void setSearchInfo() {
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
            buf.append(String.format("%n"));
            buf.append(String.format("d:%d %d:%s t:%.2f n:%d nps:%d", currDepth,
                    currMoveNr, currMove, currTime / 1000.0, currNodes, currNps));
            final String newPV = buf.toString();
            gui.runOnUIThread(new Runnable() {
                public void run() {
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
            pvTime = currTime = time;
            pvNodes = currNodes = nodes;
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
    }
    SearchListener listener;
    
    public ChessController(GUIInterface gui) {
        this.gui = gui;
        listener = new SearchListener();
        thinkingPV = "";
    }

    public final void newGame(boolean humanIsWhite, int ttLogSize, boolean verbose) {
        stopComputerThinking();
        this.humanIsWhite = humanIsWhite;
        humanPlayer = new HumanPlayer();
        computerPlayer = new ComputerPlayer();
        computerPlayer.verbose = verbose;
        computerPlayer.setTTLogSize(ttLogSize);
        computerPlayer.setListener(listener);
        if (humanIsWhite) {
            game = new Game(humanPlayer, computerPlayer);
        } else {
            game = new Game(computerPlayer, humanPlayer);
        }
    }
    public final void startGame() {
        gui.setSelection(-1);
        updateGUI();
        startComputerThinking();
    }
    
    public final void setPosHistory(List<String> posHistStr) {
		try {
			String fen = posHistStr.get(0);
			Position pos = TextIO.readFEN(fen);
			game.processString("new");
			game.pos = pos;
			String[] strArr = posHistStr.get(1).split(" ");
			final int arrLen = strArr.length;
			for (int i = 0; i < arrLen; i++) {
				game.processString(strArr[i]);
			}
			int numUndo = Integer.parseInt(posHistStr.get(2));
			for (int i = 0; i < numUndo; i++) {
				game.processString("undo");
			}
		} catch (ChessParseError e) {
			// Just ignore invalid positions
		}
    }
    
    public final List<String> getPosHistory() {
    	return game.getPosHistory();
    }
    
    public String getFEN() {
    	return TextIO.toFEN(game.pos);
    }
    
    public void setFEN(String fen) throws ChessParseError {
    	Position pos = TextIO.readFEN(fen);
    	game.processString("new");
    	game.pos = pos;
		gui.setSelection(-1);
		updateGUI();
		startComputerThinking();
    }
    
    public String getPGN() {
    	List<String> posHist = getPosHistory();
    	String fen = posHist.get(0);
        String moves = game.getMoveListString(true);
    	String pgn = "";
    	pgn += String.format("[Date \"%s\"]%n", "xxxx.xx.xx"); // FIXME!!! Compute real date
    	String white = "Player";
    	String black = ComputerPlayer.engineName;
    	if (!humanIsWhite) {
    		String tmp = white; white = black; black = tmp;
    	}
    	pgn += String.format("[White \"%s\"]%n", white);
    	pgn += String.format("[Black \"%s\"]%n", black);
    	pgn += String.format("[Result \"%s\"]%n", game.getPGNResultString());
    	if (!fen.equals(TextIO.startPosFEN)) {
    		pgn += String.format("[FEN \"%s\"]%n", fen);
    		pgn += "[SetUp \"1\"]\n";
    	}
    	pgn += "\n";
    	pgn += moves;
    	return pgn;
    }
    
    public final boolean humansTurn() {
        return game.pos.whiteMove == humanIsWhite;
    }

    public final void takeBackMove() {
        if (humansTurn()) {
            if (game.getLastMove() != null) {
                game.processString("undo");
                if (game.getLastMove() != null) {
                    game.processString("undo");
                } else {
                    game.processString("redo");
                }
                updateGUI();
                setSelection();
            }
        } else if (game.getGameState() != Game.GameState.ALIVE) {
            if (game.getLastMove() != null) {
                game.processString("undo");
                if (!humansTurn()) {
                	if (game.getLastMove() != null) {
                		game.processString("undo");
                	} else {
                		game.processString("redo");
                	}
                }
                updateGUI();
                setSelection();
            }
        }
    }

    public final void redoMove() {
        if (humansTurn()) {
            game.processString("redo");
            game.processString("redo");
            updateGUI();
            setSelection();
        }
    }

    public final void humanMove(Move m) {
        if (humansTurn()) {
            if (doMove(m)) {
                updateGUI();
                startComputerThinking();
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
        humanMove(m);
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
        return false;
    }


    final private void updateGUI() {
        setStatusString();
        setMoveList();
        setThinkingPV();
        gui.setPosition(game.pos);
    }

    final private void setStatusString() {
        String str = game.pos.whiteMove ? "White's move" : "Black's move";
        if (computerThread != null) str += " (thinking)";
        if (game.getGameState() != Game.GameState.ALIVE) {
            str = game.getGameStateString();
        }
        gui.setStatusString(str);
    }

    public final void setMoveList() {
        String str = game.getMoveListString(true);
        gui.setMoveListString(str);
    }
    
    public final void setThinkingPV() {
    	String str = new String();
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

    
    private void startComputerThinking() {
        if (game.pos.whiteMove != humanIsWhite) {
            if (computerThread == null) {
                computerThread = new Thread(new Runnable() {
                   public void run() {
                       computerPlayer.timeLimit(gui.timeLimit(), gui.timeLimit(), gui.randomMode());
                       final String cmd = computerPlayer.getCommand(new Position(game.pos),
                               game.haveDrawOffer(), game.getHistory());
                       gui.runOnUIThread(new Runnable() {
                           public void run() {
                               game.processString(cmd);
                               thinkingPV = "";
                               updateGUI();
                               setSelection();
                               stopComputerThinking();
                           }
                       });
                   }
                });
                thinkingPV = "";
                updateGUI();
                computerThread.start();
            }
        }
    }

    private synchronized void stopComputerThinking() {
        if (computerThread != null) {
            computerPlayer.timeLimit(0, 0, false);
            try {
                computerThread.join();
            } catch (InterruptedException ex) {
                System.out.printf("Could not stop thread%n");
            }
            computerThread = null;
            updateGUI();
        }
    }

    public synchronized void setTimeLimit() {
        if (computerThread != null) {
            computerPlayer.timeLimit(gui.timeLimit(), gui.timeLimit(), gui.randomMode());
        }
    }
}
