package dev.vro.glance;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String FONT = "font", PRESET = "preset", TEXT = "text",
            OPACITY = "opacity", BG = "bg", RADIUS = "radius",
            H24 = "h24", SECONDS = "seconds", PROGRESS = "progress",
            WEATHER = "weather", FAHR = "fahr",
            CITY = "city", LAT = "lat", LON = "lon",
            HAS = "has", TEMP = "temp", HI = "hi", LO = "lo", CODE = "code", FETCHED = "fetched",
            FONT_OK = "fontok_";

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences("glance", Context.MODE_PRIVATE);
    }
}
