package com.gathin.tvhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

/** India (men) and IPL matches from ESPN's public cricket feed. */
final class Cricket {
    static final class Match {
        String teams = "", line = "", status = "", series = "";
        boolean live, upcoming, india, ipl;
        long time;
    }

    interface Cb { void on(List<Match> list); }

    static Match top(List<Match> l) { return l.isEmpty() ? null : l.get(0); }

    static long updatedAt(Context ctx) {
        File f = new File(ctx.getCacheDir(), "cricket.json");
        return f.exists() ? f.lastModified() : 0;
    }

    static void load(final Context ctx, final Cb cb) {
        final File cf = new File(ctx.getCacheDir(), "cricket.json");
        if (cf.exists()) {
            try { cb.on(parse(new String(Files.readAllBytes(cf.toPath()), "UTF-8"))); } catch (Exception e) { /* refetch */ }
            if (System.currentTimeMillis() - cf.lastModified() < 90000L) return;
        }
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                String raw = ForYou.get("https://site.api.espn.com/apis/personalized/v2/scoreboard/header?sport=cricket&region=in&tz=Asia/Calcutta");
                Files.write(cf.toPath(), raw.getBytes("UTF-8"));
                try {
                    File ext = ctx.getExternalFilesDir(null);
                    if (ext != null) Files.write(new File(ext, "cricket-raw.json").toPath(), raw.getBytes("UTF-8"));
                } catch (Exception e) { /* debug copy only */ }
                final List<Match> l = parse(raw);
                main.post(() -> cb.on(l));
            } catch (Exception e) {
                // offline: keep what is saved
            }
        }).start();
    }

    private static List<Match> parse(String raw) throws Exception {
        List<Match> all = new ArrayList<>();
        walk(new JSONObject(raw), "", all);
        List<Match> keep = new ArrayList<>();
        boolean fb = false; for (Match m : all) if (m.india || m.ipl) keep.add(m); if (keep.isEmpty()) { fb = true; for (Match m : all) if (m.live || m.upcoming) keep.add(m); }
        Collections.sort(keep, (a, b) -> {
            int ra = a.live ? 0 : a.upcoming ? 1 : 2;
            int rb = b.live ? 0 : b.upcoming ? 1 : 2;
            if (ra != rb) return ra - rb;
            return ra == 1 ? Long.compare(a.time, b.time) : Long.compare(b.time, a.time);
        });
        if (fb && keep.size() > 3) return new ArrayList<>(keep.subList(0, 3)); return keep;
    }

    private static void walk(Object o, String series, List<Match> out) {
        if (o instanceof JSONArray) {
            JSONArray a = (JSONArray) o;
            for (int i = 0; i < a.length(); i++) walk(a.opt(i), series, out);
            return;
        }
        if (!(o instanceof JSONObject)) return;
        JSONObject j = (JSONObject) o;
        JSONArray ev = j.optJSONArray("events");
        if (ev != null) {
            String s = j.optString("name", series);
            for (int i = 0; i < ev.length(); i++) {
                JSONObject e = ev.optJSONObject(i);
                if (e != null) out.add(event(e, s));
            }
        }
        JSONArray sp = j.optJSONArray("sports");
        if (sp != null) walk(sp, series, out);
        JSONArray lg = j.optJSONArray("leagues");
        if (lg != null) walk(lg, series, out);
    }

    private static Match event(JSONObject e, String series) {
        Match m = new Match();
        m.series = series;
        String name = e.optString("name", e.optString("shortName", ""));
        String state = e.optString("status", "");
        JSONObject fs = e.optJSONObject("fullStatus");
        JSONObject ft = fs == null ? null : fs.optJSONObject("type");
        if ((state.isEmpty() || state.length() > 4) && ft != null) state = ft.optString("state", "");
        String detail = e.optString("summary", "");
        if (detail.isEmpty() && ft != null) detail = ft.optString("detail", ft.optString("description", ""));
        m.live = state.equals("in");
        m.upcoming = state.equals("pre");
        StringBuilder teams = new StringBuilder(), line = new StringBuilder();
        boolean indiaTeam = false;
        JSONArray comps = e.optJSONArray("competitors");
        if (comps != null) {
            for (int i = 0; i < comps.length(); i++) {
                JSONObject c = comps.optJSONObject(i);
                if (c == null) continue;
                String dn = c.optString("displayName", c.optString("name", ""));
                String ab = c.optString("abbreviation", dn);
                String sc = c.optString("score", "");
                if (dn.contains("India") && !dn.contains("Women") && !dn.contains("U19") && !dn.contains("Under")) indiaTeam = true;
                if (teams.length() > 0) teams.append("  v  ");
                teams.append(ab);
                if (!sc.isEmpty() && !sc.equals("null")) {
                    if (line.length() > 0) line.append("      ");
                    line.append(ab).append(' ').append(sc);
                }
            }
        }
        if (teams.length() == 0) teams.append(name);
        String ls = (series + " " + name).toLowerCase(Locale.ENGLISH);
        boolean women = ls.contains("women");
        m.india = indiaTeam && !women && !ls.contains("under-19") && !ls.contains("u19");
        m.ipl = !women && (ls.contains("indian premier league") || ls.contains("ipl"));
        m.time = parseDate(e.optString("date", ""));
        m.teams = teams.toString();
        m.line = line.toString();
        m.status = m.upcoming ? "Starts " + when(m.time) : detail;
        return m;
    }

    private static long parseDate(String s) {
        if (s == null || s.isEmpty()) return 0;
        String[] fmts = {"yyyy-MM-dd'T'HH:mm'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"};
        for (String f : fmts) {
            try {
                SimpleDateFormat d = new SimpleDateFormat(f, Locale.US);
                d.setTimeZone(TimeZone.getTimeZone("UTC"));
                return d.parse(s).getTime();
            } catch (Exception e) {
                // try the next format
            }
        }
        return 0;
    }

    static String when(long t) {
        if (t <= 0) return "soon";
        SimpleDateFormat perth = new SimpleDateFormat("EEE d MMM, h:mm a", Locale.ENGLISH);
        perth.setTimeZone(TimeZone.getTimeZone("Australia/Perth"));
        SimpleDateFormat ist = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
        ist.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        Date d = new Date(t);
        return perth.format(d) + " Perth  (" + ist.format(d) + " IST)";
    }
}