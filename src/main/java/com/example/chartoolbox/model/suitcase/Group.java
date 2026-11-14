package com.example.chartoolbox.model.suitcase;

/** The kinds of settings the suitcase carries. Everything is always saved; these are what you tick when bringing. */
public enum Group {
    KEYS("Keys", "Your key bindings"),
    OPTIONS("Game options", "Video, sound and the rest"),
    SERVERS("Server list", "Your saved servers"),
    TOOLBOX("Toolbox", "Switches and part settings"),
    WAYPOINTS("Map waypoints", "Waypoints on your servers");

    public final String label, blurb;

    Group(String label, String blurb) {
        this.label = label;
        this.blurb = blurb;
    }
}
