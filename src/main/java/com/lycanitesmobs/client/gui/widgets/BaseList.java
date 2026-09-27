package com.lycanitesmobs.client.gui.widgets;

import com.lycanitesmobs.client.util.helpers.DrawHelper;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.util.Mth;

/**
 * Base scrolling list for Lycanites screens.
 *
 * <p>1.21 port: vanilla's AbstractSelectionList became a positioned widget (constructor is width, height, y,
 * itemHeight; x0/x1/y0/y1 are now getX()/getRight()/getY()/getBottom(); render is renderWidget/renderListItems), and its
 * Entry class is only reachable through ObjectSelectionList, so this now extends that. The original drew its panel and
 * scrollbar with raw tessellator quads; the same look (translucent black panel, black track, grey handle) is drawn with
 * GuiGraphics fills. The original constructor arguments (width, screen height, top, bottom, left) are kept.
 */
public abstract class BaseList<S> extends ObjectSelectionList<BaseListEntry> {
    public DrawHelper drawHelper;
    public S screen;

    public BaseList(S screen, int width, int height, int top, int bottom, int left, int slotHeight) {
        super(Minecraft.getInstance(), width, bottom - top, top, slotHeight);
        Minecraft minecraft = Minecraft.getInstance();
        this.drawHelper = new DrawHelper(minecraft, minecraft.font);
        this.setX(left);
        this.screen = screen;
        this.createEntries();
    }

    public BaseList(S screen, int width, int height, int top, int bottom, int left) {
        this(screen, width, height, top, bottom, left, 28);
    }

    @Override
    public int getRowWidth() {
        return this.getWidth();
    }

    protected int getScrollbarWidth() {
        return 6;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRight() - this.getScrollbarWidth();
    }

    /**
     * Creates all List Entries for this List Widget.
     */
    public void createEntries() {
    }

    /**
     * Returns the index of the selected entry.
     *
     * @return The selected entry index, defaults to 0 if none are selected.
     */
    public int getSelectedIndex() {
        if (this.getSelected() != null)
            return this.getSelected().index;
        return 0;
    }

    /**
     * Draws behind the list entries, lists override this for custom backgrounds.
     */
    protected void renderBackground(GuiGraphics guiGraphics) {
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        guiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
        try {
            guiGraphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x40000000);

            try {
                this.renderListItems(guiGraphics, mouseX, mouseY, partialTicks);
            } catch (Exception e) {
                LMHelperClass.logError("BaseList renderList error: " + e.getMessage());
            }

            int maxScroll = this.getMaxScroll();
            if (maxScroll > 0) {
                int trackHeight = this.getBottom() - this.getY();
                int handleHeight = (int) ((float) trackHeight * trackHeight / (float) this.getMaxPosition());
                handleHeight = Mth.clamp(handleHeight, 32, trackHeight - 8);
                int handleY = (int) this.getScrollAmount() * (trackHeight - handleHeight) / maxScroll + this.getY();
                if (handleY < this.getY()) {
                    handleY = this.getY();
                }

                int scrollbarLeft = this.getScrollbarPosition();
                int scrollbarRight = scrollbarLeft + this.getScrollbarWidth();
                guiGraphics.fill(scrollbarLeft, this.getY(), scrollbarRight, this.getBottom(), 0xFF000000);
                guiGraphics.fill(scrollbarLeft, handleY, scrollbarRight, handleY + handleHeight, 0xFF808080);
                guiGraphics.fill(scrollbarLeft, handleY, scrollbarRight - 1, handleY + handleHeight - 1, 0xFFC0C0C0);
            }

            this.renderDecorations(guiGraphics, mouseX, mouseY);
        } finally {
            guiGraphics.disableScissor();
        }
    }
}
