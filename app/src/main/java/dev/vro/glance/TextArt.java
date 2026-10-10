package dev.vro.glance;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

/**
 * Draws text into a bitmap using a bundled font. Launchers can't be trusted to load
 * fonts from RemoteViews layouts, but a bitmap always looks the same everywhere.
 */
final class TextArt {

    // Same order as Layouts.NAMES after the 4 system entries (index 4 onwards).
    private static final int[] FONTS = {
            R.font.poppins_bold,
            R.font.poppins_extralight,
            R.font.michroma,
            R.font.dmserif_display,
            R.font.bebas_neue,
            R.font.major_mono,
            R.font.audiowide,
            R.font.righteous,
            R.font.pacifico,
            R.font.monoton,
            R.font.bungee,
            R.font.abril_fatface,
            R.font.press_start,
            R.font.limelight
    };

    /** Returns the bundled typeface for a font index, or null for system fonts / failures. */
    static Typeface typeface(Context c, int fontIdx) {
        int i = fontIdx - Layouts.SYSTEM_COUNT;
        if (i < 0 || i >= FONTS.length) return null;
        try {
            return c.getResources().getFont(FONTS[i]);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Draws one line of text, left-aligned and vertically centred, shrinking it until it fits.
     * Sizes are in pixels.
     */
    static Bitmap draw(String text, Typeface tf, int color, int wPx, int hPx,
                       float maxSizePx, float shadowPx) {
        int w = Math.max(1, wPx);
        int h = Math.max(1, hPx);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(tf);
        p.setColor(color);
        if (shadowPx > 0) p.setShadowLayer(shadowPx, 0f, shadowPx / 2f, 0x40000000);

        float size = maxSizePx;
        Rect b = new Rect();
        for (int n = 0; n < 60; n++) {
            p.setTextSize(size);
            p.getTextBounds(text, 0, text.length(), b);
            if (b.width() <= w - shadowPx * 2 && b.height() <= h - shadowPx * 2) break;
            size *= 0.94f;
        }

        float x = shadowPx - b.left;
        float y = (h - b.height()) / 2f - b.top;
        cv.drawText(text, x, y, p);
        return bmp;
    }
}
