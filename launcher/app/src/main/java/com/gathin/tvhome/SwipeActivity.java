package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class SwipeActivity extends Activity {
    private final List<String[]> queue = new ArrayList<>();
    private int pos;
    private boolean busy;
    private float u;
    private FrameLayout card;
    private ImageView poster;
    private TextView title, genre, count, stamp;
    private String shown = "";

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
        cl.topMargin = p(22);
        root.addView(count, cl);

        card = new FrameLayout(this);
        card.setClipToOutline(true);
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), p(26)); }
        });
        poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setVisibility(View.GONE);
        card.addView(poster, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout label = new LinearLayout(this);
        label.setOrientation(LinearLayout.VERTICAL);
        label.setPadding(p(18), p(40), p(18), p(18));
        label.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0xDD000000}));
        title = G.tv(this, "", p(28), Color.WHITE, true);
        genre = G.tv(this, "", p(16), 0xDDFFFFFF, false);
        label.addView(title);
        label.addView(genre);
        card.addView(label, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        stamp = G.tv(this, "", p(34), Color.WHITE, true);
        stamp.setVisibility(View.GONE);
        FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        sl.topMargin = p(22);
        card.addView(stamp, sl);
        root.addView(card, new FrameLayout.LayoutParams(p(300), p(420), Gravity.CENTER));

        TextView hint = G.tv(this, "LEFT = not for me      RIGHT = like it      OK = skip      BACK = done", p(16), 0xCCFFFFFF, false);
        FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hl.bottomMargin = p(18);
        root.addView(hint, hl);
        setContentView(root);
        show();
    }

    private void setColor(String s) {
        float hue = Math.abs(s.hashCode()) % 360;
        card.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
                Color.HSVToColor(new float[]{hue, 0.55f, 0.75f}), Color.HSVToColor(new float[]{hue, 0.65f, 0.35f})}));
    }

    private void show() {
        count.setText(Math.max(0, queue.size() - pos) + " left to rate");
        stamp.setVisibility(View.GONE);
        poster.setVisibility(View.GONE);
        if (pos >= queue.size()) {
            shown = "";
            title.setText("All rated!");
            genre.setText("See For you on Home");
            setColor("done");
            return;
        }
        final String[] t = queue.get(pos);
        shown = t[0];
        title.setText(t[0]);
        genre.setText(t[1]);
        setColor(t[0]);
        Posters.load(this, t[0], bm -> {
            if (shown.equals(t[0])) {
                poster.setImageBitmap(bm);
                poster.setVisibility(View.VISIBLE);
            }
        });
    }

    private void rate(final int v) {
        if (pos >= queue.size()) return;
        G.sp(this).edit().putInt("r_" + queue.get(pos)[0], v).apply();
        busy = true;
        stamp.setText(v > 0 ? "LIKE" : "NOPE");
        stamp.setTextColor(v > 0 ? 0xFF4CE08A : 0xFFFF5A6E);
        stamp.setVisibility(View.VISIBLE);
        card.animate().translationX(v * p(280)).rotation(v * 12f).alpha(0f).setDuration(200).withEndAction(() -> {
            card.setTranslationX(0);
            card.setRotation(0);
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