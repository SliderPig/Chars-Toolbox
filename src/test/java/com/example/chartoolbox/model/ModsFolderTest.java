package com.example.chartoolbox.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModsFolderTest {
    @TempDir
    Path dir;

    // A jar in the temp folder; modsToml == null means a jar with no mods.toml (a plain library).
    Path jar(String name, String modsToml) throws IOException {
        Path file = dir.resolve(name);
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(modsToml == null ? "some/Class.class" : "META-INF/mods.toml"));
            zip.write((modsToml == null ? "x" : modsToml).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return file;
    }

    @Test
    void readsEveryModIdOfAJar() throws IOException {
        assertEquals(Set.of("fortressboxes"), ModsFolder.modIds(jar("a.jar",
                "[[mods]]\nmodId=\"fortressboxes\"\n[[dependencies.fortressboxes]]\nmodId=\"forge\"\n")));
    }

    @Test
    void aJarWithoutModsTomlHasNone() throws IOException {
        assertTrue(ModsFolder.modIds(jar("lib.jar", null)).isEmpty());
    }

    @Test
    void aBrokenJarIsSkipped() throws Exception {
        Files.writeString(dir.resolve("broken.jar"), "not a zip");
        jar("b.jar", "[[mods]]\nmodId = 'charmap'\n");
        assertEquals(Set.of("charmap"), ModsFolder.topLevelIds(dir));
    }
}
