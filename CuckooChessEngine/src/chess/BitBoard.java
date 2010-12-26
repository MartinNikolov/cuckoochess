package chess;

public class BitBoard {

    /** Squares attacked by a king on a given square. */
    static long[] kingAttacks;
    static long[] knightAttacks;

    static final long maskAToGFiles = 0x7F7F7F7F7F7F7F7FL;
    static final long maskBToHFiles = 0xFEFEFEFEFEFEFEFEL;
    static final long maskAToFFiles = 0x3F3F3F3F3F3F3F3FL;
    static final long maskCToHFiles = 0xFCFCFCFCFCFCFCFCL;

    static final long maskRow1      = 0x00000000000000FFL;
    static final long maskRow2      = 0x000000000000FF00L;
    static final long maskRow3      = 0x0000000000FF0000L;
    static final long maskRow4      = 0x00000000FF000000L;
    static final long maskRow5      = 0x000000FF00000000L;
    static final long maskRow6      = 0x0000FF0000000000L;
    static final long maskRow7      = 0x00FF000000000000L;
    static final long maskRow8      = 0xFF00000000000000L;
    static final long maskRow1Row8  = 0xFF000000000000FFL;

    static final long maskDarkSq    = 0xAA55AA55AA55AA55L;
    static final long maskLightSq   = 0x55AA55AA55AA55AAL;

    static {
        // Compute king attacks
        kingAttacks = new long[64];

        for (int sq = 0; sq < 64; sq++) {
            long m = 1L << sq;
            long mask = (((m >>> 1) | (m << 7) | (m >>> 9)) & maskAToGFiles) |
                        (((m <<  1) | (m << 9) | (m >>> 7)) & maskBToHFiles) |
                        (m << 8) | (m >>> 8);
            kingAttacks[sq] = mask;
        }

        // Compute knight attacks
        knightAttacks = new long[64];
        for (int sq = 0; sq < 64; sq++) {
            long m = 1L << sq;
            long mask = (((m <<  6) | (m >>> 10)) & maskAToFFiles) |
                        (((m << 15) | (m >>> 17)) & maskAToGFiles) |
                        (((m << 17) | (m >>> 15)) & maskBToHFiles) |
                        (((m << 10) | (m >>>  6)) & maskCToHFiles);
            knightAttacks[sq] = mask;
        }
    }
}
