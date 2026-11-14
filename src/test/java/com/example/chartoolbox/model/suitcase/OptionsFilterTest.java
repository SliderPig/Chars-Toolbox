package com.example.chartoolbox.model.suitcase;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionsFilterTest {
    static final List<String> SAVED = List.of("version:3465", "fov:0.5", "renderDistance:7", "resourcePacks:[\"vanilla\"]",
            "incompatibleResourcePacks:[]", "lastServer:mc.example", "key_key.attack:key.mouse.left",
            "key_key.charmap.world_map:key.keyboard.n", "key_key.chartoolbox.open:key.keyboard.f12:CONTROL");
    static final List<String> TARGET = List.of("version:9999", "fov:0.0", "lastServer:keep.me", "key_key.attack:key.mouse.right");

    @Test
    void skippedLinesAreNotCarried() {
        List<String> c = OptionsFilter.carried(SAVED);
        assertEquals(List.of("fov:0.5", "renderDistance:7", "key_key.attack:key.mouse.left",
                "key_key.charmap.world_map:key.keyboard.n", "key_key.chartoolbox.open:key.keyboard.f12:CONTROL"), c);
    }

    @Test
    void mergeReplacesALineAndAppendsANewOne() {
        List<String> m = OptionsFilter.merge(TARGET, OptionsFilter.carried(SAVED), false, true, k -> true);
        assertEquals(List.of("version:9999", "fov:0.5", "lastServer:keep.me", "key_key.attack:key.mouse.right", "renderDistance:7"), m);
    }

    @Test
    void aKeyLineNeedsBothKeysAndAKnownKey() {
        List<String> m = OptionsFilter.merge(TARGET, OptionsFilter.carried(SAVED), true, false, "key.attack"::equals);
        assertEquals(List.of("version:9999", "fov:0.0", "lastServer:keep.me", "key_key.attack:key.mouse.left"), m);
    }

    @Test
    void unknownKeysAreLeftOut() {
        List<String> m = OptionsFilter.merge(TARGET, OptionsFilter.carried(SAVED), true, true, k -> !k.startsWith("key.charmap"));
        assertFalse(m.stream().anyMatch(l -> l.startsWith("key_key.charmap")));
        assertTrue(m.contains("key_key.chartoolbox.open:key.keyboard.f12:CONTROL"));
        assertTrue(m.contains("fov:0.5"));
    }

    @Test
    void junkLinesDoNoHarm() {
        List<String> m = OptionsFilter.merge(List.of("", "no colon here", "fov:0.0"), List.of("fov:0.5", "also no colon"), true, true, k -> true);
        assertTrue(m.contains("fov:0.5"));
        assertTrue(m.contains("no colon here"));
    }
}
