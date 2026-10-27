package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/** Bento home, step 2: baked-blur liquid glass over a detailed background. */
public class BentoActivity extends Activity {
    private static final String NAME = "Dipika"; // change to the name you want in the greeting
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private float u; // 1 unit = 1/540 of the screen height
    private int screenW, screenH;
    private Bitmap sharp, blurred;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView greeting, time, ampm, date, temp, cond;
    private String scene = "clear_night";
    private final List<View> glassViews = new ArrayList<>();

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 10000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        screenW = getResources().getDisplayMetrics().widthPixels;
        screenH = getResources().getDisplayMetrics().heightPixels;
        u = screenH / 540f;
        scene = defaultScene(); buildBackground();

        FrameLayout root = new FrameLayout(this);
        root.addView(new BgView(), new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout page = column();
        page.setPadding(p(28), p(22), p(28), p(18));
        root.addView(page, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout top = row();
        page.addView(top, lp(MATCH, 0, 2.1f, 0));
        top.addView(leftCard(), lp(0, MATCH, 3f, 8));

        LinearLayout right = column();
        top.addView(right, lp(0, MATCH, 7f, 0));
        LinearLayout row1 = row();
        right.addView(row1, lp(MATCH, 0, 1.15f, 0));
        row1.addView(forYouCard(), lp(0, MATCH, 2f, 8));
        row1.addView(swipeCard(), lp(0, MATCH, 1f, 8));
        LinearLayout row2 = row();
        right.addView(row2, lp(MATCH, 0, 1f, 0));
        row2.addView(arcadeCard(), lp(0, MATCH, 1f, 8));
        row2.addView(jarvisCard(), lp(0, MATCH, 1f, 8));

        GlassCard dock = new GlassCard(false, LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(dock, lp(MATCH, 0, 0.95f, 8));
        TextView first = tile("YouTube", "com.google.android.youtube.tv", 0xFFFFFFFF, 0xFFD32F2F);
        dock.addView(first, lp(0, MATCH, 1f, 6));
        dock.addView(tile("NETFLIX", "com.netflix.ninja", 0xFFFFFFFF, 0xFFE50914), lp(0, MATCH, 1f, 6));
        dock.addView(tile("JioHotstar", "in.startv.hotstar", 0xFF6C3DF4, Color.WHITE), lp(0, MATCH, 1f, 6));
        dock.addView(tile("prime video", "com.amazon.amazonvideo.livingroom", 0xFF1A98FF, Color.WHITE), lp(0, MATCH, 1f, 6));
        dock.addView(tile("SonyLIV", "com.sonyliv", 0xFF111111, Color.WHITE), lp(0, MATCH, 1f, 6));
        dock.addView(tile("ZEE5", "com.graymatrix.did", 0xFF2A1F4D, Color.WHITE), lp(0, MATCH, 1f, 6));
        TextView all = tile("All apps", null, 0x33FFFFFF, Color.WHITE);
        all.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
        dock.addView(all, lp(0, MATCH, 1f, 6));

        setContentView(root);
        updateClock();
        first.requestFocus();
        fetchWeather();
    }

    @Override protected void onResume() { super.onResume(); handler.post(tick); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(tick); }

    /** Home has nothing behind it to go back to; the "All apps" tile opens the classic launcher. */
    @Override public void onBackPressed() { }

    private void updateClock() {
        Date now = new Date();
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String part = h < 5 ? "night" : h < 12 ? "morning" : h < 17 ? "afternoon" : h < 21 ? "evening" : "night";
        greeting.setText("Good " + part + ", " + NAME);
        time.setText(new SimpleDateFormat("h:mm", Locale.ENGLISH).format(now));
        ampm.setText(new SimpleDateFormat("a", Locale.ENGLISH).format(now));
        date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(now));
    }

    // ---- background: detailed picture plus a pre-blurred copy for the glass ----

    private void buildBackground() {
        int w = 480, h = 270;
        sharp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(sharp);
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setShader(new LinearGradient(0, 0, w, h,
                palette(), null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, pt);
        int[] glow = {0xFF3D8BFF, 0xFFFF6FB5, 0xFF45E0C1};
        for (int i = 0; i < 3; i++) {
            float cx = w * (0.2f + 0.3f * i), cy = h * (0.3f + 0.25f * (i % 2));
            pt.setShader(new RadialGradient(cx, cy, 150,
                    (glow[i] & 0x00FFFFFF) | 0x90000000, 0x00000000, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, 150, pt);
        }
        pt.setShader(null);
        Random r = new Random(7);
        for (int i = 0; i < 70; i++) { // bokeh lights: the detail the glass will blur
            pt.setColor(Color.argb(60 + r.nextInt(120), 200 + r.nextInt(55), 200 + r.nextInt(55), 220 + r.nextInt(35)));
            c.drawCircle(r.nextInt(w), r.nextInt(h), 3 + r.nextInt(14), pt);
        }
        pt.setStrokeWidth(1f);
        int streaks = (scene.equals("rain") || scene.equals("storm")) ? 140 : 0;
        for (int i = 0; i < streaks; i++) { // rain streaks
            pt.setColor(Color.argb(30 + r.nextInt(40), 255, 255, 255));
            float x = r.nextInt(w), y = r.nextInt(h);
            c.drawLine(x, y, x - 3, y + 20 + r.nextInt(30), pt);
        }
        if (scene.equals("snow")) { pt.setColor(0xDDFFFFFF); for (int i = 0; i < 120; i++) c.drawCircle(r.nextInt(w), r.nextInt(h), 1 + r.nextInt(3), pt); }
        if (scene.equals("clear_night")) { pt.setColor(0xFFFFFFFF); for (int i = 0; i < 80; i++) c.drawCircle(r.nextInt(w), r.nextInt(h / 2), 1, pt); }
        Bitmap s1 = Bitmap.createScaledBitmap(sharp, 120, 68, true);
        Bitmap s2 = Bitmap.createScaledBitmap(s1, 30, 17, true);
        Bitmap s3 = Bitmap.createScaledBitmap(s2, 120, 68, true);
        blurred = Bitmap.createScaledBitmap(s3, w, h, true);
        s1.recycle(); s2.recycle(); s3.recycle();
    }

    private class BgView extends View {
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Rect src = new Rect();
        private final Rect dst = new Rect();
        BgView() { super(BentoActivity.this); glassViews.add(this); }
        @Override protected void onDraw(Canvas c) {
            src.set(0, 0, sharp.getWidth(), sharp.getHeight());
            dst.set(0, 0, getWidth(), getHeight());
            c.drawBitmap(sharp, src, dst, paint);
            c.drawColor(scene.equals("clear_day") || scene.equals("snow") || scene.equals("fog") ? 0x66000000 : 0x30000000);
        }
    }

    @Override protected void onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null); }

    private String defaultScene() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return (h >= 6 && h < 18) ? "clear_day" : "clear_night";
    }

