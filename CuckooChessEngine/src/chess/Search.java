/*
    CuckooChess - A java chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package chess;

import chess.TranspositionTable.TTEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author petero
 */
public class Search {
    Position pos;
    MoveGen moveGen;
    Evaluate eval;
    KillerTable kt;
    History ht;
    long[] posHashList;         // List of hashes for previous positions up to the last "zeroing" move.
    int posHashListSize;        // Number of used entries in posHashList
    int posHashFirstNew;        // First entry in posHashList that has not been played OTB.
    TranspositionTable tt;
    int depth;
    TreeLogger log = null;

    private static final class SearchTreeInfo {
        UndoInfo undoInfo;
        Move hashMove;         // Temporary storage for local hashMove variable
        boolean allowNullMove; // Don't allow two null-moves in a row
        Move bestMove;         // Copy of the best found move at this ply
        Move currentMove;      // Move currently being searched
        int lmr;               // LMR reduction amount
        long nodeIdx;
        SearchTreeInfo() {
            undoInfo = new UndoInfo();
            hashMove = new Move(0, 0, 0);
            allowNullMove = true;
            bestMove = new Move(0, 0, 0);
        }
    }
    SearchTreeInfo[] searchTreeInfo;

    // Time management
    long tStart;            // Time when search started
    long minTimeMillis;     // Minimum recommended thinking time
    long maxTimeMillis;     // Maximum allowed thinking time
    boolean searchNeedMoreTime; // True if negaScout should use up to maxTimeMillis time.
    private int maxNodes;   // Maximum number of nodes to search (approximately)
    int nodesToGo;          // Number of nodes until next time check
    
    // Search statistics stuff
    int nodes;
    int qNodes;
    int[] nodesPlyVec;
    int[] nodesDepthVec;
    int totalNodes;
    long tLastStats;        // Time when notifyStats was last called
    boolean verbose;
    
    public final static int MATE0 = 32000;

    public final static int UNKNOWN_SCORE = -32767; // Represents unknown static eval score
    int q0Eval; // Static eval score at first level of quiescence search 

    public Search(Position pos, long[] posHashList, int posHashListSize, TranspositionTable tt) {
        this.pos = new Position(pos);
        this.moveGen = new MoveGen();
        this.posHashList = posHashList;
        this.posHashListSize = posHashListSize;
        this.tt = tt;
        eval = new Evaluate();
        kt = new KillerTable();
        ht = new History();
        posHashFirstNew = posHashListSize;
        initNodeStats();
        minTimeMillis = -1;
        maxTimeMillis = -1;
        searchNeedMoreTime = false;
        maxNodes = -1;
        final int vecLen = 200;
        searchTreeInfo = new SearchTreeInfo[vecLen];
        for (int i = 0; i < vecLen; i++) {
            searchTreeInfo[i] = new SearchTreeInfo();
        }
    }

    class StopSearch extends Exception {
        private static final long serialVersionUID = -5546906604987117015L;
        public StopSearch() {
        }
        public StopSearch(String msg) {
            super(msg);
        }
    }

    /**
     * Used to get various search information during search
     */
    public interface Listener {
        public void notifyDepth(int depth);
        public void notifyCurrMove(Move m, int moveNr);
        public void notifyPV(int depth, int score, int time, int nodes, int nps,
                boolean isMate, boolean upperBound, boolean lowerBound, ArrayList<Move> pv);
        public void notifyStats(int nodes, int nps, int time);
    }

    Listener listener;
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private final static class MoveInfo {
        Move move;
        int nodes;
        MoveInfo(Move m, int n) { move = m;  nodes = n; }
        static public class SortByScore implements Comparator<MoveInfo> {
            public int compare(MoveInfo mi1, MoveInfo mi2) {
                if ((mi1 == null) && (mi2 == null))
                    return 0;
                if (mi1 == null)
                    return 1;
                if (mi2 == null)
                    return -1;
                return mi2.move.score - mi1.move.score;
            }
        }
        static public class SortByNodes implements Comparator<MoveInfo> {
            public int compare(MoveInfo mi1, MoveInfo mi2) {
                if ((mi1 == null) && (mi2 == null))
                    return 0;
                if (mi1 == null)
                    return 1;
                if (mi2 == null)
                    return -1;
                return mi2.nodes - mi1.nodes;
            }
        }
    }

    final public void timeLimit(int minTimeLimit, int maxTimeLimit) {
        minTimeMillis = minTimeLimit;
        maxTimeMillis = maxTimeLimit;
    }

