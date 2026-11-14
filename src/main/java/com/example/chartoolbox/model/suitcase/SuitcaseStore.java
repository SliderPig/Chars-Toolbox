package com.example.chartoolbox.model.suitcase;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The suitcase on disk: one saved set of settings (and the one before it, as a backup), outside every profile.
 * Files are only ever copied between a profile's folder and this one. No game classes, so it is tested with temp folders.
 *
 * <pre>
 * root/saved/info.properties                      when, and which profile
 * root/saved/options.txt                          the carried lines of the game's options
 * root/saved/servers.dat                          the server list
 * root/saved/config/chartoolbox/parts.properties  the Toolbox's switches
 * root/saved/config/char*.properties              each part's own settings
 * root/saved/charmap/server_x/waypoints.txt       the Map's waypoints on servers
 * root/saved-before/                              the save before this one
 * </pre>
 */
public final class SuitcaseStore {
    public record Info(long savedAt, String profileName, String profilePath, String toolboxVersion) {}

    /** A profile's two folders: the game folder and its config folder. */
    public record Profile(Path gameDir, Path configDir) {}

    private static final String SAVED = "saved", BEFORE = "saved-before", SAVING = "saving", KEPT = ".before-toolbox";
    /** In the config folder. A profile's own toolbox.properties (its suitcase mode) is deliberately not here. */
    private static final List<String> CONFIG_FILES = List.of("chartoolbox/parts.properties", "chartoolbox/settings.properties",
            "charmap.properties", "charlod.properties", "charmenu.properties");

    private final Path root;

    public SuitcaseStore(Path root) {
        this.root = root;
    }

    public Path root() {
        return root;
    }

