package com.example.chartoolbox.client;

import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.Notes;
import com.example.chartoolbox.model.Part;
import com.example.chartoolbox.model.Parts;
import com.example.chartoolbox.model.RowLayout;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkRegistry;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

// Only when the game is started with -Dchartoolbox.shots=<phase> (never in normal play): it writes what it finds as
// "[Shots] ..." lines in the log, takes pictures of the switch screen and quits. tools/run_real_client.py starts the game
// this way and reads the lines: that is how the Toolbox is checked in the real game without a person at the keyboard.
//   first     on the title screen: every part, then the Fortress Boxes switch clicked off and on
//   ways      in a flat test world: every way in, then all parts switched off and on again
//   others    on the title screen: every part, a picture, quit
final class DevShots {
    private DevShots() {}

    private static final String PHASE = System.getProperty("chartoolbox.shots", "");
    private static final Logger LOG = LogUtils.getLogger();
    private static final String FB = "fortressboxes";
    /** A key each carried mod registers and its network channel ("" = it has none): what "really off" is checked by. */
    private static final Map<String, String[]> SIGNS = Map.of(
            "charmap", new String[] {"key.charmap.world_map", "charmap:main"},
            "charlod", new String[] {"key.charlod.toggle", "charlod:far"},
            "charmenu", new String[] {"", ""},
            "charteams", new String[] {"key.charteams.open", "charteams:main"},
            "charconfig", new String[] {"key.charconfig.open", "charconfig:main"},
            FB, new String[] {"key.fortressboxes.toggle", "fortressboxes:main"});
    private static int age;
    private static boolean done;

    static void init() {
        if (!PHASE.isEmpty()) MinecraftForge.EVENT_BUS.addListener(DevShots::onTick);
    }

    // ---- what is logged about every part

    private static void report(Minecraft mc) {
        Screen title = mc.screen instanceof ToolboxScreen toolbox ? toolbox.parent() : mc.screen;
        LOG.info("[Shots] title={} menuMixins={}", title == null ? "none" : title.getClass().getSimpleName(), mixedIn(AbstractButton.class) || mixedIn(Screen.class));
        for (Part part : Parts.all()) {
            String id = part.id();
            String[] signs = SIGNS.getOrDefault(id, new String[] {"", ""});
            Notes.Note note = ToolboxScreen.noteOf(part);
            LOG.info("[Shots] {} loaded={} version={} key={} channel={} started={} separateCopy={} note={} brief={}", id,
                    ModList.get().isLoaded(id),
                    ModList.get().getModContainerById(id).map(c -> c.getModInfo().getVersion().toString()).orElse("none"),
                    signs[0].isEmpty() ? "none" : String.valueOf(Arrays.stream(mc.options.keyMappings).anyMatch(k -> k.getName().equals(signs[0]))),
                    signs[1].isEmpty() ? "none" : channel(signs[1]),
                    System.getProperty("chartoolbox.started." + id), Doubles.separateCopy(id), note.state(), note.brief());
        }
    }

