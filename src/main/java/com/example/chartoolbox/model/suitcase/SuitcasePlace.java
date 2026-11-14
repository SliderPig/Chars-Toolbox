package com.example.chartoolbox.model.suitcase;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/** Where the suitcase lives: outside every profile, in the place each system keeps an app's own files. */
public final class SuitcasePlace {
    private SuitcasePlace() {}

    /**
     * @param override a folder to use instead (the system property chartoolbox.suitcase: tests use it so they never
     *                 touch a real suitcase); null or blank for the normal place
     */
    public static Path root(String override, String osName, Map<String, String> env, String userHome) {
        if (override != null && !override.isBlank()) return Path.of(override);
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (os.contains("mac")) return Path.of(userHome, "Library", "Application Support", "CharToolbox");
        if (os.contains("win")) {
            String appData = env.get("APPDATA");
            return appData != null && !appData.isBlank() ? Path.of(appData, "CharToolbox") : Path.of(userHome, "AppData", "Roaming", "CharToolbox");
        }
        String xdg = env.get("XDG_CONFIG_HOME");
        return xdg != null && !xdg.isBlank() ? Path.of(xdg, "chartoolbox") : Path.of(userHome, ".config", "chartoolbox");
    }
}
