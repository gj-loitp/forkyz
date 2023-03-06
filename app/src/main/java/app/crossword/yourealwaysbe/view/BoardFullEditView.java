
package app.crossword.yourealwaysbe.view;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * A live view of the full playboard
 *
 * Renders the playboard on change and implements input connection with
 * soft-input.
 */
public class BoardFullEditView extends BoardEditView {
    public BoardFullEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Position findPosition(Point point) {
        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return null;
        return renderer.findPosition(point);
    }

    @Override
    public float fitToView() {
        PlayboardRenderer renderer = getRenderer();
        if (renderer == null)
            return -1;

        scrollTo(0, 0);
        int w = getWidth();
        int h = getHeight();
        float scale = renderer.fitTo(w, h);
        setCurrentScale(scale);
        return scale;
    }

    @Override
    public void onPlayboardChange(PlayboardChanges changes) {
        render(changes.getCellChanges());
        ensureVisible(changes.getPreviousWord());
        super.onPlayboardChange(changes);
    }

    @Override
    public void onNewResponse(String response) {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (isScratchMode()) {
            if (response != null)
                board.playScratchLetter(response.charAt(0));
        } else {
            board.playLetter(response);
        }
    }

    @Override
    public void onDeleteResponse() {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (isScratchMode()) {
            board.deleteScratchLetter();
        } else {
            board.deleteLetter();
        }
    }

    @Override
    public void render(Collection<Position> changes, boolean rescale) {
        PlayboardRenderer renderer = getRenderer();
        Playboard board = getBoard();
        if (renderer == null || board == null)
            return;

        setBitmap(
            renderer.draw(changes, getSuppressNotesList()),
            rescale
        );
    }

    @Override
    protected void onClick(Position position) {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (board.isInWord(position)) {
            Word previousWord = board.setHighlightLetter(position);
            notifyClick(position, previousWord);
        } else {
            PlayboardRenderer renderer = getRenderer();
            if (renderer == null)
                return;

            Position cellPos = renderer.getUnpinnedPosition(position);
            if (cellPos != null) {
                Puzzle puz = board.getPuzzle();
                if (puz == null)
                    return;

                ClueID pinnedCID = puz.getPinnedClueID();
                Word previousWord
                    = board.setHighlightLetter(cellPos, pinnedCID);
                notifyClick(position, previousWord);
            }
        }
    }

    @Override
    protected Set<String> getSuppressNotesList() {
        boolean displayScratch = getPrefs().getBoolean("displayScratch", false);
        return displayScratch
            ? Collections.emptySet()
            : null;
    }

    private void ensureVisible(Word previousWord) {
        PlayboardRenderer renderer = getRenderer();
        Playboard board = getBoard();
        if (renderer == null || board == null)
            return;

        SharedPreferences prefs = getPrefs();

        /*
         * If we jumped to a new word, ensure the first letter is visible.
         * Otherwise, insure that the current letter is visible. Only necessary
         * if the cursor is currently off screen.
         */
        if (prefs.getBoolean("ensureVisible", true)) {
            Word currentWord = board.getCurrentWord();
            Position cursorPos = board.getHighlightLetter();

            Point topLeft;
            Point bottomRight;
            Point cursorTopLeft;
            Point cursorBottomRight;

            cursorTopLeft = renderer.findPointTopLeft(cursorPos);
            cursorBottomRight = renderer.findPointBottomRight(cursorPos);

            if ((previousWord != null) && previousWord.equals(currentWord)) {
                topLeft = cursorTopLeft;
                bottomRight = cursorBottomRight;
            } else {
                topLeft = renderer.findPointTopLeft(currentWord);
                bottomRight = renderer.findPointBottomRight(currentWord);
            }

            ensureVisible(bottomRight);
            ensureVisible(topLeft);

            // ensure the cursor is always on the screen.
            ensureVisible(cursorBottomRight);
            ensureVisible(cursorTopLeft);
        }

    }

    private boolean isScratchMode() {
        return getPrefs().getBoolean("scratchMode", false);
    }
}
