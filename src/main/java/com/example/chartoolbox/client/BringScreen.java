package com.example.chartoolbox.client;

import com.example.chartoolbox.model.RowLayout.Rect;
import com.example.chartoolbox.model.suitcase.Group;
import com.example.chartoolbox.model.suitcase.SuitcaseRules;
import com.example.chartoolbox.model.suitcase.SuitcaseStore;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Bring your settings over?": shown once on the title screen of a profile when another profile's settings are waiting
 * in the suitcase. A switch per kind of setting, then Bring, Not now, or Don't ask here.
 */
public class BringScreen extends Screen {
    private static final String TITLE = "Bring your settings over?";
    private static final float ROW_H = 22, ROW_GAP = 3;
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private final Screen parent;
    private final EnumSet<Group> ticked = EnumSet.allOf(Group.class);
    private final Map<Group, Float> slide = new EnumMap<>(Group.class);
    private List<String> result; // what was done; null until Bring is pressed
    private List<Kit.Pill> pills = List.of();
    private long lastFrame;

    public BringScreen(Screen parent) {
        super(Component.literal(TITLE));
        this.parent = parent;
    }

    static String savedLine(SuitcaseStore.Info info) {
        return "Saved " + DAY.format(Instant.ofEpochMilli(info.savedAt())) + " from the profile " + info.profileName();
    }

    private float w() {
        return Math.min(360, width - 40);
    }

    private float h() {
        return Math.min(209, height - 16);
    }

    private float x() {
        return (width - w()) / 2f;
    }

    private float y() {
        return (height - h()) / 2f;
    }

    private Rect row(int i) {
        return new Rect(x() + 12, y() + 50 + i * (ROW_H + ROW_GAP), w() - 24, ROW_H);
    }

    private static Rect switchOf(Rect row) {
        return new Rect(row.x() + row.w() - 8 - 22, row.y() + 5, 22, 12);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Esc before answering counts as "Not now": this profile is asked again next time. */
    @Override
    public void onClose() {
        if (result == null) Suitcase.setMode(SuitcaseRules.afterNotNow());
        leave();
    }

    private void leave() {
        ToolboxClient.afterBring(parent);
    }

    private List<Kit.Pill> makePills() {
        List<Kit.Pill> out = new ArrayList<>();
        float right = x() + w() - 12, py = y() + h() - 26;
        if (result != null) {
            Kit.Pill done = Kit.pill(font, "Done", 0, py, true, this::leave);
            out.add(new Kit.Pill(done.label(), new Rect(right - done.rect().w(), py, done.rect().w(), Kit.PILL_H), true, done.action()));
            return out;
        }
        String[] labels = {"Bring", "Not now", "Don't ask here"};
        Runnable[] actions = {
            () -> result = Suitcase.bring(ticked),
            () -> {
                Suitcase.setMode(SuitcaseRules.afterNotNow());
                leave();
            },
            () -> {
                Suitcase.setMode(SuitcaseRules.afterDontAsk());
                leave();
            }};
        for (int i = 0; i < labels.length; i++) {
            float pw = font.width(labels[i]) + 20;
            right -= pw;
            out.add(new Kit.Pill(labels[i], new Rect(right, py, pw, Kit.PILL_H), i == 0, actions[i]));
            right -= 6;
        }
        return out;
    }

    /** Presses the pill with these words, as a click would. For the self-check. */
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
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(100, now - lastFrame);
        lastFrame = now;
        pills = makePills();
        float x = x(), y = y(), w = w();
        g.drawManaged(() -> {
            Kit.panel(g, x, y, w, h());
            Kit.title(g, font, x, y, result == null ? TITLE : "Your settings are here");
            if (result == null) {
                String line = Suitcase.info().map(BringScreen::savedLine).orElse("Nothing is saved yet.");
                g.drawString(font, Kit.fit(font, line, w - 28), (int) x + 14, (int) y + 34, Look.TEXT_DIM, false);
                Group[] groups = Group.values();
                for (int i = 0; i < groups.length; i++) {
                    Rect r = row(i);
                    boolean over = r.has(mouseX, mouseY);
                    Soft.round(g, r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), 11, over ? Look.ROW_HOVER : Look.ROW);
                    g.drawString(font, groups[i].label, (int) r.x() + 10, (int) r.y() + 7, Look.TEXT, false);
                    Rect sw = switchOf(r);
                    float room = sw.x() - 8 - (r.x() + 104);
                    g.drawString(font, Kit.fit(font, groups[i].blurb, room), (int) r.x() + 104, (int) r.y() + 7, Look.TEXT_DIM, false);
                    float target = ticked.contains(groups[i]) ? 1 : 0, t = slide.getOrDefault(groups[i], target);
                    t = target > t ? Math.min(target, t + dt / 120f) : Math.max(target, t - dt / 120f);
                    slide.put(groups[i], t);
                    Kit.toggle(g, sw, t);
                }
            } else {
                int line = 0;
                for (String said : result) {
                    for (var part : font.split(Component.literal(said), (int) w - 28)) {
                        g.drawString(font, part, (int) x + 14, (int) y + 40 + line++ * 12, Look.TEXT, false);
                    }
                }
                g.drawString(font, "Some things finish after a restart.", (int) x + 14, (int) y + 46 + line * 12, Look.PINK, false);
            }
            for (Kit.Pill p : pills) Kit.pill(g, font, p, mouseX, mouseY);
        });
        Kit.cat(g, x, y);
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
            if (result == null) {
                Group[] groups = Group.values();
                for (int i = 0; i < groups.length; i++) {
                    if (!row(i).has(mx, my)) continue;
                    Kit.click();
                    if (!ticked.remove(groups[i])) ticked.add(groups[i]);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}
