package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Calendar;
import java.util.List;

public class SubsActivity extends Activity {
    private float u;
    private List<Subs.Item> items;
    private int focusIndex = 0;

    private int p(float v) { return Math.round(v * u); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        u = G.u(this);
        build();
    }

    private void build() {
        items = Subs.load(this);
        FrameLayout root = Look.page(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(p(110), p(40), p(110), p(24));
        col.addView(G.tv(this, "Renewals", p(38), Color.WHITE, true));
        TextView hint = G.tv(this, "OK = set the renewal date      Hold OK = more      Dates move forward by themselves after they pass", p(15), 0xCCFFFFFF, false);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2);
        hl.topMargin = p(8);
        hl.bottomMargin = p(18);
        col.addView(hint, hl);
        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < items.size(); i++) {
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
            rl.bottomMargin = p(10);
            list.addView(row(items.get(i), i), rl);
        }
        LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(-1, -2);
        al.bottomMargin = p(10);
        list.addView(addRow(), al);
        sv.addView(list);
        col.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        View target = list.getChildAt(Math.min(focusIndex, list.getChildCount() - 1));
        if (target != null) target.requestFocus();
    }

    private LinearLayout shell() {
        final LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
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
        return r;
    }

    private View row(final Subs.Item it, final int idx) {
        LinearLayout r = shell();
        r.addView(G.tv(this, it.name, p(24), Color.WHITE, true), new LinearLayout.LayoutParams(0, -2, 1f));
        String info = it.date <= 0 ? "Date not set - press OK"
                : "Renews " + Subs.dateText(it) + "  -  " + (it.months == 12 ? "yearly" : "monthly");
        TextView t = G.tv(this, info, p(17), 0xDDFFFFFF, false);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.rightMargin = p(26);
        r.addView(t, tl);
        r.addView(G.tv(this, Subs.daysText(it), p(24), Color.WHITE, true));
        r.setOnClickListener(v -> pickDate(it, idx));
        r.setOnLongClickListener(v -> { more(it, idx); return true; });
        return r;
    }

    private View addRow() {
        LinearLayout r = shell();
        r.addView(G.tv(this, "+  Add a subscription", p(22), Color.WHITE, true));
        r.setOnClickListener(v -> addSub());
        return r;
    }

    private void pickDate(final Subs.Item it, final int idx) {
        Calendar c = Calendar.getInstance();
        if (it.date > 0) c.setTimeInMillis(it.date);
        new DatePickerDialog(this, AlertDialog.THEME_HOLO_DARK, (view, y, m, d) -> {
            Calendar s = Calendar.getInstance();
            s.set(y, m, d, 0, 0, 0);
            s.set(Calendar.MILLISECOND, 0);
            it.date = s.getTimeInMillis();
            while (it.date < Subs.today()) it.date = Subs.addMonths(it.date, Math.max(1, it.months));
            Subs.save(this, items);
            focusIndex = idx;
            build();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void more(final Subs.Item it, final int idx) {
        String[] opts = {"Set the date", it.months == 12 ? "Switch to monthly" : "Switch to yearly", "Remove"};
        new AlertDialog.Builder(this).setTitle(it.name).setItems(opts, (dlg, which) -> {
            if (which == 0) { pickDate(it, idx); return; }
            if (which == 1) it.months = it.months == 12 ? 1 : 12;
            else items.remove(it);
            Subs.save(this, items);
            focusIndex = Math.max(0, idx - 1);
            build();
        }).show();
    }

    private void addSub() {
        final String[] names = {"JioHotstar", "Netflix", "SonyLIV", "Prime Video", "ZEE5", "YouTube Premium",
                "Apple TV+", "Disney+", "Spotify", "Other"};
        new AlertDialog.Builder(this).setTitle("Add a subscription").setItems(names, (dlg, w) -> {
            String n = names[w].equals("Other") ? "Subscription" : names[w];
            for (Subs.Item x : items) if (x.name.equals(n)) { n = n + " 2"; break; }
            Subs.Item it = new Subs.Item(n, 0, 1);
            items.add(it);
            Subs.save(this, items);
            focusIndex = items.size() - 1;
            build();
            pickDate(it, focusIndex);
        }).show();
    }
}