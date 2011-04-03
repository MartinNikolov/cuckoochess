package org.petero.droidfish.engine.cuckoochess;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;

/** Simple InputStream look-alike on top of nio. */
class NioInputStream {
    Pipe.SourceChannel in;
    ByteBuffer buffer;
    Selector selector;

    ArrayList<Character> inBuf;
    StringBuilder lineBuf;

    public NioInputStream(Pipe pipe) {
        in = pipe.source();
        try {
            in.configureBlocking(false);
            selector = Selector.open();
            in.register(selector, SelectionKey.OP_READ);

            buffer = ByteBuffer.allocate(1024);
            inBuf = new ArrayList<Character>();
            lineBuf = new StringBuilder(128);
        } catch (IOException e) {
        }
    }

    public String readLine() {
        while (true) {
            String s = readLine(1000);
            if (s != null)
                return s;
        }
    }

    public String readLine(int timeoutMillis) {
        try {
            // Refill inBuf
            if (timeoutMillis < 1)
                timeoutMillis = 1;
            selector.select(timeoutMillis);
            buffer.clear();
            for (SelectionKey sk : selector.selectedKeys())
                if (sk.isValid() && sk.isReadable())
                    in.read(buffer);
            buffer.flip();
            while (buffer.position() < buffer.limit()) {
                byte b = buffer.get();
                inBuf.add((char)b);
            }

            // Extract line
            String ret = "";
            int i;
            for (i = 0; i < inBuf.size(); i++) {
                char c = inBuf.get(i);
                if (c == '\n') {
                    int newSize = inBuf.size() - i - 1;
                    for (int j = 0; j < newSize; j++)
                        inBuf.set(j, inBuf.get(j+i+1));
                    while (inBuf.size() > newSize)
                        inBuf.remove(inBuf.size() - 1);
                    ret = lineBuf.toString();
                    lineBuf = new StringBuilder(128);
                    break;
                } else {
                    lineBuf.append(c);
                }
            }
            if (i == inBuf.size())
                inBuf.clear();
            return ret;
        } catch (IOException e) {
        }
        return null;
    }
}
