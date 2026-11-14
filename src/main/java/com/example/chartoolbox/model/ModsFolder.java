package com.example.chartoolbox.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Reads which mods the jar files in a mods folder are, to spot a separate copy of a mod the Toolbox carries. */
public final class ModsFolder {
    private ModsFolder() {}

    private static final Pattern MOD_ID = Pattern.compile("^modId\\s*=\\s*[\"']([^\"']+)[\"']");

    /** The mod ids a jar declares in META-INF/mods.toml (only its [[mods]] blocks, not its dependency lists). */
    public static Set<String> modIds(Path jar) throws IOException {
        Set<String> ids = new HashSet<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("META-INF/mods.toml");
            if (entry == null) return ids;
            String text;
            try (InputStream in = zip.getInputStream(entry)) {
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            boolean inMods = false;
            for (String raw : text.split("\\R")) {
                String line = raw.trim();
                if (line.startsWith("[")) {
                    inMods = line.startsWith("[[mods]]");
                    continue;
                }
                Matcher m = MOD_ID.matcher(line);
                if (inMods && m.find()) ids.add(m.group(1));
            }
        }
        return ids;
    }

    /** Every mod id declared by the jar files directly in this folder. A jar that can't be read is skipped. */
    public static Set<String> topLevelIds(Path modsDir) {
        Set<String> ids = new HashSet<>();
        if (!Files.isDirectory(modsDir)) return ids;
        try (Stream<Path> files = Files.list(modsDir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                if (!Files.isRegularFile(file) || !file.getFileName().toString().endsWith(".jar")) continue;
                try {
                    ids.addAll(modIds(file));
                } catch (IOException | RuntimeException skipped) {
                    // not a readable jar: it can't be a second copy of anything
                }
            }
        } catch (IOException e) {
            // no list, no doubles to report
        }
        return ids;
    }
}
