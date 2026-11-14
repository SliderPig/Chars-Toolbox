package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.settings.Settings;
import com.example.chartoolbox.model.small.ZoomMath;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

/**
 * Zoom: hold H (the spec said C, but C is Soul Release's flash step and the game's own "save hotbar") to glide in to
 * 4x; the mouse wheel goes from 2x to 12x while it is held (the hotbar doesn't scroll), and the mouse turns slower to
 * match, so aiming feels the same. Nothing saved is touched: the turn is scaled each frame, not the sensitivity setting.
 * Off at start-up = no key at all.
 */
public final class Zoom {
    private Zoom() {}

    public static final String PART = "zoom";
    static final KeyMapping KEY = new KeyMapping("key.chartoolbox.zoom", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H, "key.categories.chartoolbox");

    private static boolean registered, held;
    private static float target = 1f, current = 1f, lastYaw, lastPitch;
    private static long lastFrame;
    /** The self-check holds the zoom by itself (it can't hold a real key down). */
    static boolean heldByTest;

    static void init(IEventBus modBus) {
        modBus.addListener(Zoom::onKeys);
        MinecraftForge.EVENT_BUS.addListener(Zoom::onTick);
        MinecraftForge.EVENT_BUS.addListener(Zoom::onScroll);
        MinecraftForge.EVENT_BUS.addListener(Zoom::onFrame);
        MinecraftForge.EVENT_BUS.addListener(Zoom::onFov);
    }

    static boolean on() {
        return Switches.isOn(PART);
    }

    private static void onKeys(RegisterKeyMappingsEvent e) {
        if (!Boolean.TRUE.equals(Switches.onAtStart(PART))) return;
        e.register(KEY);
        registered = true;
    }

    /** Where it is gliding to (for the self-check). */
    static float target() {
        return target;
    }

    /** How far in it is right now (1 = not zoomed). */
    static float current() {
        return current;
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        boolean now = on() && mc.player != null && mc.screen == null && (heldByTest || registered && KEY.isDown());
        if (now && !held) target = PartSettings.number(Settings.ZOOM_START);
        held = now;
    }

    private static void onScroll(InputEvent.MouseScrollingEvent e) {
        if (!held) return;
        target = ZoomMath.scrolled(target, e.getScrollDelta());
        e.setCanceled(true);
    }

    /** Runs each frame right after the mouse turned the view: glides the zoom and slows that turn to match it. */
    private static void onFrame(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        long t = Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(100, t - lastFrame);
        lastFrame = t;
        current = ZoomMath.step(current, held ? target : 1f, dt);
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        if (current > 1.01f) {
            float yaw = ZoomMath.turned(lastYaw, p.getYRot(), current);
            float pitch = Mth.clamp(ZoomMath.turned(lastPitch, p.getXRot(), current), -90f, 90f);
            p.yRotO += yaw - p.getYRot();
            p.xRotO += pitch - p.getXRot();
            p.setYRot(yaw);
            p.setXRot(pitch);
        }
        lastYaw = p.getYRot();
        lastPitch = p.getXRot();
    }

    private static void onFov(ViewportEvent.ComputeFov e) {
        if (current > 1.001f && e.usedConfiguredFov()) e.setFOV(ZoomMath.fov(e.getFOV(), current));
    }
}
