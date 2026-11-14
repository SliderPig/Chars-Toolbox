package com.example.chartoolbox.model.cat;

import java.util.List;

/**
 * Sleepy Cat's picture (Char's black cat): lying side-on with its head on the right, eyes shut,
 * breathing slowly, and its tail hanging over the edge it lies on, swinging like a pendulum.
 */
public final class SleepyArt {
    private SleepyArt() {}

    /** 22 x 8. '#' fur, 'p' inner ear, 'l' closed eye, 'n' nose. */
    public static final String[] BODY = {
            "..............#.....#.",
            "..............##...##.",
            "..............#p###p#.",
            "....##########ll#ll##.",
            "..###############n###.",
            ".####################.",
            ".####################.",
            "..##################..",
    };
    public static final int W = 22, H = BODY.length, TAIL = 9;

    /** How far (in art pixels) tail segment i swings at time t: it grows toward the tip, and the tip lags a little. */
    public static double tailSwing(int i, long millis) {
        return Math.sin(millis / 1000.0 * 1.6 - i * 0.18) * i * 0.55;
    }

    /** Every few seconds the back rises one pixel. */
    public static boolean inhale(long millis) {
        return Math.sin(millis / 1400.0) > 0.2;
    }

    /** Keeps the cat (left end x, lying on line y, s screen pixels a pixel) and its hanging tail on a w x h screen. */
    public static List<Integer> clampSpot(int x, int y, int s, int w, int h) {
        int cx = Math.max(4, Math.min(w - W * s - 4, x));
        int cy = Math.max(H * s + 4, Math.min(h - TAIL * s - 2, y));
        return List.of(cx, cy);
    }
}
