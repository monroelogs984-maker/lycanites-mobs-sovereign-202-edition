package com.lycanitesmobs.client.gui.screen.block;

import com.lycanitesmobs.core.container.block.EquipmentStationContainer;
import com.lycanitesmobs.core.item.equipment.ItemEquipment;
import com.lycanitesmobs.core.item.equipment.ItemEquipmentPart;
import com.lycanitesmobs.core.item.equipment.imprint.Imprints;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Equipment Station: a part or imprinted weapon + mana items, with the mana bar shown. **/
public class EquipmentStationScreen extends EquipmentWorkstationScreen<EquipmentStationContainer> {
    public EquipmentStationScreen(EquipmentStationContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void buildInfo(List<InfoLine> lines) {
        ItemStack part = Imprints.getPartOf(this.menu.getFirstStack());
        if (!(part.getItem() instanceof ItemEquipmentPart partItem)) {
            lines.add(status("station_empty", ChatFormatting.GRAY));
            return;
        }
        if (partItem.getMana(part) >= ItemEquipment.MANA_MAX) {
            lines.add(status("mana_full", ChatFormatting.GREEN));
        } else {
            lines.add(status("insert_mana", ChatFormatting.GRAY));
        }
        this.addManaBar(lines, part);
        this.addPartInfo(lines, part, 0);
    }

    @Override
    protected Component getFirstSlotHint() {
        return Component.translatable("gui.lycanitesmobs.imprint.slot.item");
    }

    @Override
    protected Component getSecondSlotHint() {
        return Component.translatable("gui.lycanitesmobs.imprint.slot.mana");
    }
}
