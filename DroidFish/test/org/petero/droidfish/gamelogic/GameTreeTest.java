package org.petero.droidfish.gamelogic;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.petero.droidfish.gamelogic.GameTree.Node;

public class GameTreeTest {

	@Test
	public final void testGameTree() throws ChessParseError {
		GameTree gt = new GameTree();
		Position expectedPos = TextIO.readFEN(TextIO.startPosFEN);
		assertEquals(expectedPos, gt.currentPos);

		List<Move> varList = gt.variations();
		assertEquals(0, varList.size());

		int varNo = gt.addMove("e4", "", 0, "", "");
		assertEquals(0, varNo);
		assertEquals(expectedPos, gt.currentPos);

		gt.goForward(varNo);
		Move move = TextIO.UCIstringToMove("e2e4");
		UndoInfo ui = new UndoInfo();
		expectedPos.makeMove(move, ui);
		assertEquals(expectedPos, gt.currentPos);

		gt.goBack();
		expectedPos.unMakeMove(move, ui);
		assertEquals(expectedPos, gt.currentPos);
		
		varNo = gt.addMove("d4", "", 0, "", "");
		assertEquals(1, varNo);
		assertEquals(expectedPos, gt.currentPos);
		varList = gt.variations();
		assertEquals(2, varList.size());

		gt.goForward(varNo);
		move = TextIO.UCIstringToMove("d2d4");
		expectedPos.makeMove(move, ui);
		assertEquals(expectedPos, gt.currentPos);

		varNo = gt.addMove("g8f6", "", 0, "", "");
		assertEquals(0, varNo);
		assertEquals(expectedPos, gt.currentPos);
		varList = gt.variations();
		assertEquals(1, varList.size());
		
		gt.goForward(-1);
		Move move2 = TextIO.UCIstringToMove("g8f6");
		UndoInfo ui2 = new UndoInfo();
		expectedPos.makeMove(move2, ui2);
		assertEquals(expectedPos, gt.currentPos);
		assertEquals("Nf6", gt.currentNode.moveStr);

		gt.goBack();
		assertEquals("d4", gt.currentNode.moveStr);
		gt.goBack();
		expectedPos.unMakeMove(move2, ui2);
		expectedPos.unMakeMove(move, ui);
		assertEquals(expectedPos, gt.currentPos);
		assertEquals("", gt.currentNode.moveStr);
		
		gt.goForward(-1); // Should remember that d2d4 was last visited branch
		expectedPos.makeMove(move, ui);
		assertEquals(expectedPos, gt.currentPos);
		
		byte[] serialState = gt.toByteArray();
		gt = new GameTree();
		gt.fromByteArray(serialState);
		assertEquals(expectedPos, gt.currentPos);

		gt.goBack();
		expectedPos.unMakeMove(move, ui);
		assertEquals(expectedPos, gt.currentPos);
		varList = gt.variations();
		assertEquals(2, varList.size());
	}

	private final String getMoveListAsString(GameTree gt) {
		StringBuilder ret = new StringBuilder();
		List<Node> lst = gt.getMoveList();
		for (int i = 0; i < lst.size(); i++) {
			if (i > 0)
				ret.append(' ');
			ret.append(lst.get(i).moveStr);
		}
		return ret.toString();
	}

	@Test
	public final void testGetMoveList() throws ChessParseError {
		GameTree gt = new GameTree();
		gt.addMove("e4", "", 0, "", "");
		gt.addMove("d4", "", 0, "", "");
		assertEquals("e4", getMoveListAsString(gt));

		gt.goForward(0);
		assertEquals("e4", getMoveListAsString(gt));

		gt.addMove("e5", "", 0, "", "");
		gt.addMove("c5", "", 0, "", "");
		assertEquals("e4 e5", getMoveListAsString(gt));

		gt.goForward(1);
		assertEquals("e4 c5", getMoveListAsString(gt));

		gt.addMove("Nf3", "", 0, "", "");
		gt.addMove("d4", "", 0, "", "");
		assertEquals("e4 c5 Nf3", getMoveListAsString(gt));

		gt.goForward(1);
		assertEquals("e4 c5 d4", getMoveListAsString(gt));
		
		gt.goBack();
		assertEquals("e4 c5 d4", getMoveListAsString(gt));

		gt.goBack();
		assertEquals("e4 c5 d4", getMoveListAsString(gt));

		gt.goBack();
		assertEquals("e4 c5 d4", getMoveListAsString(gt));

		gt.goForward(1);
		assertEquals("d4", getMoveListAsString(gt));
		
		gt.goBack();
		assertEquals("d4", getMoveListAsString(gt));
		
		gt.goForward(0);
		assertEquals("e4 c5 d4", getMoveListAsString(gt));
	}

	@Test
	public final void testReorderVariation() throws ChessParseError {
		GameTree gt = new GameTree();
		gt.addMove("e4", "", 0, "", "");
		gt.addMove("d4", "", 0, "", "");
		gt.addMove("c4", "", 0, "", "");
		assertEquals("e4 d4 c4", getVariationsAsString(gt));
		assertEquals(0, gt.currentNode.defaultChild);
		
		gt.reorderVariation(1, 0);
		assertEquals("d4 e4 c4", getVariationsAsString(gt));
		assertEquals(1, gt.currentNode.defaultChild);
		
		gt.reorderVariation(0, 2);
		assertEquals("e4 c4 d4", getVariationsAsString(gt));
		assertEquals(0, gt.currentNode.defaultChild);

		gt.reorderVariation(1, 2);
		assertEquals("e4 d4 c4", getVariationsAsString(gt));
		assertEquals(0, gt.currentNode.defaultChild);

		gt.reorderVariation(0, 1);
		assertEquals("d4 e4 c4", getVariationsAsString(gt));
		assertEquals(1, gt.currentNode.defaultChild);
	}

