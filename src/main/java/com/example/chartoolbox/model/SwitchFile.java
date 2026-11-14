package com.example.chartoolbox.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The words in config/chartoolbox/parts.properties: one "id=true" or "id=false" line per part.
 * Read forgivingly (someone may edit it by hand), and lines for parts this version doesn't know are kept.
 */
public final class SwitchFile {
    private SwitchFile() {}

    static final String HEADER = "# Char's Toolbox: which parts are on";

    public static Map<String, Boolean> parse(String text) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (String raw : text.split("\\R")) {
            String line = raw.trim();
            int eq = line.indexOf('=');
            if (line.isEmpty() || line.startsWith("#") || eq <= 0) continue;
            String id = line.substring(0, eq).trim();
            if (!id.isEmpty()) out.put(id, !line.substring(eq + 1).trim().equalsIgnoreCase("false"));
        }
        return out;
    }

    public static String format(Map<String, Boolean> values) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        values.forEach((id, on) -> sb.append(id).append('=').append(on).append('\n'));
        return sb.toString();
    }

    /** A part with no line is on. */
    public static boolean on(Map<String, Boolean> values, String id) {
        return values.getOrDefault(id, Boolean.TRUE);
    }
}
