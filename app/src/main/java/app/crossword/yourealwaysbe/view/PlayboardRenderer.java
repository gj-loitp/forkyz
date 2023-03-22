package app.crossword.yourealwaysbe.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Base64;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.PuzImage;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * For rendering part of the board
 *
 * Caches an internal bitmap and reuses. Do not mix calls to
 * draw/drawWord/drawBoxes unless deliberately erasing previous draws.
 */
public class PlayboardRenderer {
    // for calculating max scale with no puzzle
    private static final int DEFAULT_PUZZLE_WIDTH = 15;
    private static final float BASE_BOX_SIZE_INCHES = 0.25F;
    private static final Logger LOG = Logger.getLogger(PlayboardRenderer.class.getCanonicalName());
    private static final Typeface TYPEFACE_SEMI_BOLD_SANS =
        AndroidVersionUtils.Factory.getInstance().getSemiBoldTypeface();
    private static final float DESCENT_FUDGE_FACTOR = 1.3F;

    private PaintProfile profile;
    private Bitmap bitmap;
    private Playboard board;
    private float dpi;
    private float scale = 1.0F;
    private float maxScale;
    private float minScale;
    private boolean hintHighlight;
    private int widthPixels;

    private final static AndroidVersionUtils versionUtils
        = AndroidVersionUtils.Factory.getInstance();

    // colors are gotten from context
    public PlayboardRenderer(Playboard board,
                             float dpi, int widthPixels, boolean hintHighlight,
                             Context context) {
        this.dpi = dpi;
        this.widthPixels = widthPixels;
        this.board = board;
        this.hintHighlight = hintHighlight;
        this.maxScale = getDeviceMaxScale();
        this.minScale = getDeviceMinScale();
        this.profile = new PaintProfile(context, scale, dpi);
    }

    public float getMaxScale() {
        return maxScale;
    }

