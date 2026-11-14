package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.cat.SleepyArt;
import com.example.chartoolbox.model.settings.Settings;
import com.example.chartoolbox.model.small.HudWords;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

/**
 * Little HUD: one small rounded pill in a corner of your choice with where you are, which way you face, the biome, the
 * game's day and time and the real clock (FPS and the matching Nether / Overworld spot if you want them). Hidden with
 * F1 and while F3 is open; the coordinates hide when the server has turned on reduced debug info. The lines are worked
 * out 4 times a second, not every frame.
 */
public final class LittleHud {
    private LittleHud() {}

    public static final String PART = "littlehud";
    private static List<String> lines = List.of();
    private static int ticks;

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(LittleHud::onTick);
    }

    static void onOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("littlehud", (gui, g, partial, w, h) -> draw(g, w, h));
    }

    static boolean on() {
        return Switches.isOn(PART);
    }

    /** What it shows right now (for the self-check). */
    static List<String> lines() {
        return lines;
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ++ticks % 5 != 0) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (!on() || p == null || mc.level == null) {
            lines = List.of();
            return;
        }
        List<String> out = new ArrayList<>();
        boolean reduced = mc.showOnlyReducedInfo();
        if (PartSettings.on(Settings.HUD_COORDS) && !reduced) out.add(HudWords.coords(p.getX(), p.getY(), p.getZ()));
        if (PartSettings.on(Settings.HUD_OTHER) && !reduced) {
            boolean nether = mc.level.dimension() == Level.NETHER;
            if (nether || mc.level.dimension() == Level.OVERWORLD) out.add(HudWords.otherSide(p.getX(), p.getZ(), nether));
        }
        if (PartSettings.on(Settings.HUD_FACING)) {
            String way = HudWords.facing(p.getYRot());
            out.add("Facing " + Character.toLowerCase(way.charAt(0)) + way.substring(1));
        }
        if (PartSettings.on(Settings.HUD_BIOME)) out.add(biome(mc, p));
        if (PartSettings.on(Settings.HUD_DAY)) out.add(HudWords.dayTime(mc.level.getDayTime()));
        if (PartSettings.on(Settings.HUD_CLOCK)) out.add(HudWords.clock(LocalTime.now(), "12 hour".equals(PartSettings.choice(Settings.HUD_CLOCK_STYLE))));
        if (PartSettings.on(Settings.HUD_FPS)) out.add(mc.getFps() + " fps");
        lines = List.copyOf(out);
    }

    private static String biome(Minecraft mc, LocalPlayer p) {
        return mc.level.getBiome(p.blockPosition()).unwrapKey().map(k -> {
            String key = "biome." + k.location().getNamespace() + "." + k.location().getPath();
            return I18n.exists(key) ? I18n.get(key) : HudWords.prettyId(k.location().toString());
        }).orElse("Somewhere");
    }

    // ---- the Map's minimap: when it is in the same corner, the pill goes below it (or above it at the bottom)

    private static long mapStamp = Long.MIN_VALUE;
    private static int mapCorner = 1, mapSize = 200;
    private static boolean mapShown = true, mapInfo = true;

    private static void readMap() {
        Path f = FMLPaths.CONFIGDIR.get().resolve("charmap.properties");
        try {
            long stamp = Files.isRegularFile(f) ? Files.getLastModifiedTime(f).toMillis() : -1;
            if (stamp == mapStamp) return;
            mapStamp = stamp;
            Properties p = new Properties();
            if (stamp >= 0) try (Reader r = Files.newBufferedReader(f)) {
                p.load(r);
            }
            mapCorner = Integer.parseInt(p.getProperty("corner", "1").trim());
            mapSize = Integer.parseInt(p.getProperty("size", "200").trim());
            mapShown = !"false".equals(p.getProperty("minimap", "true").trim()) && !"false".equals(p.getProperty("enabled", "true").trim());
            mapInfo = !"false".equals(p.getProperty("infoText", "true").trim());
        } catch (Exception e) {
            mapShown = false;
        }
    }

    private static java.lang.reflect.Field boxesShown, boxesHeard;
    private static boolean boxesLooked;

    /** Room for Fortress Boxes' own two lines at the top left while its boxes are shown (read without needing that mod). */
    private static float fortressRoom() {
        if (!ModList.get().isLoaded("fortressboxes") || !Switches.isOn("fortressboxes")) return 0;
        try {
            if (!boxesLooked) {
                boxesLooked = true;
                Class<?> c = Class.forName("com.example.fortressboxes.client.ClientBoxes");
                boxesShown = c.getDeclaredField("shown");
                boxesHeard = c.getDeclaredField("heard");
                boxesShown.setAccessible(true);
                boxesHeard.setAccessible(true);
            }
            if (boxesShown != null && boxesShown.getBoolean(null) && boxesHeard.getBoolean(null)) return 24;
        } catch (ReflectiveOperationException | RuntimeException e) {
            boxesShown = null;
        }
        return 0;
    }

    /** How much room (in GUI units) the Map's minimap takes in this corner: 0 when it isn't there. */
    private static float mapRoom(String corner) {
        if (!ModList.get().isLoaded("charmap") || !Switches.isOn("charmap")) return 0;
        if (ticks % 40 == 0 || mapStamp == Long.MIN_VALUE) readMap();
        int index = (corner.startsWith("bottom") ? 2 : 0) + (corner.endsWith("right") ? 1 : 0);
        if (!mapShown || mapCorner != index) return 0;
        var window = Minecraft.getInstance().getWindow();
        double gs = window.getGuiScale();
        double size = Math.min(mapSize, Math.min(window.getWidth(), window.getHeight()) - 20);
        double text = mapInfo ? 2 * (9 * Math.max(1, Math.round(gs)) + 6) : 0;
        return (float) ((14 + size + text + 8) / gs);
    }

    private static float lastX, lastY;
    private static boolean drawnTopLeft;

    /** Was the pill drawn in the top-left corner this frame? (Sleepy Cat lies on it then.) */
    static boolean drawnTopLeft() {
        return drawnTopLeft;
    }

    static float pillX() {
        return lastX;
    }

    static float pillY() {
        return lastY;
    }

    private static void draw(GuiGraphics g, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        drawnTopLeft = false;
        if (!on() || lines.isEmpty() || mc.options.hideGui || mc.options.renderDebug) return;
        Font font = mc.font;
        int widest = 0;
        for (String s : lines) widest = Math.max(widest, font.width(s));
        float pw = widest + 16, ph = lines.size() * 10 + 8;
        String corner = PartSettings.choice(Settings.HUD_CORNER);
        float x = corner.endsWith("right") ? w - pw - 6 : 6;
        boolean bottom = corner.startsWith("bottom");
        float room = mapRoom(corner);
        // Sleepy Cat sleeps on top of the pill in the top-left corner (unless it was placed somewhere else): make room
        float cat = corner.equals("top left") && SleepyCat.on() && !SleepyCat.placed() ? SleepyArt.H * SleepyCat.scale() + 4 : 0;
        // Fortress Boxes writes up to two lines at the top left while its boxes are shown: the pill goes below them
        float fortress = corner.equals("top left") ? fortressRoom() : 0;
        float y = bottom ? h - ph - (corner.endsWith("right") ? 40 : 46) - room : 6 + room + fortress + cat;
        lastX = x;
        lastY = y;
        drawnTopLeft = corner.equals("top left");
        List<String> shown = lines;
        g.drawManaged(() -> {
            Soft.round(g, x, y, x + pw, y + ph, Math.min(10, ph / 2f), 0xB024152B, 0xB01C1022);
            Soft.ring(g, x, y, x + pw, y + ph, Math.min(10, ph / 2f), 1, 0x40FF8FC8);
            for (int i = 0; i < shown.size(); i++) g.drawString(font, shown.get(i), (int) x + 8, (int) y + 5 + i * 10, i == 0 ? Look.PINK_SOFT : Look.TEXT, false);
        });
    }
}
