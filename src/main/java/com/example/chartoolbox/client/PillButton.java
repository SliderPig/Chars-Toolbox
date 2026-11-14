package com.example.chartoolbox.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** A rounded pill button the Toolbox puts on the game's own screens (never a square box). Its words can change. */
final class PillButton extends AbstractWidget {
    private final Supplier<String> words;
    private final Runnable action;

    PillButton(int x, int y, int w, Supplier<String> words, Runnable action) {
        super(x, y, w, 18, Component.literal(words.get()));
        this.words = words;
        this.action = action;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        String text = words.get();
        setMessage(Component.literal(text));
        boolean over = isHoveredOrFocused();
        var font = Minecraft.getInstance().font;
        g.drawManaged(() -> {
            Soft.shadow(g, getX(), getY() + 1, getX() + width, getY() + height + 1, 9, 4, 0x60000000, false);
            Soft.round(g, getX(), getY(), getX() + width, getY() + height, 9, over ? Look.PINK : 0xFF4A2A55, over ? Look.PINK_DEEP : 0xFF3B2146);
            g.drawString(font, text, getX() + (width - font.width(text)) / 2, getY() + 5, over ? Look.KNOB_ON : Look.TEXT, false);
        });
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        Kit.click();
        action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        defaultButtonNarrationText(out);
    }
}
