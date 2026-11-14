package com.example.chartoolbox.client;

import com.example.chartoolbox.CharToolbox;
import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.Part;
import com.example.chartoolbox.model.Parts;
import com.example.chartoolbox.model.keys.Bind;
import com.example.chartoolbox.model.keys.Clash;
import com.example.chartoolbox.model.keys.ClashMemory;
import com.example.chartoolbox.model.keys.ClashWords;
import com.example.chartoolbox.model.keys.Clashes;
import com.example.chartoolbox.model.keys.FixPlan;
import com.example.chartoolbox.model.keys.FreeKeys;
import com.example.chartoolbox.model.keys.Meet;
import com.example.chartoolbox.model.keys.Move;
import com.example.chartoolbox.model.keys.Owners;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The part "Key clash fixer": reads the game's own list of keys, finds the ones two actions fight over (rules in
 * model/keys, tested there), changes keys through the game's own code, and says so once when a new clash turns up.
 * Off = it does nothing: no notice, no work when you join a world.
 */
public final class KeyClash {
    private KeyClash() {}

    public static final String PART = "keyclash";
    private static final Logger LOG = LogUtils.getLogger();
    /** The game's two creative-only hotbar keys: they give way to a mod in "fix for me". */
    private static final Set<String> SPARE = Set.of("key.saveToolbarActivator", "key.loadToolbarActivator");

