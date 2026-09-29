package com.lycanitesmobs.core.item.consumable.utility;

import com.lycanitesmobs.core.item.base.BaseItem;
import com.lycanitesmobs.core.util.PortPlaceholder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * PLACEHOLDER: registered with the official names/variants (0 = Standard, 1 = Diamond, 2 = Emerald) so the item
 * exists, but using it only reports that altars aren't ported. The official activates an altar (AltarInfo) on the
 * clicked block; the S202 redesign changes this to one soulkey per boss, so the real version comes with altars.
 */
public class ItemSoulkey extends BaseItem {
    public int variant = 0;

    public ItemSoulkey(Item.Properties properties, String itemName, int variant) {
        super(properties);
        this.itemName = itemName;
        this.variant = variant;
        this.setup();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        PortPlaceholder.notifyNotFunctional(context.getPlayer(), context.getItemInHand().getHoverName(), PortPlaceholder.ALTARS);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        PortPlaceholder.notifyNotFunctional(player, player.getItemInHand(hand).getHoverName(), PortPlaceholder.ALTARS);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), world.isClientSide);
    }
}
