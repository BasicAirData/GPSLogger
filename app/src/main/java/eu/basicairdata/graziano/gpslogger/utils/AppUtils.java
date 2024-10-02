package eu.basicairdata.graziano.gpslogger.utils;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;

import androidx.preference.PreferenceManager;

public class AppUtils {

    /**
     * Updates the navigation bar color based on the provided color theme preference.
     * If prefColorTheme is "1" , only then the navigation bar color is updated to white color.
     *
     * @param window The Window object of the current activity or dialog where the navigation bar color will be updated.
     * @param applicationContext The application context, used to access shared preferences.
     */
    public static void updateNavigationBarColor(Window window, Context applicationContext) {
        if (window != null && applicationContext instanceof Application) {
            // Retrieve the preferred color theme from shared preferences and check if it's "1"
            if (TextUtils.equals("1", PreferenceManager.getDefaultSharedPreferences(applicationContext).getString("prefColorTheme", "2"))) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    window.setNavigationBarColor(Color.WHITE);
                    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
                }
            }
        }
    }
}
