package app.crossword.yourealwaysbe;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.squareup.seismic.ShakeDetector;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.MovementStrategy;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.KeyboardManager;
import app.crossword.yourealwaysbe.util.VoiceCommands.VoiceCommand;
import app.crossword.yourealwaysbe.view.BoardEditView.BoardClickListener;
import app.crossword.yourealwaysbe.view.BoardEditView;
import app.crossword.yourealwaysbe.view.ClueTabs;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;
import app.crossword.yourealwaysbe.view.PuzzleInfoDialogs;
import app.crossword.yourealwaysbe.view.ScrollingImageView.ScaleListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;

public class PlayActivity extends PuzzleActivity
                          implements Playboard.PlayboardListener,
                                     ClueTabs.ClueTabsListener,
                                     ShakeDetector.Listener {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");
    private static final float BOARD_DIM_RATIO = 1.0F;
    private static final float ACROSTIC_BOARD_HEIGHT_RATIO_MIN = 0.25F;
    private static final float ACROSTIC_CLUE_TABS_WORD_SCALE = 0.7F;
    private static final float ACROSTIC_CLUE_TABS_HEIGHT_RATIO_MIN = 0.3F;
    private static final String SHOW_CLUES_TAB = "showCluesOnPlayScreen";
    private static final String CLUE_TABS_PAGE = "playActivityClueTabsPage";
    private static final String PREF_SHOW_ERRORS_GRID = "showErrors";
    private static final String PREF_SHOW_ERRORS_CURSOR = "showErrorsCursor";
    public static final String SHOW_TIMER = "showTimer";
    public static final String SCALE = "scale";
    private static final String PREF_RANDOM_CLUE_ON_SHAKE = "randomClueOnShake";

    private ClueTabs clueTabs;
    private ConstraintLayout constraintLayout;
    private Handler handler = new Handler(Looper.getMainLooper());
    private KeyboardManager keyboardManager;
    private MovementStrategy movement = null;
    private BoardEditView boardView;
    private TextView clue;
    private boolean hasInitialValues = false;
    private ShakeDetector shakeDetector = null;

    private Runnable fitToScreenTask = new Runnable() {
        @Override
        public void run() {
            PlayActivity.this.fitBoardToScreen();
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidthInInches = (metrics.widthPixels > metrics.heightPixels ? metrics.widthPixels : metrics.heightPixels) / Math.round(160 * metrics.density);
        LOG.info("Configuration Changed "+screenWidthInInches+" ");
        if(screenWidthInInches >= 7){
            this.handler.post(this.fitToScreenTask);
        }
    }

    /**
     * Create the activity
     *
     * This only sets up the UI widgets. The set up for the current
     * puzzle/board is done in onResume as these are held by the
     * application and may change while paused!
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.play);

        super.onCreate(savedInstanceState);

        utils.holographic(this);
        utils.finishOnHomeButton(this);

        setDefaultKeyMode(Activity.DEFAULT_KEYS_DISABLE);

        MovementStrategy movement = getMovementStrategy();

        setFullScreenMode();

        // board is loaded by BrowseActivity and put into the
        // Application, onResume sets up PlayActivity for current board
        // as it may change!
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (board == null || puz == null) {
            LOG.info("PlayActivity started but no Puzzle selected, finishing.");
            finish();
            return;
        }

        setContentView(R.layout.play);

        this.constraintLayout
            = (ConstraintLayout) this.findViewById(R.id.playConstraintLayout);

        this.clue = this.findViewById(R.id.clueLine);
        if (clue != null && clue.getVisibility() != View.GONE) {
            ConstraintSet set = new ConstraintSet();
            set.clone(constraintLayout);
            set.setVisibility(clue.getId(), ConstraintSet.GONE);
            set.applyTo(constraintLayout);

            View custom = utils.onActionBarCustom(this, R.layout.clue_line_only);
            if (custom != null) {
                clue = custom.findViewById(R.id.clueLine);
            }
        }

        this.boardView = (BoardEditView) this.findViewById(R.id.board);
        this.clueTabs = this.findViewById(R.id.playClueTab);

        this.clueTabs.setOnClueLongClickDescription(
            getString(R.string.open_clue_notes)
        );
        this.clueTabs.setOnClueClickDescription(
            getString(R.string.select_clue)
        );
        this.clueTabs.setOnBarLongClickDescription(
            getString(R.string.toggle_clue_tabs)
        );

        ForkyzKeyboard keyboardView
            = (ForkyzKeyboard) this.findViewById(R.id.keyboard);
        keyboardView.setSpecialKeyListener(
            new ForkyzKeyboard.SpecialKeyListener() {
                @Override
                public void onKeyDown(@ForkyzKeyboard.SpecialKey int key) {
                    // ignore
                }

                @Override
                public void onKeyUp(@ForkyzKeyboard.SpecialKey int key) {
                    // ignore
                    switch (key) {
                    case ForkyzKeyboard.KEY_CHANGE_CLUE_DIRECTION:
                        getBoard().toggleSelection();
                        return;
                    case ForkyzKeyboard.KEY_NEXT_CLUE:
                        getBoard().nextWord();
                        return;
                    case ForkyzKeyboard.KEY_PREVIOUS_CLUE:
                        getBoard().previousWord();
                        return;
                    default:
                        // ignore
                    }
                }
            }
        );

        keyboardManager = new KeyboardManager(this, keyboardView, boardView);

        board.setSkipCompletedLetters(
            this.prefs.getBoolean("skipFilled", false)
        );

        if(this.clue != null) {
            this.clue.setClickable(true);
            this.clue.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    if (PlayActivity.this.prefs.getBoolean(SHOW_CLUES_TAB, true)) {
                        PlayActivity.this.hideClueTabs();
                    } else {
                        PlayActivity.this.showClueTabs();
                    }
                }
            });
            ViewCompat.replaceAccessibilityAction(
                this.clue,
                AccessibilityActionCompat.ACTION_CLICK,
                getText(R.string.toggle_clue_tabs),
                null
            );
            this.clue.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View arg0) {
                    PlayActivity.this.launchClueList();
                    return true;
                }
            });
            ViewCompat.replaceAccessibilityAction(
                this.clue,
                AccessibilityActionCompat.ACTION_LONG_CLICK,
                getText(R.string.open_clue_list),
                null
            );
        }

        this.registerForContextMenu(boardView);
        boardView.addBoardClickListener(new BoardClickListener() {
            @Override
            public void onClick(Position position, Word previousWord) {
                displayKeyboard(previousWord);
            }

            @Override
            public void onLongClick(Position position) {
                Word w = board.setHighlightLetter(position);
                launchClueNotes(board.getClueID());
            }
        });
        ViewCompat.replaceAccessibilityAction(
            boardView,
            AccessibilityActionCompat.ACTION_LONG_CLICK,
            getText(R.string.open_clue_notes),
            null
        );

        // constrain to 1:1 if clueTabs is showing
        // or half of screen if acrostic
        boardView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v,
              int left, int top, int right, int bottom,
              int leftWas, int topWas, int rightWas, int bottomWas
            ) {
                boolean constrainedDims = false;

                ConstraintSet set = new ConstraintSet();
                set.clone(constraintLayout);

                boolean showCluesTab = PlayActivity.this.prefs.getBoolean(
                    SHOW_CLUES_TAB, true
                );

                if (showCluesTab) {
                    int height = bottom - top;
                    int width = right - left;

                    int orientation
                        = PlayActivity.this
                            .getResources()
                            .getConfiguration()
                            .orientation;

                    boolean portrait
                        = orientation == Configuration.ORIENTATION_PORTRAIT;

                    if (portrait) {
                        int maxHeight = 0;
                        if (isAcrostic()) {
                            DisplayMetrics metrics
                                = getResources().getDisplayMetrics();
                            maxHeight = (int)(
                                ACROSTIC_BOARD_HEIGHT_RATIO_MIN
                                    * metrics.heightPixels
                            );
                            Puzzle puz = getPuzzle();
                            if (puz != null) {
                                int proportionalHeight = (int)(
                                    ((float) puz.getHeight()) / puz.getWidth()
                                        * metrics.widthPixels
                                );
                                maxHeight = Math.max(
                                    maxHeight, proportionalHeight
                                );
                            }
                        } else {
                            maxHeight = (int)(BOARD_DIM_RATIO * width);
                        }

                        if (height > maxHeight) {
                            constrainedDims = true;
                            set.constrainMaxHeight(
                                boardView.getId(), maxHeight
                            );
                        }
                    }
                } else {
                    set.constrainMaxHeight(boardView.getId(), 0);
                }

                set.applyTo(constraintLayout);

                // if the view changed size, then rescale the view
                // cannot change layout during a layout change, so
                // use a predraw listener that requests a new layout
                // and returns false to cancel the current draw
                if (constrainedDims ||
                    left != leftWas || right != rightWas ||
                    top != topWas || bottom != bottomWas) {
                    boardView.getViewTreeObserver()
                             .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        public boolean onPreDraw() {
                            boardView.forceRedraw();
                            PlayActivity.this
                                        .boardView
                                        .getViewTreeObserver()
                                        .removeOnPreDrawListener(this);
                            return false;
                        }
                    });
                }
            }
        });

        this.boardView.setScaleListener(new ScaleListener() {
            public void onScale(float newScale) {
                prefs.edit().putFloat(SCALE, newScale).apply();
            }
        });

        int clueTextSize
            = getResources().getInteger(R.integer.clue_text_size);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            clue, 5, clueTextSize, 1, TypedValue.COMPLEX_UNIT_SP
        );

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (this.prefs.getBoolean("fitToScreen", false) || (ForkyzApplication.isLandscape(metrics)) && (ForkyzApplication.isTabletish(metrics) || ForkyzApplication.isMiniTabletish(metrics))) {
            this.handler.postDelayed(fitToScreenTask, 100);
        }

        setupVoiceButtons();
        setupVoiceCommands();

        addAccessibilityActions(this.boardView);
    }

    private void fitBoardToScreen() {
        float newScale = boardView.fitToView();
        prefs.edit().putFloat(SCALE, newScale).apply();
    }

    private static String neverNull(String val) {
        return val == null ? "" : val.trim();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.play_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (puz == null || puz.isUpdatable()) {
            menu.findItem(R.id.play_menu_reveal).setEnabled(false);
        } else {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            if (ForkyzApplication.isTabletish(metrics)) {
                menu.findItem(R.id.play_menu_reveal).setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            }
        }

        if (puz == null || puz.getSupportUrl() == null) {
            MenuItem support = menu.findItem(R.id.play_menu_support_source);
            support.setVisible(false);
            support.setEnabled(false);
        }

        menu.findItem(R.id.play_menu_scratch_mode).setChecked(isScratchMode());

        menu.findItem(R.id.play_menu_show_errors).setEnabled(
            !(puz == null || puz.isUpdatable())
        );

        boolean showErrorsGrid
            = this.prefs.getBoolean(PREF_SHOW_ERRORS_GRID, false);
        boolean showErrorsCursor
            = this.prefs.getBoolean(PREF_SHOW_ERRORS_CURSOR, false);

        int showErrorsTitle = (showErrorsGrid || showErrorsCursor)
            ? R.string.showing_errors
            : R.string.show_errors;

        menu.findItem(R.id.play_menu_show_errors)
            .setTitle(showErrorsTitle);

        menu.findItem(R.id.play_menu_show_errors_grid)
            .setChecked(showErrorsGrid);
        menu.findItem(R.id.play_menu_show_errors_cursor)
            .setChecked(showErrorsCursor);

        Box box = (board == null) ? null : board.getCurrentBox();
        boolean hasInitial = !Box.isBlock(box) && box.hasInitialValue();
        menu.findItem(R.id.play_menu_reveal_initial_letter)
            .setVisible(hasInitial);

        menu.findItem(R.id.play_menu_reveal_initial_letters)
            .setVisible(hasInitialValues);

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
        case KeyEvent.KEYCODE_SEARCH:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DPAD_CENTER:
        case KeyEvent.KEYCODE_SPACE:
        case KeyEvent.KEYCODE_ENTER:
        case KeyEvent.KEYCODE_DEL:
            return true;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            return isVolumeDownActivatesVoicePref();
        }

        char c = Character.toUpperCase(event.getDisplayLabel());
        if (Character.isLetterOrDigit(c))
            return true;

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        boolean handled = false;

        // handle back separately as it we shouldn't block a keyboard
        // hide because of it
        if (keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (!keyboardManager.handleBackKey()) {
                this.finish();
            }
            handled = true;
        }

        keyboardManager.pushBlockHide();

        if (getBoard() != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_SEARCH:
                    getBoard().nextWord();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    onDownKey();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DPAD_UP:
                    onUpKey();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    onLeftKey();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    onRightKey();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DPAD_CENTER:
                    getBoard().toggleSelection();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_SPACE:
                    if (prefs.getBoolean("spaceChangesDirection", true)) {
                        getBoard().toggleSelection();
                    } else if (isScratchMode()) {
                        getBoard().playScratchLetter(' ');
                    } else {
                        getBoard().playLetter(' ');
                    }
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_ENTER:
                    if (prefs.getBoolean("enterChangesDirection", true)) {
                        getBoard().toggleSelection();
                    } else {
                        getBoard().nextWord();
                    }
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_DEL:
                    onDeleteKey();
                    handled = true;
                    break;

                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    if (isVolumeDownActivatesVoicePref()) {
                        launchVoiceInput();
                        handled = true;
                    }
                    break;
            }

            char c = Character.toUpperCase(event.getDisplayLabel());

            if (!handled && Character.isLetterOrDigit(c)) {
                if (isScratchMode()) {
                    getBoard().playScratchLetter(c);
                } else {
                    getBoard().playLetter(c);
                }
                handled = true;
            }
        }

        if (!handled)
            handled = super.onKeyUp(keyCode, event);

        keyboardManager.popBlockHide();

        return handled;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        if (getBoard() != null) {
            if (id == R.id.play_menu_reveal_initial_letter) {
                getBoard().revealInitialLetter();
                return true;
            } if (id == R.id.play_menu_reveal_letter) {
                getBoard().revealLetter();
                return true;
            } else if (id == R.id.play_menu_reveal_word) {
                getBoard().revealWord();
                return true;
            } if (id == R.id.play_menu_reveal_initial_letters) {
                getBoard().revealInitialLetters();
                return true;
            } else if (id == R.id.play_menu_reveal_errors) {
                getBoard().revealErrors();
                return true;
            } else if (id == R.id.play_menu_reveal_puzzle) {
                showRevealPuzzleDialog();
                return true;
            } else if (id == R.id.play_menu_show_errors_grid) {
                getBoard().toggleShowErrorsGrid();
                this.prefs.edit().putBoolean(
                    PREF_SHOW_ERRORS_GRID, getBoard().isShowErrorsGrid()
                ).apply();
                invalidateOptionsMenu();
                return true;
            } else if (id == R.id.play_menu_show_errors_cursor) {
                getBoard().toggleShowErrorsCursor();
                this.prefs.edit().putBoolean(
                    PREF_SHOW_ERRORS_CURSOR, getBoard().isShowErrorsCursor()
                ).apply();
                invalidateOptionsMenu();
                return true;
            } else if (id == R.id.play_menu_scratch_mode) {
                toggleScratchMode();
                return true;
            } else if (id == R.id.play_menu_settings) {
                Intent i = new Intent(this, PreferencesActivity.class);
                this.startActivity(i);
                return true;
            } else if (id == R.id.play_menu_zoom_in) {
                float newScale = boardView.zoomIn();
                prefs.edit().putFloat(SCALE, newScale).apply();
                return true;
            } else if (id == R.id.play_menu_zoom_in_max) {
                float newScale = boardView.zoomInMax();
                this.prefs.edit().putFloat(SCALE, newScale).apply();
                return true;
            } else if (id == R.id.play_menu_zoom_out) {
                float newScale = boardView.zoomOut();
                this.prefs.edit().putFloat(SCALE, newScale).apply();
                return true;
            } else if (id == R.id.play_menu_zoom_fit) {
                fitBoardToScreen();
                return true;
            } else if (id == R.id.play_menu_zoom_reset) {
                float newScale = boardView.zoomReset();
                this.prefs.edit().putFloat(SCALE, newScale).apply();
                return true;
            } else if (id == R.id.play_menu_info) {
                showInfoDialog();
                return true;
            } else if (id == R.id.play_menu_clues) {
                PlayActivity.this.launchClueList();
                return true;
            } else if (id == R.id.play_menu_clue_notes) {
                launchClueNotes(getBoard().getClueID());
                return true;
            } else if (id == R.id.play_menu_player_notes) {
                launchPuzzleNotes();
                return true;
            } else if (id == R.id.play_menu_help) {
                Intent helpIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("file:///android_asset/playscreen.html"),
                    this,
                    HTMLActivity.class
                );
                this.startActivity(helpIntent);
                return true;
            } else if (id == R.id.play_menu_support_source) {
                actionSupportSource();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClueTabsClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board == null)
            return;
        onClueTabsClickGeneral(clue, board.getCurrentWord());
    }

    @Override
    public void onClueTabsBoardClick(
        Clue clue, Word previousWord, ClueTabs view
    ) {
        onClueTabsClickGeneral(clue, previousWord);
    }

    @Override
    public void onClueTabsLongClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board == null)
            return;
        board.jumpToClue(clue);
        launchClueNotes(clue);
    }

    @Override
    public void onClueTabsBarSwipeDown(ClueTabs view) {
        hideClueTabs();
    }

    @Override
    public void onClueTabsBarLongclick(ClueTabs view) {
        hideClueTabs();
    }

    @Override
    public void onClueTabsPageChange(ClueTabs view, int pageNumber) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(CLUE_TABS_PAGE, pageNumber);
        editor.apply();
    }

    public void onPlayboardChange(PlayboardChanges changes) {
        super.onPlayboardChange(changes);

        Word previousWord = changes.getPreviousWord();

        Position newPos = getBoard().getHighlightLetter();

        boolean isNewWord = (previousWord == null) ||
            !previousWord.checkInWord(newPos);

        if (isNewWord) {
            // hide keyboard when moving to a new word
            keyboardManager.hideKeyboard();
        }

        setClueText();

        // changed cells could mean change in reveal letters options
        if (hasInitialValues)
            invalidateOptionsMenu();
    }

    @Override
    protected void onTimerUpdate() {
        super.onTimerUpdate();

        Puzzle puz = getPuzzle();
        ImaginaryTimer timer = getTimer();

        if (puz != null && timer != null) {
            getWindow().setTitle(timer.time());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        keyboardManager.onPause();

        Playboard board = getBoard();
        if (board != null)
            board.removeListener(this);

        if (clueTabs != null) {
            clueTabs.removeListener(this);
            clueTabs.unlistenBoard();
        }

        pauseShakeDetection();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.onConfigurationChanged(getBaseContext().getResources()
                                                    .getConfiguration());

        if (prefs.getBoolean(SHOW_CLUES_TAB, false)) {
            showClueTabs();
        } else {
            hideClueTabs();
        }

        setVoiceButtonVisibility();
        registerBoard();

        if (keyboardManager != null)
            keyboardManager.onResume();

        handleFirstPlay();
        resumeShakeDetection();
    }

    private void registerBoard() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (board == null || puz == null) {
            LOG.info("PlayActivity resumed but no Puzzle selected, finishing.");
            finish();
            return;
        }

        setTitle(getString(
            R.string.play_activity_title,
            neverNull(puz.getTitle()),
            neverNull(puz.getAuthor()),
            neverNull(puz.getCopyright())
        ));

        boolean showErrorsGrid
            = this.prefs.getBoolean(PREF_SHOW_ERRORS_GRID, false);
        if (board.isShowErrorsGrid() != showErrorsGrid) {
            board.toggleShowErrorsGrid();
        }

        boolean showErrorsCursor
            = this.prefs.getBoolean(PREF_SHOW_ERRORS_CURSOR, false);
        if (board.isShowErrorsCursor() != showErrorsCursor) {
            board.toggleShowErrorsCursor();
        }

        if (boardView != null) {
            boardView.setBoard(board);

            float scale = prefs.getFloat(SCALE, 1.0F);
            scale = boardView.setCurrentScale(scale);
            prefs.edit().putFloat(SCALE, scale).apply();
        }

        if (clueTabs != null) {
            clueTabs.setBoard(board);
            clueTabs.setMaxWordScale(ACROSTIC_CLUE_TABS_WORD_SCALE);
            clueTabs.setShowWords(isAcrostic());
            clueTabs.setPage(prefs.getInt(CLUE_TABS_PAGE, 0));
            clueTabs.addListener(this);
            clueTabs.listenBoard();
            clueTabs.refresh();
        }

        board.setSkipCompletedLetters(
            this.prefs.getBoolean("skipFilled", false)
        );
        board.setMovementStrategy(this.getMovementStrategy());
        board.addListener(this);

        keyboardManager.attachKeyboardToView(boardView);

        setClueText();

        hasInitialValues = puz.hasInitialValueCells();
        // always invalidate as anything in puzzle could have changed
        invalidateOptionsMenu();
    }

    @Override
    public void hearShake() {
        if (isRandomClueOnShake())
            pickRandomUnfilledClue();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (keyboardManager != null)
            keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardManager != null)
            keyboardManager.onDestroy();
    }

    protected MovementStrategy getMovementStrategy() {
        if (movement != null) {
            return movement;
        } else {
            return ForkyzApplication.getInstance().getMovementStrategy();
        }
    }

    /**
     * Change keyboard display if the same word has been selected twice
     */
    private void displayKeyboard(Word previous) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Playboard board = getBoard();
        if (board != null) {
            Position newPos = board.getHighlightLetter();
            if ((previous != null) &&
                previous.checkInWord(newPos.getRow(), newPos.getCol())) {
                keyboardManager.showKeyboard(boardView);
            } else {
                keyboardManager.hideKeyboard();
            }
        }
    }

    private void setClueText() {
        Playboard board = getBoard();
        if (board == null)
            return;

        Clue c = board.getClue();
        clue.setText(smartHtml(getLongClueText(c)));
    }

    private void launchClueList() {
        Intent i = new Intent(this, ClueListActivity.class);
        PlayActivity.this.startActivity(i);
    }

    /**
     * Changes the constraints on clue tabs to show.
     *
     * Updates shared prefs.
     */
    private void showClueTabs() {
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.setVisibility(clueTabs.getId(), ConstraintSet.VISIBLE);
        if (isAcrostic()) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int minHeight = (int)(
                ACROSTIC_CLUE_TABS_HEIGHT_RATIO_MIN * metrics.heightPixels
            );
            set.constrainMinHeight(clueTabs.getId(), minHeight);
        }
        set.applyTo(constraintLayout);

        clueTabs.setPage(prefs.getInt(CLUE_TABS_PAGE, 0));

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SHOW_CLUES_TAB, true);
        editor.apply();
    }

    /**
     * Changes the constraints on clue tabs to hide.
     *
     * Updates shared prefs.
     */
    private void hideClueTabs() {
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.setVisibility(clueTabs.getId(), ConstraintSet.GONE);
        set.applyTo(constraintLayout);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SHOW_CLUES_TAB, false);
        editor.apply();
    }

    private void showInfoDialog() {
        DialogFragment dialog = new PuzzleInfoDialogs.Info();
        dialog.show(getSupportFragmentManager(), "PuzzleInfoDialgs.Info");
    }

    private void showRevealPuzzleDialog() {
        DialogFragment dialog = new RevealPuzzleDialog();
        dialog.show(getSupportFragmentManager(), "RevealPuzzleDialog");
    }

    private void setFullScreenMode() {
        if (prefs.getBoolean("fullScreen", false)) {
            utils.setFullScreen(getWindow());
        }
    }

    private void actionSupportSource() {
        Puzzle puz = getPuzzle();
        if (puz != null) {
            String supportUrl = puz.getSupportUrl();
            if (supportUrl != null) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(supportUrl));
                startActivity(i);
            }
        }
    }

    private boolean isScratchMode() {
        return this.prefs.getBoolean("scratchMode", false);
    }

    private void toggleScratchMode() {
        boolean scratchMode = isScratchMode();
        this.prefs.edit().putBoolean(
            "scratchMode", !scratchMode
        ).apply();
        invalidateOptionsMenu();
    }

    private void setupVoiceCommands() {
        registerVoiceCommandAnswer();
        registerVoiceCommandLetter();
        registerVoiceCommandNumber();
        registerVoiceCommandClear();
        registerVoiceCommandAnnounceClue();
        registerVoiceCommandClueHelp();

        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_delete),
            args -> { onDeleteKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_toggle),
            args -> {
                Playboard board = getBoard();
                if (board != null)
                    board.toggleSelection();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_next),
            args -> {
                Playboard board = getBoard();
                if (board != null)
                    board.nextWord();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_previous),
            args -> {
                Playboard board = getBoard();
                if (board != null)
                    board.previousWord();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_left),
            args -> { onLeftKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_right),
            args -> { onRightKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_up),
            args -> { onUpKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_down),
            args -> { onDownKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_back),
            args -> { onBackKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_clues),
            args -> { launchClueList(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_notes),
            args -> {
                Playboard board = getBoard();
                if (board != null)
                    launchClueNotes(board.getClueID());
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_jump_random),
            args -> { pickRandomUnfilledClue(); }
        ));
    }

    private void onLeftKey() {
        Playboard board = getBoard();
        if (board != null)
            board.moveLeft();
    }

    private void onRightKey() {
        Playboard board = getBoard();
        if (board != null)
            board.moveRight();
    }

    private void onDownKey() {
        Playboard board = getBoard();
        if (board != null)
            board.moveDown();
    }

    private void onUpKey() {
        Playboard board = getBoard();
        if (board != null)
            board.moveUp();
    }

    private void onBackKey() {
        if (!keyboardManager.handleBackKey())
            this.finish();
    }

    private void onDeleteKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (isScratchMode()) {
            board.deleteScratchLetter();
        } else {
            board.deleteLetter();
        }
    }

    private boolean isAcrostic() {
        Puzzle puz = getPuzzle();
        return puz == null
            ? false
            : Puzzle.Kind.ACROSTIC.equals(puz.getKind());
    }

    /**
     * Handle a click on the clue tabs
     *
     * @param clue the clue clicked
     * @param the previously selected word since last board update (a
     * clue tabs board click might have changed the word)
     */
    private void onClueTabsClickGeneral(Clue clue, Word previousWord) {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (clue.hasZone()) {
            if (!Objects.equals(clue.getClueID(), board.getClueID()))
                board.jumpToClue(clue);
            displayKeyboard(previousWord);
        }
    }

    private void handleFirstPlay() {
        if (!isFirstPlay())
            return;

        Puzzle puz = getPuzzle();
        if (puz == null || !puz.hasIntroMessage())
            return;

        DialogFragment dialog = new PuzzleInfoDialogs.Intro();
        dialog.show(getSupportFragmentManager(), "PuzzleInfoDialogs.Intro");
    }

    private boolean isRandomClueOnShake() {
        return prefs.getBoolean(PREF_RANDOM_CLUE_ON_SHAKE, false);
    }

    private void resumeShakeDetection() {
        if (!isRandomClueOnShake())
            return;

        if (shakeDetector == null) {
            shakeDetector = new ShakeDetector(this);
        }

        shakeDetector.start((SensorManager) getSystemService(SENSOR_SERVICE));
    }

    private void pauseShakeDetection() {
        if (shakeDetector != null)
            shakeDetector.stop();
    }

    private void pickRandomUnfilledClue() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        if (board == null || puz == null)
            return;

        ClueID currentID = board.getClueID();

        List<Clue> unfilledClues = new ArrayList<>();
        for (Clue clue : puz.getAllClues()) {
            ClueID cid = clue.getClueID();
            boolean current = Objects.equals(currentID, cid);

            if (!current && !board.isFilledClueID(clue.getClueID()))
                unfilledClues.add(clue);
        }

        if (unfilledClues.size() > 0) {
            // bit inefficient, but saves a field
            Random rand = new Random();
            int idx = rand.nextInt(unfilledClues.size());
            board.jumpToClue(unfilledClues.get(idx));
        }
    }

    public static class RevealPuzzleDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(getActivity());

            builder.setTitle(getString(R.string.reveal_puzzle))
                .setMessage(getString(R.string.are_you_sure))
                .setPositiveButton(
                    R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Playboard board
                                = ((PlayActivity) getActivity()).getBoard();
                            if (board != null)
                                 board.revealPuzzle();
                        }
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }
                );

            return builder.create();
        }
    }
}
