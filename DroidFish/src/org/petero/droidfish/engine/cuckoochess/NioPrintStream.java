package org.petero.droidfish.engine.cuckoochess;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

/** Simple PrintStream look-alike on top of nio. */
class NioPrintStream {
    Pipe.SinkChannel out;

    public NioPrintStream(Pipe pipe) {
        out = pipe.sink();
    }

    public void printf(String format) {
        try {
            String s = String.format(format, new Object[]{});
            out.write(ByteBuffer.wrap(s.getBytes()));
        } catch (IOException e) {
        }
    }

    public void printf(String format, Object ... args) {
        try {
            String s = String.format(format, args);
            out.write(ByteBuffer.wrap(s.getBytes()));
        } catch (IOException e) {
        }
    }
}
