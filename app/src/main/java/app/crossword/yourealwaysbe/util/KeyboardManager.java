/**
 * Manage on screen keyboard for Play/Notes/ClueList activity (and others)
 */

package app.crossword.yourealwaysbe.util;

import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.preference.PreferenceManager;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.view.ForkyzKeyboard;

public class KeyboardManager {
    private static final Logger LOG = Logger.getLogger(KeyboardManager.class.getCanonicalName());

    private Activity activity;
    private SharedPreferences prefs;
    private ForkyzKeyboard keyboardView;
    private int blockHideDepth = 0;

    private enum KeyboardMode {
        ALWAYS_SHOW, HIDE_MANUAL, SHOW_SPARINGLY, NEVER_SHOW
    }

    /**
     * Create a new manager to handle the keyboard
     *
     * To use, pass on calls to the implemented methods below.
     *
     * @param activity the activity the keyboard is for
     * @param keyboardView the keyboard view of the activity
     * @param initialView the initial view the keyboard should be
     * attached to if always shown or null if none
     */
    public KeyboardManager(
        Activity activity, ForkyzKeyboard keyboardView, View initialView
    ) {
        this.activity = activity;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        this.keyboardView = keyboardView;

        // make sure showing, then hide if appropriate
        showKeyboard(initialView);
        hideKeyboard();
    }

    /**
     * Call this from the activities onResume method
     */
    public void onResume() {
        setHideRowVisibility();

        if (isNativeKeyboard())
            keyboardView.setVisibility(View.GONE);
    }

    /**
     * Call this when the activity receives an onPause
     */
    public void onPause() {
        keyboardView.onPause();
    }

    /**
     * Call this when the activity receives an onStop
     */
    public void onStop() { }

    /**
     * Call this when the activity receives an onDestroy
     */
    public void onDestroy() { }

    /**
     * Show the keyboard -- must be called after UI drawn
     *
     * @param view the view the keyboard should work for, will request
     * focus
     */
    public void showKeyboard(View view) {
        if (getKeyboardMode() != KeyboardMode.NEVER_SHOW
                && view != null
                && view.requestFocus()) {
            if (isNativeKeyboard()) {
                InputMethodManager imm
                    = (InputMethodManager)
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
                keyboardView.setVisibility(View.GONE);
            } else {
                keyboardView.setVisibility(View.VISIBLE);
                keyboardView.attachToView(view);
            }
        }
    }

    /**
     * Attach the keyboard to a view without changing visibilty
     */
    public void attachKeyboardToView(View view) {
        keyboardView.attachToView(view);
    }

    public boolean hideKeyboard() { return hideKeyboard(false); }

    /**
     * Hide the keyboard unless the user always wants it
     *
     * Will not hide if the user is currently pressing a key
     *
     * @param force force hide the keyboard, even if user has set always
     * show
     * @return true if the hide request was not blocked by settings or
     * pushBlockHide
     */
    public boolean hideKeyboard(boolean force) {
        KeyboardMode mode = getKeyboardMode();
        boolean prefHide =
            mode != KeyboardMode.ALWAYS_SHOW
                && mode != KeyboardMode.HIDE_MANUAL;
        boolean softHide =
            prefHide && !keyboardView.hasKeysDown() && !isBlockHide();
        boolean doHide = force || softHide;

        if (doHide) {
            if (isNativeKeyboard()) {
                View focus = activity.getCurrentFocus();
                if (focus != null) {
                    InputMethodManager imm
                        = (InputMethodManager) activity.getSystemService(
                            Context.INPUT_METHOD_SERVICE
                        );
                    imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
            } else {
                keyboardView.setVisibility(View.GONE);
            }
        }

        return doHide;
    }

    /**
     * Handle back key
     *
     * Hides keyboard if mode allows it.
     *
     * @return true if key press was consumed, false if it should be
     * passed on
     */
    public boolean handleBackKey() {
        boolean force = getKeyboardMode() != KeyboardMode.ALWAYS_SHOW;
        boolean toHide =
            keyboardView.getVisibility() == View.VISIBLE
                && !isKeyboardHideButton();
        return toHide && hideKeyboard(force);
    }

    /**
     * Add a block hide request
     *
     * hideKeyboard will only have an effect if there are no block hide
     * requests (or force was passed to hideKeyboard)
     */
    public void pushBlockHide() { blockHideDepth++; }

    /**
     * Remove a block hide request
     */
    public void popBlockHide() { blockHideDepth--; }

    private boolean isBlockHide() { return blockHideDepth > 0; }

    private KeyboardMode getKeyboardMode() {
        String never = activity.getString(R.string.keyboard_never_show);
        String back = activity.getString(R.string.keyboard_hide_manual);
        String spare = activity.getString(R.string.keyboard_show_sparingly);
        String always = activity.getString(R.string.keyboard_always_show);

        String modePref = prefs.getString("keyboardShowHide", back);

        if (never.equals(modePref))
            return KeyboardMode.NEVER_SHOW;
        else if (back.equals(modePref))
            return KeyboardMode.HIDE_MANUAL;
        else if (always.equals(modePref))
            return KeyboardMode.ALWAYS_SHOW;
        else
            return KeyboardMode.SHOW_SPARINGLY;
    }

    private void setHideRowVisibility() {
        if (isKeyboardHideButton()) {
            KeyboardMode mode = getKeyboardMode();
            keyboardView.setShowHideButton(
                mode == KeyboardMode.HIDE_MANUAL
                    || mode == KeyboardMode.SHOW_SPARINGLY
            );
        } else {
            keyboardView.setShowHideButton(false);
        }
    }

    private boolean isKeyboardHideButton() {
        return prefs.getBoolean("keyboardHideButton", false);
    }

    private boolean isNativeKeyboard() {
        return prefs.getBoolean("useNativeKeyboard", false);
    }
}
