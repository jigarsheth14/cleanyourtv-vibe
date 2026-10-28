package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real posters: TVmaze for series, Apple's iTunes search for films. Cached on the TV. */
final class Posters {
    interface Cb { void on(Bitmap b); }

    private static final Map<String, Bitmap> MEM = new HashMap<>();
    private static final Set<String> MOVIES = new HashSet<>(Arrays.asList("RRR", "Pathaan", "Extraction", "Spider-Verse"));
    private static final Map<String, String> ALT = new HashMap<>();
    static { ALT.put("Spider-Verse", "Spider-Man Into the Spider-Verse"); }

    static void load(final Context ctx, final String title, final Cb cb) {
        Bitmap m = MEM.get(title);
        if (m != null) { cb.on(m); return; }
        final Handler main = new Handler(Looper.getMainLooper());
        final File dir = new File(ctx.getCacheDir(), "posters");
        dir.mkdirs();
        final File f = new File(dir, Integer.toHexString(title.hashCode()) + ".jpg");
        new Thread(() -> {
            Bitmap b = null;
            try {
                if (f.exists() && f.length() > 0) b = BitmapFactory.decodeFile(f.getPath());
                if (b == null) {
                    byte[] data = find(title);
                    if (data != null) {
                        FileOutputStream o = new FileOutputStream(f);
                        o.write(data);
                        o.close();
                        b = BitmapFactory.decodeFile(f.getPath());
                    }
                }
            } catch (Exception e) {
                b = null;
            }
            final Bitmap r = b;
            if (r != null) {
                MEM.put(title, r);
                main.post(() -> cb.on(r));
            }
        }).start();
    }

    private static byte[] find(String title) {
        String q = ALT.containsKey(title) ? ALT.get(title) : title;
        byte[] d = null;
        if (MOVIES.contains(title)) {
            d = itunes(q);
            if (d == null) d = tvmaze(q);
        } else {
            d = tvmaze(q);
            if (d == null) d = itunes(q);
        }
        return d;
    }

    private static byte[] tvmaze(String q) {
        try {
            JSONObject o = new JSONObject(text("https://api.tvmaze.com/singlesearch/shows?q=" + URLEncoder.encode(q, "UTF-8")));
            String u = o.getJSONObject("image").getString("medium");
            return bytes(u.replace("http://", "https://"));
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] itunes(String q) {
        try {
            JSONObject o = new JSONObject(text("https://itunes.apple.com/search?media=movie&limit=1&term=" + URLEncoder.encode(q, "UTF-8")));
            JSONArray a = o.getJSONArray("results");
            if (a.length() == 0) return null;
            String u = a.getJSONObject(0).getString("artworkUrl100");
            return bytes(u.replace("100x100bb", "300x450bb"));
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(String u) throws Exception { return new String(bytes(u), "UTF-8"); }

    private static byte[] bytes(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        InputStream in = c.getInputStream();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        c.disconnect();
        return bo.toByteArray();
    }
}