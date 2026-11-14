package com.example.chartoolbox;

import com.example.chartoolbox.model.Parts;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

/** The game's one switch store: config/chartoolbox/parts.properties, every part, and the mods that are really installed. */
public final class Switches {
    private Switches() {}

    private static SwitchStore store;

    /** Called once, when the Toolbox itself is constructed. */
    public static synchronized void load() {
        store = new SwitchStore(file(), Parts.all(), id -> ModList.get().isLoaded(id));
        store.load();
    }

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("chartoolbox").resolve("parts.properties");
    }

    /** The switch file was swapped for another one: read it again. */
    public static void reload() {
        store.reload();
    }

    public static boolean firstRun() {
        return store.firstRun();
    }

    public static boolean isOn(String id) {
        return store.isOn(id);
    }

    public static Boolean onAtStart(String id) {
        return store.onAtStart(id);
    }

    public static void set(String id, boolean on) {
        store.set(id, on);
    }
}
