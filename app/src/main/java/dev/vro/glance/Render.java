package dev.vro.glance;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class Render {
    static final String[] THEMES = {"Glow", "Orbit", "Poster"};
    static final String[] PRESETS = {"Wallpaper colors (Android 12+)", "Sunset", "Neon", "Ocean", "Forest", "Mono", "Aurora", "Candy"};
    static final String[] FONTS = {"Poppins Bold", "Poppins Thin", "Michroma (wide tech)", "DM Serif", "Bebas Neue", "Major Mono"};
    static final String[] TEXTS = {"Auto", "Light", "Dark"};

    // gradient start, gradient end, accent (index 0 is a placeholder for wallpaper colors)
    private static final int[][] FIXED = {
            {0, 0, 0},
            {0xFFFF512F, 0xFFDD2476, 0xFFFFE29F},
            {0xFF1A0033, 0xFF6A00F4, 0xFFFF2BD6},
            {0xFF0575E6, 0xFF021B79, 0xFF9BF6FF},
            {0xFF0B3D2E, 0xFF2E8B57, 0xFFCCFF90},
            {0xFF232526, 0xFF414345, 0xFFFFFFFF},
            {0xFF0F9B8E, 0xFF5B2A86, 0xFFB5FFE1},
            {0xFFFF5F9E, 0xFF4D7CFF, 0xFFFFF1A8}
    };

    static RemoteViews build(Context c, int wDp, int hDp) {
        SharedPreferences p = Prefs.get(c);
        int theme = Math.max(0, Math.min(p.getInt(Prefs.THEME, 0), THEMES.length - 1));
        int preset = Math.max(0, Math.min(p.getInt(Prefs.PRESET, 0), PRESETS.length - 1));
        int font = Math.max(0, Math.min(p.getInt(Prefs.FONT, 0), FONTS.length - 1));
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
            text = Color.luminance(Art.mix(start, end, 0.5f)) < 0.55f ? 0xFFFFFFFF : 0xFF141414;
        }
        if (textMode == 1) text = 0xFFFFFFFF;
        else if (textMode == 2) text = 0xFF141414;

        // time fractions for the progress art
        Calendar cal = Calendar.getInstance();
        float day = (cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)) / 86400f;
        int dow = (cal.get(Calendar.DAY_OF_WEEK) - cal.getFirstDayOfWeek() + 7) % 7;
        float week = (dow + day) / 7f;
        float month = (cal.get(Calendar.DAY_OF_MONTH) - 1 + day) / cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        boolean showProgress = p.getBoolean(Prefs.PROGRESS, true);
        RemoteViews rv = new RemoteViews(c.getPackageName(), Layouts.IDS[theme][font]);
        rv.setImageViewBitmap(R.id.bg, Art.draw(c, theme, wDp, hDp, start, end, accent, text,
                opacity, radius, showProgress, day, week, month));

        // clock + date formats
        boolean h24 = p.getBoolean(Prefs.H24, false);
        boolean sec = p.getBoolean(Prefs.SECONDS, false);
        String hh = h24 ? "HH" : (theme == 2 ? "hh" : "h");
        String timeFormat;
        String dateFormat;
        if (theme == 2) {
            timeFormat = hh + "\nmm";
            dateFormat = "EEEE\nd MMMM";
            rv.setViewVisibility(R.id.sec, sec ? View.VISIBLE : View.GONE);
            rv.setCharSequence(R.id.sec, "setFormat12Hour", "ss");
            rv.setCharSequence(R.id.sec, "setFormat24Hour", "ss");
            rv.setTextColor(R.id.sec, accent);
        } else {
            timeFormat = hh + ":mm" + (sec ? ":ss" : "");
            dateFormat = "EEEE, d MMMM";
        }
        rv.setCharSequence(R.id.clock, "setFormat12Hour", timeFormat);
        rv.setCharSequence(R.id.clock, "setFormat24Hour", timeFormat);
        rv.setCharSequence(R.id.date, "setFormat12Hour", dateFormat);
        rv.setCharSequence(R.id.date, "setFormat24Hour", dateFormat);

        rv.setTextColor(R.id.clock, text);
        rv.setTextColor(R.id.date, accent);
        rv.setTextColor(R.id.weather, text);
        rv.setTextColor(R.id.alarm, text);
        rv.setTextColor(R.id.greeting, accent);

        // greeting
        if (p.getBoolean(Prefs.GREETING, true)) {
            rv.setViewVisibility(R.id.greeting, View.VISIBLE);
            rv.setTextViewText(R.id.greeting, greeting(p.getString(Prefs.NAME, "")));
        } else {
            rv.setViewVisibility(R.id.greeting, View.GONE);
        }

        // weather
        if (p.getBoolean(Prefs.WEATHER, true)) {
            rv.setViewVisibility(R.id.weather, View.VISIBLE);
            rv.setTextViewText(R.id.weather, weatherLine(p));
        } else {
            rv.setViewVisibility(R.id.weather, View.GONE);
        }

        // next alarm
        String alarm = p.getBoolean(Prefs.ALARM, true) ? nextAlarm(c) : null;
        if (alarm != null) {
            rv.setViewVisibility(R.id.alarm, View.VISIBLE);
            rv.setTextViewText(R.id.alarm, alarm);
        } else {
            rv.setViewVisibility(R.id.alarm, View.GONE);
        }

        Intent open = new Intent(c, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.root, pi);
        return rv;
    }

    private static String greeting(String name) {
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g = hr < 5 ? "Good night" : hr < 12 ? "Good morning" : hr < 17 ? "Good afternoon" : hr < 21 ? "Good evening" : "Good night";
        return name == null || name.isEmpty() ? g : g + ", " + name;
    }

    private static String nextAlarm(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo info = am == null ? null : am.getNextAlarmClock();
        if (info == null) return null;
        long t = info.getTriggerTime();
        String time = android.text.format.DateFormat.getTimeFormat(c).format(new Date(t));
        String day = DateUtils.isToday(t) ? "" : new SimpleDateFormat("EEE ", Locale.getDefault()).format(new Date(t));
        return "\u23F0 " + day + time;
    }

    private static String weatherLine(SharedPreferences p) {
        if (!p.contains(Prefs.LAT)) return "Tap to set your city";
        if (!p.getBoolean(Prefs.HAS, false)) return "Loading weather\u2026";
        boolean f = p.getBoolean(Prefs.FAHR, false);
        return Weather.icon(p.getInt(Prefs.CODE, 0)) + " " + temp(p.getFloat(Prefs.TEMP, 0f), f)
                + "  \u2191" + temp(p.getFloat(Prefs.HI, 0f), f)
                + " \u2193" + temp(p.getFloat(Prefs.LO, 0f), f);
    }

    private static String temp(float celsius, boolean fahrenheit) {
        float v = fahrenheit ? celsius * 9f / 5f + 32f : celsius;
        return Math.round(v) + "\u00B0";
    }
}
