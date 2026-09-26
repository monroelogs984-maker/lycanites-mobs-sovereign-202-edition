package com.lycanitesmobs.core.entity.creature.beast;

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
 */
public class EntityConba extends BaseCreatureEntity implements Enemy {

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
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityVespid || target instanceof EntityVespidQueen)
            return false;
        return super.canAttack(target);
    }
}
