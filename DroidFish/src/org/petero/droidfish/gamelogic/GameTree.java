package org.petero.droidfish.gamelogic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Scanner;

import org.petero.droidfish.PGNOptions;

public class GameTree {
    // Data from the seven tag roster (STR) part of the PGN standard
    String event, site, date, round, white, black;
    // Result is the last tag pair in the STR, but it is computed on demand from the game tree.

    Position startPos;
    String timeControl;

    // Non-standard tags
    static private class TagPair {
    	String tagName;
    	String tagValue;
    }
    List<TagPair> tagPairs;

    Node rootNode;
    Node currentNode;
    Position currentPos;	// Cached value. Computable from "currentNode".

    public GameTree() {
    	try {
        	setStartPos(TextIO.readFEN(TextIO.startPosFEN));
		} catch (ChessParseError e) {
		}
    }

	final void setPlayerNames(String white, String black) {
		this.white = white;
		this.black = black;
	}

	/** Set start position. Drops the whole game tree. */
	final void setStartPos(Position pos) {
    	event = "?";
    	site = "?";
    	{
    		Calendar now = GregorianCalendar.getInstance();
    		int year = now.get(Calendar.YEAR);
    		int month = now.get(Calendar.MONTH) + 1;
    		int day = now.get(Calendar.DAY_OF_MONTH);
        	date = String.format("%04d.%02d.%02d", year, month, day);
    	}
    	round = "?";
    	white = "?";
    	black = "?";
    	startPos = pos;
    	timeControl = "?";
    	tagPairs = new ArrayList<TagPair>();
    	rootNode = new Node();
    	currentNode = rootNode;
    	currentPos = new Position(startPos);
	}

	private final void addTagPair(StringBuilder sb, String tagName, String tagValue) {
		sb.append(String.format("[%s \"%s\"]\n", tagName, tagValue));
	}

    /** Export in PGN format. */ 
    public final String toPGN(String pgnResultString, PGNOptions options) { // FIXME!!! Remove pgnResultString argument
    	StringBuilder pgn = new StringBuilder();

    	// Write seven tag roster
        addTagPair(pgn, "Event",  event);
        addTagPair(pgn, "Site",   site);
        addTagPair(pgn, "Date",   date);
        addTagPair(pgn, "Round",  round);
        addTagPair(pgn, "White",  white);
        addTagPair(pgn, "Black",  black);
        addTagPair(pgn, "Result", pgnResultString);

        // Write special tag pairs
    	String fen = TextIO.toFEN(startPos);
    	if (!fen.equals(TextIO.startPosFEN)) {
    		addTagPair(pgn, "FEN", fen);
    		addTagPair(pgn, "Setup", "1");
    	}
    	if (!timeControl.equals("?"))
    		addTagPair(pgn, "TimeControl", timeControl);

    	// Write other non-standard tag pairs
    	for (int i = 0; i < tagPairs.size(); i++)
    		addTagPair(pgn, tagPairs.get(i).tagName, tagPairs.get(i).tagValue);
    	pgn.append("\n");

    	// Write moveText section
    	StringBuilder moveText = new StringBuilder(4096);
    	Node.MoveNumber mn = new Node.MoveNumber(startPos.fullMoveCounter, startPos.whiteMove);
    	Node.addPgnData(pgn, rootNode, mn.prev(), options);
    	moveText.append(' ');
    	moveText.append(pgnResultString);
    	// FIXME!!! Add line breaks
    	pgn.append(moveText.toString());
    	pgn.append("\n\n");
    	return pgn.toString();
    }

