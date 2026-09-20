package dev.vro.glance;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String THEME = "theme", PRESET = "preset", FONT = "font", TEXT = "text",
            OPACITY = "opacity", RADIUS = "radius",
            H24 = "h24", SECONDS = "seconds",
            GREETING = "greeting", NAME = "name", ALARM = "alarm", PROGRESS = "progress",
            WEATHER = "weather", FAHR = "fahr",
            CITY = "city", LAT = "lat", LON = "lon",
            HAS = "has", TEMP = "temp", HI = "hi", LO = "lo", CODE = "code", FETCHED = "fetched";

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("glance", Context.MODE_PRIVATE);
    }
}
