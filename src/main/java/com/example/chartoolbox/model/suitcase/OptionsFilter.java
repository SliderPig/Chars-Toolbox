package com.example.chartoolbox.model.suitcase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The game's options.txt, one "name:value" line per setting; key bindings are the lines named "key_...".
 * Four lines belong to one profile only and are never carried.
 */
public final class OptionsFilter {
    private OptionsFilter() {}

    private static final Set<String> STAYS = Set.of("version", "resourcePacks", "incompatibleResourcePacks", "lastServer");
    private static final String KEY = "key_";

    /** The setting's name, or null for a line that isn't "name:value". */
    private static String name(String line) {
        int colon = line.indexOf(':');
        return colon <= 0 ? null : line.substring(0, colon);
    }

    /** The lines worth putting in the suitcase. */
    public static List<String> carried(List<String> optionsLines) {
        List<String> out = new ArrayList<>();
        for (String line : optionsLines) {
            String name = name(line);
            if (name != null && !STAYS.contains(name)) out.add(line);
        }
        return out;
    }

    /**
     * The target profile's lines with the carried ones laid over them. A key line is taken only when keys are wanted
     * and the target knows that key (a key of a mod this profile doesn't have is left out, and stays in the suitcase);
     * any other line only when options are wanted. Lines the target has and the suitcase doesn't are kept.
     */
    public static List<String> merge(List<String> target, List<String> carried, boolean keys, boolean options, Predicate<String> keyKnown) {
        Map<String, String> take = new LinkedHashMap<>();
        for (String line : carried) {
            String name = name(line);
            if (name == null || STAYS.contains(name)) continue;
            boolean wanted = name.startsWith(KEY) ? keys && keyKnown.test(name.substring(KEY.length())) : options;
            if (wanted) take.put(name, line);
        }
        List<String> out = new ArrayList<>();
        for (String line : target) {
            String name = name(line);
            String fresh = name == null ? null : take.remove(name);
            out.add(fresh != null ? fresh : line);
        }
        out.addAll(take.values());
        return out;
    }
}
