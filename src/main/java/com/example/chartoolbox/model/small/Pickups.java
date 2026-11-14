package com.example.chartoolbox.model.small;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Pickup list: "+3 Iron Ore" rows near the hotbar, worked out from the inventory's counts going up. The same thing
 * picked up again while its row still shows adds to that row ("+3" becomes "+7") and starts its time again.
 */
public final class Pickups {
    public static final int MOST = 5;
    public static final long SHOW = 3000, FADE = 500;

    public record Row(String key, String words, float alpha) {}

    private static final class Entry {
        final String key;
        String name;
        int count;
        long at;

        Entry(String key, String name, int count, long at) {
            this.key = key;
            this.name = name;
            this.count = count;
            this.at = at;
        }
    }

    private final List<Entry> entries = new ArrayList<>(); // newest first

    /** How many more of each thing there are now than before. Things that went down or away don't count. */
    public static Map<String, Integer> gains(Map<String, Integer> before, Map<String, Integer> after) {
        Map<String, Integer> out = new HashMap<>();
        after.forEach((key, n) -> {
            int more = n - before.getOrDefault(key, 0);
            if (more > 0) out.put(key, more);
        });
        return out;
    }

    public void add(String key, String name, int count, long now) {
        if (count <= 0) return;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (!e.key.equals(key) || now - e.at >= SHOW) continue;
            e.count += count;
            e.name = name;
            e.at = now;
            entries.remove(i);
            entries.add(0, e);
            return;
        }
        entries.add(0, new Entry(key, name, count, now));
        while (entries.size() > MOST * 4) entries.remove(entries.size() - 1);
    }

    /** The rows to draw now, newest first, at most five; each fades out in its last half second. */
    public List<Row> visible(long now) {
        List<Row> out = new ArrayList<>();
        for (Entry e : entries) {
            long age = now - e.at;
            if (age >= SHOW || age < 0) continue;
            float alpha = age > SHOW - FADE ? (SHOW - age) / (float) FADE : 1f;
            out.add(new Row(e.key, "+" + e.count + " " + e.name, alpha));
            if (out.size() == MOST) break;
        }
        return out;
    }

    public void clear() {
        entries.clear();
    }
}