    /** Import PGN data. */ 
    public final boolean readPGN(String pgn, PGNOptions options) throws ChessParseError {
    	boolean anythingParsed = false;
    	// First pass, remove comments
    	{
    		StringBuilder out = new StringBuilder();
    		Scanner sc = new Scanner(pgn);
    		sc.useDelimiter("");
    		while (sc.hasNext()) {
    			String c = sc.next();
    			if (c.equals("{")) {
    				sc.skip("[^}]*\\}");
    			} else if (c.equals(";")) {
    				sc.skip("[^\n]*\n");
    			} else {
    				out.append(c);
    			}
    		}
    		pgn = out.toString();
    	}

    	// Parse tag section
    	Position pos = TextIO.readFEN(TextIO.startPosFEN);
    	Scanner sc = new Scanner(pgn);
    	sc.useDelimiter("\\s+");
    	while (sc.hasNext("\\[.*")) {
    		anythingParsed = true;
    		String tagName = sc.next();
    		if (tagName.length() > 1) {
    			tagName = tagName.substring(1);
    		} else {
    			tagName = sc.next();
    		}
    		String tagValue = sc.findWithinHorizon(".*\\]", 0);
    		tagValue = tagValue.trim();
    		if (tagValue.charAt(0) == '"')
    			tagValue = tagValue.substring(1);
    		if (tagValue.charAt(tagValue.length()-1) == ']')
    			tagValue = tagValue.substring(0, tagValue.length() - 1);
    		if (tagValue.charAt(tagValue.length()-1) == '"')
    			tagValue = tagValue.substring(0, tagValue.length() - 1);
    		if (tagName.equals("FEN")) {
    			pos = TextIO.readFEN(tagValue);
    		}
    	}
    	setStartPos(pos);

    	// Handle (ignore) recursive annotation variations
    	{
    		StringBuilder out = new StringBuilder();
    		sc.useDelimiter("");
    		int level = 0;
    		while (sc.hasNext()) {
    			String c = sc.next();
    			if (c.equals("(")) {
    				level++;
    			} else if (c.equals(")")) {
    				level--;
    			} else if (level == 0) {
    				out.append(c);
    			}
    		}
    		pgn = out.toString();
    	}

    	// Parse move text section
    	sc = new Scanner(pgn);
    	sc.useDelimiter("\\s+");
    	while (sc.hasNext()) {
    		String strMove = sc.next();
    		strMove = strMove.replaceFirst("\\$?[0-9]*\\.*([^?!]*)[?!]*", "$1");
    		if (strMove.length() == 0) continue;
    		int varNo = addMove(strMove, "", 0, "", "");
    		if (varNo < 0)
    			break;
    		goForward(varNo);
    		anythingParsed = true;
    	}
    	return anythingParsed;
    }

    /** Serialize to byte array. */
    public final byte[] toByteArray() {
    	try {
        	ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
        	DataOutputStream dos = new DataOutputStream(baos);
        	dos.writeUTF(event);
        	dos.writeUTF(site);
        	dos.writeUTF(date);
        	dos.writeUTF(round);
        	dos.writeUTF(white);
        	dos.writeUTF(black);
        	dos.writeUTF(TextIO.toFEN(startPos));
        	dos.writeUTF(timeControl);
        	int nTags = tagPairs.size();
        	dos.writeInt(nTags);
        	for (int i = 0; i < nTags; i++) {
        		dos.writeUTF(tagPairs.get(i).tagName);
        		dos.writeUTF(tagPairs.get(i).tagValue);
        	}
        	Node.writeToStream(dos, rootNode);
        	List<Integer> pathFromRoot = currentNode.getPathFromRoot();
        	int pathLen = pathFromRoot.size();
        	dos.writeInt(pathLen);
        	for (int i = 0; i < pathLen; i++)
        		dos.writeInt(pathFromRoot.get(i));
			dos.flush();
	    	dos.close();
	    	byte[] ret = baos.toByteArray();
	    	baos.close();
	    	return ret;
		} catch (IOException e) {
			return null;
		}
    }

    /** De-serialize from byte array. */
    public final void fromByteArray(byte[] data) {
    	try {
        	ByteArrayInputStream bais = new ByteArrayInputStream(data);
        	DataInputStream dis = new DataInputStream(bais);
			event = dis.readUTF();
	    	site = dis.readUTF();
	    	date = dis.readUTF();
	    	round = dis.readUTF();
	    	white = dis.readUTF();
	    	black = dis.readUTF();
	    	startPos = TextIO.readFEN(dis.readUTF());
	    	currentPos = new Position(startPos);
	    	timeControl = dis.readUTF();
	    	int nTags = dis.readInt();
	    	tagPairs.clear();
	    	for (int i = 0; i < nTags; i++) {
	    		TagPair tp = new TagPair();
	    		tp.tagName = dis.readUTF();
	    		tp.tagValue = dis.readUTF();
	    		tagPairs.add(tp);
	    	}
	    	rootNode = new Node();
	    	Node.readFromStream(dis, rootNode);
	    	currentNode = rootNode;
	    	int pathLen = dis.readInt();
	    	for (int i = 0; i < pathLen; i++)
	    		goForward(dis.readInt());
	    	dis.close();
	    	bais.close();
		} catch (IOException e) {
		} catch (ChessParseError e) {
		}
    }


    /** Go backward in game tree. */
    public final void goBack() {
    	if (currentNode.parent != null) {
    		currentPos.unMakeMove(currentNode.move, currentNode.ui);
    		currentNode = currentNode.parent;
    	}
    }

