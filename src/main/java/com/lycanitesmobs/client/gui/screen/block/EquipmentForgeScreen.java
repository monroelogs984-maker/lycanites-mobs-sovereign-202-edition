package com.lycanitesmobs.client.gui.screen.block;

import com.lycanitesmobs.core.container.block.EquipmentForgeContainer;
import com.lycanitesmobs.core.container.block.EquipmentWorkstationContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Equipment Forge: weapon + part, one Imprint/Extract button, and an info panel saying what it will do or why not. **/
public class EquipmentForgeScreen extends EquipmentWorkstationScreen<EquipmentForgeContainer> {
    protected Button actionButton;

    public EquipmentForgeScreen(EquipmentForgeContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int buttonX = this.leftPos + EquipmentWorkstationContainer.FIRST_SLOT_X - 3;
        int buttonY = this.topPos + EquipmentWorkstationContainer.WORK_SLOT_Y + 30;
        int buttonWidth = EquipmentWorkstationContainer.SECOND_SLOT_X + 18 - EquipmentWorkstationContainer.FIRST_SLOT_X + 4;
        this.actionButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.lycanitesmobs.imprint.button.imprint"), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, EquipmentForgeContainer.BUTTON_ACTION);
            }
        }).bounds(buttonX, buttonY, buttonWidth, 20).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        EquipmentForgeContainer.Action action = this.menu.getAction();
        boolean extract = action == EquipmentForgeContainer.Action.EXTRACT || action == EquipmentForgeContainer.Action.EXTRACT_LEVEL_TOO_HIGH;
        this.actionButton.setMessage(Component.translatable(extract ? "gui.lycanitesmobs.imprint.button.extract" : "gui.lycanitesmobs.imprint.button.imprint"));
        this.actionButton.active = action.possible;
    }

    @Override
    protected void buildInfo(List<InfoLine> lines) {
        int forgeLevel = this.menu.getForgeLevel();
        lines.add(switch (this.menu.getAction()) {
            case EMPTY -> status("forge_empty", ChatFormatting.GRAY);
            case INSERT_PART -> status("insert_part", ChatFormatting.GRAY);
            case INSERT_WEAPON -> status("insert_weapon", ChatFormatting.GRAY);
            case NOT_A_PART -> status("not_imprintable", ChatFormatting.RED);
            case LEVEL_TOO_HIGH -> status("level_too_high", ChatFormatting.RED, forgeLevel);
            case ALREADY_IMPRINTED -> status("already_imprinted", ChatFormatting.RED);
            case IMPRINT -> status("ready_imprint", ChatFormatting.GREEN);
            case EXTRACT -> status("ready_extract", ChatFormatting.GREEN);
            case EXTRACT_LEVEL_TOO_HIGH -> status("extract_level_too_high", ChatFormatting.RED, forgeLevel);
        });
        this.addPartInfo(lines, this.menu.getShownPart(), forgeLevel);
        this.addManaBar(lines, this.menu.getShownPart());
    }

    @Override
    protected Component getFirstSlotHint() {
        return Component.translatable("gui.lycanitesmobs.imprint.slot.weapon");
    }

    @Override
    protected Component getSecondSlotHint() {
        return Component.translatable("gui.lycanitesmobs.imprint.slot.part");
    }
}
