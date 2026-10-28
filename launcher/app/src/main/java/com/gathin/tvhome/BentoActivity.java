package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private TextView greeting, time, ampm, date, temp, cond, swipeCount, mumbai, perth;
    private LinearLayout posters; private List<ForYou.Item> forYouAll = new ArrayList<>(); private int forYouOffset = 0; private int rotTick = 0;
    private View orb;
    private SynthView synth; private View homeCard;
    private WaveView wave;
    private float phase;

    private final ViewOutlineProvider rounded12 = new ViewOutlineProvider() {
        @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), p(12)); }
    };
    private final ViewOutlineProvider rounded10 = new ViewOutlineProvider() {
        @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), p(10)); }
    };

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            rotTick++; if (rotTick % 4 == 0 && !forYouAll.isEmpty()) { forYouOffset += 4; showForYou(forYouAll); } if (rotTick % 12 == 0) refreshCricket(); handler.postDelayed(this, 10000);
        }
    };

    private final Runnable weatherTick = new Runnable() {
        @Override public void run() {
            Wx.fetch(BentoActivity.this, (t, code, day, city, sr) -> applyWeather(t, code, day, city));
            handler.postDelayed(this, 20 * 60 * 1000L);
        }
    };

    private final Runnable animTick = new Runnable() {
        @Override public void run() {
            phase += 0.025f;
            if (phase > 1f) phase -= 1f;
            if (synth != null) { synth.phase = phase; synth.invalidate(); }
            if (wave != null) { wave.phase = phase; wave.invalidate(); }
            if (orb != null) {
                float s = 1f + 0.08f * (float) Math.sin(phase * 6.2832f * 2f);
                orb.setScaleX(s);
                orb.setScaleY(s);
            }
            handler.postDelayed(this, 60);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        sw = getResources().getDisplayMetrics().widthPixels;
        sh = getResources().getDisplayMetrics().heightPixels;
        u = sh / 540f;
        Backup.restore(this); scene = G.isDayNow() ? "clear_day" : "clear_night";
        rebuild();

        FrameLayout root = new FrameLayout(this);
        root.addView(new BgView(), new FrameLayout.LayoutParams(MATCH, MATCH));
        LinearLayout page = col();
        page.setPadding(p(28), p(22), p(28), p(18));
        root.addView(page, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout top = row();
        page.addView(top, lp(MATCH, 0, 2.1f, 0));
        homeCard = leftCard(); top.addView(homeCard, lp(0, MATCH, 3f, 8));
        LinearLayout right = col();
        top.addView(right, lp(0, MATCH, 7f, 0));
        LinearLayout row1 = row();
        right.addView(row1, lp(MATCH, 0, 1.15f, 0));
        row1.addView(forYouCard(), lp(0, MATCH, 2f, 8));
        row1.addView(swipeCard(), lp(0, MATCH, 1f, 8));
        LinearLayout row2 = row();
        right.addView(row2, lp(MATCH, 0, 1f, 0));
        row2.addView(arcadeCard(), lp(0, MATCH, 1f, 8));
        row2.addView(cricketCard(), lp(0, MATCH, 1.2f, 8));
        row2.addView(jarvisCard(), lp(0, MATCH, 1f, 8));

        GlassCard dock = new GlassCard(false, LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(dock, lp(MATCH, 0, 0.95f, 8));
        dockRow = dock;
        View first = buildDock();

        setContentView(root);
        updateClock();
        Wx.cached(this, (t, code, day, city, sr) -> applyWeather(t, code, day, city));
        homeCard.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshForYou(); refreshCricket(); Backup.save(this);
        handler.post(clockTick);
        handler.post(weatherTick);
        handler.post(animTick); Photos.sync(this, () -> rebuild()); resetIdle(); if (dockRow != null && !dockSig.equals(Dock.raw(this))) buildDock();
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null); Backup.save(this);
    }

    @Override public void onBackPressed() { }

    private LinearLayout dockRow;
    private String dockSig = "";
    private final Runnable idleRun = () -> startSaver();

    @Override public void onUserInteraction() { resetIdle(); }

    private void resetIdle() {
        handler.removeCallbacks(idleRun);
        handler.postDelayed(idleRun, 100000L);
    }

    private void startSaver() {
        try {
            startActivity(new Intent().setComponent(new android.content.ComponentName("com.android.systemui", "com.android.systemui.Somnambulator"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            // the TV's own screensaver timer still applies
        }
    }

    @Override public boolean onKeyDown(int k, android.view.KeyEvent e) {
        if (k == android.view.KeyEvent.KEYCODE_ALL_APPS || k == android.view.KeyEvent.KEYCODE_MENU || k == android.view.KeyEvent.KEYCODE_GUIDE) {
            startActivity(new Intent(this, AllAppsActivity.class));
            return true;
        }
        return super.onKeyDown(k, e);
    }

    private View buildDock() {
        dockRow.removeAllViews();
        dockSig = Dock.raw(this);
        View first = null;
        PackageManager pm = getPackageManager();
        for (final String pkg : Dock.load(this)) {
            if (pm.getLaunchIntentForPackage(pkg) == null && pm.getLeanbackLaunchIntentForPackage(pkg) == null) continue;
            final String label = Dock.label(this, pkg);
            View t = appTile(label, pkg, 0xFF2A1F4D, Color.WHITE, v -> launch(pkg, label));
            t.setLongClickable(true);
            t.setOnLongClickListener(v -> { editDock(pkg); return true; });
            dockRow.addView(t, lp(0, MATCH, 1f, 4));
            if (first == null) first = t;
        }
        dockRow.addView(appTile("Renewals", null, 0x55FFFFFF, Color.WHITE, v -> startActivity(new Intent(this, SubsActivity.class))), lp(0, MATCH, 1f, 4));
        View all = appTile("All apps", null, 0x55FFFFFF, Color.WHITE, v -> startActivity(new Intent(this, AllAppsActivity.class)));
        dockRow.addView(all, lp(0, MATCH, 1f, 4));
        return first != null ? first : all;
    }

    private void editDock(final String pkg) {
        final List<String> l = Dock.load(this);
        final int i = l.indexOf(pkg);
        if (i < 0) return;
        new android.app.AlertDialog.Builder(this).setTitle(Dock.label(this, pkg))
            .setItems(new String[]{"Move left", "Move right", "Move to the very front", "Remove from Home"}, (dlg, which) -> {
                l.remove(i);
                int ni = i;
                if (which == 0) { ni = Math.max(0, i - 1); l.add(ni, pkg); }
                else if (which == 1) { ni = Math.min(l.size(), i + 1); l.add(ni, pkg); }
                else if (which == 2) { ni = 0; l.add(0, pkg); }
                Dock.save(this, l);
                buildDock();
                if (dockRow.getChildCount() > 0) dockRow.getChildAt(Math.min(ni, dockRow.getChildCount() - 1)).requestFocus();
            }).show();
    }

    private void rebuild() {
        Bitmap ph = Photos.pick(this); sharp = ph != null ? Photos.compose(ph, scene, 1280, 720) : Bg.sharp(scene, 480, 270);
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

    private int lastSlot = -2;
    private void updateClock() {
        int sl = Photos.slot(); if (sl != lastSlot) { lastSlot = sl; if (sharp != null) rebuild(); }
        Date now = new Date();
        greeting.setText(G.greet());
        time.setText(new SimpleDateFormat("h:mm", Locale.ENGLISH).format(now));
        ampm.setText(new SimpleDateFormat("a", Locale.ENGLISH).format(now));
        date.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(now));
        mumbai.setText("Mumbai  " + G.zoneTime("Asia/Kolkata"));
        perth.setText("Perth  " + G.zoneTime("Australia/Perth"));
    }

    private void refreshForYou() {
        SharedPreferences s = G.sp(this);
        int left = 0;
        for (String[] t : Titles.ALL) if (s.getInt("r_" + t[0], 0) == 0) left++;
        int rated = 0; for (String key : s.getAll().keySet()) if (key.startsWith("r_")) rated++; swipeCount.setText(rated + " rated");
        if (!s.contains("fy_cache")) showForYou(fallbackItems());
        ForYou.load(this, items -> showForYou(items));
    }

    private List<ForYou.Item> fallbackItems() {
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
        List<ForYou.Item> out = new ArrayList<>();
        for (String[] t : cand) {
            ForYou.Item it = new ForYou.Item();
            it.title = t[0];
            out.add(it);
        }
        return out;
    }

    private void showForYou(List<ForYou.Item> items) {
        posters.removeAllViews();
        forYouAll = items; for (int i = 0; i < 4 && i < items.size(); i++) posters.addView(forYouTile(items.get((forYouOffset + i) % items.size())), lp(0, MATCH, 1f, 4));
    }

    // ---- cards ----

    private View leftCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        greeting = G.tv(this, "", p(15), 0xCCFFFFFF, false);
        c.addView(greeting);
        LinearLayout tr = row();
        tr.setGravity(Gravity.BOTTOM);
        time = G.tv(this, "", p(64), Color.WHITE, true);
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
        dl.topMargin = p(12);
        dl.bottomMargin = p(12);
        c.addView(div, dl);
        LinearLayout tw = row();
        tw.setGravity(Gravity.CENTER_VERTICAL);
        temp = G.tv(this, "--\u00B0", p(40), Color.WHITE, true);
        tw.addView(temp, lp(0, WRAP, 1f, 0));
        LinearLayout wr = col();
        mumbai = G.tv(this, "", p(13), Color.WHITE, true);
        perth = G.tv(this, "", p(13), Color.WHITE, true);
        wr.addView(mumbai);
        wr.addView(perth);
        tw.addView(wr, lp(WRAP, WRAP, 0f, 0));
        c.addView(tw);
        cond = G.tv(this, "Finding weather...", p(14), 0xCCFFFFFF, false);
        c.addView(cond);
        c.setOnClickListener(v -> Wx.fetch(this, (t, code, day, city, sr) -> applyWeather(t, code, day, city)));
        return c;
    }

    private View forYouCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL); c.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS); c.setOnClickListener(v -> startActivity(new Intent(this, ForYouActivity.class)));
        c.addView(G.tv(this, "For you  \u00B7  OK to see all", p(14), 0xCCFFFFFF, false));
        posters = row();
        LinearLayout.LayoutParams pl = lp(MATCH, 0, 1f, 0);
        pl.topMargin = p(8);
        c.addView(posters, pl);
        return c;
    }

    private View forYouTile(final ForYou.Item it) {
        final FrameLayout f = new FrameLayout(this);
        float hue = Math.abs(it.title.hashCode()) % 360;
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
                Color.HSVToColor(new float[]{hue, 0.55f, 0.75f}), Color.HSVToColor(new float[]{hue, 0.7f, 0.3f})});
        g.setCornerRadius(p(10));
        f.setBackground(g);
        f.setClipToOutline(true);
        f.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), p(10)); }
        });
        final TextView name = G.tv(this, it.title, p(12), Color.WHITE, true);
        name.setGravity(Gravity.BOTTOM);
        name.setPadding(p(6), p(6), p(6), p(8));
        f.addView(name, new FrameLayout.LayoutParams(MATCH, MATCH));
        final android.widget.ImageView iv = new android.widget.ImageView(this);
        iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        iv.setVisibility(View.GONE);
        f.addView(iv, new FrameLayout.LayoutParams(MATCH, MATCH));
        Posters.Cb cb = bm -> {
            iv.setImageBitmap(bm);
            iv.setVisibility(View.VISIBLE);
            name.setVisibility(View.GONE);
        };
        if (it.image != null && !it.image.isEmpty()) Posters.loadUrl(this, it.image, cb); else Posters.load(this, it.title, cb);
        if (it.platform != null) {
            TextView badge = G.tv(this, it.platform.toUpperCase(Locale.ENGLISH), p(9), Color.WHITE, true);
            badge.setPadding(p(5), p(2), p(5), p(2));
            GradientDrawable bg2 = new GradientDrawable();
            bg2.setColor(0xAA000000);
            bg2.setCornerRadius(p(6));
            badge.setBackground(bg2);
            FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.TOP | Gravity.START);
            bl.setMargins(p(5), p(5), 0, 0);
            f.addView(badge, bl);
        }
        final GradientDrawable rimD = new GradientDrawable();
        rimD.setColor(0x00000000);
        rimD.setCornerRadius(p(10));
        rimD.setStroke(p(3), 0xFFFFFFFF);
        f.setFocusable(true);
        f.setClickable(true);
        f.setOnClickListener(v -> {
            if (it.pkg != null) launch(it.pkg, it.platform);
            else Toast.makeText(this, "Rate shows in Swipe to get picks here", Toast.LENGTH_SHORT).show();
        });
        f.setOnFocusChangeListener((v, has) -> {
            float s = has ? 1.07f : 1f;
            v.animate().scaleX(s).scaleY(s).setDuration(150).start();
            v.setTranslationZ(has ? 10 : 0);
            f.setForeground(has ? rimD : null);
        });
        return f;
    }

    private View poster(final String[] t) {
        final FrameLayout f = new FrameLayout(this);
        float hue = Math.abs(t[0].hashCode()) % 360;
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
                Color.HSVToColor(new float[]{hue, 0.55f, 0.75f}), Color.HSVToColor(new float[]{hue, 0.7f, 0.3f})});
        g.setCornerRadius(p(10));
        f.setBackground(g);
        f.setClipToOutline(true);
        f.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View v, android.graphics.Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), p(10)); }
        });
        final TextView name = G.tv(this, t[0], p(12), Color.WHITE, true);
        name.setGravity(Gravity.BOTTOM);
        name.setPadding(p(6), p(6), p(6), p(8));
        f.addView(name, new FrameLayout.LayoutParams(MATCH, MATCH));
        final android.widget.ImageView iv = new android.widget.ImageView(this);
        iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        iv.setVisibility(View.GONE);
        f.addView(iv, new FrameLayout.LayoutParams(MATCH, MATCH));
        Posters.load(this, t[0], bm -> {
            iv.setImageBitmap(bm);
            iv.setVisibility(View.VISIBLE);
            name.setVisibility(View.GONE);
        });
        return f;
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
        GlassCard c = new GlassCard(true, LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = col();
        left.setGravity(Gravity.CENTER_VERTICAL);
        left.addView(G.tv(this, "ARCADE", p(12), 0xCCFFFFFF, true));
        left.addView(G.tv(this, "Play", p(26), Color.WHITE, true));
        left.addView(G.tv(this, "3 games", p(14), 0xCCFFFFFF, false));
        c.addView(left, lp(0, MATCH, 1f, 0));
        synth = new SynthView();
        synth.setClipToOutline(true);
        synth.setOutlineProvider(rounded10);
        c.addView(synth, lp(0, MATCH, 1.1f, 0));
        c.setOnClickListener(v -> startActivity(new Intent(this, GamesActivity.class)));
        return c;
    }

    private TextView cricketLine1, cricketLine2, cricketLine3;

    private View cricketCard() {
        GlassCard c = new GlassCard(true, LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        c.addView(G.tv(this, "CRICKET  \u00B7  INDIA and IPL", p(11), 0xCCFFFFFF, true));
        cricketLine1 = G.tv(this, "Loading scores...", p(21), Color.WHITE, true);
        cricketLine2 = G.tv(this, "", p(15), 0xDDFFFFFF, false);
        cricketLine3 = G.tv(this, "", p(13), 0xAAFFFFFF, false);
        for (TextView t : new TextView[]{cricketLine1, cricketLine2, cricketLine3}) {
            t.setSingleLine();
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            c.addView(t);
        }
        c.setOnClickListener(v -> startActivity(new Intent(this, CricketActivity.class)));
        return c;
    }

    private void refreshCricket() {
        Cricket.load(this, list -> {
            if (cricketLine1 == null) return;
            Cricket.Match m = Cricket.top(list);
            if (m == null) {
                cricketLine1.setText("No match today");
                cricketLine2.setText("India and IPL");
                cricketLine3.setText("OK for the full page");
                return;
            }
            cricketLine1.setText((m.live ? "LIVE  " : "") + m.teams);
            cricketLine2.setText(m.line.isEmpty() ? m.series : m.line);
            cricketLine3.setText(m.status);
        });
    }

    private View jarvisCard() {
        GlassCard c = new GlassCard(true, LinearLayout.HORIZONTAL);
        c.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = col();
        left.addView(G.tv(this, "JARVIS", p(12), 0xCCFFFFFF, true));
        left.addView(G.tv(this, "Just ask.", p(24), Color.WHITE, true));
        wave = new WaveView();
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(p(110), p(22));
        wl.topMargin = p(8);
        left.addView(wave, wl);
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

    /** Dock tile: the app's own banner from the TV when it has one, else a coloured name tile. */
    private View appTile(String label, String pkg, int color, int textColor, View.OnClickListener click) {
        final FrameLayout f = new RatioFrame(this);
        final GradientDrawable d = new GradientDrawable();
        d.setColor(0x00000000);
        d.setCornerRadius(p(15));
        f.setBackground(d);
        f.setPadding(p(3), p(3), p(3), p(3));
        Drawable art = null;
        if (pkg != null) {
            try { art = getPackageManager().getApplicationBanner(pkg); } catch (Exception e) { art = null; }
        }
        View inner;
        if (art != null) {
            ImageView iv = new ImageView(this);
            iv.setImageDrawable(art);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            inner = iv;
        } else {
            GradientDrawable g = new GradientDrawable();
            g.setColor(color);
            TextView t = new TextView(this);
            t.setBackground(g);
            t.setText(label);
            t.setTextColor(textColor);
            t.setTextSize(TypedValue.COMPLEX_UNIT_PX, p(16));
            t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            t.setGravity(Gravity.CENTER);
            inner = t;
        }
        inner.setClipToOutline(true);
        inner.setOutlineProvider(rounded12);
        f.addView(inner, new FrameLayout.LayoutParams(MATCH, MATCH));
        f.setFocusable(true);
        f.setClickable(true);
        f.setOnClickListener(click);
        f.setOnFocusChangeListener((v, has) -> {
            float s = has ? 1.1f : 1f;
            v.animate().scaleX(s).scaleY(s).setDuration(160).start();
            v.setTranslationZ(has ? 10 : 0);
            d.setStroke(has ? p(3) : 0, Color.WHITE);
        });
        return f;
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

    private static class RatioFrame extends FrameLayout {
        RatioFrame(android.content.Context c) { super(c); }
        @Override protected void onMeasure(int ws, int hs) {
            int w = MeasureSpec.getSize(ws);
            super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(Math.round(w * 9f / 16f), MeasureSpec.EXACTLY));
        }
    }

    // ---- custom views ----

    private class SynthView extends View {
        float phase;
        private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);

        SynthView() { super(BentoActivity.this); }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            float hz = h * 0.56f;
            pt.setStyle(Paint.Style.FILL);
            pt.setShader(new LinearGradient(0, 0, 0, hz, 0xFF1A0B3D, 0xFFD6249F, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, hz, pt);
            pt.setShader(null);
            pt.setColor(0xFF0B0620);
            c.drawRect(0, hz, w, h, pt);
            float cx = w / 2f, r = h * 0.34f;
            c.save();
            c.clipRect(0, 0, w, hz);
            pt.setShader(new LinearGradient(0, hz - 2 * r, 0, hz, 0xFFFFE259, 0xFFFF2E93, Shader.TileMode.CLAMP));
            c.drawCircle(cx, hz - r * 0.55f, r, pt);
            pt.setShader(null);
            pt.setColor(0xFF2A0F52);
            for (int k = 0; k < 4; k++) {
                float y = hz - r * 0.55f + r * (0.15f + 0.2f * k);
                c.drawRect(0, y, w, y + r * 0.05f * (k + 1), pt);
            }
            c.restore();
            pt.setStyle(Paint.Style.STROKE);
            pt.setStrokeWidth(Math.max(1f, u * 0.8f));
            pt.setColor(0xFFFF4FD8);
            for (int i = 0; i < 7; i++) {
                float t = (i + phase) / 7f;
                float y = hz + (h - hz) * t * t;
                c.drawLine(0, y, w, y, pt);
            }
            for (int k = -5; k <= 5; k++) c.drawLine(cx, hz, cx + k * w * 0.22f, h, pt);
        }
    }

    private class WaveView extends View {
        float phase;
        private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rf = new RectF();

        WaveView() { super(BentoActivity.this); }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            int n = 9;
            float bw = w / (n * 2f - 1f);
            pt.setColor(0xCCFFFFFF);
            for (int i = 0; i < n; i++) {
                float amp = 0.25f + 0.75f * (float) Math.abs(Math.sin(phase * 6.2832f + i * 0.8f));
                float bh = h * amp;
                float x = i * 2 * bw;
                rf.set(x, (h - bh) / 2f, x + bw, (h + bh) / 2f);
                c.drawRoundRect(rf, bw / 2f, bw / 2f, pt);
            }
        }
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
            c.drawColor(light ? 0x2E000000 : 0x12000000);
        }
    }

    /** Liquid-glass card: pre-blurred background behind it, white tint, top sheen, bright rim. */
    private class GlassCard extends LinearLayout {
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private final Rect src = new Rect();
        private final Rect dst = new Rect();
        private final int[] loc = new int[2];
        private final Paint bmp = new Paint(Paint.FILTER_BITMAP_FLAG); { bmp.setAlpha(70); }
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
            c.drawColor(focusedNow ? 0x26FFFFFF : 0x0CFFFFFF);
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