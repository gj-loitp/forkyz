package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class JellyBeanMR1Util extends IceCreamSandwichUtil {
    @Override
    public boolean isMiniTabletish(DisplayMetrics metrics) {
        // I don't know why this turned to false at this point, but it's
        // how the code was from the very start, even when targeting
        // SDK 21.
        return false;
    }

    @Override
    public void announceForAccessibility(View view, CharSequence text) {
        if (view != null && text != null)
            view.announceForAccessibility(text);
    }
}
