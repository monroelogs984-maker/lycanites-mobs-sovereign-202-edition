package com.lycanitesmobs.core.entity.base;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Minimal stub - the original adds mount/saddle/rider-control behaviour (551 lines) on top
 * of TameableCreatureEntity. Not ported yet (see PORT_PLAN.md Phase 6+); this exists only so
 * CreatureInfo's isMountable()/isEntityClassAssignableTo() class-hierarchy checks compile -
 * no mountable creature will actually be rideable until real mount logic is ported.
 */
public abstract class RideableCreatureEntity extends TameableCreatureEntity {
    protected RideableCreatureEntity(EntityType<? extends AgeableCreatureEntity> entityType, Level world) {
        super(entityType, world);
    }

    /** Official field toggled by mount abilities. Kept on the stub so mount creatures compile (riding not ported). */
    protected boolean abilityToggled = false;

    /**
     * Continuous effects applied to the rider. No-op until RideableCreatureEntity is ported (creatures override it).
     **/
    public void riderEffects(LivingEntity rider) {
    }

    /**
     * The mount's special ability, triggered by the rider. No-op until RideableCreatureEntity is ported.
     **/
    public void mountAbility(Entity rider) {
    }

    /**
     * Returns true if targetEntity is riding this creature, directly or nested (rider of a rider).
     **/
    public boolean isEntityPassenger(Entity targetEntity, Entity nestedRider) {
        for (Entity entity : nestedRider.getPassengers()) {
            if (entity.equals(targetEntity)) {
                return true;
            }
            if (this.isEntityPassenger(targetEntity, entity)) {
                return true;
            }
        }
        return false;
    }
}
