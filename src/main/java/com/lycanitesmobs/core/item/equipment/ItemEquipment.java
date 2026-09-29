package com.lycanitesmobs.core.item.equipment;

import com.lycanitesmobs.core.item.base.BaseItem;
import com.lycanitesmobs.core.util.PortPlaceholder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * PLACEHOLDER for the official assembled equipment item (parts combined at an Equipment Forge; 1055 lines of
 * damage/harvest/projectile/summon behaviour and an OBJ renderer that draws the assembled parts). Registered so the
 * item id exists; it has no parts, so it only reports that the equipment system isn't ported. The S202 redesign
 * replaces assembled equipment with a socket system (not in the first release), so this may never be ported as-is.
 */
public class ItemEquipment extends BaseItem {
    public static final int SHARPNESS_MAX = 1500;
    public static final int MANA_MAX = 1500;

    public ItemEquipment(Item.Properties properties) {
        super(properties);
        this.itemName = "equipment";
        this.setup();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        PortPlaceholder.notifyNotFunctional(player, player.getItemInHand(hand).getHoverName(), PortPlaceholder.EQUIPMENT);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), world.isClientSide);
    }
}
