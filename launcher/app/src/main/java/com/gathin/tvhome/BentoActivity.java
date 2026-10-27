package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bento home: liquid-glass cards over a weather-driven background. */
public class BentoActivity extends Activity {
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private float u;
    private int sw, sh;
    private Bitmap sharp, blurred;
    private String scene;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<View> glass = new ArrayList<>();
    private TextView greeting, time, ampm, date, temp, cond, swipeCount;
    private LinearLayout posters;
    private View orb;
    private ValueAnimator pulse;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 10000);
        }
    };

    private final Runnable weatherTick = new Runnable() {
        @Override public void run() {
            Wx.fetch(BentoActivity.this, (t, code, day, city, sr) -> applyWeather(t, code, day, city));
            handler.postDelayed(this, 20 * 60 * 1000L);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        sw = getResources().getDisplayMetrics().widthPixels;
        sh = getResources().getDisplayMetrics().heightPixels;
        u = sh / 540f;
        scene = G.isDayNow() ? "clear_day" : "clear_night";
        rebuild();

        FrameLayout root = new FrameLayout(this);
        root.addView(new BgView(), new FrameLayout.LayoutParams(MATCH, MATCH));
        LinearLayout page = col();
        page.setPadding(p(28), p(22), p(28), p(18));
        root.addView(page, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout top = row();
        page.addView(top, lp(MATCH, 0, 2.1f, 0));
        top.addView(leftCard(), lp(0, MATCH, 3f, 8));
        LinearLayout right = col();
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
        TextView first = tile("YouTube", 0xFFFFFFFF, 0xFFD32F2F, v -> launch("com.google.android.youtube.tv", "YouTube"));
        dock.addView(first, lp(0, MATCH, 1f, 5));
        dock.addView(tile("NETFLIX", 0xFFFFFFFF, 0xFFE50914, v -> launch("com.netflix.ninja", "Netflix")), lp(0, MATCH, 1f, 5));
        dock.addView(tile("JioHotstar", 0xFF6C3DF4, Color.WHITE, v -> launch("in.startv.hotstar", "JioHotstar")), lp(0, MATCH, 1f, 5));
        dock.addView(tile("prime video", 0xFF1A98FF, Color.WHITE, v -> launch("com.amazon.amazonvideo.livingroom", "Prime Video")), lp(0, MATCH, 1f, 5));
        dock.addView(tile("SonyLIV", 0xFF111111, Color.WHITE, v -> launch("com.sonyliv", "SonyLIV")), lp(0, MATCH, 1f, 5));
        dock.addView(tile("ZEE5", 0xFF2A1F4D, Color.WHITE, v -> launch("com.graymatrix.did", "ZEE5")), lp(0, MATCH, 1f, 5));
        dock.addView(tile("Renewals", 0x44FFFFFF, Color.WHITE, v -> startActivity(new Intent(this, SubsActivity.class))), lp(0, MATCH, 1f, 5));
        dock.addView(tile("All apps", 0x44FFFFFF, Color.WHITE, v -> startActivity(new Intent(this, MainActivity.class))), lp(0, MATCH, 1f, 5));

        setContentView(root);

        pulse = ValueAnimator.ofFloat(1f, 1.14f);
        pulse.setDuration(1400);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.addUpdateListener(a -> {
            float s = (Float) a.getAnimatedValue();
            orb.setScaleX(s);
            orb.setScaleY(s);
        });

        updateClock();
        Wx.cached(this, (t, code, day, city, sr) -> applyWeather(t, code, day, city));
        first.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshForYou();
        handler.post(clockTick);
        handler.post(weatherTick);
        if (!pulse.isStarted()) pulse.start();
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        pulse.cancel();
    }

    @Override public void onBackPressed() { }

    private void rebuild() {
        sharp = Bg.sharp(scene, 480, 270);
        blurred = Bg.blur(sharp);
        for (View v : glass) v.invalidate();
    }

    private void applyWeather(int t, int code, boolean day, String city) {
        temp.setText(t + "\u00B0");
        cond.setText(Wx.describe(code) + (city.isEmpty() ? "" : " \u00B7 " + city));
        String s = Wx.scene(code, day);
        if (!s.equals(scene)) {
            scene = s;
            rebuild();
        }
    }

    private void updateClock() {
        Date now = new Date();
        greeting.setText(G.greet());
        time.setText(new SimpleDateFormat("h:mm", Locale.ENGLISH).format(now));
        ampm.setText(new SimpleDateFormat("a", Locale.ENGLISH).format(now));
        date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(now));
    }

    private void refreshForYou() {
        SharedPreferences s = G.sp(this);
        final Map<String, Integer> gs = new HashMap<>();
        List<String[]> cand = new ArrayList<>();
        for (String[] t : Titles.ALL) {
            int r = s.getInt("r_" + t[0], 0);
            if (r != 0) {
                Integer o = gs.get(t[1]);
                gs.put(t[1], (o == null ? 0 : o) + r);
            } else {
                cand.add(t);
            }
        }
        Collections.sort(cand, (a, c) -> {
            Integer x = gs.get(a[1]);
            Integer y = gs.get(c[1]);
            return (y == null ? 0 : y) - (x == null ? 0 : x);
        });
        posters.removeAllViews();
        for (int i = 0; i < 4 && i < cand.size(); i++) posters.addView(poster(cand.get(i)), lp(0, MATCH, 1f, 4));
        swipeCount.setText(cand.size() + " to rate");
    }

    // ---- cards ----

    private View leftCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        greeting = G.tv(this, "", p(15), 0xCCFFFFFF, false);
        c.addView(greeting);
        LinearLayout tr = row();
        tr.setGravity(Gravity.BOTTOM);
        time = G.tv(this, "", p(66), Color.WHITE, true);
        ampm = G.tv(this, "", p(18), 0xCCFFFFFF, false);
        ampm.setPadding(p(6), 0, 0, p(10));
        tr.addView(time);
        tr.addView(ampm);
        c.addView(tr);
        date = G.tv(this, "", p(16), 0xCCFFFFFF, false);
        c.addView(date);
        View div = new View(this);
        div.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams dl = lp(MATCH, 1, 0f, 0);
        dl.topMargin = p(14);
        dl.bottomMargin = p(14);
        c.addView(div, dl);
        temp = G.tv(this, "--\u00B0", p(40), Color.WHITE, true);
        c.addView(temp);
        cond = G.tv(this, "Finding weather...", p(14), 0xCCFFFFFF, false);
        c.addView(cond);
        c.setOnClickListener(v -> Wx.fetch(this, (t, code, day, city, sr) -> applyWeather(t, code, day, city)));
        return c;
    }

    private View forYouCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL);
        c.addView(G.tv(this, "For you  \u00B7  from your swipes", p(14), 0xCCFFFFFF, false));
        posters = row();
        LinearLayout.LayoutParams pl = lp(MATCH, 0, 1f, 0);
        pl.topMargin = p(8);
        c.addView(posters, pl);
        c.setOnClickListener(v -> startActivity(new Intent(this, SwipeActivity.class)));
        return c;
    }

    private TextView poster(String[] t) {
        TextView v = G.tv(this, t[0], p(12), Color.WHITE, true);
        float hue = Math.abs(t[0].hashCode()) % 360;
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
                Color.HSVToColor(new float[]{hue, 0.55f, 0.75f}), Color.HSVToColor(new float[]{hue, 0.7f, 0.3f})});
        g.setCornerRadius(p(10));
        v.setBackground(g);
        v.setGravity(Gravity.BOTTOM);
        v.setPadding(p(6), p(6), p(6), p(8));
        return v;
    }

    private View swipeCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.addView(G.tv(this, "Swipe", p(22), Color.WHITE, true));
        swipeCount = G.tv(this, "", p(14), 0xCCFFFFFF, false);
        c.addView(swipeCount);
        c.addView(G.tv(this, "OK to start", p(12), 0x99FFFFFF, false));
        c.setOnClickListener(v -> startActivity(new Intent(this, SwipeActivity.class)));
        return c;
    }

    private View arcadeCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.addView(G.tv(this, "ARCADE", p(12), 0xCCFFFFFF, true));
        c.addView(G.tv(this, "Play", p(26), Color.WHITE, true));
        c.addView(G.tv(this, "2 games", p(14), 0xCCFFFFFF, false));
        c.setOnClickListener(v -> startActivity(new Intent(this, GamesActivity.class)));
        return c;
    }

    private View jarvisCard() {
        GlassCard c = new GlassCard(true, LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = col();
        left.addView(G.tv(this, "JARVIS", p(12), 0xCCFFFFFF, true));
        left.addView(G.tv(this, "Just ask.", p(24), Color.WHITE, true));
        c.addView(left, lp(0, WRAP, 1f, 0));
        orb = new View(this);
        GradientDrawable og = new GradientDrawable();
        og.setShape(GradientDrawable.OVAL);
        og.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        og.setGradientRadius(p(34));
        og.setGradientCenter(0.35f, 0.3f);
        og.setColors(new int[]{0xFFFFFFFF, 0xFFA9BBFF, 0xFF3A4A94});
        orb.setBackground(og);
        c.addView(orb, lp(p(68), p(68), 0f, 0));
        c.setOnClickListener(v -> {
            try {
                Intent i = new Intent("android.intent.action.VOICE_ASSIST");
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "Press the mic button on your remote and ask", Toast.LENGTH_LONG).show();
            }
        });
        return c;
    }

    private TextView tile(String label, int color, int textColor, View.OnClickListener click) {
        final GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(p(14));
        TextView t = new TextView(this);
        t.setBackground(d);
        t.setText(label);
        t.setTextColor(textColor);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, p(16));
        t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        t.setGravity(Gravity.CENTER);
        t.setFocusable(true);
        t.setOnClickListener(click);
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
            Toast.makeText(this, label + " not found on this TV", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(i);
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

    private LinearLayout col() {
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

    private class BgView extends View {
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Rect src = new Rect();
        private final Rect dst = new Rect();

        BgView() {
            super(BentoActivity.this);
            glass.add(this);
        }

        @Override protected void onDraw(Canvas c) {
            src.set(0, 0, sharp.getWidth(), sharp.getHeight());
            dst.set(0, 0, getWidth(), getHeight());
            c.drawBitmap(sharp, src, dst, paint);
            boolean light = scene.equals("clear_day") || scene.equals("snow") || scene.equals("fog");
            c.drawColor(light ? 0x66000000 : 0x30000000);
        }
    }

    /** Liquid-glass card: pre-blurred background behind it, white tint, top sheen, bright rim. */
    private class GlassCard extends LinearLayout {
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private final Rect src = new Rect();
        private final Rect dst = new Rect();
        private final int[] loc = new int[2];
        private final Paint bmp = new Paint(Paint.FILTER_BITMAP_FLAG);
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
            glass.add(this);
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
            rect.set(0, 0, w, h);
            path.reset();
            path.addRoundRect(rect, r, r, Path.Direction.CW);
            c.save();
            c.clipPath(path);
            getLocationOnScreen(loc);
            float fx = blurred.getWidth() / (float) sw, fy = blurred.getHeight() / (float) sh;
            int l = Math.max(0, Math.round(loc[0] * fx));
            int t = Math.max(0, Math.round(loc[1] * fy));
            int rr = Math.min(blurred.getWidth(), Math.round((loc[0] + w) * fx));
            int bb = Math.min(blurred.getHeight(), Math.round((loc[1] + h) * fy));
            if (rr > l && bb > t) {
                src.set(l, t, rr, bb);
                dst.set(0, 0, w, h);
                c.drawBitmap(blurred, src, dst, bmp);
            }
            c.drawColor(focusedNow ? 0x38FFFFFF : 0x22FFFFFF);
            sheen.setShader(new LinearGradient(0, 0, 0, h * 0.55f, 0x50FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(rect, sheen);
            c.restore();
            float rw = focusedNow ? 3f * u : Math.max(1f, 1.2f * u);
            rim.setStrokeWidth(rw);
            rim.setShader(new LinearGradient(0, 0, 0, h,
                    focusedNow ? 0xFFFFFFFF : 0x99FFFFFF, focusedNow ? 0xAAFFFFFF : 0x22FFFFFF, Shader.TileMode.CLAMP));
            rect.inset(rw / 2f, rw / 2f);
            c.drawRoundRect(rect, r, r, rim);
        }
    }
}