package dev.vro.glance;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class GlanceWidget extends AppWidgetProvider {
    static final String TICK = "dev.vro.glance.TICK";
    private static final long TICK_MS = 10 * 60 * 1000L;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        final PendingResult result = goAsync();
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                Weather.refresh(app);
                updateAll(app);
                schedule(app);
            } finally {
                result.finish();
            }
        }).start();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TICK.equals(intent.getAction())) {
            final PendingResult result = goAsync();
            final Context app = context.getApplicationContext();
            new Thread(() -> {
                try {
                    Weather.refreshIfStale(app);
                    updateAll(app);
                    schedule(app);
                } finally {
                    result.finish();
                }
            }).start();
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onEnabled(Context context) {
        schedule(context.getApplicationContext());
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(tickIntent(context));
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        updateAll(context);
    }

    static void updateAll(Context context) {
        AppWidgetManager m = AppWidgetManager.getInstance(context);
        int[] ids = m.getAppWidgetIds(new ComponentName(context, GlanceWidget.class));
        for (int id : ids) {
            Bundle o = m.getAppWidgetOptions(id);
            int w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280);
            int h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 130);
            m.updateAppWidget(id, Render.build(context, w, h));
        }
    }

    /** Refreshes the artwork every ~10 minutes so progress rings, greeting and alarm stay current. */
    static void schedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.setAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + TICK_MS, tickIntent(context));
    }

    private static PendingIntent tickIntent(Context context) {
        Intent i = new Intent(context, GlanceWidget.class).setAction(TICK);
        return PendingIntent.getBroadcast(context, 1, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
