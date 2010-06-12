/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import chess.TranspositionTable.TTEntry;

import java.util.ArrayList;
import java.util.Collections;
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
    UndoInfo[] undoInfoVec;

    // Time management
    long tStart;            // Time when search started
    long minTimeMillis;     // Minimum recommended thinking time
    long maxTimeMillis;     // Maximum allowed thinking time
    boolean searchNeedMoreTime; // True if negaScout should use up to maxTimeMillis time.
    int maxNodes;           // Maximum number of nodes to search (approximately)
    
    // Search statistics stuff
    int nodes;
    int qNodes;
    int[] nodesPlyVec;
    int[] nodesDepthVec;
    int totalNodes;
    long tLastStats;        // Time when notifyStats was last called
    boolean verbose;

    public final static int MATE0 = 32000;

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
        final int uiVecLen = 200;
        undoInfoVec = new UndoInfo[uiVecLen];
        for (int i = 0; i < uiVecLen; i++) {
        	undoInfoVec[i] = new UndoInfo();
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

    final public Move iterativeDeepening(ArrayList<Move> scMoves,
            int initialMinTimeMillis, int initialMaxTimeMillis,
            int maxDepth, int initialMaxNodes, boolean verbose) {
        tStart = System.currentTimeMillis();
        totalNodes = 0;
        minTimeMillis = initialMinTimeMillis;
        maxTimeMillis = initialMaxTimeMillis;
        maxNodes = initialMaxNodes;
        Position origPos = new Position(pos);
        final int aspirationDelta = 50;
        int bestScoreLastIter = 0;
        Move bestMove = scMoves.get(0);
        this.verbose = verbose;
        if ((maxDepth < 0) || (maxDepth > 100)) {
        	maxDepth = 100;
        }
        try {
        for (int depth = 1; ; depth++) {
            // FIXME!!! Order moves based on number of nodes in previous iteration
            initNodeStats();
            if (listener != null) listener.notifyDepth(depth);
            int alpha = depth > 1 ? Math.max(bestScoreLastIter - aspirationDelta, -Search.MATE0) : -Search.MATE0;
            int bestScore = -Search.MATE0;
            UndoInfo ui = new UndoInfo();
            boolean needMoreTime = false;
            for (int mi = 0; mi < scMoves.size(); mi++) {
                searchNeedMoreTime = (mi > 0);
                Move m = scMoves.get(mi);
                if ((listener != null) && (System.currentTimeMillis() - tStart >= 1000)) {
                    listener.notifyCurrMove(m, mi + 1);
                }
                nodes = qNodes = 0;
                posHashList[posHashListSize++] = pos.zobristHash();
                pos.makeMove(m, ui);
                int beta;
                if (depth > 1) {
                    beta = (mi == 0) ? Math.min(bestScoreLastIter + aspirationDelta, Search.MATE0) : alpha + 1;
                } else {
                    beta = Search.MATE0;
                }
                int score = -negaScout(-beta, -alpha, 1, depth - 1, -1);
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
                    tt.insert(pos.historyHash(), m, type, 0, depth);
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
                    score = -negaScout(-Search.MATE0, -score, 1, depth - 1, -1);
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
                    score = -negaScout(-score, Search.MATE0, 1, depth - 1, -1);
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
                        System.out.printf("%-6s %6d %6d %6d%s %s\n",
                                TextIO.moveToString(pos, m, false), score,
                                nodes, qNodes, (score > alpha ? " *" : ""), PV);
                    }
                    if (havePV && (depth > 1)) {
                        notifyPV(depth, score, false, false, m);
                    }
                }
                scMoves.get(mi).score = score;
                bestScore = Math.max(bestScore, score);
                if (depth > 1) {
                    if ((score > alpha) || (mi == 0)) {
                        alpha = score;
                        Move tmp = scMoves.get(mi);
                        for (int i = mi - 1; i >= 0;  i--) {
                            scMoves.set(i + 1, scMoves.get(i));
                        }
                        scMoves.set(0, tmp);
                        bestMove = scMoves.get(0);
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
                 Collections.sort(scMoves, new Move.SortByScore());
                 bestMove = scMoves.get(0);
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
    final public int negaScout(int alpha, int beta, int ply, int depth, int recaptureSquare) throws StopSearch {
        if (depth > 2) {
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
            if (MoveGen.inCheck(pos)) {
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

        // Check transposition table
        TTEntry ent = tt.probe(pos.historyHash());
        Move hashMove = null;
        if (ent.type != TTEntry.T_EMPTY) {
            int score = ent.getScore(ply);
            int plyToMate = MATE0 - Math.abs(score);
            if ((beta == alpha + 1) && ((ent.depth >= depth) || (ent.depth >= plyToMate))) {
                if (    (ent.type == TTEntry.T_EXACT) ||
                        (ent.type == TTEntry.T_GE) && (score >= beta) ||
                        (ent.type == TTEntry.T_LE) && (score <= alpha)) {
                    return score;
                }
            }
            hashMove = ent.getMove();
        }
        
        boolean inCheck = MoveGen.inCheck(pos);
        int posExtend = inCheck ? 1 : 0; // Check extension

        // If out of depth, perform quiescence search
        if (depth + posExtend <= 0) {
            qNodes--;
            totalNodes--;
            int score = quiesce(alpha, beta, ply, 0, inCheck);
            int type = TTEntry.T_EXACT;
            if (score <= alpha) {
                type = TTEntry.T_LE;
            } else if (score >= beta) {
                type = TTEntry.T_GE;
            }
            emptyMove.score = score;
            tt.insert(pos.historyHash(), emptyMove, type, ply, depth);
            return score;
        }

        // Try null-move pruning
        if (    (depth > 3) && (beta == alpha + 1) && !inCheck &&
                (Math.abs(beta) <= MATE0 / 2)) {
            if (MoveGen.canTakeKing(pos)) {
                return MATE0 - ply;
            }
            int mtrl = eval.material(pos, pos.whiteMove);
            int pV = Evaluate.pieceValue[Piece.WPAWN];
            int pMtrl = pV * pos.nPieces(pos.whiteMove ? Piece.WPAWN : Piece.BPAWN);
            if (mtrl > pMtrl) {
                final int R = (depth > 6) ? 3 : 2;
                pos.setWhiteMove(!pos.whiteMove);
                int score = -negaScout(-beta, -(beta - 1), ply + 1, depth - R - 1, -1);
                pos.setWhiteMove(!pos.whiteMove);
                if (score >= beta) {
                    return score;
                } else {
                    if (score == -(MATE0-(ply+3))) {
                        posExtend = 1; // mate threat extension
                    }
                }
            }
        }

        boolean futilityPrune = false;
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
                int e = eval.evalPos(pos);
                if (e + margin <= alpha) {
                    futilityPrune = true;
                }
            }
        }
        
        if ((depth > 4) && (beta > alpha + 1) && ((hashMove == null) || (hashMove.from == hashMove.to))) {
            // No hash move at PV node. Try internal iterative deepening.
            negaScout(alpha, beta, ply, depth - 4, -1);
            ent = tt.probe(pos.historyHash());
            if (ent.type != TTEntry.T_EMPTY) {
                hashMove = ent.getMove();
            }
        }
        
        // Start searching move alternatives
        ArrayList<Move> moves = moveGen.pseudoLegalMoves(pos);
        boolean seeDone = false;
        boolean hashMoveSelected = true;
        if (!selectHashMove(moves, hashMove)) {
            scoreMoveList(moves, false, ply);
            seeDone = true;
            hashMoveSelected = false;
        }
        
        UndoInfo ui = undoInfoVec[ply];
        boolean haveLegalMoves = false;
        int illegalScore = -(MATE0-(ply+1));
        int b = beta;
        int bestScore = illegalScore;
        int bestMove = -1;
        int lmrCount = 0;
        for (int mi = 0; mi < moves.size(); mi++) {
            if ((mi == 1) && !seeDone) {
                scoreMoveList(moves.subList(1, moves.size()), false, ply);
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
            if (pos.getPiece(m.to) != Piece.EMPTY) {
                isCapture = true;
                int fVal = Evaluate.pieceValue[pos.getPiece(m.from)];
                int tVal = Evaluate.pieceValue[pos.getPiece(m.to)];
                final int pV = Evaluate.pieceValue[Piece.WPAWN];
                if (Math.abs(tVal - fVal) < pV / 2) {    // "Equal" capture
                    int sVal = SEE(m);
                    if (Math.abs(sVal) < pV / 2)
                        newCaptureSquare = m.to;
                }
            }
            int moveExtend = 0;
            if (m.to == recaptureSquare) {
                moveExtend = 1;
            }
            
            if (futilityPrune && !isCapture && !isPromotion && haveLegalMoves) {
                pos.makeMove(m, ui);
                boolean givesCheck = MoveGen.inCheck(pos);
                pos.unMakeMove(m, ui);
                if (!givesCheck)
                    continue;
            }
            int extend = Math.max(posExtend, moveExtend);

            int lmr = 0;
            if ((depth >= 3) && !inCheck && !isCapture && (beta == alpha + 1) &&
                    (extend == 0) && !isPromotion) {
                pos.makeMove(m, ui);
                boolean givesCheck = MoveGen.inCheck(pos);
                pos.unMakeMove(m, ui);
                if (!givesCheck) {
                    lmrCount++;
                    if (lmrCount > 3) {
                        lmr = 1;
                    }
                }
            }

            posHashList[posHashListSize++] = pos.zobristHash();
            pos.makeMove(m, ui);
            int newDepth = depth - 1 + extend - lmr;
            int score = -negaScout(-b, -alpha, ply + 1, newDepth, newCaptureSquare);
            if ((score > alpha) && (score < beta) && (b != beta) && (score != illegalScore)) {
                newDepth += lmr;
                score = -negaScout(-beta, -alpha, ply + 1, newDepth, newCaptureSquare);
            }
            m.score = score;
            posHashListSize--;
            pos.unMakeMove(m, ui);

            if (score != illegalScore) {
                haveLegalMoves = true;
            }
            bestScore = Math.max(bestScore, score);
            if (score > alpha) {
                alpha = score;
                bestMove = mi;
            }
            if (alpha >= beta) {
                if (pos.getPiece(m.to) == Piece.EMPTY) {
                    kt.addKiller(ply, m);
                    ht.addSuccess(pos, m);
                    for (int mi2 = mi - 1; mi2 >= 0; mi2--) {
                        ht.addFail(pos, moves.get(mi2));
                    }
                }
                tt.insert(pos.historyHash(), m, TTEntry.T_GE, ply, depth);
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
            tt.insert(pos.historyHash(), moves.get(bestMove), TTEntry.T_EXACT, ply, depth);
        } else {
            emptyMove.score = bestScore;
            tt.insert(pos.historyHash(), emptyMove, TTEntry.T_LE, ply, depth);
        }
        moveGen.returnMoveList(moves);
        return bestScore;
    }

    /**
     * Quiescence search. Only non-losing captures are searched.
     */
    final public int quiesce(int alpha, int beta, int ply, int depth, final boolean inCheck) {
        qNodes++;
        totalNodes++;
        int score = inCheck ? -(MATE0 - (ply+1)) : eval.evalPos(pos);
        if (score >= beta) {
            if ((depth == 0) && MoveGen.canTakeKing(pos)) {
                // To make stale-mate detection in negaScout() work
                return MATE0 - ply;
            }
            return score;
        }
        if (score > alpha)
            alpha = score;
        int bestScore = score;
        ArrayList<Move> moves = moveGen.pseudoLegalMoves(pos);
        final boolean tryChecks = (depth > -3);
        final boolean onlyCaptures = !inCheck && !tryChecks;
        scoreMoveList(moves, onlyCaptures, ply);
        UndoInfo ui = undoInfoVec[ply];
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
                    if (!tryChecks) {
                        continue;
                    }
                    pos.makeMove(m, ui);
                    givesCheck = MoveGen.inCheck(pos);
                    givesCheckComputed = true;
                    if (!givesCheck) {
                        pos.unMakeMove(m, ui);
                        continue;
                    }
                }
            }

            if (!givesCheckComputed) {
                pos.makeMove(m, ui);
                if (depth - 1 > -4) {
                    givesCheck = MoveGen.inCheck(pos);
                }
            }
            final boolean nextInCheck = (depth - 1) > -4 ? givesCheck : false;

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
        int nCapt = 0;                  // Number of entries in captures[]
        int pV = Evaluate.pieceValue[Piece.WPAWN];
        
        final int square = m.to;
        captures[nCapt] = SEEgetPieceValue(pos.getPiece(square));
        if (square == pos.getEpSquare()) {
            captures[nCapt] = pV;
        }
        nCapt++;

        if (captures[0] != SEEkingValue) {
            pos.makeMove(m, seeUi);
            boolean white = pos.whiteMove;
            int valOnSquare = SEEgetPieceValue(pos.getPiece(square));

            for (int d = 0; d < 8; d++) {
                dirIdx[d] = SEEnextAttacker(square, d, 0);
            }
            int wNattacks = MoveGen.numKnightAttacks(pos, square, true);
            int bNattacks = MoveGen.numKnightAttacks(pos, square, false);
            final int nV = Evaluate.pieceValue[Piece.WKNIGHT];
            
            while (true) {
                int bestDir = -1;
                int bestValue = Integer.MAX_VALUE;
                for (int d = 0; d < 8; d++) {
                    int idx = dirIdx[d];
                    if (idx > 0) {
                        int val = SEEgetAttackerValue(square, white, d, idx);
                        if (val < bestValue) {
                            bestDir = d;
                            bestValue = val;
                        }
                    }
                }
                if ((white ? wNattacks : bNattacks) > 0) {
                    if (nV < bestValue) {
                        bestValue = nV;
                        bestDir = 8;
                    }
                }
                if (bestDir == -1) {
                    break;
                }
                captures[nCapt++] = valOnSquare;
                if (valOnSquare == SEEkingValue) {
                    break;
                }
                valOnSquare = bestValue;
                if (bestDir == 8) {
                    if (white) {
                        wNattacks--;
                    } else {
                        bNattacks--;
                    }
                } else {
                    dirIdx[bestDir] = SEEnextAttacker(square, bestDir, dirIdx[bestDir]);
                }
                white = !white;
            }
            pos.unMakeMove(m, seeUi);
        }
        
        int score = 0;
        for (int i = nCapt - 1; i > 0; i--) {
            score = Math.max(0, captures[i] - score);
        }
        return captures[0] - score;
    }

    static final int SEEkingValue = 9900;
    static final int[] dirDx = { 1, 1, 0, -1, -1, -1,  0,  1 };
    static final int[] dirDy = { 0, 1, 1,  1,  0, -1, -1, -1 };

    final private int SEEnextAttacker(int square, int direction, int index) {
        index++;
        int x = Position.getX(square) + dirDx[direction] * index;
        int y = Position.getY(square) + dirDy[direction] * index;
        for (;;) {
            if ((x < 0) || (x >= 8) || (y < 0) || (y >= 8)) {
                return -1;      // Outside board
            }
            int p = pos.getPiece(Position.getSquare(x, y));
            if (index == 1) {
                if ((p == Piece.WKING) || (p == Piece.BKING))
                    return index;
                if ((p == Piece.WPAWN) && (dirDy[direction] == -1) && (dirDx[direction] != 0))
                    return index;
                if ((p == Piece.BPAWN) && (dirDy[direction] == 1) && (dirDx[direction] != 0))
                    return index;
            }
            if ((p == Piece.WQUEEN) || (p == Piece.BQUEEN))
                return index;
            if ((direction & 1) == 0) {
                if ((p == Piece.WROOK) || (p == Piece.BROOK))
                    return index;
            } else {
                if ((p == Piece.WBISHOP) || (p == Piece.BBISHOP))
                    return index;
            }
            if (p != Piece.EMPTY) {
                return -1;      // Further attacks blocked by wrong piece type.
            }

            x += dirDx[direction];
            y += dirDy[direction];
            index++;
        }
    }
    
    /**
     * Get the value of an attacker piece in the indicated direction/distance.
     * If the piece has wrong color, MAX_VALUE is returned.
     */
    final private int SEEgetAttackerValue(int square, boolean white, int direction, int index) {
        int x = Position.getX(square) + dirDx[direction] * index;
        int y = Position.getY(square) + dirDy[direction] * index;
        int p = pos.getPiece(Position.getSquare(x, y));
        if (Piece.isWhite(p) == white) {
            return SEEgetPieceValue(p);
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /** Get the SEE piece value. The only difference from normal piece values is the king value. */
    final private static int SEEgetPieceValue(int p) {
        if ((p == Piece.WKING) || (p == Piece.BKING)) {
            return SEEkingValue;
        } else {
            return Evaluate.pieceValue[p];
        }
    }

    /**
     * Compute scores for each move in a move list, using SEE, killer and history information.
     * @param moves  List of moves to score.
     * @param onlyCaptures  If true, remove non-capture moves
     */
    final void scoreMoveList(List<Move> moves, boolean onlyCaptures, int ply) {
        int savedIdx = 0;
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            boolean isCapture = (pos.getPiece(m.to) != Piece.EMPTY) || (m.promoteTo != Piece.EMPTY);
            if (onlyCaptures && !isCapture) {
                moveGen.returnMove(m);
                continue;
            }
            int score = isCapture ? SEE(m) : 0;
            if (!onlyCaptures) {
                int ks = kt.getKillerScore(ply, m);
                if (ks > 0) {
                    score += ks + 50;
                } else {
                    int hs = ht.getHistScore(pos, m);
                    score += hs;
                }
            }
            m.score = score;
            moves.set(savedIdx++, m);
        }
        while (moves.size() > savedIdx) {
            moves.remove(moves.size() - 1);
        }
    }

    /**
     * Find move with highest score and move it to the front of the list.
     */
    final void selectBest(List<Move> moves, int startIdx) {
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
