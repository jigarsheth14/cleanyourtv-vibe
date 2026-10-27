package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayDeque;
import java.util.Random;

public class SnakeActivity extends Activity {
    private SnakeView v;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        v = new SnakeView(this);
        setContentView(v);
    }

    @Override protected void onResume() { super.onResume(); v.start(); }
    @Override protected void onPause() { super.onPause(); v.stop(); }

    @Override public boolean onKeyDown(int k, KeyEvent e) {
        if (v.key(k)) return true;
        return super.onKeyDown(k, e);
    }

    static class SnakeView extends View {
        static final int COLS = 32, ROWS = 18;
        private final Handler h = new Handler(Looper.getMainLooper());
        private final ArrayDeque<Integer> body = new ArrayDeque<>();
        private final Random r = new Random();
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rf = new RectF();
        private int dx = 1, dy = 0, ndx = 1, ndy = 0, food, best;
        private boolean dead;
        private final Runnable step = new Runnable() {
            @Override public void run() {
                tick();
                invalidate();
                h.postDelayed(this, 130);
            }
        };

        SnakeView(Context c) {
            super(c);
            best = G.sp(c).getInt("snake_best", 0);
            reset();
        }

        void start() { stop(); h.post(step); }
        void stop() { h.removeCallbacks(step); }

        void reset() {
            body.clear();
            int row = ROWS / 2 * COLS;
            body.addLast(row + 5);
            body.addLast(row + 6);
            body.addLast(row + 7);
            dx = 1; dy = 0; ndx = 1; ndy = 0;
            dead = false;
            placeFood();
        }

        void placeFood() {
            do { food = r.nextInt(COLS * ROWS); } while (body.contains(food));
        }

        void tick() {
            if (dead) return;
            dx = ndx;
            dy = ndy;
            int head = body.peekLast();
            int x = (head % COLS + dx + COLS) % COLS;
            int y = (head / COLS + dy + ROWS) % ROWS;
            int n = y * COLS + x;
            if (body.contains(n)) {
                dead = true;
                int sc = body.size() - 3;
                if (sc > best) {
                    best = sc;
                    G.sp(getContext()).edit().putInt("snake_best", best).apply();
                }
                return;
            }
            body.addLast(n);
            if (n == food) placeFood(); else body.pollFirst();
        }

        boolean key(int k) {
            if (k == KeyEvent.KEYCODE_DPAD_LEFT) { if (dx != 1) { ndx = -1; ndy = 0; } return true; }
            if (k == KeyEvent.KEYCODE_DPAD_RIGHT) { if (dx != -1) { ndx = 1; ndy = 0; } return true; }
            if (k == KeyEvent.KEYCODE_DPAD_UP) { if (dy != 1) { ndx = 0; ndy = -1; } return true; }
            if (k == KeyEvent.KEYCODE_DPAD_DOWN) { if (dy != -1) { ndx = 0; ndy = 1; } return true; }
            if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) { if (dead) reset(); return true; }
            return false;
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), hh = getHeight();
            c.drawColor(0xFF0B1230);
            float cell = Math.min(w / (float) COLS, hh * 0.9f / ROWS);
            float ox = (w - cell * COLS) / 2f;
            float oy = hh - cell * ROWS - hh * 0.02f;
            p.setColor(0xFF16204A);
            c.drawRect(ox, oy, ox + cell * COLS, oy + cell * ROWS, p);
            p.setColor(0xFF45E0C1);
            for (int cellId : body) {
                float x = ox + (cellId % COLS) * cell, y = oy + (cellId / COLS) * cell;
                rf.set(x + 1, y + 1, x + cell - 1, y + cell - 1);
                c.drawRoundRect(rf, cell * 0.3f, cell * 0.3f, p);
            }
            p.setColor(0xFFFF4D6D);
            c.drawCircle(ox + (food % COLS + 0.5f) * cell, oy + (food / COLS + 0.5f) * cell, cell * 0.4f, p);
            p.setColor(0xFFFFFFFF);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(hh * 0.045f);
            c.drawText("Snake   Score " + (body.size() - 3) + "   Best " + best, ox, hh * 0.06f, p);
            if (dead) {
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(hh * 0.08f);
                c.drawText("Game over - press OK", w / 2f, hh * 0.5f, p);
            }
        }
    }
}