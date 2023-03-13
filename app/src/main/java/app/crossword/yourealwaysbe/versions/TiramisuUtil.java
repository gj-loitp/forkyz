
package app.crossword.yourealwaysbe.versions;

import java.io.Serializable;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager.PackageInfoFlags;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;

@TargetApi(Build.VERSION_CODES.TIRAMISU)
public class TiramisuUtil extends RUtil {
    @Override
    public <T extends Serializable>
    T getSerializable(Bundle bundle, String key, Class<T> klass) {
        return bundle.getSerializable(key, klass);
    }

    @Override
    public void requestPostNotifications(
        ActivityResultLauncher<String> launcher
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    @Override
    public boolean shouldShowRequestNotificationPermissionRationale(
        Activity activity
    ) {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.POST_NOTIFICATIONS
        );
    }

    @Override
    public void invalidateInput(InputMethodManager imm, View view) {
        imm.invalidateInput(view);
    }

    @Override
    public String getApplicationVersionName(Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), PackageInfoFlags.of(0));
            return info.versionName;
        } catch (NameNotFoundException e) {
            return null;
        }
    }
}
