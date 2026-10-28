package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** The photo background used by the full-screen pages. */
final class Look {
    static FrameLayout page(Context c) {
        final String[] sc = {G.isDayNow() ? "clear_day" : "clear_night"};
        Wx.cached(c, (t, code, day, city, sr) -> sc[0] = Wx.scene(code, day));
        Bitmap ph = Photos.pick(c);
        Bitmap bg = ph != null ? Photos.compose(ph, sc[0], 1280, 720) : Bg.sharp(sc[0], 480, 270);
        FrameLayout root = new FrameLayout(c);
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setImageBitmap(bg);
        root.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        View scrim = new View(c);
        scrim.setBackgroundColor(0x40000000);
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        return root;
    }
}