package com.lycanitesmobs.core.entity.base;

import com.lycanitesmobs.core.entity.goals.targeting.FindParentGoal;
import com.lycanitesmobs.core.entity.goals.actions.MateGoal;
import com.lycanitesmobs.core.entity.goals.actions.FollowParentGoal;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import com.lycanitesmobs.core.data.info.item.ItemDrop;
import com.lycanitesmobs.core.data.info.Variant;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Trimmed port - adds growth/breeding on top of BaseCreatureEntity. Dropped from the original:
 * the right-click interact-command system (getInteractCommands/performCommand - spawn-egg baby
 * spawning, feed-to-breed via right click - needs ItemCustomSpawnEgg, not ported, and a base
 * command-dispatch system BaseCreatureEntity never got), MateGoal/FindParentGoal (not ported -
 * so breeding only happens if something calls breed()/procreate() directly, not via AI), and
 * ExtendedPlayer beastiary study hooks in procreate(). FollowParentGoal IS kept/ported since
 * EntityConcapedeSegment needs it to actually follow its parent segment.
 */
public abstract class AgeableCreatureEntity extends BaseCreatureEntity {

    private AgeableCreatureEntity breedingTarget;

    protected int growthTime = -24000;
    protected boolean canGrow = true;
    protected double babySpawnChance = 0D;

    protected int loveTime;
    private int loveTimeMax = 600;
    protected int breedingCooldown = 6000;

    protected boolean hasBeenFarmed = false;

    protected static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(AgeableCreatureEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> LOVE = SynchedEntityData.defineId(AgeableCreatureEntity.class, EntityDataSerializers.INT);

    protected AgeableCreatureEntity(EntityType<? extends AgeableCreatureEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AGE, 0);
        builder.define(LOVE, 0);
    }

    @Override
    protected void registerGoals() {
        // Greater Actions:
        this.goalSelector.addGoal(this.claimTravelGoalIndex(), new MateGoal(this).setMateDistance(5.0D));

        super.registerGoals();

        // Lesser Targeting:
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindParentGoal(this).setSightCheck(false).setDistance(32.0D));

