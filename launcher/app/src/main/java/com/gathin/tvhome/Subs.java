package com.gathin.tvhome;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The two things you edit by hand: the name in the greeting and your subscriptions.
 * After a change, run build-and-install.ps1 again.
 */
final class Subs {
    /** Name shown in "Good evening, ..." on Home and on the screensaver. */
    static final String NAME = "Dipika";

    // EDIT THESE (placeholders): name, colour, a renewal date (year, month, day), yearly or not.
    // Only one date per service is needed: the next renewal is worked out from it automatically.
    private static final Sub[] SUBS = {
            new Sub("Netflix",     0xFFE50914, 2026, 11, 20, false),
            new Sub("JioHotstar",  0xFF6C3DF4, 2026, 11, 28, false),
            new Sub("Prime Video", 0xFF1A98FF, 2027, 6, 1, true),
            new Sub("SonyLIV",     0xFFFFB400, 2027, 3, 4, true),
    };

    static final class Sub {
        final String name;
        final int color, year, month, day;
        final boolean yearly;

        Sub(String name, int color, int year, int month, int day, boolean yearly) {
            this.name = name;
            this.color = color;
            this.year = year;
            this.month = month;
            this.day = day;
            this.yearly = yearly;
        }

        /** First renewal on or after today, counted forward from the date given. */
        Calendar next(Calendar today) {
            Calendar base = Calendar.getInstance();
            base.clear();
            base.set(year, month - 1, day, 0, 0, 0);
            for (int n = 0; n < 2400; n++) {
                Calendar c = (Calendar) base.clone();
                c.add(yearly ? Calendar.YEAR : Calendar.MONTH, n);
                if (!c.before(today)) return c;
            }
            return base;
        }
    }

    static final class Item {
        final String name, sub, label;
        final int color, days;

        Item(String name, String sub, String label, int color, int days) {
            this.name = name;
            this.sub = sub;
            this.label = label;
            this.color = color;
            this.days = days;
        }
    }

    /** Subscriptions sorted by what renews soonest. */
    static List<Item> upcoming() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        SimpleDateFormat fmt = new SimpleDateFormat("d MMM", Locale.ENGLISH);
        List<Item> out = new ArrayList<>();
        for (Sub s : SUBS) {
            Calendar next = s.next(today);
            int days = (int) Math.round((next.getTimeInMillis() - today.getTimeInMillis()) / 86400000.0);
            String when = fmt.format(next.getTime());
            String label = days == 0 ? "Today" : days == 1 ? "Tomorrow" : days <= 60 ? days + " days" : when;
            out.add(new Item(s.name, "Auto-renews " + when, label, s.color, days));
        }
        Collections.sort(out, new Comparator<Item>() {
            @Override public int compare(Item a, Item b) { return a.days - b.days; }
        });
        return out;
    }
}
