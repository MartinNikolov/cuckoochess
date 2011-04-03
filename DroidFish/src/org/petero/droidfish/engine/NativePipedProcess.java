package org.petero.droidfish.engine;

public class NativePipedProcess implements UCIEngine {
	static {
		System.loadLibrary("jni");
	}

	private boolean processAlive;

	NativePipedProcess() {
		processAlive = false;
	}

	/** Start process. */
    @Override
	public final void initialize() {
		if (!processAlive) {
			startProcess();
			processAlive = true;
		}
	}

	/** Shut down process. */
    @Override
	public final void shutDown() {
		if (processAlive) {
			writeLineToEngine("quit");
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
    @Override
	public final String readLineFromEngine(int timeoutMillis) {
		String ret = readFromProcess(timeoutMillis);
		if (ret == null)
			return null;
		if (ret.length() > 0) {
//			System.out.printf("Engine -> GUI: %s\n", ret);
		}
		return ret;
	}

	/** Write a line to the process. \n will be added automatically. */
    @Override
	public final synchronized void writeLineToEngine(String data) {
//		System.out.printf("GUI -> Engine: %s\n", data);
		writeToProcess(data + "\n");
	}

	/** Start the child process. */
	private final native void startProcess();

	/**
	 * Read a line of data from the process.
	 * Return as soon as there is a full line of data to return, 
	 * or when timeoutMillis milliseconds have passed.
	 */
	private final native String readFromProcess(int timeoutMillis);

	/** Write data to the process. */
	private final native void writeToProcess(String data);
}
