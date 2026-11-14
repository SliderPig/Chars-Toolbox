package com.example.chartoolbox.client;

import com.example.chartoolbox.model.RowLayout.Rect;
import com.example.chartoolbox.model.suitcase.SuitcaseRules.Mode;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The settings of "Bring my settings" (the gear on its row): what is saved, how this profile deals with the suitcase,
 * and pills to save now, bring now, keep this profile apart, or delete what is saved.
 */
public class SuitcaseScreen extends Screen {
    private static final String TITLE = "Bring my settings";

    private final Screen parent;
    private String message = "";
    private long deleteArmedAt;
    private List<Kit.Pill> pills = List.of();

    public SuitcaseScreen(Screen parent) {
        super(Component.literal(TITLE));
        this.parent = parent;
    }

    private float w() {
        return Math.min(360, width - 40);
    }

    private float h() {
        return Math.min(156, height - 16);
    }

    private float x() {
        return (width - w()) / 2f;
    }

    private float y() {
        return (height - h()) / 2f;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static String modeLine() {
        if (!Suitcase.on()) return "Bring my settings is switched off.";
        Mode mode = Suitcase.mode();
        if (mode == Mode.SHARE) return "This profile saves its settings each time the game closes.";
        if (mode == Mode.ASK) return "This profile hasn't answered yet, so it saves nothing.";
        return "This profile keeps its own settings and saves nothing.";
    }

    private boolean deleteArmed() {
        return deleteArmedAt != 0 && Util.getMillis() - deleteArmedAt < 3000;
    }

    private List<Kit.Pill> makePills() {
        List<Kit.Pill> out = new ArrayList<>();
        float px = x() + 12, py = y() + h() - 48;
        px = add(out, "Save now", px, py, true, () -> message = Suitcase.saveNow());
        if (Suitcase.info().isPresent() && !Suitcase.ours()) {
            px = add(out, "Bring now", px, py, false, () -> minecraft.setScreen(new BringScreen(this)));
        }
        if (Suitcase.mode() == Mode.APART) {
            add(out, "Share this profile again", px, py, false, () -> {
                Suitcase.setMode(Mode.SHARE);
                message = "This profile saves when the game closes.";
            });
        } else {
            add(out, "Keep this profile apart", px, py, false, () -> {
                Suitcase.setMode(Mode.APART);
                message = "This profile keeps its own settings now.";
            });
        }
        float row2 = y() + h() - 26;
        if (Suitcase.info().isPresent()) {
            add(out, deleteArmed() ? "Click again to delete" : "Delete saved settings", x() + 12, row2, false, () -> {
                if (deleteArmed()) {
                    deleteArmedAt = 0;
                    message = Suitcase.delete();
                } else {
                    deleteArmedAt = Util.getMillis();
                }
            });
        }
        Kit.Pill done = Kit.pill(font, "Done", 0, row2, false, this::onClose);
        out.add(new Kit.Pill("Done", new Rect(x() + w() - 12 - done.rect().w(), row2, done.rect().w(), Kit.PILL_H), false, done.action()));
        return out;
    }

    private float add(List<Kit.Pill> out, String label, float px, float py, boolean strong, Runnable action) {
        Kit.Pill p = Kit.pill(font, label, px, py, strong, action);
        out.add(p);
        return px + p.rect().w() + 6;
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
        pills = makePills();
        float x = x(), y = y(), w = w();
        g.drawManaged(() -> {
            Kit.panel(g, x, y, w, h());
            Kit.title(g, font, x, y, TITLE);
            String saved = Suitcase.info().map(BringScreen::savedLine).orElse("Nothing saved yet.");
            g.drawString(font, Kit.fit(font, saved, w - 28), (int) x + 14, (int) y + 36, Look.TEXT, false);
            g.drawString(font, Kit.fit(font, modeLine(), w - 28), (int) x + 14, (int) y + 50, Look.TEXT_DIM, false);
            g.drawString(font, Kit.fit(font, "Kept on this PC only, in " + Suitcase.store().root(), w - 28), (int) x + 14, (int) y + 64, Look.TEXT_DIM, false);
            if (!message.isEmpty()) g.drawString(font, Kit.fit(font, message, w - 28), (int) x + 14, (int) y + 82, Look.PINK, false);
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
        }
        return super.mouseClicked(mx, my, button);
    }
}
