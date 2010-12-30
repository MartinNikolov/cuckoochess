/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import java.io.IOException;
import java.io.InputStream;

/**
 * Position evaluation routines.
 * 
 * @author petero
 */
public class Evaluate {
    static final int[] pieceValue;
    static {
        // Initialize material table
        pieceValue = new int[Piece.nPieceTypes];
        pieceValue[Piece.WKING  ] =  9900; // Used by SEE algorithm, but not included in board material sums
        pieceValue[Piece.WQUEEN ] =  1200;
        pieceValue[Piece.WROOK  ] =   600;
        pieceValue[Piece.WBISHOP] =   400;
        pieceValue[Piece.WKNIGHT] =   400;
        pieceValue[Piece.WPAWN  ] =   100;
        pieceValue[Piece.BKING  ] =  9900;
        pieceValue[Piece.BQUEEN ] =  1200;
        pieceValue[Piece.BROOK  ] =   600;
        pieceValue[Piece.BBISHOP] =   400;
        pieceValue[Piece.BKNIGHT] =   400;
        pieceValue[Piece.BPAWN  ] =   100;
        pieceValue[Piece.EMPTY  ] =     0;
    }
    
    /** Piece/square table for king during middle game. */
    static final int[] kt1 = { -22,-35,-40,-40,-40,-40,-35,-22,
  							   -22,-35,-40,-40,-40,-40,-35,-22,
    						   -25,-35,-40,-45,-45,-40,-35,-25,
    						   -15,-30,-35,-40,-40,-35,-30,-15,
    						   -10,-15,-20,-25,-25,-20,-15,-10,
    							 4, -2, -5,-15,-15, -5, -2,  4,
    							16, 14,  7, -3, -3,  7, 14, 16,
    							24, 24,  9,  0,  0,  9, 24, 24 };
    
    /** Piece/square table for king during end game. */
    static final int[] kt2 = {  0,  8, 16, 24, 24, 16,  8,  0,
    						    8, 16, 24, 32, 32, 24, 16,  8,
    						   16, 24, 32, 40, 40, 32, 24, 16,
    						   24, 32, 40, 48, 48, 40, 32, 24,
    						   24, 32, 40, 48, 48, 40, 32, 24,
    						   16, 24, 32, 40, 40, 32, 24, 16,
    						    8, 16, 24, 32, 32, 24, 16,  8,
    						    0,  8, 16, 24, 24, 16,  8,  0 };

    /** Piece/square table for pawns during middle game. */
    static final int[] pt1 = {   0,  0,  0,  0,  0,  0,  0,  0,
    							16, 32, 48, 64, 64, 48, 32, 16,
    							 6, 24, 40, 56, 56, 40, 24,  6,
    						   -10,  8, 20, 40, 40, 20,  8,-10,
    						   -12,  8, 10, 32, 32, 10,  8,-12,
    						   -12,  8,  4, 10, 10,  4,  8,-12,
    						   -12,  8,  8, -7, -7,  8,  8,-12,
    							 0,  0,  0,  0,  0,  0,  0,  0 };

    /** Piece/square table for pawns during end game. */
    static final int[] pt2 = {   0,  0,  0,  0,  0,  0,  0,  0,
    							50, 80, 90, 90, 90, 90, 80, 50,
    							34, 64, 69, 69, 69, 69, 64, 34,
    							10, 48, 48, 48, 48, 48, 48, 10,
    						   -18, 22, 22, 22, 22, 22, 22,-18,
    						   -34,  6,  6,  6,  6,  6,  6,-34,
    						   -40,  0,  0,  0,  0,  0,  0,-40,
    							 0,  0,  0,  0,  0,  0,  0,  0 };

    /** Piece/square table for knights during middle game. */
    static final int[] nt1 = { -50,-40,-30,-20,-20,-30,-40,-50,
    						   -40,-30,-10,  0,  0,-10,-30,-40,
    						   -20,  5, 10, 15, 15, 10,  5,-20,
    						   -17,  0, 10, 20, 20, 10,  0,-17,
    						   -17,  0,  3, 20, 20,  3,  0,-17,
    						   -20,-10,  0,  5,  5,  0,-10,-20,
    						   -40,-30,-10,  0,  0,-10,-30,-40,
    						   -50,-40,-30,-20,-20,-30,-40,-50 };

    /** Piece/square table for knights during end game. */
    static final int[] nt2 = { -50,-40,-30,-20,-20,-30,-40,-50,
    						   -40,-30,-10, -5, -5,-10,-30,-40,
    						   -30,-10,  0, 10, 10,  0,-10,-30,
    						   -20, -5, 10, 20, 20, 10, -5,-20,
    						   -20, -5, 10, 20, 20, 10, -5,-20,
    						   -30,-10,  0, 10, 10,  0,-10,-30,
    						   -40,-30,-10, -5, -5,-10,-30,-40,
    						   -50,-40,-30,-20,-20,-30,-40,-50 };

    /** Piece/square table for bishops during middle game. */
    static final int[] bt1 = {  0,  0,  0,  0,  0,  0,  0,  0,
    							0,  8,  4,  4,  4,  4,  8,  0,
    							0,  4,  8,  8,  8,  8,  4,  0,
    							0,  4,  8,  8,  8,  8,  4,  0,
    							0,  4,  8,  8,  8,  8,  4,  0,
    							0,  6,  8,  8,  8,  8,  6,  0,
    							0,  8,  4,  4,  4,  4,  8,  0,
    							0,  0,  0,  0,  0,  0,  0,  0 };

