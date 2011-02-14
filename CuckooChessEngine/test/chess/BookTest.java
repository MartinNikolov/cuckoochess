/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author petero
 */
public class BookTest {

    public BookTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /**
     * Test of getBookMove method, of class Book.
     */
    @Test
    public void testGetBookMove() throws ChessParseError {
        System.out.println("getBookMove");
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        Book book = new Book(true);
        Move move = book.getBookMove(pos);
        checkValid(pos, move);
    }

    /**
     * Test of getAllBookMoves method, of class Book.
     */
    @Test
    public void testGetAllBookMoves() throws ChessParseError {
        System.out.println("getAllBookMoves");
        Position pos = TextIO.readFEN(TextIO.startPosFEN);
        Book book = new Book(true);
        String moveListString = book.getAllBookMoves(pos);
        String[] strMoves = moveListString.split("\\([0-9]*\\) ");
        assertTrue(strMoves.length > 1);
        for (String strMove : strMoves) {
            Move m = TextIO.stringToMove(pos, strMove);
            checkValid(pos, m);
        }
    }

    /** Check that move is a legal move in position pos. */
    private void checkValid(Position pos, Move move) {
        assertTrue(move != null);
        Move[] moveList = new MoveGen().pseudoLegalMoves(pos);
        moveList = MoveGen.removeIllegal(pos, moveList);
        assertTrue(Arrays.asList(moveList).contains(move));
    }
}