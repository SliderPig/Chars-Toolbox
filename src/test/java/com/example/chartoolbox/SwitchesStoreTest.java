package com.example.chartoolbox;

import com.example.chartoolbox.model.Parts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchesStoreTest {
    @TempDir
    Path dir;

    @BeforeEach
    void clearProperties() {
        System.clearProperty("chartoolbox.part.fortressboxes");
        System.clearProperty("chartoolbox.started.fortressboxes");
    }

    @Test
    void firstRunWritesTheDefaults() throws Exception {
        var s = new SwitchStore(dir.resolve("chartoolbox/parts.properties"), Parts.all(), id -> false);
        s.load();
        assertTrue(s.firstRun());
        assertTrue(s.isOn("fortressboxes"));
        assertEquals("true", System.getProperty("chartoolbox.part.fortressboxes"));
        assertTrue(Files.readString(dir.resolve("chartoolbox/parts.properties")).contains("fortressboxes=true"));
    }

    @Test
    void anExistingFileIsNotAFirstRun() throws Exception {
        Path f = dir.resolve("parts.properties");
        Files.writeString(f, "fortressboxes=false\n");
        var s = new SwitchStore(f, Parts.all(), id -> false);
        s.load();
        assertFalse(s.firstRun());
        assertFalse(s.isOn("fortressboxes"));
        assertEquals("false", System.getProperty("chartoolbox.part.fortressboxes"));
    }

    @Test
    void setChangesMemoryPropertyAndFile() throws Exception {
        Path f = dir.resolve("parts.properties");
        var s = new SwitchStore(f, Parts.all(), id -> false);
        s.load();
        s.set("fortressboxes", false);
        assertFalse(s.isOn("fortressboxes"));
        assertEquals("false", System.getProperty("chartoolbox.part.fortressboxes"));
        assertTrue(Files.readString(f).contains("fortressboxes=false"));
    }

    @Test
    void setKeepsGoingWhenTheFileCannotBeWritten() throws Exception {
        Path blocker = dir.resolve("blocker");
        Files.writeString(blocker, "a file where a folder should be");
        var s = new SwitchStore(blocker.resolve("parts.properties"), Parts.all(), id -> false);
        s.load();
        s.set("fortressboxes", false);
        assertFalse(s.isOn("fortressboxes"));
        assertEquals("false", System.getProperty("chartoolbox.part.fortressboxes"));
    }

    @Test
    void aCarriedPartsStartComesFromTheHook() {
        var s = new SwitchStore(dir.resolve("parts.properties"), Parts.all(), id -> false);
        s.load();
        assertNull(s.onAtStart("fortressboxes"));
        System.setProperty("chartoolbox.started.fortressboxes", "false");
        assertEquals(Boolean.FALSE, s.onAtStart("fortressboxes"));
    }

    // Bring my settings swaps the switch file for the one from the suitcase: the game has to pick that up at once.
    @Test
    void reloadPicksUpAChangedFile() throws Exception {
        Path f = dir.resolve("parts.properties");
        var s = new SwitchStore(f, Parts.all(), id -> false);
        s.load();
        assertTrue(s.firstRun());
        Files.writeString(f, "fortressboxes=false");
        s.reload();
        assertFalse(s.isOn("fortressboxes"));
        assertEquals("false", System.getProperty("chartoolbox.part.fortressboxes"));
        assertTrue(s.firstRun(), "a reload doesn't end the first run");
    }
}
