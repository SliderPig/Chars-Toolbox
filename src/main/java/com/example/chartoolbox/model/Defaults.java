package com.example.chartoolbox.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** What is on the very first time the Toolbox runs in a profile. */
public final class Defaults {
    private Defaults() {}

    /** Each part's own default, except that a part starts off when another installed mod already does its job. */
    public static Map<String, Boolean> firstRun(List<Part> parts, Predicate<String> modLoaded) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (Part p : parts) out.put(p.id(), p.defaultOn() && sameJobMod(p, modLoaded) == null);
        return out;
    }

    /** The mod id of an installed mod that does this part's job, or null. */
    public static String sameJobMod(Part part, Predicate<String> modLoaded) {
        for (String id : part.sameJobMods()) if (modLoaded.test(id)) return id;
        return null;
    }
}
