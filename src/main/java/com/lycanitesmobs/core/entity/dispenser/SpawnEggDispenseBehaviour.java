package com.lycanitesmobs.core.entity.dispenser;

import com.lycanitesmobs.core.item.consumable.entity.ItemCustomSpawnEgg;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

public class SpawnEggDispenseBehaviour extends DefaultDispenseItemBehavior {
    @Override
    public ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof ItemCustomSpawnEgg itemCustomSpawnEgg))
            return itemStack;

        Position position = DispenserBlock.getDispensePosition(blockSource);
        if (itemCustomSpawnEgg.spawnCreature(blockSource.level(), itemStack, position.x(), position.y(), position.z()) != null) {
            itemStack.shrink(1);
        }
        return itemStack;
    }
}
