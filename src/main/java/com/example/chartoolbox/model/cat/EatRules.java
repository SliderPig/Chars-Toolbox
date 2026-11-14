package com.example.chartoolbox.model.cat;

import java.util.List;
import java.util.Set;

/**
 * Auto Eat's rules: when hunger is at or below a level, eat the most filling plain food
 * you carry. Only the game's own foods, never bad food, golden apples or another mod's food (a senzu bean is not lunch).
 */
public final class EatRules {
    private EatRules() {}

    /** A food in the inventory: slot 0-8 is the hotbar. */
    public record Food(int slot, String id, int nutrition, float saturation) {}

    static final Set<String> NEVER = Set.of("minecraft:rotten_flesh", "minecraft:spider_eye", "minecraft:poisonous_potato", "minecraft:pufferfish",
            "minecraft:suspicious_stew", "minecraft:chorus_fruit", "minecraft:chicken", "minecraft:golden_apple", "minecraft:enchanted_golden_apple");

    /** The slot of the best food to eat, or -1 when there is nothing allowed. */
    public static int best(List<Food> foods) {
        int best = -1;
        float bestScore = 0;
        for (Food f : foods) {
            if (!f.id().startsWith("minecraft:") || NEVER.contains(f.id())) continue;
            float score = f.nutrition() + f.saturation() * f.nutrition() * 2;
            if (f.slot() < 9) score += 0.5f; // already in the hotbar: nothing to swap
            if (score > bestScore) {
                bestScore = score;
                best = f.slot();
            }
        }
        return best;
    }

    public static boolean shouldEat(int food, int below, boolean creative, boolean usingItem, boolean screenOpen) {
        return food <= below && !creative && !usingItem && !screenOpen;
    }
}
