/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class KillerTableTest {

    public KillerTableTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of addKiller method, of class KillerTable.
     */
    @Test
    public void testAddKiller() {
        System.out.println("addKiller");
        KillerTable kt = new KillerTable();
        Move m = new Move(TextIO.getSquare("b1"), TextIO.getSquare("b5"), Piece.EMPTY);
        kt.addKiller(3, m);
        kt.addKiller(7, m);
        kt.addKiller(3, m);
        kt.addKiller(3, m);
    }

    /**
     * Test of getKillerScore method, of class KillerTable.
     */
    @Test
    public void testGetKillerScore() {
        System.out.println("getKillerScore");
        KillerTable kt = new KillerTable();
        Move m1 = new Move(TextIO.getSquare("b1"), TextIO.getSquare("b5"), Piece.EMPTY);
        Move m2 = new Move(TextIO.getSquare("c1"), TextIO.getSquare("d2"), Piece.EMPTY);
        Move m3 = new Move(TextIO.getSquare("e1"), TextIO.getSquare("g1"), Piece.EMPTY);
        kt.addKiller(0, m1);
        assertEquals(4, kt.getKillerScore(0, m1));
        assertEquals(0, kt.getKillerScore(0, m2));
        assertEquals(0, kt.getKillerScore(0, new Move(m2)));
        kt.addKiller(0, m1);
        assertEquals(4, kt.getKillerScore(0, m1));
        kt.addKiller(0, m2);
        assertEquals(4, kt.getKillerScore(0, m2));
        assertEquals(4, kt.getKillerScore(0, new Move(m2)));    // Must compare by value
        assertEquals(3, kt.getKillerScore(0, m1));
        kt.addKiller(0, new Move(m2));
        assertEquals(4, kt.getKillerScore(0, m2));
        assertEquals(3, kt.getKillerScore(0, m1));
        assertEquals(0, kt.getKillerScore(0, m3));
        kt.addKiller(0, m3);
        assertEquals(0, kt.getKillerScore(0, m1));
        assertEquals(3, kt.getKillerScore(0, m2));
        assertEquals(4, kt.getKillerScore(0, m3));

        assertEquals(0, kt.getKillerScore(1, m3));
        assertEquals(2, kt.getKillerScore(2, m3));
        assertEquals(0, kt.getKillerScore(3, m3));
        assertEquals(0, kt.getKillerScore(4, m3));

        kt.addKiller(2, m2);
        assertEquals(4, kt.getKillerScore(2, m2));
        assertEquals(3, kt.getKillerScore(0, m2));
    }
}
