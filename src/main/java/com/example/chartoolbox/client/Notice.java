package com.example.chartoolbox.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import org.slf4j.Logger;

import java.util.List;

/**
 * A small rounded card at the top middle of the screen, with the cat: how the Toolbox's parts say something once
 * (a new key clash, memory running low). It slides in, stays a few seconds and fades. One at a time; hidden with F1.
 */
public final class Notice {
    private Notice() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final long IN = 250, STAY = 7000, OUT = 600;
    private static final int WIDE = 210;

    private record Shown(String part, String title, String text, long at) {}

    private static Shown shown;
    private static String lastTitle = "none";

    static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("notice", (gui, g, partial, w, h) -> draw(g, w));
    }

    /** Shows a card for a part now (in place of one that may be showing); it goes the moment its part is switched off. meow = a quiet cat sound with it. */
    public static void show(String part, String title, String text, boolean meow) {
        shown = new Shown(part, title, text, Util.getMillis());
        lastTitle = title;
        LOG.info("[Toolbox] notice: {} | {}", title, text);
        if (meow) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CAT_AMBIENT, 1.15f, 0.45f));
    }

    /** The title of the last card shown in this game (for the self-check). */
    static String lastTitle() {
        return lastTitle;
    }

    static boolean showing() {
        return shown != null && Util.getMillis() - shown.at() < IN + STAY + OUT && com.example.chartoolbox.Switches.isOn(shown.part());
    }

    private static void draw(GuiGraphics g, int screenW) {
        Minecraft mc = Minecraft.getInstance();
        if (shown == null || mc.options.hideGui) return;
        long age = Util.getMillis() - shown.at();
        if (age > IN + STAY + OUT || !com.example.chartoolbox.Switches.isOn(shown.part())) {
            shown = null;
            return;
        }
        float slide = Soft.easeOut(age / (float) IN), fade = age < IN + STAY ? 1f : 1f - (age - IN - STAY) / (float) OUT;
        Font font = mc.font;
        List<FormattedCharSequence> lines = font.split(Component.literal(shown.text()), WIDE - 40);
        if (lines.size() > 5) lines = lines.subList(0, 5);
        float w = WIDE, h = 22 + lines.size() * 10;
        // top middle: the Map's minimap and the game's own toasts live at the top right, Little HUD at the top left
        float x = (screenW - w) / 2f, y = 6 - (1f - slide) * (h + 12);
        List<FormattedCharSequence> body = lines;
        g.drawManaged(() -> {
            Soft.shadow(g, x, y + 2, x + w, y + h + 2, 10, 8, Soft.alpha(0x70000000, fade), false);
            Soft.round(g, x, y, x + w, y + h, 10, Soft.alpha(Look.PANEL, fade), Soft.alpha(Look.PANEL_LOW, fade));
            Soft.ring(g, x, y, x + w, y + h, 10, 1, Soft.alpha(Look.PINK, 0.55f * fade));
            Soft.disc(g, x + 17, y + 17, 10, Soft.alpha(Look.PINK_SOFT, fade));
            int a = Math.max(5, (int) (255 * fade)) << 24;
            g.drawString(font, Kit.fit(font, shown.title(), w - 40), (int) x + 32, (int) y + 6, (Look.PINK & 0xFFFFFF) | a, false);
            for (int i = 0; i < body.size(); i++) g.drawString(font, body.get(i), (int) x + 32, (int) y + 18 + i * 10, (Look.TEXT & 0xFFFFFF) | a, false);
        });
        RenderSystem.enableBlend();
        g.setColor(1f, 1f, 1f, fade);
        g.blit(ToolboxScreen.CAT, (int) x + 9, (int) y + 9, 0f, 0f, 16, 16, 16, 16);
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