	@Test
	public final void testDeleteVariation() throws ChessParseError {
		GameTree gt = new GameTree();
		gt.addMove("e4", "", 0, "", "");
		gt.addMove("d4", "", 0, "", "");
		gt.addMove("c4", "", 0, "", "");
		gt.addMove("f4", "", 0, "", "");
		gt.deleteVariation(0);
		assertEquals("d4 c4 f4", getVariationsAsString(gt));
		assertEquals(0, gt.currentNode.defaultChild);
		
		gt.reorderVariation(0, 2);
		assertEquals("c4 f4 d4", getVariationsAsString(gt));
		assertEquals(2, gt.currentNode.defaultChild);
		gt.deleteVariation(1);
		assertEquals("c4 d4", getVariationsAsString(gt));
		assertEquals(1, gt.currentNode.defaultChild);

		gt.addMove("g4", "", 0, "", "");
		gt.addMove("h4", "", 0, "", "");
		assertEquals("c4 d4 g4 h4", getVariationsAsString(gt));
		assertEquals(1, gt.currentNode.defaultChild);
		gt.reorderVariation(1, 2);
		assertEquals("c4 g4 d4 h4", getVariationsAsString(gt));
		assertEquals(2, gt.currentNode.defaultChild);
		gt.deleteVariation(2);
		assertEquals("c4 g4 h4", getVariationsAsString(gt));
		assertEquals(0, gt.currentNode.defaultChild);
	}

	private final String getVariationsAsString(GameTree gt) {
		StringBuilder ret = new StringBuilder();
		List<Move> vars = gt.variations();
		for (int i = 0; i < vars.size(); i++) {
			if (i > 0)
				ret.append(' ');
			String moveStr = TextIO.moveToString(gt.currentPos, vars.get(i), false);
			ret.append(moveStr);
		}
		return ret.toString();
	}
	
	@Test
	public final void testGetRemainingTime() throws ChessParseError {
		GameTree gt = new GameTree();
		int initialTime = 60000;
		assertEquals(initialTime, gt.getRemainingTime(true, initialTime));
		assertEquals(initialTime, gt.getRemainingTime(false, initialTime));
		
		gt.addMove("e4", "", 0, "", "");
		gt.goForward(-1);
		assertEquals(initialTime, gt.getRemainingTime(true, initialTime));
		assertEquals(initialTime, gt.getRemainingTime(false, initialTime));
		gt.setRemainingTime(45000);
		assertEquals(45000, gt.getRemainingTime(true, initialTime));
		assertEquals(initialTime, gt.getRemainingTime(false, initialTime));

		gt.addMove("e5", "", 0, "", "");
		assertEquals(45000, gt.getRemainingTime(true, initialTime));
		assertEquals(initialTime, gt.getRemainingTime(false, initialTime));

		gt.goForward(-1);
		assertEquals(45000, gt.getRemainingTime(true, initialTime));
		assertEquals(initialTime, gt.getRemainingTime(false, initialTime));

		gt.addMove("Nf3", "", 0, "", "");
		gt.goForward(-1);
		gt.addMove("Nc6", "", 0, "", "");
		gt.goForward(-1);
		assertEquals(45000, gt.getRemainingTime(true, initialTime));
		assertEquals(initialTime, gt.getRemainingTime(false, initialTime));

		gt.setRemainingTime(30000);
		assertEquals(45000, gt.getRemainingTime(true, initialTime));
		assertEquals(30000, gt.getRemainingTime(false, initialTime));

		gt.addMove("Bb5", "", 0, "", "");
		gt.goForward(-1);
		gt.setRemainingTime(20000);
		assertEquals(20000, gt.getRemainingTime(true, initialTime));
		assertEquals(30000, gt.getRemainingTime(false, initialTime));

		gt.addMove("a6", "", 0, "", "");
		gt.goForward(-1);
		gt.setRemainingTime(15000);
		assertEquals(20000, gt.getRemainingTime(true, initialTime));
		assertEquals(15000, gt.getRemainingTime(false, initialTime));

		gt.goBack();
		assertEquals(20000, gt.getRemainingTime(true, initialTime));
		assertEquals(30000, gt.getRemainingTime(false, initialTime));

		gt.goBack();
		assertEquals(45000, gt.getRemainingTime(true, initialTime));
		assertEquals(30000, gt.getRemainingTime(false, initialTime));

		gt.goBack();
		assertEquals(45000, gt.getRemainingTime(true, initialTime));
		assertEquals(initialTime, gt.getRemainingTime(false, initialTime));
		
		gt.goBack();
		gt.goBack();
		gt.goBack();
		assertEquals(initialTime, gt.getRemainingTime(true, initialTime));
		assertEquals(initialTime, gt.getRemainingTime(false, initialTime));
	}

	// FIXME!!! Test that invalid moves are automatically deleted when calling variations().
}
