package com.example.chartoolbox.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotesTest {
    static final Part P = Parts.FORTRESS_BOXES;

    @Test
    void anOldCopyLocksTheRow() {
        var n = Notes.of(P, true, null, true, null, false);
        assertEquals(Notes.State.OLD_COPY_IN_CHARGE, n.state());
        assertTrue(n.locked());
    }

    @Test
    void aChangedSwitchAsksForARestart() {
        assertEquals(Notes.State.RESTART, Notes.of(P, false, true, false, null, false).state());
        assertEquals("Restart to finish", Notes.of(P, true, false, true, null, false).text());
    }

    @Test
    void aListeningSeparateCopyOnlyGetsANote() {
        var n = Notes.of(P, true, true, true, null, false);
        assertEquals(Notes.State.SEPARATE_COPY, n.state());
        assertFalse(n.locked());
    }

    @Test
    void firstRunSaysWhyAPartStartsOff() {
        assertEquals("Xaero's Minimap is here, so this starts off", Notes.of(P, false, false, false, "Xaero's Minimap", true).text());
    }

    @Test
    void aBuiltInPartWithoutKeysNeedsNoRestart() {
        assertEquals(Notes.State.NORMAL, Notes.of(Parts.SUITCASE, false, true, false, null, false).state());
        assertTrue(Parts.byId("charmap").restartToFinish());
        assertFalse(Parts.SUITCASE.restartToFinish());
    }

    @Test
    void normallyThereIsNoNote() {
        assertEquals("", Notes.of(P, true, true, false, null, false).text());
    }

    @Test
    void theRowGetsAShortFormThatFits() {
        assertEquals("Old copy in charge", Notes.of(P, true, null, true, null, false).brief());
        assertEquals("Restart to finish", Notes.of(P, false, true, false, null, false).brief());
        assertEquals("Separate copy found", Notes.of(P, true, true, true, null, false).brief());
        assertEquals("Xaero's Minimap is here", Notes.of(P, false, false, false, "Xaero's Minimap", true).brief());
        assertEquals("", Notes.of(P, true, true, false, null, false).brief());
    }
}