    public float getMinScale() {
        return minScale;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public float getDeviceMaxScale(){
        float retValue;
        // inches * pixels per inch * units
        retValue = 2.2F;
        Puzzle puz = (board == null) ? null : board.getPuzzle();
        int width = (puz == null) ? DEFAULT_PUZZLE_WIDTH : puz.getWidth();
        float puzzleBaseSizeInInches = width * BASE_BOX_SIZE_INCHES;
        //leave a 1/16th in gutter on the puzzle.
        float fitToScreen =  (dpi * (puzzleBaseSizeInInches + 0.0625F)) / dpi;

        if(retValue < fitToScreen){
            retValue = fitToScreen;
        }

        return retValue;
    }

    public float getDeviceMinScale(){
        //inches * (pixels / pixels per inch);
        float retValue = 0.9F * ((dpi * BASE_BOX_SIZE_INCHES) / dpi);
        return retValue;
    }

    public void setScale(float scale) {
        float maxScale = getMaxScale();
        float minScale = getMinScale();

        if (scale > maxScale) {
            scale = maxScale;
        } else if (scale < minScale) {
            scale = minScale;
        } else if (Float.isNaN(scale)) {
            scale = 1.0f;
        }
        this.bitmap = null;
        this.scale = scale;

        profile.setScale(scale, dpi);
    }

    public float getScale() {
        return this.scale;
    }

    /**
     * Draw the board or just refresh it
     *
     * Refreshes current word and reset word if not null
     *
     * @param changes a collection of positions that have changed since the
     * last draw (can be null for all). Recommended to have a fast
     * lookup.
     * @param suppressNotesLists as in drawBox
     */
    public Bitmap draw(
        Collection<Position> changes, Set<String> suppressNotesLists
    ) {
        try {
            boolean newBitmap = initialiseBitmap(
                getFullWidth(), getFullHeight()
            );

            // if new bitmap, rerender all
            if (newBitmap)
                changes = null;

            Canvas canvas = new Canvas(bitmap);

            drawBoardBoxes(canvas, changes, suppressNotesLists);
            drawPinnedClue(canvas, changes, suppressNotesLists);
            if (changes == null)
                drawImages(canvas);

            return bitmap;
        } catch (OutOfMemoryError e) {
            return bitmap;
        }
    }

    public int getNumBoxesPerRow(int wrapWidth) {
        return wrapWidth / getBoxSize();
    }

    /**
     * Draw current word
     *
     * @param changes positions that have changed since last draw (may
     * be null for all). Recommend using something with fast lookup. May
     * contain positions outside of current word.
     * @param suppressNotesLists as in drawBox
     */
    public Bitmap drawWord(
        Collection<Position> changes,
        Set<String> suppressNotesLists,
        int wrapWidth
    ) {
        return drawWord(
            this.board.getCurrentWord(), changes, suppressNotesLists, wrapWidth
        );
    }

    /**
     * Draw word suppressing no notes
     */
    public Bitmap drawWord(
        Word word,
        Collection<Position> changes,
        int wrapWidth
    ) {
        // call draw word with empty list
        return drawWord(
            word, changes, Collections.<String>emptySet(), wrapWidth
        );
    }

    /**
     * Draw given word
     *
     * @param word word to draw
     * @param changes collection of positions changed since last draw.
     * Can be null to render all. Can contain more positions than word.
     * Recommend something with fast lookup.
     * @param suppressNotesLists as in drawBox
     * @param wrapWidth if non-zero, wrap word after width (in pixels)
     * exceeded
     */
    public Bitmap drawWord(
        Word word,
        Collection<Position> changes,
        Set<String> suppressNotesLists,
        int wrapWidth
    ) {
        Box[] boxes = board.getWordBoxes(word);
        Zone zone = word.getZone();
        int boxSize = getBoxSize();
        Position highlight = board.getHighlightLetter();

        int height;
        int width;

        if (wrapWidth > 0) {
            int boxesPerRow = getNumBoxesPerRow(wrapWidth);
            height = (int) Math.ceil(boxes.length / (float) boxesPerRow);
            width = Math.min(boxesPerRow, boxes.length);
        } else {
            height = 1;
            width = boxes.length;
        }

        if (initialiseBitmap(width, height))
            changes = null;

        Canvas canvas = new Canvas(bitmap);

        for (int i = 0; i < boxes.length; i++) {
            Position pos = zone.getPosition(i);

            if (changes != null && !changes.contains(pos))
                continue;

            int x = (i % width) * boxSize;
            int y = (i / width) * boxSize;
            this.drawBox(
                canvas,
                x, y,
                pos.getRow(), pos.getCol(),
                boxes[i],
                null, highlight,
                suppressNotesLists,
                false
            );
        }

        // draw highlight outline again as it will have been overpainted
        if (highlight != null) {
            PaintProfile profile = getProfile();
            int idx = zone.indexOf(highlight);
            if (idx > -1) {
                int x = idx * boxSize;
                drawBoxOutline(canvas, x, 0, profile.getCurrentLetterBox());
            }
        }

        return bitmap;
    }

    /**
     * Draw the boxes
     *
     * @param boxes boxes to draw
     * @param changes array of positions that have changed (should be
     * rendered). Can be null to draw all.
     * @param highlight the position in the box list to highlight
     * @param suppressNotesLists as in drawBox
     * @param wrapWidth if non-zero, wrap after this number of pixels
     */
    public Bitmap drawBoxes(
        Box[] boxes,
        boolean[] changes,
        Position highlight,
        Set<String> suppressNotesLists,
        int wrapWidth
    ) {
        if (boxes == null || boxes.length == 0) {
            return null;
        }

        int boxSize = getBoxSize();

        int height;
        int width;

        if (wrapWidth > 0) {
            int boxesPerRow = getNumBoxesPerRow(wrapWidth);
            height = (int) Math.ceil(boxes.length / (float) boxesPerRow);
            width = Math.min(boxesPerRow, boxes.length);
        } else {
            height = 1;
            width = boxes.length;
        }

        if (initialiseBitmap(width, height))
            changes = null;

        Canvas canvas = new Canvas(bitmap);

        for (int i = 0; i < boxes.length; i++) {
            if (changes != null && !changes[i])
                continue;

            int x = (i % width) * boxSize;
            int y = (i / width) * boxSize;
            this.drawBox(canvas,
                         x, y,
                         0, i,
                         boxes[i],
                         null,
                         highlight,
                         suppressNotesLists,
                         false);
        }

        if (highlight != null) {
            PaintProfile profile = getProfile();
            int col = highlight.getCol();
            if (col >= 0 && col < boxes.length) {
                drawBoxOutline(
                    canvas, col * boxSize, 0, profile.getCurrentLetterBox()
                );
            }
        }

        return bitmap;
    }

    /**
     * Board position of the point
     *
     * Not checked if in bounds, if outsize main board, check with
     * getUnpinnedPosition to see if it's a position in the pinned
     * display
     */
    public Position findPosition(Point p) {
        int boxSize = getBoxSize();

        int col = p.x / boxSize;
        int row = p.y / boxSize;

        return new Position(row, col);
    }

    /**
     * Convert a position to true position on board
     *
     * If position not on board but in the display of the pinned clue,
     * return the cell on the main board corresponding to this cell on
     * the pinned clue.
     *
     * Else return null;
     */
    public Position getUnpinnedPosition(Position pos) {
        Zone pinnedZone = getPinnedZone();
        if (pinnedZone == null)
            return null;

        if (pos.getRow() != getPinnedRow())
            return null;

        int col = pos.getCol() - getPinnedCol();

        if (col >= 0 && col < pinnedZone.size())
            return pinnedZone.getPosition(col);
        else
            return null;
    }

    public int findBoxNoScale(Point p) {
        int boxSize =  (int) (BASE_BOX_SIZE_INCHES * dpi);
        LOG.info("DPI "+dpi+" scale "+ scale +" box size "+boxSize);
        return p.x / boxSize;
    }

    public Point findPointBottomRight(Position p) {
        int boxSize = getBoxSize();
        int x = (p.getCol() * boxSize) + boxSize;
        int y = (p.getRow() * boxSize) + boxSize;

        return new Point(x, y);
    }

    public Point findPointBottomRight(Word word) {
        Zone zone = word.getZone();

        if (zone == null || zone.isEmpty())
            return null;

        // for now assume that last box is bottom right
        Position p = zone.getPosition(zone.size() - 1);

        int boxSize = getBoxSize();
        int x = (p.getCol() * boxSize) + boxSize;
        int y = (p.getRow() * boxSize) + boxSize;

        return new Point(x, y);
    }

    public Point findPointTopLeft(Position p) {
        int boxSize = getBoxSize();
        int x = p.getCol() * boxSize;
        int y = p.getRow() * boxSize;

        return new Point(x, y);
    }

    public Point findPointTopLeft(Word word) {
        // for now, assume first zone position is top left
        Zone zone = word.getZone();
        if (zone == null || zone.isEmpty())
            return null;
        return findPointTopLeft(zone.getPosition(0));
    }

    public float fitTo(int width, int height) {
        return fitTo(width, height, getFullWidth(), getFullHeight());
    }

    public float fitTo(
        int width, int height, int numBoxesWidth, int numBoxesHeight
    ) {
        this.bitmap = null;
        float newScaleWidth = calculateScale(width, numBoxesWidth);
        float newScaleHeight = calculateScale(height, numBoxesHeight);
        setScale(Math.min(newScaleWidth, newScaleHeight));
        return getScale();
    }

    public float fitWidthTo(int width, int numBoxes) {
        this.bitmap = null;
        setScale(calculateScale(width, numBoxes));
        return getScale();
    }

    public float zoomIn() {
        this.bitmap = null;
        this.scale = scale * 1.25F;
        if(scale > this.getMaxScale()){
            this.scale = this.getMaxScale();
        }
        return scale;
    }

    public float zoomOut() {
        this.bitmap = null;
        this.scale = scale / 1.25F;
        if(scale < this.getMinScale()){
            scale = this.getMinScale();
        }
        return scale;
    }

    public float zoomReset() {
        this.bitmap = null;
        this.scale = 1.0F;
        return scale;
    }

    public float zoomInMax() {
        this.bitmap = null;
        this.scale = getMaxScale();

        return scale;
    }

    /**
     * Draw an individual box
     *
     * @param fullBoard whether to draw details that only make sense when the
     * full board can be seen.
     * @param suppressNotesLists set of lists to not draw notes from.
     * Empty set means draw notes from all lists, null means don't draw
     * any notes.
     */
    private void drawBox(Canvas canvas,
                         int x, int y,
                         int row, int col,
                         Box box,
                         Word currentWord,
                         Position highlight,
                         Set<String> suppressNotesLists,
                         boolean fullBoard) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();

        boolean isHighlighted
            = highlight.getCol() == col
                && highlight.getRow() == row;

        Paint outlineColor = isHighlighted
            ? profile.getCurrentLetterBox()
            : profile.getOutline(box);
        drawBoxOutline(canvas, x, y, outlineColor);

        Rect r = new Rect(x + 1, y + 1, (x + boxSize) - 1, (y + boxSize) - 1);

        if (box == null) {
            canvas.drawRect(r, profile.getBoxColor(box));
        } else {
            if (highlightError(box, isHighlighted))
                box.setCheated(true);

            boolean inCurrentWord =
                (currentWord != null) && currentWord.checkInWord(row, col);

            drawBoxBackground(
                canvas, box, row, col, r, highlight, inCurrentWord
            );

            // Bars before clue numbers to avoid obfuscating
            if (fullBoard)
                drawBoxBars(canvas, x, y, box);

            drawBoxMarks(canvas, x, y, box, inCurrentWord);
            if (fullBoard) {
                drawBoxFlags(canvas, x, y, box);
            }

            drawBoxShape(canvas, x, y, box, inCurrentWord);

            if (box.isBlank()) {
                if (suppressNotesLists != null) {
                    drawBoxNotes(
                        canvas, x, y, box, inCurrentWord, suppressNotesLists
                    );
                }
            } else {
                drawBoxLetter(
                    canvas, x, y,
                    box, row, col,
                    isHighlighted, inCurrentWord
                );
            }
        }
    }

