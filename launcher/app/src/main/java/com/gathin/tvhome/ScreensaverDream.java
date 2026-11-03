package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.KeyEvent;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ScreensaverDream extends DreamService {
    private SaverView view;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(true);
        setFullscreen(true);
        view = new SaverView(this);
        setContentView(view);
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        view.start();
        view.requestFocus();
    }

    @Override public void onDreamingStopped() {
        if (view != null) view.stop();
        super.onDreamingStopped();
    }

    void openHome() {
        Intent i = new Intent(this, BentoActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    static class SaverView extends View {
        private final ScreensaverDream dream;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rf = new RectF();
        private Bitmap bg;
        private String scene;
        private boolean night;
        private int temp = Integer.MIN_VALUE;
        private int code;
        private String city = "", sunrise = "--";
        private List<Subs.Item> subs = new ArrayList<>();
        private final Runnable tick = new Runnable() {
            @Override public void run() {
                invalidate();
                handler.postDelayed(this, 15000);
            }
        };

        SaverView(ScreensaverDream d) {
            super(d);
            dream = d;
            setFocusable(true);
            setFocusableInTouchMode(true);
            night = !G.isDayNow();
            scene = night ? "clear_night" : "clear_day";
        }

        void start() {
            stop();
            subs = Subs.setOnly(Subs.load(getContext()));
            Collections.sort(subs, (a, b) -> Subs.daysLeft(a) - Subs.daysLeft(b));
            Wx.cached(getContext(), this::weather);
            Wx.fetch(getContext(), this::weather);
            Photos.sync(getContext(), () -> { build(); invalidate(); });
            handler.post(tick);
        }

        void stop() { handler.removeCallbacksAndMessages(null); }

        private void weather(int tmp, int c, boolean day, String cty, String sr) {
            temp = tmp;
            code = c;
            city = cty;
            sunrise = sr;
            night = !day;
            scene = Wx.scene(c, day);
            build();
            invalidate();
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            super.onSizeChanged(w, h, ow, oh);
            build();
        }

        @Override public boolean onKeyDown(int k, KeyEvent e) {
            dream.openHome();
            return true;
        }

        private void build() {
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;
            Bitmap photoBmp = Photos.pickSaver(getContext()); if (photoBmp != null) { bg = Photos.compose(photoBmp, scene, w, h); return; }
            bg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bg);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new LinearGradient(0, 0, 0, h, Bg.palette(scene), null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);
            Random r = new Random(5);
            if (scene.equals("clear_night")) {
                p.setColor(0xFFFFFFFF);
                for (int i = 0; i < 140; i++) c.drawCircle(r.nextInt(w), r.nextInt((int) (h * 0.55f)), 1 + r.nextInt(2), p);
                p.setColor(0xFFF2F0FF);
                c.drawCircle(0.84f * w, 0.2f * h, 0.065f * h, p);
                p.setColor(0x22000000);
                c.drawCircle(0.82f * w, 0.18f * h, 0.012f * h, p);
                c.drawCircle(0.855f * w, 0.225f * h, 0.018f * h, p);
            } else if (scene.equals("clear_day")) {
                p.setShader(new RadialGradient(0.84f * w, 0.2f * h, 0.2f * h, 0x88FFE082, 0x00FFE082, Shader.TileMode.CLAMP));
                c.drawCircle(0.84f * w, 0.2f * h, 0.2f * h, p);
                p.setShader(null);
                p.setColor(0xFFFFD54A);
                c.drawCircle(0.84f * w, 0.2f * h, 0.06f * h, p);
            } else {
                p.setColor(0x55FFFFFF);
                for (int i = 0; i < 6; i++) {
                    float cx = w * (0.1f + 0.16f * i), cy = h * (0.15f + 0.08f * (i % 3));
                    c.drawOval(new RectF(cx - 0.12f * w, cy - 0.04f * h, cx + 0.12f * w, cy + 0.04f * h), p);
                }
            }
            Path m = new Path();
            m.moveTo(0.38f * w, 0.8f * h);
            m.quadTo(0.52f * w, 0.62f * h, 0.575f * w, 0.42f * h);
            m.quadTo(0.62f * w, 0.62f * h, 0.78f * w, 0.8f * h);
            m.close();
            p.setColor(night ? 0xFF231F6A : 0xFF3F6B9E);
            c.drawPath(m, p);
            int[] hills = night ? new int[]{0xFF5566DD, 0xFF4455C8, 0xFF3744AD, 0xFF2A348F}
                    : new int[]{0xFF7FB6E8, 0xFF5F9BD6, 0xFF4A84C0, 0xFF3A6EA8};
            for (int k = 0; k < 4; k++) {
                float base = h * (0.6f + 0.075f * k), amp = h * 0.035f, ph = k * 1.7f, fr = 0.004f * (1 + k * 0.3f);
                Path hp = new Path();
                hp.moveTo(0, h);
                for (int x = 0; x <= w; x += 10) hp.lineTo(x, base + amp * (float) Math.sin(x * fr + ph));
                hp.lineTo(w, h);
                hp.close();
                p.setColor(hills[k]);
                c.drawPath(hp, p);
                if (k >= 2) {
                    p.setColor(night ? 0xFF1A1F5A : 0xFF2B5580);
                    for (float x = r.nextInt(30); x < w; x += 24 + r.nextInt(30)) {
                        float y = base + amp * (float) Math.sin(x * fr + ph);
                        float th = h * (0.04f + 0.05f * r.nextFloat());
                        Path tr = new Path();
                        tr.moveTo(x - th * 0.28f, y + th * 0.3f);
                        tr.lineTo(x, y - th * 0.7f);
                        tr.lineTo(x + th * 0.28f, y + th * 0.3f);
                        tr.close();
                        c.drawPath(tr, p);
                    }
                }
            }
        }

        private void chip(Canvas c, float cx, float cy, float w, float h, String label, String value) {
            rf.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
            t.setStyle(Paint.Style.FILL);
            t.setColor(0x33FFFFFF);
            c.drawRoundRect(rf, h * 0.3f, h * 0.3f, t);
            t.setStyle(Paint.Style.STROKE);
            t.setStrokeWidth(2f);
            t.setColor(0x66FFFFFF);
            c.drawRoundRect(rf, h * 0.3f, h * 0.3f, t);
            t.setStyle(Paint.Style.FILL);
            t.setTextAlign(Paint.Align.CENTER);
            t.setColor(0xB0FFFFFF);
            t.setTypeface(Typeface.DEFAULT);
            t.setTextSize(h * 0.22f);
            c.drawText(label, cx, cy - h * 0.12f, t);
            t.setColor(Color.WHITE);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setTextSize(h * 0.34f);
            c.drawText(value, cx, cy + h * 0.28f, t);
        }

        @Override protected void onDraw(Canvas c) {
            if (bg == null) return;
            c.drawBitmap(bg, 0, 0, null);
            int w = getWidth(), h = getHeight();
            Date now = new Date();
            t.setStyle(Paint.Style.FILL);
            t.setTextAlign(Paint.Align.CENTER);
            t.setColor(Color.WHITE);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setTextSize(0.032f * h);
            c.drawText(G.greet(), w / 2f, 0.09f * h, t);
            t.setTextSize(0.05f * h);
            c.drawText(new SimpleDateFormat("EEEE, d MMMM", Locale.ENGLISH).format(now), w / 2f, 0.155f * h, t);
            t.setTextSize(0.30f * h);
            c.drawText(new SimpleDateFormat("h:mm", Locale.ENGLISH).format(now), w / 2f, 0.48f * h, t);
            String wv = temp == Integer.MIN_VALUE ? "--" : temp + "\u00B0 " + Wx.describe(code);
            chip(c, w / 2f - 0.19f * w, 0.585f * h, 0.17f * w, 0.085f * h, city.isEmpty() ? "WEATHER" : city.toUpperCase(Locale.ENGLISH), wv);
            chip(c, w / 2f, 0.585f * h, 0.17f * w, 0.085f * h, "SUNRISE", sunrise);
            chip(c, w / 2f + 0.19f * w, 0.585f * h, 0.17f * w, 0.085f * h, "SUNSET", G.sp(getContext()).getString("wx_ss", "--"));

            if (!subs.isEmpty()) {
                int n = Math.min(4, subs.size());
                float cw = 0.24f * w, rowH = 0.075f * h, ch = 0.06f * h + n * rowH;
                float cl = w - cw - 0.03f * w, ct = 0.04f * h;
                rf.set(cl, ct, cl + cw, ct + ch);
                t.setStyle(Paint.Style.FILL);
                t.setColor(0x44000000);
                c.drawRoundRect(rf, 0.02f * h, 0.02f * h, t);
                t.setStyle(Paint.Style.STROKE);
                t.setStrokeWidth(2f);
                t.setColor(0x55FFFFFF);
                c.drawRoundRect(rf, 0.02f * h, 0.02f * h, t);
                t.setStyle(Paint.Style.FILL);
                t.setTextAlign(Paint.Align.LEFT);
                t.setTypeface(Typeface.DEFAULT_BOLD);
                t.setColor(0xB0FFFFFF);
                t.setTextSize(0.018f * h);
                c.drawText("RENEWS IN", cl + 0.015f * w, ct + 0.04f * h, t);
                int[] dots = {0xFF5B6BFF, 0xFFFF4D4D, 0xFFFFC107, 0xFF29B6F6};
                for (int i = 0; i < n; i++) {
                    Subs.Item it = subs.get(i);
                    float y = ct + 0.06f * h + i * rowH;
                    t.setColor(dots[i % 4]);
                    Icons.draw(getContext(), c, it.name, cl + 0.012f * w, y + 0.011f * h, 0.052f * h);
                    t.setTextAlign(Paint.Align.LEFT);
                    t.setColor(Color.WHITE);
                    t.setTextSize(0.026f * h);
                    c.drawText(it.name, cl + 0.04f * w, y + 0.034f * h, t);
                    t.setTypeface(Typeface.DEFAULT);
                    t.setColor(0xB0FFFFFF);
                    t.setTextSize(0.016f * h);
                    c.drawText("Auto-renews " + Subs.dateText(it), cl + 0.04f * w, y + 0.056f * h, t);
                    t.setTypeface(Typeface.DEFAULT_BOLD);
                    t.setTextAlign(Paint.Align.RIGHT);
                    t.setColor(Color.WHITE);
                    t.setTextSize(0.024f * h);
                    c.drawText(Subs.daysText(it), cl + cw - 0.015f * w, y + 0.034f * h, t);
                }
            }
            t.setTextAlign(Paint.Align.CENTER);
            t.setTypeface(Typeface.DEFAULT);
            t.setColor(0xB0FFFFFF);
            t.setTextSize(0.022f * h);
            c.drawText(G.worldLine(), w / 2f, 0.925f * h, t);
            c.drawText("Press OK to open", w / 2f, 0.965f * h, t);
        }
    }
}