        // Lesser Actions:
        this.goalSelector.addGoal(this.claimTravelGoalIndex(), new FollowParentGoal(this).setSpeed(1.0D).setStrayDistance(3.0D));
    }

    @Override
    public void setupMob() {
        if (this.babySpawnChance > 0D && this.random.nextDouble() < this.babySpawnChance)
            this.setGrowingAge(growthTime);
        super.setupMob();
    }

    @Override
    public Component getAgeName() {
        if (this.isBaby())
            return Component.translatable("entity.baby");
        else
            return super.getAgeName();
    }

    @Override
    public boolean isPersistant() {
        if (this.hasBeenFarmed)
            return true;
        return super.isPersistant();
    }

    public void setFarmed() {
        this.hasBeenFarmed = true;
        // NOTE: 1.21.1 renamed Entity.portalTime to portalCooldown, and it's now private -
        // use the accessor methods instead of direct field access.
        if (this.getPortalCooldown() > this.getDimensionChangingDelay())
            this.setPortalCooldown(this.getDimensionChangingDelay());
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.getCommandSenderWorld().isClientSide)
            this.setScaleForAge(this.isBaby());
        else if (this.canGrow) {
            int age = this.getGrowingAge();
            if (age < 0) {
                ++age;
                this.setGrowingAge(age);
            } else if (age > 0) {
                --age;
                this.setGrowingAge(age);
            }
        }

        if (!this.canBreed())
            this.loveTime = 0;

        if (!this.getCommandSenderWorld().isClientSide)
            this.getEntityData().set(LOVE, this.loveTime);
        else
            this.loveTime = this.getIntFromDataManager(LOVE);

        if (this.isInLove()) {
            this.setFarmed();
            --this.loveTime;
            if (this.getCommandSenderWorld().isClientSide && this.loveTime % 10 == 0) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                this.getCommandSenderWorld().addParticle(ParticleTypes.HEART,
                        this.position().x() + (double) (this.random.nextFloat() * this.getDimensions(Pose.STANDING).width() * 2.0F) - (double) this.getDimensions(Pose.STANDING).width(),
                        this.position().y() + 0.5D + (double) (this.random.nextFloat() * this.getDimensions(Pose.STANDING).height()),
                        this.position().z() + (double) (this.random.nextFloat() * this.getDimensions(Pose.STANDING).width() * 2.0F) - (double) this.getDimensions(Pose.STANDING).width(),
                        d0, d1, d2);
            }
        }
    }

    @Override
    public boolean canDropItem(ItemDrop itemDrop) {
        if (itemDrop.isAdultOnly() && this.isBaby()) {
            return false;
        }
        return super.canDropItem(itemDrop);
    }

    public int getGrowingAge() {
        return this.getIntFromDataManager(AGE);
    }

    public void setGrowingAge(int age) {
        this.getEntityData().set(AGE, age);
        this.setScaleForAge(this.isBaby());
    }

    public void addGrowth(int growth) {
        int age = this.getGrowingAge();
        age += growth * 20;
        if (age > 0)
            age = 0;
        this.setGrowingAge(age);
    }

    public boolean isBaby() {
        return this.getGrowingAge() < 0;
    }

    public boolean shouldFollowParent() {
        return this.isBaby();
    }

    public boolean shouldFindParent() {
        return this.isBaby();
    }

    public double setScaleForAge(boolean adult) {
        return adult ? 0.5F : 1.0F;
    }

    @Override
    public boolean canBeTempted() {
        return !this.isInLove();
    }

    public AgeableCreatureEntity getBreedingTarget() {
        return this.breedingTarget;
    }

    public void setBreedingTarget(AgeableCreatureEntity target) {
        this.breedingTarget = target;
    }

    public AgeableCreatureEntity createChild(AgeableCreatureEntity partner) {
        return (AgeableCreatureEntity) this.creatureInfo.createEntity(this.getCommandSenderWorld());
    }

    public boolean isBreedingItem(ItemStack itemStack) {
        if (!this.creatureInfo.isFarmable() || this.getAirSupply() <= -100) {
            return false;
        }
        return this.creatureInfo.canEat(itemStack);
    }

    public boolean canBreedWith(AgeableCreatureEntity partner) {
        if (partner == this)
            return false;
        if (partner.getClass() != this.getClass())
            return false;
        if (this.getSubspecies() != partner.getSubspecies()) {
            return false;
        }
        return this.isInLove() && partner.isInLove();
    }

    public boolean isInLove() {
        return this.loveTime > 0;
    }

    public boolean canMate() {
        return this.isInLove();
    }

    public boolean breed() {
        if (!this.canBreed())
            return false;
        this.loveTime = this.loveTimeMax;
        return true;
    }

    public boolean canBreed() {
        return this.getGrowingAge() == 0;
    }

    public void procreate(AgeableCreatureEntity partner) {
        AgeableCreatureEntity baby = this.createChild(partner);

        if (baby != null) {
            this.finishBreeding();
            partner.finishBreeding();
            baby.setGrowingAge(baby.getInitialGrowthTime());
            baby.setSubspecies(this.getSubspeciesIndex());
            Variant babyVariant = this.getSubspecies().getChildVariant(this, this.getVariant(), partner.getVariant());
            baby.applyVariant(babyVariant != null ? babyVariant.getIndex() : 0);
            baby.moveTo(this.position().x(), this.position().y(), this.position().z(), this.yRotO, this.xRotO);

            for (int i = 0; i < 7; ++i) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                this.getCommandSenderWorld().addParticle(ParticleTypes.HEART,
                        this.position().x() + (double) (this.random.nextFloat() * this.getDimensions(Pose.STANDING).width() * 2.0F) - (double) this.getDimensions(Pose.STANDING).width(),
                        this.position().y() + 0.5D + (double) (this.random.nextFloat() * this.getDimensions(Pose.STANDING).height()),
                        this.position().z() + (double) (this.random.nextFloat() * this.getDimensions(Pose.STANDING).width() * 2.0F) - (double) this.getDimensions(Pose.STANDING).width(),
                        d0, d1, d2);
            }

            this.onCreateBaby(partner, baby);

            DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, baby);
        }
    }

    public void onCreateBaby(AgeableCreatureEntity partner, AgeableCreatureEntity baby) {
    }

    public void finishBreeding() {
        this.setGrowingAge(this.breedingCooldown);
        this.setBreedingTarget(null);
        this.loveTime = 0;
    }

    public int getInitialGrowthTime() {
        return this.growthTime;
    }

    public boolean hasBeenFarmed() {
        return this.hasBeenFarmed;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("Age")) {
            this.setGrowingAge(nbt.getInt("Age"));
        } else {
            this.setGrowingAge(0);
        }

        if (nbt.contains("InLove")) {
            this.loveTime = nbt.getInt("InLove");
        } else {
            this.loveTime = 0;
        }

        if (nbt.contains("HasBeenFarmed") && nbt.getBoolean("HasBeenFarmed")) {
            this.setFarmed();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Age", this.getGrowingAge());
        nbt.putInt("InLove", this.loveTime);
        nbt.putBoolean("HasBeenFarmed", this.hasBeenFarmed);
    }
}
