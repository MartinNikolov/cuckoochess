/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class HistoryTest {

    public HistoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getHistScore method, of class History.
     */
    @Test
    public void testGetHistScore() throws ChessParseError {
        System.out.println("getHistScore");
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        History hs = new History();
        Move m1 = TextIO.stringToMove(pos, "e4");
        Move m2 = TextIO.stringToMove(pos, "d4");
        assertEquals(0, hs.getHistScore(pos, m1));

        hs.addSuccess(pos, m1, 1);
        assertEquals(1 * 49 / 1, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addSuccess(pos, m1, 1);
        assertEquals(1 * 49 / 1, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addFail(pos, m1, 1);
        assertEquals(2 * 49 / 3, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addFail(pos, m1, 1);
        assertEquals(2 * 49 / 4, hs.getHistScore(pos, m1));
        assertEquals(0, hs.getHistScore(pos, m2));

        hs.addSuccess(pos, m2, 1);
        assertEquals(2 * 49 / 4, hs.getHistScore(pos, m1));
        assertEquals(1 * 49 / 1, hs.getHistScore(pos, m2));
    }
}