    private void drawBoxOutline(
        Canvas canvas, int x, int y, Paint color
    ) {
        int boxSize = getBoxSize();
        // Draw left, top, right, bottom
        canvas.drawLine(x, y, x, y + boxSize, color);
        canvas.drawLine(x, y, x + boxSize, y, color);
        canvas.drawLine(x + boxSize, y, x + boxSize, y + boxSize, color);
        canvas.drawLine(x, y + boxSize, x + boxSize, y + boxSize, color);
    }

    private void drawBoxBackground(
        Canvas canvas, Box box, int row, int col,
        Rect boxRect, Position highlight, boolean inCurrentWord
    ) {
        PaintProfile profile = getProfile();

        // doesn't depend on current word (for BoxEditText)
        boolean isHighlighted
            = highlight.getCol() == col
                && highlight.getRow() == row;
        boolean highlightError = highlightError(box, isHighlighted);

        if (isHighlighted && !highlightError) {
            canvas.drawRect(boxRect, profile.getCurrentLetterHighlight());
        } else if (isHighlighted && highlightError) {
            canvas.drawRect(boxRect, profile.getErrorHighlight());
        } else if (inCurrentWord) {
            canvas.drawRect(boxRect, profile.getCurrentWordHighlight());
        } else if (highlightError) {
            canvas.drawRect(boxRect, profile.getError());
        } else if (this.hintHighlight && box.isCheated()) {
            canvas.drawRect(boxRect, profile.getCheated());
        } else {
            canvas.drawRect(boxRect, profile.getBoxColor(box));
        }
    }

    private void drawBoxBars(
        Canvas canvas, int x, int y, Box box
    ) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int barSize = profile.getBarSize();
        int offset = barSize / 2;

        if (box.isBarredLeft()) {
            int barx = x + offset;
            drawBar(
                canvas,
                barx, y, barx, y + boxSize,
                box, box.getBarLeft()
            );
        }

        if (box.isBarredTop()) {
            int bary = y + offset;
            drawBar(
                canvas,
                x, bary, x + boxSize, bary,
                box, box.getBarTop()
            );
        }

        if (box.isBarredRight()) {
            int barx = x + boxSize - offset;
            drawBar(
                canvas,
                barx, y, barx, y + boxSize,
                box, box.getBarRight()
            );
        }

