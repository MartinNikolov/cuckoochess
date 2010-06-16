/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import java.util.ArrayList;

/**
 *
 * @author petero
 */
public class MoveGen {
	static MoveGen instance;
	static {
		instance = new MoveGen();
	}

    /**
     * Generate and return a list of pseudo-legal moves.
     * Pseudo-legal means that the moves doesn't necessarily defend from check threats.
     */
    public final ArrayList<Move> pseudoLegalMoves(Position pos) {
        ArrayList<Move> moveList = getMoveListObj();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int p = pos.getPiece(Position.getSquare(x, y));
                if ((p == Piece.EMPTY) || (Piece.isWhite(p) != pos.whiteMove)) {
                    continue;
                }
                if ((p == Piece.WROOK) || (p == Piece.BROOK) || (p == Piece.WQUEEN) || (p == Piece.BQUEEN)) {
                    if (addDirection(moveList, pos, x, y,  1,  0, true)) return moveList;
                    if (addDirection(moveList, pos, x, y,  0,  1, true)) return moveList;
                    if (addDirection(moveList, pos, x, y, -1,  0, true)) return moveList;
                    if (addDirection(moveList, pos, x, y,  0, -1, true)) return moveList;
                }
                if ((p == Piece.WBISHOP) || (p == Piece.BBISHOP) || (p == Piece.WQUEEN) || (p == Piece.BQUEEN)) {
                    if (addDirection(moveList, pos, x, y,  1,  1, true)) return moveList;
                    if (addDirection(moveList, pos, x, y, -1,  1, true)) return moveList;
                    if (addDirection(moveList, pos, x, y, -1, -1, true)) return moveList;
                    if (addDirection(moveList, pos, x, y,  1, -1, true)) return moveList;
                }
                if ((p == Piece.WKNIGHT) || (p == Piece.BKNIGHT)) {
                    if (addDirection(moveList, pos, x, y,  2,  1, false)) return moveList;
                    if (addDirection(moveList, pos, x, y,  1,  2, false)) return moveList;
                    if (addDirection(moveList, pos, x, y, -1,  2, false)) return moveList;
                    if (addDirection(moveList, pos, x, y, -2,  1, false)) return moveList;
                    if (addDirection(moveList, pos, x, y, -2, -1, false)) return moveList;
                    if (addDirection(moveList, pos, x, y, -1, -2, false)) return moveList;
                    if (addDirection(moveList, pos, x, y,  1, -2, false)) return moveList;
                    if (addDirection(moveList, pos, x, y,  2, -1, false)) return moveList;
                }
                if ((p == Piece.WKING) || (p == Piece.BKING)) {
                    if (addDirection(moveList, pos, x, y,  1,  0, false)) return moveList;
                    if (addDirection(moveList, pos, x, y,  1,  1, false)) return moveList;
                    if (addDirection(moveList, pos, x, y,  0,  1, false)) return moveList;
                    if (addDirection(moveList, pos, x, y, -1,  1, false)) return moveList;
                    if (addDirection(moveList, pos, x, y, -1,  0, false)) return moveList;
                    if (addDirection(moveList, pos, x, y, -1, -1, false)) return moveList;
                    if (addDirection(moveList, pos, x, y,  0, -1, false)) return moveList;
                    if (addDirection(moveList, pos, x, y,  1, -1, false)) return moveList;
		    
                    int k0 = pos.whiteMove ? Position.getSquare(4,0) : Position.getSquare(4,7);
                    if (Position.getSquare(x,y) == k0) {
                        int aCastle = pos.whiteMove ? Position.A1_CASTLE : Position.A8_CASTLE;
                        int hCastle = pos.whiteMove ? Position.H1_CASTLE : Position.H8_CASTLE;
                        int rook = pos.whiteMove ? Piece.WROOK : Piece.BROOK;
                        if (((pos.getCastleMask() & (1 << hCastle)) != 0) &&
                                (pos.getPiece(k0 + 1) == Piece.EMPTY) &&
                                (pos.getPiece(k0 + 2) == Piece.EMPTY) &&
                                (pos.getPiece(k0 + 3) == rook) &&
                                !sqAttacked(pos, k0) &&
                                !sqAttacked(pos, k0 + 1)) {
                            moveList.add(getMoveObj(k0, k0 + 2, Piece.EMPTY));
                        }
                        if (((pos.getCastleMask() & (1 << aCastle)) != 0) &&
                                (pos.getPiece(k0 - 1) == Piece.EMPTY) &&
                                (pos.getPiece(k0 - 2) == Piece.EMPTY) &&
                                (pos.getPiece(k0 - 3) == Piece.EMPTY) &&
                                (pos.getPiece(k0 - 4) == rook) &&
                                !sqAttacked(pos, k0) &&
                                !sqAttacked(pos, k0 - 1)) {
                            moveList.add(getMoveObj(k0, k0 - 2, Piece.EMPTY));
                        }
                    }
                }
                if ((p == Piece.WPAWN) || (p == Piece.BPAWN)) {
                    int yDir = pos.whiteMove ? 1 : -1;
                    if (pos.getPiece(Position.getSquare(x, y + yDir)) == Piece.EMPTY) { // non-capture
                        addPawnMoves(moveList, x, y, x, y + yDir);
                        if ((y == (pos.whiteMove ? 1 : 6)) &&
                                (pos.getPiece(Position.getSquare(x, y + 2 * yDir)) == Piece.EMPTY)) { // double step
                            addPawnMoves(moveList, x, y, x, y + 2 * yDir);
                        }
                    }
                    if (x > 0) { // Capture to the left
                        int cap = pos.getPiece(Position.getSquare(x - 1, y + yDir));
                        if (cap != Piece.EMPTY) {
                            if (Piece.isWhite(cap) != pos.whiteMove) {
                                if (cap == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                                    returnMoveList(moveList);
                                    moveList = getMoveListObj();
                                    moveList.add(getMoveObj(Position.getSquare(x, y), Position.getSquare(x - 1, y + yDir), Piece.EMPTY));
                                    return moveList;
                                } else {
                                    addPawnMoves(moveList, x, y, x - 1, y + yDir);
                                }
                            }
                        } else if (Position.getSquare(x - 1, y + yDir) == pos.getEpSquare()) {
                            addPawnMoves(moveList, x, y, x - 1, y + yDir);
                        }
                    }
                    if (x < 7) { // Capture to the right
                        int cap = pos.getPiece(Position.getSquare(x + 1, y + yDir));
                        if (cap != Piece.EMPTY) {
                            if (Piece.isWhite(cap) != pos.whiteMove) {
                                if (cap == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                                    returnMoveList(moveList);
                                    moveList = getMoveListObj();
                                    moveList.add(getMoveObj(Position.getSquare(x, y), Position.getSquare(x + 1, y + yDir), Piece.EMPTY));
                                    return moveList;
                                } else {
                                    addPawnMoves(moveList, x, y, x + 1, y + yDir);
                                }
                            }
                        } else if (Position.getSquare(x + 1, y + yDir) == pos.getEpSquare()) {
                            addPawnMoves(moveList, x, y, x + 1, y + yDir);
                        }
                    }
                }
            }
        }
        return moveList;
    }
    
    /**
     * Return true if the side to move is in check.
     */
    public static final boolean inCheck(Position pos) {
        int kingSq = pos.getKingSq(pos.whiteMove);
        if (kingSq < 0)
            return false;
        return sqAttacked(pos, kingSq);
    }

    /**
     * Return true if the side to move can take the opponents king.
     */
    public static final boolean canTakeKing(Position pos) {
        pos.setWhiteMove(!pos.whiteMove);
        boolean ret = inCheck(pos);
        pos.setWhiteMove(!pos.whiteMove);
        return ret;
    }

    /**
     * Return true if a square is attacked by the opposite side.
     */
    public static final boolean sqAttacked(Position pos, int square) {
        int x0 = Position.getX(square);
        int y0 = Position.getY(square);
        boolean isWhiteMove = pos.whiteMove;

        int oQueen= isWhiteMove ? Piece.BQUEEN: Piece.WQUEEN;
        int oRook = isWhiteMove ? Piece.BROOK : Piece.WROOK;
        if (checkDirection(pos, x0, y0,  1,  0, oRook, oQueen, true)) return true;
        if (checkDirection(pos, x0, y0,  0,  1, oRook, oQueen, true)) return true;
        if (checkDirection(pos, x0, y0, -1,  0, oRook, oQueen, true)) return true;
        if (checkDirection(pos, x0, y0,  0, -1, oRook, oQueen, true)) return true;

        int oBish = isWhiteMove ? Piece.BBISHOP: Piece.WBISHOP;
        if (checkDirection(pos, x0, y0,  1,  1, oBish, oQueen, true)) return true;
        if (checkDirection(pos, x0, y0, -1,  1, oBish, oQueen, true)) return true;
        if (checkDirection(pos, x0, y0, -1, -1, oBish, oQueen, true)) return true;
        if (checkDirection(pos, x0, y0,  1, -1, oBish, oQueen, true)) return true;

        int oKnight = isWhiteMove ? Piece.BKNIGHT : Piece.WKNIGHT;
        if (checkDirection(pos, x0, y0,  2,  1, oKnight, oKnight, false)) return true;
        if (checkDirection(pos, x0, y0,  1,  2, oKnight, oKnight, false)) return true;
        if (checkDirection(pos, x0, y0, -1,  2, oKnight, oKnight, false)) return true;
        if (checkDirection(pos, x0, y0, -2,  1, oKnight, oKnight, false)) return true;
        if (checkDirection(pos, x0, y0, -2, -1, oKnight, oKnight, false)) return true;
        if (checkDirection(pos, x0, y0, -1, -2, oKnight, oKnight, false)) return true;
        if (checkDirection(pos, x0, y0,  1, -2, oKnight, oKnight, false)) return true;
        if (checkDirection(pos, x0, y0,  2, -1, oKnight, oKnight, false)) return true;

        int oPawn= isWhiteMove ? Piece.BPAWN: Piece.WPAWN;
        int fwDir = isWhiteMove ? 1 : -1;
        if (checkDirection(pos, x0, y0,  1, fwDir, oPawn, oPawn, false)) return true;
        if (checkDirection(pos, x0, y0, -1, fwDir, oPawn, oPawn, false)) return true;

        int oKingSq = pos.getKingSq(!pos.whiteMove);
        if (oKingSq >= 0) {
            int ox0 = Position.getX(oKingSq);
            int oy0 = Position.getY(oKingSq);
            if ((Math.abs(x0 - ox0) <= 1) && (Math.abs(y0 - oy0) <= 1))
                return true;
        }
        
        return false;
    }

    /**
     * Remove all illegal moves from moveList.
     * "moveList" is assumed to be a list of pseudo-legal moves.
     * This function removes the moves that don't defend from check threats.
     */
    public static final ArrayList<Move> removeIllegal(Position pos, ArrayList<Move> moveList) {
        ArrayList<Move> ret = new ArrayList<Move>();
        UndoInfo ui = new UndoInfo();
        int mlSize = moveList.size();
        for (int mi = 0; mi < mlSize; mi++) {
        	Move m = moveList.get(mi);
            pos.makeMove(m, ui);
            pos.setWhiteMove(!pos.whiteMove);
            if (!inCheck(pos))
                ret.add(m);
            pos.setWhiteMove(!pos.whiteMove);
            pos.unMakeMove(m, ui);
        }
        return ret;
    }

    /**
     * Add all moves from square (x0,y0) in direction (dx,dy).
     * @param multiMove If true, the piece can move more than one step.
     * @ return True if the enemy king could be captured, false otherwise.
     */
    private final boolean addDirection(ArrayList<Move> moveList, Position pos, int x0, int y0, int dx, int dy, boolean multiMove) {
        int sq0 = Position.getSquare(x0, y0);
        int x = x0 + dx;
        int y = y0 + dy;
        while ((x >= 0) && (x < 8) && (y >= 0) && (y < 8)) {
            int sq = Position.getSquare(x, y);
            int p = pos.getPiece(sq);
            if (p == Piece.EMPTY) {
                moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
            } else {
                if (Piece.isWhite(p) != pos.whiteMove) {
                    if (p == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                        returnMoveList(moveList);
                        moveList = getMoveListObj();
                        moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
                        return true;
                    } else {
                        moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
                    }
                }
                break;
            }
            if (!multiMove) {
                break;
            }
            x += dx;
            y += dy;
        }
        return false;
    }

    /**
     * Generate all possible pawn moves from (x0,y0) to (x1,y1), taking pawn promotions into account.
     */
    private final void addPawnMoves(ArrayList<Move> moveList, int x0, int y0, int x1, int y1) {
        int sq0 = Position.getSquare(x0, y0);
        int sq1 = Position.getSquare(x1, y1);
        if (y1 == 7) { // White promotion
            moveList.add(getMoveObj(sq0, sq1, Piece.WQUEEN));
            moveList.add(getMoveObj(sq0, sq1, Piece.WKNIGHT));
            moveList.add(getMoveObj(sq0, sq1, Piece.WROOK));
            moveList.add(getMoveObj(sq0, sq1, Piece.WBISHOP));
        } else if (y1 == 0) { // Black promotion
            moveList.add(getMoveObj(sq0, sq1, Piece.BQUEEN));
            moveList.add(getMoveObj(sq0, sq1, Piece.BKNIGHT));
            moveList.add(getMoveObj(sq0, sq1, Piece.BROOK));
            moveList.add(getMoveObj(sq0, sq1, Piece.BBISHOP));
        } else { // No promotion
            moveList.add(getMoveObj(sq0, sq1, Piece.EMPTY));
        }
    }

    /**
     * Check if there is an attacking piece in a given direction starting from (x0,y0).
     * The direction is given by (dx,dy). The pieces to check for are given by p0 and p1.
     * If multiMove is true, the piece is assumed to be a long-range piece, otherwise it
     * is assumed that the piece can only move one step.
     * @return True if there is an attacking piece in the given direction, false otherwise.
     */
    private static final boolean checkDirection(Position pos, int x0, int y0, int dx, int dy, int p0, int p1, boolean multiMove) {
        int x = x0 + dx;
        int y = y0 + dy;
        while ((x >= 0) && (x < 8) && (y >= 0) && (y < 8)) {
            final int sq = Position.getSquare(x, y);
            final int p = pos.getPiece(sq);
            if (p == p0)
                return true;
            if (multiMove) {
                if (p == p1) {
                    return true;
                } else if (p != Piece.EMPTY) {
                    break;
                }
            } else {
                break;
            }
            x += dx;
            y += dy;
        }
        return false;
    }

    static final int[] knightDx = { 2, 1, -1, -2, -2, -1,  1,  2 };
    static final int[] knightDy = { 1, 2,  2,  1, -1, -2, -2, -1 };

    /**
     * Count how many knights of a given color attack a square.
     * This function does not care if the knight is pinned or not.
     * @return Number of white/black knights attacking square.
     */
    static final int numKnightAttacks(Position pos, int square, boolean white) {
        int ret = 0;
        int x0 = Position.getX(square);
        int y0 = Position.getY(square);
        for (int d = 0; d < 8; d++) {
            int x = x0 + knightDx[d];
            int y = y0 + knightDy[d];
            if ((x >= 0) && (x < 8) && (y >= 0) && (y < 8)) {
                int p = pos.getPiece(Position.getSquare(x, y));
                if (p == (white ? Piece.WKNIGHT : Piece.BKNIGHT))
                    ret++;
            }
        }
        return ret;
    }
    

    // Code to handle the Move cache.

    private Move[] moveCache = new Move[2048];
    private int movesInCache = 0;
    private Object[] moveListCache = new Object[200];
    private int moveListsInCache = 0;
    
    private final Move getMoveObj(int from, int to, int promoteTo) {
        if (movesInCache > 0) {
            Move m = moveCache[--movesInCache];
            m.from = from;
            m.to = to;
            m.promoteTo = promoteTo;
            m.score = 0;
            return m;
        }
        return new Move(from, to, promoteTo);
    }

    @SuppressWarnings("unchecked")
    private final ArrayList<Move> getMoveListObj() {
        if (moveListsInCache > 0) {
            return (ArrayList<Move>)moveListCache[--moveListsInCache];
        }
        return new ArrayList<Move>(60);
    }

    /** Return all move objects in moveList to the move cache. */
    public final void returnMoveList(ArrayList<Move> moveList) {
        if (movesInCache + moveList.size() <= moveCache.length) {
        	int mlSize = moveList.size();
        	for (int mi = 0; mi < mlSize; mi++) {
                moveCache[movesInCache++] = moveList.get(mi);
            }
        }
        moveList.clear();
        if (moveListsInCache < moveListCache.length) {
            moveListCache[moveListsInCache++] = moveList;
        }
    }
    
    public final void returnMove(Move m) {
        if (movesInCache < moveCache.length) {
            moveCache[movesInCache++] = m;
        }
    }
}