    final public Move iterativeDeepening(Move[] scMovesIn,
            int maxDepth, int initialMaxNodes, boolean verbose) {
        tStart = System.currentTimeMillis();
//        log = TreeLogger.getWriter("/home/petero/treelog.dmp", pos);
        totalNodes = 0;
        MoveInfo[] scMoves = new MoveInfo[scMovesIn.length];
        int len = 0;
        for (int mi = 0; scMovesIn[mi] != null; mi++) {
            Move m = scMovesIn[mi];
            scMoves[len++] = new MoveInfo(m, 0);
        }
        maxNodes = initialMaxNodes;
        nodesToGo = 0;
        Position origPos = new Position(pos);
        final int aspirationDelta = 25;
        int bestScoreLastIter = 0;
        Move bestMove = scMoves[0].move;
        this.verbose = verbose;
        if ((maxDepth < 0) || (maxDepth > 100)) {
            maxDepth = 100;
        }
        for (int i = 0; i < searchTreeInfo.length; i++) {
            searchTreeInfo[i].allowNullMove = true;
        }
        try {
        for (depth = 1; ; depth++) {
            initNodeStats();
            if (listener != null) listener.notifyDepth(depth);
            int alpha = depth > 1 ? Math.max(bestScoreLastIter - aspirationDelta, -Search.MATE0) : -Search.MATE0;
            int bestScore = -Search.MATE0;
            UndoInfo ui = new UndoInfo();
            boolean needMoreTime = false;
            for (int mi = 0; scMoves[mi] != null; mi++) {
                searchNeedMoreTime = (mi > 0);
                Move m = scMoves[mi].move;
                if ((listener != null) && (System.currentTimeMillis() - tStart >= 1000)) {
                    listener.notifyCurrMove(m, mi + 1);
                }
                nodes = qNodes = 0;
                posHashList[posHashListSize++] = pos.zobristHash();
                boolean givesCheck = MoveGen.givesCheck(pos, m);
                int beta;
                if (depth > 1) {
                    beta = (mi == 0) ? Math.min(bestScoreLastIter + aspirationDelta, Search.MATE0) : alpha + 1;
                } else {
                    beta = Search.MATE0;
                }

                int lmr = 0;
                boolean isCapture = (pos.getPiece(m.to) != Piece.EMPTY);
                boolean isPromotion = (m.promoteTo != Piece.EMPTY);
                if ((depth >= 3) && !isCapture && !isPromotion) {
                    if (!givesCheck && !passedPawnPush(pos, m)) {
                        if (mi >= 3)
                            lmr = 1;
                    }
                }
/*                int nodes0 = nodes;
                int qNodes0 = qNodes;
                System.out.printf("%2d %5s %5d %5d %6s %6s ",
                        mi, "-", alpha, beta, "-", "-");
                System.out.printf("%-6s...\n", TextIO.moveToUCIString(m)); */
                pos.makeMove(m, ui);
                SearchTreeInfo sti = searchTreeInfo[0];
                sti.currentMove = m;
                sti.lmr = lmr;
                sti.nodeIdx = -1;
                int score = -negaScout(-beta, -alpha, 1, depth - lmr - 1, -1, givesCheck);
                if ((lmr > 0) && (score > alpha)) {
                    sti.lmr = 0;
                    score = -negaScout(-beta, -alpha, 1, depth - 1, -1, givesCheck);
                }
                int nodesThisMove = nodes + qNodes;
                posHashListSize--;
                pos.unMakeMove(m, ui);
                {
                    int type = TTEntry.T_EXACT;
                    if (score <= alpha) {
                        type = TTEntry.T_LE;
                    } else if (score >= beta) {
                        type = TTEntry.T_GE;
                    }
                    m.score = score;
                    tt.insert(pos.historyHash(), m, type, 0, depth, UNKNOWN_SCORE);
                }
                if (score >= beta) {
                    if (mi != 0) {
                        needMoreTime = true;
                    }
                    bestMove = m;
                    if (verbose)
                        System.out.printf("%-6s %6d %6d %6d >=\n", TextIO.moveToString(pos, m, false),
                                score, nodes, qNodes);
                    notifyPV(depth, score, false, true, m);
                    nodes = qNodes = 0;
                    posHashList[posHashListSize++] = pos.zobristHash();
                    pos.makeMove(m, ui);
                    score = -negaScout(-Search.MATE0, -score, 1, depth - 1, -1, givesCheck);
                    nodesThisMove += nodes + qNodes;
                    posHashListSize--;
                    pos.unMakeMove(m, ui);
                } else if ((mi == 0) && (score <= alpha)) {
                    needMoreTime = searchNeedMoreTime = true;
                    if (verbose)
                        System.out.printf("%-6s %6d %6d %6d <=\n", TextIO.moveToString(pos, m, false),
                                score, nodes, qNodes);
                    notifyPV(depth, score, true, false, m);
                    nodes = qNodes = 0;
                    posHashList[posHashListSize++] = pos.zobristHash();
                    pos.makeMove(m, ui);
                    score = -negaScout(-score, Search.MATE0, 1, depth - 1, -1, givesCheck);
                    nodesThisMove += nodes + qNodes;
                    posHashListSize--;
                    pos.unMakeMove(m, ui);
                }
                if (verbose || ((listener != null) && (depth > 1))) {
                    boolean havePV = false;
                    String PV = "";
                    if ((score > alpha) || (mi == 0)) {
                        havePV = true;
                        if (verbose) {
                            PV = TextIO.moveToString(pos, m, false) + " ";
                            pos.makeMove(m, ui);
                            PV += tt.extractPV(pos);
                            pos.unMakeMove(m, ui);
                        }
                    }
                    if (verbose) {
/*                        System.out.printf("%2d %5d %5d %5d %6d %6d ",
                                mi, score, alpha, beta, nodes-nodes0, qNodes-qNodes0);
                        System.out.printf("%-6s\n", TextIO.moveToUCIString(m)); */
                        System.out.printf("%-6s %6d %6d %6d%s %s\n",
                                TextIO.moveToString(pos, m, false), score,
                                nodes, qNodes, (score > alpha ? " *" : ""), PV);
                    }
                    if (havePV && (depth > 1)) {
                        notifyPV(depth, score, false, false, m);
                    }
                }
                scMoves[mi].move.score = score;
                scMoves[mi].nodes = nodesThisMove;
                bestScore = Math.max(bestScore, score);
                if (depth > 1) {
                    if ((score > alpha) || (mi == 0)) {
                        alpha = score;
                        MoveInfo tmp = scMoves[mi];
                        for (int i = mi - 1; i >= 0;  i--) {
                            scMoves[i + 1] = scMoves[i];
                        }
                        scMoves[0] = tmp;
                        bestMove = scMoves[0].move;
                    }
                }
                if (depth > 1) {
                    long timeLimit = needMoreTime ? maxTimeMillis : minTimeMillis;
                    if (timeLimit >= 0) {
                        long tNow = System.currentTimeMillis();
                        if (tNow - tStart >= timeLimit)
                            break;
                    }
                }
            }
            if (depth == 1) {
                Arrays.sort(scMoves, new MoveInfo.SortByScore());
                bestMove = scMoves[0].move;
                notifyPV(depth, bestMove.score, false, false, bestMove);
            }
            long tNow = System.currentTimeMillis();
            if (verbose) {
                for (int i = 0; i < 20; i++) {
                    System.out.printf("%2d %7d %7d\n", i, nodesPlyVec[i], nodesDepthVec[i]);
                }
                System.out.printf("Time: %.3f depth:%d nps:%d\n", (tNow - tStart) * .001, depth,
                        (int)(totalNodes / ((tNow - tStart) * .001)));
            }
            if (maxTimeMillis >= 0) {
                if (tNow - tStart >= minTimeMillis)
                    break;
            }
            if (depth >= maxDepth)
                break;
            if (maxNodes >= 0) {
                if (totalNodes >= maxNodes)
                    break;
            }
            int plyToMate = Search.MATE0 - Math.abs(bestScore);
            if (depth >= plyToMate)
                break;
            bestScoreLastIter = bestScore;

            if (depth > 1) {
                // Moves that were hard to search should be searched early in the next iteration
                if ((scMoves[0] != null) && (scMoves[1] != null))
                    Arrays.sort(scMoves, 1, scMoves.length, new MoveInfo.SortByNodes());
            }
        }
        } catch (StopSearch ss) {
            pos = origPos;
        }
        notifyStats();

        if (log != null) {
            log.close();
            log = null;
        }
        return bestMove;
    }

