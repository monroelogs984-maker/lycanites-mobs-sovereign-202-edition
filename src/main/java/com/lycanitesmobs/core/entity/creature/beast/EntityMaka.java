package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed - dropped TemptGoal and FindMasterGoal (not ported - tame/master system). Kept the
 * random-alpha spawn-replacement mechanic in onFirstSpawn()/setGrowingAge() (self-contained,
 * uses the already-ported CreatureManager/DeferredLevelActionManager, same pattern as
 * EntityConcapedeHead's segment spawning). Dropped fleeHealthPercent (no such field on this
 * port's AgeableCreatureEntity/BaseCreatureEntity), getNoBagSize/getBagSize (bag subsystem not
 * ported), and the MobType.UNDEFINED attribute assignment. canBeLeashed() is the 1.21.1 no-arg
 * signature (was canBeLeashed(Player)).
 */
public class EntityMaka extends AgeableCreatureEntity {

    public EntityMaka(EntityType<? extends EntityMaka> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.babySpawnChance = 0.1D;
        this.attackCooldownMax = 10;
        this.isAggressiveByDefault = false;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public void onFirstSpawn() {
        // Random Alpha:
        CreatureInfo alphaInfo = CreatureManager.getInstance().getCreature("makaalpha");
        if (alphaInfo != null) {
            float alphaChance = (float) alphaInfo.getCreatureSpawn().getSpawnWeight() / Math.max(this.creatureInfo.getCreatureSpawn().getSpawnWeight(), 1);
            if (this.getRandom().nextFloat() <= alphaChance) {
                EntityMakaAlpha alpha = (EntityMakaAlpha) CreatureManager.getInstance().getCreature("makaalpha").createEntity(this.getCommandSenderWorld());
                alpha.copyPosition(this);
                DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, alpha);
                this.remove(RemovalReason.DISCARDED);
            }
        }
        super.onFirstSpawn();
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        BlockState blockState = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y - 1, z));
        if (blockState.is(LycanitesBlockTags.CREATURE_PATH_GRASS_PREFERRED))
            return 10F;
        if (blockState.is(LycanitesBlockTags.CREATURE_PATH_DIRT_PREFERRED))
            return 7F;
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityMaka || target instanceof EntityMakaAlpha)
            return false;
        return super.canAttack(target);
    }

    @Override
    public void setGrowingAge(int age) {
        if (age == 0 && this.getGrowingAge() < 0) {
            if (this.getRandom().nextFloat() >= 0.9F) {
                EntityMakaAlpha alpha = (EntityMakaAlpha) CreatureManager.getInstance().getCreature("makaalpha").createEntity(this.getCommandSenderWorld());
                alpha.copyPosition(this);
                DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, alpha);
                this.remove(RemovalReason.DISCARDED);
            }
        }
        super.setGrowingAge(age);
    }
}
