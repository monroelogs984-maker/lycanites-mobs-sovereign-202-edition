package com.lycanitesmobs.client.gui.screen.block;

import com.lycanitesmobs.core.container.block.EquipmentInfuserContainer;
import com.lycanitesmobs.core.item.equipment.ItemEquipmentPart;
import com.lycanitesmobs.core.item.equipment.imprint.Imprints;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Equipment Infuser: a part or imprinted weapon + charges of a matching element, with level and experience shown. **/
public class EquipmentInfuserScreen extends EquipmentWorkstationScreen<EquipmentInfuserContainer> {
    public EquipmentInfuserScreen(EquipmentInfuserContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void buildInfo(List<InfoLine> lines) {
        ItemStack part = Imprints.getPartOf(this.menu.getFirstStack());
        if (!(part.getItem() instanceof ItemEquipmentPart partItem)) {
            lines.add(status("infuser_empty", ChatFormatting.GRAY));
            return;
        }
        ItemStack charges = this.menu.getSecondStack();
        if (partItem.isAtMaxLevel(part)) {
            lines.add(status("max_level", ChatFormatting.GREEN));
        } else if (!charges.isEmpty() && !partItem.isLevelingChargeItem(charges)) {
            lines.add(status("wrong_element", ChatFormatting.RED));
        } else {
            lines.add(status("insert_charges", ChatFormatting.GRAY));
        }
        lines.add(InfoLine.text(Component.translatable("gui.lycanitesmobs.imprint.elements").append(" ").append(partItem.getElementNames())
                .withStyle(ChatFormatting.LIGHT_PURPLE)));
        if (!partItem.isAtMaxLevel(part)) {
            int experience = partItem.getExperience(part);
            int needed = partItem.getExperienceForNextLevel(part);
            lines.add(InfoLine.bar(Component.translatable("gui.lycanitesmobs.imprint.experience", experience, needed), XP_COLOR, (float) experience / needed));
        }
        this.addPartInfo(lines, part, 0);
    }

    @Override
    protected Component getFirstSlotHint() {
        return Component.translatable("gui.lycanitesmobs.imprint.slot.item");
    }

    @Override
    protected Component getSecondSlotHint() {
        return Component.translatable("gui.lycanitesmobs.imprint.slot.charges");
    }
}
