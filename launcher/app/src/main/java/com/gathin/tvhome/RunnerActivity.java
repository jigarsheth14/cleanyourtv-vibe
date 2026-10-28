package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Neon Run: endless runner. UP or OK jumps, DOWN ducks under the yellow bars. */
public class RunnerActivity extends Activity {
    private RunView v;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        v = new RunView(this);
        setContentView(v);
    }

    @Override protected void onResume() { super.onResume(); v.start(); }
    @Override protected void onPause() { super.onPause(); v.stop(); }

    @Override public boolean onKeyDown(int k, KeyEvent e) {
        if (v.key(k, true, e.getRepeatCount() == 0)) return true;
        return super.onKeyDown(k, e);
    }

    @Override public boolean onKeyUp(int k, KeyEvent e) {
        if (v.key(k, false, true)) return true;
        return super.onKeyUp(k, e);
    }

    static class RunView extends View {
        private final Handler h = new Handler(Looper.getMainLooper());
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rf = new RectF();
        private final Random r = new Random();
        private final List<float[]> obs = new ArrayList<>();
        private Shader sky, sun;
        private float jump, vy, spawn, scroll, score;
        private boolean duck, dead;
        private int best;
        private long last;
        private final Runnable step = new Runnable() {
            @Override public void run() {
                long now = System.nanoTime();
                float dt = Math.min(0.05f, (now - last) / 1e9f);
                last = now;
                update(dt);
                invalidate();
                h.postDelayed(this, 16);
            }
        };

        RunView(Context c) {
            super(c);
            best = G.sp(c).getInt("runner_best", 0);
            reset();
        }

        void start() { stop(); last = System.nanoTime(); h.post(step); }
        void stop() { h.removeCallbacks(step); }

        private void reset() {
            obs.clear();
            jump = 0f; vy = 0f; scroll = 0f; score = 0f;
            spawn = 1500f;
            dead = false;
            duck = false;
        }

        @Override protected void onSizeChanged(int w, int hh, int ow, int oh) {
            super.onSizeChanged(w, hh, ow, oh);
            float gy = hh * 0.78f, u = hh / 540f;
            sky = new LinearGradient(0, 0, 0, gy, 0xFF1A0B3D, 0xFFD6249F, Shader.TileMode.CLAMP);
            sun = new LinearGradient(0, gy - 170 * u, 0, gy, 0xFFFFE259, 0xFFFF2E93, Shader.TileMode.CLAMP);
        }

        private float[] box(float[] o, float gy, float u) {
            float bottom = o[3] == 0f ? gy : gy - u * 38f;
            return new float[]{o[0], bottom - o[2], o[0] + o[1], bottom};
        }

        private void die() {
            dead = true;
            int sc = (int) score;
            if (sc > best) {
                best = sc;
                G.sp(getContext()).edit().putInt("runner_best", best).apply();
            }
        }

        private void update(float dt) {
            if (dead || getHeight() == 0) return;
            float u = getHeight() / 540f;
            float speed = u * (280f + Math.min(320f, score * 2.5f));
            score += speed * dt / (u * 30f);
            scroll += speed * dt;
            if (jump > 0f || vy > 0f) {
                float g = u * ((duck && vy <= 0f) ? 3400f : 1700f);
                jump += vy * dt;
                vy -= g * dt;
                if (jump <= 0f) { jump = 0f; vy = 0f; }
            }
            for (int i = obs.size() - 1; i >= 0; i--) {
                float[] o = obs.get(i);
                o[0] -= speed * dt;
                if (o[0] + o[1] < 0f) obs.remove(i);
            }
            spawn -= speed * dt;
            if (spawn <= 0f) {
                boolean high = score > 15f && r.nextInt(10) < 3;
                float w = u * (high ? 70f : 24f + r.nextInt(18));
                float ht = u * (high ? 32f : 34f + r.nextInt(24));
                obs.add(new float[]{getWidth() + 20f, w, ht, high ? 1f : 0f});
                spawn = getWidth() * (0.55f + r.nextFloat() * 0.5f);
            }
            float gy = getHeight() * 0.78f;
            float ph = (duck && jump == 0f) ? u * 26f : u * 46f;
            float px = getWidth() * 0.18f;
            float pl = px + u * 4f, pr = px + u * 30f, pt = gy - jump - ph + u * 4f, pb = gy - jump - u * 2f;
            for (float[] o : obs) {
                float[] b = box(o, gy, u);
                if (pr > b[0] && pl < b[2] && pb > b[1] && pt < b[3]) { die(); return; }
            }
        }

        boolean key(int k, boolean down, boolean first) {
            if (k == KeyEvent.KEYCODE_DPAD_DOWN) { duck = down; return true; }
            boolean act = k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_CENTER
                    || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_SPACE;
            if (!act) return false;
            if (down && first) {
                if (dead) reset();
                else if (jump == 0f && vy == 0f) vy = (getHeight() / 540f) * 640f;
            }
            return true;
        }

        @Override protected void onDraw(Canvas c) {
            if (sky == null) return;
            int w = getWidth(), hh = getHeight();
            float u = hh / 540f, gy = hh * 0.78f;
            p.setStyle(Paint.Style.FILL);
            p.setShader(sky);
            c.drawRect(0, 0, w, gy, p);
            p.setShader(sun);
            c.save();
            c.clipRect(0, 0, w, gy);
            c.drawCircle(w * 0.7f, gy - 40 * u, 110 * u, p);
            c.restore();
            p.setShader(null);
            p.setColor(0xFF0B0620);
            c.drawRect(0, gy, w, hh, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2f, u * 2f));
            p.setColor(0xFFFF4FD8);
            c.drawLine(0, gy, w, gy, p);
            float sp = 70 * u;
            for (float x = -(scroll % sp); x < w; x += sp) c.drawLine(x, gy, x - 40 * u, hh, p);
            for (int i = 1; i < 5; i++) {
                float y = gy + (hh - gy) * (i * i) / 16f;
                c.drawLine(0, y, w, y, p);
            }
            p.setStyle(Paint.Style.FILL);
            for (float[] o : obs) {
                float[] b = box(o, gy, u);
                p.setColor(o[3] == 0f ? 0xFFFF4FD8 : 0xFFFFD54A);
                rf.set(b[0], b[1], b[2], b[3]);
                c.drawRoundRect(rf, 6 * u, 6 * u, p);
            }
            float pw = 34 * u, ph = (duck && jump == 0f) ? 26 * u : 46 * u, px = w * 0.18f;
            rf.set(px - 4 * u, gy - jump - ph - 4 * u, px + pw + 4 * u, gy - jump + 4 * u);
            p.setColor(0x4045E0C1);
            c.drawRoundRect(rf, 10 * u, 10 * u, p);
            rf.set(px, gy - jump - ph, px + pw, gy - jump);
            p.setColor(0xFF45E0C1);
            c.drawRoundRect(rf, 8 * u, 8 * u, p);
            p.setColor(0xFF0B0620);
            c.drawCircle(px + pw * 0.7f, rf.top + ph * 0.3f, 4 * u, p);
            p.setColor(0xFFFFFFFF);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(hh * 0.045f);
            c.drawText("Neon Run   Score " + (int) score + "   Best " + best, w * 0.04f, hh * 0.08f, p);
            p.setTextSize(hh * 0.028f);
            p.setColor(0xCCFFFFFF);
            c.drawText("UP or OK = jump      DOWN = duck under yellow bars", w * 0.04f, hh * 0.95f, p);
            if (dead) {
                p.setColor(0xFFFFFFFF);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(hh * 0.08f);
                c.drawText("Game over - press OK", w / 2f, hh * 0.45f, p);
            }
        }
    }
}