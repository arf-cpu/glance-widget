package dev.vro.glance;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.Random;

/** Draws the card artwork (background, glow, rings, progress) into a bitmap. */
final class Art {

    static Bitmap draw(Context c, int theme, int wDp, int hDp, int start, int end, int accent, int ink,
                       int opacityPct, int radiusDp, boolean progress, float day, float week, float month) {
        float s = Math.min(c.getResources().getDisplayMetrics().density, 2f);
        int w = Math.round(Math.min(Math.max(wDp, 100), 500) * s);
        int h = Math.round(Math.min(Math.max(hDp, 80), 500) * s);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        float r = radiusDp * s;
        float k = opacityPct / 100f;
        RectF rect = new RectF(0, 0, w, h);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setShader(new LinearGradient(0, 0, w, h,
                withAlpha(start, al(255, k)), withAlpha(end, al(255, k)), Shader.TileMode.CLAMP));
        cv.drawRoundRect(rect, r, r, fill);

        Path clip = new Path();
        clip.addRoundRect(rect, r, r, Path.Direction.CW);
        cv.save();
        cv.clipPath(clip);
        if (theme == 1) orbit(cv, c, s, w, h, start, accent, ink, k, progress, day, week, month);
        else if (theme == 2) poster(cv, s, w, h, accent, ink, k, progress, day);
        else glow(cv, s, w, h, start, end, accent, ink, k, progress, day);
        cv.restore();

        // glass sheen from the top edge
        Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        sheen.setShader(new LinearGradient(0, 0, 0, h * 0.65f,
                withAlpha(0xFFFFFF, al(0x3A, k)), 0x00FFFFFF, Shader.TileMode.CLAMP));
        cv.drawRoundRect(rect, r, r, sheen);

        // thin light rim
        Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1.5f * s);
        rim.setColor(withAlpha(0xFFFFFF, al(0x55, k)));
        float in = 0.75f * s;
        cv.drawRoundRect(new RectF(in, in, w - in, h - in), r, r, rim);
        return b;
    }

    // ---------- theme 0: Glow ----------
    private static void glow(Canvas cv, float s, int w, int h, int start, int end, int accent, int ink,
                             float k, boolean progress, float day) {
        blob(cv, w * 0.88f, h * 0.10f, Math.max(w, h) * 0.55f, accent, al(0x70, k));
        blob(cv, w * 0.05f, h * 1.00f, w * 0.50f, mix(start, 0xFFFFFFFF, 0.45f), al(0x60, k));
        blob(cv, w * 0.55f, h * 0.60f, h * 0.60f, mix(end, 0xFFFFFFFF, 0.45f), al(0x30, k));

        // fine grain for a tactile, frosted feel
        Random rnd = new Random(7);
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        int n = Math.round(w * h / (s * s) / 90f);
        for (int i = 0; i < n; i++) {
            dot.setColor(withAlpha(ink, al(8 + rnd.nextInt(14), k)));
            cv.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, (0.5f + rnd.nextFloat() * 0.6f) * s, dot);
        }

        if (progress) {
            float left = 20 * s, right = w - 20 * s, cy = h - 14 * s, th = 5 * s;
            Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
            track.setColor(withAlpha(ink, 0x33));
            cv.drawRoundRect(new RectF(left, cy - th / 2, right, cy + th / 2), th / 2, th / 2, track);

            float x = left + (right - left) * clamp01(day);
            Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
            bar.setShader(new LinearGradient(left, 0, right, 0, accent, mix(accent, 0xFFFFFFFF, 0.7f), Shader.TileMode.CLAMP));
            cv.drawRoundRect(new RectF(left, cy - th / 2, Math.max(x, left + th), cy + th / 2), th / 2, th / 2, bar);
            blob(cv, x, cy, 9 * s, accent, 0x99);
            Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);
            head.setColor(0xFFFFFFFF);
            cv.drawCircle(x, cy, 2.6f * s, head);
        }
    }

    // ---------- theme 1: Orbit ----------
    private static void orbit(Canvas cv, Context c, float s, int w, int h, int start, int accent, int ink,
                              float k, boolean progress, float day, float week, float month) {
        float rad = Math.min(h / 2f - 16 * s, w * 0.27f);
        float cx = w - 20 * s - rad, cy = h / 2f;
        blob(cv, cx, cy, rad * 2.3f, accent, al(0x55, k));
        blob(cv, w * 0.05f, h * 1.1f, w * 0.45f, mix(start, 0xFFFFFFFF, 0.4f), al(0x40, k));

        // minute-style ticks around the outside
        Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
        tick.setStrokeWidth(1.2f * s);
        tick.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 60; i++) {
            boolean major = i % 5 == 0;
            double a = Math.toRadians(i * 6 - 90);
            float r1 = rad + 5 * s, r2 = r1 + (major ? 5 : 3) * s;
            tick.setColor(withAlpha(ink, major ? 0x70 : 0x30));
            cv.drawLine(cx + (float) Math.cos(a) * r1, cy + (float) Math.sin(a) * r1,
                    cx + (float) Math.cos(a) * r2, cy + (float) Math.sin(a) * r2, tick);
        }

        float stroke = 3.5f * s, gap = 11f * s;
        float[] radii = {rad, rad - gap, rad - 2 * gap};
        float[] fr = {day, week, month};
        int[] cols = {accent, withAlpha(ink, 0xE0), mix(accent, ink, 0.5f)};
        Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        track.setColor(withAlpha(ink, 0x28));
        Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(stroke);
        arc.setStrokeCap(Paint.Cap.ROUND);
        Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < 3; i++) {
            cv.drawCircle(cx, cy, radii[i], track);
            if (!progress) continue;
            arc.setColor(cols[i]);
            float sweep = 360f * clamp01(fr[i]);
            cv.drawArc(new RectF(cx - radii[i], cy - radii[i], cx + radii[i], cy + radii[i]), -90, sweep, false, arc);
            double a = Math.toRadians(-90 + sweep);
            float hx = cx + (float) Math.cos(a) * radii[i], hy = cy + (float) Math.sin(a) * radii[i];
            blob(cv, hx, hy, 7 * s, cols[i], 0x90);
            head.setColor(0xFFFFFFFF);
            cv.drawCircle(hx, hy, stroke * 0.55f, head);
        }

        if (progress) {
            float inner = radii[2] - stroke;
            Typeface tf = font(c, R.font.poppins_bold);
            Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
            t.setTextAlign(Paint.Align.CENTER);
            t.setTypeface(tf);
            t.setColor(ink);
            t.setTextSize(inner * 0.62f);
            float base = cy + inner * 0.10f;
            cv.drawText(Math.round(day * 100) + "%", cx, base, t);
            t.setTextSize(inner * 0.27f);
            t.setLetterSpacing(0.15f);
            t.setColor(withAlpha(ink, 0xAA));
            cv.drawText("DAY", cx, base + inner * 0.42f, t);
        }
    }

    // ---------- theme 2: Poster ----------
    private static void poster(Canvas cv, float s, int w, int h, int accent, int ink, float k,
                               boolean progress, float day) {
        float cx = w * 0.86f, cy = h * 0.80f, rad = h * 0.85f;
        Paint sun = new Paint(Paint.ANTI_ALIAS_FLAG);
        sun.setShader(new RadialGradient(cx, cy, rad,
                new int[]{withAlpha(accent, al(0xB0, k)), withAlpha(accent, al(0x50, k)), withAlpha(accent, 0)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, rad, sun);

        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(1f * s);
        ring.setColor(withAlpha(ink, 0x22));
        for (float m : new float[]{0.55f, 0.8f, 1.05f, 1.3f}) cv.drawCircle(cx, cy, rad * m, ring);

        // halftone dots fading away from the sun
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setColor(withAlpha(ink, al(0x38, k)));
        float step = 9 * s;
        for (float y = step / 2; y < h; y += step) {
            for (float x = w * 0.5f; x < w; x += step) {
                float d = (float) Math.hypot(x - cx, y - cy) / (rad * 1.4f);
                float rr = (1f - clamp01(d)) * 2.4f * s;
                if (rr > 0.4f * s) cv.drawCircle(x, y, rr, dot);
            }
        }

        if (progress) {
            float left = 20 * s, right = w - 20 * s, y = h - 12 * s;
            Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeWidth(2f * s);
            line.setColor(withAlpha(ink, 0x30));
            cv.drawLine(left, y, right, y, line);
            for (int i = 0; i <= 4; i++) {
                float tx = left + (right - left) * i / 4f;
                line.setStrokeWidth(1f * s);
                line.setColor(withAlpha(ink, 0x55));
                cv.drawLine(tx, y - 4 * s, tx, y + 4 * s, line);
            }
            float x = left + (right - left) * clamp01(day);
            line.setStrokeWidth(2.6f * s);
            line.setColor(accent);
            cv.drawLine(left, y, x, y, line);
            blob(cv, x, y, 9 * s, accent, 0x99);
            Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);
            head.setColor(0xFFFFFFFF);
            cv.drawCircle(x, y, 2.8f * s, head);
        }
    }

    // ---------- helpers ----------
    private static void blob(Canvas cv, float cx, float cy, float rad, int color, int alpha) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(cx, cy, rad, withAlpha(color, alpha), withAlpha(color, 0), Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, rad, p);
    }

    private static Typeface font(Context c, int res) {
        try {
            Typeface t = c.getResources().getFont(res);
            return t != null ? t : Typeface.DEFAULT_BOLD;
        } catch (Exception e) {
            return Typeface.DEFAULT_BOLD;
        }
    }

    private static int al(int base, float k) {
        return Math.round(base * k);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
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
