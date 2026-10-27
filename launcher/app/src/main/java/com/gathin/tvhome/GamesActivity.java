package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Arcade: Snake and 2048. Arrow keys play, OK restarts after a game over, Back leaves. */
public class GamesActivity extends Activity {
    private static final int COLS = 32, ROWS = 18;
    private float u;
    private boolean inGame;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        u = getResources().getDisplayMetrics().heightPixels / 540f;
        showMenu();
    }

    @Override public void onBackPressed() {
        if (inGame) showMenu(); else finish();
    }

    private int p(float v) { return Math.round(v * u); }

    // ---- menu ----

    private void showMenu() {
        inGame = false;
        FrameLayout root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF0B1230, 0xFF2B2A6B}));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        root.addView(row, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View snake = card("Snake", "Eat, grow, stay alive", new Runnable() {
            @Override public void run() { showGame(new SnakeView()); }
        });
        View g2048 = card("2048", "Slide and merge tiles", new Runnable() {
            @Override public void run() { showGame(new Game2048View()); }
        });
        row.addView(snake, cardParams());
        row.addView(g2048, cardParams());

        TextView hint = new TextView(this);
        hint.setText("ARCADE   \u00B7   Back to return home");
        hint.setTextColor(0x99FFFFFF);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_PX, p(14));
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hl.bottomMargin = p(30);
        root.addView(hint, hl);

        setContentView(root);
        snake.requestFocus();
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(p(300), p(200));
        l.setMargins(p(18), p(18), p(18), p(18));
        return l;
    }

    private View card(String title, String sub, final Runnable action) {
        final GradientDrawable d = new GradientDrawable();
        d.setColor(0x33FFFFFF);
        d.setCornerRadius(p(26));
        d.setStroke(Math.max(1, p(1.5f)), 0x66FFFFFF);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setBackground(d);
        c.setFocusable(true);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, p(38));
        t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        t.setGravity(Gravity.CENTER);
        c.addView(t);
        TextView s = new TextView(this);
        s.setText(sub);
        s.setTextColor(0xCCFFFFFF);
        s.setTextSize(TypedValue.COMPLEX_UNIT_PX, p(15));
        s.setGravity(Gravity.CENTER);
        c.addView(s);
        c.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        c.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                float sc = has ? 1.08f : 1f;
                v.animate().scaleX(sc).scaleY(sc).setDuration(160).start();
                v.setTranslationZ(has ? 10 : 0);
                d.setColor(has ? 0x55FFFFFF : 0x33FFFFFF);
                d.setStroke(Math.max(1, p(has ? 3f : 1.5f)), has ? 0xFFFFFFFF : 0x66FFFFFF);
            }
        });
        return c;
    }

    private void showGame(View v) {
        inGame = true;
        setContentView(v);
        v.requestFocus();
    }

    // ---- Snake ----

    private class SnakeView extends View {
        private final int[] dx = {0, 1, 0, -1};
        private final int[] dy = {-1, 0, 1, 0};
        private final int[] sx = new int[COLS * ROWS];
        private final int[] sy = new int[COLS * ROWS];
        private int len, dir, nextDir, fx, fy, score;
        private boolean dead;
        private final Random rnd = new Random();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable step = new Runnable() {
            @Override public void run() {
                if (dead) return;
                advance();
                invalidate();
                if (!dead) handler.postDelayed(this, Math.max(70, 150 - score * 3));
            }
        };

        SnakeView() {
            super(GamesActivity.this);
            setFocusable(true);
            setFocusableInTouchMode(true);
            reset();
        }

        private void reset() {
            len = 4;
            for (int i = 0; i < len; i++) { sx[i] = COLS / 2 - i; sy[i] = ROWS / 2; }
            dir = 1;
            nextDir = 1;
            score = 0;
            dead = false;
            placeFood();
            handler.removeCallbacks(step);
            handler.postDelayed(step, 300);
            invalidate();
        }

        private void placeFood() {
            while (true) {
                fx = rnd.nextInt(COLS);
                fy = rnd.nextInt(ROWS);
                boolean clash = false;
                for (int i = 0; i < len; i++) if (sx[i] == fx && sy[i] == fy) { clash = true; break; }
                if (!clash) return;
            }
        }

        private void advance() {
            dir = nextDir;
            int nx = (sx[0] + dx[dir] + COLS) % COLS;
            int ny = (sy[0] + dy[dir] + ROWS) % ROWS;
            for (int i = 0; i < len - 1; i++) {
                if (sx[i] == nx && sy[i] == ny) { dead = true; return; }
            }
            boolean eat = nx == fx && ny == fy;
            if (eat && len < sx.length) len++;
            for (int i = len - 1; i > 0; i--) { sx[i] = sx[i - 1]; sy[i] = sy[i - 1]; }
            sx[0] = nx;
            sy[0] = ny;
            if (eat) { score++; placeFood(); }
        }

        @Override public boolean onKeyDown(int keyCode, KeyEvent e) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP: if (dir != 2) nextDir = 0; return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT: if (dir != 3) nextDir = 1; return true;
                case KeyEvent.KEYCODE_DPAD_DOWN: if (dir != 0) nextDir = 2; return true;
                case KeyEvent.KEYCODE_DPAD_LEFT: if (dir != 1) nextDir = 3; return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (dead) reset();
                    return true;
                default: return super.onKeyDown(keyCode, e);
            }
        }

        @Override protected void onDetachedFromWindow() {
            handler.removeCallbacksAndMessages(null);
            super.onDetachedFromWindow();
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            paint.setShader(new LinearGradient(0, 0, 0, h, 0xFF0B1230, 0xFF1B2A6B, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            float cw = w / (float) COLS, ch = h / (float) ROWS;
            float pad = Math.max(1f, cw * 0.06f);

            paint.setColor(0xFFFF5D8F);
            c.drawCircle((fx + 0.5f) * cw, (fy + 0.5f) * ch, Math.min(cw, ch) * 0.38f, paint);

            for (int i = len - 1; i >= 0; i--) {
                float t = i / (float) Math.max(1, len);
                paint.setColor(i == 0 ? 0xFFFFFFFF
                        : Color.rgb(60 + (int) (120 * (1 - t)), 205 - (int) (70 * t), 175));
                rect.set(sx[i] * cw + pad, sy[i] * ch + pad, (sx[i] + 1) * cw - pad, (sy[i] + 1) * ch - pad);
                c.drawRoundRect(rect, cw * 0.3f, cw * 0.3f, paint);
            }

            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            paint.setTextSize(p(22));
            paint.setColor(0xCCFFFFFF);
            c.drawText("Score " + score, p(24), p(40), paint);

            if (dead) {
                c.drawColor(0x99000000);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(Color.WHITE);
                paint.setTextSize(p(48));
                c.drawText("Game over", w / 2f, h / 2f - p(10), paint);
                paint.setTextSize(p(20));
                paint.setColor(0xCCFFFFFF);
                c.drawText("Score " + score + "   \u00B7   press OK to play again   \u00B7   Back for menu",
                        w / 2f, h / 2f + p(36), paint);
            }
        }
    }

    // ---- 2048 ----

    private class Game2048View extends View {
        private final int[][] g = new int[4][4];
        private int score;
        private boolean over;
        private final Random rnd = new Random();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        Game2048View() {
            super(GamesActivity.this);
            setFocusable(true);
            setFocusableInTouchMode(true);
            reset();
        }

        private void reset() {
            for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) g[r][c] = 0;
            score = 0;
            over = false;
            addTile();
            addTile();
            invalidate();
        }

        private void addTile() {
            List<int[]> empty = new ArrayList<>();
            for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) if (g[r][c] == 0) empty.add(new int[]{r, c});
            if (empty.isEmpty()) return;
            int[] cell = empty.get(rnd.nextInt(empty.size()));
            g[cell[0]][cell[1]] = rnd.nextInt(10) == 0 ? 4 : 2;
        }

        private int[] slide(int[] in) {
            int[] out = new int[4];
            int idx = 0, last = 0;
            for (int v : in) {
                if (v == 0) continue;
                if (last != 0 && last == v) {
                    out[idx - 1] = v * 2;
                    score += v * 2;
                    last = 0;
                } else {
                    out[idx++] = v;
                    last = v;
                }
            }
            return out;
        }

        /** dir: 0 left, 1 right, 2 up, 3 down. */
        private void move(int dir) {
            boolean changed = false;
            for (int k = 0; k < 4; k++) {
                int[] in = new int[4];
                for (int i = 0; i < 4; i++) in[i] = read(dir, k, i);
                int[] out = slide(in);
                for (int i = 0; i < 4; i++) {
                    if (in[i] != out[i]) changed = true;
                    write(dir, k, i, out[i]);
                }
            }
            if (changed) {
                addTile();
                over = !canMove();
            }
            invalidate();
        }

        private int read(int dir, int k, int i) {
            switch (dir) {
                case 0: return g[k][i];
                case 1: return g[k][3 - i];
                case 2: return g[i][k];
                default: return g[3 - i][k];
            }
        }

        private void write(int dir, int k, int i, int v) {
            switch (dir) {
                case 0: g[k][i] = v; break;
                case 1: g[k][3 - i] = v; break;
                case 2: g[i][k] = v; break;
                default: g[3 - i][k] = v; break;
            }
        }

        private boolean canMove() {
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    if (g[r][c] == 0) return true;
                    if (c < 3 && g[r][c] == g[r][c + 1]) return true;
                    if (r < 3 && g[r][c] == g[r + 1][c]) return true;
                }
            }
            return false;
        }

        @Override public boolean onKeyDown(int keyCode, KeyEvent e) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT: if (!over) move(0); return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT: if (!over) move(1); return true;
                case KeyEvent.KEYCODE_DPAD_UP: if (!over) move(2); return true;
                case KeyEvent.KEYCODE_DPAD_DOWN: if (!over) move(3); return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (over) reset();
                    return true;
                default: return super.onKeyDown(keyCode, e);
            }
        }

        private int tileColor(int v) {
            switch (v) {
                case 0: return 0x33FFFFFF;
                case 2: return 0xFFEEE4DA;
                case 4: return 0xFFEDE0C8;
                case 8: return 0xFFF2B179;
                case 16: return 0xFFF59563;
                case 32: return 0xFFF67C5F;
                case 64: return 0xFFF65E3B;
                case 128: return 0xFFEDCF72;
                case 256: return 0xFFEDCC61;
                case 512: return 0xFFEDC850;
                case 1024: return 0xFFEDC53F;
                default: return 0xFFEDC22E;
            }
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            paint.setShader(new LinearGradient(0, 0, 0, h, 0xFF0B1230, 0xFF2B2A6B, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, paint);
            paint.setShader(null);

            float size = Math.min(w, h) * 0.86f;
            float left = (w - size) / 2f, top = (h - size) / 2f;
            float gap = size * 0.025f;
            float cell = (size - gap * 5f) / 4f;

            paint.setColor(0x30FFFFFF);
            rect.set(left, top, left + size, top + size);
            c.drawRoundRect(rect, cell * 0.18f, cell * 0.18f, paint);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            for (int r = 0; r < 4; r++) {
                for (int col = 0; col < 4; col++) {
                    float x = left + gap + col * (cell + gap);
                    float y = top + gap + r * (cell + gap);
                    int v = g[r][col];
                    paint.setColor(tileColor(v));
                    rect.set(x, y, x + cell, y + cell);
                    c.drawRoundRect(rect, cell * 0.12f, cell * 0.12f, paint);
                    if (v != 0) {
                        paint.setColor(v <= 4 ? 0xFF776E65 : Color.WHITE);
                        paint.setTextSize(cell * (v < 100 ? 0.48f : v < 1000 ? 0.4f : 0.32f));
                        c.drawText(String.valueOf(v), x + cell / 2f,
                                y + cell / 2f - (paint.descent() + paint.ascent()) / 2f, paint);
                    }
                }
            }

            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(p(22));
            paint.setColor(0xCCFFFFFF);
            c.drawText("Score " + score, p(24), p(40), paint);
            paint.setTextSize(p(14));
            paint.setColor(0x99FFFFFF);
            c.drawText("Arrow keys to slide   \u00B7   Back for menu", p(24), h - p(24), paint);

            if (over) {
                c.drawColor(0x99000000);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(Color.WHITE);
                paint.setTextSize(p(48));
                c.drawText("Game over", w / 2f, h / 2f - p(10), paint);
                paint.setTextSize(p(20));
                paint.setColor(0xCCFFFFFF);
                c.drawText("Score " + score + "   \u00B7   press OK to play again",
                        w / 2f, h / 2f + p(36), paint);
            }
        }
    }
}
