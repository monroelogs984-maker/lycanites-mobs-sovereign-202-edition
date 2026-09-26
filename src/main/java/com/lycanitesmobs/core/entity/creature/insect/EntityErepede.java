package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed: the original extends RideableCreatureEntity and its whole identity is a
 * mountable ranged mud-shooter (AttackRangedGoal/ProjectileManager "mudshot" projectile, a
 * stamina-gated mount ability, speed-boost terrain tags, passenger riding offset). None of the
 * ride/mount/stamina/projectile subsystems are ported, so this is reduced to a plain ground
 * melee attacker - still walks around and fights, just can't be ridden or shoot mud.
 */
public class EntityErepede extends BaseCreatureEntity {

    public EntityErepede(EntityType<? extends EntityErepede> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.setAttackCooldownMax(10);
        this.setupMob();
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.PLAYER));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS)) return true;
        return super.isInvulnerableTo(source);
    }
}
