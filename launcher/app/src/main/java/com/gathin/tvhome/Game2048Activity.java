package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Game2048Activity extends Activity {
    private GameView v;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        v = new GameView(this);
        setContentView(v);
    }

    @Override public boolean onKeyDown(int k, KeyEvent e) {
        if (v.key(k)) return true;
        return super.onKeyDown(k, e);
    }

    static class GameView extends View {
        private final int[][] g = new int[4][4];
        private final Random r = new Random();
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rf = new RectF();
        private int score, best;
        private boolean over;
        private final int[] col = {0x22FFFFFF, 0xFFEEE4DA, 0xFFEDE0C8, 0xFFF2B179, 0xFFF59563, 0xFFF67C5F,
                0xFFF65E3B, 0xFFEDCF72, 0xFFEDCC61, 0xFFEDC850, 0xFFEDC53F, 0xFFEDC22E};

        GameView(Context c) {
            super(c);
            best = G.sp(c).getInt("g2048_best", 0);
            reset();
        }

        void reset() {
            for (int[] row : g) Arrays.fill(row, 0);
            score = 0;
            over = false;
            spawn();
            spawn();
            invalidate();
        }

        void spawn() {
            List<int[]> e = new ArrayList<>();
            for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) if (g[i][j] == 0) e.add(new int[]{i, j});
            if (e.isEmpty()) return;
            int[] c = e.get(r.nextInt(e.size()));
            g[c[0]][c[1]] = r.nextInt(10) == 0 ? 4 : 2;
        }

        int get(int d, int i, int j) {
            switch (d) {
                case 0: return g[i][j];
                case 2: return g[i][3 - j];
                case 1: return g[j][i];
                default: return g[3 - j][i];
            }
        }

        void set(int d, int i, int j, int val) {
            switch (d) {
                case 0: g[i][j] = val; break;
                case 2: g[i][3 - j] = val; break;
                case 1: g[j][i] = val; break;
                default: g[3 - j][i] = val;
            }
        }

        boolean move(int d) {
            boolean moved = false;
            for (int i = 0; i < 4; i++) {
                int[] line = new int[4];
                for (int j = 0; j < 4; j++) line[j] = get(d, i, j);
                int[] out = new int[4];
                int n = 0, last = 0;
                for (int j = 0; j < 4; j++) {
                    int val = line[j];
                    if (val == 0) continue;
                    if (last == val) { out[n - 1] = val * 2; score += val * 2; last = 0; }
                    else { out[n++] = val; last = val; }
                }
                for (int j = 0; j < 4; j++) {
                    if (out[j] != line[j]) moved = true;
                    set(d, i, j, out[j]);
                }
            }
            return moved;
        }

        boolean canMove() {
            for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
                if (g[i][j] == 0) return true;
                if (j < 3 && g[i][j] == g[i][j + 1]) return true;
                if (i < 3 && g[i][j] == g[i + 1][j]) return true;
            }
            return false;
        }

        boolean key(int k) {
            if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) { if (over) reset(); return true; }
            int d = k == KeyEvent.KEYCODE_DPAD_LEFT ? 0 : k == KeyEvent.KEYCODE_DPAD_UP ? 1
                    : k == KeyEvent.KEYCODE_DPAD_RIGHT ? 2 : k == KeyEvent.KEYCODE_DPAD_DOWN ? 3 : -1;
            if (d < 0) return false;
            if (!over && move(d)) {
                spawn();
                if (score > best) {
                    best = score;
                    G.sp(getContext()).edit().putInt("g2048_best", best).apply();
                }
                if (!canMove()) over = true;
            }
            invalidate();
            return true;
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            c.drawColor(0xFF0B1230);
            float s = Math.min(w, h) * 0.78f;
            float left = (w - s) / 2f, top = (h - s) / 2f + h * 0.04f;
            float t = s / 4f, gap = t * 0.06f;
            p.setColor(0xFF16204A);
            rf.set(left - gap, top - gap, left + s + gap, top + s + gap);
            c.drawRoundRect(rf, t * 0.12f, t * 0.12f, p);
            for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
                int val = g[i][j];
                int idx = val == 0 ? 0 : Math.min(11, Integer.numberOfTrailingZeros(val));
                p.setColor(col[idx]);
                rf.set(left + j * t + gap, top + i * t + gap, left + (j + 1) * t - gap, top + (i + 1) * t - gap);
                c.drawRoundRect(rf, t * 0.1f, t * 0.1f, p);
                if (val != 0) {
                    p.setColor(val <= 4 ? 0xFF4A3F35 : 0xFFFFFFFF);
                    p.setTextAlign(Paint.Align.CENTER);
                    p.setFakeBoldText(true);
                    p.setTextSize(t * (val < 100 ? 0.45f : val < 1000 ? 0.36f : 0.28f));
                    c.drawText(String.valueOf(val), rf.centerX(), rf.centerY() - (p.descent() + p.ascent()) / 2f, p);
                }
            }
            p.setColor(0xFFFFFFFF);
            p.setFakeBoldText(false);
            p.setTextAlign(Paint.Align.LEFT);
            p.setTextSize(h * 0.045f);
            c.drawText("2048   Score " + score + "   Best " + best, left, h * 0.07f, p);
            if (over) {
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(h * 0.08f);
                c.drawText("Game over - press OK", w / 2f, h * 0.5f, p);
            }
        }
    }
}