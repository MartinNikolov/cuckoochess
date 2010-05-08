/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

/**
 * Exception class to represent parse errors in FEN or algebraic notation.
 * @author petero
 */
public class ChessParseError extends Exception {
    public ChessParseError() {
    }
    public ChessParseError(String msg) {
        super(msg);
    }
}
