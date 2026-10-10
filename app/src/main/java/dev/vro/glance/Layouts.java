package dev.vro.glance;

import android.graphics.Typeface;

/** Font layouts: the first SYSTEM_COUNT use system font families, the rest use bundled fonts (drawn as bitmaps). */
final class Layouts {
    static final int SYSTEM_COUNT = 11;

    // System font family for each of the first SYSTEM_COUNT entries, in the same order as NAMES / IDS.
    private static final String[] FAMILIES = {
            "sans-serif-black", "sans-serif-light", "serif", "monospace",
            "sans-serif-thin", "sans-serif-condensed", "sans-serif-condensed-light", "casual",
            "cursive", "serif-monospace", "sans-serif-medium"
    };

    static final String[] NAMES = markMissing(new String[]{
            "Bold (system)", "Light (system)", "Serif (system)", "Mono (system)",
            "Thin (system)", "Condensed (system)", "Condensed Light (system)", "Casual (system)",
            "Cursive (system)", "Typewriter (system)", "Medium (system)",
            "Poppins Bold", "Poppins Thin", "Michroma", "DM Serif Display", "Bebas Neue", "Major Mono",
            "Audiowide", "Righteous", "Pacifico", "Monoton", "Bungee", "Abril Fatface", "Press Start 2P", "Limelight"});

    static final int[] IDS = {
            R.layout.widget_f00, R.layout.widget_f01, R.layout.widget_f02, R.layout.widget_f03,
            R.layout.widget_sys_thin, R.layout.widget_sys_condensed, R.layout.widget_sys_condlight,
            R.layout.widget_sys_casual, R.layout.widget_sys_cursive,
            R.layout.widget_sys_typewriter, R.layout.widget_sys_medium,
            R.layout.widget_f04, R.layout.widget_f05, R.layout.widget_f06, R.layout.widget_f07,
            R.layout.widget_f08, R.layout.widget_f09, R.layout.widget_f10, R.layout.widget_f11,
            R.layout.widget_f12, R.layout.widget_f13, R.layout.widget_f14, R.layout.widget_f15,
            R.layout.widget_f16, R.layout.widget_f17};

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
