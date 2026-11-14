package com.example.chartoolbox.model.small;

import java.time.LocalTime;
import java.util.Locale;

/** The Little HUD's lines in plain words. */
public final class HudWords {
    private HudWords() {}

    private static final String[] FACING = {"South (+Z)", "West (-X)", "North (-Z)", "East (+X)"};

    /** The way you face, from the game's yaw (0 = south, 90 = west, 180 = north, 270 = east). */
    public static String facing(float yaw) {
        float y = ((yaw % 360f) + 360f) % 360f;
        return FACING[Math.round(y / 90f) % 4];
    }

    /** "Day 3, 14:30": the game's day count and its clock (a day starts at 6:00 in the morning). */
    public static String dayTime(long dayTime) {
        long t = Math.max(0, dayTime);
        long day = t / 24000 + 1, tod = t % 24000;
        long hours = (tod / 1000 + 6) % 24, minutes = (tod % 1000) * 60 / 1000;
        return String.format(Locale.ROOT, "Day %d, %d:%02d", day, hours, minutes);
    }

    /** The real time: "9:41 pm" or "21:41". */
    public static String clock(LocalTime time, boolean twelveHour) {
        if (!twelveHour) return String.format(Locale.ROOT, "%d:%02d", time.getHour(), time.getMinute());
        int h = time.getHour() % 12 == 0 ? 12 : time.getHour() % 12;
        return String.format(Locale.ROOT, "%d:%02d %s", h, time.getMinute(), time.getHour() < 12 ? "am" : "pm");
    }

    /** The block you stand in: "120, 64, -300". */
    public static String coords(double x, double y, double z) {
        return (int) Math.floor(x) + ", " + (int) Math.floor(y) + ", " + (int) Math.floor(z);
    }

    /** Where the same spot is on the other side of a Nether portal (8 blocks here = 1 block there). */
    public static String otherSide(double x, double z, boolean inNether) {
        if (inNether) return "Overworld " + (int) Math.floor(x * 8) + ", " + (int) Math.floor(z * 8);
        return "Nether " + (int) Math.floor(x / 8) + ", " + (int) Math.floor(z / 8);
    }

    /** "minecraft:dark_forest" as words, for when the game has no name for it: "Dark forest". */
    public static String prettyId(String id) {
        String s = id.substring(id.indexOf(':') + 1).replace('_', ' ').replace('/', ' ').trim();
        return s.isEmpty() ? id : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
