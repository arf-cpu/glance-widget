package dev.vro.glance;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String PRESET = "preset", FONT = "font", TEXT = "text",
            OPACITY = "opacity", RADIUS = "radius",
            H24 = "h24", SECONDS = "seconds",
            WEATHER = "weather", FAHR = "fahr",
            CITY = "city", LAT = "lat", LON = "lon",
            HAS = "has", TEMP = "temp", HI = "hi", LO = "lo", CODE = "code";

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("glance", Context.MODE_PRIVATE);
    }
}
