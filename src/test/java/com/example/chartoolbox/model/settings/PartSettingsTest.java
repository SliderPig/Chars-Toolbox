package com.example.chartoolbox.model.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartSettingsTest {
    @Test
    void everySettingHasItsDefaultUntilChanged() {
        SettingValues v = SettingValues.parse("");
        assertEquals(4, v.number(Settings.ZOOM_START));
        assertTrue(v.on(Settings.HUD_COORDS));
        assertFalse(v.on(Settings.HUD_FPS));
        assertEquals("when low", v.choice(Settings.GEAR_SHOW));
        assertEquals("", v.text(Settings.REJOIN_COMMAND));
    }

    @Test
    void changesRoundTripThroughTheFile() {
        SettingValues v = SettingValues.parse("");
        v.set(Settings.ZOOM_START, "6");
        v.set(Settings.HUD_FPS, "true");
        v.set(Settings.GEAR_SHOW, "always");
        v.set(Settings.REJOIN_COMMAND, "/chunky continue");
        SettingValues back = SettingValues.parse(v.format());
        assertEquals(6, back.number(Settings.ZOOM_START));
        assertTrue(back.on(Settings.HUD_FPS));
        assertEquals("always", back.choice(Settings.GEAR_SHOW));
        assertEquals("/chunky continue", back.text(Settings.REJOIN_COMMAND));
    }

    @Test
    void badValuesFallBackAndNumbersStayInRange() {
        SettingValues v = SettingValues.parse("zoom.start=lots\nautoeat.below=99\ngear.show=sideways\nsomething.else=kept\n");
        assertEquals(4, v.number(Settings.ZOOM_START));
        assertEquals(Settings.EAT_BELOW.max(), v.number(Settings.EAT_BELOW));
        assertEquals("when low", v.choice(Settings.GEAR_SHOW));
        assertTrue(v.format().contains("something.else=kept"));
    }

    @Test
    void nextChoiceGoesRound() {
        SettingValues v = SettingValues.parse("");
        v.next(Settings.GEAR_SHOW);
        assertEquals("never", v.choice(Settings.GEAR_SHOW));
        v.next(Settings.GEAR_SHOW);
        assertEquals("always", v.choice(Settings.GEAR_SHOW));
    }

    @Test
    void eachPartListsItsOwnSettings() {
        assertTrue(Settings.of("littlehud").contains(Settings.HUD_COORDS));
        assertTrue(Settings.of("zoom").contains(Settings.ZOOM_START));
        assertTrue(Settings.of("nothing").isEmpty());
        for (Setting s : Settings.all()) assertFalse(s.label().isEmpty(), s.key());
    }
}
