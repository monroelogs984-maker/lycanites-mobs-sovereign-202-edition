package com.lycanitesmobs.core.entity.creature.insect;

import net.minecraft.world.entity.player.Player;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.data.tag.LycanitesBlockTags;
import com.lycanitesmobs.core.entity.goals.actions.FollowParentGoal;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.UUID;

/**
 * Trimmed - dropped the MobType.ARTHROPOD attribute assignment and bag/equipment overrides,
 * same as EntityConcapedeHead. The segment-follows-parent drag logic (setPos each tick) and
 * growth-into-a-new-head-at-the-front-of-the-chain logic are kept, since that's the whole
 * point of this creature.
 */
public class EntityConcapedeSegment extends AgeableCreatureEntity {

    UUID parentUUID = null;

    private BaseCreatureEntity backSegment;

    public EntityConcapedeSegment(EntityType<? extends EntityConcapedeSegment> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.hasStepSound = false;
        this.canGrow = true;
        this.babySpawnChance = 0D;
        this.isAggressiveByDefault = false;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(this.claimTravelGoalIndex(), new FollowParentGoal(this).setSpeed(1.0D).setStrayDistance(0));
        super.registerGoals();
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, MobSpawnType spawnReason) {
        if (this.getNearbyEntities(EntityConcapedeHead.class, null, CreatureManager.getInstance().getSpawnConfig().spawnLimitRange()).isEmpty())
            return false;
        return super.checkSpawnRules(world, spawnReason);
    }

    @Override
    public void aiStep() {
        Level world = this.getCommandSenderWorld();
        boolean isClient = world.isClientSide;

        if (!isClient && !this.hasParent() && this.parentUUID != null && this.updateTick > 0 && this.updateTick % 40 == 0) {
            if (world instanceof ServerLevel serverLevel) {
                Entity foundEntity = serverLevel.getEntity(this.parentUUID);
                if (foundEntity instanceof AgeableCreatureEntity parent && parent != this) {
                    this.setParentTarget(parent);
                }
            }
            this.parentUUID = null;
        }

        super.aiStep();

        if (!isClient) {
            if (this.backSegment != null) {
                if (!this.backSegment.isAlive())
                    this.backSegment = null;
            }

            if (this.hasParent()) {
                if (!this.getParentTarget().isAlive())
                    this.setParentTarget(null);
            }

            if (this.hasParent()) {
                this.getLookControl().setLookAt(this.getParentTarget(), 360.0F, 360.0F);
                this.lookAt(this.getParentTarget(), 360, 360);

                Vector3d parentPos = this.getFacingPositionDouble(this.getParentTarget().getX(), this.getParentTarget().getY(), this.getParentTarget().getZ(), -0.65D, this.getParentTarget().getYRot());
                double segmentPullThreshold = 0.15D;
                double segmentDistanceSq = this.distanceToSqr(LMHelperClass.convertToVec3(parentPos));
                if (segmentDistanceSq > segmentPullThreshold * segmentPullThreshold) {
                    double dragAmount = segmentPullThreshold / 2;
                    Vector3d posVector = new Vector3d(this.getX(), this.getY(), this.getZ());
                    Vector3d dragPos = this.getFacingPositionDouble(parentPos.x, parentPos.y, parentPos.z, dragAmount, posVector.dot(parentPos));
                    double distY = (parentPos.y - this.getY());
                    double dragY = this.getY() + (distY / 2);
                    this.setPos(dragPos.x, dragY, dragPos.z);
                }
            }

            if (this.getGrowingAge() <= 0)
                this.setGrowingAge(-this.growthTime);
        }
    }

    @Override
    public boolean rollLookChance() {
        if (this.hasParent())
            return false;
        return super.rollLookChance();
    }

    @Override
    public boolean rollWanderChance() {
        if (this.hasParent())
            return false;
        return super.rollWanderChance();
    }

