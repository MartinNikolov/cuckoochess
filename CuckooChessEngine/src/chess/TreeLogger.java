package chess;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Collections;

import chess.TranspositionTable.TTEntry;

public final class TreeLogger {
    private byte[] entryBuffer = new byte[16];
    private ByteBuffer bb = ByteBuffer.wrap(entryBuffer);
    
    // Used in write mode
    private FileOutputStream os = null;
    private long nextIndex = 0;

    // Used in analyze mode
    private MappedByteBuffer mapBuf = null;
    private FileChannel fc = null;
    private int numEntries = 0;

    private TreeLogger() {
    }

    /** Get a logger object set up for writing to a log file. */
    public static TreeLogger getWriter(String filename, Position pos) {
        try {
            TreeLogger log = new TreeLogger();
            log.os = new FileOutputStream(filename);
            log.writeHeader(pos);
            log.nextIndex = 0;
            return log;
        } catch (FileNotFoundException e) {
            throw new RuntimeException();
        }
    }

    private void writeHeader(Position pos) {
        try {
            byte[] fen = TextIO.toFEN(pos).getBytes();
            os.write((byte)(fen.length));
            os.write(fen);
            byte[] pad = new byte[128-1-fen.length];
            for (int i = 0; i < pad.length; i++)
                pad[i] = 0;
            os.write(pad);
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    /** Get a logger object set up for analyzing a log file. */
    public static TreeLogger getAnalyzer(String filename) {
        try {
            TreeLogger log = new TreeLogger();
            RandomAccessFile raf;
            raf = new RandomAccessFile(filename, "rw");
            log.fc = raf.getChannel();
            long len = raf.length();
            log.numEntries = (int) ((len - 128) / 16);
            log.mapBuf = log.fc.map(MapMode.READ_WRITE, 0, len);
            log.computeForwardPointers();
            return log;
        } catch (FileNotFoundException e) {
            throw new RuntimeException();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public void close() {
        try {
            if (os != null) os.close();
            if (fc != null) fc.close();
        } catch (IOException e) {
        }
    }

    /* This is the on-disk format. Big-endian byte-order is used.
     * First there is one header entry. Then there is a set of start/end entries.
     * A StartEntry can be identified by its first 4 bytes (endIndex/startIndex)
     * being either -1 (endIndex not computed), or > the entry index.
     *
     * private static class Header {
     *     byte fenLen; // Used length of fen array
     *     byte[] fen; // 126 bytes, 0-padded
     *     byte flags; // bit 0: 1 if endIndex has been computed for all StartEntries.
     * }
     *
     * private static class StartEntry {
     *     int endIndex;
     *     int parentIndex;                 // -1 for root node
     *     short move;
     *     short alpha;
     *     short beta;
     *     byte ply;
     *     byte depth;
     * }
     *
     * private static class EndEntry {
     *     int startIndex;
     *     short score;
     *     short scoreType;
     *     short evalScore;
     *     byte[] padding; // 6 bytes
     * }
     */

    // ----------------------------------------------------------------------------
    // Functions used for tree logging

    /** 
     * Log information when entering a search node.
     * @param parentId     Index of parent node.
     * @param m            Move made to go from parent node to this node
     * @param alpha        Search parameter
     * @param beta         Search parameter
     * @param ply          Search parameter
     * @param depth        Search parameter
     * @return node index
     */
    long logNodeStart(long parentIndex, Move m, int alpha, int beta, int ply, int depth) {
        bb.putInt  ( 0, (int)-1);
        bb.putInt  ( 4, (int)parentIndex);
        bb.putShort( 8, (short)(m.from + (m.to << 6) + (m.promoteTo << 12)));
        bb.putShort(10, (short)alpha);
        bb.putShort(12, (short)beta);
        bb.put     (14, (byte)ply);
        bb.put     (15, (byte)depth);
        try {
            os.write(bb.array());
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return nextIndex++;
    }

    /**
     * @param startIndex Pointer to corresponding start node entry.
     * @param score      Computed score for this node.
     * @param scoreType  See TranspositionTable, T_EXACT, T_GE, T_LE.
     * @param evalScore  Score returned by evaluation function at this node, if known.
     * @return node index
     */
    long logNodeEnd(long startIndex, int score, int scoreType, int evalScore) {
        bb.putInt  ( 0, (int)startIndex);
        bb.putShort( 4, (short)score);
        bb.putShort( 6, (short)scoreType);
        bb.putShort( 8, (short)evalScore);
        try {
            os.write(bb.array());
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return nextIndex++;
    }

    // ----------------------------------------------------------------------------
    // Functions used for tree analyzing
    
    private static int indexToFileOffs(int index) {
        return 128 + index * 16;
    }
    
    /** Compute endIndex for all StartNode entries. */
    private void computeForwardPointers() {
        if ((mapBuf.get(127) & (1<<7)) != 0)
            return;
        System.out.printf("Computing forward pointers...\n");
        StartEntry se = new StartEntry();
        EndEntry ee = new EndEntry();
        for (int i = 0; i < numEntries; i++) {
            boolean isStart = readEntry(i, se, ee);
            if (!isStart) {
                int offs = indexToFileOffs(ee.startIndex);
                mapBuf.putInt(offs, i);
            }
        }
        mapBuf.put(127, (byte)(1 << 7));
        mapBuf.force();
        System.out.printf("Computing forward pointers... done\n");
    }

    /** Get FEN string for root node position. */
    public String getRootNodeFEN() {
        int len = mapBuf.get(0);
        byte[] fenB = new byte[len];
        for (int i = 0; i < len; i++)
            fenB[i] = mapBuf.get(1+i);
        String ret = new String(fenB);
        return ret;
    }

    static class StartEntry {
        int endIndex;
        int parentIndex;                 // -1 for root node
        Move move;
        short alpha;
        short beta;
        byte ply;
        byte depth;
    }
    static class EndEntry {
        int startIndex;
        short score;
        short scoreType;
        short evalScore;
    }

    /** Read a start/end entry.
     * @return True if entry was a start entry, false if it was an end entry. */
    public boolean readEntry(int index, StartEntry se, EndEntry ee) {
        int offs = indexToFileOffs(index);
        for (int i = 0; i < 16; i++)
            bb.put(i, mapBuf.get(offs + i));
        int otherIndex = bb.getInt(0);
        boolean isStartEntry = (otherIndex == -1) || (otherIndex > index);
        if (isStartEntry) {
            se.endIndex = otherIndex;
            se.parentIndex = bb.getInt(4);
            int m = bb.getShort(8);
            se.move = new Move(m & 63, (m >> 6) & 63, (m >> 12) & 15);
            se.alpha = bb.getShort(10);
            se.beta = bb.getShort(12);
            se.ply = bb.get(14);
            se.depth = bb.get(15);
        } else {
            ee.startIndex = otherIndex;
            ee.score = bb.getShort(4);
            ee.scoreType = bb.getShort(6);
            ee.evalScore = bb.getShort(8);
        }
        return isStartEntry;
    }

    // ----------------------------------------------------------------------------
    // Functions used for the interactive tree browser

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.printf("Usage: progname filename\n");
            System.exit(1);
        }
        TreeLogger an = getAnalyzer(args[0]);
        try {
            Position rootPos = TextIO.readFEN(an.getRootNodeFEN());
            an.mainLoop(rootPos);
        } catch (ChessParseError e) {
            throw new RuntimeException();
        }
        an.close();
    }

    private void mainLoop(Position rootPos) throws IOException {
        int currIndex = -1;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String prevStr = "";

        while (true) {
            {
                ArrayList<Move> moves = getMoveSequence(currIndex);
                for (Move m : moves)
                    System.out.printf(" %s", TextIO.moveToUCIString(m));
                System.out.printf("\n");
                printNodeInfo(rootPos, currIndex);
                Position pos = getPosition(rootPos, currIndex);
                System.out.print(TextIO.asciiBoard(pos));
                System.out.printf("%s\n", TextIO.toFEN(pos));
                if (currIndex >= 0) {
                    ArrayList<Integer> children = findChildren(currIndex);
                    for (Integer c : children)
                        printNodeInfo(rootPos, c);
                }
            }
            System.out.printf("Command:");
            String cmdStr = in.readLine();
            if (cmdStr.length() == 0)
                cmdStr = prevStr;
            if (cmdStr.startsWith("q")) {
                return;
            } else if (cmdStr.startsWith("?")) {
                printHelp();
            } else if (cmdStr.startsWith("u")) {
                int n = getArg(cmdStr, 1);
                for (int i = 0; i < n; i++)
                    currIndex = findParent(currIndex);
            } else if (cmdStr.startsWith("l")) {
                ArrayList<Integer> children = findChildren(currIndex);
                String m = getArgStr(cmdStr, "");
                for (Integer c : children)
                    printNodeInfo(rootPos, c, m);
            } else if (cmdStr.startsWith("n")) {
                ArrayList<Integer> nodes = getNodeSequence(currIndex);
                for (int node : nodes)
                    printNodeInfo(rootPos, node);
            } else if (cmdStr.startsWith("d")) {
                ArrayList<Integer> nVec = getArgs(cmdStr, 0);
                for (int n : nVec) {
                    ArrayList<Integer> children = findChildren(currIndex);
                    if ((n >= 0) && (n < children.size())) {
                        currIndex = children.get(n);
                    } else
                        break;
                }
            } else if (cmdStr.startsWith("p")) {
                ArrayList<Move> moves = getMoveSequence(currIndex);
                for (Move m : moves)
                    System.out.printf(" %s", TextIO.moveToUCIString(m));
                System.out.printf("\n");
            } else {
                try {
                    int i = Integer.parseInt(cmdStr);
                    if ((i >= -1) && (i < numEntries))
                        currIndex = i;
                    printNodeInfo(rootPos, currIndex);
                } catch (NumberFormatException e) {
                }
            }
            prevStr = cmdStr;
        }
    }
    
    private static int getArg(String s, int defVal) {
        try {
            int idx = s.indexOf(' ');
            if (idx > 0) {
                return Integer.parseInt(s.substring(idx+1));
            }
        } catch (NumberFormatException e) {
        }
        return defVal;
    }
    
    ArrayList<Integer> getArgs(String s, int defVal) {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        String[] split = s.split(" ");
        try {
            for (int i = 1; i < split.length; i++)
                ret.add(Integer.parseInt(split[i]));
        } catch (NumberFormatException e) {
            ret.clear();
        }
        if (ret.size() == 0)
            ret.add(defVal);
        return ret;
    }

    private static String getArgStr(String s, String defVal) {
        int idx = s.indexOf(' ');
        if (idx > 0)
            return s.substring(idx+1);
        return defVal;
    }
    
    private void printHelp() {
        System.out.printf("  p              - Print move sequence\n");
        System.out.printf("  n              - Print node info corresponding to move sequence\n");
        System.out.printf("  l [move]       - List child nodes\n");
        System.out.printf("  d [n1 [n2...]] - Go to child \"n\"\n");
        System.out.printf("  u [levels]     - Move up\n");
        System.out.printf("  num            - Go to node \"num\"\n");
        System.out.printf("  q              - Quit\n");
        System.out.printf("  ?              - Print this help\n");
    }

    private boolean readEntries(int index, StartEntry se, EndEntry ee) {
        boolean isStart = readEntry(index, se, ee);
        if (isStart) {
            int eIdx = se.endIndex;
            if (eIdx >= 0) {
                readEntry(eIdx, null, ee);
            } else {
                return false;
            }
        } else {
            int sIdx = ee.startIndex;
            readEntry(sIdx, se, null);
        }
        return true;
    }

    /** Find the parent node to a node. */
    private int findParent(int index) {
        if (index >= 0) {
            StartEntry se = new StartEntry();
            EndEntry ee = new EndEntry();
            readEntries(index, se, ee);
            index = se.parentIndex;
        }
        return index;
    }

    /** Find all children to a node. */
    private ArrayList<Integer> findChildren(int index) {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        StartEntry se = new StartEntry();
        EndEntry ee = new EndEntry();
        int child = index + 1;
        while ((child >= 0) && (child < numEntries)) {
            boolean haveEE = readEntries(child, se, ee);
            if (se.parentIndex != index)
                break;
            ret.add(child);
            if (!haveEE)
                break;
            child = se.endIndex + 1;
        }
        return ret;
    }

    /** Get node position in parents children list. */
    private int getChildNo(int index) {
        ArrayList<Integer> childs = findChildren(findParent(index));
        for (int i = 0; i < childs.size(); i++)
            if (childs.get(i) == index)
                return i;
        return -1;
    }

    /** Get list of nodes from root position to a node. */
    private ArrayList<Integer> getNodeSequence(int index) {
        ArrayList<Integer> nodes = new ArrayList<Integer>();
        nodes.add(index);
        while (index >= 0) {
            index = findParent(index);
            nodes.add(index);
        }
        Collections.reverse(nodes);
        return nodes;
    }

    /** Find list of moves from root node to a node. */
    private ArrayList<Move> getMoveSequence(int index) {
        ArrayList<Move> moves = new ArrayList<Move>();
        StartEntry se = new StartEntry();
        EndEntry ee = new EndEntry();
        while (index >= 0) {
            readEntries(index, se, ee);
            moves.add(se.move);
            index = findParent(index);
        }
        Collections.reverse(moves);
        return moves;
    }

    /** Find the position corresponding to a node. */
    private Position getPosition(Position rootPos, int index) {
        ArrayList<Move> moves = getMoveSequence(index);
        Position ret = new Position(rootPos);
        UndoInfo ui = new UndoInfo();
        for (Move m : moves)
            ret.makeMove(m, ui);
        return ret;
    }

    private void printNodeInfo(Position rootPos, int index) {
        printNodeInfo(rootPos, index, "");
    }
    private void printNodeInfo(Position rootPos, int index, String filterMove) {
        if (index < 0) { // Root node
            System.out.printf("%8d entries:%d\n", index, numEntries);
        } else {
            StartEntry se = new StartEntry();
            EndEntry ee = new EndEntry();
            boolean haveEE = readEntries(index, se, ee);
            String m = TextIO.moveToUCIString(se.move);
            if ((filterMove.length() > 0) && !m.equals(filterMove))
                return;
            System.out.printf("%3d %8d %s a:%6d b:%6d p:%2d d:%2d", getChildNo(index), index,
                    m, se.alpha, se.beta, se.ply, se.depth);
            if (haveEE) {
                int subTreeNodes = (se.endIndex - ee.startIndex - 1) / 2;
                String type;
                switch (ee.scoreType) {
                case TTEntry.T_EXACT: type = "= "; break;
                case TTEntry.T_GE   : type = ">="; break;
                case TTEntry.T_LE   : type = "<="; break;
                default             : type = "  "; break;
                }
                System.out.printf(" s:%s%6d e:%6d sub:%d", type, ee.score, ee.evalScore,
                                                            subTreeNodes);
            }
            System.out.printf("\n");
        }
    }
}
