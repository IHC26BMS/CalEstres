package com.example.calestres;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;

final class ThemeMode {
    private static final String PREFERENCES = "cal_estres_settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    private ThemeMode() {
    }

    static void applySavedMode(Context context) {
        AppCompatDelegate.setDefaultNightMode(isDark(context)
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
    }

    static void attach(Activity activity, int buttonId) {
        TextView button = activity.findViewById(buttonId);
        updateButton(activity, button);
        button.setOnClickListener(view -> {
            boolean darkMode = !isDark(activity);
            activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_DARK_MODE, darkMode)
                    .apply();
            AppCompatDelegate.setDefaultNightMode(darkMode
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    private static boolean isDark(Context context) {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false);
    }

    private static void updateButton(Activity activity, TextView button) {
        boolean darkMode = isDark(activity);
        button.setText(darkMode ? "\u2600" : "\u263E");
        button.setContentDescription(activity.getString(darkMode
                ? R.string.theme_switch_to_light
                : R.string.theme_switch_to_dark));
    }
}
