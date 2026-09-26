package com.lycanitesmobs.core.entity.base;

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
}
