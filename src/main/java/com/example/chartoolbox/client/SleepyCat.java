package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.cat.SleepyArt;
import com.example.chartoolbox.model.settings.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;

import java.util.List;

/**
 * Sleepy Cat (Cat corner): Char's little black cat asleep on your screen, curled up with its eyes shut, breathing
 * slowly, z's drifting up and its tail hanging over the edge, swinging. It lies on top of the Little HUD pill when that
 * is in the top-left corner, otherwise in that corner; "Place the Sleepy Cat" drags it anywhere.
 */
public final class SleepyCat {
    private SleepyCat() {}

    static final int FUR = 0xFF0C080E, RIM = 0xFF5A3A5E, EAR = 0xFFFF9CC6, ZZZ = 0xFFFFB3D4;

    static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("sleepycat", (gui, g, partial, w, h) -> drawHud(g, w, h));
    }

    static boolean on() {
        return Switches.isOn(CatCorner.PART) && PartSettings.on(Settings.CAT_SLEEPY);
    }

    static int scale() {
        return PartSettings.number(Settings.SLEEPY_SIZE);
    }

    /** Was it placed by hand? (-1 = no: its spot depends on the Little HUD.) */
    static boolean placed() {
        return PartSettings.number(Settings.SLEEPY_X) >= 0 && PartSettings.number(Settings.SLEEPY_Y) >= 0;
    }

    /** Is it lying on the Little HUD pill right now (so the pill makes room above itself)? */
    static boolean onTheHud() {
        return on() && !placed() && LittleHud.drawnTopLeft();
    }

    /** Where it lies now: {left end, the line it lies on}, in GUI units. */
    static List<Integer> spot(int w, int h) {
        int s = scale();
        if (placed()) return SleepyArt.clampSpot(PartSettings.number(Settings.SLEEPY_X), PartSettings.number(Settings.SLEEPY_Y), s, w, h);
        // on the pill, its tail hanging down past the pill's left edge (not across the words)
        if (onTheHud()) return List.of(Math.max(0, (int) LittleHud.pillX() - 3 * s), (int) LittleHud.pillY());
        return SleepyArt.clampSpot(8, SleepyArt.H * s + 6, s, w, h);
    }

    /** Is it on the screen right now? (for the self-check) */
    static boolean visible() {
        Minecraft mc = Minecraft.getInstance();
        return on() && mc.player != null && !mc.options.hideGui && !(mc.screen instanceof CatPlaceScreen);
    }

    private static void drawHud(GuiGraphics g, int w, int h) {
        if (!visible()) return;
        List<Integer> at = spot(w, h);
        int s = scale();
        g.drawManaged(() -> draw(g, at.get(0), at.get(1), s, 1f));
    }

    /** The cat lying on the line y with its left end at x, each art pixel s GUI units. Draw inside g.drawManaged. */
    static void draw(GuiGraphics g, int x, int y, int s, float alpha) {
        long now = System.currentTimeMillis();
        int top = y - SleepyArt.H * s;
        tail(g, x + s, y - s, s, alpha, now);
        boolean inhale = SleepyArt.inhale(now);
        int rim = Soft.alpha(RIM, alpha);
        // a soft rim first, so the black cat shows on dark ground too
        for (int r = 0; r < SleepyArt.H; r++) {
            for (int c = 0; c < SleepyArt.W; c++) {
                if (SleepyArt.BODY[r].charAt(c) == '.') continue;
                g.fill(x + c * s - 1, top + r * s - 1, x + c * s + s + 1, top + r * s + s + 1, rim);
            }
        }
        if (inhale) g.fill(x + 6 * s - 1, top + 2 * s - 1, x + 13 * s + 1, top + 3 * s + 1, rim);
        for (int r = 0; r < SleepyArt.H; r++) {
            for (int c = 0; c < SleepyArt.W; c++) {
                int colour = switch (SleepyArt.BODY[r].charAt(c)) {
                    case '#' -> FUR;
                    case 'p', 'n' -> EAR;
                    case 'l' -> RIM;
                    default -> 0;
                };
                if (colour == 0) continue;
                g.fill(x + c * s, top + r * s, x + c * s + s, top + r * s + s, Soft.alpha(colour, alpha));
            }
        }
        if (inhale) g.fill(x + 6 * s, top + 2 * s, x + 13 * s, top + 3 * s, Soft.alpha(FUR, alpha));
        zzz(g, x + 20 * s, top + s, s, alpha, now);
    }

    private static void tail(GuiGraphics g, int x, int y, int s, float alpha, long now) {
        int rim = Soft.alpha(RIM, alpha), fur = Soft.alpha(FUR, alpha);
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < SleepyArt.TAIL; i++) {
                int px = x + (int) Math.round(SleepyArt.tailSwing(i, now) * s), py = y + i * s;
                if (pass == 0) g.fill(px - 1, py - 1, px + s + 1, py + s + 1, rim);
                else g.fill(px, py, px + s, py + s, fur);
            }
        }
    }

    private static void zzz(GuiGraphics g, int x, int y, int s, float alpha, long now) {
        Font font = Minecraft.getInstance().font;
        for (int k = 0; k < 3; k++) {
            float phase = ((now + k * 900L) % 2700L) / 2700f;
            float a = alpha * (phase < 0.2f ? phase / 0.2f : 1f - (phase - 0.2f) / 0.8f);
            if (a <= 0.03f) continue;
            float size = 0.45f + 0.25f * phase;
            g.pose().pushPose();
            g.pose().translate(x + phase * 6 * s, y - phase * 10 * s, 0);
            g.pose().scale(size * s, size * s, 1f);
            g.drawString(font, "z", 0, 0, Soft.alpha(ZZZ, a), false);
            g.pose().popPose();
        }
    }
}
