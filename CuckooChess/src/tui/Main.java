/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tui;

import java.io.IOException;

import chess.ComputerPlayer;
import chess.HumanPlayer;
import chess.Player;

/**
 *
 * @author petero
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        if ((args.length == 1) && args[0].equals("gui")) {
            gui.AppletGUI.main(args);
        } else if ((args.length == 1) && args[0].equals("uci")) {
            uci.UCIProtocol.main(args);
        } else {
            Player whitePlayer = new HumanPlayer();
            ComputerPlayer blackPlayer = new ComputerPlayer();
            blackPlayer.setTTLogSize(21);
            TUIGame game = new TUIGame(whitePlayer, blackPlayer);
            game.play();
        }
    }
}
