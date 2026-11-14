package com.example.chartoolbox.model;

import java.util.List;
import java.util.Locale;

/**
 * Every part the Toolbox has, in the order the screen shows them. A part is added here in the build step that makes
 * it real. A carried part's same-job list must match parts.lock.json (its hook is written from that file); a test checks it.
 */
public final class Parts {
    private Parts() {}

    public static final Part MAP = carried("charmap", "Map", "Minimap and world map",
            List.of("xaerominimap", "xaeroworldmap", "journeymap"), "com.example.charmap.client.SettingsScreen", true);
    public static final Part FAR_VIEW = carried("charlod", "Far view", "See distant land",
            List.of("distanthorizons"), "com.example.charlod.client.FarScreen", false);
    public static final Part MAIN_MENU = carried("charmenu", "Main menu", "Title screen scenes",
            List.of("fancymenu", "custommainmenu", "mainmenu", "custommenu"), "com.example.charmenu.client.MenuSettingsScreen", false);
    public static final Part TEAMS = carried("charteams", "Teams", "Team up with friends", List.of(), null, false);
    public static final Part CONFIG_HELPER = carried("charconfig", "Config helper", "All your settings", List.of(), null, false);
    public static final Part FORTRESS_BOXES = carried("fortressboxes", "Fortress boxes", "Shows fortress edges", List.of(), null, false);

    /** Written in the Toolbox itself: keys, options and switches carried to a new profile. */
    public static final Part SUITCASE = new Part("suitcase", "Bring my settings", "Settings follow you", Part.Kind.BUILT_IN, true,
            List.of(), "com.example.chartoolbox.client.SuitcaseScreen", false);

    /** Lists every key two actions are fighting over, and sorts them out. */
    public static final Part KEY_CLASH = new Part("keyclash", "Key clash fixer", "Finds keys with two jobs", Part.Kind.BUILT_IN, true,
            List.of(), "com.example.chartoolbox.client.KeyClashScreen", false);

    private static final String PAGE = "com.example.chartoolbox.client.PartSettingsScreen";

    /** Hold a key (H) to zoom in. Its key is only added at start-up, so switching it needs a restart to finish. */
    public static final Part ZOOM = new Part("zoom", "Zoom", "Hold a key to zoom in", Part.Kind.BUILT_IN, true,
            List.of("zoomify", "okzoomer", "ok_zoomer", "wi_zoom", "logical_zoom", "simplezoom", "justzoom"), PAGE, false, true);
    /** A small pill with where you are, which way you face, the biome and the time. Off at first. */
    public static final Part LITTLE_HUD = new Part("littlehud", "Little HUD", "Coords, biome and time", Part.Kind.BUILT_IN, false,
            List.of(), PAGE, false);
    /** "+3 Iron Ore" beside the hotbar. Off at first. */
    public static final Part PICKUP_LIST = new Part("pickuplist", "Pickup list", "Shows what you pick up", Part.Kind.BUILT_IN, false,
            List.of(), null, false);
    /** Armour and tool wear, one meow, and a guard that stops a tool before it snaps. */
    public static final Part GEAR_WATCH = new Part("gearwatch", "Gear watch", "Watches tool wear", Part.Kind.BUILT_IN, true,
            List.of(), PAGE, false);
    /** A quiet warning when the game or the PC is nearly out of memory. */
    public static final Part MEMORY_WATCH = new Part("memorywatch", "Memory watch", "Low memory warning", Part.Kind.BUILT_IN, true,
            List.of(), PAGE, false);

    /**
     * Sleepy Cat, Fullbright, Auto Rejoin and Auto Eat, each with its own switch on its page. Starts off when a mod with
     * the same four helpers is installed. Fullbright has a key: a restart finishes a switch.
     */
    public static final Part CAT_CORNER = new Part("catcorner", "Cat corner", "Cat and 3 helpers", Part.Kind.BUILT_IN, true,
            List.of("baritonegui"), PAGE, false, true);

    private static final List<Part> ALL = List.of(MAP, FAR_VIEW, MAIN_MENU, TEAMS, CONFIG_HELPER, FORTRESS_BOXES, SUITCASE, KEY_CLASH,
            ZOOM, LITTLE_HUD, PICKUP_LIST, GEAR_WATCH, MEMORY_WATCH, CAT_CORNER);

    private static Part carried(String id, String name, String blurb, List<String> sameJob, String settings, boolean settingsNeedWorld) {
        return new Part(id, name, blurb, Part.Kind.CARRIED, true, sameJob, settings, settingsNeedWorld);
    }

    public static List<Part> all() {
        return ALL;
    }

    /** The part with this id, or null. */
    public static Part byId(String id) {
        for (Part p : ALL) if (p.id().equals(id)) return p;
        return null;
    }

    /** The parts whose name or blurb has the words typed in the search box, in any letter case. Nothing typed = all. */
    public static List<Part> matching(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return ALL;
        return ALL.stream().filter(p -> p.name().toLowerCase(Locale.ROOT).contains(q)
                || p.blurb().toLowerCase(Locale.ROOT).contains(q)).toList();
    }
}