    private final void notifyPV(int depth, int score, boolean uBound, boolean lBound, Move m) {
        if (listener != null) {
            boolean isMate = false;
            if (score > MATE0 / 2) {
                isMate = true;
                score = (MATE0 - score) / 2;
            } else if (score < -MATE0 / 2) {
                isMate = true;
                score = -((MATE0 + score - 1) / 2);
            }
            long tNow = System.currentTimeMillis();
            int time = (int) (tNow - tStart);
            int nps = (time > 0) ? (int)(totalNodes / (time / 1000.0)) : 0;
            ArrayList<Move> pv = tt.extractPVMoves(pos, m);
            listener.notifyPV(depth, score, time, totalNodes, nps, isMate, uBound, lBound, pv);
        }
    }

    private final void notifyStats() {
        long tNow = System.currentTimeMillis();
        if (listener != null) {
            int time = (int) (tNow - tStart);
            int nps = (time > 0) ? (int)(totalNodes / (time / 1000.0)) : 0;
            listener.notifyStats(totalNodes, nps, time);
        }
        tLastStats = tNow;
    }

    private static final Move emptyMove = new Move(0, 0, Piece.EMPTY, 0);

    /** 
     * Main recursive search algorithm.
     * @return Score for the side to make a move, in position given by "pos".
     */
    final public int negaScout(int alpha, int beta, int ply, int depth, int recaptureSquare,
                               final boolean inCheck) throws StopSearch {
        if (log != null) {
            SearchTreeInfo sti = searchTreeInfo[ply-1];
            long idx = log.logNodeStart(sti.nodeIdx, sti.currentMove, alpha, beta, ply, depth);
            searchTreeInfo[ply].nodeIdx = idx;
        }
        if (--nodesToGo <= 0) {
            nodesToGo = 5000;
            long tNow = System.currentTimeMillis();
            long timeLimit = searchNeedMoreTime ? maxTimeMillis : minTimeMillis;
            if (    ((timeLimit >= 0) && (tNow - tStart >= timeLimit)) ||
                    ((maxNodes >= 0) && (totalNodes >= maxNodes))) {
                throw new StopSearch();
            }
            if (tNow - tLastStats >= 1000) {
                notifyStats();
            }
        }
        
        // Collect statistics
        if (verbose) {
            if (ply < 20) nodesPlyVec[ply]++;
            if (depth < 20) nodesDepthVec[depth]++;
        }
        nodes++;
        totalNodes++;
        final long hKey = pos.historyHash();

        // Draw tests
        if (canClaimDraw50(pos)) {
            if (MoveGen.canTakeKing(pos)) {
                int score = MATE0 - ply;
                if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, score, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
                return score;
            }
            if (inCheck) {
                Move[] moves = moveGen.pseudoLegalMoves(pos);
                moves = MoveGen.removeIllegal(pos, moves);
                if (moves[0] == null) {            // Can't claim draw if already check mated.
                    int score = -(MATE0-(ply+1));
                    if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, score, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
                    return score;
                }
            }
            if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, 0, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
            return 0;
        }
        if (canClaimDrawRep(pos, posHashList, posHashListSize, posHashFirstNew)) {
            if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, 0, TTEntry.T_EXACT, UNKNOWN_SCORE, hKey);
            return 0;            // No need to test for mate here, since it would have been
                                 // discovered the first time the position came up.
            // FIXME! Sometimes draws in won positions. Xboard bug or cuckoochess bug?
        }

