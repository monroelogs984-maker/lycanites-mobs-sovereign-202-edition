package com.lycanitesmobs.client.event;

import com.lycanitesmobs.core.item.equipment.ItemEquipment;
import com.lycanitesmobs.core.item.equipment.ItemEquipmentPart;
import com.lycanitesmobs.core.item.equipment.imprint.Imprints;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * S202 equipment rework: the imprint is only shown in the expanded tooltip (hold Shift): the part, its level, its mana
 * and what it does. Without Shift the item's tooltip is unchanged.
 */
public class ImprintTooltip {
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack host = event.getItemStack();
        if (!Imprints.hasImprint(host) || !Screen.hasShiftDown()) {
            return;
        }
        ItemStack part = Imprints.getPart(host);
        if (!(part.getItem() instanceof ItemEquipmentPart partItem)) {
            return;
        }
        int level = partItem.getPartLevel(part);
        int mana = partItem.getMana(part);
        List<Component> tooltip = event.getToolTip();

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("imprint.lycanitesmobs.part").append(" ").append(part.getHoverName())
                .append(" ").append(Component.translatable("imprint.lycanitesmobs.level", level)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("imprint.lycanitesmobs.mana", mana, ItemEquipment.MANA_MAX)
                .withStyle(mana > 0 ? ChatFormatting.BLUE : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(partItem.isImprintAbility() ? "imprint.lycanitesmobs.ability" : "imprint.lycanitesmobs.passive")
                .withStyle(ChatFormatting.GRAY));
        for (MutableComponent summary : Imprints.getFeatureSummaries(part)) {
            tooltip.add(Component.literal("  ").append(summary).withStyle(mana > 0 ? ChatFormatting.DARK_AQUA : ChatFormatting.DARK_GRAY));
        }
        if (mana <= 0) {
            tooltip.add(Component.translatable("imprint.lycanitesmobs.inactive").withStyle(ChatFormatting.RED));
        }
    }
}
