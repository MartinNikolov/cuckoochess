/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

/**
 * Implements the relative history heuristic.
 * @author petero
 */
public class History {
    private int countSuccess[][];
    private int countFail[][];
    int numUpdates;

    public History() {
        countSuccess = new int[Piece.nPieceTypes][64];
        countFail = new int[Piece.nPieceTypes][64];
        for (int p = 0; p < Piece.nPieceTypes; p++) {
            for (int sq = 0; sq < 64; sq++) {
                countSuccess[p][sq] = 0;
                countFail[p][sq] = 0;
            }
        }
        numUpdates = 0;
    }

    /** Record move as a success. */
    public final void addSuccess(Position pos, Move m, int depth) {
        int p = pos.getPiece(m.from);
        int cnt = depth;
        int val = countSuccess[p][m.to] + cnt;
        if (val > 1000) {
            val /= 2;
            countFail[p][m.to] /= 2;
        }
        countSuccess[p][m.to] = val;
    }

    /** Record move as a failure. */
    public final void addFail(Position pos, Move m, int depth) {
        int p = pos.getPiece(m.from);
        int cnt = depth;
        countFail[p][m.to] += cnt;
    }

    /** Get a score between 0 and 49, depending of the success/fail ratio of the move. */
    public final int getHistScore(Position pos, Move m) {
        int p = pos.getPiece(m.from);
        int succ = countSuccess[p][m.to];
        int fail = countFail[p][m.to];
        if (succ + fail > 0) {
            return succ * 49 / (succ + fail);
        } else {
            return 0;
        }
    }
}
