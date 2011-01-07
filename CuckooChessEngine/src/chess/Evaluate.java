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
    static final int[] kt1b = { -22,-35,-40,-40,-40,-40,-35,-22,
  							    -22,-35,-40,-40,-40,-40,-35,-22,
    						    -25,-35,-40,-45,-45,-40,-35,-25,
    						    -15,-30,-35,-40,-40,-35,-30,-15,
    						    -10,-15,-20,-25,-25,-20,-15,-10,
    					          4, -2, -5,-15,-15, -5, -2,  4,
    				 			 16, 14,  7, -3, -3,  7, 14, 16,
    			 				 24, 24,  9,  0,  0,  9, 24, 24 };
    
    /** Piece/square table for king during end game. */
    static final int[] kt2b = {  0,  8, 16, 24, 24, 16,  8,  0,
    						     8, 16, 24, 32, 32, 24, 16,  8,
    						    16, 24, 32, 40, 40, 32, 24, 16,
    						    24, 32, 40, 48, 48, 40, 32, 24,
    						    24, 32, 40, 48, 48, 40, 32, 24,
    						    16, 24, 32, 40, 40, 32, 24, 16,
    						     8, 16, 24, 32, 32, 24, 16,  8,
    						     0,  8, 16, 24, 24, 16,  8,  0 };

    /** Piece/square table for pawns during middle game. */
    static final int[] pt1b = {   0,  0,  0,  0,  0,  0,  0,  0,
    						 	 16, 32, 48, 64, 64, 48, 32, 16,
    							  6, 24, 40, 56, 56, 40, 24,  6,
    						    -10,  8, 20, 40, 40, 20,  8,-10,
    						    -12,  8, 10, 32, 32, 10,  8,-12,
    						    -12,  8,  4, 10, 10,  4,  8,-12,
    						    -12,  8,  8, -7, -7,  8,  8,-12,
    						 	 0,  0,  0,  0,  0,  0,  0,  0 };

    /** Piece/square table for pawns during end game. */
    static final int[] pt2b = {   0,  0,  0,  0,  0,  0,  0,  0,
    							 50, 80, 90, 90, 90, 90, 80, 50,
    							 34, 64, 69, 69, 69, 69, 64, 34,
    							 10, 48, 48, 48, 48, 48, 48, 10,
    						    -18, 22, 22, 22, 22, 22, 22,-18,
    						    -34,  6,  6,  6,  6,  6,  6,-34,
    						    -40,  0,  0,  0,  0,  0,  0,-40,
    						 	  0,  0,  0,  0,  0,  0,  0,  0 };

    /** Piece/square table for knights during middle game. */
    static final int[] nt1b = { -50,-40,-30,-20,-20,-30,-40,-50,
    						    -40,-30,-10,  0,  0,-10,-30,-40,
    						    -20,  5, 10, 15, 15, 10,  5,-20,
    						    -17,  0, 10, 20, 20, 10,  0,-17,
    						    -17,  0,  3, 20, 20,  3,  0,-17,
    						    -20,-10,  0,  5,  5,  0,-10,-20,
    						    -40,-30,-10,  0,  0,-10,-30,-40,
    						    -50,-40,-30,-20,-20,-30,-40,-50 };

    /** Piece/square table for knights during end game. */
    static final int[] nt2b = { -50,-40,-30,-20,-20,-30,-40,-50,
    						    -40,-30,-10, -5, -5,-10,-30,-40,
    						    -30,-10,  0, 10, 10,  0,-10,-30,
    						    -20, -5, 10, 20, 20, 10, -5,-20,
    						    -20, -5, 10, 20, 20, 10, -5,-20,
    						    -30,-10,  0, 10, 10,  0,-10,-30,
    						    -40,-30,-10, -5, -5,-10,-30,-40,
    						    -50,-40,-30,-20,-20,-30,-40,-50 };

    /** Piece/square table for bishops during middle game. */
    static final int[] bt1b = {  0,  0,  0,  0,  0,  0,  0,  0,
    							 0,  8,  4,  4,  4,  4,  8,  0,
    							 0,  4,  8,  8,  8,  8,  4,  0,
    							 0,  4,  8,  8,  8,  8,  4,  0,
    							 0,  4,  8,  8,  8,  8,  4,  0,
    							 0,  6,  8,  8,  8,  8,  6,  0,
    							 0,  8,  4,  4,  4,  4,  8,  0,
    							 0,  0,  0,  0,  0,  0,  0,  0 };

    /** Piece/square table for queens during middle game. */
    static final int[] qt1b = { -10, -5,  0,  0,  0,  0, -5,-10,
				 				 -5,  0,  5,  5,  5,  5,  0, -5,
				 				  0,  5,  5,  6,  6,  5,  5,  0,
				 				  0,  5,  6,  6,  6,  6,  5,  0,
				 				  0,  5,  6,  6,  6,  6,  5,  0,
				 				  0,  5,  5,  6,  6,  5,  5,  0,
				 				 -5,  0,  5,  5,  5,  5,  0, -5,
				 			    -10, -5,  0,  0,  0,  0, -5,-10 };

    /** Piece/square table for rooks during middle game. */
    static final int[] rt1b = {  0,  3,  5,  5,  5,  5,  3,  0,
				 			    15, 20, 20, 20, 20, 20, 20, 15,
				 			     0,  0,  0,  0,  0,  0,  0,  0,
				 			     0,  0,  0,  0,  0,  0,  0,  0,
				 			    -2,  0,  0,  0,  0,  0,  0, -2,
				 			    -2,  0,  0,  2,  2,  0,  0, -2,
				 			    -3,  2,  5,  5,  5,  5,  2, -3,
				 			     0,  3,  5,  5,  5,  5,  3,  0 };

    static final int[] kt1w, qt1w, rt1w, bt1w, nt1w, pt1w, kt2w, nt2w, pt2w;
    static {
        kt1w = new int[64];
        qt1w = new int[64];
        rt1w = new int[64];
        bt1w = new int[64];
        nt1w = new int[64];
        pt1w = new int[64];
        kt2w = new int[64];
        nt2w = new int[64];
        pt2w = new int[64];
        for (int i = 0; i < 64; i++) {
            kt1w[i] = kt1b[63-i];
            qt1w[i] = qt1b[63-i];
            rt1w[i] = rt1b[63-i];
            bt1w[i] = bt1b[63-i];
            nt1w[i] = nt1b[63-i];
            pt1w[i] = pt1b[63-i];
            kt2w[i] = kt2b[63-i];
            nt2w[i] = nt2b[63-i];
            pt2w[i] = pt2b[63-i];
        }
    }

    private static final int[] empty = { 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,
                                         0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,
                                         0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,
                                         0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0};
    static final int[][] psTab1 = { empty, kt1w, qt1w, rt1w, bt1w, nt1w, pt1w,
                                           kt1b, qt1b, rt1b, bt1b, nt1b, pt1b };
    static final int[][] psTab2 = { empty, kt2w, qt1w, rt1w, bt1w, nt2w, pt2w,
                                           kt2b, qt1b, rt1b, bt1b, nt2b, pt2b };

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
    	long key;
    	int score;         // Positive score means good for white
    	short passedBonusW;
    	short passedBonusB;
    	long passedPawnsW;     // The most advanced passed pawns for each file
    	long passedPawnsB;
    }
    PawnHashData ph;
    static PawnHashData[] pawnHash;
    static {
        final int numEntries = 1<<16;
    	pawnHash = new PawnHashData[numEntries];
        for (int i = 0; i < numEntries; i++) {
            PawnHashData phd = new PawnHashData();
            phd.key = -1; // Non-zero to avoid collision for positions with no pawns
            phd.score = 0;
            pawnHash[i] = phd;
        }
    }
    
    static byte[] kpkTable = null;

    // King safety variables
    private long wKingZone, bKingZone;       // Squares close to king that are worth attacking
    private int wKingAttacks, bKingAttacks; // Number of attacks close to white/black king
    
    /** Constructor. */
    public Evaluate() {
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
        
        // Kings
        {
            final int t1 = qV + 2 * rV + 2 * bV;
            final int t2 = rV;
            {
                final int k1 = pos.psScore1[Piece.WKING];
                final int k2 = pos.psScore2[Piece.WKING];
                final int t = bMtrl - bMtrlPawns;
                score += interpolate(t, t2, k2, t1, k1);
            }
            {
                final int k1 = pos.psScore1[Piece.BKING];
                final int k2 = pos.psScore2[Piece.BKING];
                final int t = wMtrl - wMtrlPawns;
                score -= interpolate(t, t2, k2, t1, k1);
            }
        }

        // Pawns
        {
            final int t1 = qV + 2 * rV + 2 * bV;
            final int t2 = rV;
            int wp1 = pos.psScore1[Piece.WPAWN];
            int wp2 = pos.psScore2[Piece.WPAWN];
            if ((wp1 != 0) || (wp2 != 0)) {
                final int tw = bMtrl - bMtrlPawns;
                score += interpolate(tw, t2, wp2, t1, wp1) / 2;
            }
            int bp1 = pos.psScore1[Piece.BPAWN];
            int bp2 = pos.psScore2[Piece.BPAWN];
            if ((bp1 != 0) || (bp2 != 0)) {
                final int tb = wMtrl - wMtrlPawns;
                score -= interpolate(tb, t2, bp2, t1, bp1) / 2;
            }
        }

        // Knights
        {
            final int t1 = qV + 2 * rV + 1 * bV + 1 * nV + 6 * pV;
            final int t2 = nV + 8 * pV;
            int n1 = pos.psScore1[Piece.WKNIGHT];
            int n2 = pos.psScore2[Piece.WKNIGHT];
            if ((n1 != 0) || (n2 != 0)) {
                score += interpolate(bMtrl, t2, n2, t1, n1);
            }
            n1 = pos.psScore1[Piece.BKNIGHT];
            n2 = pos.psScore2[Piece.BKNIGHT];
            if ((n1 != 0) || (n2 != 0)) {
                score -= interpolate(wMtrl, t2, n2, t1, n1);
            }
        }

        // Bishops
        {
            score += pos.psScore1[Piece.WBISHOP];
            score -= pos.psScore1[Piece.BBISHOP];
        }

        // Queens
        {
            score += pos.psScore1[Piece.WQUEEN];
            long m = pos.pieceTypeBB[Piece.WQUEEN];
            while (m != 0) {
                int sq = Long.numberOfTrailingZeros(m);
                mobilityAttacks = 0L;
                score += queenMobScore[rookMobility(pos, sq) + bishopMobility(pos, sq)];
                bKingAttacks += Long.bitCount(mobilityAttacks & bKingZone) * 2;
                m &= m-1;
            }
            score -= pos.psScore1[Piece.BQUEEN];
            m = pos.pieceTypeBB[Piece.BQUEEN];
            while (m != 0) {
                int sq = Long.numberOfTrailingZeros(m);
                mobilityAttacks = 0L;
                score -= queenMobScore[rookMobility(pos, sq) + bishopMobility(pos, sq)];
                wKingAttacks += Long.bitCount(mobilityAttacks & wKingZone) * 2;
                m &= m-1;
            }
        }

        // Rooks
        {
            int r1 = pos.psScore1[Piece.WROOK];
            if (r1 != 0) {
                final int nP = bMtrlPawns / pV;
                final int s = r1 * Math.min(nP, 6) / 6;
                score += s;
            }
            r1 = pos.psScore1[Piece.BROOK];
            if (r1 != 0) {
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

        final int k1 = kt1b[7*8+6] - kt1b[7*8+4];
        final int k2 = kt2b[7*8+6] - kt2b[7*8+4];
        final int t1 = qV + 2 * rV + 2 * bV;
        final int t2 = rV;
        final int t = pos.bMtrl - pos.bMtrlPawns;
        final int ks = interpolate(t, t2, k2, t1, k1);

        final int castleValue = ks + rt1b[7*8+5] - rt1b[7*8+7];
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

    /** Compute pawn hash data for pos. */
	private final void computePawnHashData(Position pos, PawnHashData ph) {
    	int score = 0;

        // Evaluate double pawns and pawn islands
        int wDouble = 0;
        int bDouble = 0;
        int wIslands = 0;
        int bIslands = 0;
        boolean wasPawn = false;
        for (int x = 0; x < 8; x++) {
            long m = pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskFile[x];
            if (m != 0) {
                wDouble += Long.bitCount(m) - 1;
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
            long m = pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskFile[x];
            if (m != 0) {
                bDouble += Long.bitCount(m) - 1;
                if (!wasPawn) {
                    bIslands++;
                    wasPawn = true;
                }
            } else {
                wasPawn = false;
            }
        }
        score -= (wDouble - bDouble) * 20;  // FIXME! Try larger values
        score -= (wIslands - bIslands) * 15;

        // Evaluate passed pawn bonus
        int passedBonusW = 0;
        int passedBonusB = 0;
        long passedPawnsW = 0;
        long passedPawnsB = 0;
        for (int x = 0; x < 8; x++) {
            long m = pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskFile[x];
        	if (m != 0) {
        	    int sq = 63-Long.numberOfLeadingZeros(m);
        		boolean passed = (BitBoard.wPawnBlockerMask[sq] & pos.pieceTypeBB[Piece.BPAWN]) == 0;
        		if (passed) {
                    int y = Position.getY(sq);
        			passedBonusW += 20 + y * 4;
        			if ((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.bPawnAttacks[sq]) != 0)
        				passedBonusW += 15;  // Guarded passed pawn
        			passedPawnsW |= 1L << sq;
        		}
        	}
            m = pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskFile[x];
        	if (m != 0) {
        	    int sq = Long.numberOfTrailingZeros(m);
                boolean passed = (BitBoard.bPawnBlockerMask[sq] & pos.pieceTypeBB[Piece.WPAWN]) == 0;
        		if (passed) {
                    int y = Position.getY(sq);
        			passedBonusB += 20 + (7-y) * 4;
                    if ((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.wPawnAttacks[sq]) != 0)
        				passedBonusB += 15;  // Guarded passed pawn
                    passedPawnsB |= 1L << sq;
        		}
            }
        }

        ph.key = pos.pawnZobristHash();
        ph.score = score;
        ph.passedBonusW = (short)passedBonusW;
        ph.passedBonusB = (short)passedBonusB;
        ph.passedPawnsW = passedPawnsW;
        ph.passedPawnsB = passedPawnsB;
    }

    /** Compute rook bonus. Rook on open/half-open file. */
	private final int rookBonus(Position pos) {
        int score = 0;
        final long wPawns = pos.pieceTypeBB[Piece.WPAWN];
        final long bPawns = pos.pieceTypeBB[Piece.BPAWN];
        long m = pos.pieceTypeBB[Piece.WROOK];
        while (m != 0) {
        	int sq = Long.numberOfTrailingZeros(m);
            final int x = Position.getX(sq);
            if ((wPawns & BitBoard.maskFile[x]) == 0) { // At least half-open file
                score += (bPawns & BitBoard.maskFile[x]) == 0 ? 25 : 12;
            }
            mobilityAttacks = 0L;
            score += rookMobScore[rookMobility(pos, sq)];
            bKingAttacks += Long.bitCount(mobilityAttacks & bKingZone);
            m &= m-1;
        }
        if ((Long.bitCount(pos.pieceTypeBB[Piece.WROOK] & 0x00ff000000000000L) > 1) &&
            ((pos.pieceTypeBB[Piece.BKING] & 0xff00000000000000L) != 0))
            score += 20; // Two rooks on 7:th row
        m = pos.pieceTypeBB[Piece.BROOK];
        while (m != 0) {
        	int sq = Long.numberOfTrailingZeros(m);
            final int x = Position.getX(sq);
            if ((bPawns & BitBoard.maskFile[x]) == 0) {
                score -= (wPawns & BitBoard.maskFile[x]) == 0 ? 25 : 12;
            }
            mobilityAttacks = 0L;
            score -= rookMobScore[rookMobility(pos, sq)];
            wKingAttacks += Long.bitCount(mobilityAttacks & wKingZone);
            m &= m-1;
        }
        if ((Long.bitCount(pos.pieceTypeBB[Piece.BROOK] & 0xff00L) > 1) &&
            ((pos.pieceTypeBB[Piece.WKING] & 0xffL) != 0))
          score -= 20; // Two rooks on 2:nd row
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
                	long oPawns = pos.pieceTypeBB[white ? Piece.BPAWN : Piece.WPAWN];
                	if ((oPawns & BitBoard.maskFile[x]) == 0) halfOpenFiles++;
                }
                safety = Math.min(safety, 6);
                if (white) {
                    if (((pos.pieceTypeBB[Piece.WKING] & 0x60L) != 0) && // King on f1 or g1
                        ((pos.pieceTypeBB[Piece.WROOK] & 0xC0L) != 0) && // Rook on g1 or h1
                        ((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskFile[6]) != 0) &&
                        ((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskFile[7]) != 0)) {
                        safety -= 6;
                    } else
                    if (((pos.pieceTypeBB[Piece.WKING] & 0x6L) != 0) && // King on b1 or c1
                        ((pos.pieceTypeBB[Piece.WROOK] & 0x3L) != 0) && // Rook on a1 or b1
                        ((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskFile[0]) != 0) &&
                        ((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskFile[1]) != 0)) {
                        safety -= 6;
                    }
                } else {
                    if (((pos.pieceTypeBB[Piece.BKING] & 0x6000000000000000L) != 0) && // King on f8 or g8
                        ((pos.pieceTypeBB[Piece.BROOK] & 0xC000000000000000L) != 0) && // Rook on g8 or h8
                        ((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskFile[6]) != 0) &&
                        ((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskFile[7]) != 0)) {
                        safety -= 6;
                    } else
                    if (((pos.pieceTypeBB[Piece.BKING] & 0x600000000000000L) != 0) && // King on b8 or c8
                        ((pos.pieceTypeBB[Piece.BROOK] & 0x300000000000000L) != 0) && // Rook on a8 or b8
                        ((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskFile[0]) != 0) &&
                        ((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskFile[1]) != 0)) {
                        safety -= 6;
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
        long m = pos.pieceTypeBB[Piece.WBISHOP];
        while (m != 0) {
            int sq = Long.numberOfTrailingZeros(m);
            mobilityAttacks = 0L;
        	score += bishMobScore[bishopMobility(pos, sq)];
            bKingAttacks += Long.bitCount(mobilityAttacks & bKingZone);
            m &= m-1;
        }
        m = pos.pieceTypeBB[Piece.BBISHOP];
        while (m != 0) {
        	int sq = Long.numberOfTrailingZeros(m);
            mobilityAttacks = 0L;
        	score -= bishMobScore[bishopMobility(pos, sq)];
            wKingAttacks += Long.bitCount(mobilityAttacks & wKingZone);
            m &= m-1;
        }
        boolean whiteDark  = (pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskDarkSq ) != 0;
        boolean whiteLight = (pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskLightSq) != 0;
        boolean blackDark  = (pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskDarkSq ) != 0;
        boolean blackLight = (pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskLightSq) != 0;
        int numWhite = (whiteDark ? 1 : 0) + (whiteLight ? 1 : 0);
        int numBlack = (blackDark ? 1 : 0) + (blackLight ? 1 : 0);

        // Bishop pair bonus
        if (numWhite == 2) {
        	final int numPawns = Long.bitCount(pos.pieceTypeBB[Piece.WPAWN]);
        	score += 20 + (8 - numPawns) * 3;
        }
        if (numBlack == 2) {
            final int numPawns = Long.bitCount(pos.pieceTypeBB[Piece.BPAWN]);
            score -= 20 + (8 - numPawns) * 3;
        }

        // FIXME!!! Bad bishop

        if ((numWhite == 1) && (numBlack == 1) && (whiteDark != blackDark) &&
            (pos.wMtrl - pos.wMtrlPawns == pos.bMtrl - pos.bMtrlPawns)) {
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
        return Long.bitCount(atk & ~occupied); // FIXME! Test to only mask out own pieces
    }

    /** Implements special knowledge for some endgame situations. */
    private final int endGameEval(Position pos, int oldScore) {
        int score = oldScore;
        final int rV = pieceValue[Piece.WROOK];
    	if (pos.wMtrl + pos.bMtrl > 6 * rV)
    		return score;
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
            // In pawn end games, passed pawns not reachable by
            // opponent king are very dangerous
            int danger = 0;
            if (bMtrlNoPawns == 0) {
                int kingPos = pos.getKingSq(false);
                int kingX = Position.getX(kingPos);
                int kingY = Position.getY(kingPos);
                long m = ph.passedPawnsW;
                while (m != 0) {
                    int sq = Long.numberOfTrailingZeros(m);
                    int x = Position.getX(sq);
                    int y = Position.getY(sq);
                    int pawnDist = Math.min(5, 7 - y);
                    int kingDistX = Math.abs(kingX - x);
                    int kingDistY = Math.abs(kingY - 7);
                    int kingDist = Math.max(kingDistX, kingDistY);
                    if (!pos.whiteMove)
                        kingDist--;
                    if (pawnDist < kingDist)
                        danger += 500;
                    m &= m-1;
                }
            }
            if (wMtrlNoPawns == 0) {
                int kingPos = pos.getKingSq(true);
                int kingX = Position.getX(kingPos);
                int kingY = Position.getY(kingPos);
                long m = ph.passedPawnsB;
                while (m != 0) {
                    int sq = Long.numberOfTrailingZeros(m);
                    int x = Position.getX(sq);
                    int y = Position.getY(sq);
                    int pawnDist = Math.min(5, y);
                    int kingDistX = Math.abs(kingX - x);
                    int kingDistY = Math.abs(kingY - 0);
                    int kingDist = Math.max(kingDistX, kingDistY);
                    if (pos.whiteMove)
                        kingDist--;
                    if (pawnDist < kingDist)
                        danger -= 500;
                    m &= m-1;
                }
            }
            score += danger;
        }
        final int qV = pieceValue[Piece.WQUEEN];
        final int pV = pieceValue[Piece.WPAWN];
        if (!handled && (pos.wMtrl == qV) && (pos.bMtrl == pV) &&
            (Long.bitCount(pos.pieceTypeBB[Piece.WQUEEN]) == 1)) {
            int wk = Long.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WKING]);
            int wq = Long.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WQUEEN]);
            int bk = Long.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BKING]);
            int bp = Long.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BPAWN]);
            score = evalKQKP(wk, wq, bk, bp);
            handled = true;
        }
        if (!handled && (pos.bMtrl == qV) && (pos.wMtrl == pV) && 
            (Long.bitCount(pos.pieceTypeBB[Piece.BQUEEN]) == 1)) {
            int bk = Long.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BKING]);
            int bq = Long.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BQUEEN]);
            int wk = Long.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WKING]);
            int wp = Long.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WPAWN]);
            score = -evalKQKP(63-bk, 63-bq, 63-wk, 63-wp);
            handled = true;
        }
        if (!handled && (score > 0)) {
            if ((wMtrlPawns == 0) && (wMtrlNoPawns <= bMtrlNoPawns + bV)) {
                score /= 8;         // Too little excess material, probably draw
                handled = true;
            } else if ((pos.pieceTypeBB[Piece.WROOK] | pos.pieceTypeBB[Piece.WKNIGHT] |
                        pos.pieceTypeBB[Piece.WQUEEN]) == 0) {
                // Check for rook pawn + wrong color bishop
                if (((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskBToHFiles) == 0) &&
                    ((pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskLightSq) == 0) &&
                    ((pos.pieceTypeBB[Piece.BKING] & 0x0303000000000000L) != 0)) {
                    score /= 50;
                    handled = true;
                } else
                if (((pos.pieceTypeBB[Piece.WPAWN] & BitBoard.maskAToGFiles) == 0) &&
                    ((pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskDarkSq) == 0) &&
                    ((pos.pieceTypeBB[Piece.BKING] & 0xC0C0000000000000L) != 0)) {
                    score /= 50;
                    handled = true;
                }
            }
        }
        if (!handled) {
            if (bMtrlPawns == 0) {
                if (wMtrlNoPawns - bMtrlNoPawns > bV) {
                    int wKnights = Long.bitCount(pos.pieceTypeBB[Piece.WKNIGHT]);
                    int wBishops = Long.bitCount(pos.pieceTypeBB[Piece.WBISHOP]);
                    if ((wKnights == 2) && (wMtrlNoPawns == 2 * nV) && (bMtrlNoPawns == 0)) {
                        score /= 50;    // KNNK is a draw
                    } else if ((wKnights == 1) && (wBishops == 1) && (wMtrlNoPawns == nV + bV) && (bMtrlNoPawns == 0)) {
                        score /= 10;
                        score += nV + bV + 300;
                        final int kSq = pos.getKingSq(false);
                        final int x = Position.getX(kSq);
                        final int y = Position.getY(kSq);
                        if ((pos.pieceTypeBB[Piece.WBISHOP] & BitBoard.maskDarkSq) != 0) {
                            score += (7 - distToH1A8[7-y][7-x]) * 10;
                        } else {
                            score += (7 - distToH1A8[7-y][x]) * 10;
                        }
                    } else {
                        score += 300;       // Enough excess material, should win
                    }
                    handled = true;
                } else if ((wMtrlNoPawns + bMtrlNoPawns == 0) && (wMtrlPawns == pV)) { // KPK
                    int wp = Long.numberOfTrailingZeros(pos.pieceTypeBB[Piece.WPAWN]);
                    score = kpkEval(pos.getKingSq(true), pos.getKingSq(false),
                                    wp, pos.whiteMove);
                    handled = true;
                }
            }
        }
        if (!handled && (score < 0)) {
            if ((bMtrlPawns == 0) && (bMtrlNoPawns <= wMtrlNoPawns + bV)) {
                score /= 8;         // Too little excess material, probably draw
                handled = true;
            } else if ((pos.pieceTypeBB[Piece.BROOK] | pos.pieceTypeBB[Piece.BKNIGHT] |
                        pos.pieceTypeBB[Piece.BQUEEN]) == 0) {
                // Check for rook pawn + wrong color bishop
                if (((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskBToHFiles) == 0) &&
                    ((pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskDarkSq) == 0) &&
                    ((pos.pieceTypeBB[Piece.WKING] & 0x0303L) != 0)) {
                    score /= 50;
                    handled = true;
                } else
                if (((pos.pieceTypeBB[Piece.BPAWN] & BitBoard.maskAToGFiles) == 0) &&
                    ((pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskLightSq) == 0) &&
                    ((pos.pieceTypeBB[Piece.WKING] & 0xC0C0L) != 0)) {
                    score /= 50;
                    handled = true;
                }
            }
        }
        if (!handled) {
            if (wMtrlPawns == 0) {
                if (bMtrlNoPawns - wMtrlNoPawns > bV) {
                    int bKnights = Long.bitCount(pos.pieceTypeBB[Piece.BKNIGHT]);
                    int bBishops = Long.bitCount(pos.pieceTypeBB[Piece.BBISHOP]);
                    if ((bKnights == 2) && (bMtrlNoPawns == 2 * nV) && (wMtrlNoPawns == 0)) {
                        score /= 50;    // KNNK is a draw
                    } else if ((bKnights == 1) && (bBishops == 1) && (bMtrlNoPawns == nV + bV) && (wMtrlNoPawns == 0)) {
                        score /= 10;
                        score -= nV + bV + 300;
                        final int kSq = pos.getKingSq(true);
                        final int x = Position.getX(kSq);
                        final int y = Position.getY(kSq);
                        if ((pos.pieceTypeBB[Piece.BBISHOP] & BitBoard.maskDarkSq) != 0) {
                            score -= (7 - distToH1A8[7-y][7-x]) * 10;
                        } else {
                            score -= (7 - distToH1A8[7-y][x]) * 10;
                        }
                    } else {
                        score -= 300;       // Enough excess material, should win
                    }
                    handled = true;
                } else if ((wMtrlNoPawns + bMtrlNoPawns == 0) && (bMtrlPawns == pV)) { // KPK
                    int bp = Long.numberOfTrailingZeros(pos.pieceTypeBB[Piece.BPAWN]);
                    score = -kpkEval(63-pos.getKingSq(false), 63-pos.getKingSq(true),
                                     63-bp, !pos.whiteMove);
                    handled = true;
                }
            }
        }
        return score;

        // FIXME! Add evaluation of KRKP
        // FIXME! Add evaluation of KRPKP   : eg 8/8/8/5pk1/1r6/R7/8/4K3 w - - 0 74
    }

    private static final int evalKQKP(int wKing, int wQueen, int bKing, int bPawn) {
        boolean canWin = false;
        if (((1L << bKing) & 0xFFFF) == 0) {
            canWin = true; // King doesn't support pawn
        } else if (Math.abs(Position.getX(bPawn) - Position.getX(bKing)) > 2) {
            canWin = true; // King doesn't support pawn
        } else {
            switch (bPawn) {
            case 8:  // a2
                canWin = ((1L << wKing) & 0x0F1F1F1F1FL) != 0;
                break;
            case 10: // c2
                canWin = ((1L << wKing) & 0x071F1F1FL) != 0;
                break;
            case 13: // f2
                canWin = ((1L << wKing) & 0xE0F8F8F8L) != 0;
                break;
            case 15: // h2
                canWin = ((1L << wKing) & 0xF0F8F8F8F8L) != 0;
                break;
            default:
                canWin = true;
                break;
            }
        }

        final int qV = pieceValue[Piece.WQUEEN];
        final int pV = pieceValue[Piece.WPAWN];
        final int dist = Math.max(Math.abs(Position.getX(wKing)-Position.getX(bPawn)),
                                  Math.abs(Position.getY(wKing)-Position.getY(bPawn)));
        int score = qV - pV - 20 * dist;
        if (!canWin)
            score /= 50;
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
