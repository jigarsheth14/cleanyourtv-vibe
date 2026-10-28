package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
        final String[] sc = {G.isDayNow() ? "clear_day" : "clear_night"};
        Wx.cached(this, (t, code, day, city, sr) -> sc[0] = Wx.scene(code, day));
        Bitmap ph = Photos.pick(this);
        Bitmap bg = ph != null ? Photos.compose(ph, sc[0], 1280, 720) : Bg.sharp(sc[0], 480, 270);

        FrameLayout root = new FrameLayout(this);
        ImageView bgv = new ImageView(this);
        bgv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bgv.setImageBitmap(bg);
        root.addView(bgv, new FrameLayout.LayoutParams(-1, -1));
        View scrim = new View(this);
        scrim.setBackgroundColor(0x30000000);
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(p(50), p(28), p(50), p(10));
        col.setClipChildren(false);
        col.setClipToPadding(false);

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(G.tv(this, "All apps", p(36), Color.WHITE, true));
        TextView hint = G.tv(this, items.size() + " apps   \u00B7   OK opens   \u00B7   hold OK to put an app on Home", p(14), 0xCCFFFFFF, false);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2);
        hl.topMargin = p(6);
        titles.addView(hint, hl);
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(G.tv(this, new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date()), p(26), Color.WHITE, true));
        col.addView(head);

        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new GridLayoutManager(this, 6));
        rv.setClipToPadding(false);
        rv.setClipChildren(false);
        rv.setPadding(p(2), p(14), p(2), p(30));
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
                LinearLayout c = new LinearLayout(AllAppsActivity.this);
                c.setOrientation(LinearLayout.VERTICAL);
                c.setGravity(Gravity.CENTER);
                c.setPadding(p(6), p(8), p(6), p(8));
                final GradientDrawable d = new GradientDrawable();
                d.setCornerRadius(p(20));
                d.setColor(0x26FFFFFF);
                d.setStroke(p(1), 0x66FFFFFF);
                c.setBackground(d);
                c.setFocusable(true);
                c.setClickable(true);
                c.setOnFocusChangeListener((v, has) -> {
                    float s = has ? 1.08f : 1f;
                    v.animate().scaleX(s).scaleY(s).setDuration(150).start();
                    v.setTranslationZ(has ? 12 : 0);
                    d.setColor(has ? 0x4DFFFFFF : 0x26FFFFFF);
                    d.setStroke(has ? p(3) : p(1), has ? 0xFFFFFFFF : 0x66FFFFFF);
                });
                ImageView iv = new ImageView(AllAppsActivity.this);
                c.addView(iv, new LinearLayout.LayoutParams(p(60), p(60)));
                TextView t = G.tv(AllAppsActivity.this, "", p(14), Color.WHITE, false);
                t.setGravity(Gravity.CENTER);
                t.setSingleLine();
                t.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, -2);
                tl.topMargin = p(6);
                c.addView(t, tl);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, p(112));
                lp.setMargins(p(6), p(6), p(6), p(6));
                c.setLayoutParams(lp);
                return new RecyclerView.ViewHolder(c) { };
            }

            @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                final Entry e = items.get(pos);
                LinearLayout c = (LinearLayout) h.itemView;
                ((ImageView) c.getChildAt(0)).setImageDrawable(e.icon);
                ((TextView) c.getChildAt(1)).setText(e.label);
                c.setOnClickListener(v -> open(e));
                c.setOnLongClickListener(v -> { choose(e); return true; });
            }

            @Override public int getItemCount() { return items.size(); }
        });
        col.addView(rv, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        rv.post(() -> {
            View v = rv.getChildAt(0);
            if (v != null) v.requestFocus();
        });
    }

    private void open(Entry e) {
        try {
            startActivity(e.intent);
        } catch (Exception ex) {
            Toast.makeText(this, "Could not open this app", Toast.LENGTH_SHORT).show();
        }
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