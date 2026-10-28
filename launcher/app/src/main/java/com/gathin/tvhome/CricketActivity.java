package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CricketActivity extends Activity {
    private float u;
    private LinearLayout list;
    private TextView updated;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            Cricket.load(CricketActivity.this, l -> render(l));
            h.postDelayed(this, 60000);
        }
    };

    private int p(float v) { return Math.round(v * u); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        u = G.u(this);
        FrameLayout root = Look.page(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(p(110), p(40), p(110), p(20));
        col.addView(G.tv(this, "Cricket", p(38), Color.WHITE, true));
        TextView sub = G.tv(this, "India (men) and IPL  -  live scores, fixtures and results", p(16), 0xCCFFFFFF, false);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-2, -2);
        sl.topMargin = p(6);
        col.addView(sub, sl);
        updated = G.tv(this, "", p(13), 0x99FFFFFF, false);
        LinearLayout.LayoutParams ul = new LinearLayout.LayoutParams(-2, -2);
        ul.topMargin = p(4);
        ul.bottomMargin = p(14);
        col.addView(updated, ul);
        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        col.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    @Override protected void onResume() { super.onResume(); h.post(refresh); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacksAndMessages(null); }

    private TextView header(String s) {
        TextView t = G.tv(this, s, p(15), 0xFFFFD54A, true);
        t.setPadding(0, p(10), 0, p(6));
        return t;
    }

    private void render(List<Cricket.Match> ms) {
        long up = Cricket.updatedAt(this);
        updated.setText(up > 0 ? "Updated " + new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date(up)) : "");
        list.removeAllViews();
        if (ms.isEmpty()) {
            list.addView(G.tv(this, "No India (men) or IPL match in the feed right now.", p(20), Color.WHITE, false));
            return;
        }
        int lastGroup = -1;
        View first = null;
        for (Cricket.Match m : ms) {
            int g = m.live ? 0 : m.upcoming ? 1 : 2;
            if (g != lastGroup) {
                list.addView(header(g == 0 ? "LIVE NOW" : g == 1 ? "COMING UP" : "RESULTS"));
                lastGroup = g;
            }
            View row = row(m);
            if (first == null) first = row;
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
            rl.bottomMargin = p(10);
            list.addView(row, rl);
        }
        if (first != null) first.requestFocus();
    }

    private View row(Cricket.Match m) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(p(20), p(14), p(20), p(14));
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(p(16));
        bg.setColor(0x33FFFFFF);
        bg.setStroke(p(1), 0x55FFFFFF);
        r.setBackground(bg);
        r.setFocusable(true);
        r.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? 0x66FFFFFF : 0x33FFFFFF);
            bg.setStroke(has ? p(3) : p(1), has ? 0xFFFFFFFF : 0x55FFFFFF);
        });
        r.addView(G.tv(this, (m.live ? "LIVE   " : "") + m.teams, p(24), m.live ? 0xFF7CF0A8 : Color.WHITE, true));
        if (!m.series.isEmpty()) r.addView(G.tv(this, m.series, p(13), 0x99FFFFFF, false));
        if (!m.line.isEmpty()) {
            TextView s = G.tv(this, m.line, p(20), Color.WHITE, true);
            s.setPadding(0, p(6), 0, 0);
            r.addView(s);
        }
        if (!m.status.isEmpty()) {
            TextView s = G.tv(this, m.status, p(16), 0xDDFFFFFF, false);
            s.setPadding(0, p(4), 0, 0);
            r.addView(s);
        }
        return r;
    }
}