    @Override
    public void setGrowingAge(int age) {
        if (this.hasParent())
            age = -this.growthTime;
        super.setGrowingAge(age);
        if (age == 0 && !this.getCommandSenderWorld().isClientSide) {
            EntityConcapedeHead concapedeHead = (EntityConcapedeHead) CreatureManager.getInstance().getCreature("concapede").createEntity(this.getCommandSenderWorld());
            concapedeHead.moveTo(this.position().x(), this.position().y(), this.position().z(), this.getYRot(), this.getXRot());
            concapedeHead.markNotFirstSpawn();
            concapedeHead.setGrowingAge(-this.growthTime / 4);
            concapedeHead.setSizeScale(this.sizeScale);
            concapedeHead.applyVariant(this.getVariantIndex());
            DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), concapedeHead.blockPosition(), null, concapedeHead, () -> {
                if (this.backSegment != null) {
                    this.backSegment.setParentTarget(concapedeHead);
                }
                this.remove(RemovalReason.DISCARDED);
            });
        }
    }

    @Override
    public boolean shouldFollowParent() {
        return true;
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
        return !this.hasParent();
    }

    @Override
    public double getFallingMod() {
        if (this.getCommandSenderWorld().isClientSide)
            return 0.0D;
        if (this.hasParent() && this.getParentTarget().position().y() > this.position().y())
            return 0.0D;
        return super.getFallingMod();
    }

    @Override
    public boolean useDirectNavigator() {
        return this.hasParent();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void setParentTarget(LivingEntity setTarget) {
        if (setTarget != this) {
            if (setTarget instanceof EntityConcapedeSegment)
                ((EntityConcapedeSegment) setTarget).setBackSegment(this);
            if (setTarget instanceof EntityConcapedeHead)
                ((EntityConcapedeHead) setTarget).setBackSegment(this);
        }
        super.setParentTarget(setTarget);
    }

    BaseCreatureEntity getBackSegment() {
        return this.backSegment;
    }

    void setBackSegment(BaseCreatureEntity backSegment) {
        this.backSegment = backSegment;
    }

    @Override
    public boolean canClimb() {
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }

    @Override
    public AgeableCreatureEntity createChild(AgeableCreatureEntity partner) {
        return null;
    }

    @Override
    public boolean breed() {
        if (!this.canBreed())
            return false;
        this.setGrowingAge(0);
        return true;
    }

    @Override
    public boolean canBreed() {
        return !this.hasParent();
    }

    @Override
    public boolean shouldFindParent() {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.hasUUID("ParentUUID")) {
            this.parentUUID = nbt.getUUID("ParentUUID");
        }
        super.readAdditionalSaveData(nbt);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.hasParent()) {
            nbt.putUUID("ParentUUID", this.getParentTarget().getUUID());
        }
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    /**
     * Returns whether this mob should despawn overtime or not. Config defined forced despawns override everything except tamed creatures and tagged creatures.
     **/
    @Override
    protected boolean canDespawnNaturally() {
        if (!super.canDespawnNaturally())
            return false;
        return !this.hasParent();
    }

    /**
     * Second stage checks for spawning, this check is ignored if there is a valid monster spawner nearby.
     **/
    @Override
    public boolean environmentSpawnCheck(Level world, BlockPos pos) {
        if (this.getNearbyEntities(EntityConcapedeHead.class, null, CreatureManager.getInstance().getSpawnConfig().spawnLimitRange()).size() <= 0)
            return false;
        return super.environmentSpawnCheck(world, pos);
    }

    @Override
    public int getBagSize() {
        return this.creatureInfo.getBagSize();
    }

    @Override
    public float getFallResistance() {
        return 100;
    }

    // ==================================================
    //                     Equipment
    // ==================================================
    @Override
    public int getNoBagSize() {
        return 0;
    }

    // ========== Get Random Subspecies ==========
    @Override
    public void getRandomVariant() {
        if (this.subspecies == null && !this.hasParent()) {
            this.subspecies = this.creatureInfo.getRandomSubspecies(this);
        }

        if (this.hasParent() && this.getParentTarget() instanceof BaseCreatureEntity) {
            this.applyVariant(((BaseCreatureEntity) this.getParentTarget()).getSubspeciesIndex());
        }
    }

    /**
     * Gets whether this mob should always display its nametag if it's a subspecies.
     **/
    @Override
    public boolean renderVariantNameTag() {
        return !this.hasParent();
    }
}
