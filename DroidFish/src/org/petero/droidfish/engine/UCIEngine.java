package org.petero.droidfish.engine;

public interface UCIEngine {

    /** Start engine. */
    public abstract void initialize();

    /** Shut down engine. */
    public abstract void shutDown();

    /**
     * Read a line from the engine.
     * @param timeoutMillis Maximum time to wait for data
     * @return The line, without terminating newline characters,
     *         or empty string if no data available,
     *         or null if I/O error.
     */
    public abstract String readLineFromEngine(int timeoutMillis);

    /** Write a line to the engine. \n will be added automatically. */
    public abstract void writeLineToEngine(String data);
}
