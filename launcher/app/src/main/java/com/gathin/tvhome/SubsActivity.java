package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class SubsActivity extends Activity {
    private float u;
    private List<Subs.Item> items;

    private int p(float v) { return Math.round(v * u); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        u = G.u(this);
        items = Subs.load(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackground(G.bg());
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(p(120), p(50), p(120), p(30));
        col.addView(G.tv(this, "Renewals", p(40), Color.WHITE, true));
        TextView hint = G.tv(this, "UP/DOWN: choose    LEFT/RIGHT: change date by a day    OK: +1 month    Plans roll over by themselves after the date.", p(15), 0xCCFFFFFF, false);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2);
        hl.topMargin = p(8);
        hl.bottomMargin = p(22);
        col.addView(hint, hl);
        View first = null;
        for (Subs.Item it : items) {
            View r = row(it);
            if (first == null) first = r;
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
            rl.bottomMargin = p(10);
            col.addView(r, rl);
        }
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        if (first != null) first.requestFocus();
    }

    private View row(final Subs.Item it) {
        final LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(p(20), p(14), p(20), p(14));
        TextView name = G.tv(this, it.name, p(24), Color.WHITE, true);
        final TextView date = G.tv(this, "", p(17), 0xCCFFFFFF, false);
        final TextView days = G.tv(this, "", p(24), Color.WHITE, true);
        r.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-2, -2);
        dl.rightMargin = p(24);
        r.addView(date, dl);
        r.addView(days);
        final Runnable upd = () -> {
            date.setText("Auto-renews " + Subs.dateText(it));
            days.setText(Subs.daysText(it));
        };
        upd.run();
        final GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(p(16));
        bg.setColor(0x22FFFFFF);
        r.setBackground(bg);
        r.setFocusable(true);
        r.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? 0x55FFFFFF : 0x22FFFFFF);
            bg.setStroke(has ? p(2) : 0, Color.WHITE);
        });
        r.setOnKeyListener((v, k, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (k == KeyEvent.KEYCODE_DPAD_LEFT) it.date = Subs.addDays(it.date, -1);
            else if (k == KeyEvent.KEYCODE_DPAD_RIGHT) it.date = Subs.addDays(it.date, 1);
            else if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER) it.date = Subs.addMonth(it.date);
            else return false;
            if (it.date < Subs.today()) it.date = Subs.today();
            Subs.save(this, items);
            upd.run();
            return true;
        });
        return r;
    }
}