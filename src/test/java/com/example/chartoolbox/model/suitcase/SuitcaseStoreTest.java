package com.example.chartoolbox.model.suitcase;

import com.example.chartoolbox.model.suitcase.SuitcaseStore.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuitcaseStoreTest {
    @TempDir
    Path dir;
    Path root;
    SuitcaseStore store;
    Profile a, b;

    static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    static String read(Path file) throws IOException {
        return Files.readString(file);
    }

    // A played-in profile: its own options, server list, switches, map settings and waypoints.
    Profile played(String name) throws IOException {
        Path game = dir.resolve(name);
        write(game.resolve("options.txt"), "version:3465\nfov:0.5\nlastServer:mc.example\nkey_key.attack:key.mouse.left\nkey_key.charmap.world_map:key.keyboard.n\n");
        write(game.resolve("servers.dat"), "servers of " + name);
        write(game.resolve("config/chartoolbox/parts.properties"), "fortressboxes=false\n");
        write(game.resolve("config/chartoolbox/toolbox.properties"), "suitcase=share\n");
        write(game.resolve("config/charmap.properties"), "showMinimap=false\n");
        write(game.resolve("charmap/server_play.example/waypoints.txt"), "Home 1 2 3");
        write(game.resolve("charmap/local_MyWorld/waypoints.txt"), "only in that world");
        return new Profile(game, game.resolve("config"));
    }

    Profile fresh(String name) throws IOException {
        Path game = dir.resolve(name);
        write(game.resolve("options.txt"), "version:9999\nfov:0.0\n");
        Files.createDirectories(game.resolve("config"));
        return new Profile(game, game.resolve("config"));
    }

    @BeforeEach
    void setUp() throws IOException {
        root = dir.resolve("suitcase");
        store = new SuitcaseStore(root);
        a = played("A");
        b = fresh("B");
    }

    @Test
    void saveWritesEverythingAndTheInfo() throws IOException {
        store.save(a, "0.3", 1000L);
        var info = store.info().orElseThrow();
        assertEquals(1000L, info.savedAt());
        assertEquals("A", info.profileName());
        assertEquals("0.3", info.toolboxVersion());
        Path saved = root.resolve("saved");
        assertEquals(List.of("fov:0.5", "key_key.attack:key.mouse.left", "key_key.charmap.world_map:key.keyboard.n"), Files.readAllLines(saved.resolve("options.txt")));
        assertEquals("servers of A", read(saved.resolve("servers.dat")));
        assertEquals("fortressboxes=false\n", read(saved.resolve("config/chartoolbox/parts.properties")));
        assertEquals("showMinimap=false\n", read(saved.resolve("config/charmap.properties")));
        assertEquals("Home 1 2 3", read(saved.resolve("charmap/server_play.example/waypoints.txt")));
        assertFalse(Files.exists(saved.resolve("charmap/local_MyWorld")), "single-player waypoints stay with their world");
        assertFalse(Files.exists(saved.resolve("config/chartoolbox/toolbox.properties")), "a profile's own mode is not carried");
    }

    @Test
    void aSecondSaveKeepsExactlyOneBackup() throws IOException {
        store.save(a, "0.3", 1L);
        write(a.gameDir().resolve("options.txt"), "fov:0.7\n");
        store.save(a, "0.3", 2L);
        assertEquals(List.of("fov:0.5", "key_key.attack:key.mouse.left", "key_key.charmap.world_map:key.keyboard.n"), Files.readAllLines(root.resolve("saved-before/options.txt")));
        write(a.gameDir().resolve("options.txt"), "fov:0.9\n");
        store.save(a, "0.3", 3L);
        assertEquals(List.of("fov:0.7"), Files.readAllLines(root.resolve("saved-before/options.txt")));
        assertEquals(List.of("fov:0.9"), Files.readAllLines(root.resolve("saved/options.txt")));
        assertEquals(3L, store.info().orElseThrow().savedAt());
    }

    @Test
    void aSuitcaseWithoutItsInfoCountsAsEmpty() throws IOException {
        assertTrue(store.info().isEmpty());
        write(root.resolve("saved/options.txt"), "fov:0.5\n");
        assertTrue(store.info().isEmpty());
        write(root.resolve("saved/info.properties"), "savedAt=yesterday\nprofileName=A\n");
        assertTrue(store.info().isEmpty());
        assertEquals(List.of("Nothing is saved yet."), store.bring(b, EnumSet.allOf(Group.class), k -> true));
    }

    @Test
    void knowsTheProfileItCameFrom() throws IOException {
        store.save(a, "0.3", 1L);
        assertTrue(store.fromProfile(a));
        assertFalse(store.fromProfile(b));
    }

    @Test
    void bringWithOnlyServersTouchesNothingElse() throws IOException {
        store.save(a, "0.3", 1L);
        store.bring(b, Set.of(Group.SERVERS), k -> true);
        assertEquals("servers of A", read(b.gameDir().resolve("servers.dat")));
        assertEquals("version:9999\nfov:0.0\n", read(b.gameDir().resolve("options.txt")));
        assertFalse(Files.exists(b.configDir().resolve("charmap.properties")));
        assertFalse(Files.exists(b.gameDir().resolve("charmap")));
    }

    @Test
    void keepsACopyOfWhatItReplaces() throws IOException {
        store.save(a, "0.3", 1L);
        write(b.gameDir().resolve("servers.dat"), "old");
        store.bring(b, Set.of(Group.SERVERS), k -> true);
        assertEquals("old", read(b.gameDir().resolve("servers.dat.before-toolbox")));
        store.bring(b, Set.of(Group.SERVERS), k -> true);
        assertEquals("old", read(b.gameDir().resolve("servers.dat.before-toolbox")), "the first copy is the one worth keeping");
    }

    @Test
    void existingWaypointsStay() throws IOException {
        write(a.gameDir().resolve("charmap/server_other.example/waypoints.txt"), "Farm 4 5 6");
        store.save(a, "0.3", 1L);
        write(b.gameDir().resolve("charmap/server_play.example/waypoints.txt"), "mine");
        store.bring(b, Set.of(Group.WAYPOINTS), k -> true);
        assertEquals("mine", read(b.gameDir().resolve("charmap/server_play.example/waypoints.txt")));
        assertEquals("Farm 4 5 6", read(b.gameDir().resolve("charmap/server_other.example/waypoints.txt")));
    }

    @Test
    void bringingDoesNotChangeTheSuitcase() throws IOException {
        store.save(a, "0.3", 1L);
        String before = read(root.resolve("saved/options.txt"));
        store.bring(b, Set.of(Group.KEYS), k -> false);
        assertEquals(before, read(root.resolve("saved/options.txt")));
        assertEquals("version:9999\nfov:0.0\n", read(b.gameDir().resolve("options.txt")), "no key was known, so nothing changed");
    }

    @Test
    void keysAndOptionsAreLaidOverTheProfilesOwn() throws IOException {
        store.save(a, "0.3", 1L);
        List<String> said = store.bring(b, Set.of(Group.KEYS, Group.OPTIONS), "key.attack"::equals);
        assertEquals(List.of("version:9999", "fov:0.5", "key_key.attack:key.mouse.left"), Files.readAllLines(b.gameDir().resolve("options.txt")));
        assertEquals("version:9999\nfov:0.0\n", read(b.gameDir().resolve("options.txt.before-toolbox")));
        assertFalse(said.isEmpty());
    }

    @Test
    void theToolboxGroupBringsSwitchesAndPartSettings() throws IOException {
        store.save(a, "0.3", 1L);
        store.bring(b, Set.of(Group.TOOLBOX), k -> true);
        assertEquals("fortressboxes=false\n", read(b.configDir().resolve("chartoolbox/parts.properties")));
        assertEquals("showMinimap=false\n", read(b.configDir().resolve("charmap.properties")));
    }

    @Test
    void theNewPartsSettingsTravelWithTheSwitches() throws IOException {
        write(a.configDir().resolve("chartoolbox/settings.properties"), "zoom.start=6\n");
        store.save(a, "0.5", 1L);
        store.bring(b, Set.of(Group.TOOLBOX), k -> true);
        assertEquals("zoom.start=6\n", read(b.configDir().resolve("chartoolbox/settings.properties")));
    }

    @Test
    void deleteRemovesTheSaveAndItsBackup() throws IOException {
        store.save(a, "0.3", 1L);
        store.save(a, "0.3", 2L);
        store.delete();
        assertTrue(store.info().isEmpty());
        assertFalse(Files.exists(root.resolve("saved")));
        assertFalse(Files.exists(root.resolve("saved-before")));
    }
}
