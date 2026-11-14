package com.example.chartoolbox.client;

import com.example.chartoolbox.model.settings.Setting;
import com.example.chartoolbox.model.settings.SettingValues;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The settings of the parts written in the Toolbox: config/chartoolbox/settings.properties (rules in
 * model/settings/SettingValues). Carried to a new profile by Bring my settings, with the switches.
 */
public final class PartSettings {
    private PartSettings() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static SettingValues values;

    static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("chartoolbox").resolve("settings.properties");
    }

    private static synchronized SettingValues values() {
        if (values == null) {
            try {
                values = Files.isRegularFile(file()) ? SettingValues.parse(Files.readString(file())) : SettingValues.parse("");
            } catch (Exception e) {
                LOG.warn("[Toolbox] Couldn't read {}: the parts use their defaults", file(), e);
                values = SettingValues.parse("");
            }
        }
        return values;
    }

    /** The file was swapped for another one (Bring my settings): read it again next time. */
    static synchronized void reload() {
        values = null;
    }

    public static boolean on(Setting s) {
        return values().on(s);
    }

    public static int number(Setting s) {
        return values().number(s);
    }

    public static String choice(Setting s) {
        return values().choice(s);
    }

    public static String text(Setting s) {
        return values().text(s);
    }

    static synchronized void set(Setting s, String value) {
        values().set(s, value);
        save();
    }

    static synchronized void flip(Setting s) {
        values().flip(s);
        save();
    }

    static synchronized void next(Setting s) {
        values().next(s);
        save();
    }

    static synchronized void nudge(Setting s, int by) {
        values().nudge(s, by);
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), values().format());
        } catch (Exception e) {
            LOG.warn("[Toolbox] Couldn't save {}", file(), e);
        }
    }
}
