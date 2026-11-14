package com.example.chartoolbox;

import com.example.chartoolbox.model.Defaults;
import com.example.chartoolbox.model.Part;
import com.example.chartoolbox.model.SwitchFile;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Which parts are on: kept in memory, in the switch file, and announced to the carried mods through system properties
 * (chartoolbox.part.&lt;id&gt; = the switch right now; a carried mod's own hook sets chartoolbox.started.&lt;id&gt; = what it
 * went by when the game started). Free of the game, so it can be tested.
 */
public final class SwitchStore {
    private static final Logger LOG = LogUtils.getLogger();
    static final String LIVE = "chartoolbox.part.", STARTED = "chartoolbox.started.";

    private final Path file;
    private final List<Part> parts;
    private final Predicate<String> modLoaded;
    private final Map<String, Boolean> values = new LinkedHashMap<>();
    private final Map<String, Boolean> atLoad = new HashMap<>();
    private boolean firstRun, warned;

    public SwitchStore(Path file, List<Part> parts, Predicate<String> modLoaded) {
        this.file = file;
        this.parts = parts;
        this.modLoaded = modLoaded;
    }

    /** Reads the file. No file yet = the first run in this profile: the defaults are worked out and written. */
    public synchronized void load() {
        values.clear();
        Map<String, Boolean> defaults = Defaults.firstRun(parts, modLoaded);
        boolean write = false;
        if (Files.isRegularFile(file)) {
            try {
                values.putAll(SwitchFile.parse(Files.readString(file)));
            } catch (IOException e) {
                LOG.warn("[Toolbox] Couldn't read {}: every part counts as on for now", file, e);
            }
        } else {
            firstRun = true;
            write = true;
        }
        // a part this profile's file has no line for yet (first run, or a part a newer Toolbox added) gets its default
        for (Part p : parts) {
            if (values.putIfAbsent(p.id(), defaults.get(p.id())) == null) write = true;
        }
        atLoad.clear();
        atLoad.putAll(values);
        values.forEach((id, on) -> System.setProperty(LIVE + id, String.valueOf(on)));
        if (write) save();
    }

    /** The file was swapped for another (Bring my settings): take its values over at once. What each part started with stays. */
    public synchronized void reload() {
        if (!Files.isRegularFile(file)) return;
        try {
            Map<String, Boolean> read = SwitchFile.parse(Files.readString(file));
            Map<String, Boolean> defaults = Defaults.firstRun(parts, modLoaded);
            values.clear();
            values.putAll(read);
            for (Part p : parts) values.putIfAbsent(p.id(), defaults.get(p.id()));
            values.forEach((id, on) -> System.setProperty(LIVE + id, String.valueOf(on)));
        } catch (IOException e) {
            LOG.warn("[Toolbox] Couldn't read {} again: the switches stay as they were", file, e);
        }
    }

    public synchronized boolean firstRun() {
        return firstRun;
    }

    public synchronized boolean isOn(String id) {
        return SwitchFile.on(values, id);
    }

    /**
     * What the part went by when the game started. For a carried mod that comes from its own hook; null means no copy
     * that knows about switches reported in (an older separate copy is the one running).
     */
    public synchronized Boolean onAtStart(String id) {
        for (Part p : parts) {
            if (!p.id().equals(id) || p.kind() != Part.Kind.CARRIED) continue;
            String started = System.getProperty(STARTED + id);
            return started == null ? null : Boolean.valueOf(started);
        }
        return atLoad.getOrDefault(id, Boolean.TRUE);
    }

    /** Flips a switch: at once in memory and for the carried mod, then in the file. A file that can't be written never stops it. */
    public synchronized void set(String id, boolean on) {
        values.put(id, on);
        System.setProperty(LIVE + id, String.valueOf(on));
        save();
    }

    private void save() {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            // written beside the file and swapped in, so a mod reading its line at the same moment never sees half a file
            Path fresh = file.resolveSibling(file.getFileName() + ".new");
            Files.writeString(fresh, SwitchFile.format(values));
            try {
                Files.move(fresh, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(fresh, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            if (!warned) LOG.warn("[Toolbox] Couldn't save {}: switches work until the game closes, but won't be remembered", file, e);
            warned = true;
        }
    }
}
