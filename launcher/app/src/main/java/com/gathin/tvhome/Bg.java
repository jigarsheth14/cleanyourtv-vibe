package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import java.util.Random;

final class Bg {
    static int[] palette(String s) {
        switch (s) {
            case "clear_day": return new int[]{0xFF2E8BE6, 0xFF74B9FF, 0xFFBFE3FF};
            case "cloud": return new int[]{0xFF4B5563, 0xFF6B7280, 0xFF9CA3AF};
            case "rain": return new int[]{0xFF1F2A3A, 0xFF2F4358, 0xFF4A6075};
            case "snow": return new int[]{0xFF8FA3B8, 0xFFC9D6E3, 0xFFEAF1F8};
            case "storm": return new int[]{0xFF111827, 0xFF1F2937, 0xFF3B2F5A};
            case "fog": return new int[]{0xFF6B7280, 0xFF9CA3AF, 0xFFD1D5DB};
            default: return new int[]{0xFF0B1230, 0xFF1B2A6B, 0xFF3A2A66};
        }
    }

    static Bitmap sharp(String scene, int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, w, h, palette(scene), null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        int[] glow = {0xFF3D8BFF, 0xFFFF6FB5, 0xFF45E0C1};
        for (int i = 0; i < 3; i++) {
            float cx = w * (0.2f + 0.3f * i);
            float cy = h * (0.3f + 0.25f * (i % 2));
            p.setShader(new RadialGradient(cx, cy, 150, (glow[i] & 0x00FFFFFF) | 0x90000000, 0x00000000, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, 150, p);
        }
        p.setShader(null);
        Random r = new Random(7);
        for (int i = 0; i < 70; i++) {
            p.setColor(Color.argb(60 + r.nextInt(120), 200 + r.nextInt(55), 200 + r.nextInt(55), 220 + r.nextInt(35)));
            c.drawCircle(r.nextInt(w), r.nextInt(h), 3 + r.nextInt(14), p);
        }
        boolean wet = scene.equals("rain") || scene.equals("storm");
        p.setStrokeWidth(1f);
        for (int i = 0; i < (wet ? 140 : 0); i++) {
            p.setColor(Color.argb(30 + r.nextInt(40), 255, 255, 255));
            float x = r.nextInt(w), y = r.nextInt(h);
            c.drawLine(x, y, x - 3, y + 20 + r.nextInt(30), p);
        }
        if (scene.equals("snow")) {
            p.setColor(0xDDFFFFFF);
            for (int i = 0; i < 120; i++) c.drawCircle(r.nextInt(w), r.nextInt(h), 1 + r.nextInt(3), p);
        }
        if (scene.equals("clear_night")) {
            p.setColor(0xFFFFFFFF);
            for (int i = 0; i < 80; i++) c.drawCircle(r.nextInt(w), r.nextInt(h / 2), 1, p);
        }
        return bm;
    }

    static Bitmap blur(Bitmap s) {
        int w = s.getWidth(), h = s.getHeight();
        Bitmap a = Bitmap.createScaledBitmap(s, 120, 68, true);
        Bitmap b = Bitmap.createScaledBitmap(a, 30, 17, true);
        Bitmap c = Bitmap.createScaledBitmap(b, 120, 68, true);
        Bitmap d = Bitmap.createScaledBitmap(c, w, h, true);
        a.recycle();
        b.recycle();
        c.recycle();
        return d;
    }
}