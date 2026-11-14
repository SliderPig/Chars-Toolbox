package com.example.chartoolbox.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchFileTest {
    @Test
    void readsForgivingly() {
        var m = SwitchFile.parse("# note\n\n fortressboxes = FALSE \nmystery=true\nzoom=maybe\n");
        assertEquals(List.of("fortressboxes", "mystery", "zoom"), List.copyOf(m.keySet()));
        assertFalse(m.get("fortressboxes"));
        assertTrue(m.get("mystery"));
        assertTrue(m.get("zoom"));
    }

    @Test
    void aMissingIdIsOn() {
        assertTrue(SwitchFile.on(Map.of(), "fortressboxes"));
    }

    @Test
    void unknownIdsSurviveASave() {
        var m = SwitchFile.parse("mystery=false\n");
        m.put("fortressboxes", true);
        assertEquals(m, SwitchFile.parse(SwitchFile.format(m)));
        assertTrue(SwitchFile.format(m).startsWith("# Char's Toolbox: which parts are on"));
    }
}
