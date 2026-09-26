package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

/**
 * Trimmed: extends BaseCreatureEntity instead of TameableCreatureEntity (not ported) - replaced
 * FindMasterGoal-driven targeting with a plain FindAttackTargetGoal(Player). Dropped StealthGoal/
 * canStealth/startStealth (stealth system not ported) and petControlsEnabled/the tame-only
 * isInvisibleTo override. Dropped the leap-toward-target-before-latching preamble (leap() isn't
 * on BaseCreatureEntity in this trimmed port) - darkling now just latches once it lands a normal
 * melee hit instead of leaping in first. The latch-onto-target mechanic itself (the creature's
 * actual identity) is kept close to verbatim since it's self-contained synced-entity-data logic,
 * just ported to 1.21.1's SynchedEntityData.Builder pattern (see defineSynchedData below).
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityDarkling extends TameableCreatureEntity implements Enemy {

    protected static final EntityDataAccessor<Integer> LATCH_TARGET = SynchedEntityData.defineId(EntityDarkling.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> LATCH_HEIGHT = SynchedEntityData.defineId(EntityDarkling.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> LATCH_ANGLE = SynchedEntityData.defineId(EntityDarkling.class, EntityDataSerializers.FLOAT);

    LivingEntity latchEntity = null;
    int latchEntityID = 0;
    double latchHeight = 0.5D;
    double latchAngle = 90D;

    public EntityDarkling(EntityType<? extends EntityDarkling> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.PLAYER));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LATCH_TARGET, 0);
        builder.define(LATCH_HEIGHT, (float) this.latchHeight);
        builder.define(LATCH_ANGLE, (float) this.latchAngle);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.hasLatchTarget()) {
            this.noPhysics = true;

            Vector3d latchPos = this.getFacingPositionDouble(this.getLatchTarget().position().x(), this.getLatchTarget().position().y() + (this.getLatchTarget().getDimensions(Pose.STANDING).height() * this.latchHeight), this.getLatchTarget().position().z(), this.getLatchTarget().getDimensions(Pose.STANDING).width() * 0.5D, this.latchAngle);
            this.setPos(latchPos.x, latchPos.y, latchPos.z);
            double distanceX = this.getLatchTarget().position().x() - this.position().x();
            double distanceZ = this.getLatchTarget().position().z() - this.position().z();
            float latchYaw = -((float) Mth.atan2(distanceX, distanceZ)) * (180F / (float) Math.PI);
            this.yBodyRot = latchYaw;
            this.setYRot(latchYaw);

            if (!this.getCommandSenderWorld().isClientSide) {
                if (this.getLatchTarget().isAlive() && !this.isInWater()) {
                    this.setTarget(this.getLatchTarget());
                    if (this.updateTick % 40 == 0) {
                        float damage = this.getAttackDamage(1);
                        if (this.attackMelee(this.getLatchTarget(), damage))
                            this.heal(damage * 2);
                    }
                } else {
                    this.setPos(this.getLatchTarget().position().x(), this.getLatchTarget().position().y(), this.getLatchTarget().position().z());
                    this.setLatchTarget(null);
                    this.noPhysics = false;
                }
            } else {
                for (int i = 0; i < 2; ++i) {
                    this.getCommandSenderWorld().addParticle(DustParticleOptions.REDSTONE, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
                }
            }
        } else {
            this.noPhysics = false;
        }
    }

    public LivingEntity getLatchTarget() {
        try {
            if (this.getCommandSenderWorld().isClientSide) {
                this.latchHeight = this.entityData.get(LATCH_HEIGHT);
                this.latchAngle = this.entityData.get(LATCH_ANGLE);
                int latchEntityID = this.getEntityData().get(LATCH_TARGET);
                if (latchEntityID != this.latchEntityID) {
                    this.latchEntity = null;
                    this.latchEntityID = latchEntityID;
                    if (latchEntityID != 0) {
                        Entity possibleLatchEntity = this.getCommandSenderWorld().getEntity(latchEntityID);
                        if (possibleLatchEntity instanceof LivingEntity livingLatchEntity)
                            this.latchEntity = livingLatchEntity;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return this.latchEntity;
    }

    public void setLatchTarget(LivingEntity entity) {
        this.latchEntity = entity;
        if (this.getCommandSenderWorld().isClientSide)
            return;
        if (entity == null) {
            this.getEntityData().set(LATCH_TARGET, 0);
            return;
        }
        this.getEntityData().set(LATCH_TARGET, entity.getId());
        this.latchHeight = 0.25D + (0.75D * this.getRandom().nextDouble());
        this.latchAngle = 360 * this.getRandom().nextDouble();
        this.entityData.set(LATCH_HEIGHT, (float) this.latchHeight);
        this.entityData.set(LATCH_ANGLE, (float) this.latchAngle);
    }

    public boolean hasLatchTarget() {
        return this.getLatchTarget() != null;
    }

    @Override
    public boolean canAttackType(EntityType<?> targetType) {
        if (this.hasLatchTarget())
            return false;
        return super.canAttackType(targetType);
    }

    @Override
    public boolean attackMelee(Entity target, double damageScale) {
        double targetKnockbackResistance = 0;
        if (target instanceof LivingEntity livingTarget) {
            targetKnockbackResistance = livingTarget.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getValue();
            livingTarget.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        }

        if (!super.attackMelee(target, damageScale))
            return false;

        if (target instanceof LivingEntity livingTarget)
            livingTarget.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(targetKnockbackResistance);

        if (!this.hasLatchTarget() && target instanceof LivingEntity livingTarget && !this.isInWater()) {
            this.setLatchTarget(livingTarget);
        }

        return true;
    }

    @Override
    public boolean hasLineOfSight(Entity target) {
        if (target == this.getLatchTarget()) {
            return true;
        }
        return super.hasLineOfSight(target);
    }

    @Override
    public boolean canClimb() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }
}
