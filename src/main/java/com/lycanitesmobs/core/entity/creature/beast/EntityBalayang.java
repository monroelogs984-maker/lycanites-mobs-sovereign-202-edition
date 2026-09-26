package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - the original extends TameableCreatureEntity (not ported yet), so this extends
 * BaseCreatureEntity directly, dropping the MobType.UNDEFINED attribute assignment
 * (BaseCreatureEntity's `attribute` field was dropped in the Phase 5 trim) and the bag-size
 * overrides (getNoBagSize/getBagSize - equipment/bag subsystem isn't ported).
 */
public class EntityBalayang extends BaseCreatureEntity implements Enemy {

    public EntityBalayang(EntityType<? extends EntityBalayang> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.flySoundSpeed = 20;
        this.setupMob();
    }

    // NOTE: 1.21.1 replaced the old setMaxUpStep(float) setter with an overridable
    // maxUpStep() getter (default 0.0F on Entity) - override it directly instead.
    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS)) return true;
        return super.isInvulnerableTo(source);
    }
}
