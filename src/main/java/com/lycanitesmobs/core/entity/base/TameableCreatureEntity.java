package com.lycanitesmobs.core.entity.base;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Minimal stub - the original adds the full taming/ownership/pet-command/hunger/stamina
 * system (1,494 lines) on top of AgeableCreatureEntity. Not ported yet (see PORT_PLAN.md
 * Phase 6+); this exists only so CreatureInfo's isTameable()/isSummonable()/
 * isEntityClassAssignableTo() class-hierarchy checks compile - no creature extending this
 * is actually tameable yet, isTamed() still inherits BaseCreatureEntity's "always false".
 */
public abstract class TameableCreatureEntity extends AgeableCreatureEntity {
    protected TameableCreatureEntity(EntityType<? extends AgeableCreatureEntity> entityType, Level world) {
        super(entityType, world);
    }
}
