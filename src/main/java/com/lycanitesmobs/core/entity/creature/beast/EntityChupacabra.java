package com.lycanitesmobs.core.entity.creature.beast;

import net.minecraft.world.entity.*;
import com.lycanitesmobs.core.entity.goals.util.GoalConditions;
import com.lycanitesmobs.core.entity.goals.actions.abilities.FireProjectilesGoal;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Trimmed - the original extends TameableCreatureEntity (not ported); this extends
 * AgeableCreatureEntity instead since it uses breed()/isBreedingItem() from that family, dropping
 * MobType.UNDEFINED/getNoBagSize/getBagSize/petControlsEnabled the same as EntityBalayang.
 * Also dropped: the FireProjectilesGoal chaos-orb ability (ProjectileManager isn't ported) and
 * shouldCreatureGroupFlee() (not a hook on BaseCreatureEntity - not added here since this batch
 * must not touch shared base-entity files while other creatures are being ported in parallel).
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityChupacabra extends TameableCreatureEntity {

    public EntityChupacabra(EntityType<? extends EntityChupacabra> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setSpeed(1.5D));
        this.goalSelector.addGoal(this.currentIdleGoalIndex(), new FireProjectilesGoal(this).setProjectile("chaosorb").setFireRate(40).setVelocity(1.0F).setScale(0.5F)
                .setConditions(new GoalConditions().setRareVariantOnly(true)));
    }

    @Override
    public boolean attackMelee(Entity target, double damageScale) {
        if (!super.attackMelee(target, damageScale))
            return false;

        if ((target instanceof Animal || (target instanceof BaseCreatureEntity && ((BaseCreatureEntity) target).isFarmableCreature())) && target.getDimensions(Pose.STANDING).height() >= 1F)
            this.breed();

        float leeching = Math.max(1, this.getAttackDamage(damageScale) / 2);
        this.heal(leeching);

        return true;
    }

    @Override
    public boolean canBeTempted() {
        return this.isBaby();
    }

    @Override
    public boolean isBreedingItem(ItemStack itemStack) {
        return false; // Breeding is triggered by attacking specific mobs instead!
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    @Override
    public int getBagSize() { return this.creatureInfo.getBagSize(); }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() { return 0; }

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() { return true; }

    @Override
    public boolean shouldCreatureGroupFlee(LivingEntity target) {
        return false;
    }
}
