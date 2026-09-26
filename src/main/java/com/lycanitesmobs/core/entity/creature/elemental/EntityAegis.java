package com.lycanitesmobs.core.entity.creature.elemental;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity implements IFusable (tame/master/bag and
 * the elemental-fusion system, e.g. Aegis+Cinder->Wisp - not ported, needs cross-referencing
 * several sibling elemental classes and a fusion-trigger system that doesn't exist here).
 * Rebased onto BaseCreatureEntity. DefendVillageGoal/DefendEntitiesGoal dropped (not ported).
 * canBeTargetedBy/shouldCreatureGroupHunt/onDamage/getDamageModifier dropped - not real
 * override points on this port's BaseCreatureEntity. The blocking/shield mechanic
 * (setBlocking/isBlocking/canAttackWhileBlocking) is already a real generic system on
 * BaseCreatureEntity, kept as-is with Aegis's own randomized setBlocking() override.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityAegis extends TameableCreatureEntity {

    public EntityAegis(EntityType<? extends EntityAegis> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.getCommandSenderWorld().isClientSide) {
            if (!this.hasAttackTarget() && this.currentBlockingTime < 2) {
                this.setBlocking();
            }
        }
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS) || source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public void setBlocking() {
        this.currentBlockingTime = this.blockingTime + this.getRandom().nextInt(this.blockingTime / 2);
    }
}
