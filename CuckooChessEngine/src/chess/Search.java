/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import chess.TranspositionTable.TTEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
    int posHashListSize;		// Number of used entries in posHashList
    int posHashFirstNew;        // First entry in posHashList that has not been played OTB.
    TranspositionTable tt;
    int depth;

    private static final class SearchTreeInfo {
        UndoInfo undoInfo;
        Move hashMove;         // Temporary storage for local hashMove variable
        boolean allowNullMove; // Don't allow two null-moves in a row
        Move bestMove;         // Copy of the best found move at this ply
        Move currentMove;      // Move currently being searched
        int lmr;               // LMR reduction amount
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
    int maxNodes;           // Maximum number of nodes to search (approximately)
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

    final public void timeLimit(int minTimeLimit, int maxTimeLimit) {
        minTimeMillis = minTimeLimit;
        maxTimeMillis = maxTimeLimit;
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
                return mi2.move.score - mi1.move.score;
            }
        }
        static public class SortByNodes implements Comparator<MoveInfo> {
            public int compare(MoveInfo mi1, MoveInfo mi2) {
                return mi2.nodes - mi1.nodes;
            }
        }
    }

    final public Move iterativeDeepening(ArrayList<Move> scMovesIn,
            int initialMinTimeMillis, int initialMaxTimeMillis,
            int maxDepth, int initialMaxNodes, boolean verbose) {
        tStart = System.currentTimeMillis();
        totalNodes = 0;
        ArrayList<MoveInfo> scMoves = new ArrayList<MoveInfo>(scMovesIn.size());
        for (Move m : scMovesIn)
        	scMoves.add(new MoveInfo(m, 0));
        minTimeMillis = initialMinTimeMillis;
        maxTimeMillis = initialMaxTimeMillis;
        maxNodes = initialMaxNodes;
        nodesToGo = 0;
        Position origPos = new Position(pos);
        final int aspirationDelta = 25;
        int bestScoreLastIter = 0;
        Move bestMove = scMoves.get(0).move;
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
            for (int mi = 0; mi < scMoves.size(); mi++) {
                searchNeedMoreTime = (mi > 0);
                Move m = scMoves.get(mi).move;
                if ((listener != null) && (System.currentTimeMillis() - tStart >= 1000)) {
                    listener.notifyCurrMove(m, mi + 1);
                }
                nodes = qNodes = 0;
                posHashList[posHashListSize++] = pos.zobristHash();
                boolean givesCheck = MoveGen.givesCheck(pos, m);
                pos.makeMove(m, ui);
                searchTreeInfo[0].currentMove = m;
                searchTreeInfo[0].lmr = 0;
                int beta;
                if (depth > 1) {
                    beta = (mi == 0) ? Math.min(bestScoreLastIter + aspirationDelta, Search.MATE0) : alpha + 1;
                } else {
                    beta = Search.MATE0;
                }
/*                int nodes0 = nodes;
                int qNodes0 = qNodes;
                System.out.printf("%2d %5s %5d %5d %6s %6s ",
                        mi, "-", alpha, beta, "-", "-");
                System.out.printf("%-6s...\n", TextIO.moveToUCIString(m)); */
                int score = -negaScout(-beta, -alpha, 1, depth - 1, -1, givesCheck);
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
                scMoves.get(mi).move.score = score;
                scMoves.get(mi).nodes = nodesThisMove;
                bestScore = Math.max(bestScore, score);
                if (depth > 1) {
                    if ((score > alpha) || (mi == 0)) {
                        alpha = score;
                        MoveInfo tmp = scMoves.get(mi);
                        for (int i = mi - 1; i >= 0;  i--) {
                            scMoves.set(i + 1, scMoves.get(i));
                        }
                        scMoves.set(0, tmp);
                        bestMove = scMoves.get(0).move;
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
                 Collections.sort(scMoves, new MoveInfo.SortByScore());
                 bestMove = scMoves.get(0).move;
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
            	if (scMoves.size() > 1)
            		Collections.sort(scMoves.subList(1, scMoves.size()),
            						 new MoveInfo.SortByNodes());
            }
        }
        } catch (StopSearch ss) {
            pos = origPos;
        }
        notifyStats();

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

        // Draw tests
        if (canClaimDraw50(pos)) {
            if (MoveGen.canTakeKing(pos))
                return MATE0 - ply;
            if (inCheck) {
                ArrayList<Move> moves = moveGen.pseudoLegalMoves(pos);
                moves = MoveGen.removeIllegal(pos, moves);
                if (moves.size() == 0) {            // Can't claim draw if already check mated.
                    return -(MATE0-(ply+1));
                }
            }
            return 0;
        }
        if (canClaimDrawRep(pos, posHashList, posHashListSize, posHashFirstNew)) {
            return 0;            // No need to test for mate here, since it would have been
                                 // discovered the first time the position came up.
        }

        int evalScore = UNKNOWN_SCORE;
        // Check transposition table
        TTEntry ent = tt.probe(pos.historyHash());
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
            tt.insert(pos.historyHash(), emptyMove, type, ply, depth, q0Eval);
            return score;
        }

        // Try null-move pruning
        sti.currentMove = emptyMove;
        if (    (depth >= 3) && (beta == alpha + 1) && !inCheck && sti.allowNullMove &&
                (Math.abs(beta) <= MATE0 / 2)) {
            if (MoveGen.canTakeKing(pos)) {
                return MATE0 - ply;
            }
            if (pos.whiteMove ? (pos.wMtrl > pos.wMtrlPawns) : (pos.bMtrl > pos.bMtrlPawns)) {
                final int R = (depth > 6) ? 3 : 2;
                pos.setWhiteMove(!pos.whiteMove);
                searchTreeInfo[ply+1].allowNullMove = false;
                int score = -negaScout(-beta, -(beta - 1), ply + 1, depth - R - 1, -1, false);
                searchTreeInfo[ply+1].allowNullMove = true;
                pos.setWhiteMove(!pos.whiteMove);
                if (score >= beta) {
                	if (score > MATE0 / 2)
                		return beta;
                    return score;
                } else {
                    if (searchTreeInfo[ply-1].lmr > 0) {
                        Move m1 = searchTreeInfo[ply-1].currentMove;
                        Move m2 = searchTreeInfo[ply+1].bestMove; // threat move
                        if (m1.from != m1.to) {
                            if ((m1.to == m2.from) || (m1.from == m2.to)) {
                                // if the threat move was made possible by a reduced
                                // move on the previous ply, the reduction was unsafe.
                                // Return alpha to trigger a non-reduced re-search.
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
                    margin = Evaluate.pieceValue[Piece.WKNIGHT] / 2;
                } else if (depth == 2) {
                    margin = Evaluate.pieceValue[Piece.WKNIGHT];
                } else {
                    margin = Evaluate.pieceValue[Piece.WROOK];
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
            negaScout(alpha, beta, ply, (depth > 8) ? (depth - 5) : (depth - 4), -1, inCheck);
            ent = tt.probe(pos.historyHash());
            if (ent.type != TTEntry.T_EMPTY) {
            	hashMove = sti.hashMove;
                ent.getMove(hashMove);
            }
        }

        // Start searching move alternatives
        ArrayList<Move> moves = moveGen.pseudoLegalMoves(pos);
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
        for (int mi = 0; mi < moves.size(); mi++) {
            if ((mi == 1) && !seeDone) {
                scoreMoveList(moves.subList(1, moves.size()), ply);
                seeDone = true;
            }
            if ((mi > 0) || !hashMoveSelected) {
                selectBest(moves, mi);
            }
            Move m = moves.get(mi);
            if (pos.getPiece(m.to) == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                moveGen.returnMoveList(moves);
                return MATE0-ply;       // King capture
            }
            int newCaptureSquare = -1;
            boolean isCapture = false;
            boolean isPromotion = (m.promoteTo != Piece.EMPTY);
            int sVal = Integer.MIN_VALUE;
            if (pos.getPiece(m.to) != Piece.EMPTY) {
                isCapture = true;
                int fVal = Evaluate.pieceValue[pos.getPiece(m.from)];
                int tVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                final int pV = Evaluate.pieceValue[Piece.WPAWN];
                if (Math.abs(tVal - fVal) < pV / 2) {    // "Equal" capture
                    sVal = SEE(m);
                    if (Math.abs(sVal) < pV / 2)
                        newCaptureSquare = m.to;
                }
            }
            int moveExtend = 0;
            if ((m.to == recaptureSquare) && (posExtend == 0)) {
                if (sVal == Integer.MIN_VALUE) sVal = SEE(m);
                int tVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                final int pV = Evaluate.pieceValue[Piece.WPAWN];
                if (sVal > tVal - pV / 2)
                    moveExtend = 1;
            }
            // FIXME! Test extending when going into pawn endgame
            // FIXME! Test extending pawn pushes to 7:th rank
            boolean mayReduce = (m.score < 53) && (!isCapture || m.score < 0) && !isPromotion;
            
            boolean givesCheck = MoveGen.givesCheck(pos, m); 
            boolean doFutility = false;
            if (futilityPrune && mayReduce && haveLegalMoves) {
                if (!givesCheck)
                	doFutility = true;
            }
            int score;
            if (doFutility) {
            	score = futilityScore;
            } else {
            	int extend = Math.max(posExtend, moveExtend);
            	int lmr = 0;
            	if ((depth >= 3) && mayReduce && (beta == alpha + 1) && (extend == 0)) {
            		if (!givesCheck) {
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
/*            	int nodes0 = nodes;
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
/*            	if (ply <= 3) {
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
                    ht.addSuccess(pos, m);
                    for (int mi2 = mi - 1; mi2 >= 0; mi2--) {
                        ht.addFail(pos, moves.get(mi2));
                    }
                }
                tt.insert(pos.historyHash(), m, TTEntry.T_GE, ply, depth, evalScore);
                moveGen.returnMoveList(moves);
                return alpha;
            }
            b = alpha + 1;
        }
        if (!haveLegalMoves && !inCheck) {
            moveGen.returnMoveList(moves);
            return 0;       // Stale-mate
        }
        if (bestMove >= 0) {
            tt.insert(pos.historyHash(), moves.get(bestMove), TTEntry.T_EXACT, ply, depth, evalScore);
        } else {
            emptyMove.score = bestScore;
            tt.insert(pos.historyHash(), emptyMove, TTEntry.T_LE, ply, depth, evalScore);
        }
        moveGen.returnMoveList(moves);
        return bestScore;
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
        ArrayList<Move> moves;
        if (inCheck) {
            moves = moveGen.pseudoLegalMoves(pos);
            scoreMoveList(moves, ply);
        } else if (tryChecks) {
            moves = moveGen.pseudoLegalCapturesAndChecks(pos);
            scoreMoveList(moves, ply);
        } else {
        	moves = moveGen.pseudoLegalCaptures(pos);
        	scoreCaptureList(moves, ply);
        }
        UndoInfo ui = searchTreeInfo[ply].undoInfo;
        final int nMoves = moves.size();
        for (int mi = 0; mi < nMoves; mi++) {
            if (mi < 8) {
                // If the first 8 moves didn't fail high, this is probably an ALL-node,
                // so spending more effort on move ordering is probably wasted time.
                selectBest(moves, mi);
            }
            Move m = moves.get(mi);
            if (pos.getPiece(m.to) == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                qNodes++;
                totalNodes++;
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

    
    private int[] dirIdx = new int[8];       // Current position in each direction.
    private int[] captures = new int[64];   // Value of captured pieces
    private UndoInfo seeUi = new UndoInfo();

    /**
     * Static exchange evaluation function.
     * @return SEE score for m. Positive value is good for the side that makes the first move.
     */
    final public int SEE(Move m) {
        final int kV = Evaluate.pieceValue[Piece.WKING];
        
        final int square = m.to;
        if (square == pos.getEpSquare()) {
            captures[0] = Evaluate.pieceValue[Piece.WPAWN];
        } else {
            captures[0] = Evaluate.pieceValue[pos.getPiece(square)];
            if (captures[0] == kV)
            	return kV;
        }
        int nCapt = 1;                  // Number of entries in captures[]

        pos.makeMove(m, seeUi);
        boolean white = pos.whiteMove;
        int valOnSquare = Evaluate.pieceValue[pos.getPiece(square)];

        for (int d = 0; d < 8; d++) {
        	dirIdx[d] = SEEnextAttacker(square, d, 0);
        }
        int wNatks = Long.bitCount(BitBoard.knightAttacks[square] & pos.pieceTypeBB[Piece.WKNIGHT]);
        int bNatks = Long.bitCount(BitBoard.knightAttacks[square] & pos.pieceTypeBB[Piece.BKNIGHT]);

        final int nV = Evaluate.pieceValue[Piece.WKNIGHT];

        while (true) {
        	int bestDir = -1;
        	int bestValue = Integer.MAX_VALUE;
        	for (int d = 0; d < 8; d++) {
        		int idx = dirIdx[d];
        		if (idx > 0) {
        	        int p = pos.getPiece(square + dirD[d] * idx);
        	        if (Piece.isWhite(p) == white) {
        	            int val = Evaluate.pieceValue[p];
            			if (val < bestValue) {
            				bestDir = d;
            				bestValue = val;
            			}
        	        }
        		}
        	}
        	if ((white ? wNatks : bNatks) > 0) {
        		if (nV < bestValue) {
        			bestValue = nV;
        			bestDir = 8;
        		}
        	}
        	if (bestDir == -1) {
        		break;
        	}
        	captures[nCapt++] = valOnSquare;
        	if (valOnSquare == kV) {
        		break;
        	}
        	valOnSquare = bestValue;
        	if (bestDir == 8) {
        		if (white) {
        			wNatks--;
        		} else {
        			bNatks--;
        		}
        	} else {
        		dirIdx[bestDir] = SEEnextAttacker(square, bestDir, dirIdx[bestDir]);
        	}
        	white = !white;
        }
        pos.unMakeMove(m, seeUi);
        
        int score = 0;
        for (int i = nCapt - 1; i > 0; i--) {
            score = Math.max(0, captures[i] - score);
        }
        return captures[0] - score;
    }

    static final int[] dirDx = { 1, 1, 0, -1, -1, -1,  0,  1 };
    static final int[] dirDy = { 0, 1, 1,  1,  0, -1, -1, -1 };
    static final int[] dirD  = { 1, 9, 8,  7, -1, -9, -8, -7 };

    final private int SEEnextAttacker(int square, int direction, int index) {
        index++;
        int x = Position.getX(square) + dirDx[direction] * index;
        int y = Position.getY(square) + dirDy[direction] * index;
        square += dirD[direction] * index;
        for (;;) {
            if ((x < 0) || (x >= 8) || (y < 0) || (y >= 8)) {
                return -1;      // Outside board
            }
            int p = pos.getPiece(square);
            switch (p) {
            case Piece.WKING: case Piece.BKING:
            	if (index == 1) return 1; else return -1;
            case Piece.WPAWN:
            	if ((index == 1) && (dirDy[direction] == -1) && (dirDx[direction] != 0)) return 1; else return -1;
            case Piece.BPAWN:
            	if ((index == 1) && (dirDy[direction] == 1) && (dirDx[direction] != 0)) return 1; else return -1;
            case Piece.WQUEEN: case Piece.BQUEEN:
            	return index;
            case Piece.WROOK: case Piece.BROOK:
            	if ((direction & 1) == 0) return index; else return -1;
            case Piece.WBISHOP: case Piece.BBISHOP:
            	if ((direction & 1) != 0) return index; else return -1;
            case Piece.EMPTY:
            	break;
            default:
            	return -1;
            }
            x += dirDx[direction];
            y += dirDy[direction];
            square += dirD[direction];
            index++;
        }
    }
    
    /**
     * Compute scores for each move in a move list, using SEE, killer and history information.
     * @param moves  List of moves to score.
     */
    final void scoreMoveList(List<Move> moves, int ply) {
    	final int mSize = moves.size();
        for (int i = 0; i < mSize; i++) {
            Move m = moves.get(i);
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
    final void scoreCaptureList(List<Move> moves, int ply) {
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            m.score = SEE(m);
        }
    }
    
    /**
     * Find move with highest score and move it to the front of the list.
     */
    final static void selectBest(List<Move> moves, int startIdx) {
    	int mSize = moves.size();
        if (mSize - startIdx < 2)
            return;
        int bestIdx = startIdx;
        int bestScore = moves.get(bestIdx).score;
        for (int i = startIdx + 1; i < mSize; i++) {
        	int sc = moves.get(i).score;
            if (sc > bestScore) {
                bestIdx = i;
                bestScore = sc;
            }
        }
        if (bestIdx != startIdx) {
            Move m = moves.get(startIdx);
            moves.set(startIdx, moves.get(bestIdx));
            moves.set(bestIdx, m);
        }
    }

    /** If hashMove exists in the move list, move the hash move to the front of the list. */
    final static boolean selectHashMove(ArrayList<Move> moves, Move hashMove) {
        if (hashMove == null) {
            return false;
        }
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            if (m.equals(hashMove)) {
                moves.set(i, moves.get(0));
                moves.set(0, m);
                m.score = 10000;
                return true;
            }
        }
        return false;
    }

    public static boolean canClaimDraw50(Position pos) {
        return (pos.halfMoveClock >= 100);
    }
    
    public static boolean canClaimDrawRep(Position pos, long[] posHashList, int posHashListSize, int posHashFirstNew) {
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

    private void initNodeStats() {
        nodes = qNodes = 0;
        nodesPlyVec = new int[20];
        nodesDepthVec = new int[20];
        for (int i = 0; i < 20; i++) {
            nodesPlyVec[i] = 0;
            nodesDepthVec[i] = 0;
        }
    }
}
