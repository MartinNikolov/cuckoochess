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
        final boolean wtm = pos.whiteMove;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
            	int sq = Position.getSquare(x, y);
                int p = pos.getPiece(sq);
                if ((p == Piece.EMPTY) || (Piece.isWhite(p) != wtm)) {
                    continue;
                }
                if ((p == Piece.WROOK) || (p == Piece.BROOK) || (p == Piece.WQUEEN) || (p == Piece.BQUEEN)) {
                    if (addDirection(moveList, pos, sq, 7-x,  1)) return moveList;
                    if (addDirection(moveList, pos, sq, 7-y,  8)) return moveList;
                    if (addDirection(moveList, pos, sq,   x, -1)) return moveList;
                    if (addDirection(moveList, pos, sq,   y, -8)) return moveList;
                }
                if ((p == Piece.WBISHOP) || (p == Piece.BBISHOP) || (p == Piece.WQUEEN) || (p == Piece.BQUEEN)) {
                    if (addDirection(moveList, pos, sq, Math.min(7-x, 7-y),  9)) return moveList;
                    if (addDirection(moveList, pos, sq, Math.min(  x, 7-y),  7)) return moveList;
                    if (addDirection(moveList, pos, sq, Math.min(  x,   y), -9)) return moveList;
                    if (addDirection(moveList, pos, sq, Math.min(7-x,   y), -7)) return moveList;
                }
                if ((p == Piece.WKNIGHT) || (p == Piece.BKNIGHT)) {
                	if (x < 6 && y < 7 && addDirection(moveList, pos, sq, 1,  10)) return moveList;
                    if (x < 7 && y < 6 && addDirection(moveList, pos, sq, 1,  17)) return moveList;
                    if (x > 0 && y < 6 && addDirection(moveList, pos, sq, 1,  15)) return moveList;
                    if (x > 1 && y < 7 && addDirection(moveList, pos, sq, 1,   6)) return moveList;
                    if (x > 1 && y > 0 && addDirection(moveList, pos, sq, 1, -10)) return moveList;
                    if (x > 0 && y > 1 && addDirection(moveList, pos, sq, 1, -17)) return moveList;
                    if (x < 7 && y > 1 && addDirection(moveList, pos, sq, 1, -15)) return moveList;
                    if (x < 6 && y > 0 && addDirection(moveList, pos, sq, 1,  -6)) return moveList;
                }
                if ((p == Piece.WKING) || (p == Piece.BKING)) {
                	if (x < 7          && addDirection(moveList, pos, sq, 1,  1)) return moveList;
                    if (x < 7 && y < 7 && addDirection(moveList, pos, sq, 1,  9)) return moveList;
                    if (         y < 7 && addDirection(moveList, pos, sq, 1,  8)) return moveList;
                    if (x > 0 && y < 7 && addDirection(moveList, pos, sq, 1,  7)) return moveList;
                    if (x > 0          && addDirection(moveList, pos, sq, 1, -1)) return moveList;
                    if (x > 0 && y > 0 && addDirection(moveList, pos, sq, 1, -9)) return moveList;
                    if (         y > 0 && addDirection(moveList, pos, sq, 1, -8)) return moveList;
                    if (x < 7 && y > 0 && addDirection(moveList, pos, sq, 1, -7)) return moveList;
		    
                    int k0 = wtm ? Position.getSquare(4,0) : Position.getSquare(4,7);
                    if (Position.getSquare(x,y) == k0) {
                        int aCastle = wtm ? Position.A1_CASTLE : Position.A8_CASTLE;
                        int hCastle = wtm ? Position.H1_CASTLE : Position.H8_CASTLE;
                        int rook = wtm ? Piece.WROOK : Piece.BROOK;
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
                    int yDir = wtm ? 8 : -8;
                    if (pos.getPiece(sq + yDir) == Piece.EMPTY) { // non-capture
                        addPawnMoves(moveList, sq, sq + yDir);
                        if ((y == (wtm ? 1 : 6)) &&
                                (pos.getPiece(sq + 2 * yDir) == Piece.EMPTY)) { // double step
                            addPawnMoves(moveList, sq, sq + yDir * 2);
                        }
                    }
                    if (x > 0) { // Capture to the left
                    	int toSq = sq + yDir - 1;
                        int cap = pos.getPiece(toSq);
                        if (cap != Piece.EMPTY) {
                            if (Piece.isWhite(cap) != wtm) {
                                if (cap == (wtm ? Piece.BKING : Piece.WKING)) {
                                    returnMoveList(moveList);
                                    moveList = getMoveListObj();
                                    moveList.add(getMoveObj(sq, toSq, Piece.EMPTY));
                                    return moveList;
                                } else {
                                    addPawnMoves(moveList, sq, toSq);
                                }
                            }
                        } else if (toSq == pos.getEpSquare()) {
                            addPawnMoves(moveList, sq, toSq);
                        }
                    }
                    if (x < 7) { // Capture to the right
                    	int toSq = sq + yDir + 1;
                        int cap = pos.getPiece(toSq);
                        if (cap != Piece.EMPTY) {
                            if (Piece.isWhite(cap) != wtm) {
                                if (cap == (wtm ? Piece.BKING : Piece.WKING)) {
                                    returnMoveList(moveList);
                                    moveList = getMoveListObj();
                                    moveList.add(getMoveObj(sq, toSq, Piece.EMPTY));
                                    return moveList;
                                } else {
                                    addPawnMoves(moveList, sq, toSq);
                                }
                            }
                        } else if (toSq == pos.getEpSquare()) {
                            addPawnMoves(moveList, sq, toSq);
                        }
                    }
                }
            }
        }
        return moveList;
    }
    
    public final ArrayList<Move> pseudoLegalCaptures(Position pos) {
    	// FIXME!!! Write test cases
        ArrayList<Move> moveList = getMoveListObj();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
            	int sq = Position.getSquare(x, y);
                int p = pos.getPiece(sq);
                if ((p == Piece.EMPTY) || (Piece.isWhite(p) != pos.whiteMove)) {
                    continue;
                }
                if ((p == Piece.WROOK) || (p == Piece.BROOK) || (p == Piece.WQUEEN) || (p == Piece.BQUEEN)) {
                    if (addCaptureDirection(moveList, pos, sq, 7-x,  1)) return moveList;
                    if (addCaptureDirection(moveList, pos, sq, 7-y,  8)) return moveList;
                    if (addCaptureDirection(moveList, pos, sq,   x, -1)) return moveList;
                    if (addCaptureDirection(moveList, pos, sq,   y, -8)) return moveList;
                }
                if ((p == Piece.WBISHOP) || (p == Piece.BBISHOP) || (p == Piece.WQUEEN) || (p == Piece.BQUEEN)) {
                    if (addCaptureDirection(moveList, pos, sq, Math.min(7-x, 7-y),  9)) return moveList;
                    if (addCaptureDirection(moveList, pos, sq, Math.min(  x, 7-y),  7)) return moveList;
                    if (addCaptureDirection(moveList, pos, sq, Math.min(  x,   y), -9)) return moveList;
                    if (addCaptureDirection(moveList, pos, sq, Math.min(7-x,   y), -7)) return moveList;
                }
                if ((p == Piece.WKNIGHT) || (p == Piece.BKNIGHT)) {
                	if (x < 6 && y < 7 && addCaptureDirection(moveList, pos, sq, 1,  10)) return moveList;
                    if (x < 7 && y < 6 && addCaptureDirection(moveList, pos, sq, 1,  17)) return moveList;
                    if (x > 0 && y < 6 && addCaptureDirection(moveList, pos, sq, 1,  15)) return moveList;
                    if (x > 1 && y < 7 && addCaptureDirection(moveList, pos, sq, 1,   6)) return moveList;
                    if (x > 1 && y > 0 && addCaptureDirection(moveList, pos, sq, 1, -10)) return moveList;
                    if (x > 0 && y > 1 && addCaptureDirection(moveList, pos, sq, 1, -17)) return moveList;
                    if (x < 7 && y > 1 && addCaptureDirection(moveList, pos, sq, 1, -15)) return moveList;
                    if (x < 6 && y > 0 && addCaptureDirection(moveList, pos, sq, 1,  -6)) return moveList;
                }
                if ((p == Piece.WKING) || (p == Piece.BKING)) {
                	if (x < 7          && addCaptureDirection(moveList, pos, sq, 1,  1)) return moveList;
                    if (x < 7 && y < 7 && addCaptureDirection(moveList, pos, sq, 1,  9)) return moveList;
                    if (         y < 7 && addCaptureDirection(moveList, pos, sq, 1,  8)) return moveList;
                    if (x > 0 && y < 7 && addCaptureDirection(moveList, pos, sq, 1,  7)) return moveList;
                    if (x > 0          && addCaptureDirection(moveList, pos, sq, 1, -1)) return moveList;
                    if (x > 0 && y > 0 && addCaptureDirection(moveList, pos, sq, 1, -9)) return moveList;
                    if (         y > 0 && addCaptureDirection(moveList, pos, sq, 1, -8)) return moveList;
                    if (x < 7 && y > 0 && addCaptureDirection(moveList, pos, sq, 1, -7)) return moveList;
                }
                if ((p == Piece.WPAWN) || (p == Piece.BPAWN)) {
                    int yDir = pos.whiteMove ? 8 : -8;
                    if ((sq + yDir < 8) || (sq + yDir >= 56)) {
                    	if (pos.getPiece(sq + yDir) == Piece.EMPTY) { // non-capture promotion
                    		addPawnMoves(moveList, sq, sq + yDir);
                    	}
                    }
                    if (x > 0) { // Capture to the left
                    	int toSq = sq + yDir - 1;
                        int cap = pos.getPiece(toSq);
                        if (cap != Piece.EMPTY) {
                            if (Piece.isWhite(cap) != pos.whiteMove) {
                                if (cap == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                                    returnMoveList(moveList);
                                    moveList = getMoveListObj();
                                    moveList.add(getMoveObj(sq, toSq, Piece.EMPTY));
                                    return moveList;
                                } else {
                                    addPawnMoves(moveList, sq, toSq);
                                }
                            }
                        } else if (toSq == pos.getEpSquare()) {
                            addPawnMoves(moveList, sq, toSq);
                        }
                    }
                    if (x < 7) { // Capture to the right
                    	int toSq = sq + yDir + 1;
                        int cap = pos.getPiece(toSq);
                        if (cap != Piece.EMPTY) {
                            if (Piece.isWhite(cap) != pos.whiteMove) {
                                if (cap == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                                    returnMoveList(moveList);
                                    moveList = getMoveListObj();
                                    moveList.add(getMoveObj(sq, toSq, Piece.EMPTY));
                                    return moveList;
                                } else {
                                    addPawnMoves(moveList, sq, toSq);
                                }
                            }
                        } else if (toSq == pos.getEpSquare()) {
                            addPawnMoves(moveList, sq, toSq);
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
     * If there is a piece type that can move from "from" to "to", return the
     * corresponding direction, 8*dy+dx.
     */
	private static final int getDirection(int from, int to) {
		int dx = Position.getX(to) - Position.getX(from);
		int dy = Position.getY(to) - Position.getY(from);
		if (dx == 0)					// Vertical rook direction
			return (dy > 0) ? 8 : -8;
		if (dy == 0)					// Horizontal rook direction
			return (dx > 0) ? 1 : -1;
		if (Math.abs(dx) == Math.abs(dy)) // Bishop direction
			return ((dy > 0) ? 8 : -8) + (dx > 0 ? 1 : -1);
		if (Math.abs(dx * dy) == 2)		// Knight direction
			return dy * 8 + dx;
		return 0;
	}

	/**
	 * Return the next piece in a given direction, starting from sq.
	 */
    private static final int nextPiece(Position pos, int sq, int delta) {
    	while (true) {
    		sq += delta;
    		int p = pos.getPiece(sq);
    		if (p != Piece.EMPTY)
    			return p;
    	}
    }
    
    /** Like nextPiece(), but handles board edges. */
    private static final int nextPieceSafe(Position pos, int sq, int delta) {
    	int dx = 0, dy = 0;
    	switch (delta) {
    	case 1: dx=1; dy=0; break;
    	case 9: dx=1; dy=1; break;
    	case 8: dx=0; dy=1; break;
    	case 7: dx=-1; dy=1; break;
    	case -1: dx=-1; dy=0; break;
    	case -9: dx=-1; dy=-1; break;
    	case -8: dx=0; dy=-1; break;
    	case -7: dx=1; dy=-1; break;
    	}
    	int x = Position.getX(sq);
    	int y = Position.getY(sq);
    	while (true) {
    		x += dx;
    		y += dy;
    		if ((x < 0) || (x > 7) || (y < 0) || (y > 7)) {
    			return Piece.EMPTY;
    		}
    		int p = pos.getPiece(Position.getSquare(x, y));
    		if (p != Piece.EMPTY)
    			return p;
    	}
    }
    
    /**
     * Return true if making a move delivers check to the opponent
     */
	public static final boolean givesCheck(Position pos, Move m) {
		boolean wtm = pos.whiteMove;
    	int oKingSq = pos.getKingSq(!wtm);
    	int oKing = wtm ? Piece.BKING : Piece.WKING;
		int p = Piece.makeWhite(m.promoteTo == Piece.EMPTY ? pos.getPiece(m.from) : m.promoteTo);
    	int d1 = MoveGen.getDirection(m.to, oKingSq);
    	switch (d1) {
    	case 8: case -8: case 1: case -1: // Rook direction
    		if ((p == Piece.WQUEEN) || (p == Piece.WROOK))
    			if ((d1 != 0) && (MoveGen.nextPiece(pos, m.to, d1) == oKing))
    				return true;
    		break;
    	case 9: case 7: case -9: case -7: // Bishop direction
    		if ((p == Piece.WQUEEN) || (p == Piece.WBISHOP)) {
    			if ((d1 != 0) && (MoveGen.nextPiece(pos, m.to, d1) == oKing))
    				return true;
    		} else if (p == Piece.WPAWN) {
    			if (((d1 > 0) == wtm) && (pos.getPiece(m.to + d1) == oKing))
    				return true;
    		}
    		break;
    	default:
    		if (d1 != 0) { // Knight direction
    			if (p == Piece.WKNIGHT)
    				return true;
    		}
    	}
    	int d2 = MoveGen.getDirection(m.from, oKingSq);
    	if ((d2 != 0) && (d2 != d1) && (MoveGen.nextPiece(pos, m.from, d2) == oKing)) {
    		int p2 = MoveGen.nextPieceSafe(pos, m.from, -d2);
    		switch (d2) {
        	case 8: case -8: case 1: case -1: // Rook direction
        		if ((p2 == (wtm ? Piece.WQUEEN : Piece.BQUEEN)) ||
        			(p2 == (wtm ? Piece.WROOK : Piece.BROOK)))
        			return true;
        		break;
        	case 9: case 7: case -9: case -7: // Bishop direction
        		if ((p2 == (wtm ? Piece.WQUEEN : Piece.BQUEEN)) ||
            		(p2 == (wtm ? Piece.WBISHOP : Piece.BBISHOP)))
        			return true;
        		break;
    		}
    	}
    	if ((m.promoteTo != Piece.EMPTY) && (d1 != 0) && (d1 == d2)) {
        	switch (d1) {
        	case 8: case -8: case 1: case -1: // Rook direction
        		if ((p == Piece.WQUEEN) || (p == Piece.WROOK))
        			if ((d1 != 0) && (MoveGen.nextPiece(pos, m.from, d1) == oKing))
        				return true;
        		break;
        	case 9: case 7: case -9: case -7: // Bishop direction
        		if ((p == Piece.WQUEEN) || (p == Piece.WBISHOP)) {
        			if ((d1 != 0) && (MoveGen.nextPiece(pos, m.from, d1) == oKing))
        				return true;
        		}
        		break;
        	}
    	}
    	if (p == Piece.WKING) {
    		if (m.to - m.from == 2) { // O-O
    			if (MoveGen.nextPieceSafe(pos, m.from, -1) == oKing)
    				return true;
    			if (MoveGen.nextPieceSafe(pos, m.from + 1, wtm ? 8 : -8) == oKing)
    				return true;
    		} else if (m.to - m.from == -2) { // O-O-O
    			if (MoveGen.nextPieceSafe(pos, m.from, 1) == oKing)
    				return true;
    			if (MoveGen.nextPieceSafe(pos, m.from - 1, wtm ? 8 : -8) == oKing)
    				return true;
    		}
    	} else if (p == Piece.WPAWN) {
    		if (pos.getPiece(m.to) == Piece.EMPTY) {
    			int dx = Position.getX(m.to) - Position.getX(m.from);
    			if (dx != 0) { // en passant
    				int epSq = m.from + dx;
    		    	int d3 = MoveGen.getDirection(epSq, oKingSq);
    		    	switch (d3) {
    		    	case 9: case 7: case -9: case -7:
    		        	if (MoveGen.nextPiece(pos, epSq, d3) == oKing) {
    		        		int p2 = MoveGen.nextPieceSafe(pos, epSq, -d3);
    		        		if ((p2 == (wtm ? Piece.WQUEEN : Piece.BQUEEN)) ||
    		                	(p2 == (wtm ? Piece.WBISHOP : Piece.BBISHOP)))
    		        			return true;
    		        	}
    		        	break;
					case 1:
						if (MoveGen.nextPiece(pos, Math.max(epSq, m.from), d3) == oKing) {
    		        		int p2 = MoveGen.nextPieceSafe(pos, Math.min(epSq, m.from), -d3);
    		        		if ((p2 == (wtm ? Piece.WQUEEN : Piece.BQUEEN)) ||
    		            		(p2 == (wtm ? Piece.WROOK : Piece.BROOK)))
    		            		return true;
						}
						break;
					case -1:
						if (MoveGen.nextPiece(pos, Math.min(epSq, m.from), d3) == oKing) {
    		        		int p2 = MoveGen.nextPieceSafe(pos, Math.max(epSq, m.from), -d3);
    		        		if ((p2 == (wtm ? Piece.WQUEEN : Piece.BQUEEN)) ||
    		            		(p2 == (wtm ? Piece.WROOK : Piece.BROOK)))
    		            		return true;
						}
						break;
    		    	}
    			}
    		}
    	}
    	return false;
    }
    
    /**
     * Return true if the side to move can take the opponents king.
     */
    public static final boolean canTakeKing(Position pos) { // FIXME!!! Optimize
        pos.setWhiteMove(!pos.whiteMove);
        boolean ret = inCheck(pos);
        pos.setWhiteMove(!pos.whiteMove);
        return ret;
    }

    /**
     * Return true if a square is attacked by the opposite side.
     */
    public static final boolean sqAttacked(Position pos, int sq) {
        int x = Position.getX(sq);
        int y = Position.getY(sq);
        boolean isWhiteMove = pos.whiteMove;

        final int oQueen= isWhiteMove ? Piece.BQUEEN: Piece.WQUEEN;
        final int oRook = isWhiteMove ? Piece.BROOK : Piece.WROOK;
        final int oBish = isWhiteMove ? Piece.BBISHOP : Piece.WBISHOP;
        final int oKnight = isWhiteMove ? Piece.BKNIGHT : Piece.WKNIGHT;

        int p;
        if (y > 0) {
            p = checkDirection(pos, sq,   y, -8); if ((p == oQueen) || (p == oRook)) return true;
            p = checkDirection(pos, sq, Math.min(  x,   y), -9); if ((p == oQueen) || (p == oBish)) return true;
            p = checkDirection(pos, sq, Math.min(7-x,   y), -7); if ((p == oQueen) || (p == oBish)) return true;
            if (x > 1         ) { p = checkDirection(pos, sq, 1, -10); if (p == oKnight) return true; }
            if (x > 0 && y > 1) { p = checkDirection(pos, sq, 1, -17); if (p == oKnight) return true; }
            if (x < 7 && y > 1) { p = checkDirection(pos, sq, 1, -15); if (p == oKnight) return true; }
            if (x < 6         ) { p = checkDirection(pos, sq, 1,  -6); if (p == oKnight) return true; }

            if (!isWhiteMove) {
            	if (x < 7 && y > 1) { p = checkDirection(pos, sq, 1, -7); if (p == Piece.WPAWN) return true; }
            	if (x > 0 && y > 1) { p = checkDirection(pos, sq, 1, -9); if (p == Piece.WPAWN) return true; }
            }
        }
        if (y < 7) {
            p = checkDirection(pos, sq, 7-y,  8); if ((p == oQueen) || (p == oRook)) return true;
            p = checkDirection(pos, sq, Math.min(7-x, 7-y),  9); if ((p == oQueen) || (p == oBish)) return true;
            p = checkDirection(pos, sq, Math.min(  x, 7-y),  7); if ((p == oQueen) || (p == oBish)) return true;
            if (x < 6         ) { p = checkDirection(pos, sq, 1,  10); if (p == oKnight) return true; }
            if (x < 7 && y < 6) { p = checkDirection(pos, sq, 1,  17); if (p == oKnight) return true; }
            if (x > 0 && y < 6) { p = checkDirection(pos, sq, 1,  15); if (p == oKnight) return true; }
            if (x > 1         ) { p = checkDirection(pos, sq, 1,   6); if (p == oKnight) return true; }
            if (isWhiteMove) {
            	if (x < 7 && y < 6) { p = checkDirection(pos, sq, 1, 9); if (p == Piece.BPAWN) return true; }
            	if (x > 0 && y < 6) { p = checkDirection(pos, sq, 1, 7); if (p == Piece.BPAWN) return true; }
            }
        }
        p = checkDirection(pos, sq, 7-x,  1); if ((p == oQueen) || (p == oRook)) return true;
        p = checkDirection(pos, sq,   x, -1); if ((p == oQueen) || (p == oRook)) return true;
        
        int oKingSq = pos.getKingSq(!isWhiteMove);
        if (oKingSq >= 0) {
            int ox = Position.getX(oKingSq);
            int oy = Position.getY(oKingSq);
            if ((Math.abs(x - ox) <= 1) && (Math.abs(y - oy) <= 1))
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
     * Add all moves from square sq0 in direction delta.
     * @param maxSteps Max steps until reaching a border. Set to 1 for non-sliding pieces.
     * @ return True if the enemy king could be captured, false otherwise.
     */
    private final boolean addDirection(ArrayList<Move> moveList, Position pos, int sq0, int maxSteps, int delta) {
    	int sq = sq0;
    	boolean wtm = pos.whiteMove;
    	final int oKing = (wtm ? Piece.BKING : Piece.WKING);
        while (maxSteps > 0) {
        	sq += delta;
            int p = pos.getPiece(sq);
            if (p == Piece.EMPTY) {
                moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
            } else {
                if (Piece.isWhite(p) != wtm) {
                    if (p == oKing) {
                        returnMoveList(moveList);
                        moveList = getMoveListObj(); // Ugly! this only works because we get back the same object
                        moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
                        return true;
                    } else {
                        moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
                    }
                }
                break;
            }
            maxSteps--;
        }
        return false;
    }

    private final boolean addCaptureDirection(ArrayList<Move> moveList, Position pos, int sq0, int maxSteps, int delta) {
    	int sq = sq0;
        while (maxSteps > 0) {
        	sq += delta;
            int p = pos.getPiece(sq);
            if (p != Piece.EMPTY) {
                if (Piece.isWhite(p) != pos.whiteMove) {
                    if (p == (pos.whiteMove ? Piece.BKING : Piece.WKING)) {
                        returnMoveList(moveList);
                        moveList = getMoveListObj(); // Ugly! this only works because we get back the same object
                        moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
                        return true;
                    } else {
                        moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
                    }
                }
                break;
            }
            maxSteps--;
        }
        return false;
    }

    /**
     * Generate all possible pawn moves from (x0,y0) to (x1,y1), taking pawn promotions into account.
     */
    private final void addPawnMoves(ArrayList<Move> moveList, int sq0, int sq1) {
            if (sq1 >= 56) { // White promotion
            moveList.add(getMoveObj(sq0, sq1, Piece.WQUEEN));
            moveList.add(getMoveObj(sq0, sq1, Piece.WKNIGHT));
            moveList.add(getMoveObj(sq0, sq1, Piece.WROOK));
            moveList.add(getMoveObj(sq0, sq1, Piece.WBISHOP));
        } else if (sq1 < 8) { // Black promotion
            moveList.add(getMoveObj(sq0, sq1, Piece.BQUEEN));
            moveList.add(getMoveObj(sq0, sq1, Piece.BKNIGHT));
            moveList.add(getMoveObj(sq0, sq1, Piece.BROOK));
            moveList.add(getMoveObj(sq0, sq1, Piece.BBISHOP));
        } else { // No promotion
            moveList.add(getMoveObj(sq0, sq1, Piece.EMPTY));
        }
    }

    /**
     * Check if there is an attacking piece in a given direction starting from sq.
     * The direction is given by delta.
     * @param maxSteps Max steps until reaching a border. Set to 1 for non-sliding pieces.
     * @return The first piece in the given direction, or EMPTY if there is no piece
     *         in that direction.
     */
    private static final int checkDirection(Position pos, int sq, int maxSteps, int delta) {
    	while (maxSteps > 0) {
    		sq += delta;
    		int p = pos.getPiece(sq);
    		if (p != Piece.EMPTY)
    			return p;
    		maxSteps--;
    	}
    	return Piece.EMPTY;
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
