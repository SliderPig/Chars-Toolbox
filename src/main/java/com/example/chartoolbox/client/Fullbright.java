package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.cat.Brightness;
import com.example.chartoolbox.model.settings.Settings;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Fullbright (Cat corner): sets the game's Brightness far past the slider's 100% so dark places look like day. Purely
 * on your own screen. ' (apostrophe) flips it. Your own brightness is kept and put back exactly. The bright value is
 * never saved: while a screen is open (where options get saved) and when the game closes, your own value is back.
 */
public final class Fullbright {
    private Fullbright() {}

    static final KeyMapping KEY = new KeyMapping("key.chartoolbox.fullbright", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_APOSTROPHE, "key.categories.chartoolbox");

    private static final Brightness RULE = new Brightness();
    private static boolean registered, lookedUp;
    private static Field valueField;

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(Fullbright::onTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, Fullbright::onShutdown);
    }

    static void onKeys(RegisterKeyMappingsEvent e) {
        if (!Boolean.TRUE.equals(Switches.onAtStart(CatCorner.PART))) return;
        e.register(KEY);
        registered = true;
    }

    static boolean wanted() {
        return CatCorner.on() && PartSettings.on(Settings.CAT_FULLBRIGHT);
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (registered && KEY.consumeClick()) {
            if (mc.player == null || mc.screen != null || !CatCorner.on()) continue;
            PartSettings.flip(Settings.CAT_FULLBRIGHT);
            mc.player.displayClientMessage(Component.literal("Fullbright " + (wanted() ? "on" : "off")).withStyle(s -> s.withColor(Look.PINK_SOFT & 0xFFFFFF)), true);
        }
        // a screen that can save the options is open (anything but chat): your own brightness, so that is what gets saved
        boolean bright = wanted() && mc.player != null && (mc.screen == null || mc.screen instanceof ChatScreen);
        apply(mc, bright);
    }

    private static boolean checkedLeftover;

    private static void apply(Minecraft mc, boolean bright) {
        // nothing of ours to put back: leave the brightness alone (another mod may have its own fullbright), except once
        // at the start, when a brightness left over from a game that closed while bright goes back to 100%
        if (!bright && Double.isNaN(RULE.saved()) && checkedLeftover) return;
        checkedLeftover = true;
        OptionInstance<Double> gamma = mc.options.gamma();
        double write = RULE.next(bright, gamma.get());
        if (!Double.isNaN(write)) write(gamma, write);
    }

    private static void onShutdown(GameShuttingDownEvent e) {
        apply(Minecraft.getInstance(), false);
    }

    /** The game's brightness right now (for the self-check). */
    static double now() {
        return Minecraft.getInstance().options.gamma().get();
    }

    /**
     * Writes the option's value straight in, past the slider's 0-100% check. The field is found by its shape (the one
     * Object field that isn't final or static), so it works whatever its name is in the released game.
     */
    private static void write(OptionInstance<Double> option, double value) {
        try {
            if (!lookedUp) {
                lookedUp = true;
                for (Field f : OptionInstance.class.getDeclaredFields()) {
                    int mod = f.getModifiers();
                    if (f.getType() == Object.class && !Modifier.isFinal(mod) && !Modifier.isStatic(mod)) {
                        f.setAccessible(true);
                        valueField = f;
                        break;
                    }
                }
            }
            if (valueField != null) valueField.set(option, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Fullbright just won't work
        }
    }
}
