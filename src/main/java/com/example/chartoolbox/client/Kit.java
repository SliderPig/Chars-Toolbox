package com.example.chartoolbox.client;

import com.example.chartoolbox.model.RowLayout.Rect;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** The rounded pieces the Toolbox's smaller screens are made of: the panel, its title with the cat, pills and switches. */
final class Kit {
    private Kit() {}

    /** A pill-shaped button: its words, where it is, whether it is the one to press, and what it does. */
    record Pill(String label, Rect rect, boolean strong, Runnable action) {}

    static final float PILL_H = 16;

    static Pill pill(Font font, String label, float x, float y, boolean strong, Runnable action) {
        return new Pill(label, new Rect(x, y, font.width(label) + 20, PILL_H), strong, action);
    }

    /** Draw inside g.drawManaged. */
    static void panel(GuiGraphics g, float x, float y, float w, float h) {
        Soft.shadow(g, x, y + 3, x + w, y + h + 3, 14, 10, 0x70000000, false);
        Soft.round(g, x, y, x + w, y + h, 14, Look.PANEL, Look.PANEL_LOW);
        Soft.ring(g, x, y, x + w, y + h, 14, 1, Soft.alpha(Look.PINK, 0.25f));
    }

    /** The pink disc and the title beside it. Draw inside g.drawManaged; the cat itself goes on with {@link #cat}. */
    static void title(GuiGraphics g, Font font, float x, float y, String text) {
        Soft.disc(g, x + 22, y + 17, 11, Look.PINK_SOFT);
        g.drawString(font, text, (int) x + 38, (int) y + 13, Look.TEXT, false);
    }

    /** The cat picture on the title's disc. A texture, so draw it AFTER the managed batch. */
    static void cat(GuiGraphics g, float x, float y) {
        RenderSystem.enableBlend();
        g.blit(ToolboxScreen.CAT, (int) x + 14, (int) y + 9, 0f, 0f, 16, 16, 16, 16);
        RenderSystem.disableBlend();
    }

    static void pill(GuiGraphics g, Font font, Pill p, int mouseX, int mouseY) {
        Rect r = p.rect();
        boolean over = r.has(mouseX, mouseY);
        int top = p.strong() || over ? Look.PINK : 0xFF4A2A55, bottom = p.strong() || over ? Look.PINK_DEEP : 0xFF3B2146;
        if (p.strong() && over) top = Look.PINK_SOFT;
        Soft.round(g, r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), 8, top, bottom);
        g.drawString(font, p.label(), (int) (r.x() + (r.w() - font.width(p.label())) / 2f), (int) r.y() + 4, p.strong() ? Look.KNOB_ON : Look.TEXT, false);
    }

    /** A switch, t from 0 (off) to 1 (on). */
    static void toggle(GuiGraphics g, Rect sw, float t) {
        float e = Soft.ease(t);
        Soft.round(g, sw.x(), sw.y(), sw.x() + sw.w(), sw.y() + sw.h(), 6, Soft.mix(Look.SWITCH_OFF, Look.PINK, e));
        Soft.disc(g, sw.x() + 6 + e * (sw.w() - 12), sw.y() + 6, 4.5f, Soft.mix(Look.KNOB_OFF, Look.KNOB_ON, e));
    }

    static String fit(Font font, String s, float w) {
        if (font.width(s) <= w) return s;
        return font.plainSubstrByWidth(s, (int) Math.max(0, w - font.width("..."))) + "...";
    }

    static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.35f));
    }
}
