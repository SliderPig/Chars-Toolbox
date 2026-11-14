package com.example.chartoolbox.model;

import java.util.List;

/**
 * One thing the Toolbox can switch on and off.
 *
 * @param id                a carried mod's own mod id, or a short name for a part built into the Toolbox
 * @param blurb             one short line saying what it does
 * @param sameJobMods       mod ids of other mods that do the same job: when one is installed this part starts off
 * @param settingsScreen    class name of the part's own settings screen (opened by the gear), or null
 * @param settingsNeedWorld that screen only works while a world is open, so the gear is hidden on the title screen
 * @param restartToFinish   switching it only fully takes effect after a restart (a carried mod, or a part with keys of
 *                          its own: keys are only added to the game at start-up)
 */
public record Part(String id, String name, String blurb, Kind kind, boolean defaultOn,
                   List<String> sameJobMods, String settingsScreen, boolean settingsNeedWorld, boolean restartToFinish) {
    public enum Kind {
        /** One of Char's own mods, riding inside the Toolbox's jar and still its own mod. */
        CARRIED,
        /** Written inside the Toolbox itself. */
        BUILT_IN
    }

    /** A carried mod always needs a restart to finish; a built-in part without keys never does. */
    public Part(String id, String name, String blurb, Kind kind, boolean defaultOn, List<String> sameJobMods, String settingsScreen, boolean settingsNeedWorld) {
        this(id, name, blurb, kind, defaultOn, sameJobMods, settingsScreen, settingsNeedWorld, kind == Kind.CARRIED);
    }
}
