package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;

/** Daily photos from Bing's picture archive, saved on the TV and rotated every 3 hours. */
final class Photos {
    private static long lastSync;
    private static String cachedPath = "";
    private static Bitmap cachedBmp;

    static int slot() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.DAY_OF_YEAR) * 8 + c.get(Calendar.HOUR_OF_DAY) / 3;
    }

    private static File dir(Context c) {
        File d = new File(c.getApplicationContext().getFilesDir(), "photos");
        d.mkdirs();
        return d;
    }

    private static File[] files(Context c) {
        File[] fs = dir(c).listFiles((dd, n) -> n.endsWith(".jpg"));
        if (fs == null) return new File[0];
        Arrays.sort(fs);
        return fs;
    }

    static Bitmap pick(Context ctx) {
        File[] fs = files(ctx);
        if (fs.length == 0) return null;
        File f = fs[slot() % fs.length];
        if (f.getPath().equals(cachedPath) && cachedBmp != null) return cachedBmp;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap b = BitmapFactory.decodeFile(f.getPath(), o);
            if (b != null) { cachedPath = f.getPath(); cachedBmp = b; }
            return b;
        } catch (Throwable e) {
            return null;
        }
    }

    static void sync(final Context ctx, final Runnable done) {
        if (System.currentTimeMillis() - lastSync < 6L * 3600000L && files(ctx).length > 0) return;
        lastSync = System.currentTimeMillis();
        final Handler main = new Handler(Looper.getMainLooper());
        final File dir = dir(ctx);
        new Thread(() -> {
            boolean added = false;
            try {
                JSONObject o = new JSONObject(new String(get("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=en-IN"), "UTF-8"));
                JSONArray a = o.getJSONArray("images");
                for (int i = 0; i < a.length(); i++) {
                    String base = a.getJSONObject(i).getString("urlbase");
                    File f = new File(dir, "b" + Integer.toHexString(base.hashCode()) + ".jpg");
                    if (f.exists()) continue;
                    byte[] data = get("https://www.bing.com" + base + "_1920x1080.jpg");
                    File t = new File(dir, f.getName() + ".part");
                    FileOutputStream os = new FileOutputStream(t);
                    os.write(data);
                    os.close();
                    if (t.renameTo(f)) added = true;
                }
                File[] all = dir.listFiles((dd, n) -> n.endsWith(".jpg"));
                if (all != null && all.length > 16) {
                    Arrays.sort(all, (x, y) -> Long.compare(x.lastModified(), y.lastModified()));
                    for (int i = 0; i < all.length - 16; i++) all[i].delete();
                }
            } catch (Exception e) {
                // offline: keep what is saved
            }
            if (added) main.post(done);
        }).start();
    }

    private static byte[] get(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(15000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        InputStream in = c.getInputStream();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        c.disconnect();
        return bo.toByteArray();
    }

    /** The photo fitted to the screen, then tinted and animated-looking for the weather. */
    static Bitmap compose(Bitmap photo, String scene, int w, int h) {
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        float s = Math.max(w / (float) photo.getWidth(), h / (float) photo.getHeight());
        float pw = photo.getWidth() * s, ph = photo.getHeight() * s;
        c.drawBitmap(photo, null, new RectF((w - pw) / 2f, (h - ph) / 2f, (w + pw) / 2f, (h + ph) / 2f), new Paint(Paint.FILTER_BITMAP_FLAG));
        int tint;
        switch (scene) {
            case "clear_night": tint = 0x66000A28; break;
            case "rain": tint = 0x55182638; break;
            case "storm": tint = 0x77101420; break;
            case "cloud": tint = 0x33404A58; break;
            case "fog": tint = 0x44C8CED6; break;
            case "snow": tint = 0x2AFFFFFF; break;
            default: tint = 0x00000000;
        }
        c.drawColor(tint);
        Paint g = new Paint();
        g.setShader(new LinearGradient(0, 0, 0, h, 0x14000000, 0x66000000, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, g);
        Random r = new Random(11);
        Paint l = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (scene.equals("rain") || scene.equals("storm")) {
            l.setStrokeWidth(Math.max(1f, h / 540f));
            for (int i = 0; i < 260; i++) {
                l.setColor(Color.argb(40 + r.nextInt(60), 255, 255, 255));
                float x = r.nextInt(w), y = r.nextInt(h);
                c.drawLine(x, y, x - h * 0.006f, y + h * 0.04f + r.nextInt(Math.max(1, h / 20)), l);
            }
        }
        if (scene.equals("snow")) {
            l.setColor(0xDDFFFFFF);
            for (int i = 0; i < 160; i++) c.drawCircle(r.nextInt(w), r.nextInt(h), 1 + r.nextInt(3), l);
        }
        return out;
    }
}