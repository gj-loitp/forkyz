package app.crossword.yourealwaysbe;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.view.ClueTabs;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.view.PlayboardRenderer;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ClickListener;
import app.crossword.yourealwaysbe.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.view.ScrollingImageView;

public class ClueListActivity extends PuzzleActivity
                              implements ClueTabs.ClueTabsListener {
    private static final Logger LOG = Logger.getLogger(
        ClueListActivity.class.getCanonicalName()
    );

    private KeyboardManager keyboardManager;
    private ScrollingImageView imageView;
    private CharSequence imageViewDescriptionBase;
    private PlayboardRenderer renderer;
    private ClueTabs clueTabs;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.clue_list_menu_clue_notes) {
            launchClueNotes();
            return true;
        } else if (id == R.id.clue_list_menu_puzzle_notes) {
            launchPuzzleNotes();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Create the activity
     *
     * This only sets up the UI widgets. The set up for the current
     * puzzle/board is done in onResume as these are held by the
     * application and may change while paused!
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        utils.holographic(this);
        utils.finishOnHomeButton(this);

        Playboard board = getBoard();
        Puzzle puz = getBoard().getPuzzle();

        if (board == null || puz == null) {
            LOG.info(
                "ClueListActivity resumed but no Puzzle selected, "
                    + "finishing."
            );
            finish();
            return;
        }

        setContentView(R.layout.clue_list);

        this.imageView = (ScrollingImageView) this.findViewById(R.id.miniboard);
        this.imageViewDescriptionBase = this.imageView.getContentDescription();
        this.imageView.setAllowOverScroll(false);

        this.imageView.setContextMenuListener(new ClickListener() {
            public void onContextMenu(Point e) {
                onTap(e);
                launchClueNotes();
            }

            public void onTap(Point e) {
                Playboard board = getBoard();

                if (board == null)
                    return;

                Word current = board.getCurrentWord();
                Zone zone = (current == null) ? null : current.getZone();
                if (zone == null)
                    return;

                int box = renderer.findBox(e).getCol();
                Position newPos = zone.getPosition(box);

                if (!Objects.equals(newPos, getBoard().getHighlightLetter())) {
                    getBoard().setHighlightLetter(newPos);
                }

                displayKeyboard();
            }
        });

        this.clueTabs = this.findViewById(R.id.clueListClueTabs);

        ForkyzKeyboard keyboard = (ForkyzKeyboard) findViewById(R.id.keyboard);
        keyboardManager = new KeyboardManager(this, keyboard, imageView);
    }

    @Override
    public void onResume() {
        super.onResume();

        Playboard board = getBoard();
        Puzzle puz = getBoard().getPuzzle();

        if (board == null || puz == null) {
            LOG.info(
                "ClueListActivity resumed but no Puzzle selected, "
                    + "finishing."
            );
            finish();
            return;
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();

        this.renderer = new PlayboardRenderer(
            board,
            metrics.densityDpi, metrics.widthPixels,
            !prefs.getBoolean("supressHints", false),
            this
        );

        scaleRendererToCurWord();

        clueTabs.setBoard(board);
        clueTabs.addListener(this);
        clueTabs.listenBoard();
        clueTabs.refresh();

        keyboardManager.onResume();

        this.render();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.clue_list_menu, menu);
        return true;
    }

    @Override
    public void onPlayboardChange(
        boolean wholeBoard, Word currentWord, Word previousWord
    ) {
        super.onPlayboardChange(wholeBoard, currentWord, previousWord);
        this.render();
    }

    @Override
    public void onClueTabsClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (board.isJumpableClue(clue)) {
            Word old = board.getCurrentWord();
            board.jumpToClue(clue);
            displayKeyboard(old);
        }
    }

    @Override
    public void onClueTabsLongClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (getPuzzle().isNotableClue(clue)) {
            board.jumpToClue(clue);
            launchClueNotes();
        } else {
            launchPuzzleNotes();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // for parity with onKeyUp
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DEL:
        case KeyEvent.KEYCODE_SPACE:
            return true;
        }

        char c = Character.toUpperCase(event.getDisplayLabel());
        if (PlayActivity.ALPHA.indexOf(c) != -1)
            return true;

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Playboard board = getBoard();
        Puzzle puz = board.getPuzzle();
        Word w = board.getCurrentWord();

        Zone zone = (w == null) ? null : w.getZone();
        Position first = null;
        Position last = null;

        if (zone != null && !zone.isEmpty()) {
            first = zone.getPosition(0);
            last = zone.getPosition(zone.size() - 1);
        }

        Clue clue = board.getClue();
        String curList = clue.getListName();
        String curClueNumber = clue.getClueNumber();

        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
            if (!keyboardManager.handleBackKey())
                this.finish();
            return true;

        case KeyEvent.KEYCODE_DPAD_LEFT:
            if (!board.getHighlightLetter().equals(first)) {
                board.moveZoneBack(false);
            } else {
                clueTabs.prevPage();
                selectFirstClue();
            }
            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            if (!board.getHighlightLetter().equals(last)) {
                board.moveZoneForward(false);
            } else {
                clueTabs.nextPage();
                selectFirstClue();
            }
            return true;

        case KeyEvent.KEYCODE_DPAD_UP:
            String prev
                = puz.getClues(curList)
                    .getPreviousClueNumber(curClueNumber, true);
            clueTabs.setForceSnap(true);
            board.jumpToClue(new ClueID(prev, curList));
            clueTabs.setForceSnap(false);
            break;

        case KeyEvent.KEYCODE_DPAD_DOWN:
            String next
                = puz.getClues(curList)
                    .getNextClueNumber(curClueNumber, true);
            clueTabs.setForceSnap(true);
            board.jumpToClue(new ClueID(next, curList));
            clueTabs.setForceSnap(false);
            break;

        case KeyEvent.KEYCODE_DEL:
            w = board.getCurrentWord();
            board.deleteLetter();

            Position p = board.getHighlightLetter();

            if (!w.checkInWord(p)) {
                board.setHighlightLetter(first);
            }

            return true;

        case KeyEvent.KEYCODE_SPACE:
            if (!prefs.getBoolean("spaceChangesDirection", true)) {
                board.playLetter(' ');

                Position curr = board.getHighlightLetter();
                int row = curr.getRow();
                int col = curr.getCol();

                if (!board.getCurrentWord().equals(w)
                        || (board.getBoxes()[row][col] == null)) {
                    board.setHighlightLetter(last);
                }
            }
            return true;
        }

        char c = Character.toUpperCase(event.getDisplayLabel());

        if (PlayActivity.ALPHA.indexOf(c) != -1) {
            board.playLetter(c);

            Position p = board.getHighlightLetter();
            int row = p.getRow();
            int col = p.getCol();

            if (!board.getCurrentWord().equals(w)
                    || (board.getBoxes()[row][col] == null)) {
                board.setHighlightLetter(last);
            }

            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();

        keyboardManager.onPause();

        if (clueTabs != null) {
            clueTabs.removeListener(this);
            clueTabs.unlistenBoard();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        keyboardManager.onDestroy();
    }

    private void displayKeyboard() {
        keyboardManager.showKeyboard(imageView);
    }

    private void displayKeyboard(Word previousWord) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Playboard board = getBoard();
        if (board != null) {
            Position newPos = board.getHighlightLetter();
            if ((previousWord != null) &&
                previousWord.checkInWord(newPos.getRow(), newPos.getCol())) {
                keyboardManager.showKeyboard(imageView);
            } else {
                keyboardManager.hideKeyboard();
            }
        }
    }

    private void render() {
        scaleRendererToCurWord();
        boolean displayScratch = prefs.getBoolean("displayScratch", false);
        Set<String> suppressNotesLists
            = displayScratch
            ? Collections.emptySet()
            : null;

        this.imageView.setBitmap(renderer.drawWord(suppressNotesLists));
        this.imageView.setContentDescription(
            renderer.getContentDescription(this.imageViewDescriptionBase)
        );
    }

    /**
     * Scale the current renderer to fit the length of the currently
     * selected word.
     */
    private void scaleRendererToCurWord() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int curWordLen = getBoard().getCurrentWord().getLength();
        if (curWordLen <= 0)
            return;
        double scale = this.renderer.fitWidthTo(
            metrics.widthPixels, curWordLen
        );
        if (scale > 1)
            this.renderer.setScale((float) 1);
    }

    private void selectFirstClue() {
        switch (clueTabs.getCurrentPageType()) {
        case CLUES:
            Playboard board = getBoard();
            Puzzle puz = board.getPuzzle();
            String listName = clueTabs.getCurrentPageListName();
            String firstClue = puz.getClues(listName).getFirstClueNumber();
            board.jumpToClue(new ClueID(firstClue, listName));
            break;
        case HISTORY:
            // nothing to do
            break;
        }
    }
}
