package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/** New shows on Netflix, Prime and the other streamers (TVmaze), ranked by what the family likes. */
final class ForYou {
    static final class Item {
        String title, platform, pkg, image;
        List<String> genres = new ArrayList<>();
        double rating, score;
    }

    interface Cb { void on(List<Item> items); }

    static String pkgFor(String platform) {
        String p = platform.toLowerCase(Locale.ENGLISH);
        if (p.contains("netflix")) return "com.netflix.ninja";
        if (p.contains("prime") || p.contains("amazon")) return "com.amazon.amazonvideo.livingroom";
        if (p.contains("hotstar") || p.contains("disney")) return "in.startv.hotstar";
        if (p.contains("sony")) return "com.sonyliv";
        if (p.contains("zee")) return "com.graymatrix.did";
        return null;
    }

    static List<Item> cachedRanked(Context ctx) {
        String cache = G.sp(ctx).getString("fy_cache", null);
        if (cache == null) return new ArrayList<>();
        try { return rank(ctx, parse(cache)); } catch (Exception e) { return new ArrayList<>(); }
    }

    static void load(final Context ctx, final Cb cb) {
        final SharedPreferences sp = G.sp(ctx);
        String cache = sp.getString("fy_cache", null);
        if (cache != null) {
            try { cb.on(rank(ctx, parse(cache))); } catch (Exception e) { /* refetch below */ }
        }
        if (cache != null && System.currentTimeMillis() - sp.getLong("fy_time", 0) < 6L * 3600000L) return;
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                Map<String, Item> seen = new HashMap<>();
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                for (int i = 0; i < 10; i++) {
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.DAY_OF_YEAR, -i);
                    try {
                        JSONArray a = new JSONArray(get("https://api.tvmaze.com/schedule/web?date=" + f.format(c.getTime())));
                        for (int k = 0; k < a.length(); k++) {
                            JSONObject emb = a.getJSONObject(k).optJSONObject("_embedded");
                            JSONObject show = emb == null ? null : emb.optJSONObject("show");
                            if (show == null) continue;
                            JSONObject img = show.optJSONObject("image");
                            JSONObject wc = show.optJSONObject("webChannel");
                            if (img == null || wc == null) continue;
                            String plat = wc.optString("name", "");
                            if (pkgFor(plat) == null) continue;
                            String type = show.optString("type", "");
                            if (!(type.equals("Scripted") || type.equals("Animation") || type.equals("Documentary"))) continue;
                            String title = show.optString("name", "");
                            String image = img.optString("medium", "");
                            if (title.isEmpty() || image.isEmpty() || seen.containsKey(title)) continue;
                            Item it = new Item();
                            it.title = title;
                            it.platform = plat;
                            it.pkg = pkgFor(plat);
                            it.image = image;
                            JSONArray g = show.optJSONArray("genres");
                            if (g != null) for (int j = 0; j < g.length(); j++) it.genres.add(g.getString(j));
                            JSONObject r = show.optJSONObject("rating");
                            it.rating = (r == null || r.isNull("average")) ? 6.0 : r.optDouble("average", 6.0);
                            seen.put(title, it);
                        }
                    } catch (Exception day) {
                        // skip a day that fails
                    }
                }
                List<Item> all = new ArrayList<>(seen.values());
                if (!all.isEmpty()) {
                    sp.edit().putString("fy_cache", toJson(all)).putLong("fy_time", System.currentTimeMillis()).apply();
                    final List<Item> ranked = rank(ctx, all);
                    main.post(() -> cb.on(ranked));
                }
            } catch (Exception e) {
                // offline or the source changed: keep what is saved
            }
        }).start();
    }

    static List<Item> rank(Context ctx, List<Item> in) {
        SharedPreferences s = G.sp(ctx);
        Map<String, Double> aff = new HashMap<>();
        for (Map.Entry<String, ?> en : s.getAll().entrySet()) {
            String k = en.getKey();
            if (!k.startsWith("r_") || !(en.getValue() instanceof Integer)) continue;
            int r = (Integer) en.getValue();
            String title = k.substring(2);
            List<String> genres = new ArrayList<>();
            String gs = s.getString("g_" + title, null);
            if (gs != null) {
                for (String g : gs.split(",")) if (!g.isEmpty()) genres.add(g);
            } else {
                for (String[] t : Titles.ALL) if (t[0].equals(title)) genres.add(t[1].equals("Sci-Fi") ? "Science-Fiction" : t[1]);
            }
            for (String g : genres) {
                Double o = aff.get(g);
                aff.put(g, (o == null ? 0.0 : o) + r);
            }
        }
        for (Item it : in) {
            double sc = it.rating / 10.0;
            for (String g : it.genres) {
                Double a = aff.get(g);
                if (a != null) sc += 0.35 * a;
            }
            if ("com.netflix.ninja".equals(it.pkg) || "com.amazon.amazonvideo.livingroom".equals(it.pkg)) sc += 0.15;
            sc += new Random(Photos.slot() * 31L + it.title.hashCode()).nextDouble() * 0.3;
            it.score = sc;
        }
        List<Item> out = new ArrayList<>(in);
        Collections.sort(out, (a, b) -> Double.compare(b.score, a.score));
        return out;
    }

    private static List<Item> parse(String s) throws Exception {
        JSONArray a = new JSONArray(s);
        List<Item> l = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            Item it = new Item();
            it.title = o.getString("t");
            it.platform = o.getString("p");
            it.pkg = pkgFor(it.platform);
            it.image = o.getString("i");
            it.rating = o.optDouble("r", 6.0);
            JSONArray g = o.optJSONArray("g");
            if (g != null) for (int j = 0; j < g.length(); j++) it.genres.add(g.getString(j));
            l.add(it);
        }
        return l;
    }

    private static String toJson(List<Item> l) throws Exception {
        JSONArray a = new JSONArray();
        for (Item it : l) {
            JSONObject o = new JSONObject();
            o.put("t", it.title);
            o.put("p", it.platform);
            o.put("i", it.image);
            o.put("r", it.rating);
            o.put("g", new JSONArray(it.genres));
            a.put(o);
        }
        return a.toString();
    }

    static String get(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        InputStream in = c.getInputStream();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        c.disconnect();
        return new String(bo.toByteArray(), "UTF-8");
    }
}