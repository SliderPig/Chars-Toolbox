package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.small.Pickups;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pickup list: "+3 Iron Ore" fades in beside the hotbar. Worked out on the PC from the inventory's counts going up
 * while no screen is open (so moving things around in a chest never counts), which works on any server. Not in
 * creative, where things come from the menu.
 */
public final class PickupList {
    private PickupList() {}

    public static final String PART = "pickuplist";
    private static final Pickups LOG = new Pickups();
    private static final Map<String, ItemStack> PICTURES = new HashMap<>();
    private static Map<String, Integer> before;
    private static int playerId = -1;

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(PickupList::onTick);
    }

    static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("pickuplist", (gui, g, partial, w, h) -> draw(g, w, h));
    }

    static boolean on() {
        return Switches.isOn(PART);
    }

    /** The rows showing right now (for the self-check). */
    static List<Pickups.Row> rows() {
        return LOG.visible(Util.getMillis());
    }

    private static String key(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        return s.hasTag() ? id + "#" + s.getTag().hashCode() : id;
    }

    private static Map<String, Integer> counts(Inventory inv) {
        Map<String, Integer> out = new HashMap<>();
        for (List<ItemStack> part : List.of(inv.items, inv.armor, inv.offhand)) {
            for (ItemStack s : part) {
                if (s.isEmpty()) continue;
                String k = key(s);
                out.merge(k, s.getCount(), Integer::sum);
                PICTURES.computeIfAbsent(k, x -> s.copyWithCount(1));
            }
        }
        return out;
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        // anything else starts counting again from what is there now, without showing a row
        if (!on() || p == null || mc.screen != null || p.isCreative() || p.isSpectator() || p.getId() != playerId) {
            before = null;
            playerId = p == null ? -1 : p.getId();
            if (p == null) {
                LOG.clear();
                PICTURES.clear();
            }
            return;
        }
        Map<String, Integer> now = counts(p.getInventory());
        if (before != null) {
            long t = Util.getMillis();
            Pickups.gains(before, now).forEach((k, n) -> {
                ItemStack pic = PICTURES.get(k);
                LOG.add(k, pic == null ? k : pic.getHoverName().getString(), n, t);
            });
        }
        before = now;
        if (PICTURES.size() > 500) PICTURES.clear();
    }

    private static void draw(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!on() || mc.options.hideGui || mc.player == null) return;
        List<Pickups.Row> rows = LOG.visible(Util.getMillis());
        if (rows.isEmpty()) return;
        Font font = mc.font;
        // right of the hotbar, past where the attack indicator can sit, going up
        float x = w / 2f + 91 + 26;
        g.drawManaged(() -> {
            for (int i = 0; i < rows.size(); i++) {
                Pickups.Row r = rows.get(i);
                float y = h - 20 - i * 19, rw = font.width(r.words()) + 30;
                Soft.round(g, x, y, x + rw, y + 17, 8.5f, Soft.alpha(0xC024152B, r.alpha()), Soft.alpha(0xC01C1022, r.alpha()));
                int a = Math.max(5, (int) (255 * r.alpha())) << 24;
                g.drawString(font, r.words(), (int) x + 22, (int) y + 5, (Look.TEXT & 0xFFFFFF) | a, false);
            }
        });
        for (int i = 0; i < rows.size(); i++) {
            Pickups.Row r = rows.get(i);
            ItemStack pic = PICTURES.get(r.key());
            if (pic == null || r.alpha() < 0.35f) continue;
            float y = h - 20 - i * 19;
            g.pose().pushPose();
            g.pose().translate(x + 3, y + 1.5f, 0);
            g.pose().scale(0.875f, 0.875f, 1f);
            g.renderItem(pic, 0, 0);
            g.pose().popPose();
        }
    }
}
