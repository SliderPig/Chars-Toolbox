package com.example.chartoolbox.model.keys;

import com.example.chartoolbox.model.keys.Bind.Rank;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.chartoolbox.model.keys.KeyRulesTest.bind;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClashMemoryTest {
    static final String B = "key.keyboard.b";
    final Clash clash = new Clash(B, List.of(bind("key.charmap.add", B, "UNIVERSAL", Rank.TOOLBOX), bind("key.fortressboxes.toggle", B, "IN_GAME", Rank.TOOLBOX)));

    @Test
    void aNewClashIsNewOnce() {
        ClashMemory m = new ClashMemory();
        assertEquals(List.of(clash), m.newOnes(List.of(clash)));
        m.remember(List.of(clash));
        assertTrue(m.newOnes(List.of(clash)).isEmpty());
    }

    @Test
    void aSeenClashIsNotNewAgain() {
        ClashMemory m = new ClashMemory();
        m.remember(List.of(clash));
        ClashMemory read = ClashMemory.parse(m.format());
        assertTrue(read.newOnes(List.of(clash)).isEmpty());
        // the same two actions on another key are a new clash
        Clash moved = new Clash("key.keyboard.n", List.of(clash.binds().get(0).withKey("key.keyboard.n", "NONE"), clash.binds().get(1).withKey("key.keyboard.n", "NONE")));
        assertEquals(1, read.newOnes(List.of(moved)).size());
    }

    @Test
    void undoRoundTrips() {
        ClashMemory m = new ClashMemory();
        m.setUndo(List.of(new Move("key.fortressboxes.toggle", "Toggle", "Fortress Boxes", B, "NONE", "key.keyboard.j"),
                new Move("key.a.b", "A | b", "Mod = x", "key.mouse.4", "SHIFT", Bind.UNBOUND)));
        ClashMemory read = ClashMemory.parse(m.format());
        assertEquals(m.undo(), read.undo());
        read.setUndo(List.of());
        assertTrue(ClashMemory.parse(read.format()).undo().isEmpty());
    }

    @Test
    void aBrokenFileIsAnEmptyMemory() {
        ClashMemory read = ClashMemory.parse("undo.0=only|two\nseen=\n\u0000garbage");
        assertTrue(read.undo().isEmpty());
        assertEquals(1, read.newOnes(List.of(clash)).size());
    }
}
