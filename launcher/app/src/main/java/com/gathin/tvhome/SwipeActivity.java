package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class SwipeActivity extends Activity {
    private final List<String[]> queue = new ArrayList<>();
    private int pos;
    private boolean busy;
    private float u;
    private LinearLayout card;
    private TextView title, genre, count;

    private int p(float v) { return Math.round(v * u); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        u = G.u(this);
        for (String[] t : Titles.ALL) if (G.sp(this).getInt("r_" + t[0], 0) == 0) queue.add(t);
        FrameLayout root = new FrameLayout(this);
        root.setBackground(G.bg());
        count = G.tv(this, "", p(18), 0xCCFFFFFF, false);
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        cl.topMargin = p(28);
        root.addView(count, cl);
        card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(p(20), p(20), p(20), p(20));
        title = G.tv(this, "", p(34), Color.WHITE, true);
        title.setGravity(Gravity.CENTER);
        genre = G.tv(this, "", p(18), 0xDDFFFFFF, false);
        genre.setGravity(Gravity.CENTER);
        card.addView(title);
        card.addView(genre);
        root.addView(card, new FrameLayout.LayoutParams(p(300), p(380), Gravity.CENTER));
        TextView hint = G.tv(this, "LEFT = not for me      RIGHT = like it      OK = skip      BACK = done", p(16), 0xCCFFFFFF, false);
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hl.bottomMargin = p(26);
        root.addView(hint, hl);
        setContentView(root);
        show();
    }

    private void setColor(String s) {
        float hue = Math.abs(s.hashCode()) % 360;
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
                Color.HSVToColor(new float[]{hue, 0.55f, 0.75f}), Color.HSVToColor(new float[]{hue, 0.65f, 0.35f})});
        g.setCornerRadius(p(26));
        card.setBackground(g);
    }

    private void show() {
        count.setText(Math.max(0, queue.size() - pos) + " left to rate");
        if (pos >= queue.size()) {
            title.setText("All rated!");
            genre.setText("See For you on Home");
            setColor("done");
            return;
        }
        String[] t = queue.get(pos);
        title.setText(t[0]);
        genre.setText(t[1]);
        setColor(t[0]);
    }

    private void rate(final int v) {
        if (pos >= queue.size()) return;
        G.sp(this).edit().putInt("r_" + queue.get(pos)[0], v).apply();
        busy = true;
        card.animate().translationX(v * p(260)).alpha(0f).setDuration(180).withEndAction(() -> {
            card.setTranslationX(0);
            card.setAlpha(1f);
            pos++;
            busy = false;
            show();
        }).start();
    }

    @Override public boolean onKeyDown(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_BACK) return super.onKeyDown(k, e);
        if (busy) return true;
        if (k == KeyEvent.KEYCODE_DPAD_LEFT) { rate(-1); return true; }
        if (k == KeyEvent.KEYCODE_DPAD_RIGHT) { rate(1); return true; }
        if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) {
            if (pos < queue.size()) { pos++; show(); }
            return true;
        }
        return super.onKeyDown(k, e);
    }
}