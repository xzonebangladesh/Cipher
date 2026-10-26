package com.xz.cipherlock;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Centralizes the small bits of persistent state the app needs to share
 * between MainActivity, the accessibility service and the notification
 * receiver. Using SharedPreferences (instead of a static field) means the
 * "explicit exit" flag survives the app's process being killed in the
 * background, which is what makes the "don't reopen after I pressed exit"
 * behaviour reliable overnight.
 */
public final class PrefsHelper {

    private static final String PREFS_NAME = "cipher_prefs";
    private static final String KEY_EXPLICIT_EXIT = "explicit_exit";
    private static final String KEY_LOCK_AT = "lock_at";

    private PrefsHelper() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** True once the parent/child has pressed the in-app "exit" action. */
    public static boolean isExplicitExit(Context context) {
        return prefs(context).getBoolean(KEY_EXPLICIT_EXIT, false);
    }

    public static void setExplicitExit(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_EXPLICIT_EXIT, value).apply();
    }

    /** Epoch millis at which the screen should relock, or 0 if none set. */
    public static long getLockAt(Context context) {
        return prefs(context).getLong(KEY_LOCK_AT, 0L);
    }

    public static void setLockAt(Context context, long epochMillis) {
        prefs(context).edit().putLong(KEY_LOCK_AT, epochMillis).apply();
    }

    public static void clearLockAt(Context context) {
        prefs(context).edit().remove(KEY_LOCK_AT).apply();
    }
}
