package app.crossword.yourealwaysbe;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jg.wordstonumbers.WordsToNumbersUtil;

import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.net.ChatGPTHelp;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.util.SpeechContract;
import app.crossword.yourealwaysbe.util.VoiceCommands.VoiceCommand;
import app.crossword.yourealwaysbe.util.VoiceCommands;
import app.crossword.yourealwaysbe.util.files.FileHandlerShared;
import app.crossword.yourealwaysbe.util.files.PuzHandle;
import app.crossword.yourealwaysbe.view.PlayboardTextRenderer;
import app.crossword.yourealwaysbe.view.PuzzleInfoDialogs;
import app.crossword.yourealwaysbe.view.SpecialEntryDialog;

import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public abstract class PuzzleActivity
        extends ForkyzActivity
        implements Playboard.PlayboardListener {

    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    public static final String SHOW_TIMER = "showTimer";
    public static final String PRESERVE_CORRECT
        = "preserveCorrectLettersInShowErrors";
    public static final String DONT_DELETE_CROSSING = "dontDeleteCrossing";
    private static final String PREF_VOLUME_ACTIVATES_VOICE
        = "volumeActivatesVoice";
    private static final String PREF_BUTTON_ACTIVATES_VOICE
        = "buttonActivatesVoice";
    private static final String PREF_BUTTON_ANNOUNCE_CLUE
        = "buttonAnnounceClue";
    private static final String PREF_EQUALS_ANNOUNCE_CLUE
        = "equalsAnnounceClue";
    private static final String PREF_ALWAYS_ANNOUNCE_CLUE
        = "alwaysAnnounceClue";
    private static final String PREF_ALWAYS_ANNOUNCE_BOX
        = "alwaysAnnounceBox";

    private static final String HELP_RESPONSE_TEXT = "helpResponse";

    private boolean firstPlay = false;
    private ImaginaryTimer timer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private PlayboardTextRenderer textRenderer;
    private View voiceButtonContainer;
    private View voiceButton;
    private View announceClueButton;
    private TextToSpeech ttsService = null;
    private boolean ttsReady = false;

    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            PuzzleActivity.this.onTimerUpdate();
        }
    };

    private VoiceCommands voiceCommandDispatcher = new VoiceCommands();
    private ActivityResultLauncher<String> voiceInputLauncher
        = registerForActivityResult(new SpeechContract(), text -> {
            voiceCommandDispatcher.dispatch(text);
        });

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        startTimer();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!super.onPrepareOptionsMenu(menu))
            return false;

        Puzzle puz = getPuzzle();
        String shareUrl = puz == null ? null : puz.getShareUrl();
        if (shareUrl == null || shareUrl.isEmpty()) {
            MenuItem open = menu.findItem(R.id.puzzle_menu_open_share_url);
            open.setVisible(false);
            open.setEnabled(false);
        }

        if (!canRequestHelpForCurrentClue()) {
            MenuItem help = menu.findItem(R.id.puzzle_menu_ask_chat_gpt);
            help.setVisible(false);
            help.setEnabled(false);
        }

        return true;
    }

    private void startTimer() {
        Puzzle puz = getPuzzle();

        if (puz != null && puz.getPercentComplete() != 100) {
            ImaginaryTimer timer = new ImaginaryTimer(puz.getTime());
            setTimer(timer);
            timer.start();

            if (prefs.getBoolean(SHOW_TIMER, false)) {
                handler.post(updateTimeTask);
            }
        }
    }

    public ImaginaryTimer getTimer() {
        return timer;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (prefs.getBoolean(SHOW_TIMER, false)) {
            handler.post(updateTimeTask);
        }

        Playboard board = getBoard();
        if (board != null) {
            boolean preserveCorrect = prefs.getBoolean(PRESERVE_CORRECT, true);
            board.setPreserveCorrectLettersInShowErrors(preserveCorrect);
            boolean noDelCrossing = prefs.getBoolean(DONT_DELETE_CROSSING, false);
            board.setDontDeleteCrossing(noDelCrossing);
        }
    }

    @Override
    public void onPlayboardChange(PlayboardChanges changes) {
        handleChangeTimer();
        handleChangeAccessibility(changes);
        handleChangeChatGPT(changes);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.puzzle_menu_special_entry) {
            specialEntry();
            return true;
        } else if (id == R.id.puzzle_menu_share_clue) {
            shareClue(false);
        } else if (id == R.id.puzzle_menu_share_clue_response) {
            shareClue(true);
        } else if (id == R.id.puzzle_menu_share_puzzle_full) {
            sharePuzzle(false);
        } else if (id == R.id.puzzle_menu_share_puzzle_orig) {
            sharePuzzle(true);
        } else if (id == R.id.puzzle_menu_open_share_url) {
            openShareUrl();
        } else if (id == R.id.puzzle_menu_ask_chat_gpt) {
            requestHelpForCurrentClue();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Puzzle puz = getPuzzle();
        ImaginaryTimer timer = getTimer();

        if ((puz != null)) {
            if ((timer != null) && (puz.getPercentComplete() != 100)) {
                timer.stop();
                puz.setTime(timer.getElapsed());
                setTimer(null);
            }

            saveBoard();
        }

        Playboard board = getBoard();
        if (board != null)
            board.removeListener(this);

        if (ttsService != null) {
            ttsService.shutdown();
            ttsService = null;
            ttsReady = false;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        ImaginaryTimer timer = getTimer();
        if (timer != null)
            timer.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Puzzle puz = getPuzzle();
        if (puz != null && puz.getPercentComplete() != 100) {
            long time = puz.getTime();
            firstPlay = (time == 0);
            ImaginaryTimer timer = new ImaginaryTimer(time);
            setTimer(timer);
            timer.start();
        }

        if (prefs.getBoolean(SHOW_TIMER, false)) {
            handler.post(updateTimeTask);
        }

        Playboard board = getBoard();
        if (board != null)
            board.addListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        ImaginaryTimer timer = getTimer();
        if (timer != null) {
            timer.stop();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_EQUALS:
            if (isAnnounceClueEquals())
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_EQUALS:
            if (isAnnounceClueEquals()) {
                announceClue(false);
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Override if you want to update your UI based on the timer
     *
     * But still call super. Only called if the showTimer pref is true
     */
    protected void onTimerUpdate() {
        if (prefs.getBoolean(SHOW_TIMER, false)) {
            handler.postDelayed(updateTimeTask, 1000);
        }
    }

    protected Playboard getBoard(){
        return ForkyzApplication.getInstance().getBoard();
    }

    protected Puzzle getPuzzle() {
        Playboard board = getBoard();
        return (board == null) ? null : getBoard().getPuzzle();
    }

    protected boolean isFirstPlay() {
        return firstPlay;
    }

    protected void setTimer(ImaginaryTimer timer) {
        this.timer = timer;
    }

    protected PuzHandle getPuzHandle() {
        return ForkyzApplication.getInstance().getPuzHandle();
    }

    protected void saveBoard() {
        ForkyzApplication.getInstance().saveBoard();
    }

    protected String getLongClueText(Clue clue) {
        boolean showCount = prefs.getBoolean("showCount", false);
        return PlayboardTextRenderer.getLongClueText(this, clue, showCount);
    }

    protected void launchClueNotes(ClueID cid) {
        if (cid != null) {
            Intent i = new Intent(this, NotesActivity.class);
            i.putExtra(NotesActivity.CLUE_NOTE_LISTNAME, cid.getListName());
            i.putExtra(NotesActivity.CLUE_NOTE_INDEX, cid.getIndex());
            this.startActivity(i);
        } else {
            launchPuzzleNotes();
        }
    }

    protected void launchClueNotes(Clue clue) {
        if (clue != null)
            launchClueNotes(clue.getClueID());
        else
            launchPuzzleNotes();
    }

    protected void launchPuzzleNotes() {
        Intent i = new Intent(this, NotesActivity.class);
        this.startActivity(i);
    }

    private boolean canRequestHelpForCurrentClue() {
        Playboard board = getBoard();
        Clue clue = board == null ? null : board.getClue();
        return ChatGPTHelp.isEnabled(this) && clue != null;
    }

    private void requestHelpForCurrentClue() {
        if (!utils.hasNetworkConnection(this)) {
            Toast t = Toast.makeText(
                this,
                R.string.help_query_but_no_active_network,
                Toast.LENGTH_LONG
            );
            t.show();
        } else if (canRequestHelpForCurrentClue()) {
            ChatGPTHelp helper = new ChatGPTHelp();
            helper.requestHelpForCurrentClue(
                this,
                getBoard(),
                (String response) -> {
                    HelpResponseDialog dialog = new HelpResponseDialog();
                    Bundle args = new Bundle();
                    args.putString(HELP_RESPONSE_TEXT, response);
                    dialog.setArguments(args);
                    dialog.show(getSupportFragmentManager(), "HelpResponseDialog");
                }
            );
        }
    }

    private void specialEntry() {
        SpecialEntryDialog dialog = new SpecialEntryDialog();
        dialog.show(getSupportFragmentManager(), "SpecialEntryDialog");
    }

    private void shareClue(boolean withResponse) {
        Playboard board = getBoard();
        Clue clue = (board == null) ? null : board.getClue();
        if (clue == null)
            return;

        Puzzle puz = board.getPuzzle();
        String source = (puz == null) ? null : puz.getSource();
        String title = (puz == null) ? null : puz.getTitle();
        String author = (puz == null) ? null : puz.getAuthor();
        Box[] response = board.getCurrentWordBoxes();

        String shareMessage = getShareMessage(
            puz, clue, response, withResponse
        );

        // ShareCompat from
        // https://stackoverflow.com/a/39619468/6882587
        // assume works better than the out-of-date android docs!
        Intent shareIntent = new ShareCompat.IntentBuilder(this)
            .setText(shareMessage)
            .setType("text/plain")
            .setChooserTitle(getString(R.string.share_clue_title))
            .createChooserIntent();

        startActivity(shareIntent);
    }

    private String getShareMessage(
        Puzzle puz, Clue clue, Box[] response, boolean withResponse
    ) {
        String clueText = getShareClueText(clue);
        String responseText = withResponse
            ? getShareResponseText(response)
            : null;

        String puzzleDetails = getSharePuzzleDetails(puz);

        if (withResponse) {
            return getString(
                R.string.share_clue_response_text,
                clueText, responseText, puzzleDetails
            );
        } else {
            return getString(
                R.string.share_clue_text,
                clueText, puzzleDetails
            );
        }
    }

    private String getShareResponseText(Box[] boxes) {
        StringBuilder responseText = new StringBuilder();
        if (boxes != null) {
            for (Box box : boxes) {
                if (box.isBlank()) {
                    responseText.append(
                        getString(R.string.share_clue_blank_box)
                    );
                } else {
                    responseText.append(box.getResponse());
                }
            }
        }
        return responseText.toString();
    }

    protected String getShareClueText(Clue clue) {
        boolean showCount = prefs.getBoolean("showCount", false);

        if (clue == null)
            return getString(R.string.unknown_hint);

        int wordLen = clue.hasZone() ? clue.getZone().size() : -1;

        if (showCount && wordLen >= 0) {
            return getString(
                R.string.clue_format_short_no_num_no_dir_with_count,
                clue.getHint(),
                wordLen
            );
        } else {
            return clue.getHint();
        }
    }

    protected void launchVoiceInput() {
        try {
            voiceInputLauncher.launch(
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );
        } catch (ActivityNotFoundException e) {
            Toast t = Toast.makeText(
                this,
                R.string.no_speech_recognition_available,
                Toast.LENGTH_LONG
            );
            t.show();
        }
    }

    protected void registerVoiceCommand(@NonNull VoiceCommand command) {
        voiceCommandDispatcher.registerVoiceCommand(command);
    }

    /**
     * Prepared command for inputting word answers
     */
    protected void registerVoiceCommandAnswer() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_answer),
            getString(R.string.command_answer_alt),
            answer -> {
                Playboard board = getBoard();
                if (board == null)
                    return;

                // remove non-word as not usually entered into grids
                String prepped
                    = answer.replaceAll("\\W+", "")
                        .toUpperCase(Locale.getDefault());
                board.playAnswer(prepped);
            }
        ));
    }

    /**
     * Prepared command for inputting letters
     */
    protected void registerVoiceCommandLetter() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_letter),
            letter -> {
                if (letter == null || letter.isEmpty())
                    return;
                Playboard board = getBoard();
                if (board != null)
                    board.playLetter(Character.toUpperCase(letter.charAt(0)));
            }
        ));
    }

    /**
     * Prepared command for jumping to clue number
     */
    protected void registerVoiceCommandNumber() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_number),
            textNumber -> {
                Playboard board = getBoard();
                if (board == null)
                    return;

                String prepped
                    = WordsToNumbersUtil.convertTextualNumbersInDocument(textNumber);
                try {
                    int number = Integer.parseInt(prepped);
                    board.jumpToClue(String.valueOf(number));
                } catch (NumberFormatException e) {
                    board.jumpToClue(textNumber);
                }
            }
        ));
    }

    /**
     * Prepared command for clearing current word
     */
    protected void registerVoiceCommandClear() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_clear),
            args -> { getBoard().clearWord(); }
        ));
    }

    /**
     * Prepared command for announcing current clue
     */
    protected void registerVoiceCommandAnnounceClue() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_announce_clue),
            args -> { announceClue(false); }
        ));
    }

    /**
     * Prepared command for announcing current clue
     */
    protected void registerVoiceCommandClueHelp() {
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_current_clue_help),
            args -> { requestHelpForCurrentClue(); }
        ));
    }

    private String getSharePuzzleDetails(Puzzle puz) {
        if (puz == null)
            return "";

        String source = puz.getSource();
        String title = puz.getTitle();
        String author = puz.getAuthor();

        if (source == null)
            source = "";
        if (title == null)
            title = "";
        if (author != null) {
            // add author if not already in title or caption
            // case insensitive trick:
            // https://www.baeldung.com/java-case-insensitive-string-matching
            String quotedAuthor = Pattern.quote(author);
            boolean removeAuthor
                = author.isEmpty()
                    || title.matches("(?i).*" + quotedAuthor + ".*")
                    || source.matches("(?i).*" + quotedAuthor + ".*");

            if (removeAuthor)
                author = null;
        }

        String shareUrl = puz.getShareUrl();

        if (shareUrl == null || shareUrl.isEmpty()) {
            return (author != null)
                ? getString(
                    R.string.share_puzzle_details_author_no_url,
                    source, title, author
                ) : getString(
                    R.string.share_puzzle_details_no_author_no_url,
                    source, title
                );
        } else {
            return (author != null)
                ? getString(
                    R.string.share_puzzle_details_author_url,
                    source, title, author, shareUrl
                ) : getString(
                    R.string.share_puzzle_details_no_author_url,
                    source, title, shareUrl
                );
        }
    }

    private void sharePuzzle(boolean writeOriginal) {
        final Puzzle puz = getPuzzle();
        if (puz == null)
            return;

        FileHandlerShared.getShareUri(
            getApplicationContext(), getPuzzle(), writeOriginal,
            (puzUri) -> {
                String mimeType = FileHandlerShared.getShareUriMimeType();
                String puzzleDetails = getSharePuzzleDetails(puz);

                Intent shareIntent = new ShareCompat.IntentBuilder(this)
                    .setStream(puzUri)
                    .setType(mimeType)
                    .setChooserTitle(getString(R.string.share_puzzle_title))
                    .createChooserIntent();

                startActivity(shareIntent);
            }
        );
    }

    private void openShareUrl() {
        Puzzle puz = getPuzzle();
        if (puz != null) {
            String shareUrl = puz.getShareUrl();
            if (shareUrl != null && !shareUrl.isEmpty()) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(shareUrl));
                startActivity(i);
            }
        }
    }

    protected boolean isVolumeDownActivatesVoicePref() {
        return prefs.getBoolean(PREF_VOLUME_ACTIVATES_VOICE, false);
    }

    /**
     * Whether to show an on-screen button to activate voice
     */
    protected boolean isButtonActivatesVoicePref() {
        return prefs.getBoolean(PREF_BUTTON_ACTIVATES_VOICE, false);
    }

    /**
     * Whether to show an on-screen button to activate voice
     */
    protected boolean isButtonAnnounceCluePref() {
        return prefs.getBoolean(PREF_BUTTON_ANNOUNCE_CLUE, false);
    }

    protected void setupVoiceButtons() {
        this.voiceButtonContainer
            = this.findViewById(R.id.voiceButtonContainer);
        this.voiceButton
            = this.findViewById(R.id.voiceButton);
        this.announceClueButton
            = this.findViewById(R.id.announceClueButton);
        voiceButton.setOnClickListener(view -> {
            launchVoiceInput();
        });
        announceClueButton.setOnClickListener(view -> {
            announceClue(false);
        });
    }

    protected void setVoiceButtonVisibility() {
        boolean anyButton
            = isButtonActivatesVoicePref() || isButtonAnnounceCluePref();

        voiceButtonContainer.setVisibility(
            anyButton ? View.VISIBLE : View.GONE
        );
        voiceButton.setVisibility(
            isButtonActivatesVoicePref() ? View.VISIBLE : View.GONE
        );
        announceClueButton.setVisibility(
            isButtonAnnounceCluePref() ? View.VISIBLE : View.GONE
        );
    }

    /**
     * Announce clue
     *
     * With accessibility service if available.
     */
    protected void announceClue(boolean onlyIfAccessibilityService) {
        Playboard board = getBoard();
        if (board == null)
            return;

        boolean showCount = prefs.getBoolean("showCount", false);
        CharSequence clue = PlayboardTextRenderer.getAccessibleCurrentClueWord(
            this, board, showCount
        );

        if (clue != null)
            announceText(clue, onlyIfAccessibilityService);
    }

    /**
     * Announce box
     *
     * With accessibility service if available.
     */
    protected void announceBox(boolean onlyIfAccessibilityService) {
        Playboard board = getBoard();
        if (board == null)
            return;

        CharSequence box =
            PlayboardTextRenderer.getAccessibleCurrentBox(this, board);

        if (box != null)
            announceText(box, onlyIfAccessibilityService);
    }

    /**
     * Add available accessibility actions to view
     *
     * Just announce clue for now
     */
    protected void addAccessibilityActions(View view) {
        ViewCompat.addAccessibilityAction(
            view,
            getString(R.string.announce_clue_label),
            (View v, AccessibilityViewCommand.CommandArguments arguments) -> {
                announceClue(true);
                return true;
            }
        );
    }

    protected boolean isAnnounceClueEquals() {
        return prefs.getBoolean(PREF_EQUALS_ANNOUNCE_CLUE, true);
    }

    private void handleChangeTimer() {
        Puzzle puz = getPuzzle();
        if (puz == null)
            return;

        ImaginaryTimer timer = getTimer();
        if (puz != null &&
            puz.getPercentComplete() == 100 &&
            timer != null) {

            timer.stop();
            puz.setTime(timer.getElapsed());
            setTimer(null);

            DialogFragment dialog = new PuzzleInfoDialogs.Finished();
            dialog.show(
                getSupportFragmentManager(),
                "PuzzleInfoDialogs.Finished"
            );
        }
    }

    private boolean isAccessibilityServiceRunning() {
        AccessibilityManager manager
            = (AccessibilityManager) getSystemService(
                Context.ACCESSIBILITY_SERVICE
            );
        return manager != null && manager.isEnabled();
    }

    private boolean isAlwaysAnnounceClue() {
        return prefs.getBoolean(PREF_ALWAYS_ANNOUNCE_CLUE, false);
    }

    private boolean isAlwaysAnnounceBox() {
        return prefs.getBoolean(PREF_ALWAYS_ANNOUNCE_BOX, false);
    }

    private void handleChangeAccessibility(PlayboardChanges changes) {
        boolean accessibilityRunning = isAccessibilityServiceRunning();
        boolean announceClue = accessibilityRunning || isAlwaysAnnounceClue();
        boolean announceBox = accessibilityRunning || isAlwaysAnnounceBox();

        if (!announceClue && !announceBox)
            return;

        Playboard board = getBoard();
        if (board == null)
            return;

        Position newPos = board.getHighlightLetter();
        boolean isNewWord = !Objects.equals(
            changes.getPreviousWord(), changes.getCurrentWord()
        );
        boolean isNewPosition
            = !Objects.equals(changes.getPreviousPosition(), newPos);

        if (isNewWord && announceClue)
            announceClue(!isAlwaysAnnounceClue());
        else if (isNewPosition && announceBox)
            announceBox(!isAlwaysAnnounceBox());
    }

    /**
     * Announce text with accessibility if running or tts
     */
    private void announceText(
        CharSequence text, boolean onlyIfAccessibilityService
    ) {
        if (isAccessibilityServiceRunning()) {
            utils.announceForAccessibility(
                findViewById(android.R.id.content), text
            );
        } else if (!onlyIfAccessibilityService) {
            if (ttsService == null) {
                ttsService = new TextToSpeech(
                    getApplicationContext(),
                    (int status) -> {
                        if (status == TextToSpeech.SUCCESS) {
                            ttsReady = true;
                            announceText(text, onlyIfAccessibilityService);
                        }
                    }
                );
            } else if (!ttsReady) {
                // hopefully rare occasion where tts being prepared but not
                // ready yet
                Toast t = Toast.makeText(
                    this,
                    R.string.speech_not_ready,
                    Toast.LENGTH_SHORT
                );
                t.show();
            } else {
                utils.speak(ttsService, text);
            }
        }
    }

    private void handleChangeChatGPT(PlayboardChanges changes) {
        if (!ChatGPTHelp.isEnabled(this))
            return;

        boolean newWord = !Objects.equals(
            changes.getPreviousWord(), changes.getCurrentWord()
        );

        if (newWord)
            invalidateOptionsMenu();
    }

    public static class HelpResponseDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = getActivity();

            String response = getArguments().getString(HELP_RESPONSE_TEXT);
            if (response == null)
                response = activity.getString(R.string.help_query_failed);

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(activity);

            builder.setTitle(
                activity.getString(R.string.help_query_response_title)
            ).setMessage(response)
                .setPositiveButton(R.string.ok, null);

            return builder.create();
        }
    }
}
