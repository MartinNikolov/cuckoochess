/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.petero.droidfish.gamelogic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.petero.droidfish.engine.ComputerPlayer;

/**
 *
 * @author petero
 */
public class Game {
    protected List<Move> moveList = null;
    protected List<UndoInfo> uiInfoList = null;
    List<Boolean> drawOfferList = null;
    protected int currentMove;
    boolean pendingDrawOffer;
    GameState drawState;
    String drawStateMoveStr; // Move required to claim DRAW_REP or DRAW_50
    GameState resignState;
    public Position pos = null;
    private ComputerPlayer computerPlayer;

    public Game(ComputerPlayer computerPlayer) {
        this.computerPlayer = computerPlayer;
        handleCommand("new");
    }

    /**
     * Update the game state according to move/command string from a player.
     * @param str The move or command to process.
     * @return True if str was understood, false otherwise.
     */
    public final boolean processString(String str) {
        if (handleCommand(str)) {
            return true;
        }
        if (getGameState() != GameState.ALIVE) {
            return false;
        }

        Move m = TextIO.stringToMove(pos, str);
        if (m == null) {
            return false;
        }

        UndoInfo ui = new UndoInfo();
        pos.makeMove(m, ui);
        while (currentMove < moveList.size()) {
            moveList.remove(currentMove);
            uiInfoList.remove(currentMove);
            drawOfferList.remove(currentMove);
        }
        moveList.add(m);
        uiInfoList.add(ui);
        drawOfferList.add(pendingDrawOffer);
        pendingDrawOffer = false;
        currentMove++;
        return true;
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
        Move m = null;
        if (currentMove > 0) {
            m = moveList.get(currentMove - 1);
        }
        return m;
    }

    public enum GameState {
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
        if (currentMove > 0) {
            return drawOfferList.get(currentMove - 1);
        } else {
            return false;
        }
    }