    private int[] palette() {
        switch (scene) {
            case "clear_day": return new int[]{0xFF2E8BE6, 0xFF74B9FF, 0xFFBFE3FF};
            case "cloud": return new int[]{0xFF4B5563, 0xFF6B7280, 0xFF9CA3AF};
            case "rain": return new int[]{0xFF1F2A3A, 0xFF2F4358, 0xFF4A6075};
            case "snow": return new int[]{0xFF8FA3B8, 0xFFC9D6E3, 0xFFEAF1F8};
            case "storm": return new int[]{0xFF111827, 0xFF1F2937, 0xFF3B2F5A};
            case "fog": return new int[]{0xFF6B7280, 0xFF9CA3AF, 0xFFD1D5DB};
            default: return new int[]{0xFF0B1230, 0xFF1B2A6B, 0xFF3A2A66};
        }
    }

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

    /** Place from the TV's IP (ipwho.is), weather from Open-Meteo; repeats every 20 minutes. */
    private void fetchWeather() {
        new Thread(() -> {
            try {
                JSONObject loc = new JSONObject(get("https://ipwho.is/"));
                double lat = loc.getDouble("latitude"), lon = loc.getDouble("longitude");
                final String city = loc.optString("city", "");
                JSONObject w = new JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=" + lat
                        + "&longitude=" + lon + "&current=temperature_2m,weather_code,is_day&timezone=auto"));
                JSONObject cur = w.getJSONObject("current");
                final int tmp = (int) Math.round(cur.getDouble("temperature_2m"));
                final int code = cur.getInt("weather_code");
                final boolean day = cur.getInt("is_day") == 1;
                runOnUiThread(() -> applyWeather(tmp, code, day, city));
            } catch (Exception e) {
                runOnUiThread(() -> cond.setText("Weather unavailable"));
            }
        }).start();
        handler.postDelayed(this::fetchWeather, 20 * 60 * 1000L);
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

    private void applyWeather(int tmp, int code, boolean day, String city) {
        temp.setText(tmp + "\u00B0");
        cond.setText(describe(code) + (city.isEmpty() ? "" : " \u00B7 " + city));
        if (code <= 1) scene = day ? "clear_day" : "clear_night";
        else if (code <= 3) scene = "cloud";
        else if (code == 45 || code == 48) scene = "fog";
        else if ((code >= 71 && code <= 77) || code == 85 || code == 86) scene = "snow";
        else if (code >= 95) scene = "storm";
        else scene = "rain";
        buildBackground();
        for (View v : glassViews) v.invalidate();
    }

    // ---- cards ----

    private View leftCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        greeting = text("", p(15), 0xCCFFFFFF, false);
        c.addView(greeting);
        LinearLayout tr = row();
        tr.setGravity(Gravity.BOTTOM);
        time = text("", p(66), Color.WHITE, true);
        ampm = text("", p(18), 0xCCFFFFFF, false);
        ampm.setPadding(p(6), 0, 0, p(10));
        tr.addView(time);
        tr.addView(ampm);
        c.addView(tr);
        date = text("", p(16), 0xCCFFFFFF, false);
        c.addView(date);
        View div = new View(this);
        div.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams dl = lp(MATCH, 1, 0f, 0);
        dl.topMargin = p(14);
        dl.bottomMargin = p(14);
        c.addView(div, dl);
        temp = text("--\u00B0", p(40), Color.WHITE, true); c.addView(temp);
        cond = text("Finding weather...", p(14), 0xCCFFFFFF, false); c.addView(cond);
        soon(c, "Weather refreshes every 20 minutes");
        return c;
    }

    private View forYouCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL);
        c.addView(text("For you", p(14), 0xCCFFFFFF, false));
        LinearLayout posters = row();
        int[][] cols = {
                {0xFF3A6EA5, 0xFF1B2A49}, {0xFFB5651D, 0xFF4A2511},
                {0xFF2E8B57, 0xFF11331F}, {0xFF8E44AD, 0xFF2C1338}};
        for (int[] cc : cols) {
            View v = new View(this);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, cc);
            g.setCornerRadius(p(10));
            v.setBackground(g);
            posters.addView(v, lp(0, MATCH, 1f, 4));
        }
        LinearLayout.LayoutParams pl = lp(MATCH, 0, 1f, 0);
        pl.topMargin = p(8);
        c.addView(posters, pl);
        soon(c, "For you: real posters arrive in step 6");
        return c;
    }

    private View swipeCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.addView(text("Swipe", p(22), Color.WHITE, true));
        c.addView(text("OK to start", p(14), 0xCCFFFFFF, false));
        soon(c, "Swipe deck arrives in step 6");
        return c;
    }

    private View arcadeCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.addView(text("ARCADE", p(12), 0xCCFFFFFF, true));
        c.addView(text("Play", p(26), Color.WHITE, true));
        c.addView(text("2 games", p(14), 0xCCFFFFFF, false));
        soon(c, "Games arrive in step 5");
        return c;
    }

    private View jarvisCard() {
        GlassCard c = new GlassCard(true, LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = column();
        left.addView(text("JARVIS", p(12), 0xCCFFFFFF, true));
        left.addView(text("Just ask.", p(24), Color.WHITE, true));
        c.addView(left, lp(0, WRAP, 1f, 0));
        View orb = new View(this);
        GradientDrawable og = new GradientDrawable();
        og.setShape(GradientDrawable.OVAL);
        og.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        og.setGradientRadius(p(34));
        og.setGradientCenter(0.35f, 0.3f);
        og.setColors(new int[]{0xFFFFFFFF, 0xFFA9BBFF, 0xFF3A4A94});
        orb.setBackground(og);
        c.addView(orb, lp(p(68), p(68), 0f, 0));
        soon(c, "Jarvis arrives in step 7");
        return c;
    }

    private TextView tile(final String label, final String pkg, int color, int textColor) {
        final GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(p(14));
        TextView t = new TextView(this);
        t.setBackground(d);
        t.setText(label);
        t.setTextColor(textColor);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, p(17));
        t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        t.setGravity(Gravity.CENTER);
        t.setFocusable(true);
        t.setClickable(true);
        if (pkg != null) t.setOnClickListener(v -> launch(pkg, label));
        t.setOnFocusChangeListener((v, has) -> {
            float s = has ? 1.1f : 1f;
            v.animate().scaleX(s).scaleY(s).setDuration(160).start();
            v.setTranslationZ(has ? 10 : 0);
            d.setStroke(has ? p(3) : 0, Color.WHITE);
        });
        return t;
    }

    private void launch(String pkg, String label) {
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) {
            Toast.makeText(this, label + " not found (" + pkg + ")", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(i);
    }

    private void soon(View v, final String msg) {
        v.setOnClickListener(x -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    // ---- helpers ----

    private int p(float v) { return Math.round(v * u); }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setClipChildren(false);
        l.setClipToPadding(false);
        return l;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setClipChildren(false);
        l.setClipToPadding(false);
        return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight, int margin) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(w, h, weight);
        l.setMargins(p(margin), p(margin), p(margin), p(margin));
        return l;
    }

    private TextView text(String s, float px, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    /**
     * Liquid-glass card: draws the pre-blurred background behind itself, then a white tint,
     * a sheen across the top and a bright rim. Lifts and brightens when focused.
     */
    private class GlassCard extends LinearLayout {
        private final Path path = new Path();
        private final RectF rectF = new RectF();
        private final Rect src = new Rect();
        private final Rect dst = new Rect();
        private final int[] loc = new int[2];
        private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean focusedNow;

        GlassCard(boolean focusable, int orientation) {
            super(BentoActivity.this);
            setOrientation(orientation);
            setClipChildren(false);
            setWillNotDraw(false);
            setPadding(p(16), p(14), p(16), p(14));
            rim.setStyle(Paint.Style.STROKE);
            glassViews.add(this);
            if (focusable) {
                setFocusable(true);
                setOnFocusChangeListener((v, has) -> {
                    focusedNow = has;
                    float s = has ? 1.04f : 1f;
                    animate().scaleX(s).scaleY(s).setDuration(160).start();
                    setTranslationZ(has ? 10 : 0);
                    invalidate();
                });
            }
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            float r = p(22);
            rectF.set(0, 0, w, h);
            path.reset();
            path.addRoundRect(rectF, r, r, Path.Direction.CW);

            c.save();
            c.clipPath(path);
            getLocationOnScreen(loc);
            float sx = blurred.getWidth() / (float) screenW;
            float sy = blurred.getHeight() / (float) screenH;
            src.set(Math.round(loc[0] * sx), Math.round(loc[1] * sy),
                    Math.round((loc[0] + w) * sx), Math.round((loc[1] + h) * sy));
            dst.set(0, 0, w, h);
            c.drawBitmap(blurred, src, dst, bmpPaint);
            c.drawColor(focusedNow ? 0x38FFFFFF : 0x22FFFFFF);
            sheen.setShader(new LinearGradient(0, 0, 0, h * 0.55f, 0x50FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(rectF, sheen);
            c.restore();

            float rw = focusedNow ? 3f * u : Math.max(1f, 1.2f * u);
            rim.setStrokeWidth(rw);
            rim.setShader(new LinearGradient(0, 0, 0, h,
                    focusedNow ? 0xFFFFFFFF : 0x99FFFFFF, focusedNow ? 0xAAFFFFFF : 0x22FFFFFF, Shader.TileMode.CLAMP));
            rectF.inset(rw / 2f, rw / 2f);
            c.drawRoundRect(rectF, r, r, rim);
        }
    }
}
