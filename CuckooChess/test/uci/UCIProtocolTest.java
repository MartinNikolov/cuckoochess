/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uci;

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
public class UCIProtocolTest {

    public UCIProtocolTest() {
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
     * Test of tokenize method, of class UCIProtocol.
     */
    @Test
    public void testTokenize() {
        System.out.println("tokenize");
        UCIProtocol uci = new UCIProtocol();
        String[] result = uci.tokenize("  a b   c de \t \t fgh");
        assertEquals(5, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertEquals("de", result[3]);
        assertEquals("fgh", result[4]);
    }
}
