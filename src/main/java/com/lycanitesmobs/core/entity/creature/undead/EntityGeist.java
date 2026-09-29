package com.lycanitesmobs.core.entity.creature.undead;

import com.lycanitesmobs.core.entity.goals.actions.BreakDoorGoal;
import com.lycanitesmobs.core.entity.goals.actions.MoveVillageGoal;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ServerLevelAccessor;
import com.lycanitesmobs.core.block.base.BlockFireBase;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Trimmed - MoveVillageGoal/BreakDoorGoal/onKillEntity villager-conversion dropped (same as
 * EntityCryptkeeper). daylightBurns()/isMinion() dropped - neither hook exists in this port
 * (spawn-event/minion system not ported). die()'s shadowfire-spread-on-death effect kept -
 * self-contained, uses the already-ported BlockShadowfire/BlockFireBase.PERMANENT.
 */
public class EntityGeist extends AgeableCreatureEntity implements Enemy {

    protected boolean shadowfireDeath = true;

    public EntityGeist(EntityType<? extends EntityGeist> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.spreadFire = true;
        this.canGrow = false;
        this.babySpawnChance = 0.01D;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(this.claimTravelGoalIndex(), new MoveVillageGoal(this));

        super.registerGoals();

        this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new BreakDoorGoal(this));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));

        if (this.getNavigation() instanceof GroundPathNavigation) {
            GroundPathNavigation pathNavigateGround = (GroundPathNavigation) this.getNavigation();
            pathNavigateGround.setCanOpenDoors(true);
            pathNavigateGround.setAvoidSun(true);
        }
    }

    @Override
    public void loadCreatureFlags() {
        this.shadowfireDeath = this.creatureInfo.getFlag("shadowfireDeath", this.shadowfireDeath);
    }

    @Override
    public void die(DamageSource damageSource) {
        boolean wasDead = this.dead;
        super.die(damageSource);
        if (wasDead || !this.dead) {
            return;
        }

        try {
            int shadowfireWidth = (int) Math.floor(this.getDimensions(this.getPose()).width()) + 1;
            int shadowfireHeight = (int) Math.floor(this.getDimensions(this.getPose()).height()) + 1;
            boolean permanent = false;
            if (damageSource.getEntity() == this) {
                permanent = true;
                shadowfireWidth *= 5;
            }

            if (!this.getCommandSenderWorld().isClientSide && (permanent || (this.getCommandSenderWorld().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && this.shadowfireDeath))) {
                for (int x = (int) this.position().x() - shadowfireWidth; x <= (int) this.position().x() + shadowfireWidth; x++) {
                    for (int y = (int) this.position().y() - shadowfireHeight; y <= (int) this.position().y() + shadowfireHeight; y++) {
                        for (int z = (int) this.position().z() - shadowfireWidth; z <= (int) this.position().z() + shadowfireWidth; z++) {
                            Block block = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y, z)).getBlock();
                            if (block != Blocks.AIR && block != ObjectManager.getBlock("shadowfire")) {
                                BlockPos placePos = new BlockPos(x, y + 1, z);
                                Block upperBlock = this.getCommandSenderWorld().getBlockState(placePos).getBlock();
                                if (upperBlock == Blocks.AIR) {
                                    this.getCommandSenderWorld().setBlockAndUpdate(placePos, ObjectManager.getBlock("shadowfire").defaultBlockState().setValue(BlockFireBase.PERMANENT, permanent));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    @Override
    public boolean daylightBurns() {
        return !this.isBaby() && !this.isMinion();
    }

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
