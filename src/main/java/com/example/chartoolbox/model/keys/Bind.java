package com.example.chartoolbox.model.keys;

/**
 * One action the game has a key for, in plain strings so the rules can be tested without the game.
 *
 * @param id        the action's own name in the game (key.charmap.add_waypoint): unique
 * @param label     what the Controls screen calls it
 * @param owner     the mod id it belongs to ("minecraft" for the game's own)
 * @param ownerName that mod's name, for people
 * @param key       the key's name in the game (key.keyboard.b, key.mouse.4; {@link #UNBOUND} = none)
 * @param modifier  NONE, SHIFT, CONTROL or ALT
 * @param context   when it can fire: UNIVERSAL, IN_GAME, GUI, or another mod's own name for one
 * @param rank      who keeps a shared key when "fix for me" sorts it out
 */
public record Bind(String id, String label, String owner, String ownerName, String key, String modifier, String context, Rank rank) {
    public static final String UNBOUND = "key.keyboard.unknown";

    /** Who keeps a key two actions share, best first. */
    public enum Rank {
        /** The game's own actions. */
        GAME,
        /** Another mod's actions. */
        MOD,
        /** The game's two creative-only hotbar keys (save and load toolbar): they give way to a mod. */
        GAME_SPARE,
        /** A Toolbox part, carried or built in: it moves first. */
        TOOLBOX
    }

    public boolean bound() {
        return key != null && !key.isEmpty() && !UNBOUND.equals(key);
    }

    public Bind withKey(String newKey, String newModifier) {
        return new Bind(id, label, owner, ownerName, newKey, newModifier, context, rank);
    }
}
