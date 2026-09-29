package com.lycanitesmobs.core.entity.creature.undead;

import com.lycanitesmobs.core.entity.goals.actions.BreakDoorGoal;
import com.lycanitesmobs.core.entity.goals.actions.MoveVillageGoal;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ServerLevelAccessor;
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
        this.goalSelector.addGoal(this.claimTravelGoalIndex(), new MoveVillageGoal(this));

        super.registerGoals();

        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(EntityType.HUSK));

        this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new BreakDoorGoal(this));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));

        if(this.getNavigation() instanceof GroundPathNavigation) {
            GroundPathNavigation pathNavigateGround = (GroundPathNavigation)this.getNavigation();
            pathNavigateGround.setCanOpenDoors(true);
            pathNavigateGround.setAvoidSun(true);
        }
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
    //                      Attacks
    // ==================================================
    // ========== On Kill ==========
    @Override
    public void onKillEntity(LivingEntity entityLivingBase) {
        super.onKillEntity(entityLivingBase);

        // Villager -> Zombie Villager (Normal: 50%, Hard: always), like vanilla zombies:
        if (this.getCommandSenderWorld().getDifficulty().getId() >= 2 && entityLivingBase instanceof Villager villager) {
            if (this.getCommandSenderWorld().getDifficulty().getId() == 2 && this.random.nextBoolean()) return;
            this.convertVillagerToZombie(villager);
        }
    }
}
