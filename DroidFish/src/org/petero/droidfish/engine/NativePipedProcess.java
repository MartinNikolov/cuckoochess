package org.petero.droidfish.engine;

public class NativePipedProcess {
	static {
		System.loadLibrary("jni");
	}

	private boolean processAlive;
	private StringBuilder inBuf;

	NativePipedProcess() {
		processAlive = false;
		inBuf = new StringBuilder(4096);
	}

	/** Start process. */
	public final void initialize() {
		if (!processAlive) {
			// FIXME!!! Run compute thread with lower priority
			startProcess();
			processAlive = true;
		}
	}

	/** Shut down process. */
	public final void shutDown() {
		if (processAlive) {
			writeLineToProcess("quit");
			processAlive = false;
		}
	}

	/**
	 * Read a line from the process.
	 * @param timeoutMillis Maximum time to wait for data
	 * @return The line, without terminating newline characters,
	 *         or empty string if no data available,
	 *         or null if I/O error.
	 */
	public final String readLineFromProcess(int timeoutMillis) {
		String ret;
		do {
			while (findNewLine() < 0) {
				String s = readFromProcess(timeoutMillis);
				timeoutMillis = 0;
				if (s == null)
					return null;
				if (s.length() == 0)
					break;
				inBuf.append(s);
			}
			int idx = findNewLine();
			if (idx < 0)
				return "";
			ret = inBuf.substring(0, idx);
			inBuf.delete(0, idx+1);
		} while (ret.length() == 0);
//		System.out.printf("Engine -> GUI: %s\n", ret);
		return ret;
	}

	/** Write a line to the process. \n will be added automatically. */
	public final synchronized void writeLineToProcess(String data) {
		if (data.length() > 0) {
//			System.out.printf("GUI -> Engine: %s\n", data);
		}
		writeToProcess(data + "\n");
	}

	private int findNewLine() {
		int idx1 = inBuf.indexOf("\n");
		int idx2 = inBuf.indexOf("\r");
		if (idx1 < 0)
			return idx2;
		if (idx2 < 0)
			return idx1;
		return Math.min(idx1, idx2);
	}

	/** Start the child process. */
	private final native void startProcess();

	/**
	 * Read data from the process.
	 * Return as soon as there is some data to return, or when timeoutMillis 
	 * milliseconds have passed.
	 */
	private final native String readFromProcess(int timeoutMillis);

	/** Write data to the process. */
	private final native void writeToProcess(String data);
}
