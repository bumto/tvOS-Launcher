package com.techgenius.tvoslauncher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class IconShape {
    public static final String ACTION_CHANGED = "com.techgenius.tvoslauncher.ICON_SHAPE_CHANGED";
    public static final String CIRCLE = "circle";
    public static final String RECTANGLE = "rectangle";
    public static final String SQUIRCLE = "squircle";
    public static final String TV_RECTANGLE = "tv_rectangle";

    private static final String PREFS = "icon_shape";
    private static final String KEY = "shape";

    private IconShape() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String get(Context c) {
        return prefs(c).getString(KEY, SQUIRCLE);
    }

    public static void set(Context c, String value) {
        prefs(c).edit().putString(KEY, value).apply();
        c.sendBroadcast(new Intent(ACTION_CHANGED).setPackage(c.getPackageName()));
    }

    public static float aspect(Context c) {
        return TV_RECTANGLE.equals(get(c)) ? 0.5625f : 1f;
    }

    public static float radius(Context c, float size) {
        String shape = get(c);
        if (CIRCLE.equals(shape)) return size / 2f;
        if (RECTANGLE.equals(shape)) return size * 0.10f;
        if (TV_RECTANGLE.equals(shape)) return size * 0.14f;
        return size * 0.22f;
    }
}
