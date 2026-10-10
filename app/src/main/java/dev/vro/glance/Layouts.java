package dev.vro.glance;

import android.graphics.Typeface;

/** Clock fonts. Every entry is a system font family, which the launcher can always render. */
final class Layouts {
    static final int SYSTEM_COUNT = 11;

    // System font family for each entry, in the same order as NAMES / IDS.
    private static final String[] FAMILIES = {
            "sans-serif-black", "sans-serif-light", "serif", "monospace",
            "sans-serif-thin", "sans-serif-condensed", "sans-serif-condensed-light", "casual",
            "cursive", "serif-monospace", "sans-serif-medium"
    };

    static final String[] NAMES = markMissing(new String[]{
            "Bold", "Light", "Serif", "Mono",
            "Thin", "Condensed", "Condensed Light", "Casual",
            "Cursive", "Typewriter", "Medium"});

    static final int[] IDS = {
            R.layout.widget_f00, R.layout.widget_f01, R.layout.widget_f02, R.layout.widget_f03,
            R.layout.widget_sys_thin, R.layout.widget_sys_condensed, R.layout.widget_sys_condlight,
            R.layout.widget_sys_casual, R.layout.widget_sys_cursive,
            R.layout.widget_sys_typewriter, R.layout.widget_sys_medium};

    /**
     * An unknown family name silently falls back to the default typeface, so a family that
     * resolves to Typeface.DEFAULT is not really installed on this phone.
     */
    private static boolean available(int idx) {
        try {
            return Typeface.create(FAMILIES[idx], Typeface.NORMAL) != Typeface.DEFAULT;
        } catch (Throwable t) {
            return true;
        }
    }

    private static String[] markMissing(String[] names) {
        for (int i = 0; i < SYSTEM_COUNT; i++) {
            if (!available(i)) names[i] = names[i] + " (not on this phone)";
        }
        return names;
    }
}