    /** Go forward in game tree.
     * @param variation Which variation to follow. -1 to follow default variation.
     */
    public final void goForward(int variation) {
    	currentNode.verifyChildren(currentPos);
    	if (variation < 0)
    		variation = currentNode.defaultChild;
    	int numChildren = currentNode.children.size();
    	if (variation >= numChildren)
    		variation = 0;
    	currentNode.defaultChild = variation;
    	if (numChildren > 0) {
    		currentNode = currentNode.children.get(variation);
    		currentPos.makeMove(currentNode.move, currentNode.ui);
            TextIO.fixupEPSquare(currentPos);
    	}
    }

    /** List of possible continuation moves. */
    public final List<Move> variations() {
    	currentNode.verifyChildren(currentPos);
    	List<Move> ret = new ArrayList<Move>();
    	for (Node child : currentNode.children)
    		ret.add(child.move);
    	return ret;
    }

    /** Add a move last in the list of variations.
     * @return Move number in variations list. -1 if moveStr is not a valid move
     */
    public final int addMove(String moveStr, String userCmd, int nag, String preComment, String postComment) {
    	currentNode.verifyChildren(currentPos);
    	int idx = currentNode.children.size();
    	Node node = new Node(currentNode, moveStr, userCmd, Integer.MIN_VALUE, nag, preComment, postComment);
    	Move move = TextIO.UCIstringToMove(moveStr);
    	if (move == null)
    		move = TextIO.stringToMove(currentPos, moveStr);
    	if (move == null)
    		return -1;
    	node.moveStr = TextIO.moveToString(currentPos, move, false);
    	node.move = move;
    	node.ui = new UndoInfo();
    	currentNode.children.add(node);
    	return idx;
	}
    
	/** Move a variation in the ordered list of variations. */
    public final void reorderVariation(int varNo, int newPos) {
    	currentNode.verifyChildren(currentPos);
    	int nChild = currentNode.children.size();
    	if ((varNo < 0) || (varNo >= nChild) || (newPos < 0) || (newPos >= nChild))
    		return;
    	Node var = currentNode.children.get(varNo);
    	currentNode.children.remove(varNo);
    	currentNode.children.add(newPos, var);

    	int newDef = currentNode.defaultChild;
    	if (varNo == newDef) {
    		newDef = newPos;
    	} else {
        	if (varNo < newDef) newDef--;
        	if (newPos <= newDef) newDef++;
    	}
		currentNode.defaultChild = newDef;
    }

    /** Delete a variation. */
    public final void deleteVariation(int varNo) {
    	currentNode.verifyChildren(currentPos);
    	int nChild = currentNode.children.size();
    	if ((varNo < 0) || (varNo >= nChild))
    		return;
    	currentNode.children.remove(varNo);
    	if (varNo == currentNode.defaultChild) {
    		currentNode.defaultChild = 0;
    	} else if (varNo < currentNode.defaultChild) {
    		currentNode.defaultChild--;
    	}
    }
    
    /* Get linear game history, using default variations at branch points. */
    public final List<Node> getMoveList() {
    	List<Node> ret = new ArrayList<Node>();
    	Node node = currentNode;
    	while (node != rootNode) {
    		ret.add(node);
    		node = node.parent;
    	}
    	Collections.reverse(ret);
    	node = currentNode;
    	Position pos = new Position(currentPos);
    	UndoInfo ui = new UndoInfo();
    	while (true) {
    		node.verifyChildren(pos);
    		if (node.defaultChild >= node.children.size())
    			break;
    		Node child = node.children.get(node.defaultChild);
    		ret.add(child);
    		pos.makeMove(child.move, ui);
    		node = child;
    	}
    	return ret;
    }


	final void setRemainingTime(int remaining) {
		currentNode.remainingTime = remaining;
	}

	final int getRemainingTime(boolean whiteMove, int initialTime) {
		int undef = Integer.MIN_VALUE;
		int remainingTime = undef;
		Node node = currentNode;
		boolean wtm = currentPos.whiteMove;
		while (true) {
			if (wtm != whiteMove) { // If wtm in current mode, black made last move
				remainingTime = node.remainingTime;
				if (remainingTime != undef)
					break;
			}
			Node parent = node.parent;
			if (parent == null)
				break;
			wtm = !wtm;
			node = parent;
		}
		if (remainingTime == undef) {
			remainingTime = initialTime;
		}
		return remainingTime;
	}