        int evalScore = UNKNOWN_SCORE;
        // Check transposition table
        TTEntry ent = tt.probe(hKey);
        Move hashMove = null;
        SearchTreeInfo sti = searchTreeInfo[ply];
        if (ent.type != TTEntry.T_EMPTY) {
            int score = ent.getScore(ply);
            evalScore = ent.evalScore;
            int plyToMate = MATE0 - Math.abs(score);
            if ((beta == alpha + 1) && ((ent.depth >= depth) || (ent.depth >= plyToMate))) {
                if (    (ent.type == TTEntry.T_EXACT) ||
                        (ent.type == TTEntry.T_GE) && (score >= beta) ||
                        (ent.type == TTEntry.T_LE) && (score <= alpha)) {
                    if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, score, ent.type, evalScore, hKey);
                    return score;
                }
            }
            hashMove = sti.hashMove;
            ent.getMove(hashMove);
        }
        
        int posExtend = inCheck ? 1 : 0; // Check extension

        // If out of depth, perform quiescence search
        if (depth + posExtend <= 0) {
            qNodes--;
            totalNodes--;
            q0Eval = evalScore;
            int score = quiesce(alpha, beta, ply, 0, inCheck);
            int type = TTEntry.T_EXACT;
            if (score <= alpha) {
                type = TTEntry.T_LE;
            } else if (score >= beta) {
                type = TTEntry.T_GE;
            }
            emptyMove.score = score;
            tt.insert(hKey, emptyMove, type, ply, depth, q0Eval);
            if (log != null) log.logNodeEnd(searchTreeInfo[ply].nodeIdx, score, type, evalScore, hKey);
            return score;
        }

        // Try null-move pruning
        // FIXME! Try null-move verification in late endgames. See loss in round 21.
        sti.currentMove = emptyMove;
        if (    (depth >= 3) && !inCheck && sti.allowNullMove &&
                (Math.abs(beta) <= MATE0 / 2)) {
            if (MoveGen.canTakeKing(pos)) {
                int score = MATE0 - ply;
                if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_EXACT, evalScore, hKey);
                return score;
            }
            boolean nullOk;
            if (pos.whiteMove) {
                nullOk = (pos.wMtrl > pos.wMtrlPawns) && (pos.wMtrlPawns > 0);
            } else {
                nullOk = (pos.bMtrl > pos.bMtrlPawns) && (pos.bMtrlPawns > 0);
            }
            if (nullOk) {
                final int R = (depth > 6) ? 3 : 2;
                pos.setWhiteMove(!pos.whiteMove);
                int epSquare = pos.getEpSquare();
                pos.setEpSquare(-1);
                searchTreeInfo[ply+1].allowNullMove = false;
                int score = -negaScout(-beta, -(beta - 1), ply + 1, depth - R - 1, -1, false);
                searchTreeInfo[ply+1].allowNullMove = true;
                pos.setEpSquare(epSquare);
                pos.setWhiteMove(!pos.whiteMove);
                if (score >= beta) {
                    if (score > MATE0 / 2)
                        score = beta;
                    emptyMove.score = score;
                    tt.insert(hKey, emptyMove, TTEntry.T_GE, ply, depth, evalScore);
                    if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_GE, evalScore, hKey);
                    return score;
                } else {
                    if ((searchTreeInfo[ply-1].lmr > 0) && (depth < 5)) {
                        Move m1 = searchTreeInfo[ply-1].currentMove;
                        Move m2 = searchTreeInfo[ply+1].bestMove; // threat move
                        if (m1.from != m1.to) {
                            if ((m1.to == m2.from) || (m1.from == m2.to) ||
                                ((BitBoard.squaresBetween[m2.from][m2.to] & (1L << m1.from)) != 0)) {
                                // if the threat move was made possible by a reduced
                                // move on the previous ply, the reduction was unsafe.
                                // Return alpha to trigger a non-reduced re-search.
                                if (log != null) log.logNodeEnd(sti.nodeIdx, alpha, TTEntry.T_LE, evalScore, hKey);
                                return alpha;
                            }
                        }
                    }
                }
            }
        }

        boolean futilityPrune = false;
        int futilityScore = alpha;
        if (!inCheck && (depth < 4) && (posExtend == 0)) {
            if ((Math.abs(alpha) <= MATE0 / 2) && (Math.abs(beta) <= MATE0 / 2)) {
                int margin;
                if (depth == 1) {
                    margin = 150;
                } else if (depth == 2) {
                    margin = 300;
                } else {
                    margin = 450;
                }
                if (evalScore == UNKNOWN_SCORE) {
                    evalScore = eval.evalPos(pos);
                }
                futilityScore = evalScore + margin;
                if (futilityScore <= alpha) {
                    futilityPrune = true;
                }
            }
        }

        if ((depth > 4) && (beta > alpha + 1) && ((hashMove == null) || (hashMove.from == hashMove.to))) {
            // No hash move at PV node. Try internal iterative deepening.
            long savedNodeIdx = sti.nodeIdx;
            negaScout(alpha, beta, ply, (depth > 8) ? (depth - 5) : (depth - 4), -1, inCheck);
            sti.nodeIdx = savedNodeIdx;
            ent = tt.probe(hKey);
            if (ent.type != TTEntry.T_EMPTY) {
                hashMove = sti.hashMove;
                ent.getMove(hashMove);
            }
        }

        // Start searching move alternatives
        // FIXME! Try hash move before generating move list.
        Move[] moves;
        if (inCheck)
            moves = moveGen.checkEvasions(pos);
        else 
            moves = moveGen.pseudoLegalMoves(pos);
        boolean seeDone = false;
        boolean hashMoveSelected = true;
        if (!selectHashMove(moves, hashMove)) {
            scoreMoveList(moves, ply);
            seeDone = true;
            hashMoveSelected = false;
        }

        UndoInfo ui = sti.undoInfo;
        boolean haveLegalMoves = false;
        int illegalScore = -(MATE0-(ply+1));
        int b = beta;
        int bestScore = illegalScore;
        int bestMove = -1;
        int lmrCount = 0;
        for (int mi = 0; moves[mi] != null; mi++) {
            if ((mi == 1) && !seeDone) {
                scoreMoveList(moves, ply, 1);
                seeDone = true;
            }
            if ((mi > 0) || !hashMoveSelected) {
                selectBest(moves, mi);
            }
            Move m = moves[mi];
            if (pos.getPiece(m.to) == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                moveGen.returnMoveList(moves);
                int score = MATE0-ply;
                if (log != null) log.logNodeEnd(sti.nodeIdx, score, TTEntry.T_EXACT, evalScore, hKey);
                return score;       // King capture
            }
            int newCaptureSquare = -1;
            boolean isCapture = false;
            boolean isPromotion = (m.promoteTo != Piece.EMPTY);
            int sVal = Integer.MIN_VALUE;
            if (pos.getPiece(m.to) != Piece.EMPTY) {
                isCapture = true;
                int fVal = Evaluate.pieceValue[pos.getPiece(m.from)];
                int tVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                final int pV = Evaluate.pV;
                if (Math.abs(tVal - fVal) < pV / 2) {    // "Equal" capture
                    sVal = SEE(m);
                    if (Math.abs(sVal) < pV / 2)
                        newCaptureSquare = m.to;
                }
            }
            int moveExtend = 0;
            if (posExtend == 0) {
                final int pV = Evaluate.pV;
                if ((m.to == recaptureSquare)) {
                    if (sVal == Integer.MIN_VALUE) sVal = SEE(m);
                    int tVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                    if (sVal > tVal - pV / 2)
                        moveExtend = 1;
                }
                if ((moveExtend == 0) && isCapture && (pos.wMtrlPawns + pos.bMtrlPawns > pV)) {
                    // Extend if going into pawn endgame
                    int capVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                    if (pos.whiteMove) {
                        if ((pos.wMtrl == pos.wMtrlPawns) && (pos.bMtrl - pos.bMtrlPawns == capVal))
                            moveExtend = 1;
                    } else {
                        if ((pos.bMtrl == pos.bMtrlPawns) && (pos.wMtrl - pos.wMtrlPawns == capVal))
                            moveExtend = 1;
                    }
                }
            }
            // FIXME! Test extending pawn pushes to 7:th rank
            boolean mayReduce = (m.score < 53) && (!isCapture || m.score < 0) && !isPromotion;
            
            boolean givesCheck = MoveGen.givesCheck(pos, m); 
            boolean doFutility = false;
            if (futilityPrune && mayReduce && haveLegalMoves) {
                if (!givesCheck && !passedPawnPush(pos, m))
                    doFutility = true;
            }
            int score;
            if (doFutility) {
                score = futilityScore;
            } else {
                int extend = Math.max(posExtend, moveExtend);
                int lmr = 0;
                if ((depth >= 3) && mayReduce && (extend == 0)) {
                    if (!givesCheck && !passedPawnPush(pos, m)) {
                        lmrCount++;
                        if ((lmrCount > 3) && (depth > 3)) {
                            lmr = 2;
                        } else {
                            lmr = 1;
                        }
                    }
                }
                posHashList[posHashListSize++] = pos.zobristHash();
                pos.makeMove(m, ui);
                sti.currentMove = m;
                int newDepth = depth - 1 + extend - lmr;
/*              int nodes0 = nodes;
                int qNodes0 = qNodes;
                if ((ply < 3) && (newDepth > 1)) {
                    System.out.printf("%2d %5s %5d %5d %6s %6s ",
                            mi, "-", alpha, beta, "-", "-");
                    for (int i = 0; i < ply; i++)
                        System.out.printf("      ");
                    System.out.printf("%-6s...\n", TextIO.moveToUCIString(m));
                } */
                sti.lmr = lmr;
                score = -negaScout(-b, -alpha, ply + 1, newDepth, newCaptureSquare, givesCheck);
                if (((lmr > 0) && (score > alpha)) ||
                    ((score > alpha) && (score < beta) && (b != beta) && (score != illegalScore))) {
                    sti.lmr = 0;
                    newDepth += lmr;
                    score = -negaScout(-beta, -alpha, ply + 1, newDepth, newCaptureSquare, givesCheck);
                }
/*              if (ply <= 3) {
                    System.out.printf("%2d %5d %5d %5d %6d %6d ",
                            mi, score, alpha, beta, nodes-nodes0, qNodes-qNodes0);
                    for (int i = 0; i < ply; i++)
                        System.out.printf("      ");
                    System.out.printf("%-6s\n", TextIO.moveToUCIString(m));
                }*/
                posHashListSize--;
                pos.unMakeMove(m, ui);
            }
            m.score = score;

            if (score != illegalScore) {
                haveLegalMoves = true;
            }
            bestScore = Math.max(bestScore, score);
            if (score > alpha) {
                alpha = score;
                bestMove = mi;
                sti.bestMove.from      = m.from;
                sti.bestMove.to        = m.to;
                sti.bestMove.promoteTo = m.promoteTo;
            }
            if (alpha >= beta) {
                if (pos.getPiece(m.to) == Piece.EMPTY) {
                    kt.addKiller(ply, m);
                    ht.addSuccess(pos, m, depth);
                    for (int mi2 = mi - 1; mi2 >= 0; mi2--) {
                        Move m2 = moves[mi2];
                        if (pos.getPiece(m2.to) == Piece.EMPTY)
                            ht.addFail(pos, m2, depth);
                    }
                }
                tt.insert(hKey, m, TTEntry.T_GE, ply, depth, evalScore);
                moveGen.returnMoveList(moves);
                if (log != null) log.logNodeEnd(sti.nodeIdx, alpha, TTEntry.T_GE, evalScore, hKey);
                return alpha;
            }
            b = alpha + 1;
        }
        if (!haveLegalMoves && !inCheck) {
            moveGen.returnMoveList(moves);
            if (log != null) log.logNodeEnd(sti.nodeIdx, 0, TTEntry.T_EXACT, evalScore, hKey);
            return 0;       // Stale-mate
        }
        if (bestMove >= 0) {
            tt.insert(hKey, moves[bestMove], TTEntry.T_EXACT, ply, depth, evalScore);
            if (log != null) log.logNodeEnd(sti.nodeIdx, bestScore, TTEntry.T_EXACT, evalScore, hKey);
        } else {
            emptyMove.score = bestScore;
            tt.insert(hKey, emptyMove, TTEntry.T_LE, ply, depth, evalScore);
            if (log != null) log.logNodeEnd(sti.nodeIdx, bestScore, TTEntry.T_LE, evalScore, hKey);
        }
        moveGen.returnMoveList(moves);
        return bestScore;
    }

    private static final boolean passedPawnPush(Position pos, Move m) {
        int p = pos.getPiece(m.from);
        if (pos.whiteMove) {
            if (p != Piece.WPAWN)
                return false;
            if ((BitBoard.wPawnBlockerMask[m.to] & pos.pieceTypeBB[Piece.BPAWN]) != 0)
                return false;
            return m.to >= 40;
        } else {
            if (p != Piece.BPAWN)
                return false;
            if ((BitBoard.bPawnBlockerMask[m.to] & pos.pieceTypeBB[Piece.WPAWN]) != 0)
                return false;
            return m.to <= 23;
        }
    }

    /**
     * Quiescence search. Only non-losing captures are searched.
     */
    final private int quiesce(int alpha, int beta, int ply, int depth, final boolean inCheck) {
        qNodes++;
        totalNodes++;
        int score;
        if (inCheck) {
            score = -(MATE0 - (ply+1));
        } else {
            if ((depth == 0) && (q0Eval != UNKNOWN_SCORE)) {
                score = q0Eval;
            } else {
                score = eval.evalPos(pos);
                if (depth == 0)
                    q0Eval = score;
            }
        }
        if (score >= beta) {
            if ((depth == 0) && (score < MATE0 - ply)) {
                if (MoveGen.canTakeKing(pos)) {
                    // To make stale-mate detection work
                    score = MATE0 - ply;
                }
            }
            return score;
        }
        final int evalScore = score;
        if (score > alpha)
            alpha = score;
        int bestScore = score;
        final boolean tryChecks = (depth > -3);
        Move[] moves;
        if (inCheck) {
            moves = moveGen.checkEvasions(pos);
            scoreMoveList(moves, ply);
        } else if (tryChecks) {
            moves = moveGen.pseudoLegalCapturesAndChecks(pos);
            scoreMoveList(moves, ply);
        } else {
            moves = moveGen.pseudoLegalCaptures(pos);
            scoreCaptureList(moves, ply);
        }
        UndoInfo ui = searchTreeInfo[ply].undoInfo;
        for (int mi = 0; moves[mi] != null; mi++) {
            if (mi < 8) {
                // If the first 8 moves didn't fail high, this is probably an ALL-node,
                // so spending more effort on move ordering is probably wasted time.
                selectBest(moves, mi);
            }
            Move m = moves[mi];
            if (pos.getPiece(m.to) == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                moveGen.returnMoveList(moves);
                return MATE0-ply;       // King capture
            }
            boolean givesCheck = false;
            boolean givesCheckComputed = false;
            if (inCheck) {
                // Allow all moves
            } else {
                if (m.score < 0) {
                    continue;
                }
                if ((pos.getPiece(m.to) == Piece.EMPTY) && (m.promoteTo == Piece.EMPTY)) {
                    // Non-capture
                    if (!tryChecks)
                        continue;
                    givesCheck = MoveGen.givesCheck(pos, m);
                    givesCheckComputed = true;
                    if (!givesCheck)
                        continue;
                    if (SEE(m) < 0) // Needed because m.score is not computed for non-captures
                        continue;
                } else {
                    int capt = Evaluate.pieceValue[pos.getPiece(m.to)];
                    int prom = Evaluate.pieceValue[m.promoteTo];
                    int optimisticScore = evalScore + capt + prom + 200;
                    if (optimisticScore < alpha) { // Delta pruning
                        if ((pos.wMtrlPawns > 0) && (pos.wMtrl > capt + pos.wMtrlPawns) &&
                            (pos.bMtrlPawns > 0) && (pos.bMtrl > capt + pos.bMtrlPawns)) {
                            if (depth -1 > -4) {
                                givesCheck = MoveGen.givesCheck(pos, m);
                                givesCheckComputed = true;
                            }
                            if (!givesCheck) {
                                if (optimisticScore > bestScore)
                                    bestScore = optimisticScore;
                                continue;
                            }
                        }
                    }
                }
            }

            if (!givesCheckComputed) {
                if (depth - 1 > -4) {
                    givesCheck = MoveGen.givesCheck(pos, m);
                }
            }
            final boolean nextInCheck = (depth - 1) > -4 ? givesCheck : false;

            pos.makeMove(m, ui);
            score = -quiesce(-beta, -alpha, ply + 1, depth - 1, nextInCheck);
            pos.unMakeMove(m, ui);
            if (score > bestScore) {
                bestScore = score;
                if (score > alpha) {
                    alpha = score;
                    if (alpha >= beta) {
                        moveGen.returnMoveList(moves);
                        return alpha;
                    }
                }
            }
        }
        moveGen.returnMoveList(moves);
        return bestScore;
    }

    
    private int[] captures = new int[64];   // Value of captured pieces
    private UndoInfo seeUi = new UndoInfo();

    /**
     * Static exchange evaluation function.
     * @return SEE score for m. Positive value is good for the side that makes the first move.
     */
    final public int SEE(Move m) {
        final int kV = Evaluate.kV;
        
        final int square = m.to;
        if (square == pos.getEpSquare()) {
            captures[0] = Evaluate.pV;
        } else {
            captures[0] = Evaluate.pieceValue[pos.getPiece(square)];
            if (captures[0] == kV)
                return kV;
        }
        int nCapt = 1;                  // Number of entries in captures[]

        pos.makeSEEMove(m, seeUi);
        boolean white = pos.whiteMove;
        int valOnSquare = Evaluate.pieceValue[pos.getPiece(square)];
        long occupied = pos.whiteBB | pos.blackBB;
        while (true) {
            int bestValue = Integer.MAX_VALUE;
            long atk;
            if (white) {
                atk = BitBoard.bPawnAttacks[square] & pos.pieceTypeBB[Piece.WPAWN] & occupied;
                if (atk != 0) {
                    bestValue = Evaluate.pV;
                } else {
                    atk = BitBoard.knightAttacks[square] & pos.pieceTypeBB[Piece.WKNIGHT] & occupied;
                    if (atk != 0) {
                        bestValue = Evaluate.nV;
                    } else {
                        long bAtk = BitBoard.bishopAttacks(square, occupied) & occupied;
                        atk = bAtk & pos.pieceTypeBB[Piece.WBISHOP];
                        if (atk != 0) {
                            bestValue = Evaluate.bV;
                        } else {
                            long rAtk = BitBoard.rookAttacks(square, occupied) & occupied;
                            atk = rAtk & pos.pieceTypeBB[Piece.WROOK];
                            if (atk != 0) {
                                bestValue = Evaluate.rV;
                            } else {
                                atk = (bAtk | rAtk) & pos.pieceTypeBB[Piece.WQUEEN];
                                if (atk != 0) {
                                    bestValue = Evaluate.qV;
                                } else {
                                    atk = BitBoard.kingAttacks[square] & pos.pieceTypeBB[Piece.WKING] & occupied;
                                    if (atk != 0) {
                                        bestValue = kV;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                atk = BitBoard.wPawnAttacks[square] & pos.pieceTypeBB[Piece.BPAWN] & occupied;
                if (atk != 0) {
                    bestValue = Evaluate.pV;
                } else {
                    atk = BitBoard.knightAttacks[square] & pos.pieceTypeBB[Piece.BKNIGHT] & occupied;
                    if (atk != 0) {
                        bestValue = Evaluate.nV;
                    } else {
                        long bAtk = BitBoard.bishopAttacks(square, occupied) & occupied;
                        atk = bAtk & pos.pieceTypeBB[Piece.BBISHOP];
                        if (atk != 0) {
                            bestValue = Evaluate.bV;
                        } else {
                            long rAtk = BitBoard.rookAttacks(square, occupied) & occupied;
                            atk = rAtk & pos.pieceTypeBB[Piece.BROOK];
                            if (atk != 0) {
                                bestValue = Evaluate.rV;
                            } else {
                                atk = (bAtk | rAtk) & pos.pieceTypeBB[Piece.BQUEEN];
                                if (atk != 0) {
                                    bestValue = Evaluate.qV;
                                } else {
                                    atk = BitBoard.kingAttacks[square] & pos.pieceTypeBB[Piece.BKING] & occupied;
                                    if (atk != 0) {
                                        bestValue = kV;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            captures[nCapt++] = valOnSquare;
            if (valOnSquare == kV)
                break;
            valOnSquare = bestValue;
            occupied &= ~(atk & -atk);
            white = !white;
        }
        pos.unMakeSEEMove(m, seeUi);
        
        int score = 0;
        for (int i = nCapt - 1; i > 0; i--) {
            score = Math.max(0, captures[i] - score);
        }
        return captures[0] - score;
    }

    /**
     * Compute scores for each move in a move list, using SEE, killer and history information.
     * @param moves  List of moves to score.
     */
    final void scoreMoveList(Move[] moves, int ply) {
        scoreMoveList(moves, ply, 0);
    }
    final void scoreMoveList(Move[] moves, int ply, int startIdx) {
        for (int i = startIdx; moves[i] != null; i++) {
            Move m = moves[i];
            boolean isCapture = (pos.getPiece(m.to) != Piece.EMPTY) || (m.promoteTo != Piece.EMPTY);
            int score = isCapture ? SEE(m) : 0;
            int ks = kt.getKillerScore(ply, m);
            if (ks > 0) {
                score += ks + 50;
            } else {
                int hs = ht.getHistScore(pos, m);
                score += hs;
            }
            m.score = score;
        }
    }
    final void scoreCaptureList(Move[] moves, int ply) {
        for (int i = 0; moves[i] != null; i++) {
            Move m = moves[i];
            m.score = SEE(m);
        }
    }
    
    /**
     * Find move with highest score and move it to the front of the list.
     */
    final static void selectBest(Move[] moves, int startIdx) {
        int bestIdx = startIdx;
        int bestScore = moves[bestIdx].score;
        for (int i = startIdx + 1; moves[i] != null; i++) {
            int sc = moves[i].score;
            if (sc > bestScore) {
                bestIdx = i;
                bestScore = sc;
            }
        }
        if (bestIdx != startIdx) {
            Move m = moves[startIdx];
            moves[startIdx] = moves[bestIdx];
            moves[bestIdx] = m;
        }
    }

    /** If hashMove exists in the move list, move the hash move to the front of the list. */
    final static boolean selectHashMove(Move[] moves, Move hashMove) {
        if (hashMove == null) {
            return false;
        }
        for (int i = 0; moves[i] != null; i++) {
            Move m = moves[i];
            if (m.equals(hashMove)) {
                moves[i] = moves[0];
                moves[0] = m;
                m.score = 10000;
                return true;
            }
        }
        return false;
    }

    public final static boolean canClaimDraw50(Position pos) {
        return (pos.halfMoveClock >= 100);
    }
    
    public final static boolean canClaimDrawRep(Position pos, long[] posHashList, int posHashListSize, int posHashFirstNew) {
        int reps = 0;
        for (int i = posHashListSize - 4; i >= 0; i -= 2) {
            if (pos.zobristHash() == posHashList[i]) {
                reps++;
                if (i >= posHashFirstNew) {
                    reps++;
                    break;
                }
            }
        }
        return (reps >= 2);
    }

    private final void initNodeStats() {
        nodes = qNodes = 0;
        nodesPlyVec = new int[20];
        nodesDepthVec = new int[20];
        for (int i = 0; i < 20; i++) {
            nodesPlyVec[i] = 0;
            nodesDepthVec[i] = 0;
        }
    }
}
