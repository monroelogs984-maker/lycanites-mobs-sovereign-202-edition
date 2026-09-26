package com.lycanitesmobs.core.item.block;

import com.lycanitesmobs.core.item.base.BaseItem;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * An item that places an effect block (cloud, fire) against the clicked face, e.g. Frosty Fur places Frost Cloud.
 */
public class ItemBlockPlacer extends BaseItem {
    public String placedBlockName;

    public ItemBlockPlacer(Item.Properties properties, String itemName, String placedBlockName) {
        super(properties);
        this.itemName = itemName;
        this.placedBlockName = placedBlockName;
        this.setup();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Block block = ObjectManager.getBlock(this.placedBlockName);
        if (block == null || (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), itemStack))) {
            return InteractionResult.FAIL;
        }

        if (world.getBlockState(pos).isAir()) {
            SoundEvent sound = ObjectManager.getSound(this.placedBlockName);
            if (sound != null) {
                world.playSound(null, pos, sound, SoundSource.PLAYERS, 1.0F, world.getRandom().nextFloat() * 0.4F + 0.8F);
            }
            world.setBlockAndUpdate(pos, block.defaultBlockState());
        }
        if (player == null || !player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
