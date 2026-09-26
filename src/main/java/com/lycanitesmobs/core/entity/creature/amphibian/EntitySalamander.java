package com.lycanitesmobs.core.entity.creature.amphibian;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Trimmed - originally extended RideableCreatureEntity (the mount/stamina/rider-ability system
 * isn't ported at all in this port). Dropped entirely: riderEffects/mountAbility/specialAttack/
 * getStaminaCost/getStaminaRecoveryWarmup/getStaminaRecoveryMax/onDismounted/
 * getPassengersRidingOffset/canStandOnFluid (all mount-specific), ExtendedPlayer capability
 * usage (not ported), applyDropEffects/getBrightness/solidCollision (custom hooks not on the
 * trimmed BaseCreatureEntity, purely cosmetic - not worth restoring there just for this).
 * getFallResistance() replaced with a direct causeFallDamage() override (see EntityAglebemu).
 */
public class EntitySalamander extends AgeableCreatureEntity implements Enemy {

    public EntitySalamander(EntityType<? extends EntitySalamander> entityType, Level world) {
        super(entityType, world);
        this.isLavaCreature = true;
        this.hasAttackSound = true;
        this.hasJumpSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0.01D;
        this.setupMob();
        // setMaxUpStep(float) doesn't exist in 1.21.1 - maxUpStep() is now derived from the
        // STEP_HEIGHT attribute (setupMob() already sets a 0.5 base default), so override it
        // directly here instead.
        this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT).setBaseValue(1.0D);
        this.setPathfindingMalus(PathType.LAVA, 0F);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        int waterWeight = 10;
        BlockPos pos = new BlockPos(x, y, z);
        if (this.getCommandSenderWorld().getBlockState(pos).getBlock() == Blocks.LAVA)
            return (super.getBlockPathWeight(x, y, z) + 1) * (waterWeight + 1);
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
        return false;
    }

    @Override
    public boolean canBreatheUnderlava() {
        return true;
    }

    @Override
    public boolean canBreatheAir() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }
}
