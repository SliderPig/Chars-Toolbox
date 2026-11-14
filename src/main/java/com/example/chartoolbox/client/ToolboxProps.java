package com.example.chartoolbox.client;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The Toolbox's own small settings for THIS profile: config/chartoolbox/toolbox.properties
 * (pauseButton=false hides the cat on the pause screen; suitcase=share|ask|apart is this profile's way with the suitcase).
 * It is never carried to another profile.
 */
final class ToolboxProps {
    private ToolboxProps() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static Properties loaded;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("chartoolbox").resolve("toolbox.properties");
    }

    private static synchronized Properties all() {
        if (loaded == null) {
            loaded = new Properties();
            if (Files.isRegularFile(file())) {
                try (Reader r = Files.newBufferedReader(file())) {
                    loaded.load(r);
                } catch (Exception e) {
                    LOG.warn("[Toolbox] Couldn't read {}", file(), e);
                }
            }
        }
        return loaded;
    }

    /** The saved value, or null. */
    static synchronized String get(String key) {
        String v = all().getProperty(key);
        return v == null ? null : v.trim();
    }

    static synchronized void set(String key, String value) {
        all().setProperty(key, value);
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file())) {
                all().store(w, "Char's Toolbox: this profile's own settings");
            }
        } catch (Exception e) {
            LOG.warn("[Toolbox] Couldn't save {}", file(), e);
        }
    }
}
