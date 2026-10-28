package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Endless rating deck: your own list, new releases, then well-rated shows pulled from TVmaze. */
public class SwipeActivity extends Activity {
    private final List<ForYou.Item> queue = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();
    private int pos;
    private boolean busy, loading;
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
        FrameLayout root = Look.page(this);
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
        genre = G.tv(this, "", p(15), 0xDDFFFFFF, false);
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

        List<ForYou.Item> seed = new ArrayList<>();
        for (String[] t : Titles.ALL) {
            ForYou.Item it = new ForYou.Item();
            it.title = t[0];
            it.genres.add(t[1].equals("Sci-Fi") ? "Science-Fiction" : t[1]);
            seed.add(it);
        }
        seed.addAll(ForYou.cachedRanked(this));
        Collections.shuffle(seed);
        for (ForYou.Item it : seed) add(it);
        show();
        if (queue.size() < 8) loadMore();
    }

    private void add(ForYou.Item it) {
        if (G.sp(this).getInt("r_" + it.title, 0) != 0) return;
        if (seen.add(it.title)) queue.add(it);
    }

    private int rated() {
        int n = 0;
        for (String k : G.sp(this).getAll().keySet()) if (k.startsWith("r_")) n++;
        return n;
    }

    private void setColor(String s) {
        float hue = Math.abs(s.hashCode()) % 360;
        card.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
                Color.HSVToColor(new float[]{hue, 0.55f, 0.75f}), Color.HSVToColor(new float[]{hue, 0.65f, 0.35f})}));
    }

    private String describe(ForYou.Item it) {
        StringBuilder sb = new StringBuilder();
        for (String g : it.genres) {
            if (sb.length() > 0) sb.append("  \u00B7  ");
            sb.append(g);
        }
        if (it.rating > 0) sb.append(sb.length() > 0 ? "  \u00B7  " : "").append(String.format(java.util.Locale.ENGLISH, "%.1f", it.rating));
        if (it.platform != null && !it.platform.isEmpty()) sb.append(sb.length() > 0 ? "  \u00B7  " : "").append(it.platform);
        return sb.toString();
    }

    private void show() {
        count.setText(rated() + " rated   -   the deck never runs out");
        stamp.setVisibility(View.GONE);
        poster.setVisibility(View.GONE);
        if (pos >= queue.size()) {
            shown = "";
            title.setText("Finding more...");
            genre.setText("If this stays, check the Wi-Fi and press OK");
            setColor("more");
            loadMore();
            return;
        }
        final ForYou.Item it = queue.get(pos);
        shown = it.title;
        title.setText(it.title);
        genre.setText(describe(it));
        setColor(it.title);
        Posters.Cb cb = bm -> {
            if (shown.equals(it.title)) {
                poster.setImageBitmap(bm);
                poster.setVisibility(View.VISIBLE);
            }
        };
        if (it.image != null && !it.image.isEmpty()) Posters.loadUrl(this, it.image, cb); else Posters.load(this, it.title, cb);
        if (queue.size() - pos < 6) loadMore();
    }

    private void loadMore() {
        if (loading) return;
        loading = true;
        new Thread(() -> {
            final List<ForYou.Item> add = new ArrayList<>();
            Random r = new Random();
            for (int tries = 0; tries < 5 && add.size() < 10; tries++) {
                try {
                    JSONArray a = new JSONArray(ForYou.get("https://api.tvmaze.com/shows?page=" + r.nextInt(240)));
                    for (int k = 0; k < a.length(); k++) {
                        JSONObject s = a.getJSONObject(k);
                        JSONObject img = s.optJSONObject("image");
                        JSONObject rt = s.optJSONObject("rating");
                        if (img == null || rt == null || rt.isNull("average")) continue;
                        double avg = rt.optDouble("average", 0);
                        if (avg < 7.3) continue;
                        String prem = s.optString("premiered", "");
                        if (prem.length() < 4 || prem.equals("null") || prem.compareTo("2005") < 0) continue;
                        String lang = s.optString("language", "");
                        if (!(lang.equals("English") || lang.equals("Hindi") || lang.equals("Tamil") || lang.equals("Telugu")
                                || lang.equals("Malayalam") || lang.equals("Korean"))) continue;
                        String image = img.optString("medium", "");
                        String name = s.optString("name", "");
                        if (image.isEmpty() || name.isEmpty()) continue;
                        ForYou.Item it = new ForYou.Item();
                        it.title = name;
                        it.image = image;
                        it.rating = avg;
                        JSONObject wc = s.optJSONObject("webChannel");
                        JSONObject nw = s.optJSONObject("network");
                        if (wc != null) it.platform = wc.optString("name", "");
                        else if (nw != null) it.platform = nw.optString("name", "");
                        JSONArray g = s.optJSONArray("genres");
                        if (g != null) for (int j = 0; j < g.length(); j++) it.genres.add(g.getString(j));
                        add.add(it);
                    }
                } catch (Exception e) {
                    // that page failed; try another
                }
            }
            Collections.shuffle(add);
            runOnUiThread(() -> {
                loading = false;
                for (ForYou.Item it : add) add(it);
                if (shown.isEmpty() && pos < queue.size()) show();
            });
        }).start();
    }

    private void rate(final int v) {
        if (pos >= queue.size()) return;
        ForYou.Item it = queue.get(pos);
        SharedPreferences.Editor e = G.sp(this).edit();
        e.putInt("r_" + it.title, v).putString("g_" + it.title, TextUtils.join(",", it.genres)).apply();
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
            if (pos < queue.size()) { pos++; show(); } else loadMore();
            return true;
        }
        return super.onKeyDown(k, e);
    }
}