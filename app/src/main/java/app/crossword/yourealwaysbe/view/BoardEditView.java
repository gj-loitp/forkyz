
package app.crossword.yourealwaysbe.view;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.util.BoxInputConnection;
import app.crossword.yourealwaysbe.util.KeyboardManager;

/**
 * A live view of the playboard
 *
 * BoardEditText is an edit text that looks like the board.
 *
 * Renders the playboard on change and implements input connection with
 * soft-input.
 */
public abstract class BoardEditView
        extends ScrollingImageView
        implements Playboard.PlayboardListener,
                    BoxInputConnection.BoxInputListener,
                    KeyboardManager.ManageableView {
    private static final int DOUBLE_CLICK_INTERVAL = 300; // ms

    private SharedPreferences prefs;
    private Playboard board;
    private boolean wordView;
    private PlayboardRenderer renderer;
    private Set<BoardClickListener> clickListeners = new HashSet<>();
    private long lastTap = 0;
    private BoxInputConnection currentInputConnection = null;
    private boolean nativeInput = false;

    public interface BoardClickListener {
        default void onClick(Position position, Word previousWord) { }
        default void onLongClick(Position position) { }
    }

    public BoardEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setBoard(Playboard board) {
        setBoard(board, false);
    }

    /**
     * Set the base board for the edit view
     *
     * Use noRender if you want to make more changes before rendering
     * Resets min/max scale as this depends on the board for some reason.
     * Set board to null to "deactivate" this view (i.e. stop it
     * drawing bitmaps on board changes).
     */
    public void setBoard(Playboard board, boolean noRender) {
        if (this.board != null)
            this.board.removeListener(this);

        if (board == null) {
            this.board =  null;
            // free up renderer and bitmap
            this.renderer = null;
        } else {
            this.board = board;

            DisplayMetrics metrics
                = getContext().getResources().getDisplayMetrics();

            renderer = new PlayboardRenderer(
                board,
                metrics.densityDpi,
                metrics.widthPixels,
                !prefs.getBoolean("supressHints", false),
                getContext()
            );
            setMaxScale(renderer.getMaxScale());
            setMinScale(renderer.getMinScale());

            float scale = getCurrentScale();

            // reset scale in case it violates new board dims
            setCurrentScale(scale, true);

            if (!noRender)
                render(true);

            // TODO: needed?
            //setFocusable(true);

            // don't worry about unlistening because Playboard keeps a
            // weak set.
            board.addListener(this);
        }
    }

    /**
     * Detach view from board (and stop listening/updating)
     */
    public void detach() {
        setBoard(null);
    }

    @Override
    public void setMaxScale(float maxScale) {
        super.setMaxScale(maxScale);
        if (renderer != null)
            renderer.setMaxScale(maxScale);
    }

    @Override
    public void setMinScale(float minScale) {
        super.setMinScale(minScale);
        if (renderer != null)
            renderer.setMinScale(minScale);
    }

    /**
     * Add listener for clicks on board positions
     *
     * Does not store in a weak set, so listener will survive as long as
     * view does
     */
    public void addBoardClickListener(BoardClickListener listener) {
        clickListeners.add(listener);
    }

    public void removeBoardClickListener(BoardClickListener listener) {
        clickListeners.remove(listener);
    }

    @Override
    public float setCurrentScale(float scale) {
        return setCurrentScale(scale, false);
    }

    public float setCurrentScale(float scale, boolean noRender) {
        float oldScale = getCurrentScale();

        if (renderer != null) {
            if (scale > renderer.getMaxScale()) {
                scale = renderer.getMaxScale();
            } else if (scale < renderer.getMinScale()) {
                scale = renderer.getMinScale();
            } else if (Float.isNaN(scale)) {
                scale = 1F;
            }

            renderer.setScale(scale);
        }

        super.setCurrentScale(scale);
        if (!noRender && Float.compare(scale, oldScale) != 0) {
            render(true);
        }

        return scale;
    }

    public abstract Position findPosition(Point point);

    /**
     * Returns new scale
     */
    public abstract float fitToView();

    /**
     * Returns new scale
     */
    public float zoomIn() {
        float newScale = renderer.zoomIn();
        setCurrentScale(newScale);
        return newScale;
    }

    /**
     * Returns new scale
     */
    public float zoomInMax() {
        float newScale = renderer.zoomInMax();
        setCurrentScale(newScale);
        return newScale;
    }

    /**
     * Returns new scale
     */
    public float zoomOut() {
        float newScale = renderer.zoomOut();
        setCurrentScale(newScale);
        return newScale;
    }

    /**
     * Returns new scale
     */
    public float zoomReset() {
        float newScale = renderer.zoomReset();
        setCurrentScale(newScale);
        return newScale;
    }

    @Override
    public void onPlayboardChange(PlayboardChanges changes) {
        if (currentInputConnection != null)
            currentInputConnection.setResponse(getCurrentResponse());
    }

    @Override
    public void setNativeInput(boolean nativeInput) {
        this.nativeInput = nativeInput;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return nativeInput;
    }

    // Set input type to be raw keys if a keyboard is used
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        currentInputConnection = new BoxInputConnection(
            this,
            getCurrentResponse(),
            this
        );
        currentInputConnection.setOutAttrs(outAttrs);
        return currentInputConnection;
    }

    @Override
    public InputConnection onCreateForkyzInputConnection(EditorInfo outAttrs) {
        BaseInputConnection fic = new BaseInputConnection(this, false);
        outAttrs.inputType = InputType.TYPE_NULL;
        return fic;
    }

    // This method is a hack needed in PlayActivity when clue tabs are
    // shown. The constrain of the board to 1:1 seems to have no effect
    // until render(true) is called.
    public void forceRedraw() {
        render(true);
    }

    @Override
    protected void onScale(float scale, Point center) {
        lastTap = System.currentTimeMillis();

        int w = getImageView().getWidth();
        int h = getImageView().getHeight();
        scale = renderer.fitTo(w, h);

        super.onScale(scale, center);

        render(true);
    }

    @Override
    protected void onContextMenu(Point point) {
        super.onContextMenu(point);
        Position position = findPosition(point);
        if (position != null) {
            for (BoardClickListener listener : clickListeners) {
                listener.onLongClick(position);
            }
        }
    }

    @Override
    protected void onTap(Point point) {
        super.onTap(point);

        requestFocus();

        boolean doubleTapOn = prefs.getBoolean("doubleTap", false);
        long clickInterval = System.currentTimeMillis() - lastTap;

        if (doubleTapOn && clickInterval < DOUBLE_CLICK_INTERVAL) {
            fitToView();
            notifyScaleChange(getCurrentScale());
        } else {
            Position position = findPosition(point);
            if (position != null)
                onClick(position);
        }

        lastTap = System.currentTimeMillis();
    }

    /**
     * Click on a board position
     *
     * Note, position may not be in a word on the board, or indeed in
     * the board itself. Will be checked (by concrete implementations)
     * to see if it is an actual cell click.
     */
    abstract protected void onClick(Position position);

    protected void notifyClick(Position position, Word previousWord) {
        for (BoardClickListener listener : clickListeners) {
            listener.onClick(position, previousWord);
        }
    }

    protected void render() {
        render(false);
    }

    protected void render(Collection<Position> changes) {
        render(changes, false);
    }

    protected void render(boolean rescale) {
        render(null, rescale);
    }

    /**
     * Render board
     *
     * @param changes collection of positions on the board that have
     * changed. Null means render all again.
     */
    abstract protected void render(Collection<Position> changes, boolean rescale);

    protected PlayboardRenderer getRenderer() { return renderer; }
    protected Playboard getBoard() { return board; }
    protected SharedPreferences getPrefs() { return prefs; }

    /**
     * What scratch to suppress when rendering
     */
    abstract protected Set<String> getSuppressNotesList();

    private String getCurrentResponse() {
        Playboard board = getBoard();
        Box box = (board == null) ? null : board.getCurrentBox();
        return (box == null)
            ? Box.BLANK
            : box.getResponse();
    }
}
