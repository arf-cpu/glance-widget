package dev.vro.glance;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** Draws the card (optional) and the day-progress strip into one bitmap. */
final class Art {

    static Bitmap draw(Context c, int wDp, int hDp, int start, int end, int accent, int ink,
                       int bgPct, int radiusDp, boolean progress, float day) {
        float s = Math.min(c.getResources().getDisplayMetrics().density, 2f);
        int w = Math.round(Math.min(Math.max(wDp, 100), 500) * s);
        int h = Math.round(Math.min(Math.max(hDp, 80), 500) * s);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);

        if (bgPct > 0) {
            float r = radiusDp * s;
            RectF rect = new RectF(0, 0, w, h);
            int a = Math.round(255f * bgPct / 100f);

            Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            fill.setShader(new LinearGradient(0, 0, w, h, withAlpha(start, a), withAlpha(end, a), Shader.TileMode.CLAMP));
            cv.drawRoundRect(rect, r, r, fill);

            // glass sheen from the top edge
            int sheenAlpha = Math.round(0x3A * bgPct / 100f);
            Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
            sheen.setShader(new LinearGradient(0, 0, 0, h * 0.65f, (sheenAlpha << 24) | 0xFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            cv.drawRoundRect(rect, r, r, sheen);

            // thin light rim
            Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(1.5f * s);
            rim.setColor((Math.round(0x55 * bgPct / 100f) << 24) | 0xFFFFFF);
            float in = 0.75f * s;
            cv.drawRoundRect(new RectF(in, in, w - in, h - in), r, r, rim);
        }

        if (progress) strip(cv, s, w, h, accent, ink, day);
        return b;
    }

    /** 24 hour ticks (past = accent, now = glowing, future = dim), a fill line with a glowing head, and a percentage. */
    private static void strip(Canvas cv, float s, int w, int h, int accent, int ink, float day) {
        day = Math.max(0f, Math.min(1f, day));
        float left = 20 * s, labelRight = w - 20 * s, right = labelRight - 46 * s;
        float cy = h - 24 * s;
        int cur = Math.min(23, (int) (day * 24));
        float cell = (right - left) / 24f;

        Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
        tick.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 24; i++) {
            float x = left + (i + 0.5f) * cell;
            float half = (i % 6 == 0 ? 6.5f : 3.8f) * s;
            if (i < cur) {
                tick.setStrokeWidth(1.7f * s);
                tick.setColor(accent);
            } else if (i == cur) {
                blob(cv, x, cy, 10 * s, accent, 0x90);
                half += 2 * s;
                tick.setStrokeWidth(2.8f * s);
                tick.setColor(accent);
            } else {
                tick.setStrokeWidth(1.7f * s);
                tick.setColor(withAlpha(ink, 0x45));
            }
            cv.drawLine(x, cy - half, x, cy + half, tick);
        }

        float y = cy + 11 * s;
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeWidth(2.6f * s);
        line.setColor(withAlpha(ink, 0x30));
        cv.drawLine(left, y, right, y, line);

        float x = left + (right - left) * day;
        if (x > left + 0.5f) {
            line.setShader(new LinearGradient(left, 0, right, 0, accent, mix(accent, 0xFFFFFFFF, 0.55f), Shader.TileMode.CLAMP));
            cv.drawLine(left, y, x, y, line);
            line.setShader(null);
        }
        blob(cv, x, y, 8 * s, accent, 0xA0);
        Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);
        head.setColor(0xFFFFFFFF);
        cv.drawCircle(x, y, 2.4f * s, head);

        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setTextAlign(Paint.Align.RIGHT);
        t.setTextSize(13 * s);
        t.setColor(ink);
        if (Color.luminance(ink) > 0.5f) t.setShadowLayer(3 * s, 0, 1.5f * s, 0x66000000);
        cv.drawText(Math.round(day * 100) + "%", labelRight, cy + 3 * s + 4.5f * s, t);
    }

    private static void blob(Canvas cv, float cx, float cy, float rad, int color, int alpha) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(cx, cy, rad, withAlpha(color, alpha), withAlpha(color, 0), Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, rad, p);
    }

    static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    static int mix(int a, int b, float t) {
        return Color.argb(255,
                Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }
}
