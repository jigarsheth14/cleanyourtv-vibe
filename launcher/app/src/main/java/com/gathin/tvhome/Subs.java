package com.gathin.tvhome;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class Subs {
    static final class Item {
        String name;
        long date;
        int months;
        Item(String n, long d, int m) { name = n; date = d; months = m; }
    }

    static long today() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    static long addMonths(long t, int m) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        c.add(Calendar.MONTH, m);
        return c.getTimeInMillis();
    }

    static int daysLeft(Item i) {
        if (i.date <= 0) return 9999;
        return (int) Math.round((i.date - today()) / 86400000.0);
    }

    static List<Item> load(Context ctx) {
        String raw = G.sp(ctx).getString("subs2", null);
        List<Item> l = new ArrayList<>();
        if (raw == null) {
            for (String n : new String[]{"JioHotstar", "Netflix", "SonyLIV", "Prime Video"}) l.add(new Item(n, 0, 1));
            save(ctx, l);
            return l;
        }
        for (String part : raw.split(";")) {
            String[] f = part.split("\\|");
            if (f.length >= 2) {
                try {
                    l.add(new Item(f[0], Long.parseLong(f[1]), f.length > 2 ? Integer.parseInt(f[2]) : 1));
                } catch (Exception e) {
                    // skip a damaged entry
                }
            }
        }
        boolean changed = false;
        for (Item i : l) {
            if (i.date <= 0) continue;
            while (i.date < today()) { i.date = addMonths(i.date, Math.max(1, i.months)); changed = true; }
        }
        if (changed) save(ctx, l);
        return l;
    }

    static void save(Context ctx, List<Item> l) {
        StringBuilder sb = new StringBuilder();
        for (Item i : l) sb.append(i.name).append('|').append(i.date).append('|').append(i.months).append(';');
        G.sp(ctx).edit().putString("subs2", sb.toString()).apply();
    }

    static List<Item> setOnly(List<Item> l) {
        List<Item> out = new ArrayList<>();
        for (Item i : l) if (i.date > 0) out.add(i);
        Collections.sort(out, (a, b) -> daysLeft(a) - daysLeft(b));
        return out;
    }

    static String dateText(Item i) {
        return i.date <= 0 ? "Not set" : new SimpleDateFormat("d MMM", Locale.ENGLISH).format(new Date(i.date));
    }

    static String daysText(Item i) {
        if (i.date <= 0) return "--";
        int d = daysLeft(i);
        return d <= 0 ? "Today" : d == 1 ? "1 day" : d + " days";
    }
}