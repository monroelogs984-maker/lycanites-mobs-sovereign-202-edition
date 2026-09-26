package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed: the original extends RideableCreatureEntity and its whole identity is a
 * mountable ranged poison-laser shooter (AttackRangedGoal/mount-ability both using
 * ProjectileManager's "poisonray", none of which is ported). Reduced to a plain melee attacker
 * with a short leash range (matching the original's melee goal's 4-block max chase). Kept
 * isStrongSwimmer/isPushedByFluid (real swimming behavior, no unported dependency) and the
 * daytime-lurking isAggressive check (uses testLightLevel()/isDaytime(), already available).
 * Dropped canBreatheUnderwater (LivingEntity.canBreatheUnderwater() is final/tag-driven in
 * 1.21.1, can't be overridden - see BaseCreatureEntity's own note on this), getFallResistance
 * and petControlsEnabled (not on BaseCreatureEntity), and the tame-gated isAggressive branch.
 */
public class EntityEyewig extends BaseCreatureEntity {

    public EntityEyewig(EntityType<? extends EntityEyewig> entityType, Level world) {
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
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false).setMaxChaseDistanceSq(4.0F));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.PLAYER));
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isAggressive() {
        if (this.getCommandSenderWorld() != null && this.getCommandSenderWorld().isDay())
            return this.testLightLevel() < 2;
        return super.isAggressive();
    }

    @Override
    public boolean canClimb() {
        return true;
    }

    @Override
    public boolean isStrongSwimmer() {
        return true;
    }
}