    /** Has Main Menu's mixin code been put into this class of the game? (Its handlers are all named charmenu$...) */
    private static boolean mixedIn(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods()).anyMatch(m -> m.getName().contains("charmenu$"));
    }

    /** Is this network channel registered? (Forge keeps that list to itself, hence the reflection.) */
    private static String channel(String name) {
        try {
            Method find = NetworkRegistry.class.getDeclaredMethod("findTarget", ResourceLocation.class);
            find.setAccessible(true);
            return String.valueOf(((Optional<?>) find.invoke(null, new ResourceLocation(name))).isPresent());
        } catch (Throwable t) {
            return "unknown";
        }
    }

    // ---- phase "ways": goes into a small flat test world and tries every way into the Toolbox

    /** Where the self-check says the mouse is (so a picture can show the card that follows the mouse); -1 = the real mouse. */
    static int mouseX = -1, mouseY = -1;
    private static boolean made;
    private static int worldAge;

    /** Makes (once) and joins a small flat creative test world with cheats; true while the player is in it. */
    private static boolean world(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            if (!made && mc.getOverlay() == null && (mc.screen instanceof TitleScreen || mc.screen instanceof ToolboxScreen)) {
                made = true;
                // phase "all" walks round a normal world (nicer pictures, real terrain for the Map and Far View); the rest use a flat one
                var preset = PHASE.equals("all") || PHASE.equals("fps") ? WorldPresets.NORMAL : WorldPresets.FLAT;
                long seed = PHASE.equals("all") || PHASE.equals("fps") ? 20261113L : 1L;
                mc.createWorldOpenFlows().createFreshLevel("toolboxshots", new LevelSettings("toolboxshots", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(seed, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(preset).value().createWorldDimensions());
            }
            return false;
        }
        return true;
    }

    private static void command(Minecraft mc, String text) {
        if (mc.player != null) mc.player.connection.sendCommand(text);
    }

    // ---- phase "small": Zoom, Little HUD, Pickup list, Gear watch and Memory watch in a flat test world

    private static final String[] SMALL = {"zoom", "littlehud", "pickuplist", "gearwatch", "memorywatch"};

    private static void small(Minecraft mc) {
        if (!world(mc)) return;
        worldAge++;
        int t = worldAge - 100;
        switch (t) {
            case 60 -> {
                mc.setScreen(null);
                for (String id : SMALL) Switches.set(id, true);
                LOG.info("[Shots] zoom key={}", Arrays.stream(mc.options.keyMappings).filter(k -> k.getName().equals("key.chartoolbox.zoom"))
                        .map(KeyMapping::saveString).findFirst().orElse("none"));
                command(mc, "gamemode survival");
                command(mc, "time set 6000");
                command(mc, "weather clear");
                command(mc, "give @s diamond_pickaxe{Damage:1559,Enchantments:[{id:\"minecraft:efficiency\",lvl:1s}]} 1");
                command(mc, "item replace entity @s armor.head with iron_helmet{Damage:150}");
            }
            case 70 -> command(mc, "give @s iron_ore 3");
            case 75 -> command(mc, "give @s iron_ore 4");
            case 78 -> command(mc, "give @s cobblestone 10");
            case 90 -> {
                LOG.info("[Shots] hud lines={}", String.join(" | ", LittleHud.lines()));
                LOG.info("[Shots] pickups rows={}", String.join(" | ", PickupList.rows().stream().map(r -> r.words()).toList()));
                shot("small_hud_pickups");
            }
            case 95 -> {
                var ev = net.minecraftforge.client.ForgeHooksClient.onClickInput(0, mc.options.keyAttack, net.minecraft.world.InteractionHand.MAIN_HAND);
                LOG.info("[Shots] gear strip={} lowest={} guards={} attack cancelled={} meows={}", GearWatch.stripShowing(),
                        String.format(java.util.Locale.ROOT, "%.3f", GearWatch.lowest()), GearWatch.guards(mc.player), ev.isCanceled(), GearWatch.meows);
            }
            case 98 -> shot("small_gear");
            case 100 -> {
                // something to look at: a diamond block 12 blocks ahead, the same picture before and after zooming
                mc.player.setYRot(0);
                mc.player.setXRot(0);
                command(mc, "setblock ~ ~1 ~12 minecraft:diamond_block");
            }
            case 107 -> shot("small_zoom_before");
            case 110 -> Zoom.heldByTest = true;
            case 130 -> {
                LOG.info("[Shots] zoom current={}", String.format(java.util.Locale.ROOT, "%.2f", Zoom.current()));
                turnStart = mc.player.getYRot();
                mc.player.turn(100, 0); // what moving the mouse 100 steps does: 15 degrees unzoomed
            }
            case 131 -> LOG.info("[Shots] zoom turn={}", String.format(java.util.Locale.ROOT, "%.2f", mc.player.getYRot() - turnStart));
            case 133 -> {
                shot("small_zoom");
                MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.InputEvent.MouseScrollingEvent(1.0, false, false, false, 0, 0));
                LOG.info("[Shots] zoom scrolled={} selected={}", String.format(java.util.Locale.ROOT, "%.1f", Zoom.target()), mc.player.getInventory().selected);
            }
            case 140 -> Zoom.heldByTest = false;
            case 160 -> LOG.info("[Shots] zoom after={}", String.format(java.util.Locale.ROOT, "%.2f", Zoom.current()));
            case 165 -> MemoryWatch.say(com.example.chartoolbox.model.small.MemoryRules.Warn.GAME);
            case 172 -> {
                LOG.info("[Shots] memory notice={}", Notice.lastTitle());
                shot("small_memory");
            }
            case 180 -> mc.setScreen(new PauseScreen(true));
            case 190 -> {
                LOG.info("[Shots] pause line={}", MemoryWatch.pauseLine());
                shot("small_pause");
            }
            case 200 -> mc.setScreen(new PartSettingsScreen(null, "littlehud"));
            case 203 -> {
                if (mc.screen instanceof PartSettingsScreen s) {
                    s.clickSetting("hud.fps");
                    s.clickSetting("hud.corner");
                }
                LOG.info("[Shots] settings fps={} corner={}", PartSettings.on(com.example.chartoolbox.model.settings.Settings.HUD_FPS),
                        PartSettings.choice(com.example.chartoolbox.model.settings.Settings.HUD_CORNER));
            }
            case 206 -> shot("small_settings_hud");
            case 210 -> mc.setScreen(new PartSettingsScreen(null, "gearwatch"));
            case 216 -> shot("small_settings_gear");
            case 220 -> mc.setScreen(new PartSettingsScreen(null, "zoom"));
            case 226 -> shot("small_settings_zoom");
            case 230 -> {
                ToolboxScreen screen = new ToolboxScreen(null, false);
                mc.setScreen(screen);
            }
            case 232 -> {
                if (mc.screen instanceof ToolboxScreen s) s.mouseScrolled(0, 0, -20);
            }
            case 236 -> {
                report(mc);
                shot("small_rows");
            }
            case 240 -> mc.setScreen(null);
            case 250 -> shot("small_hud_moved");
            case 255 -> {
                for (String id : SMALL) Switches.set(id, false);
                command(mc, "give @s iron_ore 5");
            }
            case 270 -> {
                var ev = net.minecraftforge.client.ForgeHooksClient.onClickInput(0, mc.options.keyAttack, net.minecraft.world.InteractionHand.MAIN_HAND);
                LOG.info("[Shots] off hud={} pickups={} gear={} guard={} pause={} attack cancelled={} notice={}", LittleHud.lines().isEmpty(), PickupList.rows().size(),
                        GearWatch.stripShowing(), GearWatch.guards(mc.player), MemoryWatch.pauseLine().isEmpty(), ev.isCanceled(), Notice.showing());
                shot("small_off");
            }
            case 275 -> {
                for (String id : SMALL) Switches.set(id, true);
            }
            case 285 -> stop(mc);
            default -> { }
        }
    }

    private static float turnStart;

    // ---- phase "cat": Cat corner's four helpers, in a flat test world at night, then a fake kick for Auto Rejoin

    private static boolean left;
    private static int afterWorld;

    private static String gamma() {
        return String.format(java.util.Locale.ROOT, "%.2f", Fullbright.now());
    }

    private static void setting(com.example.chartoolbox.model.settings.Setting s, String value) {
        PartSettings.set(s, value);
    }

    private static void cat(Minecraft mc) {
        if (left) {
            rejoinPart(mc);
            return;
        }
        if (!world(mc)) return;
        worldAge++;
        int t = worldAge - 100;
        var S = new Object() {
            final com.example.chartoolbox.model.settings.Setting sleepy = com.example.chartoolbox.model.settings.Settings.CAT_SLEEPY,
                    bright = com.example.chartoolbox.model.settings.Settings.CAT_FULLBRIGHT, eat = com.example.chartoolbox.model.settings.Settings.CAT_EAT,
                    rejoin = com.example.chartoolbox.model.settings.Settings.CAT_REJOIN, x = com.example.chartoolbox.model.settings.Settings.SLEEPY_X,
                    y = com.example.chartoolbox.model.settings.Settings.SLEEPY_Y;
        };
        switch (t) {
            case 60 -> {
                mc.setScreen(null);
                Switches.set("catcorner", true);
                Switches.set("littlehud", false);
                setting(S.sleepy, "true");
                command(mc, "difficulty easy");
                command(mc, "gamemode survival");
                command(mc, "time set 18000");
                command(mc, "give @s bread 3");
            }
            case 75 -> {
                var at = SleepyCat.spot(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
                LOG.info("[Shots] cat visible={} spot={},{} onHud={} key={}", SleepyCat.visible(), at.get(0), at.get(1), SleepyCat.onTheHud(),
                        Arrays.stream(mc.options.keyMappings).filter(k -> k.getName().equals("key.chartoolbox.fullbright")).map(KeyMapping::saveString).findFirst().orElse("none"));
                shot("cat_corner");
            }
            case 80 -> Switches.set("littlehud", true);
            case 90 -> {
                LOG.info("[Shots] cat onHud={} pill={},{}", SleepyCat.onTheHud(), (int) LittleHud.pillX(), (int) LittleHud.pillY());
                shot("cat_on_hud");
            }
            case 95 -> Switches.set("littlehud", false);
            case 98 -> {
                LOG.info("[Shots] gamma before={}", gamma());
                shot("cat_dark");
            }
            case 100 -> setting(S.bright, "true");
            case 106 -> {
                LOG.info("[Shots] gamma on={}", gamma());
                shot("cat_bright");
            }
            case 110 -> mc.setScreen(new PauseScreen(true));
            case 115 -> LOG.info("[Shots] gamma paused={}", gamma());
            case 118 -> mc.setScreen(null);
            case 125 -> LOG.info("[Shots] gamma back={}", gamma());
            case 130 -> setting(S.bright, "false");
            case 135 -> LOG.info("[Shots] gamma off={}", gamma());
            case 140 -> {
                setting(S.eat, "true");
                command(mc, "effect give @s minecraft:hunger 20 200 true");
            }
            case 240 -> {
                int bread = 0;
                for (var st : mc.player.getInventory().items) if (st.getItem() == net.minecraft.world.item.Items.BREAD) bread += st.getCount();
                LOG.info("[Shots] eat ate={} meals={} food={} bread={}", AutoEat.meals > 0, AutoEat.meals, mc.player.getFoodData().getFoodLevel(), bread);
                command(mc, "effect clear @s");
            }
            case 250 -> mc.setScreen(new CatPlaceScreen(null));
            case 255 -> {
                if (mc.screen instanceof CatPlaceScreen s) {
                    int[] c = s.cat();
                    s.mouseClicked(c[0] + 10, c[1] - 4, 0);
                    s.mouseDragged(200, 120, 0, 0, 0);
                    s.mouseReleased(200, 120, 0);
                }
                LOG.info("[Shots] placed x={} y={}", PartSettings.number(S.x), PartSettings.number(S.y));
            }
            case 258 -> shot("cat_place");
            case 262 -> mc.setScreen(null);
            case 270 -> shot("cat_placed");
            case 275 -> mc.setScreen(new PartSettingsScreen(null, "catcorner"));
            case 280 -> shot("cat_page");
            case 290 -> {
                Switches.set("catcorner", false);
                mc.setScreen(null);
            }
            case 300 -> {
                LOG.info("[Shots] off visible={} fullbright={} eat={} rejoin={}", SleepyCat.visible(), Fullbright.wanted(), AutoEat.on(), AutoRejoin.on());
                shot("cat_off");
            }
            case 305 -> {
                Switches.set("catcorner", true);
                setting(S.rejoin, "true");
                left = true;
                mc.level.disconnect();
                mc.clearLevel(new TitleScreen());
                mc.setScreen(new TitleScreen());
            }
            default -> { }
        }
    }

    /** Auto Rejoin: a fake kick from a server that isn't there (a dead port on this PC), so the countdown and a retry really run. */
    private static void rejoinPart(Minecraft mc) {
        afterWorld++;
        switch (afterWorld) {
            case 40 -> {
                AutoRejoin.rememberForTest(new ServerData("Toolbox test server", "127.0.0.1:25599", false));
                mc.setScreen(new net.minecraft.client.gui.screens.DisconnectedScreen(new TitleScreen(), net.minecraft.network.chat.Component.literal("Connection Lost"),
                        net.minecraft.network.chat.Component.literal("Kicked for the Toolbox test")));
            }
            case 50 -> {
                LOG.info("[Shots] rejoin planned={} countdown={}", AutoRejoin.planned(), AutoRejoin.countdown());
                shot("cat_rejoin_countdown");
            }
            case 300 -> LOG.info("[Shots] rejoin tries={} planned={} screen={}", AutoRejoin.tries(), AutoRejoin.planned(),
                    mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
            case 305 -> {
                if (mc.screen != null) for (var child : mc.screen.children()) if (child instanceof PillButton b) b.onClick(0, 0);
                LOG.info("[Shots] rejoin after pill planned={} on={}", AutoRejoin.planned(), AutoRejoin.on());
            }
            case 310 -> shot("cat_rejoin_off");
            case 320 -> stop(mc);
            default -> { }
        }
    }

    // ---- phase "all": every part on together in a normal world, each carried mod's screen through its real key or
    // command, the frame rate with everything on and everything off, and clean pictures for the store page

    private static final java.util.List<Integer> FPS_ON = new java.util.ArrayList<>(), FPS_OFF = new java.util.ArrayList<>();

    private static void press(Minecraft mc, String name) {
        for (KeyMapping k : mc.options.keyMappings) if (k.getName().equals(name)) KeyMapping.click(k.getKey());
    }

    private static String screenName(Minecraft mc) {
        return mc.screen == null ? "none" : mc.screen.getClass().getSimpleName();
    }

    private static double average(java.util.List<Integer> xs) {
        return xs.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private static void all(Minecraft mc) {
        if (!world(mc)) return;
        worldAge++;
        int t = worldAge - 100;
        if (t >= 230 && t <= 330 && t % 20 == 10) FPS_ON.add(mc.getFps());
        if (t >= 360 && t <= 460 && t % 20 == 0) FPS_OFF.add(mc.getFps());
        switch (t) {
            case 40 -> {
                command(mc, "gamerule sendCommandFeedback false");
                command(mc, "gamerule announceAdvancements false");
            }
            case 60 -> {
                mc.setScreen(null);
                for (Part p : Parts.all()) Switches.set(p.id(), true);
                command(mc, "gamemode survival");
                command(mc, "time set 1000");
                command(mc, "weather clear");
                command(mc, "give @s iron_pickaxe{Damage:230} 1");
            }
            case 70 -> command(mc, "give @s oak_log 6");
            case 72 -> command(mc, "give @s apple 2");
            case 78 -> mc.getToasts().clear();
            case 80 -> {
                LOG.info("[Shots] all hud={} cat={} pickups={} gear={}", LittleHud.lines().size(), SleepyCat.visible(), PickupList.rows().size(), GearWatch.stripShowing());
                shot("all_world");
            }
            case 95 -> press(mc, "key.charmap.world_map");
            case 105 -> {
                LOG.info("[Shots] all map screen={}", screenName(mc));
                shot("all_map");
            }
            case 110 -> mc.setScreen(null);
            case 115 -> press(mc, "key.charteams.open");
            case 125 -> {
                LOG.info("[Shots] all teams screen={}", screenName(mc));
                shot("all_teams");
            }
            case 130 -> mc.setScreen(null);
            case 135 -> command(mc, "configs"); // a server command: the built-in server opens the screen
            case 145 -> {
                LOG.info("[Shots] all configs screen={}", screenName(mc));
                shot("all_configs");
            }
            case 150 -> mc.setScreen(null);
            case 155 -> press(mc, "key.charlod.settings");
            case 165 -> {
                LOG.info("[Shots] all farview screen={}", screenName(mc));
                shot("all_farview");
            }
            case 170 -> mc.setScreen(null);
            case 175 -> press(mc, "key.fortressboxes.toggle");
            case 185 -> {
                // B is still both the Map's "add waypoint" and Fortress Boxes here (nobody has fixed the clash in this profile)
                LOG.info("[Shots] all B screen={}", screenName(mc));
                mc.setScreen(null);
            }
            case 190 -> Zoom.heldByTest = true;
            case 205 -> {
                LOG.info("[Shots] all zoom={}", String.format(java.util.Locale.ROOT, "%.2f", Zoom.current()));
                shot("all_zoom");
                Zoom.heldByTest = false;
            }
            case 215 -> mc.getToasts().clear();
            case 340 -> {
                for (Part p : Parts.all()) Switches.set(p.id(), false);
            }
            case 470 -> {
                LOG.info("[Shots] all fps on={} off={}", String.format(java.util.Locale.ROOT, "%.0f", average(FPS_ON)), String.format(java.util.Locale.ROOT, "%.0f", average(FPS_OFF)));
                for (Part p : Parts.all()) Switches.set(p.id(), true);
                mc.setScreen(new ToolboxScreen(null, false));
            }
            case 480 -> shot("all_rows_top");
            case 482 -> {
                if (mc.screen instanceof ToolboxScreen s) s.mouseScrolled(0, 0, -20);
            }
            case 488 -> {
                report(mc);
                shot("all_rows_new");
            }
            case 495 -> mc.setScreen(new KeyClashScreen(null));
            case 502 -> shot("all_keyclash");
            case 510 -> stop(mc);
            default -> { }
        }
    }


    // ---- phase "fps": do the eight parts written in the Toolbox cost frames? In a normal world, once it has settled,
    // they are switched on and off in turn four times; the frame rate is sampled in each half and the averages compared.

    private static final String[] NEW_PARTS = {"suitcase", "keyclash", "zoom", "littlehud", "pickuplist", "gearwatch", "memorywatch", "catcorner"};
    private static final java.util.List<Integer> NEW_ON = new java.util.ArrayList<>(), NEW_OFF = new java.util.ArrayList<>();

    private static void fps(Minecraft mc) {
        if (!world(mc)) return;
        worldAge++;
        int t = worldAge - 100;
        if (t == 40) {
            command(mc, "gamerule sendCommandFeedback false");
            command(mc, "gamerule announceAdvancements false");
        }
        if (t == 60) {
            mc.setScreen(null);
            for (Part p : Parts.all()) Switches.set(p.id(), true);
            command(mc, "gamemode survival");
            command(mc, "time set 1000");
            command(mc, "weather clear");
            command(mc, "give @s iron_pickaxe{Damage:230} 1");
        }
        if (t >= 600 && t <= 1400) {
            int w = (t - 600) % 200;
            if (w == 40 || w == 60 || w == 80) NEW_ON.add(mc.getFps());
            if (w == 100 && t < 1400) {
                NEW_ON.add(mc.getFps());
                for (String id : NEW_PARTS) Switches.set(id, false);
            }
            if (w == 140 || w == 160 || w == 180) NEW_OFF.add(mc.getFps());
            if (w == 0 && t > 600) {
                NEW_OFF.add(mc.getFps());
                for (String id : NEW_PARTS) Switches.set(id, true);
            }
        }
        if (t == 1405) {
            LOG.info("[Shots] fps newparts on={} off={} samples={}+{} values on={} off={}", String.format(java.util.Locale.ROOT, "%.1f", average(NEW_ON)),
                    String.format(java.util.Locale.ROOT, "%.1f", average(NEW_OFF)), NEW_ON.size(), NEW_OFF.size(), NEW_ON, NEW_OFF);
            stop(mc);
        }
    }

    private static void ways(Minecraft mc) {
        if (!world(mc)) return;
        worldAge++;
        // the first 100 ticks are left alone: Config Helper opens its own welcome a moment after a first join
        switch (worldAge - 100) {
            case 60 -> {
                mc.setScreen(null);
                // whatever key the Toolbox is on in this profile (Y unless it was changed)
                KeyMapping open = toolboxKey(mc);
                LOG.info("[Shots] toolbox key={}", open == null ? "none" : open.saveString());
                if (open != null) KeyMapping.click(open.getKey());
            }
            case 70 -> {
                way("key", mc);
                shot("toolbox_ways_world");
            }
            case 80 -> {
                mc.setScreen(null);
                ClientCommandHandler.runCommand("toolbox");
            }
            case 90 -> way("command", mc);
            case 95 -> mc.setScreen(new PauseScreen(true));
            case 105 -> {
                CatButton cat = null;
                if (mc.screen != null) for (var child : mc.screen.children()) if (child instanceof CatButton b) cat = b;
                LOG.info("[Shots] way pause button={}", cat != null);
                shot("toolbox_ways_pause");
                if (cat != null) cat.onClick(0, 0);
            }
            case 115 -> way("pause click", mc);
            case 120 -> {
                Screen fromModsList = ModList.get().getModContainerById("chartoolbox")
                        .flatMap(c -> ConfigScreenHandler.getScreenFactoryFor(c.getModInfo())).map(f -> f.apply(mc, null)).orElse(null);
                LOG.info("[Shots] way modslist screen={}", fromModsList == null ? "none" : fromModsList.getClass().getSimpleName());
                mc.setScreen(new ToolboxScreen(null, false));
            }
            case 130 -> clickSwitch(mc); // off: the row gets its "Restart to finish" note, so the card has something to say
            case 132 -> {
                if (mc.screen instanceof ToolboxScreen screen && screen.rowOf(FB) != null) {
                    mouseX = (int) screen.rowOf(FB).x() + 40;
                    mouseY = (int) screen.rowOf(FB).y() + 13;
                }
            }
            case 150 -> shot("toolbox_ways_card");
            case 155 -> {
                mouseX = mouseY = -1;
                clickSwitch(mc);
            }
            case 165 -> {
                LOG.info("[Shots] back on={} file={}", Switches.isOn(FB), fileLine());
                mc.setScreen(null);
            }
            // every part switched off while the world is open, then on again: nothing may crash or stay on screen
            case 185 -> {
                // the Key clash fixer's notice: B is both the Map's "add waypoint" and Fortress Boxes in a new profile
                LOG.info("[Shots] notice title={} showing={}", Notice.lastTitle(), Notice.showing());
                shot("toolbox_ways_all_on");
            }
            case 190 -> Parts.all().forEach(p -> Switches.set(p.id(), false));
            case 230 -> {
                LOG.info("[Shots] all off={}", Parts.all().stream().noneMatch(p -> Switches.isOn(p.id())));
                shot("toolbox_ways_all_off");
            }
            case 235 -> Parts.all().forEach(p -> Switches.set(p.id(), true));
            case 290 -> {
                LOG.info("[Shots] all back on={}", Parts.all().stream().allMatch(p -> Switches.isOn(p.id())));
                shot("toolbox_ways_back_on");
            }
            case 295 -> mc.setScreen(new ToolboxScreen(null, false));
            case 310 -> {
                report(mc);
                shot("toolbox_ways_rows");
            }
            case 320 -> stop(mc);
            default -> { }
        }
    }

    private static void way(String name, Minecraft mc) {
        LOG.info("[Shots] way {} screen={}", name, mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
    }

    // ---- the other phases: everything happens on the title screen

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || done) return;
        Minecraft mc = Minecraft.getInstance();
        if (PHASE.equals("ways")) {
            ways(mc);
            return;
        }
        if (PHASE.equals("small")) {
            small(mc);
            return;
        }
        if (PHASE.equals("cat")) {
            cat(mc);
            return;
        }
        if (PHASE.equals("all")) {
            all(mc);
            return;
        }
        if (PHASE.equals("fps")) {
            fps(mc);
            return;
        }
        if (mc.getOverlay() != null || !(mc.screen instanceof TitleScreen || mc.screen instanceof ToolboxScreen || mc.screen instanceof BringScreen
                || mc.screen instanceof SuitcaseScreen || mc.screen instanceof KeyClashScreen)) {
            age = 0;
            return;
        }
        if (age == 39 && !Doubles.ready()) return; // the mods folder hasn't been read yet
        age++;
        if (PHASE.equals("suitcase-bring")) {
            bringing(mc);
            return;
        }
        if (PHASE.equals("keyclash")) {
            keyclash(mc);
            return;
        }
        if (PHASE.equals("suitcase-save") && age == 45) packTheSuitcase(mc);
        if (age == 40) {
            LOG.info("[Shots] firstRun={} screen={}", Switches.firstRun(), mc.screen instanceof ToolboxScreen ? "ToolboxScreen" : "TitleScreen");
            if (!(mc.screen instanceof ToolboxScreen)) mc.setScreen(new ToolboxScreen(mc.screen, false));
            report(mc);
        }
        if (age == 60) shot("toolbox_" + PHASE);
        if (PHASE.equals("first")) {
            if (age == 70) clickSwitch(mc);
            if (age == 90) {
                LOG.info("[Shots] after click on={} property={} file={} note={}", Switches.isOn(FB),
                        System.getProperty("chartoolbox.part." + FB), fileLine(), ToolboxScreen.noteOf(Parts.FORTRESS_BOXES).state());
                shot("toolbox_first_off");
            }
            if (age == 100) clickSwitch(mc);
            if (age == 110) LOG.info("[Shots] back on={} file={}", Switches.isOn(FB), fileLine());
            if (age == 115) stop(mc);
        } else if (age == 70) {
            stop(mc);
        }
    }

    // ---- phases "suitcase-save" and "suitcase-bring": one profile packs the suitcase, a second one brings it over

    private static KeyMapping toolboxKey(Minecraft mc) {
        for (KeyMapping k : mc.options.keyMappings) if (k.getName().equals("key.chartoolbox.open")) return k;
        return null;
    }

    /** Profile A: change a key, an option and a switch, give it a server and a waypoint, then save. */
    private static void packTheSuitcase(Minecraft mc) {
        try {
            KeyMapping key = toolboxKey(mc);
            key.setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F12));
            KeyMapping.resetMapping();
            mc.options.renderDistance().set(7);
            Switches.set(FB, false);
            ServerList servers = new ServerList(mc);
            servers.load();
            servers.add(new ServerData("Test server", "test.example", false), false);
            servers.save();
            java.nio.file.Path waypoints = mc.gameDirectory.toPath().resolve("charmap").resolve("server_test.example").resolve("waypoints.txt");
            Files.createDirectories(waypoints.getParent());
            Files.writeString(waypoints, "Home|1|64|2");
            String said = Suitcase.saveNow();
            LOG.info("[Shots] suitcase saved={} from={} mode={}", said, Suitcase.info().map(i -> i.profileName()).orElse("nothing"), Suitcase.mode());
        } catch (Exception e) {
            LOG.error("[Shots] suitcase save failed", e);
        }
    }

    /** Profile B: the question must be up by itself; press Bring, look at what changed, press Done. */
    private static void bringing(Minecraft mc) {
        if (age == 40) {
            LOG.info("[Shots] bring screen={} mode={} savedFrom={}", mc.screen.getClass().getSimpleName(), Suitcase.mode(),
                    Suitcase.info().map(i -> i.profileName()).orElse("nothing"));
        }
        if (age == 60) shot("toolbox_suitcase-bring");
        if (age == 70 && mc.screen instanceof BringScreen bring) bring.press("Bring");
        if (age == 90) {
            ServerList servers = new ServerList(mc);
            servers.load();
            KeyMapping key = toolboxKey(mc);
            LOG.info("[Shots] brought key={} renderDistance={} fortressboxes={} servers={} waypoints={} mode={}",
                    key == null ? "none" : key.saveString(), mc.options.renderDistance().get(), Switches.isOn(FB), servers.size(),
                    Files.exists(mc.gameDirectory.toPath().resolve("charmap").resolve("server_test.example").resolve("waypoints.txt")), Suitcase.mode());
            shot("toolbox_suitcase-bring_done");
        }
        if (age == 100 && mc.screen instanceof BringScreen bring) bring.press("Done");
        if (age == 110) {
            LOG.info("[Shots] after bring screen={}", mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
            shot("toolbox_suitcase-bring_after");
        }
        if (age == 125) {
            mc.setScreen(new SuitcaseScreen(null));
        }
        if (age == 140) shot("toolbox_suitcase-settings");
        if (age == 150) stop(mc);
    }

    // ---- phase "keyclash": the fixer on the title screen, with Char's testing-instance mods beside the Toolbox

    private static final String FB_KEY = "key.fortressboxes.toggle";

    private static String keyOf(String id) {
        KeyMapping k = KeyClash.mapping(id);
        return k == null ? "none" : k.getKey().getName() + (k.getKeyModifier().name().equals("NONE") ? "" : "+" + k.getKeyModifier().name());
    }

    private static String clashList() {
        StringBuilder sb = new StringBuilder();
        for (var c : KeyClash.clashes()) sb.append(sb.isEmpty() ? "" : " ; ").append(c.signature());
        return sb.toString();
    }

    private static String optionsLine(Minecraft mc, String id) {
        try {
            for (String line : Files.readAllLines(mc.gameDirectory.toPath().resolve("options.txt"))) if (line.startsWith("key_" + id + ":")) return line;
            return "missing";
        } catch (Exception e) {
            return "unreadable";
        }
    }

    private static void keyclash(Minecraft mc) {
        Screen title = mc.screen instanceof ToolboxScreen t ? t.parent() : mc.screen instanceof TitleScreen ? mc.screen : null;
        switch (age) {
            case 40 -> {
                LOG.info("[Shots] clashes count={} row={}", KeyClash.clashes().size(), KeyClash.rowLine());
                for (var c : KeyClash.clashes()) {
                    StringBuilder who = new StringBuilder();
                    for (var b : c.binds()) who.append(" | ").append(b.id()).append(" (").append(b.ownerName()).append(", ").append(b.rank()).append(", ").append(b.context()).append(")");
                    LOG.info("[Shots] clash {}{}", c.signature(), who);
                }
                mc.setScreen(new ToolboxScreen(title, false));
            }
            case 50 -> shot("toolbox_keyclash_row");
            case 60 -> mc.setScreen(new KeyClashScreen(mc.screen));
            case 70 -> shot("toolbox_keyclash_list");
            case 80 -> {
                if (mc.screen instanceof KeyClashScreen s) LOG.info("[Shots] pressed fix={}", s.press("Fix for me"));
            }
            case 90 -> {
                for (var m : KeyClash.plan()) LOG.info("[Shots] plan {} {} -> {}", m.id(), m.fromKey(), m.toKey());
                shot("toolbox_keyclash_preview");
            }
            case 100 -> {
                if (mc.screen instanceof KeyClashScreen s) LOG.info("[Shots] pressed doit={}", s.press("Do it"));
            }
            case 110 -> {
                LOG.info("[Shots] after fix clashes={} undo={} fortressboxes={} file={}", KeyClash.clashes().size(), KeyClash.undoList().size(), keyOf(FB_KEY), optionsLine(mc, FB_KEY));
                shot("toolbox_keyclash_fixed");
            }
            case 120 -> {
                if (mc.screen instanceof KeyClashScreen s) LOG.info("[Shots] pressed undo={}", s.press("Undo last fix"));
            }
            case 130 -> {
                LOG.info("[Shots] after undo clashes={} fortressboxes={} list={}", KeyClash.clashes().size(), keyOf(FB_KEY), clashList());
                shot("toolbox_keyclash_undone");
            }
            case 140 -> {
                if (mc.screen instanceof KeyClashScreen s) s.pressOn(FB_KEY, "Change");
            }
            case 142 -> shot("toolbox_keyclash_waiting");
            case 145 -> {
                if (mc.screen instanceof KeyClashScreen s) s.keyPressed(GLFW.GLFW_KEY_N, GLFW.glfwGetKeyScancode(GLFW.GLFW_KEY_N), 0);
            }
            case 150 -> {
                LOG.info("[Shots] changed fortressboxes={} file={}", keyOf(FB_KEY), optionsLine(mc, FB_KEY));
                shot("toolbox_keyclash_changed");
            }
            case 160 -> {
                if (mc.screen instanceof KeyClashScreen s) s.pressOn(FB_KEY, "Clear");
            }
            case 165 -> LOG.info("[Shots] cleared fortressboxes={}", keyOf(FB_KEY));
            case 170 -> {
                KeyClash.set(FB_KEY, InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B), net.minecraftforge.client.settings.KeyModifier.NONE);
                LOG.info("[Shots] put back fortressboxes={}", keyOf(FB_KEY));
            }
            case 175 -> stop(mc);
            default -> { }
        }
    }

    private static void stop(Minecraft mc) {
        done = true;
        LOG.info("[Shots] done");
        mc.stop();
    }

    private static void clickSwitch(Minecraft mc) {
        if (!(mc.screen instanceof ToolboxScreen screen)) return;
        RowLayout.Rect row = screen.rowOf(FB);
        if (row == null) return;
        RowLayout.Rect sw = RowLayout.switchOf(row);
        screen.mouseClicked(sw.x() + sw.w() / 2, sw.y() + sw.h() / 2, 0);
    }

    private static String fileLine() {
        try {
            for (String line : Files.readAllLines(Switches.file())) if (line.startsWith(FB)) return line;
            return "missing";
        } catch (Exception e) {
            return "unreadable";
        }
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }
}
