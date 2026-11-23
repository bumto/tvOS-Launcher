package com.techgenius.tvoslauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class IconSize {
    public static final String ACTION_CHANGED = "com.techgenius.tvoslauncher.ICON_SIZE_CHANGED";
    public static final int SMALL = 85;
    public static final int MEDIUM = 100;
    public static final int LARGE = 115;
    public static final int EXTRA_LARGE = 130;

    private static final String PREFS = "icon_size";
    private static final String KEY = "scale_pct";

    private IconSize() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int get(Context c) {
        return prefs(c).getInt(KEY, MEDIUM);
    }

    public static void set(Context c, int value) {
        prefs(c).edit().putInt(KEY, value).apply();
        c.sendBroadcast(new Intent(ACTION_CHANGED).setPackage(c.getPackageName()));
    }

    public static float factor(Context c) {
        return get(c) / 100f;
    }
}
