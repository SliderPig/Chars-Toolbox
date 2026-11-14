package com.example.chartoolbox.client;

import com.example.chartoolbox.model.cat.RejoinRules;
import com.example.chartoolbox.model.settings.Settings;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

/**
 * Auto Rejoin (Cat corner): when a server kicks you or the connection drops, the disconnect screen counts down and joins
 * the same server again, waiting longer each try (10 s, 20 s ... a minute; the host may still be waking up), up to 20
 * tries. Once back in it can run a command (empty by default; Char's own is /chunky continue). Back on the disconnect
 * screen stops it; quitting normally never starts it. A rounded on/off pill sits on the disconnect screen.
 */
public final class AutoRejoin {
    private AutoRejoin() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static ServerData lastServer;
    private static int tries;
    private static long rejoinAt, commandAt;
    private static boolean rejoining;

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(AutoRejoin::onLogin);
        MinecraftForge.EVENT_BUS.addListener(AutoRejoin::onOpening);
        MinecraftForge.EVENT_BUS.addListener(AutoRejoin::onInit);
        MinecraftForge.EVENT_BUS.addListener(AutoRejoin::onRender);
        MinecraftForge.EVENT_BUS.addListener(AutoRejoin::onTick);
    }

    static boolean on() {
        return CatCorner.on() && PartSettings.on(Settings.CAT_REJOIN);
    }

    /** For the self-check: the server it would rejoin (a real kick sets this when you join a server). */
    static void rememberForTest(ServerData server) {
        lastServer = server;
    }

    static int tries() {
        return tries;
    }

    static boolean planned() {
        return rejoinAt > 0;
    }

    /** The countdown line, or "" when nothing is planned. */
    static String countdown() {
        if (rejoinAt <= 0 || lastServer == null) return "";
        long left = Math.max(0, (rejoinAt - System.currentTimeMillis() + 999) / 1000);
        return "Joining " + lastServer.name + " again in " + left + " s (try " + (tries + 1) + " of " + RejoinRules.MOST_TRIES + "). Back stops it.";
    }

    private static void plan() {
        rejoinAt = on() && lastServer != null && RejoinRules.tryAgain(tries) ? System.currentTimeMillis() + RejoinRules.delayMs(tries) : 0;
    }

    private static void onLogin(ClientPlayerNetworkEvent.LoggingIn e) {
        ServerData sd = Minecraft.getInstance().getCurrentServer();
        if (sd != null) lastServer = sd;
        if (rejoining) {
            rejoining = false;
            commandAt = System.currentTimeMillis() + PartSettings.number(Settings.REJOIN_DELAY) * 1000L;
        }
        tries = 0;
        rejoinAt = 0;
    }

    private static void onOpening(ScreenEvent.Opening e) {
        if (!(e.getNewScreen() instanceof DisconnectedScreen)) return;
        // the server it was on (or was joining), when the game still knows it; otherwise the last one it got into
        ServerData now = Minecraft.getInstance().getCurrentServer();
        if (now != null) lastServer = now;
        plan();
    }

    private static void onInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof DisconnectedScreen screen) || !CatCorner.on()) return;
        e.addListener(new PillButton(screen.width / 2 - 70, screen.height - 28, 140, () -> "Auto Rejoin: " + (on() ? "on" : "off"), () -> {
            PartSettings.flip(Settings.CAT_REJOIN);
            plan();
        }));
    }

    private static void onRender(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof DisconnectedScreen screen)) return;
        String line = countdown();
        if (line.isEmpty()) return;
        var g = e.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        float w = font.width(line) + 20, x = (screen.width - w) / 2f;
        // on a dark rounded pill, so it reads over any background (Main Menu's cherry trees are pink too)
        g.drawManaged(() -> {
            Soft.round(g, x, 7, x + w, 23, 8, 0xE024152B, 0xE01C1022);
            g.drawString(font, line, (int) x + 10, 11, Look.PINK, false);
        });
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        if (rejoinAt > 0) {
            if (!(mc.screen instanceof DisconnectedScreen) || !on()) {
                rejoinAt = 0; // you left the disconnect screen, or switched it off: stopped
            } else if (now >= rejoinAt) {
                rejoinAt = 0;
                tries++;
                rejoining = true;
                LOG.info("[Toolbox] auto rejoin: joining {} again (try {})", lastServer.name, tries);
                ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(lastServer.ip), lastServer, false);
            }
        }
        if (commandAt > 0 && now >= commandAt && mc.player != null) {
            commandAt = 0;
            String cmd = RejoinRules.command(PartSettings.text(Settings.REJOIN_COMMAND));
            if (cmd != null && on()) mc.player.connection.sendCommand(cmd);
        }
    }
}
