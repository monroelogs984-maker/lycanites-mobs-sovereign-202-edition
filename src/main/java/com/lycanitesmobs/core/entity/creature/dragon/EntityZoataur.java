package com.lycanitesmobs.core.entity.creature.dragon;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - original extends RideableCreatureEntity (mount/stamina/pet-control/bag, not
 * ported) - dropped mountAbility()/getStaminaCost()/petControlsEnabled(). hasRiderTarget()
 * (mount-specific) dropped from onDamage()'s blocking-chance check. spawnsUnderground field
 * and getFallResistance() dropped - not real hooks on this port's BaseCreatureEntity. Random
 * blocking-on-damage mechanic (isBlocking()/setBlocking()/currentBlockingTime) is generic and
 * already on BaseCreatureEntity, kept as-is.
 */
public class EntityZoataur extends AgeableCreatureEntity implements Enemy {

    public EntityZoataur(EntityType<? extends EntityZoataur> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.spreadFire = true;
        this.canGrow = true;
        this.babySpawnChance = 0.1D;
        this.setupMob();
    }

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
    public void aiStep() {
        if (!this.getCommandSenderWorld().isClientSide && this.isBlocking() && this.hasAttackTarget()) {
            this.setTarget(null);
        }
        super.aiStep();
    }

    // NOTE: original @Override's onDamage(DamageSource, float) - a Forge-only LivingEntity hook
    // in 1.20.1 that doesn't exist in vanilla/NeoForge 1.21.1 and isn't restored on this port's
    // BaseCreatureEntity. The random blocking-on-hit mechanic this drove is dropped - isBlocking()
    // still works generically if triggered some other way.

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.CACTUS)) return true;
        return super.isInvulnerableTo(source);
    }
}
