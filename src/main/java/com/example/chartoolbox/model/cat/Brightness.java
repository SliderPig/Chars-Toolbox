package com.example.chartoolbox.model.cat;

/**
 * Fullbright's rule: the game's Brightness is set far past the slider's 100% while it is on, and
 * your own brightness is remembered and put back exactly when it goes off. A brightness above 100% with nothing
 * remembered is left over from a game that closed while bright, and goes back to 100%.
 */
public final class Brightness {
    public static final double BRIGHT = 15.0;

    private Double saved;

    /** What to write into the game's brightness now, or NaN when it is right as it is. */
    public double next(boolean on, double current) {
        if (on) {
            if (current >= BRIGHT) return Double.NaN;
            if (saved == null) saved = Math.min(1.0, current);
            return BRIGHT;
        }
        if (saved != null) {
            double back = saved;
            saved = null;
            return back;
        }
        return current > 1.0 ? 1.0 : Double.NaN;
    }

    /** Your own brightness while it is bright (for tests). */
    public double saved() {
        return saved == null ? Double.NaN : saved;
    }
}
