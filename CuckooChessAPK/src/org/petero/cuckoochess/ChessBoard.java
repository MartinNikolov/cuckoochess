package org.petero.cuckoochess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;
import chess.Move;
import chess.Piece;
import chess.Position;

public class ChessBoard extends TextView {
	private Position pos;
    private int selectedSquare;
    private int x0, y0, sqSize;
    private boolean flipped;
    private Typeface chessFont;

    private Paint darkPaint;
    private Paint brightPaint;
    private Paint redOutline;
    private Paint whitePiecePaint;
    private Paint blackPiecePaint;
    
	public ChessBoard(Context context, AttributeSet attrs) {
		super(context, attrs);
    	chessFont = Typeface.createFromFile("assets/casefont.ttf");
    	pos = new Position();
        selectedSquare = -1;
        x0 = y0 = sqSize = 0;
        flipped = false;

        darkPaint = new Paint();
        darkPaint.setARGB(255, 128, 128, 128);

        brightPaint = new Paint();
        brightPaint.setARGB(255, 190, 190, 90);

        redOutline = new Paint();
        redOutline.setARGB(255, 255, 0, 0);
        redOutline.setStyle(Paint.Style.STROKE);
        
        whitePiecePaint = new Paint();
        whitePiecePaint.setARGB(255, 255, 255, 255);
    	whitePiecePaint.setTypeface(chessFont);
        
        blackPiecePaint = new Paint();
        blackPiecePaint.setARGB(255, 0, 0, 0);
    	blackPiecePaint.setTypeface(chessFont);
	}

    /**
     * Set the board to a given state.
     * @param pos
     */
    final public void setPosition(Position pos) {
        this.pos = pos;
        invalidate();
    }

    /**
     * Set/clear the board flipped status.
     * @param flipped
     */
    final public void setFlipped(boolean flipped) {
        this.flipped = flipped;
        invalidate();
    }

    /**
     * Set/clear the selected square.
     * @param square The square to select, or -1 to clear selection.
     */
    final public void setSelection(int square) {
        if (square != this.selectedSquare) {
            this.selectedSquare = square;
            invalidate();
        }
    }
    
	@Override
	protected void onDraw(Canvas canvas) {
		final int width = getWidth();
		final int height = getHeight();
        sqSize = (Math.min(width, height) - 4) / 8;
        x0 = (width - sqSize * 8) / 2;
        y0 = (height - sqSize * 8) / 2;

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                final int xCrd = getXCrd(x);
                final int yCrd = getYCrd(y);
                Paint paint = Position.darkSquare(x, y) ? darkPaint : brightPaint;
                canvas.drawRect(xCrd, yCrd, xCrd+sqSize, yCrd+sqSize, paint);

                int sq = Position.getSquare(x, y);
                int p = pos.getPiece(sq);
                drawPiece(canvas, xCrd + sqSize / 2, yCrd + sqSize / 2, p);
            }
        }
        if (selectedSquare >= 0) {
            int selX = Position.getX(selectedSquare);
            int selY = Position.getY(selectedSquare);
            redOutline.setStrokeWidth(sqSize/(float)16);
            int x0 = getXCrd(selX);
            int y0 = getYCrd(selY);
            canvas.drawRect(x0, y0, x0 + sqSize, y0 + sqSize, redOutline);
        }
    }

    private final void drawPiece(Canvas canvas, int xCrd, int yCrd, int p) {
        String ps;
        switch (p) {
            case Piece.EMPTY:
                ps = "";
                break;
            case Piece.WKING:
                ps = "k";
                break;
            case Piece.WQUEEN:
                ps = "q";
                break;
            case Piece.WROOK:
                ps = "r";
                break;
            case Piece.WBISHOP:
                ps = "b";
                break;
            case Piece.WKNIGHT:
                ps = "n";
                break;
            case Piece.WPAWN:
                ps = "p";
                break;
            case Piece.BKING:
                ps = "l";
                break;
            case Piece.BQUEEN:
                ps = "w";
                break;
            case Piece.BROOK:
                ps = "t";
                break;
            case Piece.BBISHOP:
                ps = "v";
                break;
            case Piece.BKNIGHT:
                ps = "m";
                break;
            case Piece.BPAWN:
                ps = "o";
                break;
            default:
                ps = "?";
                break;
        }
        if (ps.length() > 0) {
        	Paint paint = Piece.isWhite(p) ? whitePiecePaint : blackPiecePaint;
        	paint.setTextSize(10.0f);
            Rect bounds = new Rect();
            paint.getTextBounds(ps, 0, ps.length(), bounds);
            int xCent = bounds.centerX();
            int yCent = bounds.centerY();
            canvas.drawText(ps, xCrd - xCent, yCrd - yCent, paint);
        }
    }

    private final int getXCrd(int x) {
        return x0 + sqSize * (flipped ? 7 - x : x);
    }
    private final int getYCrd(int y) {
        return y0 + sqSize * (flipped ? y : (7 - y));
    }

    /**
     * Compute the square corresponding to the coordinates of a mouse event.
     * @param evt Details about the mouse event.
     * @return The square corresponding to the mouse event, or -1 if outside board.
     */
    /*
    final int eventToSquare(MouseEvent evt) {
        int xCrd = evt.getX();
        int yCrd = evt.getY();

        int sq = -1;
        if ((xCrd >= x0) && (yCrd >= y0) && (sqSize > 0)) {
            int x = (xCrd - x0) / sqSize;
            int y = 7 - (yCrd - y0) / sqSize;
            if ((x >= 0) && (x < 8) && (y >= 0) && (y < 8)) {
                if (flipped) {
                    x = 7 - x;
                    y = 7 - y;
                }
                sq = Position.getSquare(x, y);
            }
        }
        return sq;
    }
    */

    final Move mousePressed(int sq) {
        Move m = null;
        int p = pos.getPiece(sq);
        if ((selectedSquare >= 0) && (sq == selectedSquare)) {
            int fromPiece = pos.getPiece(selectedSquare);
            if ((fromPiece == Piece.EMPTY) || (Piece.isWhite(fromPiece) != pos.isWhiteMove())) {
                return m; // Can't move the piece the oppenent just moved.
            }
        }
        if ((selectedSquare < 0) &&
                ((p == Piece.EMPTY) || (Piece.isWhite(p) != pos.isWhiteMove()))) {
            return m;  // You must click on one of your own pieces.
        }

        if (selectedSquare >= 0) {
            if (sq != selectedSquare) {
                if ((p == Piece.EMPTY) || (Piece.isWhite(p) != pos.isWhiteMove())) {
                    m = new Move(selectedSquare, sq, Piece.EMPTY);
                    setSelection(sq);
                }
            }
        }
        if (m == null) {
            setSelection(-1);
        }
        return m;
    }

}
