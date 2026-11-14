package com.example.chartoolbox.model.small;

/** Zoom's sums: how far in, how it glides, how narrow the view, how slow the mouse. */
public final class ZoomMath {
    private ZoomMath() {}

    public static final float LEAST = 2f, MOST = 12f, STEP = 1.25f;
    /** How quickly the zoom glides to where it is going: about 70 ms to get two thirds of the way. */
    private static final float GLIDE_MS = 70f;

    /** One notch of the mouse wheel while zoomed: a quarter further in or out, kept between 2x and 12x. */
    public static float scrolled(float target, double notches) {
        float z = (float) (target * Math.pow(STEP, notches));
        return Math.max(LEAST, Math.min(MOST, z));
    }

    /** The zoom a frame later, gliding toward the target and never past it. */
    public static float step(float current, float target, float dtMs) {
        if (Math.abs(target - current) < 0.001f) return target;
        float k = 1f - (float) Math.exp(-Math.max(0f, dtMs) / GLIDE_MS);
        return current + (target - current) * k;
    }

    public static double fov(double fov, float zoom) {
        return fov / Math.max(1f, zoom);
    }

    /**
     * The mouse turned the view from last to now this frame; zoomed in, it only turns a zoom-th as far, so aiming feels the
     * same at any zoom. A jump of more than 90 degrees is the server turning you (a teleport), so it is left as it is.
     */
    public static float turned(float last, float now, float zoom) {
        float delta = now - last;
        if (Math.abs(delta) > 90f) return now;
        return last + delta / Math.max(1f, zoom);
    }
}
