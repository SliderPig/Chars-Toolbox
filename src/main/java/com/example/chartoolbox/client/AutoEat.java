package com.example.chartoolbox.client;

import com.example.chartoolbox.model.cat.EatRules;
import com.example.chartoolbox.model.settings.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto Eat (Cat corner): when hunger drops to the set level (14 = 7 drumsticks), eats the most filling plain food you
 * carry, then puts your hand back on what it held. Only the game's own safe foods (rules in model/cat/EatRules); never
 * in creative, while a screen is open, or while you are using something else.
 */
public final class AutoEat {
    private AutoEat() {}

    private static boolean eating;
    private static int prevSlot, startFood, ticks, cooldown;
    /** How many times it has eaten this game (for the self-check). */
    static int meals;

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(AutoEat::onTick);
    }

    static boolean on() {
        return CatCorner.on() && PartSettings.on(Settings.CAT_EAT);
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            eating = false;
            return;
        }
        if (eating) {
            ticks++;
            mc.options.keyUse.setDown(true);
            if (p.getFoodData().getFoodLevel() > startFood || ticks > 60 || mc.screen != null || !on()) finish(mc);
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (!on() || p.isSpectator()) return;
        int food = p.getFoodData().getFoodLevel();
        if (!EatRules.shouldEat(food, PartSettings.number(Settings.EAT_BELOW), p.isCreative(), p.isUsingItem(), mc.screen != null)) return;
        List<EatRules.Food> foods = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack st = p.getInventory().items.get(i);
            if (st.isEmpty() || !st.isEdible()) continue;
            FoodProperties f = st.getItem().getFoodProperties(st, p);
            if (f != null) foods.add(new EatRules.Food(i, BuiltInRegistries.ITEM.getKey(st.getItem()).toString(), f.getNutrition(), f.getSaturationModifier()));
        }
        int slot = EatRules.best(foods);
        if (slot < 0) {
            cooldown = 20 * 30; // nothing to eat: look again in half a minute
            return;
        }
        var inv = p.getInventory();
        prevSlot = inv.selected;
        if (slot >= 9) { // from the bag into the last hotbar slot
            mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, slot, 8, ClickType.SWAP, p);
            slot = 8;
        }
        inv.selected = slot;
        startFood = food;
        ticks = 0;
        eating = true;
        meals++;
        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        mc.options.keyUse.setDown(true);
    }

    private static void finish(Minecraft mc) {
        eating = false;
        mc.options.keyUse.setDown(false);
        if (mc.player != null) mc.player.getInventory().selected = prevSlot;
        cooldown = 20;
    }
}
