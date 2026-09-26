package com.lycanitesmobs.core.entity.creature.amphibian;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Trimmed - originally extended TameableCreatureEntity (not ported); uses AgeableCreatureEntity
 * directly instead since only canGrow/babySpawnChance were actually needed. Dropped:
 * MobType.UNDEFINED attribute assignment (attribute field not ported), spawnsOnLand/
 * spawnsInWater flags (not ported - JSON-driven CreatureSpawnConfig already handles placement),
 * bag-size overrides and petControlsEnabled (equipment/tame systems not ported), the random-leap
 * behaviour and water AI-speed-boost (leap()/getAISpeedModifier()/waterContact() hooks aren't
 * present on the trimmed BaseCreatureEntity - not worth restoring them there just for flavour).
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityAglebemu extends TameableCreatureEntity implements Enemy {

    public EntityAglebemu(EntityType<? extends EntityAglebemu> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.spreadFire = false;
        this.canGrow = true;
        this.babySpawnChance = 0.01D;
        this.setupMob();
        this.setPathfindingMalus(PathType.WATER, 0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setRange(3));
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
    public boolean canBreatheAir() {
        return true;
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }

    // Original had getFallResistance()=100 (a custom hook not on the trimmed
    // BaseCreatureEntity); overriding vanilla's own fall-damage hook directly instead.
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }
}
