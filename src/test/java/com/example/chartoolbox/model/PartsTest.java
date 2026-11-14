package com.example.chartoolbox.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartsTest {
    @Test
    void findsByNameOrBlurbInAnyCase() {
        assertEquals(List.of(Parts.FORTRESS_BOXES), Parts.matching("FORTRESS"));
        assertEquals(List.of(Parts.FORTRESS_BOXES), Parts.matching("edges"));
        assertTrue(Parts.matching("zzz").isEmpty());
        assertEquals(Parts.all(), Parts.matching("  "));
    }

    // parts.lock.json says which mods are packed into the jar and which same-job mods each one's hook was written
    // with; the list the screen shows has to say the same, or a part and its hook would disagree on a first run.
    @Test
    void theLockFileAndThePartsAgree() throws Exception {
        JsonObject lock = JsonParser.parseString(Files.readString(Path.of("parts.lock.json"))).getAsJsonObject();
        Map<String, List<String>> fromLock = new HashMap<>();
        for (String id : lock.keySet()) {
            List<String> same = new ArrayList<>();
            if (lock.getAsJsonObject(id).has("sameJob")) for (JsonElement e : lock.getAsJsonObject(id).getAsJsonArray("sameJob")) same.add(e.getAsString());
            fromLock.put(id, same);
        }
        Map<String, List<String>> fromParts = new HashMap<>();
        for (Part p : Parts.all()) if (p.kind() == Part.Kind.CARRIED) fromParts.put(p.id(), p.sameJobMods());
        assertEquals(fromLock, fromParts);
        assertEquals(6, fromParts.size());
    }
}
