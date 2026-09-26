package com.lycanitesmobs.client.item;

import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.item.consumable.entity.ItemCustomSpawnEgg;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;

/**
 * Tints spawn egg layers with the creature's egg colors.
 *
 * <p>1.21.1: item tints are ARGB now - a plain RGB int has alpha 0 and renders the layer invisible, so every color
 * goes through FastColor.ARGB32.opaque().
 */
public class ItemColorCustomSpawnEgg implements ItemColor {
    @Override
    public int getColor(ItemStack itemStack, int tintIndex) {
        if (!(itemStack.getItem() instanceof ItemCustomSpawnEgg itemCustomSpawnEgg))
            return -1;

        CreatureInfo creatureInfo = itemCustomSpawnEgg.getCreatureInfo(itemStack);
        if (creatureInfo != null) {
            return FastColor.ARGB32.opaque(tintIndex == 0 ? creatureInfo.getEggBackColor() : creatureInfo.getEggForeColor());
        }
        return FastColor.ARGB32.opaque(tintIndex == 0 ? 0x227744 : 0x11EE44);
    }
}
