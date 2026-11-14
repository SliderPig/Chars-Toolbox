package com.example.chartoolbox.client;

import com.example.chartoolbox.model.RowLayout.Rect;
import com.example.chartoolbox.model.cat.SleepyArt;
import com.example.chartoolbox.model.settings.Settings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** "Place the Sleepy Cat": the game shows through; drag the cat where you want it to sleep. Saved when you let go. */
public class CatPlaceScreen extends Screen {
    private static final String HINT = "Drag the cat where you want it to sleep";

    private final Screen parent;
    private boolean dragging;
    private int grabX, grabY, catX, catY;
    private List<Kit.Pill> pills = List.of();

    public CatPlaceScreen(Screen parent) {
        super(Component.literal("Place the Sleepy Cat"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<Integer> at = SleepyCat.spot(width, height);
        catX = at.get(0);
        catY = at.get(1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    /** Where the cat is on this screen, as {x, y} (for the self-check). */
    int[] cat() {
        return new int[] {catX, catY};
    }

    private Rect catRect() {
        int s = SleepyCat.scale();
        return new Rect(catX - 2, catY - SleepyArt.H * s - 2, SleepyArt.W * s + 4, (SleepyArt.H + SleepyArt.TAIL) * s + 4);
    }

    private List<Kit.Pill> makePills() {
        List<Kit.Pill> out = new ArrayList<>();
        float py = 34; // under the hint at the top, clear of the hotbar
        Kit.Pill done = Kit.pill(font, "Done", 0, py, true, this::onClose);
        Kit.Pill back = Kit.pill(font, "Back to its corner", 0, py, false, () -> {
            PartSettings.set(Settings.SLEEPY_X, "-1");
            PartSettings.set(Settings.SLEEPY_Y, "-1");
            init();
        });
        float total = done.rect().w() + 6 + back.rect().w(), x = (width - total) / 2f;
        out.add(new Kit.Pill(back.label(), new Rect(x, py, back.rect().w(), Kit.PILL_H), false, back.action()));
        out.add(new Kit.Pill(done.label(), new Rect(x + back.rect().w() + 6, py, done.rect().w(), Kit.PILL_H), true, done.action()));
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        pills = makePills();
        Rect c = catRect();
        int s = SleepyCat.scale();
        float hw = font.width(HINT) + 24, hx = (width - hw) / 2f;
        g.drawManaged(() -> {
            Soft.round(g, hx, 10, hx + hw, 28, 9, 0xE024152B, 0xE01C1022);
            g.drawString(font, HINT, (int) hx + 12, 15, Look.TEXT, false);
            // a soft pink glow round the cat shows what to grab
            Soft.round(g, c.x() - 3, c.y() - 3, c.x() + c.w() + 3, c.y() + c.h() + 3, 8, Soft.alpha(Look.PINK, dragging ? 0.35f : 0.18f));
            SleepyCat.draw(g, catX, catY, s, 1f);
            for (Kit.Pill p : pills) Kit.pill(g, font, p, mouseX, mouseY);
        });
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        for (Kit.Pill p : pills) {
            if (!p.rect().has(mx, my)) continue;
            Kit.click();
            p.action().run();
            return true;
        }
        if (catRect().has(mx, my)) {
            dragging = true;
            grabX = (int) mx - catX;
            grabY = (int) my - catY;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!dragging) return super.mouseDragged(mx, my, button, dx, dy);
        List<Integer> at = SleepyArt.clampSpot((int) mx - grabX, (int) my - grabY, SleepyCat.scale(), width, height);
        catX = at.get(0);
        catY = at.get(1);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            PartSettings.set(Settings.SLEEPY_X, String.valueOf(catX));
            PartSettings.set(Settings.SLEEPY_Y, String.valueOf(catY));
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }
}
