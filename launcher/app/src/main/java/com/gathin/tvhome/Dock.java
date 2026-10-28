package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

final class Dock {
    static final String DEF = "com.google.android.youtube.tv,com.netflix.ninja,in.startv.hotstar,com.amazon.amazonvideo.livingroom,com.sonyliv,com.graymatrix.did";
    static String raw(Context c) { return G.sp(c).getString("dock", DEF); }
    static List<String> load(Context c) {
        List<String> l = new ArrayList<>();
        for (String s : raw(c).split(",")) if (!s.isEmpty()) l.add(s);
        return l;
    }
    static void save(Context c, List<String> l) { G.sp(c).edit().putString("dock", TextUtils.join(",", l)).apply(); }
    static String label(Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }
}