    /** Piece/square table for queens during middle game. */
    static final int[] qt1 = { -10, -5,  0,  0,  0,  0, -5,-10,
				 				-5,  0,  5,  5,  5,  5,  0, -5,
				 				 0,  5,  5,  6,  6,  5,  5,  0,
				 				 0,  5,  6,  6,  6,  6,  5,  0,
				 				 0,  5,  6,  6,  6,  6,  5,  0,
				 				 0,  5,  5,  6,  6,  5,  5,  0,
				 				-5,  0,  5,  5,  5,  5,  0, -5,
				 			   -10, -5,  0,  0,  0,  0, -5,-10 };

    /** Piece/square table for rooks during middle game. */
    static final int[] rt1 = {  0,  3,  5,  5,  5,  5,  3,  0,
				 			   15, 20, 20, 20, 20, 20, 20, 15,
				 			    0,  0,  0,  0,  0,  0,  0,  0,
				 			    0,  0,  0,  0,  0,  0,  0,  0,
				 			   -2,  0,  0,  0,  0,  0,  0, -2,
				 			   -2,  0,  0,  2,  2,  0,  0, -2,
				 			   -3,  2,  5,  5,  5,  5,  2, -3,
				 			    0,  3,  5,  5,  5,  5,  3,  0 };

    static final int[][] distToH1A8 = { { 0, 1, 2, 3, 4, 5, 6, 7 },
										{ 1, 2, 3, 4, 5, 6, 7, 6 },
										{ 2, 3, 4, 5, 6, 7, 6, 5 },
										{ 3, 4, 5, 6, 7, 6, 5, 4 },
										{ 4, 5, 6, 7, 6, 5, 4, 3 },
										{ 5, 6, 7, 6, 5, 4, 3, 2 },
										{ 6, 7, 6, 5, 4, 3, 2, 1 },
										{ 7, 6, 5, 4, 3, 2, 1, 0 } };

    static final int[] rookMobScore = {-10,-7,-4,-1,2,5,7,9,11,12,13,14,14,14,14};
    static final int[] bishMobScore = {-15,-10,-6,-2,2,6,10,13,16,18,20,22,23,24};
    static final int[] queenMobScore = {-10,-8,-6,-4,-2,0,2,4,6,8,10,12,14,16,18,19,20,20,20,20,20,20,20,20,20,20,20,20};

    private static final class PawnHashData {
    	PawnHashData() {
            nPawns = new byte[2][8];
            passedPawns = new byte[2][8];
    	}
    	long key;
    	byte [][] nPawns;  // nPawns[0/1][file] contains the number of white/black pawns on a file.
    	int score;         // Positive score means good for white
    	int passedBonusW;
    	int passedBonusB;
    	byte [][] passedPawns; // passedPawns[0/1][file] contains the row of the most advanced
    	                       // passed pawn on a file, or 0 if there is no free pawn on that file.
    }
    PawnHashData ph;
    static PawnHashData[] pawnHash;
    static {
        final int numEntries = 1<<12;
    	pawnHash = new PawnHashData[numEntries];
        for (int i = 0; i < numEntries; i++) {
            PawnHashData phd = new PawnHashData();
            phd.key = -1; // Non-zero to avoid collision for positions with no pawns
            phd.score = 0;
            pawnHash[i] = phd;
        }
    }
    
    int [][] pieces;
    int [] nPieces;
    static byte[] kpkTable = null;

    // King safety variables
    private long wKingZone, bKingZone;       // Squares close to king that are worth attacking
    private int wKingAttacks, bKingAttacks; // Number of attacks close to white/black king
    
