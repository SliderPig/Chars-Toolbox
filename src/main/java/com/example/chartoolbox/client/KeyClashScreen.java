package com.example.chartoolbox.client;

import com.example.chartoolbox.model.RowLayout.Rect;
import com.example.chartoolbox.model.keys.Bind;
import com.example.chartoolbox.model.keys.Clash;
import com.example.chartoolbox.model.keys.Move;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The Key clash fixer's screen (the gear on its row): one rounded card per key that two actions share, each action with
 * its mod and Change / Clear; "Fix for me" first shows what it will change, then does it; "Undo last fix" puts it back.
 */
public class KeyClashScreen extends Screen {
    private static final String TITLE = "Key clashes";
    private static final float HEAD = 20, LINE = 18, CARD_GAP = 6;

    private final Screen parent;
    private List<Move> preview;       // not null = showing what "fix for me" will change
    private String capturing;         // the action waiting for its new key
    private String message = "";
    private float scroll, most;
    private List<Kit.Pill> pills = List.of(), rowPills = List.of();

    public KeyClashScreen(Screen parent) {
        super(Component.literal(TITLE));
        this.parent = parent;
    }

    private float w() {
        return Math.min(420, width - 40);
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
        float top = y() + 48, bottom = y() + h() - 30;
        return new Rect(x() + 12, top, w() - 24, Math.max(0, bottom - top));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // ---- the pills along the bottom

    private List<Kit.Pill> makePills() {
        List<Kit.Pill> out = new ArrayList<>();
        float py = y() + h() - 23, px = x() + 12;
        if (preview != null) {
            px = add(out, "Do it", px, py, true, () -> {
                int n = preview.size();
                KeyClash.apply(preview);
                preview = null;
                message = n == 1 ? "Done. 1 key moved." : "Done. " + n + " keys moved.";
            });
            add(out, "Cancel", px, py, false, () -> preview = null);
        } else {
            if (!KeyClash.clashes().isEmpty()) px = add(out, "Fix for me", px, py, true, () -> {
                preview = KeyClash.plan();
                capturing = null;
                message = "";
                scroll = 0;
            });
            if (KeyClash.canUndo()) add(out, "Undo last fix", px, py, false, () -> {
                KeyClash.undo();
                message = "Put back as it was.";
            });
        }
        Kit.Pill done = Kit.pill(font, "Done", 0, py, false, this::onClose);
        out.add(new Kit.Pill("Done", new Rect(x() + w() - 12 - done.rect().w(), py, done.rect().w(), Kit.PILL_H), false, done.action()));
        return out;
    }

    private float add(List<Kit.Pill> out, String label, float px, float py, boolean strong, Runnable action) {
        Kit.Pill p = Kit.pill(font, label, px, py, strong, action);
        out.add(p);
        return px + p.rect().w() + 6;
    }

    /** Presses the bottom pill with these words, as a click would. For the self-check. */
    public boolean press(String label) {
        for (Kit.Pill p : makePills()) {
            if (!p.label().equals(label)) continue;
            p.action().run();
            return true;
        }
        return false;
    }

    /** Presses Change or Clear on an action's line. For the self-check. */
    public void pressOn(String id, String label) {
        if (label.equals("Change")) capturing = id;
        else if (label.equals("Clear")) clear(id);
    }

    private void clear(String id) {
        KeyClash.clear(id);
        message = labelOf(id) + " has no key now.";
    }

    private static String labelOf(String id) {
        for (Bind b : KeyClash.binds()) if (b.id().equals(id)) return b.label();
        return id;
    }

    // ---- drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g);
        pills = makePills();
        float x = x(), y = y(), w = w();
        Rect area = area();
        List<Clash> clashes = KeyClash.clashes();
        List<Kit.Pill> rows = new ArrayList<>();
        float content = preview != null ? previewHeight() : cardsHeight(clashes);
        most = Math.max(0, content - area.h());
        scroll = Mth.clamp(scroll, 0, most);
        g.drawManaged(() -> {
            Kit.panel(g, x, y, w, h());
            Kit.title(g, font, x, y, TITLE);
            String sub = !message.isEmpty() ? message : preview != null ? "This is what will change:"
                    : clashes.isEmpty() ? "" : "Pressing one of these keys does two things at once.";
            g.drawString(font, Kit.fit(font, sub, w - 28), (int) x + 14, (int) y + 33, message.isEmpty() ? Look.TEXT_DIM : Look.PINK, false);
            g.enableScissor((int) area.x(), (int) area.y(), (int) Math.ceil(area.x() + area.w()), (int) Math.ceil(area.y() + area.h()));
            if (preview != null) drawPreview(g, area);
            else if (clashes.isEmpty()) {
                String all = "No key has two jobs. All good!";
                g.drawString(font, all, (int) (area.x() + (area.w() - font.width(all)) / 2f), (int) (area.y() + area.h() / 2f - 4), Look.TEXT, false);
            } else drawCards(g, area, clashes, rows, mouseX, mouseY);
            g.disableScissor();
            if (most > 0) {
                float bx = area.x() + area.w() + 4, thumbH = Math.max(14, area.h() * area.h() / (area.h() + most));
                float by = area.y() + (area.h() - thumbH) * (scroll / most);
                Soft.round(g, bx, area.y(), bx + 3, area.y() + area.h(), 1.5f, Soft.alpha(Look.PINK_SOFT, 0.10f));
                Soft.round(g, bx, by, bx + 3, by + thumbH, 1.5f, Soft.alpha(Look.PINK_SOFT, 0.75f));
            }
            for (Kit.Pill p : pills) Kit.pill(g, font, p, mouseX, mouseY);
        });
        rowPills = rows;
        Kit.cat(g, x, y);
    }

