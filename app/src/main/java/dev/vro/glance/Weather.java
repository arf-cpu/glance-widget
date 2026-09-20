package dev.vro.glance;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class Weather {

    /** Fetches current weather from Open-Meteo. Call from a background thread. */
    static void refresh(Context c) {
        try {
            SharedPreferences p = Prefs.get(c);
            if (!p.getBoolean(Prefs.WEATHER, true) || !p.contains(Prefs.LAT)) return;
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + p.getFloat(Prefs.LAT, 0f)
                    + "&longitude=" + p.getFloat(Prefs.LON, 0f)
                    + "&current=temperature_2m,weather_code"
                    + "&daily=temperature_2m_max,temperature_2m_min"
                    + "&timezone=auto&forecast_days=1";
            JSONObject j = new JSONObject(get(url));
            JSONObject cur = j.getJSONObject("current");
            JSONObject day = j.getJSONObject("daily");
            p.edit()
                    .putFloat(Prefs.TEMP, (float) cur.getDouble("temperature_2m"))
                    .putInt(Prefs.CODE, cur.getInt("weather_code"))
                    .putFloat(Prefs.HI, (float) day.getJSONArray("temperature_2m_max").getDouble(0))
                    .putFloat(Prefs.LO, (float) day.getJSONArray("temperature_2m_min").getDouble(0))
                    .putBoolean(Prefs.HAS, true)
                    .apply();
        } catch (Exception ignored) {
            // offline or API hiccup: keep showing the last known weather
        }
    }

    /** Looks up a city name. Saves it and returns a display label, or null if not found. */
    static String geocode(Context c, String name) {
        try {
            String url = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=en&format=json&name="
                    + URLEncoder.encode(name, "UTF-8");
            JSONObject j = new JSONObject(get(url));
            if (!j.has("results")) return null;
            JSONObject r = j.getJSONArray("results").getJSONObject(0);
            String label = r.getString("name") + (r.has("country") ? ", " + r.getString("country") : "");
            Prefs.get(c).edit()
                    .putFloat(Prefs.LAT, (float) r.getDouble("latitude"))
                    .putFloat(Prefs.LON, (float) r.getDouble("longitude"))
                    .putString(Prefs.CITY, label)
                    .putBoolean(Prefs.HAS, false)
                    .apply();
            return label;
        } catch (Exception e) {
            return null;
        }
    }

    static String icon(int code) {
        if (code == 0) return "\u2600\uFE0F";
        if (code <= 2) return "\uD83C\uDF24\uFE0F";
        if (code == 3) return "\u2601\uFE0F";
        if (code == 45 || code == 48) return "\uD83C\uDF2B\uFE0F";
        if (code >= 51 && code <= 57) return "\uD83C\uDF26\uFE0F";
        if (code >= 61 && code <= 67) return "\uD83C\uDF27\uFE0F";
        if (code >= 71 && code <= 77) return "\u2744\uFE0F";
        if (code >= 80 && code <= 82) return "\uD83C\uDF27\uFE0F";
        if (code == 85 || code == 86) return "\u2744\uFE0F";
        if (code >= 95) return "\u26C8\uFE0F";
        return "\u2601\uFE0F";
    }

    private static String get(String u) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection();
        h.setConnectTimeout(8000);
        h.setReadTimeout(8000);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(h.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            h.disconnect();
        }
    }
}
