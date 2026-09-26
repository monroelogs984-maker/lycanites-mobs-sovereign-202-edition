package com.lycanitesmobs.core.entity.creature.reptile;

import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed - dropped TemptGoal (not ported) and the hasMaster() check in canBeLeashed (tame
 * system not ported). Kept the poison-cloud trail (BlockPoisonCloud is already ported).
 */
public class EntityAspid extends AgeableCreatureEntity {

    public EntityAspid(EntityType<? extends EntityAspid> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0.1D;
        this.attackCooldownMax = 10;
        this.isAggressiveByDefault = false;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.getCommandSenderWorld().isClientSide && (this.tickCount % 10 == 0 || this.isMoving() && this.tickCount % 5 == 0)) {
            int trailHeight = this.isBaby() ? 1 : 2;
            for (int y = 0; y < trailHeight; y++) {
                BlockPos pos = this.blockPosition().offset(0, y, 0);
                BlockState blockState = this.getCommandSenderWorld().getBlockState(pos);
                if (blockState.is(LycanitesBlockTags.ASPID_POISON_CLOUD_REPLACEABLE))
                    this.getCommandSenderWorld().setBlockAndUpdate(pos, ObjectManager.getBlock("poisoncloud").defaultBlockState());
            }
        }
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        BlockState blockState = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y - 1, z));
        if (blockState.getBlock() != Blocks.AIR) {
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_GRASS_PREFERRED))
                return 10F;
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_DIRT_PREFERRED))
                return 7F;
        }
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean canBeLeashed() {
        if (!this.hasAttackTarget())
            return true;
        return super.canBeLeashed();
    }
}
