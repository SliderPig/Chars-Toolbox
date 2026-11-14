package com.example.chartoolbox.model.keys;

import java.util.List;
import java.util.function.Function;

/** The short line on the Key clash fixer's row in the switch screen. */
public final class ClashWords {
    private ClashWords() {}

    /** "" when nothing clashes, "Key B has 2 jobs" for one clash, "3 keys clash" for more. */
    public static String rowLine(List<Clash> clashes, Function<String, String> keyName) {
        if (clashes.isEmpty()) return "";
        if (clashes.size() == 1) return "Key " + keyName.apply(clashes.get(0).key()) + " has " + clashes.get(0).binds().size() + " jobs";
        return clashes.size() + " keys clash";
    }
}
