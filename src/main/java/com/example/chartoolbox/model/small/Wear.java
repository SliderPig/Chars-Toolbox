package com.example.chartoolbox.model.small;

import java.util.HashSet;
import java.util.Set;

/** Gear watch's rules: how worn something is, when the strip shows, the one meow, and the tool guard. */
public final class Wear {
    private Wear() {}

    /** The strip shows when something is under a quarter; the meow comes under a tenth; the guard at 3 uses left. */
    public static final float LOW = 0.25f, VERY_LOW = 0.10f;
    public static final int GUARD_LEFT = 3;

    public enum Show {
        ALWAYS("always"), WHEN_LOW("when low"), NEVER("never");

        public final String word;

        Show(String word) {
            this.word = word;
        }

        public static Show of(String word) {
            for (Show s : values()) if (s.word.equals(word)) return s;
            return WHEN_LOW;
        }
    }

    public enum Guard {
        ENCHANTED("enchanted tools"), EVERY_TOOL("every tool"), OFF("off");

        public final String word;

        Guard(String word) {
            this.word = word;
        }

        public static Guard of(String word) {
            for (Guard g : values()) if (g.word.equals(word)) return g;
            return ENCHANTED;
        }
    }

    /** How much is left, from 1 (new) to 0 (about to break). Things that don't wear count as new. */
    public static float fraction(int damage, int max) {
        if (max <= 0) return 1f;
        return Math.max(0f, Math.min(1f, (max - damage) / (float) max));
    }

    public static boolean stripShows(Show show, float lowest) {
        return show == Show.ALWAYS || show == Show.WHEN_LOW && lowest < LOW;
    }

    /** Should this tool stop breaking blocks and hitting, so it doesn't snap? */
    public static boolean guarded(Guard guard, int damage, int max, boolean enchanted) {
        if (max <= 0 || guard == Guard.OFF) return false;
        if (guard == Guard.ENCHANTED && !enchanted) return false;
        return max - damage <= GUARD_LEFT;
    }

    /** One meow per item when it drops under a tenth; once it is mended above that, it may meow again later. */
    public static final class Meows {
        private final Set<String> told = new HashSet<>();

        /** @param key which item: its slot and kind ("head|minecraft:iron_helmet") */
        public boolean check(String key, float fraction) {
            if (fraction >= VERY_LOW) {
                told.remove(key);
                return false;
            }
            return told.add(key);
        }
    }
}
