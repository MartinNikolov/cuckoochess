/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uci.UCIProtocol;

/**
 *
 * @author petero
 */
public class Game {
    List<Move> moveList = null;
    List<UndoInfo> uiInfoList = null;
    List<Boolean> drawOfferList = null;
    int currentMove;
    boolean pendingDrawOffer;
    GameState drawState;
    GameState resignState;
    public Position pos = null;
    Player whitePlayer;
    Player blackPlayer;
    
    public Game(Player whitePlayer, Player blackPlayer) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        handleCommand("new");
    }

    /**
     * Administrate a game between two players, human or computer.
     */
    public void play() throws IOException {
        handleCommand("new");
        while (true) {
            // Print last move
            if (currentMove > 0) {
                Position prevPos = new Position(pos);
                prevPos.unMakeMove(moveList.get(currentMove - 1), uiInfoList.get(currentMove - 1));
                String moveStr= TextIO.moveToString(prevPos, moveList.get(currentMove - 1), false);
                if (haveDrawOffer()) {
                    moveStr += " (offer draw)";
                }
                String msg = String.format("Last move: %d%s %s",
                        prevPos.fullMoveCounter, prevPos.isWhiteMove() ? "." : "...",
                        moveStr);
                System.out.println(msg);
            }
//            System.out.printf("Hash: %016x\n", pos.zobristHash());
            {
                Evaluate eval = new Evaluate();
                int evScore = eval.evalPos(pos) * (pos.isWhiteMove() ? 1 : -1);
                System.out.printf("Eval: %.2f%n", evScore / 100.0);
            }
            
            // Check game state
            System.out.print(TextIO.asciiBoard(pos));
            String stateStr = getGameStateString();
            if (stateStr.length() > 0) {
                System.out.printf("%s%n", stateStr);
            }
            
            if (getGameState() != GameState.ALIVE) {
                activateHumanPlayer();
            }
            
            // Get command from current player and act on it
            Player pl = pos.isWhiteMove() ? whitePlayer : blackPlayer;
            String moveStr = pl.getCommand(new Position(pos), haveDrawOffer(), getHistory());
            if (moveStr.equals("quit")) {
                return;
            } else {
                boolean ok = processString(moveStr);
                if (!ok) {
                    System.out.printf("Invalid move: %s\n", moveStr);
                }
            }
        }
    }

    /**
     * Update the game state according to move/command string from a player.
     * @param str The move or command to process.
     * @return True if str was understood, false otherwise.
     */
    public boolean processString(String str) {
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
                return "Game over, draw by repetition!";
            case DRAW_50:
                return "Game over, draw by 50 move rule!";
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
    public Move getLastMove() {
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
     * Get the current state of the game.
     */
    public GameState getGameState() {
        List<Move> moves = new MoveGen().pseudoLegalMoves(pos);
        moves = MoveGen.removeIllegal(pos, moves);
        if (moves.size() == 0) {
            if (MoveGen.inCheck(pos)) {
                return pos.isWhiteMove() ? GameState.BLACK_MATE : GameState.WHITE_MATE;
            } else {
                return pos.isWhiteMove() ? GameState.WHITE_STALEMATE : GameState.BLACK_STALEMATE;
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
    public boolean haveDrawOffer() {
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
    private boolean handleCommand(String moveStr) {
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
            whitePlayer.clearTT();
            blackPlayer.clearTT();
            activateHumanPlayer();
            return true;
        } else if (moveStr.equals("undo")) {
            if (currentMove > 0) {
                pos.unMakeMove(moveList.get(currentMove - 1), uiInfoList.get(currentMove - 1));
                currentMove--;
                pendingDrawOffer = false;
                drawState = GameState.ALIVE;
                resignState = GameState.ALIVE;
                return handleCommand("swap");
            } else {
                System.out.println("Nothing to undo");
            }
            return true;
        } else if (moveStr.equals("redo")) {
            if (currentMove < moveList.size()) {
                pos.makeMove(moveList.get(currentMove), uiInfoList.get(currentMove));
                currentMove++;
                pendingDrawOffer = false;
                return handleCommand("swap");
            } else {
                System.out.println("Nothing to redo");
            }
            return true;
        } else if (moveStr.equals("swap") || moveStr.equals("go")) {
            Player tmp = whitePlayer;
            whitePlayer = blackPlayer;
            blackPlayer = tmp;
            return true;
        } else if (moveStr.equals("list")) {
            listMoves();
            return true;
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
                activateHumanPlayer();
            }
            return true;
        } else if (moveStr.equals("getpos")) {
            String fen = TextIO.toFEN(pos);
            System.out.println(fen);
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
                resignState = pos.isWhiteMove() ? GameState.RESIGN_WHITE : GameState.RESIGN_BLACK;
                return true;
            } else {
                return true;
            }
        } else if (moveStr.startsWith("testsuite ")) {
            String testSuiteCmd = moveStr.substring(moveStr.indexOf(" ") + 1);
            return handleTestSuite(testSuiteCmd);
        } else if (moveStr.startsWith("book")) {
            String bookCmd = moveStr.substring(moveStr.indexOf(" ") + 1);
            return handleBookCmd(bookCmd);
        } else if (moveStr.startsWith("time")) {
            try {
                String timeStr = moveStr.substring(moveStr.indexOf(" ") + 1);
                int timeLimit = Integer.parseInt(timeStr);
                whitePlayer.timeLimit(timeLimit, timeLimit);
                blackPlayer.timeLimit(timeLimit, timeLimit);
                return true;
            }
            catch (NumberFormatException nfe) {
                System.out.printf("Number format exception: %s\n", nfe.getMessage());
                return false;
            }
        } else if (moveStr.equals("uci")) {
            whitePlayer = null;
            blackPlayer = null;
            UCIProtocol.main(new String[0]);
            System.exit(0);
            return false;
        } else if (moveStr.equals("help")) {
            showHelp();
            return true;
        } else {
            return false;
        }
    }

    /** Swap players around if needed to make the human player in control of the next move. */
    private void activateHumanPlayer() {
        if (!(pos.isWhiteMove() ? whitePlayer : blackPlayer).isHumanPlayer()) {
            Player tmp = whitePlayer;
            whitePlayer = blackPlayer;
            blackPlayer = tmp;
        }
    }
    
    /**
     * Print a list of all moves.
     */
    private void listMoves() {
        String movesStr = getMoveListString(false);
        System.out.printf("%s", movesStr);
    }

    final public String getMoveListString(boolean compressed) {
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
            if (pos.isWhiteMove()) {
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
        String gameResult = "";
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
        if (gameResult.length() > 0) {
            if (compressed) {
                ret.append(gameResult);
            } else {
                ret.append(String.format("%s%n", gameResult));
            }
        }
        return ret.toString();
    }

    /** Return a list of previous positions in this game, back to the last "zeroing" move. */
    public List<Position> getHistory() {
        List<Position> posList = new ArrayList<Position>();
        Position pos = new Position(this.pos);
        for (int i = currentMove; i > 0; i--) {
            if (pos.halfMoveClock == 0)
                break;
            pos.unMakeMove(moveList.get(i- 1), uiInfoList.get(i- 1));
            posList.add(new Position(pos));
        }
        Collections.reverse(posList);
        return posList;
    }

    private boolean handleDrawCmd(String drawCmd) {
        if (drawCmd.startsWith("rep") || drawCmd.startsWith("50")) {
            boolean rep = drawCmd.startsWith("rep");
            Move m = null;
            String ms = drawCmd.substring(drawCmd.indexOf(" ") + 1);
            if (!ms.isEmpty()) {
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

    private boolean handleTestSuite(String cmd) {
        LineNumberReader fr = null;
        try {
            int idx = cmd.indexOf(" ");
            String filename = cmd.substring(0, idx);
            String timeStr = cmd.substring(idx + 1, cmd.length());
            int timeLimit = Integer.parseInt(timeStr);
//            System.out.printf("file:%s time:%s (%d)\n", filename, timeStr, timeLimit);
            fr = new LineNumberReader(new FileReader(filename));
            String line;
            Player pl = whitePlayer.isHumanPlayer() ? blackPlayer : whitePlayer;
            if (pl.isHumanPlayer()) {
                System.out.printf("No computer player available");
                return false;
            }
            ComputerPlayer cp = (ComputerPlayer)pl;
            int numRight = 0;
            int numTotal = 0;
            while ((line = fr.readLine()) != null) {
                if (line.startsWith("#") || (line.length() == 0)) {
                    continue;
                }
                int idx1 = line.indexOf(" bm ");
                String fen = line.substring(0, idx1);
                int idx2 = line.indexOf(";", idx1);
                String bm = line.substring(idx1 + 4, idx2);
//                System.out.printf("Line %3d: fen:%s bm:%s\n", fr.getLineNumber(), fen, bm);
                Position testPos = TextIO.readFEN(fen);
                cp.clearTT();
                TwoReturnValues<Move, String> ret = cp.searchPosition(testPos, timeLimit);
                Move sm = ret.first;
                String PV = ret.second;
                Move m = new Move(sm);
                String[] answers = bm.split(" ");
                boolean correct = false;
                for (String a : answers) {
                    Move am = TextIO.stringToMove(testPos, a);
                    if (am == null) {
                        throw new ChessParseError("Invalid move " + a);
                    }
                    if (am.equals(m)) {
                        correct = true;
                        break;
                    }
                }
                if (correct) {
                    numRight++;
                }
                numTotal++;
                System.out.printf("%3d : %6s %6d %d %03d/%03d %s : %s\n", fr.getLineNumber(),
                        TextIO.moveToString(testPos, sm, false), sm.score, correct ? 1 : 0,
                        numRight, numTotal, bm, PV);
            }
            fr.close();
        } catch (NumberFormatException nfe) {
            System.out.printf("Number format exception: %s\n", nfe.getMessage());
            return false;
        } catch (FileNotFoundException fnfe) {
            System.out.printf("File not found: %s\n", fnfe.getMessage());
            return false;
        } catch (IOException ex) {
            System.out.printf("IO error: %s\n", ex.getMessage());
        } catch (ChessParseError cpe) {
            int lineNo = (fr == null) ? -1 : fr.getLineNumber();
            System.out.printf("Parse error, line %d: %s\n", lineNo, cpe.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            int lineNo = (fr == null) ? -1 : fr.getLineNumber();
            System.out.printf("Parse error, line %d: %s\n", lineNo, e.getMessage());
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException ex) {
                    // Stupid FileReader class forces me to catch this meaningless exception
                }
            }
        }
        return true;
    }

    private boolean handleBookCmd(String bookCmd) {
        if (bookCmd.equals("off")) {
            whitePlayer.useBook(false);
            blackPlayer.useBook(false);
            return true;
        } else if (bookCmd.equals("on")) {
            whitePlayer.useBook(true);
            whitePlayer.useBook(true);
            return true;
        }
        return false;
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

    private void showHelp() {
        System.out.println("Enter a move, or one of the following special commands:");
        System.out.println("  new             - Start a new game");
        System.out.println("  undo            - Undo last half-move");
        System.out.println("  redo            - Redo next half-move");
        System.out.println("  swap            - Swap sides");
        System.out.println("  go              - Same as swap");
        System.out.println("  list            - List all moves in current game");
        System.out.println("  setpos FEN      - Set a position using a FEN string");
        System.out.println("  getpos          - Print current position in FEN notation");
        System.out.println("  draw rep [move] - Claim draw by repetition");
        System.out.println("  draw 50 [move]  - Claim draw by 50-move rule");
        System.out.println("  draw offer move - Play move and offer draw");
        System.out.println("  draw accept     - Accept a draw offer");
        System.out.println("  resign          - Resign the current game");
        System.out.println("  testsuite filename maxtime");
        System.out.println("  book on|off     - Turn opening book on/off");
        System.out.println("  time t          - Set computer thinking time, ms");
        System.out.println("  uci             - Switch to uci protocol.");
        System.out.println("  help            - Show this help");
        System.out.println("  quit            - Terminate program");
    }
}
