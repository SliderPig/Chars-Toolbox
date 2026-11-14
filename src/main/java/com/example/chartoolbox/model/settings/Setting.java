package com.example.chartoolbox.model.settings;

import java.util.List;

/**
 * One setting of a part written in the Toolbox, shown on that part's settings page (the gear on its row).
 *
 * @param part    the part's id
 * @param key     its name in config/chartoolbox/settings.properties
 * @param kind    a switch, a choice that goes round when clicked, a number with - and +, or words typed in
 * @param shown   false = kept in the file but not listed (a cat's place, set by dragging it)
 */
public record Setting(String part, String key, String label, Kind kind, String defaultValue, List<String> choices, int min, int max, boolean shown) {
    public enum Kind { SWITCH, CHOICE, NUMBER, TEXT }

    static Setting onOff(String part, String key, String label, boolean on) {
        return new Setting(part, key, label, Kind.SWITCH, String.valueOf(on), List.of(), 0, 0, true);
    }

    static Setting choice(String part, String key, String label, String def, String... choices) {
        return new Setting(part, key, label, Kind.CHOICE, def, List.of(choices), 0, 0, true);
    }

    static Setting number(String part, String key, String label, int def, int min, int max) {
        return new Setting(part, key, label, Kind.NUMBER, String.valueOf(def), List.of(), min, max, true);
    }

    static Setting hiddenNumber(String part, String key, int def, int min, int max) {
        return new Setting(part, key, key, Kind.NUMBER, String.valueOf(def), List.of(), min, max, false);
    }

    static Setting text(String part, String key, String label, String def) {
        return new Setting(part, key, label, Kind.TEXT, def, List.of(), 0, 0, true);
    }
}
