/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uci;

import chess.ChessParseError;
import chess.ComputerPlayer;
import chess.Move;
import chess.Piece;
import chess.Position;
import chess.TextIO;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * Handle the UCI protocol mode.
 * @author petero
 */
public class UCIProtocol {
    // Data set by the "position" command.
    Position pos;
    ArrayList<Move> moves;

    // Engine data
    EngineControl engine;

    // Set to true to break out of main loop
    boolean quit;


    public static void main(boolean autoStart) {
        UCIProtocol uciProt = new UCIProtocol();
        uciProt.mainLoop(System.in, System.out, autoStart);
    }

    public UCIProtocol() {
        try {
            pos = TextIO.readFEN(TextIO.startPosFEN);
        } catch (ChessParseError ex) {
            throw new RuntimeException();
        }
        moves = new ArrayList<Move>();
        quit = false;
    }

    final public void mainLoop(InputStream is, PrintStream os, boolean autoStart) {
        try {
            if (autoStart) {
                handleCommand("uci", os);
            }
            InputStreamReader inStrRead = new InputStreamReader(is);
            BufferedReader inBuf = new BufferedReader(inStrRead);
            String line;
            while ((line = inBuf.readLine()) != null) {
                handleCommand(line, os);
                if (quit) {
                    break;
                }
            }
        } catch (IOException ex) {
            // If stream closed or other I/O error, terminate program
        }
    }

    final void handleCommand(String cmdLine, PrintStream os) {
        String[] tokens = tokenize(cmdLine);
        try {
            String cmd = tokens[0];
            if (cmd.equals("uci")) {
                os.printf("id name %s%n", ComputerPlayer.engineName);
                os.printf("id author Peter Osterlund%n");
                EngineControl.printOptions(os);
                os.printf("uciok%n");
            } else if (cmd.equals("isready")) {
                initEngine(os);
                os.printf("readyok%n");
            } else if (cmd.equals("setoption")) {
                initEngine(os);
                String optionName = "";
                String optionValue = "";
                if (tokens[1].endsWith("name")) {
                    int idx = 2;
                    while ((idx < tokens.length) && !tokens[idx].equals("value")) {
                        optionName += tokens[idx++].toLowerCase();
                        optionName += " ";
                    }
                    optionName = optionName.trim();
                    if ((idx < tokens.length) && tokens[idx++].equals("value")) {
                        while ((idx < tokens.length)) {
                            optionValue += tokens[idx++].toLowerCase();
                            optionValue += " ";
                        }
                        optionValue = optionValue.trim();
                    }
                    engine.setOption(optionName, optionValue);
                }
            } else if (cmd.equals("ucinewgame")) {
                if (engine != null) {
                    engine.newGame();
                }
            } else if (cmd.equals("position")) {
                String fen = null;
                int idx = 1;
                if (tokens[idx].equals("startpos")) {
                    idx++;
                    fen = TextIO.startPosFEN;
                } else if (tokens[idx].equals("fen")) {
                    idx++;
                    fen = "";
                    while ((idx < tokens.length) && !tokens[idx].equals("moves")) {
                        fen += tokens[idx++];
                        fen += " ";
                    }
                    fen = fen.trim();
                }
                if (fen != null) {
                    pos = TextIO.readFEN(fen);
                    moves.clear();
                    if ((idx < tokens.length) && tokens[idx++].equals("moves")) {
                        for (int i = idx; i < tokens.length; i++) {
                            Move m = stringToMove(tokens[i]);
                            if (m != null) {
                                moves.add(m);
                            } else {
                                break;
                            }
                        }
                    }
                }
            } else if (cmd.equals("go")) {
                initEngine(os);
                int idx = 1;
                SearchParams sPar = new SearchParams();
                boolean ponder = false;
                while (idx < tokens.length) {
                    String subCmd = tokens[idx++];
                    if (subCmd.equals("searchmoves")) {
                        while (idx < tokens.length) {
                            Move m = stringToMove(tokens[idx]);
                            if (m != null) {
                                sPar.searchMoves.add(m);
                                idx++;
                            } else {
                                break;
                            }
                        }
                    } else if (subCmd.equals("ponder")) {
                        ponder = true;
                    } else if (subCmd.equals("wtime")) {
                        sPar.wTime = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("btime")) {
                        sPar.bTime = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("winc")) {
                        sPar.wInc = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("binc")) {
                        sPar.bInc = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("movestogo")) {
                        sPar.movesToGo = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("depth")) {
                        sPar.depth = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("nodes")) {
                        sPar.nodes = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("mate")) {
                        sPar.mate = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("movetime")) {
                        sPar.moveTime = Integer.parseInt(tokens[idx++]);
                    } else if (subCmd.equals("infinite")) {
                        sPar.infinite = true;
                    }
                }
                if (ponder) {
                    engine.startPonder(pos, moves, sPar);
                } else {
                    engine.startSearch(pos, moves, sPar);
                }
            } else if (cmd.equals("stop")) {
                engine.stopSearch();
            } else if (cmd.equals("ponderhit")) {
                engine.ponderHit();
            } else if (cmd.equals("quit")) {
                if (engine != null) {
                    engine.stopSearch();
                }
                quit = true;
            }
        } catch (ChessParseError ex) {
        } catch (ArrayIndexOutOfBoundsException e) {
        } catch (NumberFormatException nfe) {
        }
    }

    final private void initEngine(PrintStream os) {
        if (engine == null) {
            engine = new EngineControl(os);
        }
    }

    /** Convert a string to tokens by splitting at whitespace characters. */
    final String[] tokenize(String cmdLine) {
        cmdLine = cmdLine.trim();
        return cmdLine.split("\\s+");
    }

    /**
     * Convert a string to a Move object.
     * @return A move object, or null if move has invalid syntax
     */
    final Move stringToMove(String move) {
        Move m = null;
        if ((move.length() < 4) || (move.length() > 5))
            return m;
        int fromSq = TextIO.getSquare(move.substring(0, 2));
        int toSq   = TextIO.getSquare(move.substring(2, 4));
        if ((fromSq < 0) || (toSq < 0)) {
            return m;
        }
        char prom = ' ';
        boolean white = true;
        if (move.length() == 5) {
            prom = move.charAt(4);
            if (Position.getY(toSq) == 7) {
                white = true;
            } else if (Position.getY(toSq) == 0) {
                white = false;
            } else {
                return m;
            }
        }
        int promoteTo;
        switch (prom) {
            case ' ':
                promoteTo = Piece.EMPTY;
                break;
            case 'q':
                promoteTo = white ? Piece.WQUEEN : Piece.BQUEEN;
                break;
            case 'r':
                promoteTo = white ? Piece.WROOK : Piece.BROOK;
                break;
            case 'b':
                promoteTo = white ? Piece.WBISHOP : Piece.BBISHOP;
                break;
            case 'n':
                promoteTo = white ? Piece.WKNIGHT : Piece.BKNIGHT;
                break;
            default:
                return m;
        }
        m = new Move(fromSq, toSq, promoteTo);
        return m;
    }
}
