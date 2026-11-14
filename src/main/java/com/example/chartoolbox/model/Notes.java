package com.example.chartoolbox.model;

/** The short line a part's row can show instead of its blurb, and whether its switch is locked. */
public final class Notes {
    private Notes() {}

    public enum State { NORMAL, RESTART, SEPARATE_COPY, OLD_COPY_IN_CHARGE }

    /**
     * @param text  the whole sentence, shown in the card by the mouse
     * @param brief a few words of it that fit on the row itself
     */
    public record Note(State state, String text, String brief, boolean locked) {}

    private static final Note NONE = new Note(State.NORMAL, "", "", false);

    /**
     * @param onAtStart      what the part was when the game started; null for a carried mod whose running copy never
     *                       reported in, which means an older copy from before switches existed is the one running
     * @param separateCopy   the mods folder holds its own file of this mod
     * @param sameJobModName the display name of an installed mod that does the same job, or null
     */
    public static Note of(Part part, boolean on, Boolean onAtStart, boolean separateCopy, String sameJobModName, boolean firstRun) {
        if (part.kind() == Part.Kind.CARRIED && onAtStart == null) {
            return new Note(State.OLD_COPY_IN_CHARGE, "An older separate copy is in charge. Delete it to use this switch.", "Old copy in charge", true);
        }
        if (part.restartToFinish() && onAtStart != null && on != onAtStart) return new Note(State.RESTART, "Restart to finish", "Restart to finish", false);
        if (separateCopy) return new Note(State.SEPARATE_COPY, "Separate copy found, you can delete it", "Separate copy found", false);
        if (firstRun && sameJobModName != null && !on) {
            return new Note(State.NORMAL, sameJobModName + " is here, so this starts off", sameJobModName + " is here", false);
        }
        return NONE;
    }
}