    /** Constructor. */
    public Evaluate() {
        firstPawn = new int[2][8];
        pieces = new int[Piece.nPieceTypes][64];
        nPieces = new int[Piece.nPieceTypes];
        if (kpkTable == null) {
            kpkTable = new byte[2*32*64*48/8];
            InputStream inStream = getClass().getResourceAsStream("/kpk.bitbase");
            try {
                int len = inStream.read(kpkTable);
                if (len != kpkTable.length)
                    throw new RuntimeException();
                inStream.close();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
    }

    /**
     * Static evaluation of a position.
     * @param pos The position to evaluate.
     * @return The evaluation score, measured in centipawns.
     *         Positive values are good for the side to make the next move.
     */
    final public int evalPos(Position pos) {
    	int score = pos.wMtrl - pos.bMtrl;

        wKingAttacks = bKingAttacks = 0;
        wKingZone = BitBoard.kingAttacks[pos.getKingSq(true)]; wKingZone |= wKingZone << 8;
        bKingZone = BitBoard.kingAttacks[pos.getKingSq(false)]; bKingZone |= bKingZone >>> 8;

        score += pieceSquareEval(pos);
        score += pawnBonus(pos);
        score += tradeBonus(pos);
        score += castleBonus(pos);

        score += rookBonus(pos);
        score += bishopEval(pos, score);
        score += kingSafety(pos);
        score = endGameEval(pos, score);

        if (!pos.whiteMove)
            score = -score;
        return score;
    }

    /** Compute white_material - black_material. */
    static final int material(Position pos) {
        return pos.wMtrl - pos.bMtrl;
    }
    
    /** Compute score based on piece square tables. Positive values are good for white. */
    private final int pieceSquareEval(Position pos) {
        int score = 0;
        final int qV = pieceValue[Piece.WQUEEN];
        final int rV = pieceValue[Piece.WROOK];
        final int bV = pieceValue[Piece.WBISHOP];
        final int nV = pieceValue[Piece.WKNIGHT];
        final int pV = pieceValue[Piece.WPAWN];
        final int wMtrl = pos.wMtrl;
        final int bMtrl = pos.bMtrl;
        final int wMtrlPawns = pos.wMtrlPawns;
        final int bMtrlPawns = pos.bMtrlPawns;

        for (int p = 0; p < Piece.nPieceTypes; p++)
        	nPieces[p] = 0;

        // Kings
        {
            int sq = pos.getKingSq(true);
            final int p = Piece.WKING;
            pieces[p][nPieces[p]++] = sq;
            final int k1 = kt1[63-sq];
            final int k2 = kt2[63-sq];
            final int t1 = qV + 2 * rV + 2 * bV;
            final int t2 = rV;
            final int t = bMtrl - bMtrlPawns;
            final int s = interpolate(t, t2, k2, t1, k1);
            score += s;
        }
        {
            int sq = pos.getKingSq(false);
            final int p = Piece.BKING;
            pieces[p][nPieces[p]++] = sq;
            final int k1 = kt1[sq];
            final int k2 = kt2[sq];
            final int t1 = qV + 2 * rV + 2 * bV;
            final int t2 = rV;
            final int t = wMtrl - wMtrlPawns;
            final int s = interpolate(t, t2, k2, t1, k1);
            score -= s;
        }

        // Pawns
        {
            final int t1 = qV + 2 * rV + 2 * bV;
            final int t2 = rV;
            int p = Piece.WPAWN;
            long m = pos.pieceTypeBB[p];
            if (m != 0) {
                int wp1 = 0, wp2 = 0;
                do {
                    int sq = Long.numberOfTrailingZeros(m);
                    pieces[p][nPieces[p]++] = sq;
                    wp1 += pt1[63-sq];
                    wp2 += pt2[63-sq];
                    m &= m-1;
                } while (m != 0);
                final int tw = bMtrl - bMtrlPawns;
                score += interpolate(tw, t2, wp2, t1, wp1) / 2;
            }
            p = Piece.BPAWN;
            m = pos.pieceTypeBB[p];
            if (m != 0) {
                int bp1 = 0, bp2 = 0;
                do {
                    int sq = Long.numberOfTrailingZeros(m);
                    pieces[p][nPieces[p]++] = sq;
                    bp1 += pt1[sq];
                    bp2 += pt2[sq];
                    m &= m-1;
                } while (m != 0);
                final int tb = wMtrl - wMtrlPawns;
                score -= interpolate(tb, t2, bp2, t1, bp1) / 2;
            }
        }

        // Knights
        {
            int p = Piece.WKNIGHT;
            long m = pos.pieceTypeBB[p];
            final int t1 = qV + 2 * rV + 1 * bV + 1 * nV + 6 * pV;
            final int t2 = nV + 8 * pV;
            if (m != 0) {
                int n1 = 0, n2 = 0;
                do {
                    int sq = Long.numberOfTrailingZeros(m);
                    pieces[p][nPieces[p]++] = sq;
                    n1 += nt1[63-sq];
                    n2 += nt2[63-sq];
                    m &= m-1;
                } while (m != 0);
                score += interpolate(bMtrl, t2, n2, t1, n1);
            }
            p = Piece.BKNIGHT;
            m = pos.pieceTypeBB[p];
            if (m != 0) {
                int n1 = 0, n2 = 0;
                do {
                    int sq = Long.numberOfTrailingZeros(m);
                    pieces[p][nPieces[p]++] = sq;
                    n1 += nt1[sq];
                    n2 += nt2[sq];
                    m &= m-1;
                } while (m != 0);
                score -= interpolate(wMtrl, t2, n2, t1, n1);
            }
        }

        // Bishops
        {
            int p = Piece.WBISHOP;
            long m = pos.pieceTypeBB[p];
            while (m != 0) {
                int sq = Long.numberOfTrailingZeros(m);
                pieces[p][nPieces[p]++] = sq;
                score += bt1[63-sq];
                m &= m-1;
            }
            p = Piece.BBISHOP;
            m = pos.pieceTypeBB[p];
            while (m != 0) {
                int sq = Long.numberOfTrailingZeros(m);
                pieces[p][nPieces[p]++] = sq;
                score -= bt1[sq];
                m &= m-1;
            }
        }

        // Queens
        {
            int p = Piece.WQUEEN;
            long m = pos.pieceTypeBB[p];
            while (m != 0) {
                int sq = Long.numberOfTrailingZeros(m);
                pieces[p][nPieces[p]++] = sq;
                score += qt1[63-sq];
                mobilityAttacks = 0L;
                score += queenMobScore[rookMobility(pos, sq) + bishopMobility(pos, sq)];
                bKingAttacks += Long.bitCount(mobilityAttacks & bKingZone) * 2;
                m &= m-1;
            }
            p = Piece.BQUEEN;
            m = pos.pieceTypeBB[p];
            while (m != 0) {
                int sq = Long.numberOfTrailingZeros(m);
                pieces[p][nPieces[p]++] = sq;
                score -= qt1[sq];
                mobilityAttacks = 0L;
                score -= queenMobScore[rookMobility(pos, sq) + bishopMobility(pos, sq)];
                wKingAttacks += Long.bitCount(mobilityAttacks & wKingZone) * 2;
                m &= m-1;
            }
        }

        // Rooks
        {
            int p = Piece.WROOK;
            long m = pos.pieceTypeBB[p];
            if (m != 0) {
                int r1 = 0;
                do {
                    int sq = Long.numberOfTrailingZeros(m);
                    pieces[p][nPieces[p]++] = sq;
                    r1 += rt1[63-sq];
                    m &= m-1;
                } while (m != 0);
                final int nP = bMtrlPawns / pV;
                final int s = r1 * Math.min(nP, 6) / 6;
                score += s;
            }
        }
        {
            int p = Piece.BROOK;
            long m = pos.pieceTypeBB[p];
            if (m != 0) {
                int r1 = 0;
                do {
                    int sq = Long.numberOfTrailingZeros(m);
                    pieces[p][nPieces[p]++] = sq;
                    r1 += rt1[sq];
                    m &= m-1;
                } while (m != 0);
                final int nP = wMtrlPawns / pV;
                final int s = r1 * Math.min(nP, 6) / 6;
                score -= s;
            }
        }

        return score;
    }

    /** Implement the "when ahead trade pieces, when behind trade pawns" rule. */
    private final int tradeBonus(Position pos) {
        final int pV = pieceValue[Piece.WPAWN];
        final int qV = pieceValue[Piece.WQUEEN];
        final int rV = pieceValue[Piece.WROOK];
        final int bV = pieceValue[Piece.WBISHOP];
        final int nV = pieceValue[Piece.WKNIGHT];

        final int wM = pos.wMtrl;
        final int bM = pos.bMtrl;
        final int wPawn = pos.wMtrlPawns;
        final int bPawn = pos.bMtrlPawns;
        final int deltaScore = wM - bM;

        int pBonus = 0;
        pBonus += interpolate((deltaScore > 0) ? wPawn : bPawn, 0, -30 * deltaScore / 100, 6 * pV, 0);
        pBonus += interpolate((deltaScore > 0) ? bM : wM, 0, 30 * deltaScore / 100, qV + 2 * rV + 2 * bV + 2 * nV, 0);

        return pBonus;
    }

    /** Score castling ability. */
    private final int castleBonus(Position pos) {
    	if (pos.getCastleMask() == 0) return 0;
        final int qV = pieceValue[Piece.WQUEEN];
        final int rV = pieceValue[Piece.WROOK];
        final int bV = pieceValue[Piece.WBISHOP];

        final int k1 = kt1[7*8+6] - kt1[7*8+4];
        final int k2 = kt2[7*8+6] - kt2[7*8+4];
        final int t1 = qV + 2 * rV + 2 * bV;
        final int t2 = rV;
        final int t = pos.bMtrl - pos.bMtrlPawns;
        final int ks = interpolate(t, t2, k2, t1, k1);

        final int castleValue = ks + rt1[7*8+5] - rt1[7*8+7];
        if (castleValue <= 0) {
            return 0;
        }
        int h1Dist = 100;
        if (pos.h1Castle()) {
            h1Dist = 2;
            if (pos.getPiece(5) != Piece.EMPTY) h1Dist++;
            if (pos.getPiece(6) != Piece.EMPTY) h1Dist++;
        }
        int a1Dist = 100;
        if (pos.a1Castle()) {
            a1Dist = 2;
            if (pos.getPiece(3) != Piece.EMPTY) a1Dist++;
            if (pos.getPiece(2) != Piece.EMPTY) a1Dist++;
            if (pos.getPiece(1) != Piece.EMPTY) a1Dist++;
        }
        final int wBonus = castleValue / Math.min(a1Dist, h1Dist);

        int h8Dist = 100;
        if (pos.h8Castle()) {
            h8Dist = 2;
            if (pos.getPiece(61) != Piece.EMPTY) h8Dist++;
            if (pos.getPiece(62) != Piece.EMPTY) h8Dist++;
        }
        int a8Dist = 100;
        if (pos.a8Castle()) {
            a8Dist = 2;
            if (pos.getPiece(59) != Piece.EMPTY) a8Dist++;
            if (pos.getPiece(58) != Piece.EMPTY) a8Dist++;
            if (pos.getPiece(57) != Piece.EMPTY) a8Dist++;
        }
        final int bBonus = castleValue / Math.min(a8Dist, h8Dist);

        return wBonus - bBonus;
    }

    private final int pawnBonus(Position pos) {
    	long key = pos.pawnZobristHash();
    	PawnHashData phd = pawnHash[(int)key & (pawnHash.length - 1)];
    	if (phd.key != key)
    		computePawnHashData(pos, phd);
    	int score = phd.score;

    	final int qV = pieceValue[Piece.WQUEEN];
        final int rV = pieceValue[Piece.WROOK];
        final int hiMtrl = qV + rV;
        score += interpolate(pos.bMtrl - pos.bMtrlPawns, 0, 2 * phd.passedBonusW, hiMtrl, phd.passedBonusW);
        score -= interpolate(pos.wMtrl - pos.wMtrlPawns, 0, 2 * phd.passedBonusB, hiMtrl, phd.passedBonusB);
    	
        ph = phd;
    	return score;
    }

    /**
	 * firstPawn[0/1][file] contains the rank of the first (least advanced) pawn for a color/file.
	 * If there is no pawn, the value is set to 7/0, ie the promotion row for pawns of that color.
	 */
	private int [][] firstPawn;

    /** Compute nPawns[][] corresponding to pos. */
	private final void computePawnHashData(Position pos, PawnHashData ph) {
    	byte[][] nPawns = ph.nPawns;
    	byte[][] passedPawns = ph.passedPawns;
        for (int x = 0; x < 8; x++) {
            nPawns[0][x] = 0;
            nPawns[1][x] = 0;
            firstPawn[0][x] = 7;
            firstPawn[1][x] = 0;
        }

        int np = nPieces[Piece.WPAWN];
        int[] pawns = pieces[Piece.WPAWN];
        for (int i = 0; i < np; i++) {
            int sq = pawns[i];
            int x = Position.getX(sq);
            int y = Position.getY(sq);
            nPawns[0][x]++;
            firstPawn[0][x] = Math.min(firstPawn[0][x], y);
        }
        np = nPieces[Piece.BPAWN];
        pawns = pieces[Piece.BPAWN];
        for (int i = 0; i < np; i++) {
            int sq = pawns[i];
            int x = Position.getX(sq);
            int y = Position.getY(sq);
            nPawns[1][x]++;
            firstPawn[1][x] = Math.max(firstPawn[1][x], y);
        }

    	int score = 0;

        // Evaluate double pawns and pawn islands
        int wDouble = 0;
        int bDouble = 0;
        int wIslands = 0;
        int bIslands = 0;
        boolean wasPawn = false;
        for (int x = 0; x < 8; x++) {
            if (nPawns[0][x] > 0) {
                wDouble += nPawns[0][x] - 1;
                if (!wasPawn) {
                    wIslands++;
                    wasPawn = true;
                }
            } else {
                wasPawn = false;
            }
        }
        wasPawn = false;
        for (int x = 0; x < 8; x++) {
            if (nPawns[1][x] > 0) {
                bDouble += nPawns[1][x] - 1;
                if (!wasPawn) {
                    bIslands++;
                    wasPawn = true;
                }
            } else {
                wasPawn = false;
            }
        }
        score -= (wDouble - bDouble) * 20;
        score -= (wIslands - bIslands) * 15;
        
        // Evaluate passed pawn bonus
        int passedBonusW = 0;
        int passedBonusB = 0;
        for (int x = 0; x < 8; x++) {
            passedPawns[0][x] = 0;
        	if (nPawns[0][x] > 0) {
        		int y = 6;
        		while (pos.getPiece(Position.getSquare(x, y)) != Piece.WPAWN)
        			y--;
        		boolean passed = true;
        		if ((x > 0) && (firstPawn[1][x - 1] >= y + 1)) passed = false;
        		if (           (firstPawn[1][x + 0] >= y + 1)) passed = false;
        		if ((x < 7) && (firstPawn[1][x + 1] >= y + 1)) passed = false;
        		if (passed) {
        			passedBonusW += 20 + y * 4;
        			if ((x > 0) && (pos.getPiece(Position.getSquare(x - 1, y - 1)) == Piece.WPAWN) ||
        				(x < 7) && (pos.getPiece(Position.getSquare(x + 1, y - 1)) == Piece.WPAWN)) {
        				passedBonusW += 15;  // Guarded passed pawn
        			}
                    passedPawns[0][x] = (byte)y;
        		}
        	}
            passedPawns[1][x] = 0;
        	if (nPawns[1][x] > 0) {
        		int y = 1;
        		while (pos.getPiece(Position.getSquare(x, y)) != Piece.BPAWN)
        			y++;
        		boolean passed = true;
        		if ((x > 0) && (firstPawn[0][x - 1] <= y - 1)) passed = false;
        		if (           (firstPawn[0][x + 0] <= y - 1)) passed = false;
        		if ((x < 7) && (firstPawn[0][x + 1] <= y - 1)) passed = false;
        		if (passed) {
        			passedBonusB += 20 + (7-y) * 4;
        			if ((x > 0) && (pos.getPiece(Position.getSquare(x - 1, y + 1)) == Piece.BPAWN) ||
        				(x < 7) && (pos.getPiece(Position.getSquare(x + 1, y + 1)) == Piece.BPAWN)) {
        				passedBonusB += 15;  // Guarded passed pawn
        			}
                    passedPawns[1][x] = (byte)y;
        		}
            }
        }

        ph.key = pos.pawnZobristHash();
        ph.score = score;
        ph.passedBonusW = passedBonusW;
        ph.passedBonusB = passedBonusB;
    }

    /** Compute rook bonus. Rook on open/half-open file. */
	private final int rookBonus(Position pos) {
        int score = 0;
        int nP = nPieces[Piece.WROOK];
        for (int i = 0; i < nP; i++) {
        	int sq = pieces[Piece.WROOK][i];
            final int x = Position.getX(sq);
            if (ph.nPawns[0][x] == 0) { // At least half-open file
                score += ph.nPawns[1][x] == 0 ? 25 : 12;
            }
            mobilityAttacks = 0L;
            score += rookMobScore[rookMobility(pos, sq)];
            bKingAttacks += Long.bitCount(mobilityAttacks & bKingZone);
        }
        nP = nPieces[Piece.BROOK];
        for (int i = 0; i < nP; i++) {
        	int sq = pieces[Piece.BROOK][i];
            final int x = Position.getX(sq);
            if (ph.nPawns[1][x] == 0) {
                score -= ph.nPawns[0][x] == 0 ? 25 : 12;
            }
            mobilityAttacks = 0L;
            score -= rookMobScore[rookMobility(pos, sq)];
            wKingAttacks += Long.bitCount(mobilityAttacks & wKingZone);
        }
        return score;
    }

    /** Compute king safety for both kings. */
    private final int kingSafety(Position pos) {
        final int qV = pieceValue[Piece.WQUEEN];
        final int rV = pieceValue[Piece.WROOK];
        final int bV = pieceValue[Piece.WBISHOP];
        final int nV = pieceValue[Piece.WKNIGHT];
        final int maxM = qV + 2 * rV + 2 * bV + 2 * nV;
        final int minM = rV + bV;
        final int m = (pos.wMtrl - pos.wMtrlPawns + pos.bMtrl - pos.bMtrlPawns) / 2;
        if (m <= minM)
            return 0;
        int score = 0;
        for (int i = 0; i < 2; i++) {
            boolean white = (i == 0);
            int kSq = pos.getKingSq(white);
            int xk = Position.getX(kSq);
            int yk = Position.getY(kSq);
            int safety = 0;
            int halfOpenFiles = 0;
            if (white ? (yk < 2) : (yk >= 6)) {
                int yb = white ? 0 : 56;    // king home rank
                int yd = white ? 8 : -8;    // pawn direction
                int ownPawn = white ? Piece.WPAWN : Piece.BPAWN;
                int otherPawn = white ? Piece.BPAWN : Piece.WPAWN;
                final int xa = Math.max(xk - 1, 0);
                final int xb = Math.min(xk + 1, 7);
                for (int x = xa; x <= xb; x++) {
                	int p = pos.getPiece(yb + x + yd);
                	if (p == ownPawn) safety += 3; else if (p == otherPawn) safety -= 2;
                	p = pos.getPiece(yb + x + 2 * yd);
                	if (p == ownPawn) safety += 2; else if (p == otherPawn) safety -= 2;
                	p = pos.getPiece(yb + x + 3 * yd);
                	if (p == otherPawn) safety -= 1;
                	if (ph.nPawns[1-i][x] == 0) halfOpenFiles++;
                }
                safety = Math.min(safety, 6);
                if (((xk == 5) || (xk == 6)) && (yk == (white ? 0 : 7))) {
                    // FIXME! Handle rook trapped on a/b files too
                    int myRook = white ? Piece.WROOK : Piece.BROOK;
                    if (((pos.getPiece(yb + 6) == myRook) && (ph.nPawns[i][6] > 0)) ||
                        ((pos.getPiece(yb + 7) == myRook) && (ph.nPawns[i][7] > 0))) {
                        safety -= 5; // Trapped rook
                    }
                }
            }
            final int kSafety = (safety - 9) * 15 - halfOpenFiles * 25;
            if (white) {
                score += kSafety;
            } else {
                score -= kSafety;
            }
        }
        score += (bKingAttacks - wKingAttacks) * 4;
        final int kSafety = interpolate(m, minM, 0, maxM, score);
        return kSafety;

        // FIXME! g pawn is valuable (avoid g5, g4, gxf5)
    }

    /** Compute bishop evaluation. */
    private final int bishopEval(Position pos, int oldScore) {
        int score = 0;
        int nP = nPieces[Piece.WBISHOP];
        for (int i = 0; i < nP; i++) {
        	int sq = pieces[Piece.WBISHOP][i];
            mobilityAttacks = 0L;
        	score += bishMobScore[bishopMobility(pos, sq)];
            bKingAttacks += Long.bitCount(mobilityAttacks & bKingZone);
        }
        nP = nPieces[Piece.BBISHOP];
        for (int i = 0; i < nP; i++) {
        	int sq = pieces[Piece.BBISHOP][i];
            mobilityAttacks = 0L;
        	score -= bishMobScore[bishopMobility(pos, sq)];
            wKingAttacks += Long.bitCount(mobilityAttacks & wKingZone);
        }
        boolean whiteDark  = (pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskDarkSq ) != 0;
        boolean whiteLight = (pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskLightSq) != 0;
        boolean blackDark  = (pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskDarkSq ) != 0;
        boolean blackLight = (pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskLightSq) != 0;
        int numWhite = (whiteDark ? 1 : 0) + (whiteLight ? 1 : 0);
        int numBlack = (blackDark ? 1 : 0) + (blackLight ? 1 : 0);

        // Bishop pair bonus
        if (numWhite == 2) {
        	final int numPawns = nPieces[Piece.WPAWN];
        	score += 20 + (8 - numPawns) * 3;
        }
        if (numBlack == 2) {
            final int numPawns = nPieces[Piece.BPAWN];
            score -= 20 + (8 - numPawns) * 3;
        }

        // FIXME!!! Bad bishop

        if ((numWhite == 1) && (numBlack == 1) && (whiteDark != blackDark)) {
            final int penalty = (oldScore + score) / 2;
            final int qV = pieceValue[Piece.WQUEEN];
            final int rV = pieceValue[Piece.WROOK];
            final int bV = pieceValue[Piece.WBISHOP];
            final int loMtrl = 2 * bV;
            final int hiMtrl = 2 * (qV + rV + bV);
            int mtrl = pos.wMtrl + pos.bMtrl - pos.wMtrlPawns - pos.bMtrlPawns;
            score -= interpolate(mtrl, loMtrl, penalty, hiMtrl, 0);
        }

        return score;
    }
    
    /** Count the number of pseudo-legal moves for a bishop of given color on square (x0,y0). */
    private final static int bishopMobility(Position pos, int sq) {
        long occupied = pos.whiteBB | pos.blackBB;
        long atk = BitBoard.bishopAttacks(sq, occupied);
        mobilityAttacks |= atk;
        return Long.bitCount(atk & ~occupied);
    }

    private static long mobilityAttacks;

    /** Count the number of pseudo-legal moves for a rook of given color on square sq. */
    private final static int rookMobility(Position pos, int sq) {
        long occupied = pos.whiteBB | pos.blackBB;
        long atk = BitBoard.rookAttacks(sq, occupied);
        mobilityAttacks |= atk;
        return Long.bitCount(atk & ~occupied);
    }

    /** Implements special knowledge for some endgame situations. */
    private final int endGameEval(Position pos, int oldScore) {
        int score = oldScore;
        final int rV = pieceValue[Piece.WROOK];
    	if (pos.wMtrl + pos.bMtrl > 6 * rV)
    		return score;
        final int pV = pieceValue[Piece.WPAWN];
        final int bV = pieceValue[Piece.WBISHOP];
        final int nV = pieceValue[Piece.WKNIGHT];
        final int wMtrlPawns = pos.wMtrlPawns;
        final int bMtrlPawns = pos.bMtrlPawns;
        final int wMtrlNoPawns = pos.wMtrl - wMtrlPawns;
        final int bMtrlNoPawns = pos.bMtrl - bMtrlPawns;

        boolean handled = false;
        if ((wMtrlPawns + bMtrlPawns == 0) && (wMtrlNoPawns < rV) && (bMtrlNoPawns < rV)) {
            // King + minor piece vs king + minor piece is a draw
            score /= 50;
            handled = true;
        }
        if (!handled) {
            if (bMtrlPawns == 0) {
                if (wMtrlNoPawns - bMtrlNoPawns > bV) {
                    int wKnights = nPieces[Piece.WKNIGHT];
                    int wBishops = nPieces[Piece.WBISHOP];
                    if ((wKnights == 2) && (wMtrlNoPawns == 2 * nV) && (bMtrlNoPawns == 0)) {
                        score /= 50;    // KNNK is a draw
                    } else if ((wKnights == 1) && (wBishops == 1) && (wMtrlNoPawns == nV + bV) && (bMtrlNoPawns == 0)) {
                        score /= 10;
                        score += nV + bV + 300;
                        final int kSq = pos.getKingSq(false);
                        final int x = Position.getX(kSq);
                        final int y = Position.getY(kSq);
                        if (bishopOnDark(pos)) {
                            score += (7 - distToH1A8[7-y][7-x]) * 10;
                        } else {
                            score += (7 - distToH1A8[7-y][x]) * 10;
                        }
                    } else {
                        score += 300;       // Enough excess material, should win
                    }
                    handled = true;
                } else if ((wMtrlNoPawns + bMtrlNoPawns == 0) && (nPieces[Piece.WPAWN] == 1)) { // KPK
                    score = kpkEval(pos.getKingSq(true), pos.getKingSq(false),
                                    pieces[Piece.WPAWN][0], pos.whiteMove);
                    handled = true;
                }
            }
        }
        if (!handled && (score > 0)) {
            if ((wMtrlPawns == 0) && (wMtrlNoPawns <= bMtrlNoPawns + bV)) {
                score /= 8;         // Too little excess material, probably draw
                handled = true;
            } else if ((pos.wMtrl == pV + bV) && (nPieces[Piece.WPAWN] == 1) && (nPieces[Piece.WBISHOP] == 1)) {
                // FIXME! Use bitboards. Handle multiple pawns/bishops.
                // Check for rook pawn + wrong color bishop
                int pFile = Position.getX(pieces[Piece.WPAWN][0]);
                if ((pFile == 0) || (pFile == 7)) {
                    int bSq = pieces[Piece.WBISHOP][0];
                    boolean bDark = Position.darkSquare(Position.getX(bSq),
                                                        Position.getY(bSq));
                    if (bDark == (pFile == 0)) {
                        int oKing = pos.getKingSq(false);
                        int kx = Position.getX(oKing);
                        int ky = Position.getY(oKing);
                        if (((pFile == 0) && (kx <= 1) && (ky >= 6)) ||
                            ((pFile == 7) && (kx >= 6) && (ky >= 6))) {
                            score /= 50;
                            handled = true;
                        }
                    }
                }
            }
        }
        if (!handled) {
            if (wMtrlPawns == 0) {
                if (bMtrlNoPawns - wMtrlNoPawns > bV) {
                    int bKnights = nPieces[Piece.BKNIGHT];
                    int bBishops = nPieces[Piece.BBISHOP];
                    if ((bKnights == 2) && (bMtrlNoPawns == 2 * nV) && (wMtrlNoPawns == 0)) {
                        score /= 50;    // KNNK is a draw
                    } else if ((bKnights == 1) && (bBishops == 1) && (bMtrlNoPawns == nV + bV) && (wMtrlNoPawns == 0)) {
                        score /= 10;
                        score -= nV + bV + 300;
                        final int kSq = pos.getKingSq(true);
                        final int x = Position.getX(kSq);
                        final int y = Position.getY(kSq);
                        if (bishopOnDark(pos)) {
                            score -= (7 - distToH1A8[7-y][7-x]) * 10;
                        } else {
                            score -= (7 - distToH1A8[7-y][x]) * 10;
                        }
                    } else {
                        score -= 300;       // Enough excess material, should win
                    }
                    handled = true;
                } else if ((wMtrlNoPawns + bMtrlNoPawns == 0) && (nPieces[Piece.BPAWN] == 1)) { // KPK
                    score = -kpkEval(63-pos.getKingSq(false), 63-pos.getKingSq(true),
                                     63-pieces[Piece.BPAWN][0], !pos.whiteMove);
                    handled = true;
                }
            }
        }
        if (!handled && (score < 0)) {
            if ((bMtrlPawns == 0) && (bMtrlNoPawns <= wMtrlNoPawns + bV)) {
                score /= 8;         // Too little excess material, probably draw
                handled = true;
            } else if ((pos.bMtrl == pV + bV) && (nPieces[Piece.BPAWN] == 1) && (nPieces[Piece.BBISHOP] == 1)) {
                // Check for rook pawn + wrong color bishop
                int pFile = Position.getX(pieces[Piece.BPAWN][0]);
                if ((pFile == 0) || (pFile == 7)) {
                    int bSq = pieces[Piece.BBISHOP][0];
                    boolean bDark = Position.darkSquare(Position.getX(bSq),
                                                        Position.getY(bSq));
                    if (bDark == (pFile == 7)) {
                        int oKing = pos.getKingSq(true);
                        int kx = Position.getX(oKing);
                        int ky = Position.getY(oKing);
                        if (((pFile == 0) && (kx <= 1) && (ky <= 1)) ||
                            ((pFile == 7) && (kx >= 6) && (ky <= 1))) {
                            score /= 50;
                            handled = true;
                        }
                    }
                }
            }
        }
        if (!handled) {
            // In pawn end games, passed pawns not reachable by
            // opponent king are very dangerous
            int danger = 0;
            if (bMtrlNoPawns == 0) {
                int kingPos = pos.getKingSq(false);
                int kingX = Position.getX(kingPos);
                int kingY = Position.getY(kingPos);
                for (int x = 0; x < 8; x++) {
                    int y = ph.passedPawns[0][x];
                    if (y > 0) {
                        int pawnDist = Math.min(5, 7 - y);
                        int kingDistX = Math.abs(kingX - x);
                        int kingDistY = Math.abs(kingY - 7);
                        int kingDist = Math.max(kingDistX, kingDistY);
                        if (!pos.whiteMove)
                            kingDist--;
                        if (pawnDist < kingDist)
                            danger += 500;
                    }
                }
            }
            if (wMtrlNoPawns == 0) {
                int kingPos = pos.getKingSq(true);
                int kingX = Position.getX(kingPos);
                int kingY = Position.getY(kingPos);
                for (int x = 0; x < 8; x++) {
                    int y = ph.passedPawns[1][x];
                    if (y > 0) {
                        int pawnDist = Math.min(5, y);
                        int kingDistX = Math.abs(kingX - x);
                        int kingDistY = Math.abs(kingY - 0);
                        int kingDist = Math.max(kingDistX, kingDistY);
                        if (pos.whiteMove)
                            kingDist--;
                        if (pawnDist < kingDist)
                            danger -= 500;
                    }
                }
            }
            score += danger;
        }
        return score;
    }

    private static final int kpkEval(int wKing, int bKing, int wPawn, boolean whiteMove) {
        if (Position.getX(wKing) >= 4) { // Mirror X
            wKing ^= 7;
            bKing ^= 7;
            wPawn ^= 7;
        }
        int index = whiteMove ? 0 : 1;
        index = index * 32 + Position.getY(wKing)*4+Position.getX(wKing);
        index = index * 64 + bKing;
        index = index * 48 + wPawn - 8;

        int bytePos = index / 8;
        int bitPos = index % 8;
        boolean draw = (((int)kpkTable[bytePos]) & (1 << bitPos)) == 0;
        if (draw)
            return 0;
        final int qV = pieceValue[Piece.WQUEEN];
        final int pV = pieceValue[Piece.WPAWN];
        return qV - pV / 4 * (7-Position.getY(wPawn));
    }

    /**
     * Decide if there is a bishop on a white square.
     * Note that this method assumes that there is at most one bishop.
     */
    private final boolean bishopOnDark(Position pos) {
    	int nP = nPieces[Piece.WBISHOP];
    	if (nP > 0) {
    		int sq = pieces[Piece.WBISHOP][0];
            final int x = Position.getX(sq);
            final int y = Position.getY(sq);
            if (Position.darkSquare(x, y))
            	return true;
    	}
    	nP = nPieces[Piece.BBISHOP];
    	if (nP > 0) {
    		int sq = pieces[Piece.BBISHOP][0];
            final int x = Position.getX(sq);
            final int y = Position.getY(sq);
            if (Position.darkSquare(x, y))
            	return true;
    	}
    	return false;
    }

    /**
     * Interpolate between (x1,y1) and (x2,y2).
     * If x < x1, return y1, if x > x2 return y2. Otherwise, use linear interpolation.
     */
    private static final int interpolate(int x, int x1, int y1, int x2, int y2) {
    	if (x > x2) {
    		return y2;
    	} else if (x < x1) {
            return y1;
        } else {
            return (x - x1) * (y2 - y1) / (x2 - x1) + y1;
        }
    }
}
