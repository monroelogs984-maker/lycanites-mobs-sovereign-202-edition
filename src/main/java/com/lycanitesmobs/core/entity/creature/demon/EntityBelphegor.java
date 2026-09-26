package com.lycanitesmobs.core.entity.creature.demon;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends TameableCreatureEntity (tame/pet-control/bag, not ported) and is
 * entirely a ranged fireball caster (AttackRangedGoal/fireProjectile need ProjectileManager,
 * not ported) plus door-breaking (BreakDoorGoal not ported) and a client-side hellfire-orb
 * visual sync (EntityRahovart.updateHellfireOrbs, rendering helper, not ported). Substituted a
 * plain melee goal so it isn't left with no attack at all after the ranged trim.
 */
public class EntityBelphegor extends BaseCreatureEntity implements Enemy {

    public EntityBelphegor(EntityType<? extends EntityBelphegor> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityBehemophet)
            return false;
        return super.canAttack(target);
    }

    @Override
    public boolean canBurn() {
        return false;
    }
}
