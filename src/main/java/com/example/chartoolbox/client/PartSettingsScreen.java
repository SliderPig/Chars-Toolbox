package com.example.chartoolbox.client;

import com.example.chartoolbox.model.Part;
import com.example.chartoolbox.model.Parts;
import com.example.chartoolbox.model.RowLayout.Rect;
import com.example.chartoolbox.model.settings.Setting;
import com.example.chartoolbox.model.settings.Settings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The settings page of a part written in the Toolbox (the gear on its row): one rounded row per setting with a pink
 * switch, a choice that goes round when clicked, a number with - and +, or a field to type in. Changes save at once.
 */
public class PartSettingsScreen extends Screen {
    private static final float ROW_H = 22, GAP = 4;
    /** Extra pills a part adds under its settings ("Place the Sleepy Cat"), by part id. */
    static final Map<String, Map<String, Consumer<Screen>>> EXTRAS = new HashMap<>();

    private final Screen parent;
    private final Part part;
    private final List<Setting> settings;
    private final Map<String, EditBox> fields = new LinkedHashMap<>();
    private final Map<String, Float> slide = new HashMap<>();
    private List<Kit.Pill> pills = List.of();
    private float scroll, most;
    private long lastFrame;

    public PartSettingsScreen(Screen parent, String partId) {
        super(Component.literal("Settings"));
        this.parent = parent;
        this.part = Parts.byId(partId);
        this.settings = Settings.of(partId).stream().filter(Setting::shown).toList();
    }

    private float w() {
        return Math.min(380, width - 40);
    }

    private float h() {
        return Math.min(260, height - 30);
    }

    private float x() {
        return (width - w()) / 2f;
    }

    private float y() {
        return (height - h()) / 2f;
    }

    private Rect area() {
        float top = y() + 36, bottom = y() + h() - 30;
        return new Rect(x() + 12, top, w() - 24, Math.max(0, bottom - top));
    }

    private Rect rowAt(int i) {
        Rect a = area();
        return new Rect(a.x(), a.y() + i * (ROW_H + GAP) - scroll, a.w(), ROW_H);
    }

    private Rect switchIn(Rect r) {
        return new Rect(r.x() + r.w() - 30, r.y() + 5, 22, 12);
    }

    private Rect fieldIn(Rect r) {
        float fw = Math.min(150, r.w() * 0.45f);
        return new Rect(r.x() + r.w() - 8 - fw, r.y() + 3, fw, 16);
    }

    @Override
    protected void init() {
        fields.clear();
        for (int i = 0; i < settings.size(); i++) {
            Setting s = settings.get(i);
            if (s.kind() != Setting.Kind.TEXT) continue;
            Rect f = fieldIn(rowAt(i));
            EditBox box = new EditBox(font, (int) f.x() + 6, (int) f.y() + 4, (int) f.w() - 12, 10, Component.literal(s.label()));
            box.setBordered(false);
            box.setMaxLength(120);
            box.setTextColor(Look.TEXT);
            box.setValue(PartSettings.text(s));
            box.setHint(Component.literal("nothing").withStyle(st -> st.withColor(Look.HINT)));
            box.setResponder(v -> PartSettings.set(s, v.trim()));
            fields.put(s.key(), box);
            addWidget(box);
        }
    }

