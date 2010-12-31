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
        long occupied = pos.whiteBB | pos.blackBB;
        if (pos.whiteMove) {
            // Queen moves
            long squares = pos.pieceTypeBB[Piece.WQUEEN];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = (BitBoard.rookAttacks(sq, occupied) | BitBoard.bishopAttacks(sq, occupied)) & ~pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Rook moves
            squares = pos.pieceTypeBB[Piece.WROOK];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.rookAttacks(sq, occupied) & ~pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Bishop moves
            squares = pos.pieceTypeBB[Piece.WBISHOP];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.bishopAttacks(sq, occupied) & ~pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

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
            if (addPawnMovesByMask(moveList, pos, m, -8, true)) return moveList;
            m = ((m & BitBoard.maskRow3) << 8) & ~(pos.whiteBB | pos.blackBB);
            addPawnDoubleMovesByMask(moveList, pos, m, -16);

            int epSquare = pos.getEpSquare();
            long epMask = (epSquare >= 0) ? (1L << epSquare) : 0L;
            m = (pawns << 7) & BitBoard.maskAToGFiles & (pos.blackBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, -7, true)) return moveList;

            m = (pawns << 9) & BitBoard.maskBToHFiles & (pos.blackBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, -9, true)) return moveList;
        } else {
            // Queen moves
            long squares = pos.pieceTypeBB[Piece.BQUEEN];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = (BitBoard.rookAttacks(sq, occupied) | BitBoard.bishopAttacks(sq, occupied)) & ~pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Rook moves
            squares = pos.pieceTypeBB[Piece.BROOK];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.rookAttacks(sq, occupied) & ~pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Bishop moves
            squares = pos.pieceTypeBB[Piece.BBISHOP];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.bishopAttacks(sq, occupied) & ~pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }
            
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
            if (addPawnMovesByMask(moveList, pos, m, 8, true)) return moveList;
            m = ((m & BitBoard.maskRow6) >>> 8) & ~(pos.whiteBB | pos.blackBB);
            addPawnDoubleMovesByMask(moveList, pos, m, 16);

            int epSquare = pos.getEpSquare();
            long epMask = (epSquare >= 0) ? (1L << epSquare) : 0L;
            m = (pawns >>> 9) & BitBoard.maskAToGFiles & (pos.whiteBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, 9, true)) return moveList;

            m = (pawns >>> 7) & BitBoard.maskBToHFiles & (pos.whiteBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, 7, true)) return moveList;
        }

        return moveList;
    }

    /** Generate captures, checks, and possibly some other moves that are too hard to filter out. */
    public final ArrayList<Move> pseudoLegalCapturesAndChecks(Position pos) {
        ArrayList<Move> moveList = getMoveListObj();
        long occupied = pos.whiteBB | pos.blackBB;
        if (pos.whiteMove) {
            int bKingSq = pos.getKingSq(false);
            long discovered = 0; // Squares that could generate discovered checks
            long kRookAtk = BitBoard.rookAttacks(bKingSq, occupied);
            if ((BitBoard.rookAttacks(bKingSq, occupied & ~kRookAtk) &
                    (pos.pieceTypeBB[Piece.WQUEEN] | pos.pieceTypeBB[Piece.WROOK])) != 0)
                discovered |= kRookAtk;
            long kBishAtk = BitBoard.bishopAttacks(bKingSq, occupied);
            if ((BitBoard.bishopAttacks(bKingSq, occupied & ~kBishAtk) &
                    (pos.pieceTypeBB[Piece.WQUEEN] | pos.pieceTypeBB[Piece.WBISHOP])) != 0)
                discovered |= kBishAtk;

            // Queen moves
            long squares = pos.pieceTypeBB[Piece.WQUEEN];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = (BitBoard.rookAttacks(sq, occupied) | BitBoard.bishopAttacks(sq, occupied));
                if ((discovered & (1L<<sq)) == 0) m &= (pos.blackBB | kRookAtk | kBishAtk);
                m &= ~pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Rook moves
            squares = pos.pieceTypeBB[Piece.WROOK];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.rookAttacks(sq, occupied);
                if ((discovered & (1L<<sq)) == 0) m &= (pos.blackBB | kRookAtk);
                m &= ~pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Bishop moves
            squares = pos.pieceTypeBB[Piece.WBISHOP];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.bishopAttacks(sq, occupied);
                if ((discovered & (1L<<sq)) == 0) m &= (pos.blackBB | kBishAtk);
                m &= ~pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // King moves
            {
                int sq = pos.getKingSq(true);
                long m = BitBoard.kingAttacks[sq];
                m &= ((discovered & (1L<<sq)) == 0) ? pos.blackBB : ~pos.whiteBB;
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
            long kKnightAtk = BitBoard.knightAttacks[bKingSq];
            while (knights != 0) {
                int sq = Long.numberOfTrailingZeros(knights);
                long m = BitBoard.knightAttacks[sq] & ~pos.whiteBB;
                if ((discovered & (1L<<sq)) == 0) m &= (pos.blackBB | kKnightAtk);
                m &= ~pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                knights &= knights-1;
            }

            // Pawn moves
            // Captures
            long pawns = pos.pieceTypeBB[Piece.WPAWN];
            int epSquare = pos.getEpSquare();
            long epMask = (epSquare >= 0) ? (1L << epSquare) : 0L;
            long m = (pawns << 7) & BitBoard.maskAToGFiles & (pos.blackBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, -7, false)) return moveList;
            m = (pawns << 9) & BitBoard.maskBToHFiles & (pos.blackBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, -9, false)) return moveList;

            // Discovered checks and promotions
            long pawnAll = discovered | BitBoard.maskRow7;
            m = ((pawns & pawnAll) << 8) & ~(pos.whiteBB | pos.blackBB);
            if (addPawnMovesByMask(moveList, pos, m, -8, false)) return moveList;
            m = ((m & BitBoard.maskRow3) << 8) & ~(pos.whiteBB | pos.blackBB);
            addPawnDoubleMovesByMask(moveList, pos, m, -16);

            // Normal checks
            m = ((pawns & ~pawnAll) << 8) & ~(pos.whiteBB | pos.blackBB);
            if (addPawnMovesByMask(moveList, pos, m & BitBoard.bPawnAttacks[bKingSq], -8, false)) return moveList;
            m = ((m & BitBoard.maskRow3) << 8) & ~(pos.whiteBB | pos.blackBB);
            addPawnDoubleMovesByMask(moveList, pos, m & BitBoard.bPawnAttacks[bKingSq], -16);
        } else {
            int wKingSq = pos.getKingSq(true);
            long discovered = 0; // Squares that could generate discovered checks
            long kRookAtk = BitBoard.rookAttacks(wKingSq, occupied);
            if ((BitBoard.rookAttacks(wKingSq, occupied & ~kRookAtk) &
                    (pos.pieceTypeBB[Piece.BQUEEN] | pos.pieceTypeBB[Piece.BROOK])) != 0)
                discovered |= kRookAtk;
            long kBishAtk = BitBoard.bishopAttacks(wKingSq, occupied);
            if ((BitBoard.bishopAttacks(wKingSq, occupied & ~kBishAtk) &
                    (pos.pieceTypeBB[Piece.BQUEEN] | pos.pieceTypeBB[Piece.BBISHOP])) != 0)
                discovered |= kBishAtk;

            // Queen moves
            long squares = pos.pieceTypeBB[Piece.BQUEEN];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = (BitBoard.rookAttacks(sq, occupied) | BitBoard.bishopAttacks(sq, occupied));
                if ((discovered & (1L<<sq)) == 0) m &= pos.whiteBB | kRookAtk | kBishAtk;
                m &= ~pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Rook moves
            squares = pos.pieceTypeBB[Piece.BROOK];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.rookAttacks(sq, occupied);
                if ((discovered & (1L<<sq)) == 0) m &= pos.whiteBB | kRookAtk;
                m &= ~pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Bishop moves
            squares = pos.pieceTypeBB[Piece.BBISHOP];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.bishopAttacks(sq, occupied);
                if ((discovered & (1L<<sq)) == 0) m &= pos.whiteBB | kBishAtk;
                m &= ~pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }
            
            // King moves
            {
                int sq = pos.getKingSq(false);
                long m = BitBoard.kingAttacks[sq];
                m &= ((discovered & (1L<<sq)) == 0) ? pos.whiteBB : ~pos.blackBB;
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
            long kKnightAtk = BitBoard.knightAttacks[wKingSq];
            while (knights != 0) {
                int sq = Long.numberOfTrailingZeros(knights);
                long m = BitBoard.knightAttacks[sq] & ~pos.blackBB;
                if ((discovered & (1L<<sq)) == 0) m &= pos.whiteBB | kKnightAtk;
                m &= ~pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                knights &= knights-1;
            }

            // Pawn moves
            // Captures
            long pawns = pos.pieceTypeBB[Piece.BPAWN];
            int epSquare = pos.getEpSquare();
            long epMask = (epSquare >= 0) ? (1L << epSquare) : 0L;
            long m = (pawns >>> 9) & BitBoard.maskAToGFiles & (pos.whiteBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, 9, false)) return moveList;
            m = (pawns >>> 7) & BitBoard.maskBToHFiles & (pos.whiteBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, 7, false)) return moveList;

            // Discovered checks and promotions
            long pawnAll = discovered | BitBoard.maskRow2;
            m = ((pawns & pawnAll) >>> 8) & ~(pos.whiteBB | pos.blackBB);
            if (addPawnMovesByMask(moveList, pos, m, 8, false)) return moveList;
            m = ((m & BitBoard.maskRow6) >>> 8) & ~(pos.whiteBB | pos.blackBB);
            addPawnDoubleMovesByMask(moveList, pos, m, 16);

            // Normal checks
            m = ((pawns & ~pawnAll) >>> 8) & ~(pos.whiteBB | pos.blackBB);
            if (addPawnMovesByMask(moveList, pos, m & BitBoard.wPawnAttacks[wKingSq], 8, false)) return moveList;
            m = ((m & BitBoard.maskRow6) >>> 8) & ~(pos.whiteBB | pos.blackBB);
            addPawnDoubleMovesByMask(moveList, pos, m & BitBoard.wPawnAttacks[wKingSq], 16);
        }

        return moveList;
    }

    public final ArrayList<Move> pseudoLegalCaptures(Position pos) {
        ArrayList<Move> moveList = getMoveListObj();
        long occupied = pos.whiteBB | pos.blackBB;
        if (pos.whiteMove) {
            // Queen moves
            long squares = pos.pieceTypeBB[Piece.WQUEEN];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = (BitBoard.rookAttacks(sq, occupied) | BitBoard.bishopAttacks(sq, occupied)) & pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Rook moves
            squares = pos.pieceTypeBB[Piece.WROOK];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.rookAttacks(sq, occupied) & pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Bishop moves
            squares = pos.pieceTypeBB[Piece.WBISHOP];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.bishopAttacks(sq, occupied) & pos.blackBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

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
            m = (pawns << 8) & ~(pos.whiteBB | pos.blackBB);
            m &= BitBoard.maskRow8;
            if (addPawnMovesByMask(moveList, pos, m, -8, false)) return moveList;

            int epSquare = pos.getEpSquare();
            long epMask = (epSquare >= 0) ? (1L << epSquare) : 0L;
            m = (pawns << 7) & BitBoard.maskAToGFiles & (pos.blackBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, -7, false)) return moveList;
            m = (pawns << 9) & BitBoard.maskBToHFiles & (pos.blackBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, -9, false)) return moveList;
        } else {
            // Queen moves
            long squares = pos.pieceTypeBB[Piece.BQUEEN];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = (BitBoard.rookAttacks(sq, occupied) | BitBoard.bishopAttacks(sq, occupied)) & pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Rook moves
            squares = pos.pieceTypeBB[Piece.BROOK];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.rookAttacks(sq, occupied) & pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }

            // Bishop moves
            squares = pos.pieceTypeBB[Piece.BBISHOP];
            while (squares != 0) {
                int sq = Long.numberOfTrailingZeros(squares);
                long m = BitBoard.bishopAttacks(sq, occupied) & pos.whiteBB;
                if (addMovesByMask(moveList, pos, sq, m)) return moveList;
                squares &= squares-1;
            }
            
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
            m = (pawns >>> 8) & ~(pos.whiteBB | pos.blackBB);
            m &= BitBoard.maskRow1;
            if (addPawnMovesByMask(moveList, pos, m, 8, false)) return moveList;

            int epSquare = pos.getEpSquare();
            long epMask = (epSquare >= 0) ? (1L << epSquare) : 0L;
            m = (pawns >>> 9) & BitBoard.maskAToGFiles & (pos.whiteBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, 9, false)) return moveList;
            m = (pawns >>> 7) & BitBoard.maskBToHFiles & (pos.whiteBB | epMask);
            if (addPawnMovesByMask(moveList, pos, m, 7, false)) return moveList;
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
    public static final boolean canTakeKing(Position pos) {
        pos.setWhiteMove(!pos.whiteMove);
        boolean ret = inCheck(pos);
        pos.setWhiteMove(!pos.whiteMove);
        return ret;
    }

    /**
     * Return true if a square is attacked by the opposite side.
     */
    public static final boolean sqAttacked(Position pos, int sq) {
        if (pos.whiteMove) {
            if ((BitBoard.knightAttacks[sq] & pos.pieceTypeBB[Piece.BKNIGHT]) != 0)
                return true;
            if ((BitBoard.kingAttacks[sq] & pos.pieceTypeBB[Piece.BKING]) != 0)
                return true;
            if ((BitBoard.wPawnAttacks[sq] & pos.pieceTypeBB[Piece.BPAWN]) != 0)
                return true;
            long occupied = pos.whiteBB | pos.blackBB;
            long bbQueen = pos.pieceTypeBB[Piece.BQUEEN];
            if ((BitBoard.bishopAttacks(sq, occupied) & (pos.pieceTypeBB[Piece.BBISHOP] | bbQueen)) != 0)
                return true;
            if ((BitBoard.rookAttacks(sq, occupied) & (pos.pieceTypeBB[Piece.BROOK] | bbQueen)) != 0)
                return true;
        } else {
            if ((BitBoard.knightAttacks[sq] & pos.pieceTypeBB[Piece.WKNIGHT]) != 0)
                return true;
            if ((BitBoard.kingAttacks[sq] & pos.pieceTypeBB[Piece.WKING]) != 0)
                return true;
            if ((BitBoard.bPawnAttacks[sq] & pos.pieceTypeBB[Piece.WPAWN]) != 0)
                return true;
            long occupied = pos.whiteBB | pos.blackBB;
            long bbQueen = pos.pieceTypeBB[Piece.WQUEEN];
            if ((BitBoard.bishopAttacks(sq, occupied) & (pos.pieceTypeBB[Piece.WBISHOP] | bbQueen)) != 0)
                return true;
            if ((BitBoard.rookAttacks(sq, occupied) & (pos.pieceTypeBB[Piece.WROOK] | bbQueen)) != 0)
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

    private final boolean addPawnMovesByMask(ArrayList<Move> moveList, Position pos, long mask,
                                             int delta, boolean allPromotions) {
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
                if (allPromotions) {
                    moveList.add(getMoveObj(sq0, sq, Piece.WROOK));
                    moveList.add(getMoveObj(sq0, sq, Piece.WBISHOP));
                }
            } else { // Black promotion
                moveList.add(getMoveObj(sq0, sq, Piece.BQUEEN));
                moveList.add(getMoveObj(sq0, sq, Piece.BKNIGHT));
                if (allPromotions) {
                    moveList.add(getMoveObj(sq0, sq, Piece.BROOK));
                    moveList.add(getMoveObj(sq0, sq, Piece.BBISHOP));
                }
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

    private final void addPawnDoubleMovesByMask(ArrayList<Move> moveList, Position pos,
                                                long mask, int delta) {
        while (mask != 0) {
            int sq = Long.numberOfTrailingZeros(mask);
            moveList.add(getMoveObj(sq + delta, sq, Piece.EMPTY));
            mask &= (mask - 1);
        }
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
