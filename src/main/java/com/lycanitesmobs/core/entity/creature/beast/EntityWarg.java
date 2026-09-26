package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - same as EntityFeradon/EntityMaug: the full leap-mount ability (queued leap,
 * landing-paralysis AoE via ObjectManager.getEffect("paralysis"), stamina, rider effects) is
 * dropped entirely, since RideableCreatureEntity is just a stub and there's no generic leap()
 * hook. Dropped MobType.UNDEFINED attribute assignment, getFallResistance, petControlsEnabled,
 * getNoBagSize/getBagSize.
 */
public class EntityWarg extends RideableCreatureEntity {

    public EntityWarg(EntityType<? extends EntityWarg> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.spreadFire = false;
        this.canGrow = true;
        this.babySpawnChance = 0.1D;
        this.setupMob();
        this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }
}
