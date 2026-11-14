package com.example.chartoolbox.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefaultsTest {
    static final Part MAP = new Part("charmap", "Map", "", Part.Kind.CARRIED, true, List.of("xaerominimap", "journeymap"), null, false);
    static final Part HUD = new Part("littlehud", "Little HUD", "", Part.Kind.BUILT_IN, false, List.of(), null, false);

    @Test
    void usesEachPartsDefault() {
        assertEquals(Map.of("charmap", true, "littlehud", false), Defaults.firstRun(List.of(MAP, HUD), id -> false));
    }

    @Test
    void aSameJobModStartsThePartOff() {
        assertFalse(Defaults.firstRun(List.of(MAP), "journeymap"::equals).get("charmap"));
        assertEquals("journeymap", Defaults.sameJobMod(MAP, "journeymap"::equals));
        assertNull(Defaults.sameJobMod(MAP, id -> false));
    }
}