    @Override
    public void tick() {
        fields.values().forEach(EditBox::tick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private List<Kit.Pill> makePills() {
        List<Kit.Pill> out = new ArrayList<>();
        float py = y() + h() - 23, px = x() + 12;
        for (Map.Entry<String, Consumer<Screen>> e : EXTRAS.getOrDefault(part.id(), Map.of()).entrySet()) {
            Kit.Pill p = Kit.pill(font, e.getKey(), px, py, false, () -> e.getValue().accept(this));
            out.add(p);
            px += p.rect().w() + 6;
        }
        Kit.Pill done = Kit.pill(font, "Done", 0, py, false, this::onClose);
        out.add(new Kit.Pill("Done", new Rect(x() + w() - 12 - done.rect().w(), py, done.rect().w(), Kit.PILL_H), false, done.action()));
        return out;
    }

    /** The pills of a row: a choice's value, or a number's - and +. */
    private List<Kit.Pill> rowPills(Setting s, Rect r) {
        List<Kit.Pill> out = new ArrayList<>();
        if (s.kind() == Setting.Kind.CHOICE) {
            Kit.Pill p = Kit.pill(font, PartSettings.choice(s), 0, 0, false, () -> PartSettings.next(s));
            out.add(new Kit.Pill(p.label(), new Rect(r.x() + r.w() - 8 - p.rect().w(), r.y() + 3, p.rect().w(), Kit.PILL_H), false, p.action()));
        } else if (s.kind() == Setting.Kind.NUMBER) {
            float right = r.x() + r.w() - 8;
            out.add(new Kit.Pill("+", new Rect(right - 18, r.y() + 3, 18, Kit.PILL_H), false, () -> PartSettings.nudge(s, 1)));
            out.add(new Kit.Pill("-", new Rect(right - 18 - 4 - 30 - 4 - 18, r.y() + 3, 18, Kit.PILL_H), false, () -> PartSettings.nudge(s, -1)));
        }
        return out;
    }

    /** Clicks a setting's switch or choice (or a number's +) through the real click handler. For the self-check. */
    public void clickSetting(String key) {
        for (int i = 0; i < settings.size(); i++) {
            Setting s = settings.get(i);
            if (!s.key().equals(key)) continue;
            Rect r = rowAt(i);
            Rect at = s.kind() == Setting.Kind.SWITCH ? switchIn(r) : rowPills(s, r).isEmpty() ? r : rowPills(s, r).get(0).rect();
            mouseClicked(at.x() + at.w() / 2, at.y() + at.h() / 2, 0);
        }
    }

    /** Presses a bottom pill by its words, as a click would. For the self-check. */
    public boolean press(String label) {
        for (Kit.Pill p : makePills()) {
            if (!p.label().equals(label)) continue;
            p.action().run();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        long now = net.minecraft.Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(100, now - lastFrame);
        lastFrame = now;
        pills = makePills();
        Rect area = area();
        most = Math.max(0, settings.size() * (ROW_H + GAP) - GAP - area.h());
        scroll = Mth.clamp(scroll, 0, most);
        for (int i = 0; i < settings.size(); i++) {
            EditBox box = fields.get(settings.get(i).key());
            if (box == null) continue;
            Rect f = fieldIn(rowAt(i));
            box.setX((int) f.x() + 6);
            box.setY((int) f.y() + 4);
            box.visible = f.y() >= area.y() && f.y() + f.h() <= area.y() + area.h();
        }
        float x = x(), y = y(), w = w();
        g.drawManaged(() -> {
            Kit.panel(g, x, y, w, h());
            Kit.title(g, font, x, y, part.name());
            g.enableScissor((int) area.x(), (int) area.y(), (int) Math.ceil(area.x() + area.w()), (int) Math.ceil(area.y() + area.h()));
            if (settings.isEmpty()) g.drawString(font, "Nothing to set here.", (int) area.x() + 6, (int) area.y() + 6, Look.TEXT_DIM, false);
            for (int i = 0; i < settings.size(); i++) row(g, settings.get(i), rowAt(i), mouseX, mouseY, dt, area);
            g.disableScissor();
            // more rows than fit: a slim rounded bar at the right shows there is more, and where you are
            if (most > 0) {
                float bx = area.x() + area.w() + 4, thumbH = Math.max(14, area.h() * area.h() / (area.h() + most));
                float by = area.y() + (area.h() - thumbH) * (scroll / most);
                Soft.round(g, bx, area.y(), bx + 3, area.y() + area.h(), 1.5f, Soft.alpha(Look.PINK_SOFT, 0.10f));
                Soft.round(g, bx, by, bx + 3, by + thumbH, 1.5f, Soft.alpha(Look.PINK_SOFT, 0.75f));
            }
            for (Kit.Pill p : pills) Kit.pill(g, font, p, mouseX, mouseY);
        });
        for (EditBox box : fields.values()) if (box.visible) box.render(g, mouseX, mouseY, partial);
        Kit.cat(g, x, y);
    }

    private void row(GuiGraphics g, Setting s, Rect r, int mouseX, int mouseY, float dt, Rect area) {
        if (r.y() + r.h() < area.y() || r.y() > area.y() + area.h()) return;
        boolean over = area.has(mouseX, mouseY) && r.has(mouseX, mouseY);
        Soft.round(g, r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), 11, over ? Look.ROW_HOVER : Look.ROW);
        float room = r.w() - 20 - switch (s.kind()) {
            case SWITCH -> 34f;
            case CHOICE -> rowPills(s, r).get(0).rect().w() + 12;
            case NUMBER -> 96f;
            case TEXT -> fieldIn(r).w() + 12;
        };
        g.drawString(font, Kit.fit(font, s.label(), room), (int) r.x() + 10, (int) r.y() + 7, Look.TEXT, false);
        switch (s.kind()) {
            case SWITCH -> {
                float target = PartSettings.on(s) ? 1 : 0, t = slide.getOrDefault(s.key(), target);
                t = target > t ? Math.min(target, t + dt / 120f) : Math.max(target, t - dt / 120f);
                slide.put(s.key(), t);
                Kit.toggle(g, switchIn(r), t);
            }
            case CHOICE, NUMBER -> {
                for (Kit.Pill p : rowPills(s, r)) Kit.pill(g, font, p, area.has(mouseX, mouseY) ? mouseX : -1, mouseY);
                if (s.kind() == Setting.Kind.NUMBER) {
                    String v = String.valueOf(PartSettings.number(s));
                    float right = r.x() + r.w() - 8 - 18 - 4;
                    g.drawString(font, v, (int) (right - 15 - font.width(v) / 2f), (int) r.y() + 7, Look.PINK, false);
                }
            }
            case TEXT -> {
                Rect f = fieldIn(r);
                Soft.round(g, f.x(), f.y(), f.x() + f.w(), f.y() + f.h(), 8, Look.FIELD);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (Kit.Pill p : pills) {
                if (!p.rect().has(mx, my)) continue;
                Kit.click();
                p.action().run();
                return true;
            }
            Rect area = area();
            if (area.has(mx, my)) {
                for (int i = 0; i < settings.size(); i++) {
                    Setting s = settings.get(i);
                    Rect r = rowAt(i);
                    if (!r.has(mx, my)) continue;
                    if (s.kind() == Setting.Kind.SWITCH) {
                        Kit.click();
                        PartSettings.flip(s);
                        return true;
                    }
                    for (Kit.Pill p : rowPills(s, r)) {
                        if (!p.rect().has(mx, my)) continue;
                        Kit.click();
                        p.action().run();
                        return true;
                    }
                    if (s.kind() == Setting.Kind.TEXT) {
                        EditBox box = fields.get(s.key());
                        setFocused(box);
                        box.setFocused(true);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        scroll = Mth.clamp(scroll - (float) amount * 16f, 0, most);
        return true;
    }
}
