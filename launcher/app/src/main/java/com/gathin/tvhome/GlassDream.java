package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.View;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Screensaver: liquid-glass clock with live weather, sunrise and a "Renews in" card.
 * Network only while it is running (Open-Meteo, place from the IP). Drifts slowly so nothing burns in.
 */
public class GlassDream extends DreamService {

    private DreamView view;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);   // any key wakes the TV back to where it was
        setFullscreen(true);
        setScreenBright(false);
        view = new DreamView(this);
        setContentView(view);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        view.start();
    }

    @Override
    public void onDreamingStopped() {
        view.stop();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (view != null) view.stop();
        super.onDetachedFromWindow();
    }

    private static class DreamView extends View {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path path = new Path();
        private final float[] starX = new float[90];
        private final float[] starY = new float[90];
        private final float[] starR = new float[90];
        private volatile String temp = "--\u00B0";
        private volatile String cond = "";
        private volatile String city = "";
        private volatile String sunrise = "--";

        private final Runnable tick = new Runnable() {
            @Override public void run() {
                invalidate();
                handler.postDelayed(this, 15000);
            }
        };
        private final Runnable refresh = new Runnable() {
            @Override public void run() {
                fetch();
                handler.postDelayed(this, 20 * 60 * 1000L);
            }
        };

        DreamView(Context c) {
            super(c);
            Random r = new Random(11);
            for (int i = 0; i < starX.length; i++) {
                starX[i] = r.nextFloat();
                starY[i] = r.nextFloat() * 0.55f;
                starR[i] = 0.5f + r.nextFloat() * 1.5f;
            }
        }

        void start() {
            handler.post(tick);
            handler.post(refresh);
        }

        void stop() {
            handler.removeCallbacksAndMessages(null);
        }

        // ---- weather ----

        private String get(String u) throws Exception {
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

        private String describe(int c) {
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

        private void fetch() {
            new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        JSONObject loc = new JSONObject(get("https://ipwho.is/"));
                        double lat = loc.getDouble("latitude"), lon = loc.getDouble("longitude");
                        String place = loc.optString("city", "");
                        JSONObject w = new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude="
                                + lat + "&longitude=" + lon
                                + "&current=temperature_2m,weather_code&daily=sunrise&forecast_days=1&timezone=auto"));
                        JSONObject cur = w.getJSONObject("current");
                        int t = (int) Math.round(cur.getDouble("temperature_2m"));
                        String c = describe(cur.getInt("weather_code"));
                        String sr = w.getJSONObject("daily").getJSONArray("sunrise").getString(0);
                        Date d = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH).parse(sr);
                        sunrise = new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(d);
                        temp = t + "\u00B0";
                        cond = c;
                        city = place;
                        postInvalidate();
                    } catch (Exception e) {
                        cond = "Weather unavailable";
                        postInvalidate();
                    }
                }
            }).start();
        }

        // ---- drawing ----

        private void label(Canvas c, String s, float x, float y, float size, int color, boolean bold, Paint.Align al) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(al);
            paint.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
            paint.setTextSize(size);
            paint.setColor(color);
            paint.setLetterSpacing(0f);
            c.drawText(s, x, y, paint);
        }

        private void glass(Canvas c, RectF r, float radius, float u) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setColor(0x2AFFFFFF);
            c.drawRoundRect(r, radius, radius, paint);
            paint.setShader(new LinearGradient(0, r.top, 0, r.top + r.height() * 0.55f,
                    0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRoundRect(r, radius, radius, paint);
            paint.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0x99FFFFFF, 0x22FFFFFF, Shader.TileMode.CLAMP));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1f, 1.2f * u));
            c.drawRoundRect(r, radius, radius, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
        }

        private void hills(Canvas c, int w, int h, boolean night) {
            int[] cols = night
                    ? new int[]{0xFF1F2F7A, 0xFF182466, 0xFF101A4C}
                    : new int[]{0xFF6FA8DC, 0xFF4F8FCB, 0xFF3A78B5};
            for (int k = 0; k < 3; k++) {
                path.reset();
                path.moveTo(0, h);
                for (int x = 0; x <= w; x += 12) {
                    float fx = x / (float) w;
                    float y = h * (0.80f + 0.045f * k)
                            - h * (0.05f - 0.008f * k) * (float) Math.sin(fx * (5 + 2 * k) + k * 1.7f);
                    if (k == 1) {
                        float d = (fx - 0.5f) / 0.12f;
                        y -= h * 0.16f * (float) Math.exp(-d * d);
                    }
                    path.lineTo(x, y);
                }
                path.lineTo(w, h);
                path.close();
                paint.setStyle(Paint.Style.FILL);
                paint.setShader(null);
                paint.setColor(cols[k]);
                c.drawPath(path, paint);
            }
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            float u = h / 540f;
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            boolean night = hour < 6 || hour >= 18;
            Date now = cal.getTime();

            // sky
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, 0, 0, h,
                    night ? 0xFF070C26 : 0xFF2E8BE6, night ? 0xFF2B2A6B : 0xFFBFE3FF, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            if (night) {
                paint.setColor(0xCCFFFFFF);
                for (int i = 0; i < starX.length; i++) c.drawCircle(starX[i] * w, starY[i] * h, starR[i] * u, paint);
            }
            paint.setColor(night ? 0xFFEDEBFF : 0xFFFFF2B0);
            c.drawCircle(w * 0.72f, h * 0.24f, h * 0.04f, paint);
            hills(c, w, h, night);

            // slow drift against burn-in
            double ang = cal.get(Calendar.MINUTE) / 60.0 * 2 * Math.PI;
            c.save();
            c.translate((float) Math.sin(ang) * 8 * u, (float) Math.cos(ang) * 5 * u);

            float cx = w / 2f;
            String part = hour < 5 ? "night" : hour < 12 ? "morning" : hour < 17 ? "afternoon" : hour < 21 ? "evening" : "night";
            label(c, "Good " + part + ", " + Subs.NAME, cx, h * 0.14f, 17 * u, 0xCCFFFFFF, false, Paint.Align.CENTER);
            label(c, new SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(now),
                    cx, h * 0.20f, 22 * u, 0xFFFFFFFF, true, Paint.Align.CENTER);

            // big clock: h:mm with a small am/pm
            String time = new SimpleDateFormat("h:mm", Locale.ENGLISH).format(now);
            String ap = new SimpleDateFormat("a", Locale.ENGLISH).format(now);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            paint.setTextSize(190 * u);
            float tw = paint.measureText(time);
            paint.setTextSize(30 * u);
            float aw = paint.measureText(ap);
            float gap = 10 * u;
            float startX = cx - (tw + gap + aw) / 2f;
            label(c, time, startX, h * 0.56f, 190 * u, 0xFFFFFFFF, true, Paint.Align.LEFT);
            label(c, ap, startX + tw + gap, h * 0.56f, 30 * u, 0xCCFFFFFF, false, Paint.Align.LEFT);

            // chips: weather and sunrise
            float chipW = 210 * u, chipH = 54 * u, chipGap = 14 * u;
            float chipTop = h * 0.66f;
            float left = cx - chipW - chipGap / 2f;
            rect.set(left, chipTop, left + chipW, chipTop + chipH);
            glass(c, rect, 16 * u, u);
            label(c, city.isEmpty() ? "WEATHER" : city.toUpperCase(Locale.ENGLISH),
                    rect.left + 14 * u, rect.top + 19 * u, 11 * u, 0xAAFFFFFF, true, Paint.Align.LEFT);
            label(c, (temp + " " + cond).trim(), rect.left + 14 * u, rect.top + 42 * u, 18 * u, 0xFFFFFFFF, true, Paint.Align.LEFT);
            rect.set(cx + chipGap / 2f, chipTop, cx + chipGap / 2f + chipW, chipTop + chipH);
            glass(c, rect, 16 * u, u);
            label(c, "SUNRISE", rect.left + 14 * u, rect.top + 19 * u, 11 * u, 0xAAFFFFFF, true, Paint.Align.LEFT);
            label(c, sunrise, rect.left + 14 * u, rect.top + 42 * u, 18 * u, 0xFFFFFFFF, true, Paint.Align.LEFT);

            // renews card, top right
            List<Subs.Item> items = Subs.upcoming();
            int n = Math.min(4, items.size());
            if (n > 0) {
                float cardW = w * 0.22f;
                float cardL = w * 0.76f, cardT = h * 0.07f;
                float rowH = 42 * u;
                float cardH = 14 * u + 24 * u + n * rowH + 8 * u;
                rect.set(cardL, cardT, cardL + cardW, cardT + cardH);
                glass(c, rect, 22 * u, u);
                label(c, "RENEWS IN", cardL + 16 * u, cardT + 26 * u, 11 * u, 0xAAFFFFFF, true, Paint.Align.LEFT);
                for (int i = 0; i < n; i++) {
                    Subs.Item it = items.get(i);
                    float mid = cardT + 14 * u + 28 * u + i * rowH + rowH / 2f;
                    paint.setStyle(Paint.Style.FILL);
                    paint.setShader(null);
                    paint.setColor(it.color);
                    c.drawCircle(cardL + 21 * u, mid, 4.5f * u, paint);
                    label(c, it.name, cardL + 36 * u, mid - 2 * u, 15 * u, 0xFFFFFFFF, true, Paint.Align.LEFT);
                    label(c, it.sub, cardL + 36 * u, mid + 14 * u, 10.5f * u, 0x99FFFFFF, false, Paint.Align.LEFT);
                    label(c, it.label, cardL + cardW - 16 * u, mid + 5 * u, 15 * u, 0xFFFFFFFF, true, Paint.Align.RIGHT);
                }
            }
            c.restore();
        }
    }
}
