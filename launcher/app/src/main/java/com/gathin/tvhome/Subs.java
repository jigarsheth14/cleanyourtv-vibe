package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class Subs {
    static final class Item {
        String name;
        long date;
        Item(String n, long d) { name = n; date = d; }
    }

    static long today() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    static long addDays(long t, int d) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.add(Calendar.DAY_OF_YEAR, d);
        return c.getTimeInMillis();
    }

    static long addMonth(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.add(Calendar.MONTH, 1);
        return c.getTimeInMillis();
    }

    static int daysLeft(Item i) { return (int) Math.round((i.date - today()) / 86400000.0); }

    static List<Item> load(Context ctx) {
        SharedPreferences s = G.sp(ctx);
        String raw = s.getString("subs", null);
        List<Item> l = new ArrayList<>();
        if (raw == null) {
            String[] n = {"JioHotstar", "Netflix", "SonyLIV", "Prime Video"};
            for (int i = 0; i < n.length; i++) l.add(new Item(n[i], addDays(today(), 7 * (i + 1))));
            save(ctx, l);
            return l;
        }
        for (String part : raw.split(";")) {
            String[] f = part.split("\\|");
            if (f.length == 2) {
                try { l.add(new Item(f[0], Long.parseLong(f[1]))); } catch (Exception e) { }
            }
        }
        boolean changed = false;
        for (Item i : l) {
            while (i.date < today()) { i.date = addMonth(i.date); changed = true; }
        }
        if (changed) save(ctx, l);
        return l;
    }

    static void save(Context ctx, List<Item> l) {
        StringBuilder sb = new StringBuilder();
        for (Item i : l) sb.append(i.name).append('|').append(i.date).append(';');
        G.sp(ctx).edit().putString("subs", sb.toString()).apply();
    }

    static String dateText(Item i) { return new SimpleDateFormat("d MMM", Locale.ENGLISH).format(new Date(i.date)); }

    static String daysText(Item i) {
        int d = daysLeft(i);
        return d <= 0 ? "Today" : d == 1 ? "1 day" : d + " days";
    }
}