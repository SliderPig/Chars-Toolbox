package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.settings.Settings;
import net.minecraft.client.gui.screens.Screen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Cat corner: four small helpers, each with its own switch on this part's settings page:
 * Sleepy Cat (on at first), Fullbright, Auto Rejoin and Auto Eat (off at first: they act for you, so you choose them).
 */
public final class CatCorner {
    private CatCorner() {}

    public static final String PART = "catcorner";

    static void init() {
        Fullbright.init();
        AutoRejoin.init();
        AutoEat.init();
        Map<String, Consumer<Screen>> extras = new LinkedHashMap<>();
        extras.put("Place the Sleepy Cat", page -> page.getMinecraft().setScreen(new CatPlaceScreen(page)));
        PartSettingsScreen.EXTRAS.put(PART, extras);
        ToolboxScreen.LIVE.put(PART, CatCorner::rowLine);
    }

    static boolean on() {
        return Switches.isOn(PART);
    }

    /** The row's live line: which helpers are on, when it isn't just the cat. */
    static String rowLine() {
        if (!on()) return "";
        StringBuilder sb = new StringBuilder();
        if (PartSettings.on(Settings.CAT_FULLBRIGHT)) sb.append(sb.isEmpty() ? "" : ", ").append("Fullbright");
        if (PartSettings.on(Settings.CAT_REJOIN)) sb.append(sb.isEmpty() ? "" : ", ").append("Rejoin");
        if (PartSettings.on(Settings.CAT_EAT)) sb.append(sb.isEmpty() ? "" : ", ").append("Eat");
        return sb.isEmpty() ? "" : "Cat + " + sb;
    }
}
