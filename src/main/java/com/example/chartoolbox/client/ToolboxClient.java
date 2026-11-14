package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import org.lwjgl.glfw.GLFW;

/** The ways into the Toolbox on a PC: the Y key, /toolbox, the Mods list, the cat on the pause screen, and what the first title screen shows. */
public final class ToolboxClient {
    private ToolboxClient() {}

    private static final KeyMapping OPEN = new KeyMapping("key.chartoolbox.open", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.categories.chartoolbox");

    private static boolean greeted, welcomed;

    public static void init(IEventBus modBus) {
        modBus.addListener(ToolboxClient::onKeys);
        // what the new parts draw on the screen while you play (each draws nothing while its part is off)
        modBus.addListener(GearWatch::onOverlays);
        modBus.addListener(PickupList::onOverlays);
        modBus.addListener(LittleHud::onOverlays);
        modBus.addListener(SleepyCat::onOverlays);
        modBus.addListener(Notice::onOverlays);
        modBus.addListener(Fullbright::onKeys);
        MinecraftForge.EVENT_BUS.addListener(ToolboxClient::onTick);
        MinecraftForge.EVENT_BUS.addListener(ToolboxClient::onCommands);
        MinecraftForge.EVENT_BUS.addListener(ToolboxClient::onScreenInit);
        // the Config button beside the Toolbox in the game's own Mods list
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ToolboxScreen(parent, false)));
        Doubles.scan();
        Suitcase.init();
        KeyClash.init();
        Zoom.init(modBus);
        LittleHud.init();
        PickupList.init();
        GearWatch.init();
        MemoryWatch.init();
        CatCorner.init();
        DevShots.init();
    }

    /** The key the Toolbox opens with, for people ("Y"). */
    static String openKeyName() {
        return OPEN.getTranslatedKeyMessage().getString();
    }

    /** Opens the switch screen over whatever is showing; closing it goes back there. */
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ToolboxScreen(mc.screen, false));
    }

    private static void onKeys(RegisterKeyMappingsEvent e) {
        e.register(OPEN);
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.screen == null) open();
        }
    }

    private static void onCommands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("toolbox").executes(c -> {
            // next tick: the chat screen closes itself right after the command runs
            Minecraft.getInstance().tell(ToolboxClient::open);
            return 1;
        }));
    }

    private static void onScreenInit(ScreenEvent.Init.Post e) {
        Screen screen = e.getScreen();
        if (screen instanceof PauseScreen && !"false".equalsIgnoreCase(ToolboxProps.get("pauseButton"))) {
            e.addListener(new CatButton(screen.width - CatButton.SIZE - 6, 6));
        }
        // The first title screen of a launch. Settings waiting in the suitcase from another profile: ask about them.
        // The first time the Toolbox runs in a profile: show the switches once (after that question, if there was one).
        if (screen instanceof TitleScreen && !greeted) {
            greeted = true;
            boolean ask = Suitcase.shouldAsk();
            if (!ask && !Switches.firstRun()) return;
            Minecraft mc = Minecraft.getInstance();
            mc.tell(() -> {
                if (mc.screen != screen) return;
                if (ask) mc.setScreen(new BringScreen(screen));
                else welcome(screen);
            });
        }
    }

    private static void welcome(Screen title) {
        welcomed = true;
        Minecraft.getInstance().setScreen(new ToolboxScreen(title, true));
    }

    /** The "bring your settings over?" screen is done: on a first run the switch screen comes next, otherwise back to where it was opened from. */
    static void afterBring(Screen parent) {
        if (parent instanceof TitleScreen && Switches.firstRun() && !welcomed) welcome(parent);
        else Minecraft.getInstance().setScreen(parent);
    }
}
