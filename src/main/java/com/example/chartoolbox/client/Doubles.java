package com.example.chartoolbox.client;

import com.example.chartoolbox.model.ModsFolder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.util.Set;

/**
 * Spots a mod the Toolbox carries that is ALSO in the mods folder as a file of its own. Forge then runs that separate
 * file and leaves the copy inside the Toolbox alone, so the row tells the player they can delete it.
 */
public final class Doubles {
    private Doubles() {}

    private static volatile Set<String> ids;

    /** Reads the mods folder once, off the main thread (it opens every jar there). */
    static void scan() {
        Thread t = new Thread(() -> ids = ModsFolder.topLevelIds(FMLPaths.MODSDIR.get()), "Toolbox mods folder scan");
        t.setDaemon(true);
        t.start();
    }

    /** True when the mods folder has its own jar of this mod. False until the folder has been read. */
    public static boolean separateCopy(String modId) {
        Set<String> now = ids;
        return now != null && now.contains(modId);
    }

    /** For the self-check: has the folder been read yet? */
    static boolean ready() {
        return ids != null;
    }
}
