package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.settings.Settings;
import com.example.chartoolbox.model.small.Wear;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gear watch: a small rounded strip left of the hotbar with your armour and held tool and a thin wear bar under each,
 * shown when something is getting low (or always, or never). One soft meow and a pink pulse when an item drops under a
 * tenth, once per item. The tool guard: with 3 uses left a tool stops breaking blocks and hitting, so it doesn't snap
 * (sneak to use it anyway); by default only for enchanted tools.
 */
public final class GearWatch {
    private GearWatch() {}

    public static final String PART = "gearwatch";
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
    private static final Wear.Meows MEOWS = new Wear.Meows();
    private static final Map<EquipmentSlot, Long> PULSE = new HashMap<>();
    private static long lastSaid;
    /** How many meows this game (for the self-check). */
    static int meows;

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(GearWatch::onTick);
        MinecraftForge.EVENT_BUS.addListener(GearWatch::onClick);
    }

    static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("gearwatch", (gui, g, partial, w, h) -> draw(g, w, h));
    }

    static boolean on() {
        return Switches.isOn(PART);
    }

    /** Things that wear out and are worth watching in this slot; armour slots only count armour and elytras. */
    private static boolean watched(EquipmentSlot slot, ItemStack s) {
        if (s.isEmpty() || !s.isDamageableItem()) return false;
        return slot != EquipmentSlot.MAINHAND || !(s.getItem() instanceof ArmorItem);
    }

    private record Shown(EquipmentSlot slot, ItemStack stack, float left) {}

    private static List<Shown> shown(LocalPlayer p) {
        List<Shown> out = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = p.getItemBySlot(slot);
            if (watched(slot, s)) out.add(new Shown(slot, s, Wear.fraction(s.getDamageValue(), s.getMaxDamage())));
        }
        return out;
    }

    /** The lowest wear among what is shown (1 = nothing worn), for the self-check. */
    static float lowest() {
        LocalPlayer p = Minecraft.getInstance().player;
        float low = 1f;
        if (p != null) for (Shown s : shown(p)) low = Math.min(low, s.left());
        return low;
    }

    static boolean stripShowing() {
        LocalPlayer p = Minecraft.getInstance().player;
        return on() && p != null && !shown(p).isEmpty() && Wear.stripShows(Wear.Show.of(PartSettings.choice(Settings.GEAR_SHOW)), lowest());
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!on() || p == null) return;
        for (Shown s : shown(p)) {
            String key = s.slot().getName() + "|" + BuiltInRegistries.ITEM.getKey(s.stack().getItem());
            if (!MEOWS.check(key, s.left())) continue;
            PULSE.put(s.slot(), Util.getMillis());
            meows++;
            com.mojang.logging.LogUtils.getLogger().info("[Toolbox] gear watch: {} is nearly worn out", key);
            if (PartSettings.on(Settings.GEAR_MEOW)) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CAT_AMBIENT, 1.2f, 0.5f));
        }
    }

    /** Is this held thing one the guard looks after? Tools and weapons: not armour, elytras or shields. */
    private static boolean tool(ItemStack s) {
        return s.isDamageableItem() && !(s.getItem() instanceof ArmorItem) && !(s.getItem() instanceof ElytraItem) && !(s.getItem() instanceof ShieldItem);
    }

    /** True when the guard would stop this click (for the self-check, and the click itself). */
    static boolean guards(LocalPlayer p) {
        ItemStack s = p.getMainHandItem();
        if (!on() || !tool(s) || p.isShiftKeyDown()) return false;
        return Wear.guarded(Wear.Guard.of(PartSettings.choice(Settings.GEAR_GUARD)), s.getDamageValue(), s.getMaxDamage(), s.isEnchanted());
    }

    private static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (!e.isAttack() || p == null || !guards(p)) return;
        e.setCanceled(true);
        e.setSwingHand(false);
        long now = Util.getMillis();
        if (now - lastSaid > 2000) {
            lastSaid = now;
            ItemStack s = p.getMainHandItem();
            int left = s.getMaxDamage() - s.getDamageValue();
            p.displayClientMessage(Component.literal("Your " + s.getHoverName().getString() + " has " + left + (left == 1 ? " use" : " uses")
                    + " left, so it stopped. Sneak to use it anyway.").withStyle(st -> st.withColor(Look.PINK_SOFT & 0xFFFFFF)), true);
        }
    }

    private static int wearColour(float left) {
        return left < Wear.VERY_LOW ? 0xFFFF5A6E : left < Wear.LOW ? 0xFFFFC85A : 0xFF8FE3A5;
    }

    private static void draw(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!on() || p == null || mc.options.hideGui || p.isSpectator()) return;
        List<Shown> items = shown(p);
        if (items.isEmpty() || !Wear.stripShows(Wear.Show.of(PartSettings.choice(Settings.GEAR_SHOW)), lowest())) return;
        float sw = items.size() * 18 + 6, sh = 24;
        // left of the hotbar, and further left when the off-hand slot shows there
        float x = w / 2f - 91 - 8 - sw - (p.getOffhandItem().isEmpty() ? 0 : 29), y = h - sh - 1;
        long now = Util.getMillis();
        g.drawManaged(() -> {
            Soft.round(g, x, y, x + sw, y + sh, 9, 0xC024152B, 0xC01C1022);
            for (int i = 0; i < items.size(); i++) {
                Shown s = items.get(i);
                float ix = x + 3 + i * 18;
                long since = now - PULSE.getOrDefault(s.slot(), 0L);
                if (since < 1500) {
                    float a = 1f - since / 1500f;
                    Soft.round(g, ix - 1, y + 1, ix + 19, y + sh - 1, 7, Soft.alpha(Look.PINK, 0.55f * a));
                }
                Soft.round(g, ix + 1, y + 18.5f, ix + 17, y + 21.5f, 1.5f, 0x40FFFFFF);
                Soft.round(g, ix + 1, y + 18.5f, ix + 1 + 16 * Math.max(0.12f, s.left()), y + 21.5f, 1.5f, wearColour(s.left()));
            }
        });
        for (int i = 0; i < items.size(); i++) g.renderItem(items.get(i).stack(), (int) (x + 4 + i * 18), (int) y + 2);
    }
}
