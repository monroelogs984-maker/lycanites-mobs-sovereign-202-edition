package com.lycanitesmobs.core.entity.creature.imp;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Trimmed - same reasoning as EntityAfrit: ranged fireball attack (AttackRangedGoal/
 * fireProjectile) substituted with AttackMeleeGoal since ProjectileManager isn't ported.
 * Dropped the land/leap flight-state machine, petControlsEnabled/bag getters/
 * getFallResistance (tame/equipment, not ported). isFlying() simplified to always true.
 */
public class EntityArix extends TameableCreatureEntity implements Enemy {

    public EntityArix(EntityType<? extends EntityArix> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.setupMob();
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getCommandSenderWorld().isClientSide && !this.hasPerchTarget()) {
            for (int i = 0; i < 2; ++i) {
                this.getCommandSenderWorld().addParticle(ParticleTypes.ITEM_SNOWBALL, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
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
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.type().equals(ObjectManager.getDamageSource(this.level(), "ooze").type())) return true;
        return super.isInvulnerableTo(source);
    }
}