    /**
     * Handle a special command.
     * @param moveStr  The command to handle
     * @return  True if command handled, false otherwise.
     */
    protected boolean handleCommand(String moveStr) {
        if (moveStr.equals("new")) {
            moveList = new ArrayList<Move>();
            uiInfoList = new ArrayList<UndoInfo>();
            drawOfferList = new ArrayList<Boolean>();
            currentMove = 0;
            pendingDrawOffer = false;
            drawState = GameState.ALIVE;
            resignState = GameState.ALIVE;
            try {
                pos = TextIO.readFEN(TextIO.startPosFEN);
            } catch (ChessParseError ex) {
                throw new RuntimeException();
            }
            if (computerPlayer != null)
            	computerPlayer.clearTT();
            return true;
        } else if (moveStr.equals("undo")) {
            if (currentMove > 0) {
                pos.unMakeMove(moveList.get(currentMove - 1), uiInfoList.get(currentMove - 1));
                currentMove--;
                pendingDrawOffer = false;
                drawState = GameState.ALIVE;
                resignState = GameState.ALIVE;
                return true;
            } else {
                System.out.println("Nothing to undo");
            }
            return true;
        } else if (moveStr.equals("redo")) {
            if (currentMove < moveList.size()) {
                pos.makeMove(moveList.get(currentMove), uiInfoList.get(currentMove));
                currentMove++;
                pendingDrawOffer = false;
                return true;
            } else {
                System.out.println("Nothing to redo");
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
                resignState = pos.whiteMove ? GameState.RESIGN_WHITE : GameState.RESIGN_BLACK;
                return true;
            } else {
                return true;
            }
        } else if (moveStr.startsWith("setpos ")) {
            String fen = moveStr.substring(moveStr.indexOf(" ") + 1);
            Position newPos = null;
            try {
                newPos = TextIO.readFEN(fen);
            } catch (ChessParseError ex) {
                System.out.printf("Invalid FEN: %s (%s)%n", fen, ex.getMessage());
            }
            if (newPos != null) {
                handleCommand("new");
                pos = newPos;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get game state to save at program exit
     * @return Array of 3 strings: fen, moveList, numUndo.
     */
	public final List<String> getPosHistory() {
        List<String> ret = new ArrayList<String>();

        Position pos = new Position(this.pos);
        for (int i = currentMove; i > 0; i--) {
            pos.unMakeMove(moveList.get(i - 1), uiInfoList.get(i - 1));
        }
        ret.add(TextIO.toFEN(pos)); // Store initial FEN

        StringBuilder moves = new StringBuilder();
        for (int i = 0; i < moveList.size(); i++) {
            Move move = moveList.get(i);
            String strMove = TextIO.moveToString(pos, move, false);
            moves.append(String.format(" %s", strMove));
            UndoInfo ui = new UndoInfo();
            pos.makeMove(move, ui);
        }
        ret.add(moves.toString()); // Store move list string
        int numUndo = moveList.size() - currentMove;
        ret.add(((Integer)numUndo).toString());
        return ret;
	}

    public final String getMoveListString(boolean compressed) {
        StringBuilder ret = new StringBuilder();

        // Undo all moves in move history.
        Position pos = new Position(this.pos);
        for (int i = currentMove; i > 0; i--) {
            pos.unMakeMove(moveList.get(i - 1), uiInfoList.get(i - 1));
        }

        // Print all moves
        String whiteMove = "";
        String blackMove = "";
        for (int i = 0; i < currentMove; i++) {
            Move move = moveList.get(i);
            String strMove = TextIO.moveToString(pos, move, false);
            if (drawOfferList.get(i)) {
                strMove += " (d)";
            }
            if (pos.whiteMove) {
                whiteMove = strMove;
            } else {
                blackMove = strMove;
                if (whiteMove.length() == 0) {
                    whiteMove = "...";
                }
                if (compressed) {
                    ret.append(String.format("%d. %s %s ",
                            pos.fullMoveCounter, whiteMove, blackMove));
                } else {
                    ret.append(String.format("%3d.  %-10s %-10s%n",
                            pos.fullMoveCounter, whiteMove, blackMove));
                }
                whiteMove = "";
                blackMove = "";
            }
            UndoInfo ui = new UndoInfo();
            pos.makeMove(move, ui);
        }
        if ((whiteMove.length() > 0) || (blackMove.length() > 0)) {
            if (whiteMove.length() == 0) {
                whiteMove = "...";
            }
            if (compressed) {
                ret.append(String.format("%d. %s %s ",
                        pos.fullMoveCounter, whiteMove, blackMove));
            } else {
                ret.append(String.format("%3d.  %-8s %-8s%n",
                        pos.fullMoveCounter, whiteMove, blackMove));
            }
        }
        String gameResult = getPGNResultString();
        if (!gameResult.equals("*")) {
            if (compressed) {
                ret.append(gameResult);
            } else {
                ret.append(String.format("%s%n", gameResult));
            }
        }
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
    public final TwoReturnValues<Position, ArrayList<Move>> getUCIHistory() {
        ArrayList<Move> mList = new ArrayList<Move>();
        Position pos = new Position(this.pos);
        for (int i = currentMove; i > 0; i--) {
            if (pos.halfMoveClock == 0)
                break;
            Move m = moveList.get(i - 1);
            pos.unMakeMove(m, uiInfoList.get(i- 1));
            mList.add(new Move(m));
        }
        Collections.reverse(mList);
        return new TwoReturnValues<Position, ArrayList<Move>>(pos, mList);
    }

    private final boolean handleDrawCmd(String drawCmd) {
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
                List<Position> oldPositions = new ArrayList<Position>();
                if (m != null) {
                    UndoInfo ui = new UndoInfo();
                    Position tmpPos = new Position(pos);
                    tmpPos.makeMove(m, ui);
                    oldPositions.add(tmpPos);
                }
                oldPositions.add(pos);
                Position tmpPos = pos;
                for (int i = currentMove - 1; i >= 0; i--) {
                    tmpPos = new Position(tmpPos);
                    tmpPos.unMakeMove(moveList.get(i), uiInfoList.get(i));
                    oldPositions.add(tmpPos);
                }
                int repetitions = 0;
                Position firstPos = oldPositions.get(0);
                for (Position p : oldPositions) {
                    if (p.drawRuleEquals(firstPos))
                        repetitions++;
                }
                if (repetitions >= 3) {
                    valid = true;
                }
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

     private boolean insufficientMaterial() {
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
