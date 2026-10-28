package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class G {
    static final String NAME = "Sheth Family";
    static float u(Context c) { return c.getResources().getDisplayMetrics().heightPixels / 540f; }
    static SharedPreferences sp(Context c) { return c.getSharedPreferences("bento", Context.MODE_PRIVATE); }
    static TextView tv(Context c, String s, float px, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }
    static boolean isDayNow() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h >= 6 && h < 18;
    }
    static String greet() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String part = h < 5 ? "night" : h < 12 ? "morning" : h < 17 ? "afternoon" : h < 21 ? "evening" : "night";
        return "Good " + part + ", " + NAME;
    }
    static String zoneTime(String id) {
        SimpleDateFormat f = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
        f.setTimeZone(TimeZone.getTimeZone(id));
        return f.format(new Date());
    }
    static String worldLine() {
        return "Mumbai " + zoneTime("Asia/Kolkata") + "      Perth " + zoneTime("Australia/Perth");
    }
    static android.graphics.drawable.GradientDrawable bg() {
        return new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF1E3A5F, 0xFF0E1A33, 0xFF3A2A66});
    }
}