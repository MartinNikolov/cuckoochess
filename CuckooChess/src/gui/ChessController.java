/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gui;

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
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * The glue between the chess engine and the GUI.
 * @author petero
 */
public class ChessController {
    Player humanPlayer;
    ComputerPlayer computerPlayer;
    Game game;
    AppletGUI gui;
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
            buf.append(String.format("d:%d %d:%s t:%.2f n:%d nps:%d%n", currDepth,
                    currMoveNr, currMove, currTime / 1000.0, currNodes, currNps));
            final String newPV = buf.toString();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    thinkingPV = newPV;
                    setMoveList();
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
                boolean upperBound, boolean lowerBound, List<Move> pv) {
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
    
    ChessController(AppletGUI gui) {
        this.gui = gui;
        listener = new SearchListener();
        thinkingPV = "";
    }

    final void newGame(boolean humanIsWhite) {
        stopComputerThinking();
        this.humanIsWhite = humanIsWhite;
        humanPlayer = new HumanPlayer();
        computerPlayer = new ComputerPlayer();
        computerPlayer.setListener(listener);
        if (humanIsWhite) {
            game = new Game(humanPlayer, computerPlayer);
        } else {
            game = new Game(computerPlayer, humanPlayer);
        }
        gui.setSelection(-1);
        updateGUI();
        startComputerThinking();
    }

    final boolean humansTurn() {
        return game.pos.isWhiteMove() == humanIsWhite;
    }

    final void takeBackMove() {
        if (humansTurn()) {
            if (game.getLastMove() != null) {
                game.processString("undo");
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

    final void redoMove() {
        if (humansTurn()) {
            game.processString("redo");
            game.processString("redo");
            updateGUI();
            setSelection();
        }
    }

    final void humanMove(Move m) {
        if (humansTurn()) {
            if (doMove(m.from, m.to)) {
                updateGUI();
                startComputerThinking();
            } else {
                gui.setSelection(-1);
            }
        }
    }

    /**
     * Move a piece from one square to another.
     * @return True if the move was legal, false otherwise.
     */
    final private boolean doMove(int from, int to) {
        Position pos = game.pos;
        final boolean white = pos.isWhiteMove();
        List<Move> moves = new MoveGen().pseudoLegalMoves(pos);
        moves = MoveGen.removeIllegal(pos, moves);
        int promoteTo = Piece.EMPTY;
        for (Move m : moves) {
            if ((m.from == from) && (m.to == to)) {
                if ((m.promoteTo != Piece.EMPTY) && (promoteTo == Piece.EMPTY)) {
                    int choice = gui.getPromotePiece();
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
        gui.setPosition(game.pos);
    }

    final private void setStatusString() {
        String str = game.pos.isWhiteMove() ? "White's move" : "Black's move";
        if (game.getGameState() != Game.GameState.ALIVE) {
            str = game.getGameStateString();
        }
        gui.setStatusString(str);
    }

    final void setMoveList() {
        String str = game.getMoveListString(true);
        str += String.format("%n");
        if ((thinkingPV.length() > 0) && gui.showThinking()) {
            str += String.format("%s%n", thinkingPV);
        }
        gui.setMoveListString(str);
    }

    final private void setSelection() {
        Move m = game.getLastMove();
        int sq = (m != null) ? m.to : -1;
        gui.setSelection(sq);
    }

    
    private void startComputerThinking() {
        if (game.pos.isWhiteMove() != humanIsWhite) {
            if (computerThread == null) {
                computerThread = new Thread(new Runnable() {
                   public void run() {
                       computerPlayer.timeLimit(gui.timeLimit(), gui.timeLimit());
                       final String cmd = computerPlayer.getCommand(new Position(game.pos),
                               game.haveDrawOffer(), game.getHistory());
                       SwingUtilities.invokeLater(new Runnable() {
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
                computerThread.start();
            }
        }
    }

    private synchronized void stopComputerThinking() {
        if (computerThread != null) {
            computerPlayer.timeLimit(0, 0);
            try {
                computerThread.join();
            } catch (InterruptedException ex) {
                System.out.printf("Could not stop thread%n");
            }
            computerThread = null;
        }
    }

    public synchronized void setTimeLimit() {
        if (computerThread != null) {
            computerPlayer.timeLimit(gui.timeLimit(), gui.timeLimit());
        }
    }
}
