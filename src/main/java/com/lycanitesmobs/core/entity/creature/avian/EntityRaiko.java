package com.lycanitesmobs.core.entity.creature.avian;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - was RideableCreatureEntity implements Enemy (now AgeableCreatureEntity). Dropped the
 * entire entity-pickup mechanic (hasPickupEntity() exists on BaseCreatureEntity but
 * pickupEntity()/dropPickupEntity()/canPickupEntity()/getPickupOffset() and the
 * ExtendedEntity capability it relied on are not ported - this was raiko's whole "carry things
 * off" identity, out of scope for a batch port), the land/fly state machine that gated on it,
 * and mount-ability/stamina. isFlying() already comes for free generically from
 * BaseCreatureEntity (JSON "flying" flag via extraMobBehaviour), so no override needed here.
 */
public class EntityRaiko extends AgeableCreatureEntity implements Enemy {

    public EntityRaiko(EntityType<? extends EntityRaiko> entityType, Level world) {
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
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public boolean rollWanderChance() {
        if (this.isFlying())
            return this.getRandom().nextDouble() <= 0.25D;
        return this.getRandom().nextDouble() <= 0.008D;
    }

    @Override
    public boolean isAggressive() {
        if (!this.hasSpawnEvent() && this.getCommandSenderWorld().isDay() && this.testLightLevel() >= 2) {
            return false;
        }
        return super.isAggressive();
    }

    @Override
    public boolean isStrongSwimmer() {
        return false;
    }
}
