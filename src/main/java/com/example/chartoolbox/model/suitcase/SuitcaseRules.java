package com.example.chartoolbox.model.suitcase;

import java.util.Locale;

/**
 * When a profile saves into the suitcase and when it is asked to bring settings over. The point of the three modes:
 * a brand-new profile holds nothing worth keeping, so it must never overwrite a good suitcase just by being closed.
 */
public final class SuitcaseRules {
    private SuitcaseRules() {}

    /** One profile's way with the suitcase, kept in its own config/chartoolbox/toolbox.properties as suitcase=... */
    public enum Mode {
        /** Saves its settings every time the game closes. */
        SHARE,
        /** Someone else's settings are waiting: asked on the title screen, and saves nothing until it has answered. */
        ASK,
        /** Keeps its own settings: never asked, never saves. */
        APART;

        public String word() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The mode a saved word stands for, or null when it stands for none. */
        public static Mode read(String word) {
            if (word == null) return null;
            for (Mode m : values()) if (m.word().equals(word.trim().toLowerCase(Locale.ROOT))) return m;
            return null;
        }
    }

    /** A profile seen for the first time: it shares, unless another profile's settings are already waiting. */
    public static Mode initial(boolean suitcaseExists, boolean fromThisProfile) {
        return suitcaseExists && !fromThisProfile ? Mode.ASK : Mode.SHARE;
    }

    public static boolean shouldAsk(Mode mode, boolean partOn, boolean suitcaseExists, boolean fromThisProfile) {
        return mode == Mode.ASK && partOn && suitcaseExists && !fromThisProfile;
    }

    public static boolean savesOnClose(Mode mode, boolean partOn) {
        return mode == Mode.SHARE && partOn;
    }

    public static Mode afterBring() {
        return Mode.SHARE;
    }

    public static Mode afterNotNow() {
        return Mode.ASK;
    }

    public static Mode afterDontAsk() {
        return Mode.APART;
    }
}
