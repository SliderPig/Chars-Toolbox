package com.example.chartoolbox.client;

import com.example.chartoolbox.CharToolbox;
import com.example.chartoolbox.Switches;
import com.example.chartoolbox.model.Defaults;
import com.example.chartoolbox.model.Notes;
import com.example.chartoolbox.model.Part;
import com.example.chartoolbox.model.Parts;
import com.example.chartoolbox.model.RowLayout;
import com.example.chartoolbox.model.RowLayout.Placed;
import com.example.chartoolbox.model.RowLayout.Rect;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The switch screen: one pill-shaped row per part with a pink switch. Flipping a switch works right away; a row can
 * carry a short note ("Restart to finish", "Separate copy found..."). Everything rounded, nothing boxy.
 */
public class ToolboxScreen extends Screen {
    static final ResourceLocation CAT = new ResourceLocation(CharToolbox.MODID, "textures/gui/cat.png");
    private static final ResourceLocation GEAR = new ResourceLocation(CharToolbox.MODID, "textures/gui/gear.png");
    private static final ResourceLocation LOCK = new ResourceLocation(CharToolbox.MODID, "textures/gui/lock.png");
    private static final Logger LOG = LogUtils.getLogger();
    private static final String TITLE = "Char's Toolbox";
    private static final String WELCOME = "Welcome! Pick what you want switched on. You can change it any time.";
    private static final Map<String, ResourceLocation> ICONS = new HashMap<>();
    /** A part's live line ("Key B has 2 jobs"): shown in pink in place of its blurb while it says something. */
    static final Map<String, java.util.function.Supplier<String>> LIVE = new HashMap<>();

    private final Screen parent;
    private final boolean welcome;
    private EditBox find;
    private float scroll, most;
    private List<Placed> placed = List.of();
    private List<FormattedCharSequence> welcomeLines = List.of();
    private final Map<String, Float> slide = new HashMap<>();
    private long lastFrame;
    private Part hovered;
    private Notes.Note hoveredNote;
    private boolean hoveredCut;

    public ToolboxScreen(Screen parent, boolean welcome) {
        super(Component.literal(TITLE));
        this.parent = parent;
        this.welcome = welcome;
    }

    /** The screen this one was opened over (the self-check asks: which title screen is it?). */
    Screen parent() {
        return parent;
    }

    // ---- where things are

    private int panelW() {
        return Math.min(420, width - 40);
    }

    private int panelH() {
        return Math.min(260, height - 30);
    }

    private int px() {
        return (width - panelW()) / 2;
    }

    private int py() {
        return (height - panelH()) / 2;
    }

    private Rect listArea() {
        float top = py() + 34 + (welcomeLines.isEmpty() ? 0 : welcomeLines.size() * 10 + 12);
        float bottom = py() + panelH() - 28;
        return new Rect(px() + 12, top, panelW() - 24, Math.max(0, bottom - top));
    }

    private Rect searchPill() {
        int w = Math.max(60, Math.min(130, panelW() - 36 - font.width(TITLE) - 28));
        return new Rect(px() + panelW() - 12 - w, py() + 9, w, 16);
    }

    private Rect donePill() {
        return new Rect(px() + panelW() - 12 - 54, py() + panelH() - 23, 54, 16);
    }

    private Rect shifted(Rect r) {
        return new Rect(r.x(), r.y() - scroll, r.w(), r.h());
    }

    /** Where this part's row is on the screen right now, or null when it isn't listed. */
    public Rect rowOf(String partId) {
        for (Placed p : placed) if (p.part() != null && p.part().id().equals(partId)) return shifted(p.rect());
        return null;
    }

    /** The note a part's row shows, from what is really installed and running. */
    public static Notes.Note noteOf(Part p) {
        ModList mods = ModList.get();
        boolean carried = p.kind() == Part.Kind.CARRIED;
        if (carried && !mods.isLoaded(p.id())) return new Notes.Note(Notes.State.NORMAL, "Missing from this copy of the Toolbox", "Missing", true);
        String same = Defaults.sameJobMod(p, mods::isLoaded);
        String sameName = same == null ? null : mods.getModContainerById(same).map(c -> c.getModInfo().getDisplayName()).orElse(same);
        return Notes.of(p, Switches.isOn(p.id()), Switches.onAtStart(p.id()), carried && Doubles.separateCopy(p.id()), sameName, Switches.firstRun());
    }

    // ---- set-up

    @Override
    protected void init() {
        String typed = find == null ? "" : find.getValue();
        welcomeLines = welcome ? font.split(Component.literal(WELCOME), panelW() - 40) : List.of();
        Rect pill = searchPill();
        find = new EditBox(font, (int) pill.x() + 9, (int) pill.y() + 4, (int) pill.w() - 16, 10, Component.literal("Find a part"));
        find.setBordered(false);
        find.setMaxLength(40);
        find.setTextColor(Look.TEXT);
        find.setHint(Component.literal("Find a part").withStyle(s -> s.withColor(Look.HINT)));
        find.setValue(typed);
        find.setResponder(s -> scroll = 0);
        addWidget(find);
    }

