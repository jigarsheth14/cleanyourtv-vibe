package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AllAppsActivity extends Activity {
    static final class Entry {
        String pkg, label;
        Drawable icon;
        Intent intent;
    }

    private final List<Entry> items = new ArrayList<>();
    private float u;

    private int p(float v) { return Math.round(v * u); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        u = G.u(this);
        load();
        FrameLayout root = new FrameLayout(this);
        root.setBackground(G.bg());
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(p(50), p(30), p(50), p(20));
        col.addView(G.tv(this, "All apps", p(34), Color.WHITE, true));
        TextView hint = G.tv(this, "OK opens an app.   Hold OK to put it on Home.", p(15), 0xCCFFFFFF, false);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2);
        hl.topMargin = p(6);
        hl.bottomMargin = p(16);
        col.addView(hint, hl);

        GridView gv = new GridView(this);
        gv.setNumColumns(6);
        gv.setHorizontalSpacing(p(10));
        gv.setVerticalSpacing(p(10));
        gv.setClipToPadding(false);
        GradientDrawable sel = new GradientDrawable();
        sel.setColor(0x44FFFFFF);
        sel.setCornerRadius(p(16));
        sel.setStroke(p(2), 0xCCFFFFFF);
        gv.setSelector(sel);
        gv.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return items.size(); }
            @Override public Object getItem(int i) { return items.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View cv, ViewGroup parent) {
                Entry e = items.get(i);
                LinearLayout c = new LinearLayout(AllAppsActivity.this);
                c.setOrientation(LinearLayout.VERTICAL);
                c.setGravity(Gravity.CENTER);
                c.setPadding(p(8), p(10), p(8), p(10));
                ImageView iv = new ImageView(AllAppsActivity.this);
                iv.setImageDrawable(e.icon);
                c.addView(iv, new LinearLayout.LayoutParams(p(64), p(64)));
                TextView t = G.tv(AllAppsActivity.this, e.label, p(14), Color.WHITE, false);
                t.setGravity(Gravity.CENTER);
                t.setSingleLine();
                t.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, -2);
                tl.topMargin = p(6);
                c.addView(t, tl);
                return c;
            }
        });
        gv.setOnItemClickListener((a, v, pos, id) -> {
            try { startActivity(items.get(pos).intent); }
            catch (Exception ex) { Toast.makeText(this, "Could not open this app", Toast.LENGTH_SHORT).show(); }
        });
        gv.setOnItemLongClickListener((a, v, pos, id) -> { choose(items.get(pos)); return true; });
        col.addView(gv, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    private void choose(final Entry e) {
        new AlertDialog.Builder(this).setTitle(e.label)
            .setItems(new String[]{"Add to the front of Home", "Add to the end of Home"}, (dlg, which) -> {
                List<String> l = Dock.load(this);
                if (e.pkg == null || e.pkg.equals("settings")) { Toast.makeText(this, "Settings cannot be added", Toast.LENGTH_SHORT).show(); return; }
                if (l.contains(e.pkg)) { Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show(); return; }
                if (l.size() >= 7) { Toast.makeText(this, "Home is full (7 apps). Hold OK on one there to remove it.", Toast.LENGTH_LONG).show(); return; }
                if (which == 0) l.add(0, e.pkg); else l.add(e.pkg);
                Dock.save(this, l);
                Toast.makeText(this, e.label + " added to Home", Toast.LENGTH_SHORT).show();
            }).show();
    }

    private void load() {
        PackageManager pm = getPackageManager();
        Map<String, Entry> m = new LinkedHashMap<>();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || m.containsKey(pkg)) continue;
                Entry e = new Entry();
                e.pkg = pkg;
                e.label = String.valueOf(ri.loadLabel(pm));
                e.icon = ri.loadIcon(pm);
                e.intent = new Intent(Intent.ACTION_MAIN).addCategory(cat)
                        .setClassName(pkg, ri.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                m.put(pkg, e);
            }
        }
        items.addAll(m.values());
        Collections.sort(items, (a, c) -> a.label.compareToIgnoreCase(c.label));
        Entry s = new Entry();
        s.pkg = "settings";
        s.label = "Settings";
        try { s.icon = pm.getApplicationIcon("com.android.tv.settings"); } catch (Exception ex) { s.icon = pm.getDefaultActivityIcon(); }
        s.intent = new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        items.add(0, s);
    }
}