package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.settings.Settings;
import com.example.chartoolbox.model.small.MemoryRules;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.lang.management.ManagementFactory;

/**
 * Memory watch: every 5 seconds it looks at the game's own memory and the PC's free memory (rules in
 * model/small/MemoryRules) and says so once, with a small cat notice, when either is nearly gone. At most once every
 * 10 minutes, never while loading or in the first half minute of a world. A small memory line on the pause screen.
 */
public final class MemoryWatch {
    private MemoryWatch() {}

    public static final String PART = "memorywatch";
    private static final MemoryRules RULES = new MemoryRules();
    private static int ticks, inWorld;

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(MemoryWatch::onTick);
        MinecraftForge.EVENT_BUS.addListener(MemoryWatch::onPause);
    }

    static boolean on() {
        return Switches.isOn(PART);
    }

    /** The PC's free and total memory in bytes, or -1 when Java can't tell. */
    static double[] pc() {
        try {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
                return new double[] {os.getFreeMemorySize(), os.getTotalMemorySize()};
            }
        } catch (Throwable t) {
            // not this Java
        }
        return new double[] {-1, -1};
    }

    static double gameUsed() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    static double gameMax() {
        return Runtime.getRuntime().maxMemory();
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        inWorld = mc.player == null ? 0 : inWorld + 1;
        if (!on() || ++ticks % 100 != 0) return;
        boolean loading = mc.player == null || mc.getOverlay() != null || inWorld < 600;
        double[] pc = pc();
        MemoryRules.Warn warn = RULES.sample(Util.getMillis(), gameUsed(), gameMax(), pc[0], pc[1], loading);
        if (warn != null) say(warn);
    }

    static void say(MemoryRules.Warn warn) {
        Notice.show(PART, warn == MemoryRules.Warn.GAME ? "The game's memory is nearly full" : "Your PC is low on memory", MemoryRules.words(warn), false);
    }

    /** The line on the pause screen, or "" when it is switched off. */
    static String pauseLine() {
        if (!on() || !PartSettings.on(Settings.MEM_PAUSE)) return "";
        return MemoryRules.line(gameUsed(), gameMax(), pc()[0]);
    }

    private static void onPause(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof PauseScreen screen)) return;
        String line = pauseLine();
        if (line.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        e.getGuiGraphics().drawString(mc.font, line, 6, screen.height - 14, Look.TEXT_DIM, true);
    }
}
