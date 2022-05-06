package app.crossword.yourealwaysbe.versions;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.text.StaticLayout;
import android.text.TextPaint;

import app.crossword.yourealwaysbe.ForkyzActivity;
import app.crossword.yourealwaysbe.util.NightModeHelper;

public interface AndroidVersionUtils {

    void setContext(Context ctx);

    void finishOnHomeButton(AppCompatActivity a);

    void holographic(AppCompatActivity playActivity);

    void onActionBarWithText(MenuItem a);

    void onActionBarWithText(SubMenu reveal);

    void restoreNightMode(ForkyzActivity forkyzActivity);

    void restoreNightMode(NightModeHelper nightMode);

    class Factory {
        private static AndroidVersionUtils INSTANCE;

        public static AndroidVersionUtils getInstance() {
            if(INSTANCE != null){
                return INSTANCE;
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return INSTANCE = new OreoUtil();
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return INSTANCE = new MarshmallowUtil();
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return INSTANCE = new LollipopUtil();
            }
            else {
                return INSTANCE = new HoneycombUtil();
            }
        }
    }

    View onActionBarCustom(AppCompatActivity a, int id);

    void hideWindowTitle(AppCompatActivity a);

    void hideActionBar(AppCompatActivity a);

    void onActionBarWithoutText(MenuItem a);

    void hideTitleOnPortrait(AppCompatActivity a);

    void nextNightMode(ForkyzActivity activity);

    boolean isNightModeAvailable();

    // This has a dependency on JobScheduler which is only available in SDK version 21.
    //
    // TODO: It might be possible to replicate this functionality on older versions using
    // AlarmManager.
    boolean isBackgroundDownloadAvaliable();

    // Checks whether a background download may have updated the available puzzles, requiring a
    // UI refresh.
    boolean checkBackgroundDownload(SharedPreferences prefs, boolean hasWritePermissions);

    void clearBackgroundDownload(SharedPreferences prefs);

    void createNotificationChannel(Context context);

    int immutablePendingIntentFlag();

    StaticLayout getStaticLayout(CharSequence text, TextPaint style, int width);
}
