package dev.vro.glance;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PREVIEW_W = 300, PREVIEW_H = 140;

    private SharedPreferences sp;
    private FrameLayout preview;
    private LinearLayout panel;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        sp = Prefs.get(this);

        int id = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id));
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        preview = new FrameLayout(this);
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(200)));

        ScrollView scroll = new ScrollView(this);
        GradientDrawable sheet = new GradientDrawable();
        sheet.setColor(0xF2121212);
        float r = dp(24);
        sheet.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        scroll.setBackground(sheet);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(16), dp(20), dp(32));
        scroll.addView(panel);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        header("Look");
        spinner("Color style", Render.PRESETS, Prefs.PRESET);
        spinner("Clock font", Render.FONTS, Prefs.FONT);
        spinner("Text color", Render.TEXTS, Prefs.TEXT);
        seek("Card opacity", Prefs.OPACITY, 15, 100, 80, "%");
        seek("Corner radius", Prefs.RADIUS, 0, 48, 28, "dp");

        header("Clock");
        toggle("24-hour time", Prefs.H24, false, null);
        toggle("Show seconds", Prefs.SECONDS, false, null);

        header("Weather");
        toggle("Show weather", Prefs.WEATHER, true, () -> fetchWeatherAsync());
        toggle("Use Fahrenheit", Prefs.FAHR, false, null);
        cityRow();

        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        GlanceWidget.updateAll(this);
    }

    // ---------- UI helpers ----------

    private void header(String t) {
        TextView v = new TextView(this);
        v.setText(t.toUpperCase());
        v.setTextColor(0xFF9BB0FF);
        v.setTextSize(12);
        v.setLetterSpacing(0.1f);
        v.setPadding(0, dp(20), 0, dp(4));
        panel.addView(v);
    }

    private TextView label(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextColor(WHITE);
        v.setPadding(0, dp(10), 0, 0);
        panel.addView(v);
        return v;
    }

    private void spinner(String title, String[] items, final String key) {
        label(title);
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items));
        s.setSelection(sp.getInt(key, 0));
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                sp.edit().putInt(key, pos).apply();
                refresh();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        panel.addView(s);
    }

    private void seek(final String title, final String key, final int min, int max, int def, final String unit) {
        final TextView t = label(title + ": " + sp.getInt(key, def) + unit);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(sp.getInt(key, def) - min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                sp.edit().putInt(key, min + progress).apply();
                t.setText(title + ": " + (min + progress) + unit);
                refresh();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        panel.addView(sb);
    }

    private void toggle(String title, final String key, boolean def, final Runnable after) {
        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextColor(WHITE);
        sw.setChecked(sp.getBoolean(key, def));
        sw.setPadding(0, dp(10), 0, dp(10));
        sw.setOnCheckedChangeListener((button, on) -> {
            sp.edit().putBoolean(key, on).apply();
            refresh();
            if (on && after != null) after.run();
        });
        panel.addView(sw);
    }

    private void cityRow() {
        label("City");
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        final EditText city = new EditText(this);
        city.setHint("Type your city");
        city.setSingleLine(true);
        city.setTextColor(WHITE);
        city.setHintTextColor(0x88FFFFFF);
        city.setText(sp.getString(Prefs.CITY, ""));
        Button find = new Button(this);
        find.setText("Find");
        find.setOnClickListener(v -> {
            final String q = city.getText().toString().trim();
            if (q.isEmpty()) return;
            toast("Searching\u2026");
            new Thread(() -> {
                final String found = Weather.geocode(this, q);
                if (found != null) Weather.refresh(this);
                GlanceWidget.updateAll(this);
                runOnUiThread(() -> {
                    toast(found != null ? "Set to " + found : "City not found");
                    if (found != null) city.setText(found);
                    refresh();
                });
            }).start();
        });
        row.addView(city, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(find);
        panel.addView(row);
    }

    private void fetchWeatherAsync() {
        new Thread(() -> {
            Weather.refresh(this);
            GlanceWidget.updateAll(this);
            runOnUiThread(this::refresh);
        }).start();
    }

    private void refresh() {
        if (preview == null) return;
        try {
            preview.removeAllViews();
            RemoteViews rv = Render.build(this, PREVIEW_W, PREVIEW_H);
            View v = rv.apply(this, preview);
            preview.addView(v, new FrameLayout.LayoutParams(dp(PREVIEW_W), dp(PREVIEW_H), Gravity.CENTER));
        } catch (Exception ignored) {
            // preview is cosmetic; never crash the settings screen over it
        }
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
