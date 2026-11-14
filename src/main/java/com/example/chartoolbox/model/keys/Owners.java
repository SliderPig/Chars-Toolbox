package com.example.chartoolbox.model.keys;

import java.util.Set;

/** Which mod an action belongs to, worked out from its name (key.charmap.add) or its group in Controls (key.categories.charmap). */
public final class Owners {
    private Owners() {}

    /** The mod id, or null when neither the name nor the group names a loaded mod (or names the game itself). */
    public static String guess(String name, String category, Set<String> modIds) {
        String found = firstModWord(name, modIds);
        if (found == null) found = firstModWord(category, modIds);
        return found;
    }

    private static String firstModWord(String text, Set<String> modIds) {
        if (text == null) return null;
        for (String word : text.split("[.:/ ]")) {
            if (word.isEmpty() || word.equals("minecraft") || word.equals("forge") || word.equals("key") || word.equals("categories")) continue;
            if (modIds.contains(word)) return word;
        }
        return null;
    }
}
