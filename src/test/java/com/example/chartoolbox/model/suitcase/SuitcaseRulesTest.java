package com.example.chartoolbox.model.suitcase;

import com.example.chartoolbox.model.suitcase.SuitcaseRules.Mode;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuitcaseRulesTest {
    @Test
    void aNewProfileSharesUnlessSomeoneElseAlreadySaved() {
        assertEquals(Mode.SHARE, SuitcaseRules.initial(false, false));
        assertEquals(Mode.SHARE, SuitcaseRules.initial(true, true));
        assertEquals(Mode.ASK, SuitcaseRules.initial(true, false));
    }

    @Test
    void theProfileThatSavedIsNotAsked() {
        assertFalse(SuitcaseRules.shouldAsk(Mode.ASK, true, true, true));
        assertFalse(SuitcaseRules.shouldAsk(Mode.SHARE, true, true, false));
        assertTrue(SuitcaseRules.shouldAsk(Mode.ASK, true, true, false));
    }

    @Test
    void nothingIsAskedWithoutASuitcaseOrWithThePartOff() {
        assertFalse(SuitcaseRules.shouldAsk(Mode.ASK, true, false, false));
        assertFalse(SuitcaseRules.shouldAsk(Mode.ASK, false, true, false));
        assertFalse(SuitcaseRules.shouldAsk(Mode.APART, true, true, false));
    }

    // A brand-new profile holds nothing worth keeping: closing it must not overwrite a good suitcase.
    @Test
    void aProfileThatWasAskedDoesNotSaveUntilItAnswers() {
        assertFalse(SuitcaseRules.savesOnClose(Mode.ASK, true));
        assertEquals(Mode.ASK, SuitcaseRules.afterNotNow());
        assertTrue(SuitcaseRules.savesOnClose(SuitcaseRules.afterBring(), true));
    }

    @Test
    void keptApartNeverSaves() {
        assertEquals(Mode.APART, SuitcaseRules.afterDontAsk());
        assertFalse(SuitcaseRules.savesOnClose(Mode.APART, true));
        assertFalse(SuitcaseRules.savesOnClose(Mode.SHARE, false));
    }

    @Test
    void modesAreReadForgivingly() {
        assertEquals(Mode.APART, Mode.read(" Apart "));
        assertEquals(null, Mode.read("nonsense"));
        assertEquals(null, Mode.read(null));
        assertEquals("share", Mode.SHARE.word());
    }

    @Test
    void theSuitcaseLivesWhereEachSystemKeepsSuchThings() {
        assertEquals(Path.of("X:/test"), SuitcasePlace.root("X:/test", "Windows 11", Map.of(), "C:/Users/me"));
        assertEquals(Path.of("C:/Users/me/AppData/Roaming", "CharToolbox"),
                SuitcasePlace.root(null, "Windows 11", Map.of("APPDATA", "C:/Users/me/AppData/Roaming"), "C:/Users/me"));
        assertEquals(Path.of("C:/Users/me", "AppData", "Roaming", "CharToolbox"), SuitcasePlace.root("", "Windows 10", Map.of(), "C:/Users/me"));
        assertEquals(Path.of("/Users/me", "Library", "Application Support", "CharToolbox"), SuitcasePlace.root(null, "Mac OS X", Map.of(), "/Users/me"));
        assertEquals(Path.of("/home/me/.cfg", "chartoolbox"), SuitcasePlace.root(null, "Linux", Map.of("XDG_CONFIG_HOME", "/home/me/.cfg"), "/home/me"));
        assertEquals(Path.of("/home/me", ".config", "chartoolbox"), SuitcasePlace.root(null, "Linux", Map.of(), "/home/me"));
    }
}