    /** What is saved, or empty when nothing usable is (no suitcase yet, or one whose info can't be read). */
    public Optional<Info> info() {
        Path file = root.resolve(SAVED).resolve("info.properties");
        if (!Files.isRegularFile(file)) return Optional.empty();
        try (Reader r = Files.newBufferedReader(file)) {
            Properties p = new Properties();
            p.load(r);
            String name = p.getProperty("profileName"), path = p.getProperty("profilePath", "");
            if (name == null) return Optional.empty();
            return Optional.of(new Info(Long.parseLong(p.getProperty("savedAt", "").trim()), name, path, p.getProperty("toolboxVersion", "")));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Was the suitcase last saved by this very profile? */
    public boolean fromProfile(Profile p) {
        return info().map(i -> i.profilePath().equals(where(p))).orElse(false);
    }

    private static String where(Profile p) {
        return p.gameDir().toAbsolutePath().normalize().toString();
    }

    /** Saves this profile's settings. The save that was there becomes the one backup. */
    public void save(Profile from, String toolboxVersion, long now) throws IOException {
        Path fresh = root.resolve(SAVING);
        deleteTree(fresh);
        Files.createDirectories(fresh);

        Path options = from.gameDir().resolve("options.txt");
        if (Files.isRegularFile(options)) Files.write(fresh.resolve("options.txt"), OptionsFilter.carried(Files.readAllLines(options)));
        copyIfThere(from.gameDir().resolve("servers.dat"), fresh.resolve("servers.dat"));
        for (String f : CONFIG_FILES) copyIfThere(from.configDir().resolve(f), fresh.resolve("config").resolve(f));
        for (Path world : serverWorlds(from.gameDir().resolve("charmap"))) {
            copyIfThere(world.resolve("waypoints.txt"), fresh.resolve("charmap").resolve(world.getFileName().toString()).resolve("waypoints.txt"));
        }

        Properties info = new Properties();
        info.setProperty("savedAt", Long.toString(now));
        info.setProperty("profileName", String.valueOf(from.gameDir().toAbsolutePath().normalize().getFileName()));
        info.setProperty("profilePath", where(from));
        info.setProperty("toolboxVersion", toolboxVersion);
        try (Writer w = Files.newBufferedWriter(fresh.resolve("info.properties"))) {
            info.store(w, "Char's Toolbox: saved settings. Safe to delete; nothing here is ever sent anywhere.");
        }

        Path saved = root.resolve(SAVED), before = root.resolve(BEFORE);
        deleteTree(before);
        if (Files.exists(saved)) Files.move(saved, before);
        Files.move(fresh, saved);
    }

    /**
     * Brings the ticked groups into a profile. A file that gets replaced is first copied beside itself as
     * name.before-toolbox (the first such copy is kept). The suitcase itself is not changed.
     *
     * @param keyKnown does this profile have a key binding of that name?
     * @return what was done, in plain words
     */
    public List<String> bring(Profile to, Set<Group> groups, Predicate<String> keyKnown) throws IOException {
        if (info().isEmpty()) return List.of("Nothing is saved yet.");
        Path saved = root.resolve(SAVED);
        List<String> said = new ArrayList<>();

        boolean keys = groups.contains(Group.KEYS), options = groups.contains(Group.OPTIONS);
        Path savedOptions = saved.resolve("options.txt");
        if ((keys || options) && Files.isRegularFile(savedOptions)) {
            Path file = to.gameDir().resolve("options.txt");
            List<String> target = Files.isRegularFile(file) ? Files.readAllLines(file) : List.of();
            List<String> merged = OptionsFilter.merge(target, Files.readAllLines(savedOptions), keys, options, keyKnown);
            if (!merged.equals(target)) {
                keepCopy(file);
                Files.createDirectories(file.getParent());
                Files.write(file, merged);
            }
            if (keys) said.add("Your keys are set.");
            if (options) said.add("Your game options are set.");
        }

        if (groups.contains(Group.SERVERS) && Files.isRegularFile(saved.resolve("servers.dat"))) {
            replace(saved.resolve("servers.dat"), to.gameDir().resolve("servers.dat"));
            said.add("Your server list is here.");
        }

        if (groups.contains(Group.TOOLBOX)) {
            int n = 0;
            for (String f : CONFIG_FILES) {
                Path source = saved.resolve("config").resolve(f);
                if (!Files.isRegularFile(source)) continue;
                replace(source, to.configDir().resolve(f));
                n++;
            }
            if (n > 0) said.add("Your Toolbox switches and part settings are here.");
        }

        if (groups.contains(Group.WAYPOINTS)) {
            int n = 0;
            for (Path world : serverWorlds(saved.resolve("charmap"))) {
                Path source = world.resolve("waypoints.txt");
                Path target = to.gameDir().resolve("charmap").resolve(world.getFileName().toString()).resolve("waypoints.txt");
                if (!Files.isRegularFile(source) || Files.exists(target)) continue; // waypoints this profile already has stay
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
                n++;
            }
            if (n > 0) said.add(n == 1 ? "Map waypoints for 1 server are here." : "Map waypoints for " + n + " servers are here.");
        }

        if (said.isEmpty()) said.add("There was nothing to bring for what you ticked.");
        return said;
    }

    /** Throws the saved settings and their backup away. */
    public void delete() throws IOException {
        deleteTree(root.resolve(SAVED));
        deleteTree(root.resolve(BEFORE));
        deleteTree(root.resolve(SAVING));
    }

    // ---- small file helpers

    /** The Map's folders of servers (server_...). Single-player worlds stay with their own profile. */
    private static List<Path> serverWorlds(Path charmap) throws IOException {
        if (!Files.isDirectory(charmap)) return List.of();
        try (Stream<Path> dirs = Files.list(charmap)) {
            return dirs.filter(d -> Files.isDirectory(d) && d.getFileName().toString().startsWith("server_")).sorted().toList();
        }
    }

    private static void copyIfThere(Path source, Path target) throws IOException {
        if (!Files.isRegularFile(source)) return;
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void keepCopy(Path file) throws IOException {
        Path kept = file.resolveSibling(file.getFileName() + KEPT);
        if (Files.isRegularFile(file) && !Files.exists(kept)) Files.copy(file, kept);
    }

    private static void replace(Path source, Path target) throws IOException {
        keepCopy(target);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> all = Files.walk(dir)) {
            for (Path p : all.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