    private float cardsHeight(List<Clash> clashes) {
        float total = 0;
        for (Clash c : clashes) total += HEAD + c.binds().size() * LINE + 4 + CARD_GAP;
        return Math.max(0, total - CARD_GAP);
    }

    private void drawCards(GuiGraphics g, Rect area, List<Clash> clashes, List<Kit.Pill> rows, int mouseX, int mouseY) {
        float cy = area.y() - scroll;
        for (Clash c : clashes) {
            float ch = HEAD + c.binds().size() * LINE + 4, x0 = area.x(), x1 = area.x() + area.w();
            if (cy + ch >= area.y() && cy <= area.y() + area.h()) {
                Soft.round(g, x0, cy, x1, cy + ch, 9, Look.ROW);
                // the key itself on a small pink pill, then how many jobs it has
                String key = KeyClash.keyName(c.key());
                float kw = Math.max(18, font.width(key) + 10);
                Soft.round(g, x0 + 6, cy + 4, x0 + 6 + kw, cy + 16, 6, Look.PINK_SOFT);
                g.drawString(font, key, (int) (x0 + 6 + (kw - font.width(key)) / 2f), (int) cy + 6, Look.KNOB_ON, false);
                g.drawString(font, c.binds().size() + " jobs", (int) (x0 + 12 + kw), (int) cy + 6, Look.TEXT_DIM, false);
                float ly = cy + HEAD;
                for (Bind b : c.binds()) {
                    lineOf(g, b, x0, x1, ly, rows, area, mouseX, mouseY);
                    ly += LINE;
                }
            }
            cy += ch + CARD_GAP;
        }
    }

