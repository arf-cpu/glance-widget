package dev.vro.glance;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

final class Render {
    static final String[] PRESETS = {"Wallpaper colors (Android 12+)", "Sunset", "Neon", "Ocean", "Forest", "Mono"};
    static final String[] FONTS = {"Bold", "Light", "Serif", "Mono"};
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

    static RemoteViews build(Context c, int wDp, int hDp) {
        SharedPreferences p = Prefs.get(c);
        int preset = p.getInt(Prefs.PRESET, 0);
        int font = p.getInt(Prefs.FONT, 0);
        int textMode = p.getInt(Prefs.TEXT, 0);
        int opacity = p.getInt(Prefs.OPACITY, 80);
        int radius = p.getInt(Prefs.RADIUS, 28);
        boolean night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        int start, end, accent, text;
        if (preset == 0 && Build.VERSION.SDK_INT >= 31) {
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
            text = Color.luminance(blend(start, end)) < 0.55f ? 0xFFFFFFFF : 0xFF141414;
        }
        if (textMode == 1) text = 0xFFFFFFFF;
        else if (textMode == 2) text = 0xFF141414;

        boolean h24 = p.getBoolean(Prefs.H24, false);
        boolean sec = p.getBoolean(Prefs.SECONDS, false);
        String timeFormat = (h24 ? "HH:mm" : "h:mm") + (sec ? ":ss" : "");

        int[] layouts = {R.layout.widget_bold, R.layout.widget_light, R.layout.widget_serif, R.layout.widget_mono};
        RemoteViews rv = new RemoteViews(c.getPackageName(), layouts[font]);

        rv.setImageViewBitmap(R.id.bg, card(c, wDp, hDp, start, end, opacity, radius));

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

    private static Bitmap card(Context c, int wDp, int hDp, int start, int end, int opacityPct, int radiusDp) {
        float s = Math.min(c.getResources().getDisplayMetrics().density, 2f);
        int w = Math.round(Math.min(Math.max(wDp, 100), 500) * s);
        int h = Math.round(Math.min(Math.max(hDp, 80), 500) * s);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        int a = Math.round(255f * opacityPct / 100f);
        float r = radiusDp * s;
        RectF rect = new RectF(0, 0, w, h);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setShader(new LinearGradient(0, 0, w, h, withAlpha(start, a), withAlpha(end, a), Shader.TileMode.CLAMP));
        cv.drawRoundRect(rect, r, r, fill);

        // glass sheen: light fading from the top edge
        int sheenAlpha = Math.round(0x3A * opacityPct / 100f);
        Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        sheen.setShader(new LinearGradient(0, 0, 0, h * 0.65f, (sheenAlpha << 24) | 0xFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        cv.drawRoundRect(rect, r, r, sheen);

        // thin light rim
        Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1.5f * s);
        rim.setColor(((Math.round(0x55 * opacityPct / 100f)) << 24) | 0xFFFFFF);
        float inset = 0.75f * s;
        cv.drawRoundRect(new RectF(inset, inset, w - inset, h - inset), r, r, rim);
        return b;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int blend(int a, int b) {
        return Color.argb(255,
                (Color.red(a) + Color.red(b)) / 2,
                (Color.green(a) + Color.green(b)) / 2,
                (Color.blue(a) + Color.blue(b)) / 2);
    }
}
