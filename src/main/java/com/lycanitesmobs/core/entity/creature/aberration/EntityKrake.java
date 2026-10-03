package com.lycanitesmobs.core.entity.creature.aberration;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.TemptGoal;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed - TemptGoal dropped (tame/diet-tempt system not ported). canBeLeashed(Player) fixed to
 * 1.21.1's no-arg canBeLeashed(). MobType.UNDEFINED attribute assignment dropped (no such field
 * here). getAISpeedModifier()/waterContact() are already restored on BaseCreatureEntity.
 */
public class EntityKrake extends TameableCreatureEntity implements Enemy {

    public EntityKrake(EntityType<? extends EntityKrake> entityType, Level world) {
        super(entityType, world);
        this.spawnsOnLand = true;
        this.spawnsInWater = true;
        this.hasAttackSound = true;
        this.babySpawnChance = 0.1D;
        this.canGrow = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new TemptGoal(this).setIncludeDiet(true));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false).setRange(1));
    }

    @Override
    public boolean hasLineOfSight(Entity target) {
        if (target == this.getLastHurtByMob() && this.getLastHurtByMobTimestamp() + (3 * 20) > this.tickCount) {
            return true;
        }
        if (target instanceof Player || target instanceof Villager) {
            return target.isSprinting();
        }
        return true;
    }

    @Override
    public float getAISpeedModifier() {
        if (this.isInWater())
            return 1.5F;
        else if (this.waterContact())
            return 1.25F;
        return super.getAISpeedModifier();
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        int waterWeight = 10;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState blockState = this.getCommandSenderWorld().getBlockState(pos);
        if (blockState.getBlock() == Blocks.WATER)
            return (super.getBlockPathWeight(x, y, z) + 1) * (waterWeight + 1);
        if (this.getCommandSenderWorld().isRaining() && this.getCommandSenderWorld().canSeeSkyFromBelowWater(pos))
            return (super.getBlockPathWeight(x, y, z) + 1) * (waterWeight + 1);
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        if (!this.hasAttackTarget())
            return true;
        return super.canBeLeashed();
    }

    public BlockPos getWanderPosition(BlockPos wanderPosition) {
        BlockPos groundPos;
        for (groundPos = wanderPosition.below(); groundPos.getY() > 0 && !this.getCommandSenderWorld().getBlockState(groundPos).isSolid(); groundPos = groundPos.below()) {
        }
        return groundPos.above();
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    @Override
    public int getBagSize() { return this.creatureInfo.getBagSize(); }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() { return 0; }
}
