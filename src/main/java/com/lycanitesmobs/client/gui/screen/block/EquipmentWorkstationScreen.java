package com.lycanitesmobs.client.gui.screen.block;

import com.lycanitesmobs.core.container.block.EquipmentWorkstationContainer;
import com.lycanitesmobs.core.item.equipment.ItemEquipment;
import com.lycanitesmobs.core.item.equipment.ItemEquipmentPart;
import com.lycanitesmobs.core.item.equipment.imprint.Imprints;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared look of the S202 equipment workstation screens: a vanilla-style panel drawn in code (no texture), the two
 * work slots on the left, and an info panel on the right that explains the part and what the station will do.
 * Subclasses fill the info panel through {@link #buildInfo}.
 */
public abstract class EquipmentWorkstationScreen<T extends EquipmentWorkstationContainer> extends AbstractContainerScreen<T> {
    protected static final int PANEL_X = 70;
    protected static final int PANEL_Y = 16;
    protected static final int PANEL_WIDTH = EquipmentWorkstationContainer.WIDTH - PANEL_X - 8;
    protected static final int PANEL_HEIGHT = EquipmentWorkstationContainer.INVENTORY_Y - 14 - PANEL_Y;
    protected static final int LINE_HEIGHT = 10;

    protected static final int TEXT_DARK = 0x404040;
    protected static final int MANA_COLOR = 0xFF3F76E4;
    protected static final int XP_COLOR = 0xFF80FF20;

    /** One info panel row: wrapped text, or a bar. **/
    protected record InfoLine(Component text, int barColor, float barFill) {
        static InfoLine text(Component text) {
            return new InfoLine(text, 0, -1);
        }

        static InfoLine bar(Component label, int color, float fill) {
            return new InfoLine(label, color, Math.max(0, Math.min(1, fill)));
        }
    }

    public EquipmentWorkstationScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = EquipmentWorkstationContainer.WIDTH;
        this.imageHeight = EquipmentWorkstationContainer.HEIGHT;
        this.inventoryLabelX = EquipmentWorkstationContainer.INVENTORY_X;
        this.inventoryLabelY = EquipmentWorkstationContainer.INVENTORY_Y - 11;
    }

    /** The rows shown in the info panel for the current slot contents. **/
    protected abstract void buildInfo(List<InfoLine> lines);

    /** Short hints under the work slots (e.g. "Weapon", "Part"). **/
    protected abstract Component getFirstSlotHint();

    protected abstract Component getSecondSlotHint();

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Panel (vanilla container style):
        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF000000);
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFFFFFFFF);
        guiGraphics.fill(x + 3, y + 3, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF555555);
        guiGraphics.fill(x + 3, y + 3, x + this.imageWidth - 3, y + this.imageHeight - 3, 0xFFC6C6C6);

        for (Slot slot : this.menu.slots) {
            drawSlotFrame(guiGraphics, x + slot.x - 1, y + slot.y - 1);
        }

        // Info panel:
        int panelX = x + PANEL_X;
        int panelY = y + PANEL_Y;
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFF373737);
        guiGraphics.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFFFFFFF);
        guiGraphics.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + PANEL_HEIGHT - 1, 0xFF1E1E1E);
    }

    protected static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // Slot hints, centred under each work slot:
        int hintY = EquipmentWorkstationContainer.WORK_SLOT_Y + 19;
        this.drawCentered(guiGraphics, this.getFirstSlotHint(), EquipmentWorkstationContainer.FIRST_SLOT_X + 8, hintY);
        this.drawCentered(guiGraphics, this.getSecondSlotHint(), EquipmentWorkstationContainer.SECOND_SLOT_X + 8, hintY);

        // Info panel rows:
        List<InfoLine> lines = new ArrayList<>();
        this.buildInfo(lines);
        int textX = PANEL_X + 4;
        int textWidth = PANEL_WIDTH - 8;
        int lineY = PANEL_Y + 4;
        int bottom = PANEL_Y + PANEL_HEIGHT - 4;
        for (InfoLine line : lines) {
            if (line.barFill() >= 0) {
                if (lineY + LINE_HEIGHT > bottom) break;
                guiGraphics.drawString(this.font, line.text(), textX, lineY, 0xFFFFFF, false);
                int barX = textX + this.font.width(line.text()) + 4;
                int barRight = PANEL_X + PANEL_WIDTH - 5;
                guiGraphics.fill(barX, lineY + 1, barRight, lineY + 7, 0xFF000000);
                guiGraphics.fill(barX + 1, lineY + 2, barX + 1 + Math.round((barRight - barX - 2) * line.barFill()), lineY + 6, line.barColor());
                lineY += LINE_HEIGHT;
                continue;
            }
            for (FormattedCharSequence wrapped : this.font.split(line.text(), textWidth)) {
                if (lineY + LINE_HEIGHT > bottom) break;
                guiGraphics.drawString(this.font, wrapped, textX, lineY, 0xFFFFFF, false);
                lineY += LINE_HEIGHT;
            }
        }
    }

    protected void drawCentered(GuiGraphics guiGraphics, Component text, int centerX, int y) {
        // Scaled down so the hint fits under an 18 px slot.
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, y, 0);
        guiGraphics.pose().scale(0.75F, 0.75F, 1);
        guiGraphics.drawString(this.font, text, -this.font.width(text) / 2, 0, TEXT_DARK, false);
        guiGraphics.pose().popPose();
    }


    // ==================================================
    //                   Shared Info Rows
    // ==================================================
    /** Part name + level (with an optional cap), its mode, mana bar and what it does. **/
    protected void addPartInfo(List<InfoLine> lines, ItemStack part, int levelCap) {
        if (!(part.getItem() instanceof ItemEquipmentPart partItem)) {
            return;
        }
        // The part's name already includes its level.
        boolean tooHigh = levelCap > 0 && partItem.getPartLevel(part) > levelCap;
        lines.add(InfoLine.text(part.getHoverName().copy().withStyle(tooHigh ? ChatFormatting.RED : ChatFormatting.GOLD)));
        lines.add(InfoLine.text(Component.translatable(partItem.isImprintAbility() ? "gui.lycanitesmobs.imprint.ability" : "gui.lycanitesmobs.imprint.passive")
                .withStyle(ChatFormatting.GRAY)));
        for (MutableComponent summary : Imprints.getFeatureSummaries(part)) {
            lines.add(InfoLine.text(Component.literal(" ").append(summary).withStyle(ChatFormatting.AQUA)));
        }
    }

    protected void addManaBar(List<InfoLine> lines, ItemStack part) {
        if (part.getItem() instanceof ItemEquipmentPart partItem) {
            int mana = partItem.getMana(part);
            lines.add(InfoLine.bar(Component.translatable("gui.lycanitesmobs.imprint.mana", mana, ItemEquipment.MANA_MAX),
                    MANA_COLOR, (float) mana / ItemEquipment.MANA_MAX));
        }
    }

    protected static InfoLine status(String key, ChatFormatting color, Object... args) {
        return InfoLine.text(Component.translatable("gui.lycanitesmobs.imprint.status." + key, args).withStyle(color));
    }
}
