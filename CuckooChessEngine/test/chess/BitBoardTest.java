package chess;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BitBoardTest {
    public BitBoardTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /** Test of kingAttacks, of class BitBoard. */
    @Test
    public void testKingAttacks() throws ChessParseError {
        System.out.println("kingAttacks");
        assertEquals(5, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("g1")]));
        assertEquals(3, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("h1")]));
        assertEquals(3, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("a1")]));
        assertEquals(5, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("a2")]));
        assertEquals(3, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("h8")]));
        assertEquals(5, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("a6")]));
        assertEquals(8, Long.bitCount(BitBoard.kingAttacks[TextIO.getSquare("b2")]));
    }

    /** Test of knightAttacks, of class BitBoard. */
    @Test
    public void testKnightAttacks() throws ChessParseError {
        System.out.println("knightAttacks");
        assertEquals(3, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("g1")]));
        assertEquals(2, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("a1")]));
        assertEquals(2, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("h1")]));
        assertEquals(4, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("h6")]));
        assertEquals(4, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("b7")]));
        assertEquals(8, Long.bitCount(BitBoard.knightAttacks[TextIO.getSquare("c6")]));
        assertEquals((1L<<TextIO.getSquare("e2")) |
                     (1L<<TextIO.getSquare("f3")) |
                     (1L<<TextIO.getSquare("h3")),
                     BitBoard.knightAttacks[TextIO.getSquare("g1")]);
    }
    
    /** Test of squaresBetween[][], of class BitBoard. */
    @Test
    public void testSquaresBetween() throws ChessParseError {
        System.out.println("squaresBetween");
        // Tests that the set of nonzero elements is correct
        for (int sq1 = 0; sq1 < 64; sq1++) {
            for (int sq2 = 0; sq2 < 64; sq2++) {
                int d = MoveGen.getDirection(sq1, sq2);
                if (d == 0) {
                    assertEquals(0, BitBoard.squaresBetween[sq1][sq2]);
                } else {
                    int dx = Position.getX(sq1) - Position.getX(sq2);
                    int dy = Position.getY(sq1) - Position.getY(sq2);
                    if (Math.abs(dx * dy) == 2) { // Knight direction
                        assertEquals(0, BitBoard.squaresBetween[sq1][sq2]);
                    } else {
                        if ((Math.abs(dx) > 1) || (Math.abs(dy) > 1)) {
                            assertTrue(BitBoard.squaresBetween[sq1][sq2] != 0);
                        } else {
                            assertTrue(BitBoard.squaresBetween[sq1][sq2] == 0);
                        }
                    }
                }
            }
        }

        assertEquals(0x0040201008040200L, BitBoard.squaresBetween[0][63]);
        assertEquals(0x000000001C000000L, BitBoard.squaresBetween[TextIO.getSquare("b4")][TextIO.getSquare("f4")]);
    }

    @Test
    public void testTrailingZeros() {
        for (int i = 0; i < 64; i++) {
            long mask = 1L << i;
            assertEquals(i, BitBoard.numberOfTrailingZeros(mask));
        }
    }
}
