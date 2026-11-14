package com.example.chartoolbox.model.small;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Zoom, Little HUD, Pickup list, Gear watch and Memory watch: the sums and rules, without the game. */
class SmallPartsTest {
    // ---- Zoom

    @Test
    void scrollingZoomsInAndOutWithinLimits() {
        assertEquals(5f, ZoomMath.scrolled(4f, 1), 0.001f);
        assertEquals(3.2f, ZoomMath.scrolled(4f, -1), 0.001f);
        assertEquals(12f, ZoomMath.scrolled(11f, 3), 0.001f);
        assertEquals(2f, ZoomMath.scrolled(2.2f, -2), 0.001f);
    }

    @Test
    void zoomGlidesWithoutOvershooting() {
        float z = 1f;
        for (int i = 0; i < 60; i++) {
            z = ZoomMath.step(z, 4f, 16f);
            assertTrue(z <= 4f);
        }
        assertEquals(4f, z, 0.02f);
        assertEquals(1f, ZoomMath.step(1f, 4f, 0f), 0.0001f);
        assertEquals(1f, ZoomMath.step(1.0005f, 1f, 16f), 0.0001f);
    }

    @Test
    void theViewNarrowsAndTheMouseSlowsToMatch() {
        assertEquals(17.5, ZoomMath.fov(70, 4f), 0.001);
        assertEquals(12.5f, ZoomMath.turned(10f, 20f, 4f), 0.001f);
        // a big jump is the server turning you (a teleport), not the mouse: left alone
        assertEquals(200f, ZoomMath.turned(10f, 200f, 4f), 0.001f);
    }

    // ---- Little HUD

    @Test
    void facingInPlainWords() {
        assertEquals("South (+Z)", HudWords.facing(0f));
        assertEquals("West (-X)", HudWords.facing(90f));
        assertEquals("North (-Z)", HudWords.facing(180f));
        assertEquals("North (-Z)", HudWords.facing(-180f));
        assertEquals("East (+X)", HudWords.facing(-90f));
        assertEquals("East (+X)", HudWords.facing(270f));
        assertEquals("South (+Z)", HudWords.facing(44f));
        assertEquals("West (-X)", HudWords.facing(46f));
        assertEquals("South (+Z)", HudWords.facing(720f));
    }

    @Test
    void theGamesDayAndTime() {
        assertEquals("Day 1, 6:00", HudWords.dayTime(0));
        assertEquals("Day 1, 0:00", HudWords.dayTime(18000));
        assertEquals("Day 2, 12:00", HudWords.dayTime(30000));
        assertEquals("Day 1, 6:30", HudWords.dayTime(500));
    }

    @Test
    void realClock() {
        assertEquals("9:41 pm", HudWords.clock(LocalTime.of(21, 41), true));
        assertEquals("12:05 am", HudWords.clock(LocalTime.of(0, 5), true));
        assertEquals("12:00 pm", HudWords.clock(LocalTime.of(12, 0), true));
        assertEquals("21:41", HudWords.clock(LocalTime.of(21, 41), false));
    }

    @Test
    void coordinatesAndTheOtherSide() {
        assertEquals("120, 64, -300", HudWords.coords(120.7, 64.2, -299.5));
        assertEquals("Nether 100, -8", HudWords.otherSide(800, -64, false));
        assertEquals("Overworld 800, -64", HudWords.otherSide(100, -8, true));
    }

    @Test
    void biomeNamesWithoutTheGame() {
        assertEquals("Dark forest", HudWords.prettyId("minecraft:dark_forest"));
        assertEquals("Soul sand valley", HudWords.prettyId("soul_sand_valley"));
    }

    // ---- Pickup list

    @Test
    void onlyGainsCount() {
        assertEquals(Map.of("iron", 3, "dirt", 1), Pickups.gains(Map.of("iron", 2, "stone", 9), Map.of("iron", 5, "dirt", 1, "stone", 4)));
        assertTrue(Pickups.gains(Map.of("iron", 5), Map.of("iron", 5)).isEmpty());
    }

    @Test
    void repeatsMergeAndOldOnesFade() {
        Pickups log = new Pickups();
        log.add("iron", "Iron Ore", 3, 0);
        log.add("iron", "Iron Ore", 4, 1000);
        List<Pickups.Row> rows = log.visible(1500);
        assertEquals(1, rows.size());
        assertEquals("+7 Iron Ore", rows.get(0).words());
        assertEquals(1f, rows.get(0).alpha(), 0.001f);
        assertTrue(log.visible(3900).get(0).alpha() < 1f);
        assertTrue(log.visible(4100).isEmpty());
    }

    @Test
    void atMostFiveNewestFirst() {
        Pickups log = new Pickups();
        for (int i = 0; i < 7; i++) log.add("item" + i, "Item " + i, 1, i * 10L);
        List<Pickups.Row> rows = log.visible(100);
        assertEquals(5, rows.size());
        assertEquals("+1 Item 6", rows.get(0).words());
        assertEquals("+1 Item 2", rows.get(4).words());
    }

    // ---- Gear watch

