package com.example.chartoolbox.model.small;

import java.util.Locale;

/**
 * Memory watch: fed a sample every few seconds, it says when to warn. The game's own memory over 90% of its limit for
 * 30 seconds, or the PC's free memory under 8% (or under 500 MB) two samples in a row. At most one warning every 10
 * minutes, and none while the game is loading. No "clean memory" button: those don't really fix anything.
 */
public final class MemoryRules {
    public enum Warn { GAME, PC }

    static final double GAME_FULL = 0.90, PC_LOW_PART = 0.08, PC_LOW_BYTES = 500.0 * (1 << 20);
    static final long GAME_FOR = 30_000, QUIET_FOR = 600_000;
    private static final double GB = 1 << 30;

    private long gameHighSince = -1;
    private int pcLowSamples;
    private long lastWarn = Long.MIN_VALUE;

    /** @param pcFree the PC's free memory in bytes, or below 0 when it can't be read */
    public Warn sample(long now, double gameUsed, double gameMax, double pcFree, double pcTotal, boolean loading) {
        if (loading) {
            gameHighSince = -1;
            pcLowSamples = 0;
            return null;
        }
        boolean gameHigh = gameMax > 0 && gameUsed / gameMax > GAME_FULL;
        if (!gameHigh) gameHighSince = -1;
        else if (gameHighSince < 0) gameHighSince = now;
        boolean pcLow = pcFree >= 0 && pcTotal > 0 && (pcFree < PC_LOW_BYTES || pcFree / pcTotal < PC_LOW_PART);
        pcLowSamples = pcLow ? pcLowSamples + 1 : 0;
        Warn warn = gameHigh && now - gameHighSince >= GAME_FOR ? Warn.GAME : pcLowSamples >= 2 ? Warn.PC : null;
        if (warn == null || (lastWarn != Long.MIN_VALUE && now - lastWarn < QUIET_FOR)) return null;
        lastWarn = now;
        return warn;
    }

    /** The small line on the pause screen. pcFree below 0 = left out. */
    public static String line(double gameUsed, double gameMax, double pcFree) {
        String s = String.format(Locale.ROOT, "Memory: %.1f of %.1f GB used", gameUsed / GB, gameMax / GB);
        return pcFree < 0 ? s : s + String.format(Locale.ROOT, ", PC %.1f GB free", pcFree / GB);
    }

    /** What the notice says, in plain words. */
    public static String words(Warn warn) {
        return warn == Warn.GAME
                ? "The game has nearly used all its memory. Lower the render distance, or give it more memory in your launcher."
                : "Your PC is nearly out of memory. Closing other programs, like browser tabs, will help.";
    }
}
