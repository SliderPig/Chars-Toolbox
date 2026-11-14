package com.example.chartoolbox.model.keys;

import java.util.ArrayList;
import java.util.List;

/**
 * The keys "fix for me" may hand out, in the order it tries them: letters a hand reaches easily first (not H: Zoom has it), then a few
 * function keys, then the keypad. Never a key the game keeps for itself (Esc, F1, F3, Enter, Shift, Ctrl, Alt...).
 */
public final class FreeKeys {
    private FreeKeys() {}

    public static final List<String> ORDER;

    static {
        List<String> keys = new ArrayList<>();
        for (char c : "jnoiklpugzxvcbmrtyfqe".toCharArray()) keys.add("key.keyboard." + c);
        for (String f : List.of("f6", "f7", "f8", "f9", "f12")) keys.add("key.keyboard." + f);
        for (int i = 0; i <= 9; i++) keys.add("key.keyboard.keypad." + i);
        ORDER = List.copyOf(keys);
    }
}
