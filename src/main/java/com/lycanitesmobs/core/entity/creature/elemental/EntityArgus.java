package com.lycanitesmobs.core.entity.creature.elemental;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity implements IFusable (tame/bag/fusion, not
 * ported - see EntityAegis). StealthGoal dropped (not ported, same as EntityWraamon). Ranged
 * chaosorb attack (AttackRangedGoal/attackRanged/fireProjectile) dropped - needs
 * ProjectileManager, not ported at all yet; Argus ends up melee-only.
 */
public class EntityArgus extends BaseCreatureEntity implements Enemy {

    public EntityArgus(EntityType<? extends EntityArgus> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
        this.setAttackCooldownMax(40);
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true).setMaxChaseDistanceSq(5.0F));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getCommandSenderWorld().isClientSide)
            for (int i = 0; i < 2; ++i) {
                this.getCommandSenderWorld().addParticle(ParticleTypes.PORTAL, this.position().x() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), this.position().y() + this.random.nextDouble() * (double) this.getDimensions(Pose.STANDING).height(), this.position().z() + (this.random.nextDouble() - 0.5D) * (double) this.getDimensions(Pose.STANDING).width(), 0.0D, 0.0D, 0.0D);
            }
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
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean creatureCanBreatheUnderwater() {
        return true;
    }
}
