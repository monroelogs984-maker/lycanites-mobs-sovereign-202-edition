package com.lycanitesmobs.core.entity.dispenser;

import com.lycanitesmobs.core.data.info.projectile.ProjectileInfo;
import com.lycanitesmobs.core.entity.base.BaseProjectileEntity;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;

/**
 * Fires a charge item's projectile from a dispenser.
 *
 * <p>1.21.1: vanilla's AbstractProjectileDispenseBehavior became ProjectileDispenseBehavior, which needs the item to
 * be a ProjectileItem - this extends DefaultDispenseItemBehavior directly instead. Old-projectile-class dispensing
 * is dropped, every charge is a JSON projectile now.
 */
public class BaseProjectileDispenseBehaviour extends DefaultDispenseItemBehavior {
    protected ProjectileInfo projectileInfo;

    public BaseProjectileDispenseBehaviour(ProjectileInfo projectileInfo) {
        super();
        this.projectileInfo = projectileInfo;
    }

    @Override
    public ItemStack execute(BlockSource blockSource, ItemStack stack) {
        Position position = DispenserBlock.getDispensePosition(blockSource);
        Direction facing = blockSource.state().getValue(DispenserBlock.FACING);

        BaseProjectileEntity projectile = this.projectileInfo.createProjectile(blockSource.level(), position.x(), position.y(), position.z());
        if (projectile == null)
            return stack;

        projectile.shoot(facing.getStepX(), facing.getStepY(), facing.getStepZ(), (float) this.projectileInfo.getVelocity(), 0F);
        DeferredLevelActionManager.spawnEntity(blockSource.level(), blockSource.pos(), null, projectile);
        stack.shrink(1);
        return stack;
    }

    @Override
    protected void playSound(BlockSource blockSource) {
        SoundEvent soundEvent = this.projectileInfo.getLaunchSound();
        if (soundEvent == null)
            return;
        blockSource.level().playSound(null, blockSource.pos(), soundEvent, SoundSource.AMBIENT, 1.0F, 1.0F / (blockSource.level().getRandom().nextFloat() * 0.4F + 0.8F));
    }
}