    @Test
    void wearAndWhenTheStripShows() {
        assertEquals(0.5f, Wear.fraction(50, 100), 0.001f);
        assertEquals(1f, Wear.fraction(0, 0), 0.001f);
        assertTrue(Wear.stripShows(Wear.Show.WHEN_LOW, 0.2f));
        assertFalse(Wear.stripShows(Wear.Show.WHEN_LOW, 0.3f));
        assertTrue(Wear.stripShows(Wear.Show.ALWAYS, 1f));
        assertFalse(Wear.stripShows(Wear.Show.NEVER, 0.01f));
    }

    @Test
    void theGuardStopsATool3UsesFromBreaking() {
        assertTrue(Wear.guarded(Wear.Guard.EVERY_TOOL, 97, 100, false));
        assertFalse(Wear.guarded(Wear.Guard.EVERY_TOOL, 96, 100, false));
        assertFalse(Wear.guarded(Wear.Guard.ENCHANTED, 99, 100, false));
        assertTrue(Wear.guarded(Wear.Guard.ENCHANTED, 99, 100, true));
        assertFalse(Wear.guarded(Wear.Guard.OFF, 99, 100, true));
        assertFalse(Wear.guarded(Wear.Guard.EVERY_TOOL, 0, 0, true));
    }

    @Test
    void oneMeowPerItemUntilItIsRepaired() {
        Wear.Meows m = new Wear.Meows();
        assertFalse(m.check("head|minecraft:iron_helmet", 0.5f));
        assertTrue(m.check("head|minecraft:iron_helmet", 0.09f));
        assertFalse(m.check("head|minecraft:iron_helmet", 0.05f));
        assertFalse(m.check("head|minecraft:iron_helmet", 0.5f));
        assertTrue(m.check("head|minecraft:iron_helmet", 0.08f));
        assertTrue(m.check("hand|minecraft:diamond_pickaxe", 0.02f));
    }

    // ---- Memory watch

    static final long GB = 1L << 30, MB = 1L << 20;

    @Test
    void gameMemoryNearlyFullForThirtySeconds() {
        MemoryRules r = new MemoryRules();
        long t = 0;
        for (; t < 30_000; t += 5000) assertNull(r.sample(t, 3.7 * GB, 4 * GB, 4 * GB, 16 * GB, false));
        assertEquals(MemoryRules.Warn.GAME, r.sample(t, 3.7 * GB, 4 * GB, 4 * GB, 16 * GB, false));
    }

    @Test
    void aDipStartsTheWaitAgain() {
        MemoryRules r = new MemoryRules();
        r.sample(0, 3.7 * GB, 4 * GB, 4 * GB, 16 * GB, false);
        r.sample(20_000, 3.7 * GB, 4 * GB, 4 * GB, 16 * GB, false);
        r.sample(25_000, 2 * GB, 4 * GB, 4 * GB, 16 * GB, false);
        assertNull(r.sample(35_000, 3.7 * GB, 4 * GB, 4 * GB, 16 * GB, false));
    }

    @Test
    void thePcNearlyOutOfMemory() {
        MemoryRules r = new MemoryRules();
        assertNull(r.sample(0, GB, 4 * GB, 400 * MB, 16 * GB, false));
        assertEquals(MemoryRules.Warn.PC, r.sample(5000, GB, 4 * GB, 400 * MB, 16 * GB, false));
        MemoryRules s = new MemoryRules();
        s.sample(0, GB, 4 * GB, 600 * MB, 8 * GB, false);
        assertEquals(MemoryRules.Warn.PC, s.sample(5000, GB, 4 * GB, 600 * MB, 8 * GB, false));   // 7% of 8 GB
        MemoryRules fine = new MemoryRules();
        fine.sample(0, GB, 4 * GB, 2 * GB, 8 * GB, false);
        assertNull(fine.sample(5000, GB, 4 * GB, 2 * GB, 8 * GB, false));
    }

    @Test
    void atMostOnceEveryTenMinutesAndNeverWhileLoading() {
        MemoryRules r = new MemoryRules();
        r.sample(0, GB, 4 * GB, 100 * MB, 8 * GB, false);
        assertEquals(MemoryRules.Warn.PC, r.sample(5000, GB, 4 * GB, 100 * MB, 8 * GB, false));
        for (long t = 10_000; t < 605_000; t += 5000) assertNull(r.sample(t, GB, 4 * GB, 100 * MB, 8 * GB, false), "at " + t);
        assertEquals(MemoryRules.Warn.PC, r.sample(610_000, GB, 4 * GB, 100 * MB, 8 * GB, false));
        MemoryRules loading = new MemoryRules();
        loading.sample(0, GB, 4 * GB, 100 * MB, 8 * GB, true);
        assertNull(loading.sample(5000, GB, 4 * GB, 100 * MB, 8 * GB, true));
    }

    @Test
    void thePauseScreenLine() {
        assertEquals("Memory: 1.8 of 4.0 GB used, PC 2.1 GB free", MemoryRules.line(1.8 * GB, 4 * GB, 2.1 * GB));
        assertEquals("Memory: 1.8 of 4.0 GB used", MemoryRules.line(1.8 * GB, 4 * GB, -1));
    }
}
