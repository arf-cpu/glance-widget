package dev.vro.glance;

import android.app.PendingIntent;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;

final class Render {
    static final String[] PRESETS = {"Wallpaper colors (Android 12+)", "Sunset", "Neon", "Ocean", "Forest", "Mono"};
    static final String[] TEXTS = {"Auto", "Light", "Dark"};

    // gradient start, gradient end, accent (index 0 is a placeholder for wallpaper colors)
    private static final int[][] FIXED = {
            {0, 0, 0},
            {0xFFFF512F, 0xFFDD2476, 0xFFFFE29F},
            {0xFF1A0033, 0xFF6A00F4, 0xFFFF2BD6},
            {0xFF0575E6, 0xFF021B79, 0xFF9BF6FF},
            {0xFF0B3D2E, 0xFF2E8B57, 0xFFCCFF90},
            {0xFF232526, 0xFF414345, 0xFFFFFFFF}
    };

    // ---------- fonts: four built-in system ones, bundled ones, and Anurati if the add-on was installed ----------

    private static int anuratiLayout(Context c) {
        return c.getResources().getIdentifier("widget_anurati", "layout", c.getPackageName());
    }

    static String[] fontNames(Context c) {
        boolean extra = anuratiLayout(c) != 0;
        String[] out = new String[Layouts.NAMES.length + (extra ? 1 : 0)];
        System.arraycopy(Layouts.NAMES, 0, out, 0, Layouts.NAMES.length);
        if (extra) out[out.length - 1] = "Anurati";
        return out;
    }

    static int fontCount(Context c) {
        return Layouts.NAMES.length + (anuratiLayout(c) != 0 ? 1 : 0);
    }

    private static int fontLayout(Context c, int idx) {
        return idx < Layouts.IDS.length ? Layouts.IDS[idx] : anuratiLayout(c);
    }

    static int clampFont(Context c, int idx) {
        return Math.max(0, Math.min(idx, fontCount(c) - 1));
    }

    /**
     * trial = true is used by the settings preview: it uses the chosen font even if it has not been proven to load yet.
     * Otherwise a bundled font is only used after the preview has loaded it successfully once.
     */
    static RemoteViews build(Context c, int wDp, int hDp, boolean trial) {
        SharedPreferences p = Prefs.get(c);
        int preset = Math.max(0, Math.min(p.getInt(Prefs.PRESET, 0), PRESETS.length - 1));
        int fontIdx = clampFont(c, p.getInt(Prefs.FONT, 0));
        int textMode = p.getInt(Prefs.TEXT, 0);
        int opacity = p.getInt(Prefs.OPACITY, 80);
        int bgPct = p.getBoolean(Prefs.BG, true) ? opacity : 0;
        int radius = p.getInt(Prefs.RADIUS, 28);
        boolean progress = p.getBoolean(Prefs.PROGRESS, true);
        boolean night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        boolean dynamic = preset == 0 && Build.VERSION.SDK_INT >= 31;

        int start, end, accent, text;
        if (dynamic) {
            if (night) {
                start = c.getColor(android.R.color.system_accent1_700);
                end = c.getColor(android.R.color.system_accent2_900);
                accent = c.getColor(android.R.color.system_accent1_100);
                text = c.getColor(android.R.color.system_neutral1_50);
            } else {
                start = c.getColor(android.R.color.system_accent1_100);
                end = c.getColor(android.R.color.system_accent2_200);
                accent = c.getColor(android.R.color.system_accent1_700);
                text = c.getColor(android.R.color.system_neutral1_900);
            }
        } else {
            int[] f = FIXED[preset == 0 ? 3 : preset];
            start = f[0];
            end = f[1];
            accent = f[2];
            text = Color.luminance(Art.mix(start, end, 0.5f)) < 0.55f ? 0xFFFFFFFF : 0xFF141414;
        }

        // With little or no card behind the text, follow the wallpaper instead of the card colors.
        if (bgPct < 35) {
            boolean lightWall = wallpaperIsLight(c);
            text = lightWall ? 0xFF141414 : 0xFFFFFFFF;
            if (dynamic) {
                accent = c.getColor(lightWall ? android.R.color.system_accent1_700 : android.R.color.system_accent1_100);
            } else if (lightWall) {
                accent = Art.mix(accent, 0xFF000000, 0.45f);
            }
        }
        if (textMode == 1) text = 0xFFFFFFFF;
        else if (textMode == 2) text = 0xFF141414;

        Calendar cal = Calendar.getInstance();
        float day = (cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)) / 86400f;

