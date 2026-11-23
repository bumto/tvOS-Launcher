package com.techgenius.tvoslauncher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class IconPacks {
    public static final String BUILTIN_CIRCO = "builtin:circo";

    private static final String PREF_NAME = "icon_pack";
    private static final String KEY_SELECTED = "selected";
    private static final String BUILTIN_ASSET_DIR = "iconpacks/circo/";
    private static final String[] THEME_ACTIONS = {
            "org.adw.ActivityStarter.THEMES",
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES"
    };

    private static final Map<String, Pack> PACK_CACHE = new HashMap<>();
    private static final Map<String, Map<String, String>> MAP_CACHE = new HashMap<>();
    private static final Map<String, Bitmap> ICON_CACHE = new HashMap<>();

    public static final class Pack {
        final String id;
        final String pkg;
        final String label;
        final boolean builtIn;

        Pack(String id, String pkg, String label, boolean builtIn) {
            this.id = id;
            this.pkg = pkg;
            this.label = label;
            this.builtIn = builtIn;
        }
    }

    private IconPacks() { }

    private static android.content.SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getSelected(Context c) {
        if (c == null) return "";
        return prefs(c).getString(KEY_SELECTED, BUILTIN_CIRCO);
    }

    public static void setSelected(Context c, String id) {
        if (c == null) return;
        prefs(c).edit().putString(KEY_SELECTED, id == null ? "" : id).apply();
        clearCaches();
        CustomMedia.clearIconCache();
    }

    public static boolean isActive(Context c) {
        return !getSelected(c).isEmpty();
    }

    public static void clearCaches() {
        synchronized (PACK_CACHE) { PACK_CACHE.clear(); }
        synchronized (MAP_CACHE) { MAP_CACHE.clear(); }
        synchronized (ICON_CACHE) { ICON_CACHE.clear(); }
    }

    public static String labelFor(Context c, String id) {
        if (id == null || id.isEmpty()) {
            return c == null ? "Original icons" : I18n.s(c, "Original icons", "Orijinal simgeler");
        }
        if (BUILTIN_CIRCO.equals(id)) return "Circo";
        if (c != null) {
            for (Pack p : list(c)) {
                if (id.equals(p.id)) return p.label;
            }
        }
        return id.startsWith("pkg:") ? id.substring(4) : id;
    }

    public static boolean hasIcon(Context c, String key, String pkg) {
        if (c == null || pkg == null || pkg.isEmpty() || pkg.equals(c.getPackageName())) return false;
        return loadPack(c, getSelected(c)) != null;
    }

    public static Bitmap getIcon(Context c, String key, String pkg) {
        if (!hasIcon(c, key, pkg)) return null;
        String cacheKey = safeKey(key) + "|" + safeKey(pkg);
        synchronized (ICON_CACHE) {
            Bitmap cached = ICON_CACHE.get(cacheKey);
            if (cached != null && !cached.isRecycled()) return cached;
        }
        Pack pack = loadPack(c, getSelected(c));
        if (pack == null) return null;
        Map<String, String> map = mappings(c, pack);
        String component = componentFromKey(key, pkg);
        String drawable = null;
        if (component != null) drawable = map.get(component);
        if (drawable == null) drawable = map.get("pkg:" + pkg);
        Bitmap icon = null;
        if (drawable != null) icon = loadDrawable(c, pack, drawable);
        if (icon == null) {
            Bitmap back = null;
            String backName = map.get("_iconback");
            if (backName != null) back = loadDrawable(c, pack, backName);
            float scale = 0.75f;
            try {
                String s = map.get("_scale");
                if (s != null) scale = Float.parseFloat(s);
            } catch (Throwable ignored) { }
            icon = maskApplicationIcon(c, pkg, back, scale);
        }
        if (icon != null) {
            synchronized (ICON_CACHE) { ICON_CACHE.put(cacheKey, icon); }
        }
        return icon;
    }

    public static List<Pack> list(Context c) {
        List<Pack> packs = new ArrayList<>();
        packs.add(new Pack(BUILTIN_CIRCO, null, "Circo", true));
        if (c == null) return packs;
        PackageManager pm = c.getPackageManager();
        Set<String> pkgs = new HashSet<>();
        for (String action : THEME_ACTIONS) {
            List<ResolveInfo> ris;
            try {
                ris = pm.queryIntentActivities(new Intent(action), 0);
            } catch (Throwable t) {
                ris = Collections.emptyList();
            }
            for (ResolveInfo ri : ris) {
                if (ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (pkg != null && !pkg.equals(c.getPackageName())) pkgs.add(pkg);
            }
        }
        for (String pkg : pkgs) {
            String label = pkg;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                CharSequence l = ai.loadLabel(pm);
                if (l != null && l.length() > 0) label = l.toString();
            } catch (Throwable ignored) { }
            packs.add(new Pack("pkg:" + pkg, pkg, label, false));
        }
        Collections.sort(packs, (a, b) -> {
            if (a.builtIn != b.builtIn) return a.builtIn ? -1 : 1;
            return a.label.compareToIgnoreCase(b.label);
        });
        return packs;
    }

    private static Pack loadPack(Context c, String id) {
        if (c == null || id == null || id.isEmpty()) return null;
        synchronized (PACK_CACHE) {
            Pack cached = PACK_CACHE.get(id);
            if (cached != null) return cached;
        }
        Pack found = null;
        for (Pack p : list(c)) {
            if (id.equals(p.id)) {
                found = p;
                break;
            }
        }
        if (found != null) {
            synchronized (PACK_CACHE) { PACK_CACHE.put(id, found); }
        }
        return found;
    }

    private static Map<String, String> mappings(Context c, Pack pack) {
        synchronized (MAP_CACHE) {
            Map<String, String> cached = MAP_CACHE.get(pack.id);
            if (cached != null) return cached;
        }
        Map<String, String> map = new HashMap<>();
        try {
            XmlPullParser parser = openAppFilter(c, pack);
            if (parser != null) {
                int type;
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (type != XmlPullParser.START_TAG) continue;
                    String tag = parser.getName();
                    if ("iconback".equals(tag)) {
                        String img = parser.getAttributeValue(null, "img1");
                        if (img != null) map.put("_iconback", img);
                    } else if ("scale".equals(tag)) {
                        String factor = parser.getAttributeValue(null, "factor");
                        if (factor != null) map.put("_scale", factor);
                    } else if ("item".equals(tag)) {
                        String component = parser.getAttributeValue(null, "component");
                        String drawable = parser.getAttributeValue(null, "drawable");
                        if (component == null || drawable == null) continue;
                        String normalized = normalizeComponent(component);
                        if (normalized != null) {
                            map.put(normalized, drawable);
                            int slash = normalized.indexOf('/');
                            if (slash > 0) {
                                String pkgOnly = "pkg:" + normalized.substring(0, slash);
                                if (!map.containsKey(pkgOnly)) map.put(pkgOnly, drawable);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            map.clear();
        }
        synchronized (MAP_CACHE) { MAP_CACHE.put(pack.id, map); }
        return map;
    }

    private static XmlPullParser openAppFilter(Context c, Pack pack) throws Exception {
        if (pack.builtIn) {
            XmlPullParser p = Xml.newPullParser();
            p.setInput(c.getAssets().open(BUILTIN_ASSET_DIR + "appfilter.xml"), "UTF-8");
            return p;
        }
        if (pack.pkg == null) return null;
        PackageManager pm = c.getPackageManager();
        Resources res = pm.getResourcesForApplication(pack.pkg);
        int xmlId = res.getIdentifier("appfilter", "xml", pack.pkg);
        if (xmlId != 0) return res.getXml(xmlId);
        int rawId = res.getIdentifier("appfilter", "raw", pack.pkg);
        if (rawId != 0) {
            XmlPullParser p = Xml.newPullParser();
            p.setInput(res.openRawResource(rawId), "UTF-8");
            return p;
        }
        try {
            InputStream in = res.getAssets().open("appfilter.xml");
            XmlPullParser p = Xml.newPullParser();
            p.setInput(in, "UTF-8");
            return p;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static String normalizeComponent(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        int a = s.indexOf('{');
        int b = s.lastIndexOf('}');
        if (a >= 0 && b > a) s = s.substring(a + 1, b);
        s = s.replace("ComponentInfo", "").trim();
        int slash = s.indexOf('/');
        if (slash <= 0) return null;
        String pkg = s.substring(0, slash).trim();
        String activity = s.substring(slash + 1).trim();
        if (activity.startsWith(".")) activity = pkg + activity;
        return pkg + "/" + activity;
    }

    private static String componentFromKey(String key, String pkg) {
        if (pkg == null || pkg.isEmpty()) return null;
        if (key == null || key.isEmpty()) return pkg;
        int slash = key.indexOf('/');
        if (slash <= 0) return pkg;
        String activity = key.substring(slash + 1).trim();
        if (activity.startsWith(".")) activity = pkg + activity;
        return pkg + "/" + activity;
    }

    private static String safeKey(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private static Bitmap loadDrawable(Context c, Pack pack, String name) {
        String safe = name.replaceAll("[^a-zA-Z0-9_.-]", "_");
        if (pack.builtIn) {
            try (InputStream in = c.getAssets().open(BUILTIN_ASSET_DIR + safe + ".png")) {
                Bitmap b = BitmapFactory.decodeStream(in);
                return b != null && !b.isRecycled() ? b : null;
            } catch (Throwable t) {
                return null;
            }
        }
        if (pack.pkg == null) return null;
        try {
            Resources res = c.getPackageManager().getResourcesForApplication(pack.pkg);
            int id = res.getIdentifier(safe, "drawable", pack.pkg);
            if (id == 0) id = res.getIdentifier(safe, "mipmap", pack.pkg);
            if (id == 0) return null;
            Drawable d = res.getDrawable(id, null);
            return d == null ? null : drawableToBitmap(d, 256);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap maskApplicationIcon(Context c, String pkg, Bitmap back, float scale) {
        if (c == null || pkg == null || pkg.isEmpty()) return null;
        try {
            ApplicationInfo ai = c.getPackageManager().getApplicationInfo(pkg, 0);
            Drawable d = ai.loadIcon(c.getPackageManager());
            if (d == null) return null;
            Bitmap src = drawableToBitmap(d, 256);
            return composeCircularIcon(src, back, 192, scale);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap drawableToBitmap(Drawable d, int size) {
        if (d instanceof BitmapDrawable) {
            Bitmap b = ((BitmapDrawable) d).getBitmap();
            if (b != null && !b.isRecycled() && b.getWidth() > 0 && b.getHeight() > 0) return b;
        }
        int w = d.getIntrinsicWidth();
        int h = d.getIntrinsicHeight();
        if (w <= 0 || h <= 0) {
            w = size;
            h = size;
        }
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        float scale = Math.min(size / (float) w, size / (float) h);
        int dw = Math.max(1, Math.round(w * scale));
        int dh = Math.max(1, Math.round(h * scale));
        int left = (size - dw) / 2;
        int top = (size - dh) / 2;
        d.setBounds(left, top, left + dw, top + dh);
        d.draw(cv);
        return out;
    }

    private static Bitmap composeCircularIcon(Bitmap src, Bitmap back, int size, float scale) {
        if (src == null || src.isRecycled()) return null;
        Bitmap trimmed = CustomMedia.trimTransparentPadding(src);
        if (trimmed == null || trimmed.isRecycled() || trimmed.getWidth() <= 0 || trimmed.getHeight() <= 0) {
            trimmed = src;
        }
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        float cx = size / 2f;
        float cy = size / 2f;
        float radius = size * 0.48f;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        if (back != null && !back.isRecycled()) {
            cv.drawBitmap(back, null, new android.graphics.RectF(0, 0, size, size), paint);
        } else {
            paint.setColor(0xFF323232);
            cv.drawCircle(cx, cy, radius, paint);
        }
        Path clip = new Path();
        clip.addCircle(cx, cy, radius, Path.Direction.CW);
        cv.save();
        cv.clipPath(clip);
        float target = size * Math.max(0.35f, Math.min(1f, scale));
        float s = target / Math.max(trimmed.getWidth(), trimmed.getHeight());
        float dw = trimmed.getWidth() * s;
        float dh = trimmed.getHeight() * s;
        cv.drawBitmap(trimmed, null, new android.graphics.RectF(cx - dw / 2f, cy - dh / 2f, cx + dw / 2f, cy + dh / 2f), paint);
        cv.restore();
        return out;
    }
}
