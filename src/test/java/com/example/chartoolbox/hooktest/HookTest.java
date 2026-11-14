package com.example.chartoolbox.hooktest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests the hook every carried mod gets. The ToolboxSwitch beside this file is written by
// tools/install_hook.py src/test/java --package com.example.chartoolbox.hooktest --id hooktest --same-job somemapmod
class HookTest {
    @Test
    void withoutTheToolboxItIsAlwaysOn() {
        assertTrue(ToolboxSwitch.decide(false, "false", () -> "false", () -> true));
    }

    @Test
    void theLiveValueBeatsTheFile() {
        assertFalse(ToolboxSwitch.decide(true, "false", () -> "true", () -> false));
    }

    @Test
    void theFileIsUsedBeforeAnythingIsLive() {
        assertFalse(ToolboxSwitch.decide(true, null, () -> " FALSE ", () -> false));
    }

    @Test
    void nothingAnywhereMeansOn() {
        assertTrue(ToolboxSwitch.decide(true, null, () -> null, () -> false));
    }

    // Forge builds mods side by side, so this mod may look before the Toolbox has written its file: on a first run it
    // has to reach the same answer by itself.
    @Test
    void aSameJobModStartsItOffOnAFirstRun() {
        assertFalse(ToolboxSwitch.decide(true, null, () -> null, () -> true));
    }

    @Test
    void aLineInTheFileBeatsTheSameJobRule() {
        assertTrue(ToolboxSwitch.decide(true, null, () -> "true", () -> true));
    }

    @Test
    void outsideTheGameItIsOn() {
        assertTrue(ToolboxSwitch.on());
        assertTrue(ToolboxSwitch.onAtStart());
    }
}
