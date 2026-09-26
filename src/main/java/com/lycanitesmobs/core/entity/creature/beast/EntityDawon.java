package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.level.Level;

/**
 * Trimmed - the original extends TameableCreatureEntity (not ported), dropping
 * MobType.UNDEFINED/getNoBagSize/getBagSize/petControlsEnabled the same as EntityBalayang.
 * Also dropped: the random-leap aiStep behavior (leap() isn't on BaseCreatureEntity - not added
 * here since this batch must not touch shared base-entity files while other creatures are being
 * ported in parallel) and the getFallResistance() immunity override (same reason).
 */
public class EntityDawon extends BaseCreatureEntity {

    public EntityDawon(EntityType<? extends EntityDawon> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
        this.attackCooldownMax = 15;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(ZombifiedPiglin.class).setSpeed(1.5D).setDamageScale(8.0D).setRange(2.5D));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setSpeed(1.5D));
    }
}