    private static ClashMemory memory;
    private static Set<KeyMapping> gamesOwn;
    private static int cachedSig;
    private static List<Bind> binds = List.of();
    private static List<Clash> clashes = List.of();
    private static final Map<String, IKeyConflictContext> CONTEXTS = new HashMap<>();
    private static int joinedTicks = -1;

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(KeyClash::onTick);
        MinecraftForge.EVENT_BUS.addListener(KeyClash::onJoin);
        ToolboxScreen.LIVE.put(PART, KeyClash::rowLine);
    }

    static boolean on() {
        return Switches.isOn(PART);
    }

    // ---- the game's keys, as the rules see them

    /** Every action the game has, in the game's order. Worked out again only when a key changed. */
    static synchronized List<Bind> binds() {
        Minecraft mc = Minecraft.getInstance();
        KeyMapping[] all = mc.options.keyMappings;
        int sig = all.length;
        for (KeyMapping k : all) sig = sig * 31 + k.getKey().hashCode() * 7 + k.getKeyModifier().ordinal();
        if (sig == cachedSig && !binds.isEmpty()) return binds;
        Set<String> modIds = new HashSet<>();
        ModList.get().forEachModContainer((id, c) -> modIds.add(id));
        Set<String> toolbox = new HashSet<>();
        toolbox.add(CharToolbox.MODID);
        for (Part p : Parts.all()) if (p.kind() == Part.Kind.CARRIED) toolbox.add(p.id());
        Set<KeyMapping> game = gamesOwn(mc.options);
        List<Bind> out = new ArrayList<>();
        CONTEXTS.clear();
        for (KeyConflictContext c : KeyConflictContext.values()) CONTEXTS.put(c.name(), c);
        for (KeyMapping k : all) {
            IKeyConflictContext ctx = k.getKeyConflictContext();
            String ctxName = ctx instanceof Enum<?> e ? e.name() : ctx.getClass().getName();
            CONTEXTS.putIfAbsent(ctxName, ctx);
            boolean ours = game.contains(k);
            String owner = ours ? "minecraft" : Owners.guess(k.getName(), k.getCategory(), modIds);
            String ownerName = ours ? "Minecraft" : owner == null ? Component.translatable(k.getCategory()).getString() : modName(owner);
            Bind.Rank rank = ours ? (SPARE.contains(k.getName()) ? Bind.Rank.GAME_SPARE : Bind.Rank.GAME)
                    : owner != null && toolbox.contains(owner) ? Bind.Rank.TOOLBOX : Bind.Rank.MOD;
            out.add(new Bind(k.getName(), Component.translatable(k.getName()).getString(), owner == null ? "" : owner, ownerName,
                    k.getKey().getName(), k.getKeyModifier().name(), ctxName, rank));
        }
        binds = List.copyOf(out);
        clashes = Clashes.find(binds, meet());
        cachedSig = sig;
        return binds;
    }

    static synchronized List<Clash> clashes() {
        binds();
        return clashes;
    }

    /** Forge's own answer to "can these two be live at the same time?" */
    private static Meet meet() {
        return (a, b) -> {
            IKeyConflictContext ca = CONTEXTS.get(a), cb = CONTEXTS.get(b);
            return ca != null && cb != null && ca.conflicts(cb);
        };
    }

    /** The game's own actions: what Options itself holds (mods' keys are added to the list after). */
    private static Set<KeyMapping> gamesOwn(Options options) {
        if (gamesOwn != null) return gamesOwn;
        Set<KeyMapping> found = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Field f : Options.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.setAccessible(true);
                if (f.getType() == KeyMapping.class) found.add((KeyMapping) f.get(options));
                else if (f.getType() == KeyMapping[].class) {
                    KeyMapping[] arr = (KeyMapping[]) f.get(options);
                    // the hotbar slots are a small array of 9; the whole list (game + mods) is the big one
                    if (arr != null && arr.length <= 9) Collections.addAll(found, arr);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                // skip a field we can't read
            }
        }
        found.remove(null);
        return gamesOwn = found;
    }

    private static String modName(String id) {
        String name = ModList.get().getModContainerById(id).map(c -> c.getModInfo().getDisplayName()).orElse(id);
        return name.startsWith("{C} ") ? name.substring(4) : name;
    }

    // ---- words

    /** The row's live line on the switch screen: "" when all is fine or the part is off. */
    static String rowLine() {
        return on() ? ClashWords.rowLine(clashes(), KeyClash::keyName) : "";
    }

    /** A key's name for people: "B", "Mouse Button 4". */
    static String keyName(String key) {
        return InputConstants.getKey(key).getDisplayName().getString();
    }

    /** With its modifier: "Shift + B". */
    static String keyName(String key, String modifier) {
        InputConstants.Key k = InputConstants.getKey(key);
        if (Bind.UNBOUND.equals(key)) return "no key";
        return KeyModifier.valueFromString(modifier).getCombinedName(k, k::getDisplayName).getString();
    }

    // ---- changing keys

    static KeyMapping mapping(String id) {
        for (KeyMapping k : Minecraft.getInstance().options.keyMappings) if (k.getName().equals(id)) return k;
        return null;
    }

    /** Puts an action on a key, the way the Controls screen does, and saves. */
    static void set(String id, InputConstants.Key key, KeyModifier modifier) {
        KeyMapping k = mapping(id);
        if (k == null) return;
        Minecraft mc = Minecraft.getInstance();
        k.setKeyModifierAndCode(modifier, key);
        KeyMapping.resetMapping();
        mc.options.save();
        cachedSig = 0;
    }

    static void clear(String id) {
        set(id, InputConstants.UNKNOWN, KeyModifier.NONE);
    }

    /** What "fix for me" would do now. */
    static List<Move> plan() {
        return FixPlan.make(binds(), meet(), FreeKeys.ORDER);
    }

    /** Does it, and keeps it for Undo. */
    static void apply(List<Move> moves) {
        for (Move m : moves) set(m.id(), InputConstants.getKey(m.toKey()), KeyModifier.NONE);
        memory().setUndo(moves);
        memory().remember(clashes());
        saveMemory();
        LOG.info("[Toolbox] key clash fixer moved {} key(s)", moves.size());
    }

    static boolean canUndo() {
        return !memory().undo().isEmpty();
    }

    static List<Move> undoList() {
        return memory().undo();
    }

    /** Puts back what the last "fix for me" moved. */
    static void undo() {
        List<Move> moves = new ArrayList<>(memory().undo());
        Collections.reverse(moves);
        for (Move m : moves) set(m.id(), InputConstants.getKey(m.fromKey()), KeyModifier.valueFromString(m.fromModifier()));
        memory().setUndo(List.of());
        saveMemory();
    }

    // ---- what it remembers in this profile

    private static Path memoryFile() {
        return FMLPaths.CONFIGDIR.get().resolve("chartoolbox").resolve("keyclash.properties");
    }

    private static ClashMemory memory() {
        if (memory == null) {
            try {
                memory = Files.isRegularFile(memoryFile()) ? ClashMemory.parse(Files.readString(memoryFile())) : new ClashMemory();
            } catch (Exception e) {
                memory = new ClashMemory();
            }
        }
        return memory;
    }

    private static void saveMemory() {
        try {
            Files.createDirectories(memoryFile().getParent());
            Files.writeString(memoryFile(), memory().format());
        } catch (Exception e) {
            LOG.warn("[Toolbox] Couldn't save {}", memoryFile(), e);
        }
    }

    // ---- the notice, once per new clash, a few seconds after joining a world

    private static void onJoin(ClientPlayerNetworkEvent.LoggingIn e) {
        joinedTicks = 0;
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || joinedTicks < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            joinedTicks = -1;
            return;
        }
        joinedTicks++;
        // never while the world is still loading or a screen is open
        if (joinedTicks < 100 || mc.screen != null || mc.getOverlay() != null) return;
        joinedTicks = -1;
        if (!on()) return;
        List<Clash> fresh = memory().newOnes(clashes());
        if (fresh.isEmpty()) return;
        memory().remember(clashes());
        saveMemory();
        String open = ToolboxClient.openKeyName();
        String text;
        if (fresh.size() == 1) {
            Clash c = fresh.get(0);
            Set<String> mods = new LinkedHashSet<>();
            for (Bind b : c.binds()) mods.add(b.ownerName());
            text = keyName(c.key()) + " does " + c.binds().size() + " things (" + String.join(", ", mods) + "). Sort it out in the Toolbox (" + open + ").";
        } else {
            text = fresh.size() + " keys do more than one thing. Sort them out in the Toolbox (" + open + ").";
        }
        Notice.show(PART, "Two jobs on one key", text, false);
    }
}
