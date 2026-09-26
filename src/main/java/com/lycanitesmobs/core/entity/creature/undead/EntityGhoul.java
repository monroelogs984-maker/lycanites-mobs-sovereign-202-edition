package com.lycanitesmobs.core.entity.creature.undead;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - same pattern as EntityCryptkeeper/EntityGeist: MoveVillageGoal/BreakDoorGoal/
 * onKillEntity villager-conversion and daylightBurns() dropped.
 */
public class EntityGhoul extends AgeableCreatureEntity implements Enemy {

    public EntityGhoul(EntityType<? extends EntityGhoul> entityType, Level world) {
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

        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));

        if (this.getNavigation() instanceof GroundPathNavigation pathNavigateGround) {
            pathNavigateGround.setCanOpenDoors(true);
            pathNavigateGround.setAvoidSun(true);
        }
    }
}