        if (box.isBarredBottom()) {
            int bary = y + boxSize - offset;
            drawBar(
                canvas,
                x, bary, x + boxSize, bary,
                box, box.getBarBottom()
            );
        }
    }

    private void drawBar(
        Canvas canvas,
        int xstart, int ystart, int xend, int yend,
        Box box, Box.Bar barStyle
    ) {
        PaintProfile profile = getProfile();
        Paint barColor = profile.getBarColor(box, barStyle);
        Path path = new Path();
        path.moveTo(xstart, ystart);
        path.lineTo(xend, yend);
        canvas.drawPath(path, barColor);
    }

    private void drawBoxMarks(
        Canvas canvas, int x, int y, Box box, boolean inCurrentWord
    ) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int numberOffset = profile.getNumberOffset();
        TextPaint numberText = profile.getNumberText(box, inCurrentWord);

        if (box.hasClueNumber()) {
            String clueNumber = box.getClueNumber();
            drawHtmlText(
                canvas,
                clueNumber,
                x + numberOffset,
                y + numberOffset / 2,
                boxSize,
                numberText
            );
        }

        if (box.hasMarks()) {
            int markHeight = getTotalHeight(numberText);

            // 3x3 by guarantee of Box
            String[][] marks = box.getMarks();
            for (int row = 0; row < 3; row++) {
                int markY;
                switch (row) {
                case 1: // middle
                    markY = boxSize / 2 - markHeight / 2 - numberOffset / 2;
                    break;
                case 2: // bottom
                    markY = boxSize - numberOffset - markHeight;
                    break;
                default: // top
                    markY = numberOffset / 2;
                }

                for (int col = 0; col < 3; col++) {
                    if (marks[row][col] != null) {
                        int fullWidth = boxSize - 2 * numberOffset;
                        Layout.Alignment align;
                        switch (col) {
                        case 1: // centre
                            align = Layout.Alignment.ALIGN_CENTER;
                            break;
                        case 2: // right
                            align = Layout.Alignment.ALIGN_OPPOSITE;
                            break;
                        default: // left
                            align = Layout.Alignment.ALIGN_NORMAL;
                        }

                        drawHtmlText(
                            canvas,
                            marks[row][col],
                            x + numberOffset,
                            y + markY,
                            fullWidth,
                            align,
                            numberText
                        );
                    }
                }
            }
        }
    }

    private void drawBoxFlags(Canvas canvas, int x, int y, Box box) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int barSize = profile.getBarSize();
        int numberOffset = profile.getNumberOffset();
        int numberTextSize = profile.getNumberTextSize();

        Puzzle puz = board.getPuzzle();

        boolean flagAcross = false;
        boolean flagDown = false;

        for (ClueID cid : box.getIsPartOfClues()) {
            if (box.isStartOf(cid) && puz.isFlagged(cid)) {
                if (isClueProbablyAcross(cid))
                    flagAcross = true;
                else
                    flagDown = true;

            }
        }

        if (flagDown) {
            String clueNumber = box.getClueNumber();
            int numDigits = clueNumber == null ? 0 : clueNumber.length();
            int numWidth = numDigits * numberTextSize / 2;
            Rect bar = new Rect(
                x + numberOffset + numWidth + barSize,
                y + 1 * barSize,
                x + boxSize - barSize,
                y + 2 * barSize
            );
            canvas.drawRect(bar, profile.getFlag());
        }

        if (flagAcross) {
            Rect bar = new Rect(
                x + 1 * barSize,
                y + barSize + numberOffset + numberTextSize,
                x + 2 * barSize,
                y + boxSize - barSize
            );
            canvas.drawRect(bar, profile.getFlag());
        }
    }

    private void drawBoxShape(
        Canvas canvas, int x, int y, Box box, boolean inCurrentWord
    ) {
        if (!box.hasShape())
            return;

        int boxSize = getBoxSize();
        PaintProfile profile = getProfile();
        Paint paint = profile.getShape(box, inCurrentWord);
        int off = profile.getShapeStrokeWidth();

        Path path;

        switch(box.getShape()) {
        case CIRCLE:
            canvas.drawCircle(
                x + boxSize / 2, y + boxSize / 2, boxSize / 2 - off, paint
            );
            break;
        case ARROW_LEFT:
            path = new Path();
            path.moveTo(x + boxSize - off, y + boxSize / 2);
            path.lineTo(x + off, y + boxSize / 2);
            path.lineTo(x + boxSize / 4, y + boxSize / 4);
            path.moveTo(x + off, y + boxSize / 2);
            path.lineTo(x + boxSize / 4, y + 3 * boxSize / 4);
            canvas.drawPath(path, paint);
            break;
        case ARROW_RIGHT:
            path = new Path();
            path.moveTo(x + off, y + boxSize / 2);
            path.lineTo(x + boxSize - off, y + boxSize / 2);
            path.lineTo(x + 3 * boxSize / 4, y + boxSize / 4);
            path.moveTo(x + boxSize - off, y + boxSize / 2);
            path.lineTo(x + 3 * boxSize / 4, y + 3 * boxSize / 4);
            canvas.drawPath(path, paint);
            break;
        case ARROW_UP:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + boxSize - off);
            path.lineTo(x + boxSize / 2, y + off);
            path.lineTo(x + boxSize / 4, y + boxSize / 4);
            path.moveTo(x + boxSize / 2, y + off);
            path.lineTo(x + 3 * boxSize / 4, y + boxSize / 4);
            canvas.drawPath(path, paint);
            break;
        case ARROW_DOWN:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + off);
            path.lineTo(x + boxSize / 2, y + boxSize - off);
            path.lineTo(x + boxSize / 4, y + 3 * boxSize / 4);
            path.moveTo(x + boxSize / 2, y + boxSize - off);
            path.lineTo(x + 3 * boxSize / 4, y + 3 * boxSize / 4);
            canvas.drawPath(path, paint);
            break;
        case TRIANGLE_LEFT:
            path = new Path();
            path.moveTo(x + boxSize - off, y + boxSize / 2);
            path.lineTo(x + off, y + off);
            path.lineTo(x + off, y + boxSize - off);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case TRIANGLE_RIGHT:
            path = new Path();
            path.moveTo(x + off, y + boxSize / 2);
            path.lineTo(x + boxSize - off, y + off);
            path.lineTo(x + boxSize - off, y + boxSize - off);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case TRIANGLE_UP:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + boxSize - off);
            path.lineTo(x + off, y + off);
            path.lineTo(x + boxSize - off, y + off);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case TRIANGLE_DOWN:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + off);
            path.lineTo(x + off, y + boxSize - off);
            path.lineTo(x + boxSize - off, y + boxSize - off);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case DIAMOND:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + off);
            path.lineTo(x + boxSize - off, y + boxSize / 2);
            path.lineTo(x + boxSize / 2, y + boxSize - off);
            path.lineTo(x + off, y + boxSize / 2);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case CLUB:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + 3 * boxSize / 5);
            path.quadTo(
                x + off, y + boxSize - off,
                x + off, y + boxSize / 2
            );
            path.quadTo(
                x + off, y + boxSize / 5,
                x + 2 * boxSize / 5, y + 2 * boxSize / 5
            );
            path.quadTo(
                x + off, y + off,
                x + boxSize / 2, y + off
            );
            path.quadTo(
                x + boxSize - off, y + off,
                x + 3 * boxSize / 5, y + 2 * boxSize / 5
            );
            path.quadTo(
                x + boxSize - off, y + boxSize / 5,
                x + boxSize - off, y + boxSize / 2
            );
            path.quadTo(
                x + boxSize - off, y + boxSize - off,
                x + boxSize / 2, y + 3 * boxSize / 5
            );
            path.quadTo(
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + 3 * boxSize / 4, y + boxSize - off
            );
            path.lineTo(
                x + boxSize / 4, y + boxSize - off
            );
            path.quadTo(
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + boxSize / 2, y + 3 * boxSize / 5
            );
            canvas.drawPath(path, paint);
            break;
        case HEART:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + boxSize / 4);
            path.cubicTo(
                x + boxSize / 2, y + off,
                x + off, y + off,
                x + off, y + boxSize / 3
            );
            path.cubicTo(
                x + off, y + boxSize / 2,
                x + boxSize / 2, y + 2 * boxSize / 3,
                x + boxSize / 2, y + boxSize - off
            );
            path.cubicTo(
                x + boxSize / 2, y + 2 * boxSize / 3,
                x + boxSize - off, y + boxSize / 2,
                x + boxSize - off, y + boxSize / 3
            );
            path.cubicTo(
                x + boxSize - off, y + off,
                x + boxSize / 2, y + off,
                x + boxSize / 2, y + boxSize / 4
            );
            canvas.drawPath(path, paint);
            break;
        case SPADE:
            path = new Path();
            path.moveTo(x + boxSize / 2, y + 3 * boxSize / 5);
            path.cubicTo(
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + off, y + 4 * boxSize / 5,
                x + off, y + 3 * boxSize / 5
            );
            path.cubicTo(
                x + off, y + boxSize / 2,
                x + boxSize / 2, y + boxSize / 3,
                x + boxSize / 2, y + off
            );
            path.cubicTo(
                x + boxSize / 2, y + boxSize / 3,
                x + boxSize - off, y + boxSize / 2,
                x + boxSize - off, y + 3 * boxSize / 5
            );
            path.cubicTo(
                x + boxSize - off, y + 4 * boxSize / 5,
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + boxSize / 2, y + 3 * boxSize / 5
            );
            path.quadTo(
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + 2 * boxSize / 3, y + boxSize - off
            );
            path.lineTo(x + boxSize / 3, y + boxSize - off);
            path.quadTo(
                x + boxSize / 2, y + 4 * boxSize / 5,
                x + boxSize / 2, y + 3 * boxSize / 5
            );
            canvas.drawPath(path, paint);
            break;
        case STAR:
            path = new Path();
            path.moveTo(x + off, y + 2 * boxSize / 5);
            path.lineTo(x + 2 * boxSize / 5, y + 2 * boxSize / 5);
            path.lineTo(x + boxSize / 2, y + off);
            path.lineTo(x + 3 * boxSize / 5, y + 2 * boxSize / 5);
            path.lineTo(x + boxSize - off, y + 2 * boxSize / 5);
            path.lineTo(x + 7 * boxSize / 10, y + 3 * boxSize / 5);
            path.lineTo(x + 4 * boxSize / 5, y + boxSize - off);
            path.lineTo(x + boxSize / 2, y + 7 * boxSize / 10);
            path.lineTo(x + boxSize / 5, y + boxSize - off);
            path.lineTo(x + 3 * boxSize / 10, y + 3 * boxSize / 5);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case SQUARE:
            canvas.drawRect(
                x + off, y + off, x + boxSize - off, y + boxSize - off, paint
            );
            break;
        case RHOMBUS:
            path = new Path();
            path.moveTo(x + 2 * boxSize / 5, y + off);
            path.lineTo(x + boxSize - off, y + off);
            path.lineTo(x + 3 * boxSize / 5, y + boxSize - off);
            path.lineTo(x + off, y + boxSize - off);
            path.close();
            canvas.drawPath(path, paint);
            break;
        case FORWARD_SLASH:
            canvas.drawLine(
                x + off, y + off, x + boxSize - off, y + boxSize - off, paint
            );
            break;
        case BACK_SLASH:
            canvas.drawLine(
                x + off, y + boxSize - off, x + boxSize - off, y + off, paint
            );
            break;
        case X:
            canvas.drawLine(
                x + off, y + off, x + boxSize - off, y + boxSize - off, paint
            );
            canvas.drawLine(
                x + off, y + boxSize - off, x + boxSize - off, y + off, paint
            );
            break;
        }
    }

    private void drawBoxLetter(
        Canvas canvas, int x, int y,
        Box box, int row, int col,
        boolean isHighlighted, boolean inCurrentWord
    ) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int textOffset = profile.getTextOffset();
        TextPaint thisLetter = profile.getLetterText(box, inCurrentWord);

        String letterString = box.isBlank()
            ? null
            : box.getResponse();

        if (letterString == null)
            return;

        if (highlightError(box, isHighlighted)) {
            if (isHighlighted) {
                thisLetter = profile.getCellColor();
            } else if (inCurrentWord) {
                thisLetter = profile.getErrorHighlight();
            }
        }

        if (letterString.length() > 1) {
            thisLetter = getIdealTextSize(letterString, thisLetter, boxSize);
        }

        int yoffset
            = boxSize - textOffset - getTotalHeight(thisLetter);
        drawText(
            canvas,
            letterString,
            x + (boxSize / 2),
            y + yoffset,
            boxSize,
            thisLetter
        );
    }

    private void drawBoxNotes(
        Canvas canvas, int x, int y, Box box,
        boolean inCurrentWord, Set<String> suppressNotesLists
    ) {
        PaintProfile profile = getProfile();
        int boxSize = profile.getBoxSize();
        int textOffset = profile.getTextOffset();
        TextPaint noteText = profile.getNoteText(box, inCurrentWord);
        TextPaint miniNoteText = profile.getMiniNoteText(box, inCurrentWord);
        TextPaint letterText = profile.getLetterText(box, inCurrentWord);

        String noteStringAcross = null;
        String noteStringDown = null;

        for (ClueID cid : box.getIsPartOfClues()) {
            if (suppressNotesLists.contains(cid.getListName()))
                continue;

            Note note = board.getPuzzle().getNote(cid);
            if (note == null)
                continue;

            String scratch = note.getScratch();
            if (scratch == null)
                continue;

            int pos = box.getCluePosition(cid);
            if (pos < 0 || pos >= scratch.length())
                continue;

            char noteChar = scratch.charAt(pos);
            if (noteChar == ' ')
                continue;

            if (isClueProbablyAcross(cid)) {
                noteStringAcross =
                    Character.toString(noteChar);
            } else {
                noteStringDown =
                    Character.toString(noteChar);
            }
        }

        float[] mWidth = new float[1];
        letterText.getTextWidths("M", mWidth);
        float letterTextHalfWidth = mWidth[0] / 2;

        if (noteStringAcross != null && noteStringDown != null) {
            if (noteStringAcross.equals(noteStringDown)) {
                // Same scratch letter in both directions
                // Align letter with across and down answers
                int noteTextHeight = getTotalHeight(noteText);
                drawText(
                    canvas,
                    noteStringAcross,
                    x + (int)(boxSize - letterTextHalfWidth),
                    y + boxSize - noteTextHeight - textOffset,
                    boxSize,
                    noteText
                );
            } else {
                // Conflicting scratch letters
                // Display both letters side by side
                int noteTextHeight = getTotalHeight(miniNoteText);
                drawText(
                    canvas,
                    noteStringAcross,
                    x + (int)(boxSize * 0.05 + letterTextHalfWidth),
                    y + boxSize - noteTextHeight - textOffset,
                    boxSize,
                    miniNoteText
                );
                int yoffset =
                    boxSize
                    - noteTextHeight
                    + (int) miniNoteText.ascent();
                drawText(
                    canvas,
                    noteStringDown,
                    x + (int)(boxSize - letterTextHalfWidth),
                    y + yoffset,
                    boxSize,
                    miniNoteText
                );
            }
        } else if (noteStringAcross != null) {
            // Across scratch letter only - display in bottom left
            int noteTextHeight = getTotalHeight(noteText);
            drawText(
                canvas,
                noteStringAcross,
                x + (boxSize / 2),
                y + boxSize - noteTextHeight - textOffset,
                boxSize,
                noteText
            );
        } else if (noteStringDown != null) {
            // Down scratch letter only - display in bottom left
            int noteTextHeight = getTotalHeight(noteText);
            drawText(
                canvas,
                noteStringDown,
                x + (int)(boxSize - letterTextHalfWidth),
                y + boxSize - noteTextHeight - textOffset,
                boxSize,
                noteText
            );
        }
    }

    /**
     * Estimate general direction of clue
     *
     * Bias towards across if unsure
     */
    private boolean isClueProbablyAcross(ClueID cid) {
        Puzzle puz = board == null ? null : board.getPuzzle();
        if (puz == null)
            return true;

        Clue clue = puz.getClue(cid);
        Zone zone = (clue == null) ? null : clue.getZone();
        if (zone == null || zone.size() <= 1)
            return true;

        Position pos0 = zone.getPosition(0);
        Position pos1 = zone.getPosition(1);

        return pos1.getCol() > pos0.getCol();
    }

    private boolean highlightError(Box box, boolean hasCursor) {
        boolean showErrors = board != null && (
            this.board.isShowErrorsGrid()
            || (this.board.isShowErrorsCursor() && hasCursor)
        );

        return showErrors
            && !box.isBlank()
            && box.hasSolution()
            && !Objects.equals(box.getSolution(), box.getResponse());
    }

    private static void drawText(
        Canvas canvas,
        CharSequence text,
        int x, int  y, int width,
        TextPaint style
    ) {
        drawText(
            canvas, text, x, y, width, Layout.Alignment.ALIGN_NORMAL, style
        );
    }

    private static void drawText(
        Canvas canvas,
        CharSequence text,
        int x, int  y, int width, Layout.Alignment align,
        TextPaint style
    ) {
        // with some help from:
        // https://stackoverflow.com/a/41870464
        StaticLayout staticLayout
            = versionUtils.getStaticLayout(text, style, width, align);
        canvas.save();
        canvas.translate(x, y);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * Calculate text size to avoid overflow
     *
     * See how much space it would be, recommend a smaller version if
     * needed.
     *
     * Returns the text paint with the right size, may or may not be the
     * original style passed
     */
    private static TextPaint getIdealTextSize(
        CharSequence text, TextPaint style, int width
    ) {
        float desiredWidth = StaticLayout.getDesiredWidth(text, style);
        float styleSize = style.getTextSize();
        if (desiredWidth > width) {
            // -1 needed else on rare occasions overruns lines
            int newSize = (int) ((width / desiredWidth) * styleSize) - 1;
            TextPaint newStyle = new TextPaint(style);
            newStyle.setTextSize(newSize);
            return newStyle;
        } else {
            return style;
        }
    }

    private static void drawHtmlText(
        Canvas canvas, String text, int x, int y, int width, TextPaint style
    ) {
        drawText(canvas, HtmlCompat.fromHtml(text, 0), x, y, width, style);
    }

    private static void drawHtmlText(
        Canvas canvas, String text,
        int x, int y, int width, Layout.Alignment align,
        TextPaint style
    ) {
        drawText(
            canvas, HtmlCompat.fromHtml(text, 0), x, y, width, align, style
        );
    }

    private float calculateScale(int numPixels, int numBoxes) {
        double density = (double) dpi * (double) BASE_BOX_SIZE_INCHES;
        return (float) ((double) numPixels / (double) numBoxes / density);
    }

    private int getTotalHeight(TextPaint style) {
        return (int) Math.ceil(
            - style.ascent()
            + DESCENT_FUDGE_FACTOR * style.descent()
        );
    }

    private void drawImages(Canvas canvas) {
        Puzzle puz = (board == null) ? null : board.getPuzzle();
        if (puz == null)
            return;

        int boxSize = getBoxSize();

        for (PuzImage image : puz.getImages()) {
            Object tag = image.getTag();
            if (tag == null || !(tag instanceof Bitmap))
                tagImageWithBitmap(image);

            tag = image.getTag();
            if (tag instanceof Bitmap) {
                Bitmap bmp = (Bitmap) tag;
                int startx = image.getCol() * boxSize;
                int starty = image.getRow() * boxSize;
                int endx = startx + image.getWidth() * boxSize;
                int endy = starty + image.getHeight() * boxSize;

                canvas.drawBitmap(
                    bmp,
                    new Rect(0, 0, bmp.getWidth(), bmp.getHeight()),
                    new Rect(startx, starty, endx, endy),
                    null
                );
            }
        }
    }

    private void tagImageWithBitmap(PuzImage image) {
        String url = image.getURL();
        if (url.substring(0, 5).equalsIgnoreCase("data:")) {
            int start = url.indexOf(",") + 1;
            if (start > 0) {
                byte[] data = Base64.decode(
                    url.substring(start), Base64.DEFAULT
                );
                Bitmap imgBmp
                    = BitmapFactory.decodeByteArray(data, 0, data.length);
                image.setTag(imgBmp);
            }
        }
    }

    /**
     * Refresh the pinned clue (or draw)
     *
     * Refresh parts in current or reset word, unless renderAll.
     *
     * @param canvas to draw on (assumed large enough)
     * @param changes the positions that have changed (can be null for
     * all)
     * @param suppressNotesLists as in drawBox
     */
    private void drawPinnedClue(
        Canvas canvas, Collection<Position> changes,
        Set<String> suppressNotesLists
    ) {
        Puzzle puz = this.board.getPuzzle();
        Box[][] boxes = this.board.getBoxes();
        int boxSize = getBoxSize();
        Position highlight = board.getHighlightLetter();

        if (!puz.hasPinnedClueID())
            return;

        Zone pinnedZone = getPinnedZone();
        if (pinnedZone == null)
            return;

        Word currentWord = this.board.getCurrentWord();

        int pinnedRow = getPinnedRow();
        int pinnedCol = getPinnedCol();

        int y =  pinnedRow * boxSize;

        for (int i = 0; i < pinnedZone.size(); i++) {
            Position pos = pinnedZone.getPosition(i);
            if (changes != null && !changes.contains(pos))
                continue;

            int x = (pinnedCol + i) * boxSize;
            int row = pos.getRow();
            int col = pos.getCol();

            this.drawBox(
                canvas,
                x, y, row, col,
                boxes[row][col],
                currentWord, highlight,
                suppressNotesLists,
                true
            );
        }

        // draw highlight outline again as it will have been overpainted
        if (highlight != null) {
            PaintProfile profile = getProfile();
            int idx = pinnedZone.indexOf(highlight);
            if (idx > -1) {
                int x = (pinnedCol + idx) * boxSize;
                drawBoxOutline(canvas, x, y, profile.getCurrentLetterBox());
            }
        }
    }

    /**
     * Row on which pinned word is rendered
     *
     * Or -1 if nothing pinned
     */
    private int getPinnedRow() {
        Puzzle puz = this.board.getPuzzle();
        return puz.hasPinnedClueID() ? puz.getHeight() + 1 : -1;
    }

    /**
     * Col of first box of pinned word
     *
     * Or -1 if nothing pinned
     */
    private int getPinnedCol() {
        Zone pinnedZone = getPinnedZone();
        return pinnedZone == null
            ? -1
            : (getFullWidth() - pinnedZone.size()) / 2;
    }

    /**
     * Make sure bitmap field has a bitmap
     *
     * @return true if a new (blank) bitmap created, else old one used
     */
    private boolean initialiseBitmap(int width, int height) {
        if (bitmap != null)
            return false;

        int boxSize = getBoxSize();

        bitmap = Bitmap.createBitmap(
            width * boxSize, height * boxSize, Bitmap.Config.ARGB_8888
        );
        bitmap.eraseColor(Color.TRANSPARENT);

        return true;
    }

    private int getFullWidth() {
        Puzzle puz = this.board.getPuzzle();
        int width = puz.getWidth();

        if (puz.hasPinnedClueID()) {
            Zone pinnedZone = getPinnedZone();
            if (pinnedZone != null)
                width = Math.max(width, pinnedZone.size());
        }

        return width;
    }

    private int getFullHeight() {
        Puzzle puz = this.board.getPuzzle();
        int height = puz.getHeight();

        if (puz.hasPinnedClueID())
            height += 2;

        return height;
    }

    /**
     * Refresh board (current word) on canvas or draw all
     *
     * @param canvas canvas to draw on
     * @param boxSize the size of a box
     * @param changes the positions that have changed since the last
     * draw (can be null, which means render all)
     * @param renderAll whether to just draw the whole board anyway
     * @param suppressNotesLists the notes lists not to draw (null means
     * draw none, empty means draw all)
     */
    private void drawBoardBoxes(
        Canvas canvas,
        Collection<Position> changes,
        Set<String> suppressNotesLists
    ) {
        Puzzle puz = board.getPuzzle();
        Box[][] boxes = board.getBoxes();
        int boxSize = getBoxSize();
        int width = puz.getWidth();
        int height = puz.getHeight();
        Position highlight = board.getHighlightLetter();
        Word currentWord = this.board.getCurrentWord();

        // just have one object for some efficiency
        Position tempPos = new Position(0, 0);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (changes != null) {
                    tempPos.setRow(row);
                    tempPos.setCol(col);
                    if (!changes.contains(tempPos))
                        continue;
                }

                int x = col * boxSize;
                int y = row * boxSize;
                this.drawBox(
                    canvas,
                    x, y, row, col,
                    boxes[row][col],
                    currentWord, highlight,
                    suppressNotesLists,
                    true
                );
            }
        }

        // draw highlight outline again as it will have been overpainted
        if (highlight != null) {
            PaintProfile profile = getProfile();
            int curX = highlight.getCol() * boxSize;
            int curY = highlight.getRow() * boxSize;
            drawBoxOutline(canvas, curX, curY, profile.getCurrentLetterBox());
        }
    }

    private Zone getPinnedZone() {
        Puzzle puz = this.board.getPuzzle();
        Clue pinnedClue = puz.getClue(puz.getPinnedClueID());
        return pinnedClue == null
            ? null
            : pinnedClue.getZone();
    }

    /**
     * The size of a box in pixels according to current scale
     */
    private int getBoxSize() {
        return getProfile().getBoxSize();
    }

    /**
     * Metrics and paint objects for drawing
     */
    private PaintProfile getProfile() {
        return profile;
    }

    private static class PaintProfile {
        // box colours also can be used for text for "transparent" text
        // (e.g. highlighted error cells)
        private final TextPaint cellBox = new TextPaint();
        private final TextPaint blockBox = new TextPaint();
        private final Paint shape = new Paint();
        private final Paint blockShape = new Paint();
        private final Paint outline = new Paint();
        private final Paint bar = new Paint();
        private final Paint barDashed = new Paint();
        private final Paint barDotted = new Paint();
        private final Paint cheated = new Paint();
        private final Paint currentLetterBox = new Paint();
        private final Paint currentLetterHighlight = new Paint();
        private final Paint currentWordHighlight = new Paint();
        private final TextPaint letterText = new TextPaint();
        private final TextPaint blockLetterText = new TextPaint();
        private final TextPaint numberText = new TextPaint();
        private final TextPaint blockNumberText = new TextPaint();
        private final TextPaint noteText = new TextPaint();
        private final TextPaint blockNoteText = new TextPaint();
        private final TextPaint miniNoteText = new TextPaint();
        private final TextPaint blockMiniNoteText = new TextPaint();
        private final Paint error = new Paint();
        private final TextPaint errorHighlight = new TextPaint();
        private final Paint flag = new Paint();
        private final TextPaint onBlock = new TextPaint();

        private int boxSize;
        private int numberTextSize = boxSize / 4;
        private int miniNoteTextSize = boxSize / 2;
        private int noteTextSize = Math.round(boxSize * 0.6F);
        private int letterTextSize = Math.round(boxSize * 0.7F);
        private int barSize = boxSize / 12;
        private int numberOffset = barSize;
        private int textOffset = boxSize / 30;
        private int shapeStrokeWidth = Math.max(1, boxSize / 15);

        public PaintProfile(Context context, float scale, float dpi) {
            int blockColor
                = ContextCompat.getColor(context, R.color.blockColor);
            int cellColor = ContextCompat.getColor(context, R.color.cellColor);
            int currentWordHighlightColor = ContextCompat.getColor(
                context, R.color.currentWordHighlightColor
            );
            int currentLetterHighlightColor = ContextCompat.getColor(
                context, R.color.currentLetterHighlightColor
            );
            int errorColor
                = ContextCompat.getColor(context, R.color.errorColor);
            int errorHighlightColor
                = ContextCompat.getColor(context, R.color.errorHighlightColor);
            int cheatedColor
                = ContextCompat.getColor(context, R.color.cheatedColor);
            int boardLetterColor
                = ContextCompat.getColor(context, R.color.boardLetterColor);
            int boardNoteColor
                = ContextCompat.getColor(context, R.color.boardNoteColor);
            int flagColor = ContextCompat.getColor(context, R.color.flagColor);
            int onBlockColor
                = ContextCompat.getColor(context, R.color.onBlockColor);
            int boardShapeColor
                = ContextCompat.getColor(context, R.color.boardShapeColor);
            int blockShapeColor
                = ContextCompat.getColor(context, R.color.blockShapeColor);

            outline.setColor(blockColor);
            outline.setStrokeWidth(2.0F);

            // line styles set in scale
            bar.setColor(blockColor);
            bar.setStyle(Style.STROKE);
            barDashed.setColor(blockColor);
            barDashed.setStyle(Style.STROKE);
            barDotted.setColor(blockColor);
            barDotted.setStyle(Style.STROKE);

            numberText.setTextAlign(Align.LEFT);
            numberText.setColor(boardLetterColor);
            numberText.setAntiAlias(true);
            numberText.setTypeface(Typeface.MONOSPACE);

            blockNumberText.setTextAlign(Align.LEFT);
            blockNumberText.setColor(onBlockColor);
            blockNumberText.setAntiAlias(true);
            blockNumberText.setTypeface(Typeface.MONOSPACE);

            noteText.setTextAlign(Align.CENTER);
            noteText.setColor(boardNoteColor);
            noteText.setAntiAlias(true);
            noteText.setTypeface(TYPEFACE_SEMI_BOLD_SANS);

            blockNoteText.setTextAlign(Align.CENTER);
            blockNoteText.setColor(onBlockColor);
            blockNoteText.setAntiAlias(true);
            blockNoteText.setTypeface(TYPEFACE_SEMI_BOLD_SANS);

            miniNoteText.setTextAlign(Align.CENTER);
            miniNoteText.setColor(boardNoteColor);
            miniNoteText.setAntiAlias(true);
            miniNoteText.setTypeface(TYPEFACE_SEMI_BOLD_SANS);

            blockMiniNoteText.setTextAlign(Align.CENTER);
            blockMiniNoteText.setColor(onBlockColor);
            blockMiniNoteText.setAntiAlias(true);
            blockMiniNoteText.setTypeface(TYPEFACE_SEMI_BOLD_SANS);

            letterText.setTextAlign(Align.CENTER);
            letterText.setColor(boardLetterColor);
            letterText.setAntiAlias(true);
            letterText.setTypeface(Typeface.SANS_SERIF);

            blockLetterText.setTextAlign(Align.CENTER);
            blockLetterText.setColor(onBlockColor);
            blockLetterText.setAntiAlias(true);
            blockLetterText.setTypeface(Typeface.SANS_SERIF);

            shape.setColor(boardShapeColor);
            shape.setAntiAlias(true);
            shape.setStyle(Style.STROKE);
            shape.setStrokeJoin(Paint.Join.ROUND);

            blockShape.setColor(blockShapeColor);
            blockShape.setAntiAlias(true);
            blockShape.setStyle(Style.STROKE);
            blockShape.setStrokeJoin(Paint.Join.ROUND);

            currentWordHighlight.setColor(currentWordHighlightColor);
            currentLetterHighlight.setColor(currentLetterHighlightColor);
            currentLetterBox.setColor(cellColor);
            currentLetterBox.setStrokeWidth(2.0F);

            error.setTextAlign(Align.CENTER);
            error.setColor(errorColor);
            error.setAntiAlias(true);
            error.setTypeface(Typeface.SANS_SERIF);

            errorHighlight.setTextAlign(Align.CENTER);
            errorHighlight.setColor(errorHighlightColor);
            errorHighlight.setAntiAlias(true);
            errorHighlight.setTypeface(Typeface.SANS_SERIF);

            blockBox.setColor(blockColor);
            blockBox.setTextAlign(Align.CENTER);
            blockBox.setAntiAlias(true);
            blockBox.setTypeface(Typeface.SANS_SERIF);

            cellBox.setColor(cellColor);
            cellBox.setTextAlign(Align.CENTER);
            cellBox.setAntiAlias(true);
            cellBox.setTypeface(Typeface.SANS_SERIF);

            cheated.setColor(cheatedColor);
            flag.setColor(flagColor);

            setScale(scale, dpi);
        }

        public TextPaint getBoxColor(Box box) {
            if (box == null || !box.hasColor())
               return Box.isBlock(box) ? blockBox : cellBox;
            else
               return getRelativePaint(cellBox, box.getColor());
        }

        public TextPaint getCellColor() {
            return cellBox;
        }

        public Paint getShape(Box box, boolean inCurrentWord) {
            Paint base = Box.isBlock(box) ? blockShape : shape;
            if (box == null || !box.hasTextColor() || inCurrentWord) {
               return base;
            } else {
                Paint mixedPaint = new Paint(base);
                Paint cell = getCellColor();
                mixedPaint.setColor(getRelativeColor(
                    cell.getColor(), box.getTextColor()
                ));
                return mixedPaint;
            }
        }

        public Paint getOutline(Box box) {
            return outline;
        }

        public Paint getBarColor(Box box, Box.Bar barStyle) {
            Paint base = bar;
            if (barStyle == Box.Bar.DOTTED)
                base = barDotted;
            else if (barStyle == Box.Bar.DASHED)
                base = barDashed;

            if (box == null || !box.hasBarColor()) {
               return base;
            } else {
                Paint mixedPaint = new Paint(base);
                Paint cell = getCellColor();
                mixedPaint.setColor(getRelativeColor(
                    cell.getColor(), box.getBarColor()
                ));
                return mixedPaint;
            }
        }

        public Paint getCheated() {
            return cheated;
        }

        public Paint getCurrentLetterBox() {
            return currentLetterBox;
        }

        public Paint getCurrentLetterHighlight() {
            return currentLetterHighlight;
        }

        public Paint getCurrentWordHighlight() {
            return currentWordHighlight;
        }

        public TextPaint getLetterText(Box box, boolean inCurrentWord) {
            return mixTextPaint(
                box, letterText, blockLetterText, inCurrentWord
            );
        }

        public TextPaint getNumberText(Box box, boolean inCurrentWord) {
            return mixTextPaint(
                box, numberText, blockNumberText, inCurrentWord
            );
        }

        public TextPaint getNoteText(Box box, boolean inCurrentWord) {
            return mixTextPaint(box, noteText, blockNoteText, inCurrentWord);
        }

        public TextPaint getMiniNoteText(Box box, boolean inCurrentWord) {
            return mixTextPaint(
                box, miniNoteText, blockMiniNoteText, inCurrentWord
            );
        }

        public Paint getError() {
            return error;
        }

        public TextPaint getErrorHighlight() {
            return errorHighlight;
        }

        public Paint getFlag() {
            return flag;
        }

        public int getBoxSize() { return boxSize; }
        public int getNumberTextSize() { return numberTextSize; }
        public int getMiniNoteTextSize() { return miniNoteTextSize; }
        public int getNoteTextSize() { return noteTextSize; }
        public int getLetterTextSize() { return letterTextSize; }
        public int getBarSize() { return barSize; }
        public int getNumberOffset() { return numberOffset; }
        public int getTextOffset() { return textOffset; }
        public int getShapeStrokeWidth() { return shapeStrokeWidth; }

        public void setScale(float scale, float dpi) {
            boxSize = calcBoxSize(scale, dpi);
            numberTextSize = boxSize / 4;
            miniNoteTextSize = boxSize / 2;
            noteTextSize = Math.round(boxSize * 0.6F);
            letterTextSize = Math.round(boxSize * 0.7F);
            barSize = boxSize / 12;
            numberOffset = barSize;
            textOffset = boxSize / 30;
            shapeStrokeWidth = Math.max(1, boxSize / 15);

            numberText.setTextSize(numberTextSize);
            blockNumberText.setTextSize(numberTextSize);
            letterText.setTextSize(letterTextSize);
            blockLetterText.setTextSize(letterTextSize);
            cellBox.setTextSize(letterTextSize);
            blockBox.setTextSize(letterTextSize);
            error.setTextSize(letterTextSize);
            errorHighlight.setTextSize(letterTextSize);
            noteText.setTextSize(noteTextSize);
            blockNoteText.setTextSize(noteTextSize);
            miniNoteText.setTextSize(miniNoteTextSize);
            blockMiniNoteText.setTextSize(miniNoteTextSize);

            shape.setStrokeWidth(shapeStrokeWidth);
            blockShape.setStrokeWidth(shapeStrokeWidth);

            bar.setStrokeWidth(barSize);
            barDashed.setStrokeWidth(barSize);
            float dashSize = boxSize / 9.0F;
            barDashed.setPathEffect(new DashPathEffect(
                new float[] { 2 * dashSize, dashSize }, dashSize
            ));
            barDotted.setStrokeWidth(barSize);
            barDotted.setPathEffect(new DashPathEffect(
                new float[] { barSize, barSize}, (barSize / 2)
            ));
        }

        private TextPaint mixTextPaint(
            Box box, TextPaint cellBase, TextPaint blockBase,
            boolean inCurrentWord
        ) {
            TextPaint base = Box.isBlock(box) ? blockBase : cellBase;
            if (box == null || !box.hasTextColor() || inCurrentWord) {
               return base;
            } else {
                TextPaint mixedPaint = new TextPaint(base);
                Paint cell = getCellColor();
                mixedPaint.setColor(getRelativeColor(
                    cell.getColor(), box.getTextColor()
                ));
                return mixedPaint;
            }
        }

        private int calcBoxSize(float scale, float dpi) {
            int boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * scale);
            if (boxSize == 0) {
                boxSize = (int) (BASE_BOX_SIZE_INCHES * dpi * 0.25F);
            }
            return boxSize;
        }

        /**
         * Return a new paint based on color
         *
         * For use when "inverting" a color to appear on the board. Relative
         * vs. a pure boxColor background is the pure color. Vs. a pure black
         * background in the inverted color. Somewhere in between is
         * somewhere in between.
         *
         * @param base the standard background color
         * @param color 24-bit 0x00rrggbb "pure" color
         */
        private Paint getRelativePaint(Paint base, int pureColor) {
            Paint mixedPaint = new Paint(base);
            mixedPaint.setColor(getRelativeColor(base.getColor(), pureColor));
            return mixedPaint;
        }

        /**
         * Return a new text paint based on color
         */
        private TextPaint getRelativePaint(TextPaint base, int pureColor) {
            TextPaint mixedPaint = new TextPaint(base);
            mixedPaint.setColor(getRelativeColor(base.getColor(), pureColor));
            return mixedPaint;
        }

        private int getRelativeColor(int baseColor, int pureColor) {
            int mixedR = mixColors(Color.red(baseColor), Color.red(pureColor));
            int mixedG = mixColors(Color.green(baseColor), Color.green(pureColor));
            int mixedB = mixColors(Color.blue(baseColor), Color.blue(pureColor));

            return Color.rgb(mixedR, mixedG, mixedB);
        }

        /**
         * Tint a 0-255 pure color against a base
         *
         * See getRelativePaint
         */
        private int mixColors(int base, int pure) {
            double baseBias = base / 255.0;
            return (int)(
                (baseBias * pure) + ((1- baseBias) * (255 - pure))
            );
        }
    }
}