        int layout = fontLayout(c, fontIdx);
        if (fontIdx >= Layouts.SYSTEM_COUNT && !trial && !p.getBoolean(Prefs.FONT_OK + fontIdx, false)) {
            layout = Layouts.IDS[0];
        }
        RemoteViews rv = new RemoteViews(c.getPackageName(), layout);

        rv.setImageViewBitmap(R.id.bg, Art.draw(c, wDp, hDp, start, end, accent, text, bgPct, radius, progress, day));
        float d = c.getResources().getDisplayMetrics().density;
        rv.setViewPadding(R.id.content, Math.round(20 * d), Math.round(12 * d), Math.round(20 * d),
                Math.round((progress ? 34 : 12) * d));

        boolean h24 = p.getBoolean(Prefs.H24, false);
        boolean sec = p.getBoolean(Prefs.SECONDS, false);
        String timeFormat = (h24 ? "HH:mm" : "h:mm") + (sec ? ":ss" : "");
        rv.setCharSequence(R.id.clock, "setFormat12Hour", timeFormat);
        rv.setCharSequence(R.id.clock, "setFormat24Hour", timeFormat);
        rv.setCharSequence(R.id.date, "setFormat12Hour", "EEEE, d MMMM");
        rv.setCharSequence(R.id.date, "setFormat24Hour", "EEEE, d MMMM");

        rv.setTextColor(R.id.clock, text);
        rv.setTextColor(R.id.date, accent);
        rv.setTextColor(R.id.weather, text);

        if (p.getBoolean(Prefs.WEATHER, true)) {
            rv.setViewVisibility(R.id.weather, View.VISIBLE);
            rv.setTextViewText(R.id.weather, weatherLine(p));
        } else {
            rv.setViewVisibility(R.id.weather, View.GONE);
        }

        Intent open = new Intent(c, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.root, pi);
        return rv;
    }

    /** Small views showing an error message, so a failure is visible instead of a silently missing widget. */
    static RemoteViews error(Context c, Throwable t) {
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_error);
        String msg = t.getClass().getSimpleName() + ": " + t.getMessage();
        rv.setTextViewText(R.id.error_text, "Glance Widget error\n" + msg);
        return rv;
    }

    private static boolean wallpaperIsLight(Context c) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(c);
            WallpaperColors wc = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
            if (wc == null) return false;
            if (Build.VERSION.SDK_INT >= 31) {
                return (wc.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;
            }
            return Color.luminance(wc.getPrimaryColor().toArgb()) > 0.6f;
        } catch (Exception e) {
            return false;
        }
    }

    private static String weatherLine(SharedPreferences p) {
        if (!p.contains(Prefs.LAT)) return "Tap to set your city";
        if (!p.getBoolean(Prefs.HAS, false)) return "Loading weather\u2026";
        boolean f = p.getBoolean(Prefs.FAHR, false);
        return Weather.icon(p.getInt(Prefs.CODE, 0)) + " " + temp(p.getFloat(Prefs.TEMP, 0f), f)
                + "   \u2191" + temp(p.getFloat(Prefs.HI, 0f), f)
                + " \u2193" + temp(p.getFloat(Prefs.LO, 0f), f);
    }

    private static String temp(float celsius, boolean fahrenheit) {
        float v = fahrenheit ? celsius * 9f / 5f + 32f : celsius;
        return Math.round(v) + "\u00B0";
    }
}
