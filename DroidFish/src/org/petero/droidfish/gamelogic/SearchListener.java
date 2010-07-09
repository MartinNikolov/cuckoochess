package org.petero.droidfish.gamelogic;

import java.util.ArrayList;


/**
 * Used to get various search information during search
 */
public interface SearchListener {
    public void notifyDepth(int depth);
    public void notifyCurrMove(Move m, int moveNr);
    public void notifyPV(int depth, int score, int time, int nodes, int nps,
    		boolean isMate, boolean upperBound, boolean lowerBound, ArrayList<Move> pv);
    public void notifyStats(int nodes, int nps, int time);
}
