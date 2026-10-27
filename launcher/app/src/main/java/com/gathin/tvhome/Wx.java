package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

final class Wx {
    interface Cb { void on(int temp, int code, boolean day, String city, String sunrise); }

    static String scene(int code, boolean day) {
        if (code <= 1) return day ? "clear_day" : "clear_night";
        if (code <= 3) return "cloud";
        if (code == 45 || code == 48) return "fog";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "snow";
        if (code >= 95) return "storm";
        return "rain";
    }

    static String describe(int c) {
        if (c == 0) return "Clear";
        if (c == 1) return "Mostly clear";
        if (c == 2) return "Partly cloudy";
        if (c == 3) return "Cloudy";
        if (c == 45 || c == 48) return "Fog";
        if (c >= 51 && c <= 57) return "Drizzle";
        if (c >= 61 && c <= 67) return "Rain";
        if (c >= 71 && c <= 77) return "Snow";
        if (c >= 80 && c <= 82) return "Showers";
        if (c == 85 || c == 86) return "Snow showers";
        if (c >= 95) return "Thunderstorm";
        return "Weather";
    }

    static void cached(Context ctx, Cb cb) {
        SharedPreferences s = G.sp(ctx);
        if (s.contains("wx_t")) {
            cb.on(s.getInt("wx_t", 0), s.getInt("wx_c", 0), s.getBoolean("wx_d", true),
                    s.getString("wx_city", ""), s.getString("wx_sr", ""));
        }
    }

    static void fetch(Context ctx, final Cb cb) {
        final Context app = ctx.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                JSONObject loc = new JSONObject(get("https://ipwho.is/"));
                double lat = loc.getDouble("latitude");
                double lon = loc.getDouble("longitude");
                final String city = loc.optString("city", "");
                JSONObject w = new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=" + lat
                        + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code,is_day&daily=sunrise&forecast_days=1&timezone=auto"));
                JSONObject cur = w.getJSONObject("current");
                final int t = (int) Math.round(cur.getDouble("temperature_2m"));
                final int code = cur.getInt("weather_code");
                final boolean day = cur.getInt("is_day") == 1;
                String sr = w.getJSONObject("daily").getJSONArray("sunrise").getString(0);
                final String sunrise = fmt(sr.substring(sr.indexOf('T') + 1));
                G.sp(app).edit().putInt("wx_t", t).putInt("wx_c", code).putBoolean("wx_d", day)
                        .putString("wx_city", city).putString("wx_sr", sunrise).apply();
                main.post(() -> cb.on(t, code, day, city, sunrise));
            } catch (Exception e) {
                // keep showing the last known weather
            }
        }).start();
    }

    private static String fmt(String hm) {
        try {
            String[] p = hm.split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int hh = h % 12 == 0 ? 12 : h % 12;
            return hh + ":" + (m < 10 ? "0" : "") + m + " " + (h >= 12 ? "PM" : "AM");
        } catch (Exception e) {
            return hm;
        }
    }

    private static String get(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String l;
        while ((l = r.readLine()) != null) sb.append(l);
        r.close();
        c.disconnect();
        return sb.toString();
    }
}