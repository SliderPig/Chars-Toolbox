package com.example.chartoolbox.model.cat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cat corner's four helpers: the rules, without the game. */
class CatCornerTest {
    // ---- Auto Eat

    static EatRules.Food food(int slot, String id, int nutrition, float saturation) {
        return new EatRules.Food(slot, id, nutrition, saturation);
    }

    @Test
    void eatsTheMostFillingPlainFood() {
        List<EatRules.Food> bag = List.of(food(3, "minecraft:bread", 5, 0.6f), food(12, "minecraft:cooked_beef", 8, 0.8f), food(1, "minecraft:apple", 4, 0.3f));
        assertEquals(12, EatRules.best(bag));
    }

    @Test
    void neverBadFoodGoldenApplesOrModdedFood() {
        assertEquals(-1, EatRules.best(List.of(food(0, "minecraft:rotten_flesh", 4, 0.1f), food(1, "minecraft:golden_apple", 4, 1.2f),
                food(2, "minecraft:enchanted_golden_apple", 4, 1.2f), food(3, "minecraft:spider_eye", 2, 0.8f), food(4, "minecraft:pufferfish", 1, 0.1f),
                food(5, "minecraft:chorus_fruit", 4, 0.3f), food(6, "minecraft:suspicious_stew", 6, 0.6f), food(7, "minecraft:chicken", 2, 0.3f),
                food(8, "minecraft:poisonous_potato", 2, 0.3f), food(9, "dragonminez:senzu_bean", 20, 1f))));
    }

    @Test
    void aTieGoesToTheHotbar() {
        assertEquals(2, EatRules.best(List.of(food(20, "minecraft:bread", 5, 0.6f), food(2, "minecraft:bread", 5, 0.6f))));
    }

    @Test
    void eatsOnlyWhenHungryEnoughAndFree() {
        assertTrue(EatRules.shouldEat(14, 14, false, false, false));
        assertFalse(EatRules.shouldEat(15, 14, false, false, false));
        assertFalse(EatRules.shouldEat(5, 14, true, false, false));   // creative
        assertFalse(EatRules.shouldEat(5, 14, false, true, false));   // already using something
        assertFalse(EatRules.shouldEat(5, 14, false, false, true));   // a screen is open
    }

    // ---- Auto Rejoin

    @Test
    void rejoinWaitsLongerEachTryUpToAMinute() {
        assertEquals(10_000, RejoinRules.delayMs(0));
        assertEquals(20_000, RejoinRules.delayMs(1));
        assertEquals(60_000, RejoinRules.delayMs(5));
        assertEquals(60_000, RejoinRules.delayMs(30));
    }

    @Test
    void rejoinGivesUpAfterItsTries() {
        assertTrue(RejoinRules.tryAgain(0));
        assertTrue(RejoinRules.tryAgain(19));
        assertFalse(RejoinRules.tryAgain(20));
    }

    @Test
    void theCommandAfterRejoiningIsTidied() {
        assertEquals("chunky continue", RejoinRules.command("/chunky continue"));
        assertEquals("chunky continue", RejoinRules.command("  chunky continue "));
        assertNull(RejoinRules.command(""));
        assertNull(RejoinRules.command(" / "));
    }

    // ---- Fullbright

    @Test
    void fullbrightKeepsYourOwnBrightnessSafe() {
        Brightness b = new Brightness();
        assertEquals(Brightness.BRIGHT, b.next(true, 0.4), 0.0001);
        assertEquals(0.4, b.saved(), 0.0001);
        assertTrue(Double.isNaN(b.next(true, Brightness.BRIGHT)));   // already bright: nothing to write
        assertEquals(0.4, b.next(false, Brightness.BRIGHT), 0.0001);  // off: back to exactly yours
        assertTrue(Double.isNaN(b.next(false, 0.4)));
    }

    @Test
    void aBrightnessLeftOverFromACrashIsPutBack() {
        assertEquals(1.0, new Brightness().next(false, 15.0), 0.0001);
    }

    // ---- Sleepy Cat

    @Test
    void theSleepyCatsPictureIsWhole() {
        for (String row : SleepyArt.BODY) assertEquals(SleepyArt.W, row.length());
        assertEquals(8, SleepyArt.H);
        assertEquals(0.0, SleepyArt.tailSwing(0, 1234), 0.0001);   // the root of the tail never moves
        assertTrue(Math.abs(SleepyArt.tailSwing(8, 1000)) <= 8 * 0.55 + 0.0001);
    }

    @Test
    void theCatStaysOnTheScreen() {
        assertEquals(List.of(4, 12), SleepyArt.clampSpot(-50, 2, 1, 400, 300));
        assertEquals(List.of(400 - SleepyArt.W * 2 - 4, 300 - 9 * 2 - 2), SleepyArt.clampSpot(9999, 9999, 2, 400, 300));
    }
}
