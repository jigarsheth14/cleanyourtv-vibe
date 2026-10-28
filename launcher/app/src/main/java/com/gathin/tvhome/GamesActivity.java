package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GamesActivity extends Activity {
    private float u;
    private TextView snake, g2048, runner;

    private int p(float v) { return Math.round(v * u); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        u = G.u(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackground(G.bg());
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setClipChildren(false);
        runner = tile(0xFF8E2DE2, RunnerActivity.class);
        snake = tile(0xFF2E8B57, SnakeActivity.class);
        g2048 = tile(0xFFB5651D, Game2048Activity.class);
        for (TextView t : new TextView[]{runner, snake, g2048}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(p(220), p(300));
            lp.setMargins(p(14), 0, p(14), 0);
            row.addView(t, lp);
        }
        root.addView(row, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        runner.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        runner.setText("Neon Run\n\nBest: " + G.sp(this).getInt("runner_best", 0));
        snake.setText("Snake\n\nBest: " + G.sp(this).getInt("snake_best", 0));
        g2048.setText("2048\n\nBest: " + G.sp(this).getInt("g2048_best", 0));
    }

    private TextView tile(int color, final Class<?> cls) {
        final GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(p(26));
        TextView t = G.tv(this, "", p(28), Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(d);
        t.setFocusable(true);
        t.setOnClickListener(v -> startActivity(new Intent(this, cls)));
        t.setOnFocusChangeListener((v, has) -> {
            float s = has ? 1.08f : 1f;
            v.animate().scaleX(s).scaleY(s).setDuration(160).start();
            d.setStroke(has ? p(4) : 0, Color.WHITE);
        });
        return t;
    }
}