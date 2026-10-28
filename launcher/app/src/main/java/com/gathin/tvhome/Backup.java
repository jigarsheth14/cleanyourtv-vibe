package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONObject;

/** Saves dock order, ratings and renewals to a file you can copy to the PC, and loads one back. */
final class Backup {
    static void save(Context c) {
        try {
            File d = c.getExternalFilesDir(null);
            if (d == null) return;
            JSONObject o = new JSONObject();
            for (Map.Entry<String, ?> e : G.sp(c).getAll().entrySet()) {
                String k = e.getKey();
                if (k.startsWith("fy_") || k.startsWith("wx_") || k.startsWith("cr_")) continue;
                Object v = e.getValue();
                JSONObject t = new JSONObject();
                if (v instanceof Integer) { t.put("t", "i"); t.put("v", ((Integer) v).intValue()); }
                else if (v instanceof Long) { t.put("t", "l"); t.put("v", ((Long) v).longValue()); }
                else if (v instanceof Boolean) { t.put("t", "b"); t.put("v", ((Boolean) v).booleanValue()); }
                else if (v instanceof Float) { t.put("t", "f"); t.put("v", ((Float) v).doubleValue()); }
                else if (v instanceof String) { t.put("t", "s"); t.put("v", v); }
                else continue;
                o.put(k, t);
            }
            Files.write(new File(d, "bento-backup.json").toPath(), o.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            // a failed backup must never break the home screen
        }
    }

    static boolean restore(Context c) {
        try {
            File d = c.getExternalFilesDir(null);
            if (d == null) return false;
            File f = new File(d, "bento-restore.json");
            if (!f.exists()) return false;
            JSONObject o = new JSONObject(new String(Files.readAllBytes(f.toPath()), "UTF-8"));
            SharedPreferences.Editor ed = G.sp(c).edit();
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                JSONObject t = o.getJSONObject(k);
                String type = t.getString("t");
                if (type.equals("i")) ed.putInt(k, t.getInt("v"));
                else if (type.equals("l")) ed.putLong(k, t.getLong("v"));
                else if (type.equals("b")) ed.putBoolean(k, t.getBoolean("v"));
                else if (type.equals("f")) ed.putFloat(k, (float) t.getDouble("v"));
                else if (type.equals("s")) ed.putString(k, t.getString("v"));
            }
            ed.apply();
            f.delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}