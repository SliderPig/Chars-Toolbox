package com.example.chartoolbox.model.keys;

import java.util.List;

/** Two or more actions fighting over one key, in the order the game registered them. */
public record Clash(String key, List<Bind> binds) {
    /** The same actions on the same key always give the same words, in any order: what "seen before" is checked by. */
    public String signature() {
        return key + "=" + String.join("+", binds.stream().map(Bind::id).sorted().toList());
    }
}
