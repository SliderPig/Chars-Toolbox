package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.suitcase.Group;
import com.example.chartoolbox.model.suitcase.SuitcasePlace;
import com.example.chartoolbox.model.suitcase.SuitcaseRules;
import com.example.chartoolbox.model.suitcase.SuitcaseRules.Mode;
import com.example.chartoolbox.model.suitcase.SuitcaseStore;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The part "Bring my settings": this profile's keys, options, server list, switches and part settings are saved into a
 * suitcase outside every profile, and a new profile is asked once whether to bring them over. Files are only copied on
 * this PC; nothing is sent anywhere. The rules and the file work are in model/suitcase (tested there); this is the
 * game's side of it.
 */
public final class Suitcase {
    private Suitcase() {}

    public static final String PART = "suitcase";
    private static final Logger LOG = LogUtils.getLogger();
    private static SuitcaseStore store;
    private static Mode mode;

    static void init() {
        // chartoolbox.suitcase = another folder to use: the self-checks set it, so they never touch a real suitcase
        store = new SuitcaseStore(SuitcasePlace.root(System.getProperty("chartoolbox.suitcase"), System.getProperty("os.name"), System.getenv(), System.getProperty("user.home")));
        mode = Mode.read(ToolboxProps.get("suitcase"));
        if (mode == null) setMode(SuitcaseRules.initial(store.info().isPresent(), store.fromProfile(profile())));
        MinecraftForge.EVENT_BUS.addListener(Suitcase::onShutdown);
    }

    private static SuitcaseStore.Profile profile() {
        return new SuitcaseStore.Profile(FMLPaths.GAMEDIR.get(), FMLPaths.CONFIGDIR.get());
    }

    private static String version() {
        return ModList.get().getModContainerById("chartoolbox").map(c -> c.getModInfo().getVersion().toString()).orElse("?");
    }

    static boolean on() {
        return Switches.isOn(PART);
    }

    static SuitcaseStore store() {
        return store;
    }

    static Optional<SuitcaseStore.Info> info() {
        return store.info();
    }

    /** Is what's saved from this very profile? */
    static boolean ours() {
        return store.fromProfile(profile());
    }

    static Mode mode() {
        return mode;
    }

    static void setMode(Mode m) {
        mode = m;
        ToolboxProps.set("suitcase", m.word());
    }

    /** Should the title screen ask "bring your settings over?" */
    static boolean shouldAsk() {
        return SuitcaseRules.shouldAsk(mode, on(), store.info().isPresent(), ours());
    }

    /** Saves this profile's settings now, and from now on every time the game closes. Returns what to tell the player. */
    static String saveNow() {
        try {
            Minecraft.getInstance().options.save();
            store.save(profile(), version(), System.currentTimeMillis());
            setMode(Mode.SHARE);
            return "Saved.";
        } catch (IOException | RuntimeException e) {
            LOG.warn("[Toolbox] Couldn't save the settings to {}", store.root(), e);
            return "Couldn't save: " + e.getMessage();
        }
    }

    /** Brings the ticked groups into this profile and makes the game use them. Returns what was done, in plain words. */
    static List<String> bring(Set<Group> groups) {
        Minecraft mc = Minecraft.getInstance();
        try {
            mc.options.save(); // so the file holds what the game has now, and only the brought lines change
            Set<String> known = new HashSet<>();
            for (KeyMapping key : mc.options.keyMappings) known.add(key.getName());
            List<String> said = new ArrayList<>(store.bring(profile(), groups, known::contains));
            if (groups.contains(Group.KEYS) || groups.contains(Group.OPTIONS)) {
                mc.options.load();
                KeyMapping.resetMapping();
                mc.resizeDisplay();
            }
            if (groups.contains(Group.TOOLBOX)) {
                Switches.reload();
                PartSettings.reload();
            }
            setMode(SuitcaseRules.afterBring());
            return said;
        } catch (IOException | RuntimeException e) {
            LOG.warn("[Toolbox] Couldn't bring the settings from {}", store.root(), e);
            return List.of("Couldn't bring your settings: " + e.getMessage());
        }
    }

    static String delete() {
        try {
            store.delete();
            return "Deleted.";
        } catch (IOException | RuntimeException e) {
            LOG.warn("[Toolbox] Couldn't delete {}", store.root(), e);
            return "Couldn't delete: " + e.getMessage();
        }
    }

    private static void onShutdown(GameShuttingDownEvent e) {
        if (!SuitcaseRules.savesOnClose(mode, on())) return;
        try {
            Minecraft.getInstance().options.save();
            store.save(profile(), version(), System.currentTimeMillis());
        } catch (IOException | RuntimeException x) {
            LOG.warn("[Toolbox] Couldn't save the settings to {} on the way out", store.root(), x);
        }
    }
}
