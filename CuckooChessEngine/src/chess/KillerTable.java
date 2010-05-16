/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package chess;

import java.util.ArrayList;

/**
 * Implement a table of killer moves for the killer heuristic.
 * @author petero
 */
public class KillerTable {
    /** There is one KTEntry for each ply in the search tree. */
    static final class KTEntry {
        public KTEntry() {
            ent = new Move[2];
            int sq = Position.getSquare(0, 0);
            ent[0] = new Move(sq, sq, Piece.EMPTY);
            ent[1] = new Move(sq, sq, Piece.EMPTY);
        }
        Move[] ent;
    }
    ArrayList<KTEntry> ktList;

    /** Create an empty killer table. */
    public KillerTable() {
        ktList = new ArrayList<KTEntry>(0);
    }
    
    /** Add a killer move to the table. Moves are replaced on an LRU basis. */
    final public void addKiller(int ply, Move m) {
        while (ktList.size() <= ply) {
            ktList.add(new KTEntry());
        }
        KTEntry ent = ktList.get(ply);
        if (!m.equals(ent.ent[0])) {
            Move tmp = ent.ent[1];
            ent.ent[1] = ent.ent[0];
            ent.ent[0] = tmp;
            tmp.from = m.from;
            tmp.to = m.to;
            tmp.promoteTo = m.promoteTo;
            tmp.score = m.score;
        }
    }

    /**
     * Get a score for move m based on hits in the killer table.
     * The score is 4 for primary   hit at ply.
     * The score is 3 for secondary hit at ply.
     * The score is 2 for primary   hit at ply - 2.
     * The score is 1 for secondary hit at ply - 2.
     * The score is 0 otherwise.
     */
    final public int getKillerScore(int ply, Move m) {
        if (ply < ktList.size()) {
            KTEntry ent = ktList.get(ply);
            if (m.equals(ent.ent[0])) {
                return 4;
            } else if (m.equals(ent.ent[1])) {
                return 3;
            }
        }
        if ((ply - 2 >= 0) && (ply - 2 < ktList.size())) {
            KTEntry ent = ktList.get(ply - 2);
            if (m.equals(ent.ent[0])) {
                return 2;
            } else if (m.equals(ent.ent[1])) {
                return 1;
            }
        }
        return 0;
    }
}
