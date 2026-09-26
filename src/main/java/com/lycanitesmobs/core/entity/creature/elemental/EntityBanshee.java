package com.lycanitesmobs.core.entity.creature.elemental;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/bag, not ported).
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityBanshee extends TameableCreatureEntity implements Enemy {

    private int strafeTime = 60;

    public EntityBanshee(EntityType<? extends EntityBanshee> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
        this.noPhysics = true;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.getCommandSenderWorld().isClientSide && this.hasAttackTarget()) {
            if (this.strafeTime-- <= 0) {
                this.strafeTime = 60 + this.getRandom().nextInt(40);
                this.strafe(this.getRandom().nextBoolean() ? -1F : 1F, 0D);
            }
        }

        if (this.getCommandSenderWorld().isClientSide)
            for (int i = 0; i < 2; ++i) {
                this.getCommandSenderWorld().addParticle(ParticleTypes.WITCH, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
            }
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean useDirectNavigator() {
        return true;
    }

    @Override
    public boolean hasLineOfSight(Entity target) {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }
}
