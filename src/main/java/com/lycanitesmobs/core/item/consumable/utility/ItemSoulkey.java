package com.lycanitesmobs.core.item.consumable.utility;

import com.lycanitesmobs.core.altar.BossAltar;
import com.lycanitesmobs.core.item.base.BaseItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A boss soulkey. S202 redesign: each soulkey belongs to one boss (soulkey = Rahovart, soulkeydiamond = Asmodeus,
 * soulkeyemerald = Amalgalich - matched by colour to the boss pedestals). Used on that boss's pedestal, it takes the
 * player straight into the boss fight (see BossAltar). The official keys were tiers (variant 0/1/2) used on any altar
 * block formation; the variant field is kept for data compatibility but no longer used.
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
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(context.getPlayer() instanceof ServerPlayer player) || !(world instanceof ServerLevel serverLevel)) {
            return BossAltar.forPedestal(world.getBlockState(pos).getBlock()) != null ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        BossAltar pedestalAltar = BossAltar.forPedestal(world.getBlockState(pos).getBlock());
        if (pedestalAltar == null) {
            player.displayClientMessage(Component.translatable("message.soulkey.none"), false);
            return InteractionResult.FAIL;
        }
        BossAltar keyAltar = BossAltar.forSoulkey(this.itemName);
        if (keyAltar != pedestalAltar) {
            player.displayClientMessage(Component.translatable("message.soulkey.invalid"), false);
            return InteractionResult.FAIL;
        }

        ItemStack itemStack = context.getItemInHand();
        if (!pedestalAltar.activate(player, serverLevel, pos)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.soulkey.active"), false);
        return InteractionResult.SUCCESS;
    }
}