	/**
     *  A node object represents a position in the game tree.
     *  The position is defined by the move that leads to the position from the parent position.
     *  The root node is special in that it doesn't have a move.
     */
    static class Node {
    	String moveStr;				// String representation of move leading to this node. Empty string root node.
    	Move move;					// Computed on demand for better PGN parsing performance.
    								// Subtrees of invalid moves will be dropped when detected.
    								// Always valid for current node.
    	private UndoInfo ui;		// Computed when move is computed
    	String userCmd;				// User action. Draw claim/offer/accept or resign.

    	int remainingTime;			// Remaining time in ms for side that played moveStr, or INT_MIN if unknown.
    	int nag;					// Numeric annotation glyph
    	String preComment;			// Comment before move
    	String postComment;			// Comment after move
    	
    	private Node parent;		// Null if root node
    	int defaultChild;
    	private List<Node> children;

		public Node() {
    		this.moveStr = "";
    		this.move = null;
    		this.ui = null;
    		this.userCmd = "";
    		this.remainingTime = Integer.MIN_VALUE;
    		this.parent = null;
    		this.children = new ArrayList<Node>();
    		this.defaultChild = 0;
    		this.nag = 0;
    		this.preComment = "";
    		this.postComment = "";
		}

		public Node(Node parent, String moveStr, String userCmd, int remainingTime, int nag,
    				String preComment, String postComment) {
    		this.moveStr = moveStr;
    		this.move = null;
    		this.ui = null;
    		this.userCmd = userCmd;
    		this.remainingTime = remainingTime;
    		this.parent = parent;
    		this.children = new ArrayList<Node>();
    		this.defaultChild = 0;
    		this.nag = nag;
    		this.preComment = preComment;
    		this.postComment = postComment;
    	}

    	/** nodePos must represent the same position as this Node object. */
        private final void verifyChildren(Position nodePos) {
        	boolean anyToRemove = false;
        	for (Node child : children) {
        		if (child.move == null) {
        	    	Move move = TextIO.stringToMove(nodePos, child.moveStr);
        			if (move != null) {
        				child.moveStr = TextIO.moveToString(nodePos, move, false);
        				child.move = move;
        				child.ui = new UndoInfo();
        			} else {
        				anyToRemove = true;
        			}
        		}
        	}
        	if (anyToRemove) {
        		List<Node> validChildren = new ArrayList<Node>();
            	for (Node child : children)
            		if (child.move != null)
            			validChildren.add(child);
            	children = validChildren;
        	}
        }

		final List<Integer> getPathFromRoot() {
			List<Integer> ret = new ArrayList<Integer>(64);
			Node node = this;
			while (node.parent != null) {
				ret.add(node.parent.defaultChild);
				node = node.parent;
			}
			Collections.reverse(ret);
			return ret;
		}

		static final void writeToStream(DataOutputStream dos, Node node) throws IOException {
			while (true) {
				dos.writeUTF(node.moveStr);
				if (node.move != null) {
					dos.writeByte(node.move.from);
					dos.writeByte(node.move.to);
					dos.writeByte(node.move.promoteTo);
				} else {
					dos.writeByte(-1);
				}
				dos.writeUTF(node.userCmd);
				dos.writeInt(node.remainingTime);
				dos.writeInt(node.nag);
				dos.writeUTF(node.preComment);
				dos.writeUTF(node.postComment);
				dos.writeInt(node.defaultChild);
				int nChildren = node.children.size();
				dos.writeInt(nChildren);
				if (nChildren == 0)
					break;
				for (int i = 1; i < nChildren; i++) {
					writeToStream(dos, node.children.get(i));
				}
				node = node.children.get(0);
			}
		}

		static final void readFromStream(DataInputStream dis, Node node) throws IOException {
			while (true) {
				node.moveStr = dis.readUTF();
				int from = dis.readByte();
				if (from >= 0) {
					int to = dis.readByte();
					int prom = dis.readByte();
					node.move = new Move(from, to, prom);
					node.ui = new UndoInfo();
				}
				node.userCmd = dis.readUTF();
				node.remainingTime = dis.readInt();
				node.nag = dis.readInt();
				node.preComment = dis.readUTF();
				node.postComment = dis.readUTF();
				node.defaultChild = dis.readInt();
				int nChildren = dis.readInt();
				if (nChildren == 0)
					break;
				for (int i = 1; i < nChildren; i++) {
					Node child = new Node();
					child.parent = node;
					readFromStream(dis, child);
					node.children.add(child);
				}
				Node child = new Node();
				child.parent = node;
				node.children.add(0, child);
				node = child;
			}
		}