    private void lineOf(GuiGraphics g, Bind b, float x0, float x1, float ly, List<Kit.Pill> rows, Rect area, int mouseX, int mouseY) {
        if (b.id().equals(capturing)) {
            String ask = "Press a key for " + b.label() + " (Esc = stop)";
            g.drawString(font, Kit.fit(font, ask, x1 - x0 - 20), (int) x0 + 10, (int) ly + 4, Look.PINK, false);
            return;
        }
        Kit.Pill clear = Kit.pill(font, "Clear", 0, 0, false, () -> clear(b.id()));
        Kit.Pill change = Kit.pill(font, "Change", 0, 0, false, () -> {
            capturing = b.id();
            message = "";
        });
        float cx = x1 - 6 - clear.rect().w(), chx = cx - 4 - change.rect().w(), py = ly + 1;
        Kit.Pill changeAt = new Kit.Pill("Change", new Rect(chx, py, change.rect().w(), Kit.PILL_H), false, change.action());
        Kit.Pill clearAt = new Kit.Pill("Clear", new Rect(cx, py, clear.rect().w(), Kit.PILL_H), false, clear.action());
        boolean inside = area.has(mouseX, mouseY);
        Kit.pill(g, font, changeAt, inside ? mouseX : -1, inside ? mouseY : -1);
        Kit.pill(g, font, clearAt, inside ? mouseX : -1, inside ? mouseY : -1);
        if (ly + LINE > area.y() && ly < area.y() + area.h()) {
            rows.add(changeAt);
            rows.add(clearAt);
        }
        float room = chx - 8 - (x0 + 10);
        String what = b.label();
        String who = "  " + b.ownerName();
        String mod = KeyModifier.NONE.name().equals(b.modifier()) ? "" : KeyClash.keyName(b.key(), b.modifier()) + "  ";
        String shown = Kit.fit(font, mod + what, room);
        g.drawString(font, shown, (int) x0 + 10, (int) ly + 5, Look.TEXT, false);
        float left = room - font.width(shown);
        if (left > 20) g.drawString(font, Kit.fit(font, who, left), (int) (x0 + 10 + font.width(shown)), (int) ly + 5, Look.TEXT_DIM, false);
    }

    private float previewHeight() {
        return Math.max(1, preview.size()) * (LINE + 4);
    }

    private void drawPreview(GuiGraphics g, Rect area) {
        float ly = area.y() - scroll, x0 = area.x(), x1 = area.x() + area.w();
        if (preview.isEmpty()) {
            g.drawString(font, "Nothing to change.", (int) x0 + 10, (int) ly + 5, Look.TEXT, false);
            return;
        }
        for (Move m : preview) {
            Soft.round(g, x0, ly, x1, ly + LINE, 9, Look.ROW);
            String to = Bind.UNBOUND.equals(m.toKey()) ? "no key (none free)" : KeyClash.keyName(m.toKey());
            String change = KeyClash.keyName(m.fromKey(), m.fromModifier()) + " to " + to;
            float cw = font.width(change);
            g.drawString(font, change, (int) (x1 - 10 - cw), (int) ly + 5, Look.PINK, false);
            String who = m.label() + "  " + m.ownerName();
            g.drawString(font, Kit.fit(font, who, x1 - x0 - 30 - cw), (int) x0 + 10, (int) ly + 5, Look.TEXT, false);
            ly += LINE + 4;
        }
    }

    // ---- mouse and keys

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (capturing != null) {
            if (button != 0) {
                finishCapture(InputConstants.Type.MOUSE.getOrCreate(button));
                return true;
            }
            capturing = null;
        }
        if (button == 0) {
            for (Kit.Pill p : pills) {
                if (!p.rect().has(mx, my)) continue;
                Kit.click();
                p.action().run();
                return true;
            }
            if (area().has(mx, my)) {
                for (Kit.Pill p : rowPills) {
                    if (!p.rect().has(mx, my)) continue;
                    Kit.click();
                    p.action().run();
                    return true;
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

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (capturing != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                capturing = null;
                return true;
            }
            InputConstants.Key k = InputConstants.getKey(key, scan);
            // Shift, Ctrl or Alt on their own: put it on that key for now and keep waiting, so Ctrl + B can be made
            if (KeyModifier.isKeyCodeModifier(k)) {
                KeyClash.set(capturing, k, KeyModifier.NONE);
                return true;
            }
            finishCapture(k);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void finishCapture(InputConstants.Key k) {
        String id = capturing;
        capturing = null;
        KeyModifier mod = KeyModifier.getActiveModifier();
        KeyClash.set(id, k, mod);
        message = labelOf(id) + " is on " + KeyClash.keyName(k.getName(), mod.name()) + " now.";
    }
}
