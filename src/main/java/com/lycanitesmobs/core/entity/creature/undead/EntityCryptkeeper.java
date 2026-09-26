package com.lycanitesmobs.core.entity.creature.undead;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - MoveVillageGoal/BreakDoorGoal (Lycanites' own, not ported - vanilla has its own
 * BreakDoorGoal that could be wired in later) dropped, along with the villager-to-zombie-villager
 * onKillEntity() conversion hook (that exact override point doesn't exist in 1.21.1's
 * LivingEntity anymore, and it's a flavor feature, not core AI). MobType.UNDEAD attribute
 * assignment and bag-size overrides dropped, same as every other creature this session.
 */
public class EntityCryptkeeper extends AgeableCreatureEntity implements Enemy {

    public EntityCryptkeeper(EntityType<? extends EntityCryptkeeper> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = false;
        this.spreadFire = true;
        this.canGrow = false;
        this.babySpawnChance = 0.1D;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.HUSK));

        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));

        if (this.getNavigation() instanceof GroundPathNavigation pathNavigateGround) {
            pathNavigateGround.setCanOpenDoors(true);
            pathNavigateGround.setAvoidSun(true);
        }
    }
}
