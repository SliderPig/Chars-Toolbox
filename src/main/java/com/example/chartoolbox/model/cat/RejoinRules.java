package com.example.chartoolbox.model.cat;

/**
 * Auto Rejoin's rules: after a kick, wait 10 s, then 20 s, ... at most a minute between tries (the
 * server may still be waking up), give up after 20 tries in a row.
 */
public final class RejoinRules {
    private RejoinRules() {}

    public static final int MOST_TRIES = 20;

    public static long delayMs(int tries) {
        return Math.min(60_000L, 10_000L * (tries + 1));
    }

    public static boolean tryAgain(int tries) {
        return tries < MOST_TRIES;
    }

    /** The command to run after rejoining, without its slash; null when there is none. */
    public static String command(String typed) {
        String c = typed == null ? "" : typed.trim();
        if (c.startsWith("/")) c = c.substring(1).trim();
        return c.isEmpty() ? null : c;
    }
}
