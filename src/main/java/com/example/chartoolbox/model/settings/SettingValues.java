package com.example.chartoolbox.model.settings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The words in config/chartoolbox/settings.properties: one "key=value" line per setting that was changed. Read
 * forgivingly: a value that makes no sense counts as the default, numbers stay in their range, and lines this version
 * doesn't know are kept. Carried to a new profile with the Toolbox's switches (Bring my settings).
 */
public final class SettingValues {
    private final Map<String, String> raw = new LinkedHashMap<>();

    public static SettingValues parse(String text) {
        SettingValues v = new SettingValues();
        for (String line : text.split("\\R")) {
            int eq = line.indexOf('=');
            if (line.isBlank() || line.strip().startsWith("#") || eq <= 0) continue;
            v.raw.put(line.substring(0, eq).strip(), line.substring(eq + 1).strip());
        }
        return v;
    }

    public String format() {
        StringBuilder sb = new StringBuilder("# Char's Toolbox: settings of the parts written in the Toolbox\n");
        raw.forEach((k, val) -> sb.append(k).append('=').append(val).append('\n'));
        return sb.toString();
    }

    public void set(Setting s, String value) {
        raw.put(s.key(), value == null ? "" : value.replace('\n', ' ').replace('\r', ' '));
    }

    public boolean on(Setting s) {
        String v = raw.get(s.key());
        if ("true".equalsIgnoreCase(v)) return true;
        if ("false".equalsIgnoreCase(v)) return false;
        return Boolean.parseBoolean(s.defaultValue());
    }

    public int number(Setting s) {
        int n;
        try {
            n = Integer.parseInt(raw.getOrDefault(s.key(), s.defaultValue()).strip());
        } catch (NumberFormatException e) {
            n = Integer.parseInt(s.defaultValue());
        }
        return Math.max(s.min(), Math.min(s.max(), n));
    }

    public String choice(Setting s) {
        String v = raw.get(s.key());
        return v != null && s.choices().contains(v) ? v : s.defaultValue();
    }

    public String text(Setting s) {
        return raw.getOrDefault(s.key(), s.defaultValue());
    }

    /** Clicks a choice on to the next one, round to the first after the last. */
    public void next(Setting s) {
        int i = s.choices().indexOf(choice(s));
        set(s, s.choices().get((i + 1) % s.choices().size()));
    }

    /** Flips a switch. */
    public void flip(Setting s) {
        set(s, String.valueOf(!on(s)));
    }

    /** A number one step up or down, kept in range. */
    public void nudge(Setting s, int by) {
        set(s, String.valueOf(Math.max(s.min(), Math.min(s.max(), number(s) + by))));
    }
}
