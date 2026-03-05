package com.example.screenshoot.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SoundPrefs {

    private static final String PREFS = "sound_prefs";
    private static final String KEY_COUNTDOWN = "countdown_sound"; // "off", "soft", "loud"
    private static final String KEY_DELETE = "delete_sound";       // "off", "soft", "loud"

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void setCountdownSound(Context ctx, String value) {
        prefs(ctx).edit().putString(KEY_COUNTDOWN, value).apply();
    }

    public static void setDeleteSound(Context ctx, String value) {
        prefs(ctx).edit().putString(KEY_DELETE, value).apply();
    }

    public static String getCountdownSound(Context ctx) {
        return prefs(ctx).getString(KEY_COUNTDOWN, "soft");
    }

    public static String getDeleteSound(Context ctx) {
        return prefs(ctx).getString(KEY_DELETE, "soft");
    }
}

