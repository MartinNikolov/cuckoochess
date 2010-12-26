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
}
