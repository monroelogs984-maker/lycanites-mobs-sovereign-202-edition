package com.lycanitesmobs.core.entity.creature.imp;

import com.lycanitesmobs.core.entity.item.CustomItemEntity;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Trimmed - original's only attack is a ranged fireball via AttackRangedGoal/fireProjectile
 * (ProjectileManager not ported) - substituted AttackMeleeGoal so it isn't defenseless. Dropped
 * the land/leap flight-state machine (isSafeToLand/isSitting/leap not on this port's
 * BaseCreatureEntity), applyDropEffects/getDamageModifier (not real hooks here),
 * getFallResistance/petControlsEnabled/bag getters (tame/equipment, not ported). Kept the
 * cosmetic smoke/flame particle trail and the ground-seeking wander position override.
 * isFlying() simplified to always true since the land-state machine is gone.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityAfrit extends TameableCreatureEntity implements Enemy {

    public EntityAfrit(EntityType<? extends EntityAfrit> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.setupMob();
        this.setPathfindingMalus(PathType.LAVA, 0F);
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackRangedGoal(this).setSpeed(0.75D).setRange(14.0F).setMinChaseDistance(5.0F).setCheckSight(false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getCommandSenderWorld().isClientSide && !this.hasPerchTarget()) {
            for (int i = 0; i < 2; ++i) {
                this.getCommandSenderWorld().addParticle(ParticleTypes.SMOKE, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
                this.getCommandSenderWorld().addParticle(ParticleTypes.FLAME, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public BlockPos getWanderPosition(BlockPos wanderPosition) {
        BlockPos groundPos;
        for (groundPos = wanderPosition.below(); groundPos.getY() > 0 && this.getCommandSenderWorld().getBlockState(groundPos).getBlock() == Blocks.AIR; groundPos = groundPos.below()) {
        }
        if (this.getCommandSenderWorld().getBlockState(groundPos).isSolid()) {
            return groundPos.above();
        }
        return super.getWanderPosition(wanderPosition);
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean isStrongSwimmer() {
        return true;
    }

    @Override
    public boolean canBurn() {
        return false;
    }

    @Override
    public boolean canBreatheUnderlava() {
        return true;
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("scorchfireball", target, range, 0, new Vector3d(0, 0, 0), 0.8f, 2f, 6F);
        super.attackRanged(target, range);
    }

    @Override
    public void applyDropEffects(CustomItemEntity entityItem) {
        entityItem.setCanBurn(false);
    }
}
