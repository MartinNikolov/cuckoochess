package org.petero.droidfish.gamelogic;

import java.util.ArrayList;
import java.util.List;

public class TimeControl {
	private long timeControl;
	private int movesPerSession;
	private long increment;

	List<Long> whiteTimes; // Remaining time before making move "n".
	List<Long> blackTimes;
	int currentMove;
	boolean whiteToMove;

	long elapsed; // Accumulated elapsed time for this move.
	long timerT0; // Time when timer started. 0 if timer is stopped.


	/** Constructor. Sets time control to "game in 5min". */
	public TimeControl() {
		setTimeControl(5 * 60 * 1000, 0, 0);
		reset();
	}

	public final void reset() {
		whiteTimes = new ArrayList<Long>(0);
		blackTimes = new ArrayList<Long>(0);
		currentMove = 1;
		whiteToMove = true;
		elapsed = 0;
		timerT0 = 0;
	}

	/** Set time control to "moves" moves in "time" milliseconds, + inc milliseconds per move. */
	public final void setTimeControl(long time, int moves, long inc) {
		timeControl = time;
		movesPerSession = moves;
		increment = inc;
	}

	public final void setCurrentMove(int move, boolean whiteToMove) {
		currentMove = move;
		this.whiteToMove = whiteToMove;
		timerT0 = 0;
		elapsed = 0;
	}

	public final boolean clockRunning() {
		return timerT0 != 0;
	}

	public final void startTimer(long now) {
		if (!clockRunning()) {
			timerT0 = now;
		}
	}

	public final void stopTimer(long now) {
		if (clockRunning()) {
			long timerT1 = now;
			long currElapsed = timerT1 - timerT0;
			timerT0 = 0;
			if (currElapsed > 0) {
				elapsed += currElapsed;
			}
		}
	}

	/** Update remaining time when a move is made. */
	public final void moveMade(long now) {
		stopTimer(now);
		long remaining = getRemainingTime(whiteToMove, now);
		long prevRemaining = remaining + elapsed;
		remaining += increment;
		if (getMovesToTC() == 1) {
			remaining += timeControl;
		}
		List<Long> times = whiteToMove ? whiteTimes : blackTimes;
		while (times.size() < currentMove + 2)
			times.add(prevRemaining);
		while (times.size() > currentMove + 2)
			times.remove(times.size() - 1);
		times.set(currentMove + 1, remaining);
		elapsed = 0;
	}

	/** Get remaining time */
	public final long getRemainingTime(boolean whiteToMove, long now) {
		List<Long> times = whiteToMove ? whiteTimes : blackTimes;
		long remaining;
		int idx = Math.max(currentMove, 1);
		if (whiteToMove && !this.whiteToMove)
			idx++;
		if (times.size() == 0) {
			remaining = timeControl; // No moves made, use initial time
		} else if (idx < times.size()) {
			remaining = times.get(idx); // Take time from undo history
		} else {
			remaining = times.get(times.size() - 1); // Take last known time
		}
		if (whiteToMove == this.whiteToMove) { 
			remaining -= elapsed;
			if (timerT0 != 0) {
				remaining -= now - timerT0;
			}
		}
		return remaining;
	}

	public final long getIncrement() {
		return increment;
	}
	
	public final int getMovesToTC() {
		if (movesPerSession <= 0)
			return 0;
		int nextTC = 1;
		while (nextTC <= currentMove)
			nextTC += movesPerSession;
		return nextTC - currentMove;
	}

	public final String saveState() {
		StringBuilder ret = new StringBuilder(4096);
		ret.append(timeControl); ret.append(' ');
		ret.append(movesPerSession); ret.append(' ');
		ret.append(increment); ret.append(' ');
		ret.append(whiteTimes.size()); ret.append(' ');
		for (int i = 0; i < whiteTimes.size(); i++) {
			ret.append(whiteTimes.get(i)); ret.append(' ');
		}
		ret.append(blackTimes.size()); ret.append(' ');
		for (int i = 0; i < blackTimes.size(); i++) {
			ret.append(blackTimes.get(i)); ret.append(' ');
		}
		ret.append(currentMove); ret.append(' ');
		ret.append(whiteToMove ? 1 : 0); ret.append(' ');
		ret.append(elapsed); ret.append(' ');
		ret.append(timerT0);
		return ret.toString();
	}

	public final void restoreState(String state) {
		String[] tokens = state.split(" ");
		try {
			int idx = 0;
			timeControl = Long.parseLong(tokens[idx++]);
			movesPerSession = Integer.parseInt(tokens[idx++]);
			increment = Long.parseLong(tokens[idx++]);
			int len = Integer.parseInt(tokens[idx++]);
			whiteTimes.clear();
			for (int i = 0; i < len; i++)
				whiteTimes.add(Long.parseLong(tokens[idx++]));
			len = Integer.parseInt(tokens[idx++]);
			blackTimes.clear();
			for (int i = 0; i < len; i++)
				blackTimes.add(Long.parseLong(tokens[idx++]));
			currentMove = Integer.parseInt(tokens[idx++]);
			whiteToMove = (Integer.parseInt(tokens[idx++]) != 0);
			elapsed = Long.parseLong(tokens[idx++]);
			timerT0 = Long.parseLong(tokens[idx++]);
		} catch (NumberFormatException nfe) {
        } catch (ArrayIndexOutOfBoundsException aioob) {
        }
	}
}
