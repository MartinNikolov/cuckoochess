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
        long squares;
        if (pos.whiteMove)
            squares = (pos.pieceTypeBB[Piece.WQUEEN ] | 
                       pos.pieceTypeBB[Piece.WROOK  ] | 
                       pos.pieceTypeBB[Piece.WBISHOP]);
        else
            squares = (pos.pieceTypeBB[Piece.BQUEEN ] | 
                       pos.pieceTypeBB[Piece.BROOK  ] | 
                       pos.pieceTypeBB[Piece.BBISHOP]);
        ArrayList<Move> moveList = getMoveListObj();
        while (squares != 0) {
            int sq = Long.numberOfTrailingZeros(squares);
            int x = Position.getX(sq);
            int y = Position.getY(sq);
            int p = pos.getPiece(sq);
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
            squares &= squares-1;
        }
        if (pos.whiteMove) {
            // King moves
            {
                int sq = pos.getKingSq(true);
                long m = BitBoard.kingAttacks[sq] & ~pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                final int k0 = 4;
                if (sq == k0) {
                    final long OO_SQ = 0x60L;
                    final long OOO_SQ = 0xEL;
                    if (((pos.getCastleMask() & (1 << Position.H1_CASTLE)) != 0) &&
                        ((OO_SQ & (pos.whiteBB | pos.blackBB)) == 0) &&
                        (pos.getPiece(k0 + 3) == Piece.WROOK) &&
                        !sqAttacked(pos, k0) &&
                        !sqAttacked(pos, k0 + 1)) {
                        moveList.add(getMoveObj(k0, k0 + 2, Piece.EMPTY));
                    }
                    if (((pos.getCastleMask() & (1 << Position.A1_CASTLE)) != 0) &&
                        ((OOO_SQ & (pos.whiteBB | pos.blackBB)) == 0) &&
                        (pos.getPiece(k0 - 4) == Piece.WROOK) &&
                        !sqAttacked(pos, k0) &&
                        !sqAttacked(pos, k0 - 1)) {
                        moveList.add(getMoveObj(k0, k0 - 2, Piece.EMPTY));
                    }
                }
            }

            // Knight moves
            long knights = pos.pieceTypeBB[Piece.WKNIGHT];
            while (knights != 0) {
                int sq = Long.numberOfTrailingZeros(knights);
                long m = BitBoard.knightAttacks[sq] & ~pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                knights &= knights-1;
            }

            // Pawn moves
            long pawns = pos.pieceTypeBB[Piece.WPAWN];
            long m = (pawns << 8) & ~(pos.whiteBB | pos.blackBB);
            if (addPawnMovesByMask(moveList, pos, m, -8)) return moveList;
            m = ((m & BitBoard.maskRow3) << 8) & ~(pos.whiteBB | pos.blackBB);
            if (addPawnMovesByMask(moveList, pos, m, -16)) return moveList;

            int epSquare = pos.getEpSquare();
            long epMask = (epSquare >= 0) ? (1L << epSquare) : 0L;
            m = (pawns << 7) & BitBoard.maskAToGFiles & (pos.blackBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, -7)) return moveList;

            m = (pawns << 9) & BitBoard.maskBToHFiles & (pos.blackBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, -9)) return moveList;
        } else {
            // King moves
            {
                int sq = pos.getKingSq(false);
                long m = BitBoard.kingAttacks[sq] & ~pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                final int k0 = 60;
                if (sq == k0) {
                    final long OO_SQ = 0x6000000000000000L;
                    final long OOO_SQ = 0xE00000000000000L;
                    if (((pos.getCastleMask() & (1 << Position.H8_CASTLE)) != 0) &&
                        ((OO_SQ & (pos.whiteBB | pos.blackBB)) == 0) &&
                        (pos.getPiece(k0 + 3) == Piece.BROOK) &&
                        !sqAttacked(pos, k0) &&
                        !sqAttacked(pos, k0 + 1)) {
                        moveList.add(getMoveObj(k0, k0 + 2, Piece.EMPTY));
                    }
                    if (((pos.getCastleMask() & (1 << Position.A8_CASTLE)) != 0) &&
                        ((OOO_SQ & (pos.whiteBB | pos.blackBB)) == 0) &&
                        (pos.getPiece(k0 - 4) == Piece.BROOK) &&
                        !sqAttacked(pos, k0) &&
                        !sqAttacked(pos, k0 - 1)) {
                        moveList.add(getMoveObj(k0, k0 - 2, Piece.EMPTY));
                    }
                }
            }

            // Knight moves
            long knights = pos.pieceTypeBB[Piece.BKNIGHT];
            while (knights != 0) {
                int sq = Long.numberOfTrailingZeros(knights);
                long m = BitBoard.knightAttacks[sq] & ~pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                knights &= knights-1;
            }

            // Pawn moves
            long pawns = pos.pieceTypeBB[Piece.BPAWN];
            long m = (pawns >>> 8) & ~(pos.whiteBB | pos.blackBB);
            if (addPawnMovesByMask(moveList, pos, m, 8)) return moveList;
            m = ((m & BitBoard.maskRow6) >>> 8) & ~(pos.whiteBB | pos.blackBB);
            if (addPawnMovesByMask(moveList, pos, m, 16)) return moveList;

            int epSquare = pos.getEpSquare();
            long epMask = (epSquare >= 0) ? (1L << epSquare) : 0L;
            m = (pawns >>> 9) & BitBoard.maskAToGFiles & (pos.whiteBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, 9)) return moveList;

            m = (pawns >>> 7) & BitBoard.maskBToHFiles & (pos.whiteBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, 7)) return moveList;
        }

        return moveList;
    }

    public final ArrayList<Move> pseudoLegalCaptures(Position pos) {
    	// FIXME!!! Write test cases
        long squares;
        if (pos.whiteMove)
            squares = (pos.pieceTypeBB[Piece.WQUEEN ] | 
                       pos.pieceTypeBB[Piece.WROOK  ] | 
                       pos.pieceTypeBB[Piece.WBISHOP]);
        else
            squares = (pos.pieceTypeBB[Piece.BQUEEN ] | 
                       pos.pieceTypeBB[Piece.BROOK  ] | 
                       pos.pieceTypeBB[Piece.BBISHOP]);
        ArrayList<Move> moveList = getMoveListObj();
        while (squares != 0) {
            int sq = Long.numberOfTrailingZeros(squares);
            int x = Position.getX(sq);
            int y = Position.getY(sq);
            int p = pos.getPiece(sq);
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
            squares &= squares-1;
        }
        if (pos.whiteMove) {
            // Knight moves
            long knights = pos.pieceTypeBB[Piece.WKNIGHT];
            while (knights != 0) {
                int sq = Long.numberOfTrailingZeros(knights);
                long m = BitBoard.knightAttacks[sq] & pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                knights &= knights-1;
            }

            // King moves
            int sq = pos.getKingSq(true);
            long m = BitBoard.kingAttacks[sq] & pos.blackBB;
            if (addMovesByMask(moveList, pos, sq, m)) return moveList;

            // Pawn moves
            long pawns = pos.pieceTypeBB[Piece.WPAWN];
            int epSquare = pos.getEpSquare();
            long epMask = (epSquare >= 0) ? (1L << epSquare) : 0L;
            m = (pawns << 7) & BitBoard.maskAToGFiles & (pos.blackBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, -7)) return moveList;
            m = (pawns << 9) & BitBoard.maskBToHFiles & (pos.blackBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, -9)) return moveList;
        } else {
            // Knight moves
            long knights = pos.pieceTypeBB[Piece.BKNIGHT];
            while (knights != 0) {
                int sq = Long.numberOfTrailingZeros(knights);
                long m = BitBoard.knightAttacks[sq] & pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                knights &= knights-1;
            }

            // King moves
            int sq = pos.getKingSq(false);
            long m = BitBoard.kingAttacks[sq] & pos.whiteBB;
            if (addMovesByMask(moveList, pos, sq, m)) return moveList;

            // Pawn moves
            long pawns = pos.pieceTypeBB[Piece.BPAWN];
            int epSquare = pos.getEpSquare();
            long epMask = (epSquare >= 0) ? (1L << epSquare) : 0L;
            m = (pawns >>> 9) & BitBoard.maskAToGFiles & (pos.whiteBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, 9)) return moveList;
            m = (pawns >>> 7) & BitBoard.maskBToHFiles & (pos.whiteBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, 7)) return moveList;
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

    private final boolean addPawnMovesByMask(ArrayList<Move> moveList, Position pos, long mask, int delta) {
        if (mask == 0)
            return false;
        long oKingMask = pos.pieceTypeBB[pos.whiteMove ? Piece.BKING : Piece.WKING];
        if ((mask & oKingMask) != 0) {
            int sq = Long.numberOfTrailingZeros(mask & oKingMask);
            returnMoveList(moveList);
            moveList = getMoveListObj(); // Ugly! this only works because we get back the same object
            moveList.add(getMoveObj(sq + delta, sq, Piece.EMPTY));
            return true;
        }
        long promMask = mask & BitBoard.maskRow1Row8;
        mask &= ~promMask;
        while (promMask != 0) {
            int sq = Long.numberOfTrailingZeros(promMask);
            int sq0 = sq + delta;
            if (sq >= 56) { // White promotion
                moveList.add(getMoveObj(sq0, sq, Piece.WQUEEN));
                moveList.add(getMoveObj(sq0, sq, Piece.WKNIGHT));
                moveList.add(getMoveObj(sq0, sq, Piece.WROOK));
                moveList.add(getMoveObj(sq0, sq, Piece.WBISHOP));
            } else { // Black promotion
                moveList.add(getMoveObj(sq0, sq, Piece.BQUEEN));
                moveList.add(getMoveObj(sq0, sq, Piece.BKNIGHT));
                moveList.add(getMoveObj(sq0, sq, Piece.BROOK));
                moveList.add(getMoveObj(sq0, sq, Piece.BBISHOP));
            }
            promMask &= (promMask - 1);
        }
        while (mask != 0) {
            int sq = Long.numberOfTrailingZeros(mask);
            moveList.add(getMoveObj(sq + delta, sq, Piece.EMPTY));
            mask &= (mask - 1);
        }
        return false;
    }
    
    
    private final boolean addMovesByMask(ArrayList<Move> moveList, Position pos, int sq0, long mask) {
        long oKingMask = pos.pieceTypeBB[pos.whiteMove ? Piece.BKING : Piece.WKING];
        if ((mask & oKingMask) != 0) {
            int sq = Long.numberOfTrailingZeros(mask & oKingMask);
            returnMoveList(moveList);
            moveList = getMoveListObj(); // Ugly! this only works because we get back the same object
            moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
            return true;
        }
        while (mask != 0) {
            int sq = Long.numberOfTrailingZeros(mask);
            moveList.add(getMoveObj(sq0, sq, Piece.EMPTY));
            mask &= (mask - 1);
        }
        return false;
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
