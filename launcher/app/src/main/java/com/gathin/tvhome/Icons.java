package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** The real app icons, taken from the apps installed on the TV. */
final class Icons {
    private static final Map<String, Bitmap> CACHE = new HashMap<>();
    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private static String[] candidates(String name) {
        String n = name.toLowerCase(Locale.ENGLISH);
        if (n.contains("hotstar")) return new String[]{"in.startv.hotstar", "in.startv.hotstar.dplus.tv"};
        if (n.contains("netflix")) return new String[]{"com.netflix.ninja"};
        if (n.contains("sony")) return new String[]{"com.sonyliv"};
        if (n.contains("prime")) return new String[]{"com.amazon.amazonvideo.livingroom", "com.amazon.avod.thirdpartyclient"};
        if (n.contains("zee")) return new String[]{"com.graymatrix.did"};
        if (n.contains("youtube")) return new String[]{"com.google.android.youtube.tv"};
        return new String[0];
    }

    private static Bitmap get(Context ctx, String name, int px) {
        String key = name + "@" + px;
        Bitmap hit = CACHE.get(key);
        if (hit != null) return hit;
        PackageManager pm = ctx.getPackageManager();
        for (String pkg : candidates(name)) {
            try {
                Drawable d = pm.getApplicationIcon(pkg);
                Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
                Canvas cv = new Canvas(b);
                d.setBounds(0, 0, px, px);
                d.draw(cv);
                CACHE.put(key, b);
                return b;
            } catch (Exception e) {
                // not installed, try the next package name
            }
        }
        return null;
    }

    static void draw(Context ctx, Canvas c, String name, float x, float y, float size) {
        RectF rf = new RectF(x, y, x + size, y + size);
        float r = size * 0.22f;
        Bitmap bm = get(ctx, name, Math.max(1, Math.round(size)));
        if (bm != null) {
            Path p = new Path();
            p.addRoundRect(rf, r, r, Path.Direction.CW);
            c.save();
            c.clipPath(p);
            c.drawBitmap(bm, null, rf, PAINT);
            c.restore();
            return;
        }
        float hue = Math.abs(name.hashCode()) % 360;
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        f.setColor(Color.HSVToColor(new float[]{hue, 0.6f, 0.8f}));
        c.drawRoundRect(rf, r, r, f);
        f.setColor(Color.WHITE);
        f.setTextAlign(Paint.Align.CENTER);
        f.setFakeBoldText(true);
        f.setTextSize(size * 0.55f);
        c.drawText(name.substring(0, 1).toUpperCase(Locale.ENGLISH), rf.centerX(), rf.centerY() - (f.descent() + f.ascent()) / 2f, f);
    }
}