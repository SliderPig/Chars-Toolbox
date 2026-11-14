package com.example.chartoolbox.model.settings;

import java.util.List;

/** Every setting of the parts written in the Toolbox, in the order their pages list them. */
public final class Settings {
    private Settings() {}

    // Zoom
    public static final Setting ZOOM_START = Setting.number("zoom", "zoom.start", "Zoom when you press it (x)", 4, 2, 12);

    // Little HUD
    public static final Setting HUD_CORNER = Setting.choice("littlehud", "hud.corner", "Corner", "top left", "top left", "top right", "bottom left", "bottom right");
    public static final Setting HUD_COORDS = Setting.onOff("littlehud", "hud.coords", "Where you are", true);
    public static final Setting HUD_FACING = Setting.onOff("littlehud", "hud.facing", "Which way you face", true);
    public static final Setting HUD_BIOME = Setting.onOff("littlehud", "hud.biome", "Biome", true);
    public static final Setting HUD_DAY = Setting.onOff("littlehud", "hud.day", "Day and time in the game", true);
    public static final Setting HUD_CLOCK = Setting.onOff("littlehud", "hud.clock", "Real clock", true);
    public static final Setting HUD_CLOCK_STYLE = Setting.choice("littlehud", "hud.clockStyle", "Clock style", "12 hour", "12 hour", "24 hour");
    public static final Setting HUD_FPS = Setting.onOff("littlehud", "hud.fps", "Frames a second", false);
    public static final Setting HUD_OTHER = Setting.onOff("littlehud", "hud.otherSide", "Same spot in the Nether / Overworld", false);

    // Gear watch
    public static final Setting GEAR_SHOW = Setting.choice("gearwatch", "gear.show", "Show the wear strip", "when low", "always", "when low", "never");
    public static final Setting GEAR_GUARD = Setting.choice("gearwatch", "gear.guard", "Stop a tool 3 uses before it breaks", "enchanted tools", "enchanted tools", "every tool", "off");
    public static final Setting GEAR_MEOW = Setting.onOff("gearwatch", "gear.meow", "Meow when something is nearly broken", true);

    // Memory watch
    public static final Setting MEM_PAUSE = Setting.onOff("memorywatch", "memory.pauseLine", "Memory line on the pause screen", true);

    // Cat corner: its four helpers, each with its own switch, and their settings
    public static final Setting CAT_SLEEPY = Setting.onOff("catcorner", "cat.sleepy", "Sleepy Cat on your screen", true);
    public static final Setting SLEEPY_SIZE = Setting.number("catcorner", "cat.sleepySize", "Sleepy Cat size", 2, 1, 4);
    public static final Setting SLEEPY_X = Setting.hiddenNumber("catcorner", "cat.sleepyX", -1, -1, 10000);
    public static final Setting SLEEPY_Y = Setting.hiddenNumber("catcorner", "cat.sleepyY", -1, -1, 10000);
    public static final Setting CAT_FULLBRIGHT = Setting.onOff("catcorner", "cat.fullbright", "Fullbright (see in the dark)", false);
    public static final Setting CAT_REJOIN = Setting.onOff("catcorner", "cat.rejoin", "Auto Rejoin after a kick", false);
    public static final Setting REJOIN_COMMAND = Setting.text("catcorner", "rejoin.command", "Command after rejoining", "");
    public static final Setting REJOIN_DELAY = Setting.number("catcorner", "rejoin.delay", "Seconds before that command", 15, 0, 120);
    public static final Setting CAT_EAT = Setting.onOff("catcorner", "cat.eat", "Auto Eat when hungry", false);
    public static final Setting EAT_BELOW = Setting.number("catcorner", "autoeat.below", "Eat when hunger is at or below (20 = full)", 14, 1, 19);

    private static final List<Setting> ALL = List.of(ZOOM_START,
            HUD_CORNER, HUD_COORDS, HUD_FACING, HUD_BIOME, HUD_DAY, HUD_CLOCK, HUD_CLOCK_STYLE, HUD_FPS, HUD_OTHER,
            GEAR_SHOW, GEAR_GUARD, GEAR_MEOW, MEM_PAUSE,
            CAT_SLEEPY, SLEEPY_SIZE, SLEEPY_X, SLEEPY_Y, CAT_FULLBRIGHT, CAT_REJOIN, REJOIN_COMMAND, REJOIN_DELAY, CAT_EAT, EAT_BELOW);

    public static List<Setting> all() {
        return ALL;
    }

    /** A part's settings, in page order (hidden ones included). */
    public static List<Setting> of(String part) {
        return ALL.stream().filter(s -> s.part().equals(part)).toList();
    }
}