    	static class MoveNumber {
    		final int moveNo;
    		final boolean wtm; // White to move
    		MoveNumber(int moveNo, boolean wtm) {
    			this.moveNo = moveNo;
    			this.wtm = wtm;
    		}
    		public final MoveNumber next() {
    			if (wtm) return new MoveNumber(moveNo, false);
    			else     return new MoveNumber(moveNo + 1, true);
    		}
    		public final MoveNumber prev() {
    			if (wtm) return new MoveNumber(moveNo - 1, false);
    			else     return new MoveNumber(moveNo, true);
    		}
    	}

		/** Export whole tree rooted at "node" in PGN format. */ 
    	public static final void addPgnData(StringBuilder pgn, Node node, MoveNumber moveNum, PGNOptions options) {
    		int l0 = pgn.length();
    		boolean needMoveNr = node.addPgnDataOneNode(pgn, moveNum, true, options);
    		while (true) {
    			int nChild = node.children.size();
    			if (nChild == 0)
    				break;
    			if (pgn.length() > l0) pgn.append(' ');
    			MoveNumber nextMN = moveNum.next();
    			needMoveNr = node.children.get(0).addPgnDataOneNode(pgn, nextMN, needMoveNr, options);
    			if (options.exp.variations) {
    				for (int i = 1; i < nChild; i++) {
    					pgn.append(" (");
    					addPgnData(pgn, node.children.get(i), nextMN, options);
    					pgn.append(')');
    					needMoveNr = true;
    				}
    			}
    			node = node.children.get(0);
    			moveNum = moveNum.next();
    		}
    	}

    	/** Export this node in PGN format. */ 
    	private final boolean addPgnDataOneNode(StringBuilder pgn, MoveNumber mn, boolean needMoveNr, PGNOptions options) {
    		int l0 = pgn.length();
    		if ((preComment.length() > 0) && options.exp.comments) {
    			pgn.append('{');
    			pgn.append(preComment);
    			pgn.append('}');
    			needMoveNr = true;
    		}
    		if (moveStr.length() > 0) {
    			if (pgn.length() > l0) pgn.append(' ');
    			if (mn.wtm) {
    				pgn.append(mn.moveNo);
    				pgn.append(". ");
    			} else {
    				if (needMoveNr) {
    					pgn.append(mn.moveNo);
    					pgn.append("... ");
    				}
    			}
    			pgn.append(moveStr);
    			needMoveNr = false;
    		}
    		if ((nag > 0) && options.exp.nag) {
    			if (pgn.length() > l0) pgn.append(' ');
    			pgn.append('$');
    			pgn.append(nag);
    			needMoveNr = true;
    		}
    		if ((postComment.length() > 0) && options.exp.comments) {
    			if (pgn.length() > l0) pgn.append(' ');
    			pgn.append('{');
    			pgn.append(postComment);
    			pgn.append('}');
    			needMoveNr = true;
    		}
    		if ((userCmd.length() > 0) && options.exp.userCmd) {
    			if (pgn.length() > l0) pgn.append(' ');
    			addExtendedInfo(pgn, "usercmd", userCmd);
    			needMoveNr = true;
    		}
    		if ((remainingTime != Integer.MIN_VALUE) && options.exp.clockInfo) {
    			if (pgn.length() > l0) pgn.append(' ');
    			addExtendedInfo(pgn, "clk", getTimeStr(remainingTime));
    			needMoveNr = true;
    		}
    		return needMoveNr;
		}

    	private static final void addExtendedInfo(StringBuilder pgn, String extCmd, String extData) {
    		pgn.append("{[%");
    		pgn.append(extCmd);
    		pgn.append(' ');
    		pgn.append(extData);
    		pgn.append("]}");
    	}

    	private static final String getTimeStr(int remainingTime) {
    		int secs = (int)Math.floor((remainingTime + 999) / 1000.0);
    		boolean neg = false;
    		if (secs < 0) {
    			neg = true;
    			secs = -secs;
    		}
    		int mins = secs / 60;
    		secs -= mins * 60;
    		int hours = mins / 60;
    		mins -= hours * 60;
    		StringBuilder ret = new StringBuilder();
    		if (neg) ret.append('-');
    		if (hours < 10) ret.append('0');
    		ret.append(hours);
    		ret.append(':');
    		if (mins < 10) ret.append('0');
    		ret.append(mins);
    		ret.append(':');
    		if (secs < 10) ret.append('0');
    		ret.append(secs);
    		return ret.toString();
    	}
    }
}
