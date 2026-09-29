package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.entity.goals.actions.TemptGoal;
import net.minecraft.world.entity.player.Player;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Trimmed - dropped TemptGoal (not ported), the MobType.ARTHROPOD attribute assignment
 * (BaseCreatureEntity's `attribute` field was dropped during the Phase 5 trim), and the bag/
 * equipment overrides (getNoBagSize/getBagSize - that subsystem isn't ported). The segment-chain
 * spawning logic (the actual "centipede body" mechanic) is kept close to the original since
 * that's the point of this creature.
 */
public class EntityConcapedeHead extends AgeableCreatureEntity {

    protected static int CONCAPEDE_SIZE_MAX = 10;
    private BaseCreatureEntity backSegment;
    protected boolean isHungry = true;

    public EntityConcapedeHead(EntityType<? extends EntityConcapedeHead> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0D;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
		this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new TemptGoal(this).setIncludeDiet(true));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));
    }

    @Override
    public void loadCreatureFlags() {
        CONCAPEDE_SIZE_MAX = this.creatureInfo.getFlag("sizeMax", CONCAPEDE_SIZE_MAX);
    }

    @Override
    public void onFirstSpawn() {
        if (!this.getCommandSenderWorld().isClientSide && this.backSegment == null) {
            this.setGrowingAge(-this.growthTime / 4);
            int segmentCount = this.getRandom().nextInt(CONCAPEDE_SIZE_MAX);
            AgeableCreatureEntity parentSegment = this;
            for (int segment = 0; segment < segmentCount; segment++) {
                EntityConcapedeSegment segmentEntity = (EntityConcapedeSegment) CreatureManager.getInstance().getCreature("concapedesegment").createEntity(parentSegment.getCommandSenderWorld());
                segmentEntity.moveTo(parentSegment.position().x(), parentSegment.position().y(), parentSegment.position().z(), 0.0F, 0.0F);
                segmentEntity.setParentTarget(parentSegment);
                segmentEntity.applyVariant(this.getVariantIndex());
                segmentEntity.setSizeScale(this.sizeScale);
                segmentEntity.inheritSpawnEventFrom(this);
                segmentEntity.markNotFirstSpawn();
                DeferredLevelActionManager.spawnEntity(parentSegment.getCommandSenderWorld(), segmentEntity.blockPosition(), null, segmentEntity);
                parentSegment = segmentEntity;
            }
        }
        super.onFirstSpawn();
    }

    @Override
    public boolean shouldFollowParent() {
        return false;
    }

    @Override
    public boolean rollLookChance() {
        return false;
    }

    @Override
    public boolean rollWanderChance() {
        return false;
    }

    @Override
    public void setGrowingAge(int age) {
        if (!this.getCommandSenderWorld().isClientSide && age == 0 && !this.isHungry && CreatureManager.getInstance().getCreature("concapedesegment") != null) {
            age = -(this.growthTime / 4);
            this.isHungry = true;

            int size = 0;
            BaseCreatureEntity lastSegment = this;
            while (size <= CONCAPEDE_SIZE_MAX) {
                size++;

                BaseCreatureEntity trailingSegment = null;
                if (lastSegment instanceof EntityConcapedeHead)
                    trailingSegment = ((EntityConcapedeHead) lastSegment).getBackSegment();
                else if (lastSegment instanceof EntityConcapedeSegment)
                    trailingSegment = ((EntityConcapedeSegment) lastSegment).getBackSegment();

                if (trailingSegment == null || trailingSegment == lastSegment) {
                    break;
                }
                lastSegment = trailingSegment;
            }

            if (size < CONCAPEDE_SIZE_MAX) {
                EntityConcapedeSegment segmentEntity = (EntityConcapedeSegment) CreatureManager.getInstance().getCreature("concapedesegment").createEntity(lastSegment.getCommandSenderWorld());
                segmentEntity.moveTo(lastSegment.position().x(), lastSegment.position().y(), lastSegment.position().z(), 0.0F, 0.0F);
                segmentEntity.setParentTarget(lastSegment);
                segmentEntity.applyVariant(this.getVariantIndex());
                segmentEntity.setSizeScale(this.sizeScale);
                segmentEntity.inheritSpawnEventFrom(this);
                segmentEntity.markNotFirstSpawn();
                DeferredLevelActionManager.spawnEntity(lastSegment.getCommandSenderWorld(), segmentEntity.blockPosition(), null, segmentEntity);
            }
        }
        super.setGrowingAge(age);
    }

    @Override
    public float getBlockPathWeight(int x, int y, int z) {
        BlockState blockState = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y - 1, z));
        Block block = blockState.getBlock();
        if (block != Blocks.AIR) {
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_GRASS_PREFERRED))
                return 10F;
            if (blockState.is(LycanitesBlockTags.CREATURE_PATH_DIRT_PREFERRED))
                return 7F;
        }
        return super.getBlockPathWeight(x, y, z);
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof EntityConcapedeSegment)
            return false;
        return super.canAttack(target);
    }

    @Override
    public boolean isAggressive() {
        if (this.isInLove())
            return false;
        if (this.isDaytime())
            return this.testLightLevel() < 2;
        else
            return super.isAggressive();
    }

    @Override
    public boolean isProtective(Entity entity) {
        if (this.isInLove())
            return false;
        if (entity instanceof EntityConcapedeSegment) {
            BaseCreatureEntity checkCreature = (BaseCreatureEntity) entity;
            while (true) {
                if (!checkCreature.hasParent()) {
                    break;
                }
                if (checkCreature.getParentTarget() == this) {
                    return true;
                }
                if (checkCreature.getParentTarget() instanceof BaseCreatureEntity) {
                    checkCreature = (BaseCreatureEntity) checkCreature.getParentTarget();
                    continue;
                }
                break;
            }
        }
        return false;
    }

    @Override
    public boolean canClimb() {
        return true;
    }

    @Override
    public AgeableCreatureEntity createChild(AgeableCreatureEntity partner) {
        return null;
    }

    @Override
    public boolean canBreed() {
        return this.getGrowingAge() >= 0;
    }

    @Override
    public boolean breed() {
        if (super.breed()) {
            this.isHungry = false;
            if (this.getAge() == 0) {
                this.setGrowingAge(0);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canMate() {
        return false;
    }

    BaseCreatureEntity getBackSegment() {
        return this.backSegment;
    }

    void setBackSegment(BaseCreatureEntity backSegment) {
        this.backSegment = backSegment;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.contains("IsHungry")) {
            this.isHungry = nbt.getBoolean("IsHungry");
        }
        super.readAdditionalSaveData(nbt);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("IsHungry", this.isHungry);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
	@Override
	public int getBagSize() { return this.creatureInfo.getBagSize(); }

    // ==================================================
   	//                     Immunities
   	// ==================================================
    @Override
    public float getFallResistance() {
    	return 100;
    }

	// ==================================================
	//                     Equipment
	// ==================================================
	@Override
	public int getNoBagSize() { return 0; }
}
