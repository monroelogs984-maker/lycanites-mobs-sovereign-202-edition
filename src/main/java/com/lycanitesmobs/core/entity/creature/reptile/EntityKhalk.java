package com.lycanitesmobs.core.entity.creature.reptile;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Trimmed - was TameableCreatureEntity implements IGroupHeavy (neither ported, now
 * AgeableCreatureEntity). Dropped the random-lunging aiStep (leap() not ported) and the
 * lava-terraforming die() override (CustomItemEntity/applyDropEffects not ported - block
 * griefing on death is flavor, not core identity). canBreatheUnderwater() renamed to
 * creatureCanBreatheUnderwater() (LivingEntity.canBreatheUnderwater() is final in 1.21.1) - but
 * khalk only overrode canBreatheUnderlava/canBreatheAir, not that one, so unaffected.
 */
public class EntityKhalk extends AgeableCreatureEntity implements Enemy {

    public EntityKhalk(EntityType<? extends EntityKhalk> entityType, Level world) {
        super(entityType, world);
        this.isLavaCreature = true;
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0.01D;
        this.setupMob();
        this.setPathfindingMalus(PathType.LAVA, 0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (this.getCommandSenderWorld().getBlockState(pos).getBlock() == Blocks.LAVA)
            return (super.getBlockPathWeight(x, y, z) + 1) * 11F;

        if (this.getTarget() != null)
            return super.getBlockPathWeight(x, y, z);
        if (this.lavaContact())
            return -999999.0F;

        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBurn() {
        return false;
    }

    @Override
    public boolean waterDamage() {
        return true;
    }

    @Override
    public boolean canBreatheUnderlava() {
        return true;
    }

    @Override
    public boolean canBreatheAir() {
        return true;
    }
}
