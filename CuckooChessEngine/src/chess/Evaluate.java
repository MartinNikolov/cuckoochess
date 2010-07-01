/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

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
    static final int[][] kt1 = { {-22,-35,-40,-40,-40,-40,-35,-22 },
    							 {-22,-35,-40,-40,-40,-40,-35,-22 },
    							 {-25,-35,-40,-45,-45,-40,-35,-25 },
    							 {-15,-30,-35,-40,-40,-35,-30,-15 },
    							 {-10,-15,-20,-25,-25,-20,-15,-10 },
    							 {  8,  6, -5,-15,-15, -5,  6,  8 },
    							 { 16, 14, 12,  7,  7, 12, 14, 16 },
    							 { 24, 24, 14, 10, 10, 14, 24, 24 } };
    
    /** Piece/square table for king during end game. */
    static final int[][] kt2 = { {  0,  8, 16, 24, 24, 16,  8,  0 },
    							 {  8, 16, 24, 32, 32, 24, 16,  8 },
    							 { 16, 24, 32, 40, 40, 32, 24, 16 },
    							 { 24, 32, 40, 48, 48, 40, 32, 24},
    							 { 24, 32, 40, 48, 48, 40, 32, 24},
    							 { 16, 24, 32, 40, 40, 32, 24, 16},
    							 {  8, 16, 24, 32, 32, 24, 16,  8 },
    							 {  0,  8, 16, 24, 24, 16,  8,  0 } };

    /** Piece/square table for pawns during middle game. */
    static final int[][] pt1 = { {  0,  0,  0,  0,  0,  0,  0,  0 },
    							 { 16, 32, 48, 64, 64, 48, 32, 16 },
    							 {  6, 24, 40, 56, 56, 40, 24,  6 },
    							 {-10,  8, 20, 40, 40, 20,  8,-10 },
    							 {-12,  8, 10, 32, 32, 10,  8,-12 },
    							 {-12,  8,  4, 10, 10,  4,  8,-12 },
    							 {-12,  8,  8, -7, -7,  8,  8,-12 },
    							 {  0,  0,  0,  0,  0,  0,  0,  0 } };

    /** Piece/square table for pawns during end game. */
    static final int[][] pt2 = { {  0,  0,  0,  0,  0,  0,  0,  0 },
    							 { 50, 80, 90, 90, 90, 90, 80, 50 },
    							 { 34, 64, 69, 69, 69, 69, 64, 34 },
    							 { 10, 48, 48, 48, 48, 48, 48, 10 },
    							 {-18, 22, 22, 22, 22, 22, 22,-18 },
    							 {-34,  6,  6,  6,  6,  6,  6,-34 },
    							 {-40,  0,  0,  0,  0,  0,  0,-40 },
    							 {  0,  0,  0,  0,  0,  0,  0,  0 } };

    /** Piece/square table for knights during middle game. */
    static final int[][] nt1 = { {-50,-40,-30,-20,-20,-30,-40,-50 },
    							 {-40,-30,-10,  0,  0,-10,-30,-40 },
    							 {-20,  5, 10, 15, 15, 10,  5,-20 },
    							 {-17,  0, 10, 20, 20, 10,  0,-17 },
    							 {-17,  0,  3, 20, 20,  3,  0,-17 },
    							 {-20,-10,  0,  5,  5,  0,-10,-20 },
    							 {-40,-30,-10,  0,  0,-10,-30,-40 },
    							 {-50,-40,-30,-20,-20,-30,-40,-50 } };

    /** Piece/square table for knights during end game. */
    static final int[][] nt2 = { {-50,-40,-30,-20,-20,-30,-40,-50 },
    							 {-40,-30,-10, -5, -5,-10,-30,-40 },
    							 {-30,-10,  0, 10, 10,  0,-10,-30 },
    							 {-20, -5, 10, 20, 20, 10, -5,-20 },
    							 {-20, -5, 10, 20, 20, 10, -5,-20 },
    							 {-30,-10,  0, 10, 10,  0,-10,-30 },
    							 {-40,-30,-10, -5, -5,-10,-30,-40 },
    							 {-50,-40,-30,-20,-20,-30,-40,-50 } };

    /** Piece/square table for bishops during middle game. */
    static final int[][] bt1 = { {  0,  0,  0,  0,  0,  0,  0,  0 },
    							 {  0,  8,  4,  4,  4,  4,  8,  0 },
    							 {  0,  4,  8,  8,  8,  8,  4,  0 },
    							 {  0,  4,  8,  8,  8,  8,  4,  0 },
    							 {  0,  4,  8,  8,  8,  8,  4,  0 },
    							 {  0,  6,  8,  8,  8,  8,  6,  0 },
    							 {  0,  8,  4,  4,  4,  4,  8,  0 },
    							 {  0,  0,  0,  0,  0,  0,  0,  0 } };

    /** Piece/square table for queens during middle game. */
    static final int[][] qt1 = { {-10, -5,  0,  0,  0,  0, -5,-10 },
				 				 { -5,  0,  5,  5,  5,  5,  0, -5 },
				 				 {  0,  5,  5,  6,  6,  5,  5,  0 },
				 				 {  0,  5,  6,  6,  6,  6,  5,  0 },
				 				 {  0,  5,  6,  6,  6,  6,  5,  0 },
				 				 {  0,  5,  5,  6,  6,  5,  5,  0 },
				 				 { -5,  0,  5,  5,  5,  5,  0, -5 },
				 				 {-10, -5,  0,  0,  0,  0, -5,-10 } };

    /** Piece/square table for queens during middle game. */
    static final int[][] rt1 = { {  0,  1,  2,  2,  2,  2,  1,  0 },
				 				 { 10, 15, 15, 15, 15, 15, 15, 10 },
				 				 {  0,  0,  0,  0,  0,  0,  0,  0 },
				 				 {  0,  0,  0,  0,  0,  0,  0,  0 },
				 				 { -1,  0,  0,  0,  0,  0,  0, -1 },
				 				 { -2,  0,  0,  0,  0,  0,  0, -2 },
				 				 { -3,  2,  5,  5,  5,  5,  2, -3 },
				 				 {  0,  3,  5,  5,  5,  5,  3,  0 } };
    
    static final int[][] distToH1A8 = { { 0, 1, 2, 3, 4, 5, 6, 7 },
										{ 1, 2, 3, 4, 5, 6, 7, 6 },
										{ 2, 3, 4, 5, 6, 7, 6, 5 },
										{ 3, 4, 5, 6, 7, 6, 5, 4 },
										{ 4, 5, 6, 7, 6, 5, 4, 3 },
										{ 5, 6, 7, 6, 5, 4, 3, 2 },
										{ 6, 7, 6, 5, 4, 3, 2, 1 },
										{ 7, 6, 5, 4, 3, 2, 1, 0 } };

    private static final class PawnHashData {
    	PawnHashData() {
            nPawns = new byte[2][8];
    	}
    	long key;
    	byte [][] nPawns;  // nPawns[0/1][file] contains the number of white/black pawns on a file.
    	int score;         // Positive score means good for white
    	int passedBonusW;
    	int passedBonusB;
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
    
    /** Constructor. */
    public Evaluate() {
        firstPawn = new int[2][8];
        pieces = new int[Piece.nPieceTypes][64];
        nPieces = new int[Piece.nPieceTypes];
    }

    /**
     * Static evaluation of a position.
     * @param pos The position to evaluate.
     * @return The evaluation score, measured in centipawns.
     *         Positive values are good for the side to make the next move.
     */
    final public int evalPos(Position pos) {
    	int score = pos.wMtrl - pos.bMtrl;

        score += pieceSquareEval(pos);
        score += pawnBonus(pos);
        score += tradeBonus(pos);
        score += castleBonus(pos);

        score += rookBonus(pos);
        score += kingSafety(pos);
        score += bishopEval(pos, score);
        score = endGameEval(pos, score);

        if (!pos.whiteMove)
            score = -score;
        return score;
    }

    /** Compute white_material - black_material. */
    static final int material(Position pos) {
        return pos.wMtrl - pos.bMtrl;
    }
    
    static final int material(Position pos, boolean white) {
        return white ? pos.wMtrl : pos.bMtrl;
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

        // White/black pawn scores. All scores are summed first, then a 
        // single interpolation gives the final score.
        int wp1 = 0;
        int wp2 = 0;
        int bp1 = 0;
        int bp2 = 0;

        for (int p = 0; p < Piece.nPieceTypes; p++)
        	nPieces[p] = 0;
        for (int sq = 0; sq < 64; sq++) {
            int p = pos.getPiece(sq);
            if (p == Piece.EMPTY) continue;
            pieces[p][nPieces[p]++] = sq;
            final int x = Position.getX(sq);
            final int y = Position.getY(sq);
            switch (p) {
            case Piece.WKING:
            {
            	final int k1 = kt1[7-y][x];
            	final int k2 = kt2[7-y][x];
            	final int t1 = qV + 2 * rV + 2 * bV;
            	final int t2 = rV;
            	final int t = bMtrl - bMtrlPawns;
            	final int s = interpolate(t, t2, k2, t1, k1);
            	score += s;
            	break;
            }
            case Piece.BKING:
            {
            	final int k1 = kt1[y][x];
            	final int k2 = kt2[y][x];
            	final int t1 = qV + 2 * rV + 2 * bV;
            	final int t2 = rV;
            	final int t = wMtrl - wMtrlPawns;
            	final int s = interpolate(t, t2, k2, t1, k1);
            	score -= s;
            	break;
            }
            case Piece.WPAWN:
            {
            	wp1 += pt1[7-y][x];
            	wp2 += pt2[7-y][x];
            	break;
            }
            case Piece.BPAWN:
            {
            	bp1 += pt1[y][x];
            	bp2 += pt2[y][x];
            	break;
            }
            case Piece.WKNIGHT:
            {
            	final int n1 = nt1[7-y][x];
            	final int n2 = nt2[7-y][x];
            	final int t1 = qV + 2 * rV + 1 * bV + 1 * nV + 6 * pV;
            	final int t2 = nV + 8 * pV;
            	final int t = bMtrl;
            	final int s = interpolate(t, t2, n2, t1, n1);
            	score += s;
            	break;
            }
            case Piece.BKNIGHT:
            {
            	final int n1 = nt1[y][x];
            	final int n2 = nt2[y][x];
            	final int t1 = qV + 2 * rV + 1 * bV + 1 * nV + 6 * pV;
            	final int t2 = nV + 8 * pV;
            	final int t = wMtrl;
            	final int s = interpolate(t, t2, n2, t1, n1);
            	score -= s;
            	break;
            }
            case Piece.WBISHOP:
            {
            	score += bt1[7-y][x];
            	break;
            }
            case Piece.BBISHOP:
            {
            	score -= bt1[y][x];
            	break;
            }
            case Piece.WQUEEN:
            {
            	score += qt1[7-y][x];
            	score += rookMobility(pos, x, y, sq);
            	score += bishopMobility(pos, x, y, sq);
            	break;
            }
            case Piece.BQUEEN:
            {
            	score -= qt1[y][x];
            	score -= rookMobility(pos, x, y, sq);
            	score -= bishopMobility(pos, x, y, sq);
            	break;
            }
            case Piece.WROOK:
            {
            	final int r1 = rt1[7-y][x];
            	final int nP = bMtrlPawns / pV;
            	final int s = r1 * Math.min(nP, 6) / 6;
            	score += s;
            	break;
            }
            case Piece.BROOK:
            {
            	final int r1 = rt1[y][x];
            	final int nP = wMtrlPawns / pV;
            	final int s = r1 * Math.min(nP, 6) / 6;
            	score -= s;
            	break;
            }
            }
        }
        {
        	final int t1 = qV + 2 * rV + 2 * bV;
        	final int t2 = rV;
        	final int tw = bMtrl - bMtrlPawns;
        	score += interpolate(tw, t2, wp2, t1, wp1) / 2;
        	final int tb = wMtrl - wMtrlPawns;
        	score -= interpolate(tb, t2, bp2, t1, bp1) / 2;
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
    final int castleBonus(Position pos) {
    	if (pos.getCastleMask() == 0) return 0;
        final int qV = pieceValue[Piece.WQUEEN];
        final int rV = pieceValue[Piece.WROOK];
        final int bV = pieceValue[Piece.WBISHOP];

        final int k1 = kt1[7-0][6] - kt1[7-0][4];
        final int k2 = kt2[7-0][6] - kt2[7-0][4];
        final int t1 = qV + 2 * rV + 2 * bV;
        final int t2 = rV;
        final int t = pos.bMtrl - pos.bMtrlPawns;
        final int ks = interpolate(t, t2, k2, t1, k1);

        final int castleValue = ks + rt1[7-0][5] - rt1[7-0][7];
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

    final int pawnBonus(Position pos) {
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
    final void computePawnHashData(Position pos, PawnHashData ph) {
    	byte[][] nPawns = ph.nPawns;
        for (int x = 0; x < 8; x++) {
            nPawns[0][x] = 0;
            nPawns[1][x] = 0;
            firstPawn[0][x] = 7;
            firstPawn[1][x] = 0;
        }

        for (int sq = 0; sq < 64; sq++) {
        	int p = pos.getPiece(sq);
        	switch (p) {
        	case Piece.WPAWN: {
        		int x = Position.getX(sq);
        		int y = Position.getY(sq);
                nPawns[0][x]++;
            	firstPawn[0][x] = Math.min(firstPawn[0][x], y);
            	break;
        	}
        	case Piece.BPAWN: {
        		int x = Position.getX(sq);
        		int y = Position.getY(sq);
            	nPawns[1][x]++;
            	firstPawn[1][x] = Math.max(firstPawn[1][x], y);
            	break;
        	}
        	}
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
        		}
        	}
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
        		}
            }
        }

        ph.key = pos.pawnZobristHash();
        ph.score = score;
        ph.passedBonusW = passedBonusW;
        ph.passedBonusB = passedBonusB;
    }
    
    /** Compute rook bonus. Rook on open/half-open file. */
    final int rookBonus(Position pos) {
        int score = 0;
        int nP = nPieces[Piece.WROOK];
        for (int i = 0; i < nP; i++) {
        	int sq = pieces[Piece.WROOK][i];
            final int x = Position.getX(sq);
            final int y = Position.getY(sq);
            if (ph.nPawns[0][x] == 0) { // At least half-open file
                score += ph.nPawns[1][x] == 0 ? 25 : 12;
            }
            score += rookMobility(pos, x, y, sq) / 2;
        }
        nP = nPieces[Piece.BROOK];
        for (int i = 0; i < nP; i++) {
        	int sq = pieces[Piece.BROOK][i];
            final int x = Position.getX(sq);
            final int y = Position.getY(sq);
            if (ph.nPawns[1][x] == 0) {
                score -= ph.nPawns[0][x] == 0 ? 25 : 12;
            }
            score -= rookMobility(pos, x, y, sq) / 2;
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
                	if (p == ownPawn) safety += 2; else if (p == otherPawn) safety -= 2;
                	p = pos.getPiece(yb + x + 2 * yd);
                	if (p == ownPawn) safety += 1; else if (p == otherPawn) safety -= 2;
                	p = pos.getPiece(yb + x + 3 * yd);
                	if (p == otherPawn) safety -= 1;
                	if (ph.nPawns[1-i][x] == 0) halfOpenFiles++;
                }
                safety = Math.min(safety, 6);
            }
            final int kSafety = (safety - 6) * 15 - halfOpenFiles * 20;
            if (white) {
                score += kSafety;
            } else {
                score -= kSafety;
            }
        }
        final int kSafety = interpolate(m, minM, 0, maxM, score);
        return kSafety;
    }

    /** Compute bishop evaluation. */
    private final int bishopEval(Position pos, int oldScore) {
        int score = 0;
        boolean whiteDark = false;
        boolean whiteLight = false;
        boolean blackDark = false;
        boolean blackLight = false;
        int nP = nPieces[Piece.WBISHOP];
        for (int i = 0; i < nP; i++) {
        	int sq = pieces[Piece.WBISHOP][i];
        	final int x = Position.getX(sq);
        	final int y = Position.getY(sq);
        	if (Position.darkSquare(x, y))
        		whiteDark = true;
        	else
        		whiteLight = true;
        	score += bishopMobility(pos, x, y, sq) * 2;
        }
        nP = nPieces[Piece.BBISHOP];
        for (int i = 0; i < nP; i++) {
        	int sq = pieces[Piece.BBISHOP][i];
        	final int x = Position.getX(sq);
        	final int y = Position.getY(sq);
        	if (Position.darkSquare(x, y))
        		blackDark = true;
        	else
        		blackLight = true;
        	score -= bishopMobility(pos, x, y, sq) * 2;
        }
        int numWhite = (whiteDark ? 1 : 0) + (whiteLight ? 1 : 0);
        int numBlack = (blackDark ? 1 : 0) + (blackLight ? 1 : 0);
        
        // Bishop pair bonus
        final int pV = pieceValue[Piece.WPAWN];
        if (numWhite == 2) {
            final int numPawns = pos.wMtrlPawns / pV;
            score += 20 + (8 - numPawns) * 2;
        }
        if (numBlack == 2) {
            final int numPawns = pos.bMtrlPawns / pV;
            score -= 20 + (8 - numPawns) * 2;
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
    final static int bishopMobility(Position pos, int x0, int y0, int sq0) {
        int mobility = 0;
        mobility += dirMobility(pos, sq0, Math.min(  x0,   y0), -9);
        mobility += dirMobility(pos, sq0, Math.min(  x0, 7-y0),  7);
        mobility += dirMobility(pos, sq0, Math.min(7-x0,   y0), -7);
        mobility += dirMobility(pos, sq0, Math.min(7-x0, 7-y0),  9);
        return mobility;
    }

    /** Count the number of pseudo-legal moves for a rook of given color on square (x0,y0). */
    final static int rookMobility(Position pos, int x0, int y0, int sq0) {
        int mobility = 0;
        mobility += dirMobility(pos, sq0,   x0, -1);
        mobility += dirMobility(pos, sq0, 7-x0,  1);
        mobility += dirMobility(pos, sq0,   y0, -8);
        mobility += dirMobility(pos, sq0, 7-y0,  8);
        return mobility;
    }

    private static final int dirMobility(Position pos, int sq, int loops, int delta) {
        int mobility = 0;
        while (loops > 0) {
        	sq += delta;
        	int p = pos.getPiece(sq);
        	if (p != Piece.EMPTY)
        		break;
       		mobility++;
        	loops--;
        }
        return mobility;
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
            if (bMtrlPawns == 0) {
                if (wMtrlNoPawns - bMtrlNoPawns > bV) {
                    int wKnights = pos.nPieces(Piece.WKNIGHT);
                    int wBishops = pos.nPieces(Piece.WBISHOP);
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
                }
            }
        }
        if (!handled) {
            if ((score > 0) && (wMtrlPawns == 0) && (wMtrlNoPawns <= bMtrlNoPawns + bV)) {
                score /= 8;         // Too little excess material, probably draw
                handled = true;
            }
        }
        if (!handled) {
            if (wMtrlPawns == 0) {
                if (bMtrlNoPawns - wMtrlNoPawns > bV) {
                    int bKnights = pos.nPieces(Piece.BKNIGHT);
                    int bBishops = pos.nPieces(Piece.BBISHOP);
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
                }
            }
        }
        if (!handled) {
            if ((score < 0) && (bMtrlPawns == 0) && (bMtrlNoPawns <= wMtrlNoPawns + bV)) {
                score /= 8;         // Too little excess material, probably draw
                handled = true;
            }
        }

        // FIXME!!! Implement end game knowledge or EGTB for kpk
        // FIXME!!! Bishop + a|h pawn is draw if bad bishop and other king controls promotion square
        return score;
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
    static final int interpolate(int x, int x1, int y1, int x2, int y2) {
    	if (x > x2) {
    		return y2;
    	} else if (x < x1) {
            return y1;
        } else {
            return (x - x1) * (y2 - y1) / (x2 - x1) + y1;
        }
    }
}