    @Override
    public void tick() {
        find.tick();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // ---- drawing

    private String fit(String s, float w) {
        if (font.width(s) <= w) return s;
        return font.plainSubstrByWidth(s, (int) Math.max(0, w - font.width("..."))) + "...";
    }

    private static List<Part> ofKind(List<Part> parts, Part.Kind kind) {
        return parts.stream().filter(p -> p.kind() == kind).toList();
    }

    @Override
    public void render(GuiGraphics g, int realX, int realY, float partial) {
        // (the self-check can say where the mouse is)
        int mouseX = DevShots.mouseX >= 0 ? DevShots.mouseX : realX, mouseY = DevShots.mouseX >= 0 ? DevShots.mouseY : realY;
        renderBackground(g);
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(100, now - lastFrame);
        lastFrame = now;

        Rect area = listArea();
        List<Part> shown = Parts.matching(find.getValue());
        placed = RowLayout.place(List.of("From your mods", "New"), List.of(ofKind(shown, Part.Kind.CARRIED), ofKind(shown, Part.Kind.BUILT_IN)), area);
        most = Math.max(0, RowLayout.contentHeight(placed, area) - area.h());
        scroll = Mth.clamp(scroll, 0, most);
        hovered = null;

        g.drawManaged(() -> {
            panel(g);
            header(g);
            rows(g, mouseX, mouseY, area, dt);
            footer(g, mouseX, mouseY);
        });
        find.render(g, mouseX, mouseY, partial);
        pictures(g, area);
        // the card only comes up when it has more to say than the row itself does
        if (hovered != null && (hoveredCut || !hoveredNote.text().equals(hoveredNote.brief()))) g.drawManaged(() -> card(g, mouseX, mouseY));
    }

    private void panel(GuiGraphics g) {
        int x = px(), y = py(), w = panelW(), h = panelH();
        Soft.shadow(g, x, y + 3, x + w, y + h + 3, 14, 10, 0x70000000, false);
        Soft.round(g, x, y, x + w, y + h, 14, Look.PANEL, Look.PANEL_LOW);
        Soft.ring(g, x, y, x + w, y + h, 14, 1, Soft.alpha(Look.PINK, 0.25f));
    }

    private void header(GuiGraphics g) {
        int x = px(), y = py();
        Soft.disc(g, x + 22, y + 17, 11, Look.PINK_SOFT);
        g.drawString(font, TITLE, x + 38, y + 13, Look.TEXT, false);
        Rect pill = searchPill();
        Soft.round(g, pill.x(), pill.y(), pill.x() + pill.w(), pill.y() + pill.h(), 8, Look.FIELD);
        if (!welcomeLines.isEmpty()) {
            float x0 = x + 12, x1 = x + panelW() - 12, y0 = y + 32, y1 = y0 + welcomeLines.size() * 10 + 8;
            Soft.round(g, x0, y0, x1, y1, 8, 0xFF3A2244, 0xFF2E1B36);
            Soft.ring(g, x0, y0, x1, y1, 8, 1, Soft.alpha(Look.PINK, 0.6f));
            for (int i = 0; i < welcomeLines.size(); i++) g.drawString(font, welcomeLines.get(i), (int) x0 + 8, (int) y0 + 5 + i * 10, Look.TEXT, false);
        }
    }

    private void rows(GuiGraphics g, int mouseX, int mouseY, Rect area, float dt) {
        g.enableScissor((int) area.x(), (int) area.y(), (int) Math.ceil(area.x() + area.w()), (int) Math.ceil(area.y() + area.h()));
        for (Placed p : placed) {
            Rect r = shifted(p.rect());
            if (r.y() + r.h() < area.y() || r.y() > area.y() + area.h()) continue;
            if (p.part() == null) g.drawString(font, p.label(), (int) r.x() + 4, (int) r.y() + 3, Look.TEXT_DIM, false);
            else row(g, p.part(), r, area.has(mouseX, mouseY) && r.has(mouseX, mouseY), dt);
        }
        if (placed.isEmpty()) g.drawString(font, "No part with that name.", (int) area.x() + 4, (int) area.y() + 4, Look.TEXT_DIM, false);
        g.disableScissor();
        // more parts than fit: a slim rounded bar at the right shows there is more, and where you are
        if (most > 0) {
            float bx = area.x() + area.w() + 4, thumbH = Math.max(14, area.h() * area.h() / (area.h() + most));
            float by = area.y() + (area.h() - thumbH) * (scroll / most);
            Soft.round(g, bx, area.y(), bx + 3, area.y() + area.h(), 1.5f, Soft.alpha(Look.PINK_SOFT, 0.10f));
            Soft.round(g, bx, by, bx + 3, by + thumbH, 1.5f, Soft.alpha(Look.PINK_SOFT, 0.75f));
        }
    }

    private void row(GuiGraphics g, Part part, Rect r, boolean over, float dt) {
        Notes.Note note = noteOf(part);
        boolean locked = note.locked(), hasNote = !note.text().isEmpty();
        float x0 = r.x(), y0 = r.y(), x1 = x0 + r.w(), y1 = y0 + r.h();
        Soft.round(g, x0, y0, x1, y1, 13, locked ? Soft.alpha(Look.ROW, 0.55f) : over ? Look.ROW_HOVER : Look.ROW);
        if (locked) Soft.ring(g, x0, y0, x1, y1, 13, 1, Soft.alpha(Look.PINK, 0.35f));
        Soft.disc(g, x0 + 13, y0 + 13, 10, locked ? Soft.alpha(Look.PINK_SOFT, 0.5f) : Look.PINK_SOFT);

        Rect sw = RowLayout.switchOf(r);
        boolean gear = hasGear(part) && !locked;
        float textX = x0 + 28, room = (gear ? RowLayout.gearOf(r).x() : sw.x()) - 6 - textX;
        // the row shows the note's short form; the card by the mouse has the whole sentence
        String live = hasNote || !LIVE.containsKey(part.id()) ? "" : LIVE.get(part.id()).get();
        String second = hasNote ? note.brief() : !live.isEmpty() ? live : part.blurb();
        String name = fit(part.name(), room), under = fit(second, room);
        g.drawString(font, name, (int) textX, (int) y0 + 4, locked ? Look.TEXT_DIM : Look.TEXT, false);
        g.drawString(font, under, (int) textX, (int) y0 + 14, hasNote || !live.isEmpty() ? Look.PINK : Look.TEXT_DIM, false);

        if (!locked) {
            float target = Switches.isOn(part.id()) ? 1 : 0, t = slide.getOrDefault(part.id(), target);
            t = target > t ? Math.min(target, t + dt / 120f) : Math.max(target, t - dt / 120f);
            slide.put(part.id(), t);
            float e = Soft.ease(t);
            Soft.round(g, sw.x(), sw.y(), sw.x() + sw.w(), sw.y() + sw.h(), 6, Soft.mix(Look.SWITCH_OFF, Look.PINK, e));
            Soft.disc(g, sw.x() + 6 + e * (sw.w() - 12), sw.y() + 6, 4.5f, Soft.mix(Look.KNOB_OFF, Look.KNOB_ON, e));
        }
        if (over) {
            hovered = part;
            hoveredNote = note;
            hoveredCut = !name.equals(part.name()) || !under.equals(second);
        }
    }

    private void footer(GuiGraphics g, int mouseX, int mouseY) {
        Rect done = donePill();
        boolean over = done.has(mouseX, mouseY);
        g.drawString(font, fit("Switches work right away", done.x() - (px() + 16) - 6), px() + 16, py() + panelH() - 19, Look.TEXT_DIM, false);
        Soft.round(g, done.x(), done.y(), done.x() + done.w(), done.y() + done.h(), 8, over ? Look.PINK : 0xFF4A2A55, over ? Look.PINK_DEEP : 0xFF3B2146);
        g.drawString(font, "Done", (int) (done.x() + (done.w() - font.width("Done")) / 2f), (int) done.y() + 4, Look.TEXT, false);
    }

    /** The small pictures: they are textures, which don't go through the shape batch, so they are drawn after it. */
    private void pictures(GuiGraphics g, Rect area) {
        RenderSystem.enableBlend();
        g.blit(CAT, px() + 14, py() + 9, 0f, 0f, 16, 16, 16, 16);
        g.enableScissor((int) area.x(), (int) area.y(), (int) Math.ceil(area.x() + area.w()), (int) Math.ceil(area.y() + area.h()));
        for (Placed p : placed) {
            if (p.part() == null) continue;
            Rect r = shifted(p.rect());
            if (r.y() + r.h() < area.y() || r.y() > area.y() + area.h()) continue;
            Part part = p.part();
            boolean locked = noteOf(part).locked();
            if (locked) g.setColor(1f, 1f, 1f, 0.5f);
            g.blit(ICONS.computeIfAbsent(part.id(), id -> new ResourceLocation(CharToolbox.MODID, "textures/gui/part/" + id + ".png")),
                    (int) r.x() + 5, (int) r.y() + 5, 0f, 0f, 16, 16, 16, 16);
            if (locked) g.setColor(1f, 1f, 1f, 1f);
            Rect sw = RowLayout.switchOf(r);
            if (locked) g.blit(LOCK, (int) sw.x() + 5, (int) sw.y(), 0f, 0f, 12, 12, 12, 12);
            else if (hasGear(part)) {
                Rect gear = RowLayout.gearOf(r);
                g.blit(GEAR, (int) gear.x(), (int) gear.y(), 0f, 0f, 12, 12, 12, 12);
            }
        }
        g.disableScissor();
        RenderSystem.disableBlend();
    }

    /** A rounded card by the mouse with a row's whole text, for when the row had to cut it short or carries a note. */
    private void card(GuiGraphics g, int mouseX, int mouseY) {
        int wide = 190;
        List<FormattedCharSequence> lines = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        for (FormattedCharSequence s : font.split(Component.literal(hovered.name()), wide)) {
            lines.add(s);
            colours.add(Look.PINK);
        }
        for (FormattedCharSequence s : font.split(Component.literal(hovered.blurb()), wide)) {
            lines.add(s);
            colours.add(Look.TEXT);
        }
        String live = LIVE.containsKey(hovered.id()) ? LIVE.get(hovered.id()).get() : "";
        if (!live.isEmpty()) {
            for (FormattedCharSequence s : font.split(Component.literal(live), wide)) {
                lines.add(s);
                colours.add(Look.PINK_SOFT);
            }
        }
        if (!hoveredNote.text().isEmpty()) {
            for (FormattedCharSequence s : font.split(Component.literal(hoveredNote.text()), wide)) {
                lines.add(s);
                colours.add(Look.PINK_SOFT);
            }
        }
        int w = 0;
        for (FormattedCharSequence s : lines) w = Math.max(w, font.width(s));
        float cw = w + 16, ch = lines.size() * 10 + 10;
        float x = Mth.clamp(mouseX + 10, 4, Math.max(4, width - cw - 4)), y = Mth.clamp(mouseY + 12, 4, Math.max(4, height - ch - 4));
        Soft.shadow(g, x, y + 2, x + cw, y + ch + 2, 8, 6, 0x70000000, false);
        Soft.round(g, x, y, x + cw, y + ch, 8, 0xF83A2244, 0xF82E1B36);
        Soft.ring(g, x, y, x + cw, y + ch, 8, 1, Soft.alpha(Look.PINK, 0.6f));
        for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), (int) x + 8, (int) y + 6 + i * 10, colours.get(i), false);
    }

    // ---- mouse

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.35f));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            if (donePill().has(mx, my)) {
                click();
                onClose();
                return true;
            }
            if (searchPill().has(mx, my)) {
                setFocused(find);
                find.setFocused(true);
                return true;
            }
            Rect area = listArea();
            if (area.has(mx, my)) {
                for (Placed p : placed) {
                    if (p.part() == null) continue;
                    Rect r = shifted(p.rect());
                    if (!r.has(mx, my)) continue;
                    Part part = p.part();
                    if (noteOf(part).locked()) return true;
                    click();
                    if (hasGear(part) && RowLayout.gearOf(r).has(mx, my)) openSettings(part);
                    else Switches.set(part.id(), !Switches.isOn(part.id()));
                    return true;
                }
            }
            find.setFocused(false);
            if (getFocused() == find) setFocused(null);
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll = Mth.clamp(scroll - (float) amount * 16f, 0, most);
        return true;
    }

    /** A part shows a gear when it has a settings screen of its own, it is switched on, and that screen can open here (a
     *  carried mod's own screen only once that mod has started; the Toolbox's own pages always). */
    private boolean hasGear(Part part) {
        return part.settingsScreen() != null && Switches.isOn(part.id()) && (part.kind() == Part.Kind.BUILT_IN || Boolean.TRUE.equals(Switches.onAtStart(part.id())))
                && (!part.settingsNeedWorld() || minecraft.level != null);
    }

    /** The gear: opens the part's own settings screen by its class name, so the Toolbox needs none of that mod's code. */
    private void openSettings(Part part) {
        try {
            Class<?> type = Class.forName(part.settingsScreen());
            Screen screen;
            if (type == PartSettingsScreen.class) {
                minecraft.setScreen(new PartSettingsScreen(this, part.id()));
                return;
            }
            try {
                var withParent = type.getDeclaredConstructor(Screen.class);
                withParent.setAccessible(true);
                screen = (Screen) withParent.newInstance(this);
            } catch (NoSuchMethodException none) {
                var plain = type.getDeclaredConstructor();
                plain.setAccessible(true);
                screen = (Screen) plain.newInstance();
            }
            minecraft.setScreen(screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warn("[Toolbox] Couldn't open the settings of {}", part.id(), e);
        }
    }
}
