package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ForYouActivity extends Activity {
    private final List<ForYou.Item> items = new ArrayList<>();
    private float u;
    private RecyclerView rv;
    private TextView empty;
    private boolean focused;

    private int p(float v) { return Math.round(v * u); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        u = G.u(this);
        FrameLayout root = Look.page(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(p(46), p(26), p(46), p(8));
        col.addView(G.tv(this, "For you", p(34), Color.WHITE, true));
        TextView hint = G.tv(this, "New on Netflix, Prime, JioHotstar, SonyLIV and Zee5   -   OK opens the app   -   the picks reshuffle every few hours and learn from Swipe", p(14), 0xCCFFFFFF, false);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2);
        hl.topMargin = p(6);
        col.addView(hint, hl);
        empty = G.tv(this, "Loading picks...", p(20), Color.WHITE, false);
        LinearLayout.LayoutParams el = new LinearLayout.LayoutParams(-2, -2);
        el.topMargin = p(30);
        col.addView(empty, el);

        rv = new RecyclerView(this);
        rv.setLayoutManager(new GridLayoutManager(this, 6));
        rv.setClipToPadding(false);
        rv.setPadding(p(2), p(12), p(2), p(24));
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
                final FrameLayout f = new FrameLayout(ForYouActivity.this);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, p(196));
                lp.setMargins(p(6), p(6), p(6), p(6));
                f.setLayoutParams(lp);
                GradientDrawable g = new GradientDrawable();
                g.setColor(0x33FFFFFF);
                g.setCornerRadius(p(14));
                f.setBackground(g);
                f.setClipToOutline(true);
                f.setOutlineProvider(new ViewOutlineProvider() {
                    @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), p(14)); }
                });
                ImageView iv = new ImageView(ForYouActivity.this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                f.addView(iv, new FrameLayout.LayoutParams(-1, -1));
                TextView t = G.tv(ForYouActivity.this, "", p(13), Color.WHITE, true);
                t.setGravity(Gravity.BOTTOM);
                t.setPadding(p(8), p(30), p(8), p(8));
                t.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0xCC000000}));
                f.addView(t, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
                TextView badge = G.tv(ForYouActivity.this, "", p(9), Color.WHITE, true);
                badge.setPadding(p(5), p(2), p(5), p(2));
                GradientDrawable bg2 = new GradientDrawable();
                bg2.setColor(0xAA000000);
                bg2.setCornerRadius(p(6));
                badge.setBackground(bg2);
                FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
                bl.setMargins(p(6), p(6), 0, 0);
                f.addView(badge, bl);
                final GradientDrawable rim = new GradientDrawable();
                rim.setColor(0x00000000);
                rim.setCornerRadius(p(14));
                rim.setStroke(p(3), 0xFFFFFFFF);
                f.setFocusable(true);
                f.setClickable(true);
                f.setOnFocusChangeListener((v, has) -> {
                    float s = has ? 1.06f : 1f;
                    v.animate().scaleX(s).scaleY(s).setDuration(150).start();
                    v.setTranslationZ(has ? 12 : 0);
                    f.setForeground(has ? rim : null);
                });
                return new RecyclerView.ViewHolder(f) { };
            }

            @Override public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                final ForYou.Item it = items.get(pos);
                FrameLayout f = (FrameLayout) h.itemView;
                final ImageView iv = (ImageView) f.getChildAt(0);
                ((TextView) f.getChildAt(1)).setText(it.title);
                TextView badge = (TextView) f.getChildAt(2);
                badge.setText(it.platform == null ? "" : it.platform.toUpperCase(Locale.ENGLISH));
                badge.setVisibility(it.platform == null ? View.GONE : View.VISIBLE);
                iv.setImageDrawable(null);
                final String url = it.image;
                iv.setTag(url);
                if (url != null && !url.isEmpty()) {
                    Posters.loadUrl(ForYouActivity.this, url, bm -> {
                        if (url.equals(iv.getTag())) iv.setImageBitmap(bm);
                    });
                }
                f.setOnClickListener(v -> open(it));
            }

            @Override public int getItemCount() { return items.size(); }
        });
        col.addView(rv, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(col, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        ForYou.load(this, list -> {
            items.clear();
            items.addAll(list);
            rv.getAdapter().notifyDataSetChanged();
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            if (!focused && !items.isEmpty()) {
                focused = true;
                rv.post(() -> {
                    View v = rv.getChildAt(0);
                    if (v != null) v.requestFocus();
                });
            }
        });
    }

    private void open(ForYou.Item it) {
        if (it.pkg == null) { Toast.makeText(this, "Open this one from its app", Toast.LENGTH_SHORT).show(); return; }
        PackageManager pm = getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(it.pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(it.pkg);
        if (i == null) { Toast.makeText(this, it.platform + " is not on this TV", Toast.LENGTH_LONG).show(); return; }
        startActivity(i);
    }
}