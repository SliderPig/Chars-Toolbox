package com.example.chartoolbox.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** The small round black-cat button on the pause screen that opens the Toolbox. */
final class CatButton extends AbstractWidget {
    static final int SIZE = 20;

    CatButton(int x, int y) {
        super(x, y, SIZE, SIZE, Component.literal("Char's Toolbox"));
        setTooltip(Tooltip.create(Component.literal("Char's Toolbox")));
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        float cx = getX() + SIZE / 2f, cy = getY() + SIZE / 2f;
        g.drawManaged(() -> {
            Soft.shadow(g, getX(), getY() + 1, getX() + SIZE, getY() + SIZE + 1, SIZE / 2f, 4, 0x70000000, false);
            Soft.disc(g, cx, cy, SIZE / 2f, isHoveredOrFocused() ? Look.PINK : Look.PINK_SOFT);
        });
        g.blit(ToolboxScreen.CAT, getX() + 2, getY() + 2, 0f, 0f, 16, 16, 16, 16);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        ToolboxClient.open();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        defaultButtonNarrationText(out);
    }
}
