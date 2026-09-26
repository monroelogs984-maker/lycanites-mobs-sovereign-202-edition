package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.goals.actions.abilities.FireProjectilesGoal;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import com.lycanitesmobs.core.entity.goals.actions.AttackRangedGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.creature.insect.EntityVespid;
import com.lycanitesmobs.core.entity.creature.insect.EntityVespidQueen;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAvoidTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed - the original extends TameableCreatureEntity (not ported); this extends
 * BaseCreatureEntity directly, dropping MobType.UNDEFINED/getNoBagSize/getBagSize/
 * petControlsEnabled the same as EntityBalayang. Also dropped entirely: the vespid-infection
 * mechanic (infectWithVespids/spawnVespidSwarm/getSpeciesName/getTextureName overrides/
 * extraAnimation01/NBT fields) - it's gated on hasSpawnEventType() and an extraAnimation01 sync
 * field, neither of which exist on BaseCreatureEntity (not added here - this batch must not
 * touch shared base-entity files while other creatures are being ported in parallel), the
 * ranged "poop" projectile attack (ProjectileManager isn't ported), the random-leap aiStep
 * behavior (leap() not on BaseCreatureEntity, same reason), shouldCreatureGroupFlee() (not a
 * hook here), and getFallResistance(). claimSpecialTargetGoalIndex() doesn't exist either -
 * substituted claimFindTargetGoalIndex() for the avoid-target goals below, which claims from the
 * same targetSelector priority counter family.
 * <p>
 * NOTE: depends on EntityVespid/EntityVespidQueen (ported by a parallel fork in this same batch
 * of work) for the canAttack() immunity check - if those aren't compiled yet this won't build
 * standalone, but should resolve once all batches land.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 * PHASE 6a UPDATE (2026-09-26): ranged attack restored (projectiles ported) - any wording above about a
 * substituted melee attack or ProjectileManager being unported is outdated.
 */
public class EntityConba extends TameableCreatureEntity implements Enemy {

    public EntityConba(EntityType<? extends EntityConba> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(this.claimPriorityGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true));

        super.registerGoals();

        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAvoidTargetGoal(this).setTargetClass(Player.class).setTameTargetting(false));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAvoidTargetGoal(this).setTargetClass(Villager.class));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAvoidTargetGoal(this).setTargetClass(Pillager.class));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new FireProjectilesGoal(this).setProjectile("poop").setFireRate(20).setVelocity(1.2F));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityVespid || target instanceof EntityVespidQueen)
            return false;
        return super.canAttack(target);
    }

    @Override
    public void attackRanged(Entity target, float range) {
        this.fireProjectile("poop", target, range, 0, new Vector3d(0, 0, 0), 1.2f, 2f, 1F);
        super.attackRanged(target, range);
    }
}
