package chess;

public class BitBoard {

    /** Squares attacked by a king on a given square. */
    static long[] kingAttacks;

    static {
        kingAttacks = new long[64];
        for (int sq = 0; sq < 64; sq++) {
            int x0 = Position.getX(sq);
            int y0 = Position.getY(sq);
            long mask = 0L;
            for (int x = x0-1; x <= x0+1; x++) {
                for (int y = y0-1; y <= y0+1; y++) {
                    if ((x >= 0) && (x < 8) && (y >= 0) && (y < 8)) {
                        if ((x == x0) && (y == y0))
                            continue;
                        mask |= 1L << Position.getSquare(x, y);
                    }
                }
            }
            kingAttacks[sq] = mask;
        }
    }
}
