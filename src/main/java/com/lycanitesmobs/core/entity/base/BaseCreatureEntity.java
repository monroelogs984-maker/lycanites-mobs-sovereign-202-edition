package com.lycanitesmobs.core.entity.base;

import com.google.common.base.Predicate;
import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.data.info.Variant;
import com.lycanitesmobs.core.manager.ModAttributes;
import com.lycanitesmobs.core.data.info.creature.*;
import com.lycanitesmobs.core.data.info.item.ItemDrop;
import com.lycanitesmobs.core.entity.goals.actions.LookIdleGoal;
import com.lycanitesmobs.core.entity.goals.actions.WanderGoal;
import com.lycanitesmobs.core.entity.goals.targeting.AvoidIfHitGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import com.lycanitesmobs.core.entity.goals.targeting.RevengeGoal;
import com.lycanitesmobs.core.entity.navigation.DirectNavigator;
import com.lycanitesmobs.core.entity.util.CreatureStats;
import com.lycanitesmobs.core.entity.util.Targeting;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.MoverType;
import com.lycanitesmobs.core.entity.navigation.CreaturePathNavigator;
import com.lycanitesmobs.core.entity.navigation.CreatureMoveController;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import com.lycanitesmobs.core.entity.util.CreatureRelationshipEntry;
import com.lycanitesmobs.core.entity.util.CreatureRelationships;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import java.util.HashMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Trimmed NeoForge 1.21.1 port of the original 7000+ line BaseCreatureEntity.
 * Dropped subsystems (deferred to later phases, see PORT_PLAN.md): capabilities
 * (ExtendedEntity/Player/World), networking sync (MessageCreature), containers/inventory
 * (CreatureContainer/CreatureInventory), pets (PetEntry), summoning pedestals, equipment
 * parts, projectiles, custom navigation (CreatureMoveController/CreaturePathNavigator -
 * vanilla GroundPathNavigation/MoveControl used instead), creature relationships/taming
 * reputation, minions, boss health bar UI, battle phase transform/fusion, and most of the
 * spawn-eligibility checking chain (checkSpawnRules always allows for now - natural spawn
 * light/biome/group-limit checks aren't ported).
 */
public abstract class BaseCreatureEntity extends PathfinderMob {
    public static final Holder<Attribute> DEFENSE = ModAttributes.DEFENSE;
    public static final Holder<Attribute> RANGED_SPEED = ModAttributes.RANGED_SPEED;

    protected static final EntityDataAccessor<Byte> TARGET = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> ATTACK_PHASE = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> ANIMATION_STATE = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> ANIMATION_ATTACK_COOLDOWN_MAX = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> STEALTH = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Boolean> BABY = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Byte> COLOR = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> CLIMBING = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> EXPERIENCE = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Byte> SUBSPECIES = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> VARIANT = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.BYTE);

    private static int BOSS_DAMAGE_LIMIT = 50;

    protected CreatureInfo creatureInfo;
    protected CreatureStats creatureStats;
    protected Subspecies subspecies = null;
    protected Variant variant = null;
    protected ExtraMobBehaviour extraMobBehaviour;
    protected long updateTick = 0;
    private long packCheckTick = Long.MIN_VALUE;
    private boolean cachedInPack = false;
    private MutableComponent cachedFullName;
    protected String spawnEventType = "";
    protected int spawnEventCount = -1;
    protected EntityDimensions creatureSize;

    protected double sizeScale = 1.0D;
    protected float hitAreaWidthScale = 1;
    protected float hitAreaHeightScale = 1;
    protected byte attackPhaseMax = 0;
    protected byte attackPhase = 0;
    protected int battlePhase = 0;
    protected int fleeTime = 200;
    protected int currentFleeTime = 0;
    protected float renderTick = 0;
    protected boolean isAggressiveByDefault = true;
    protected boolean spreadFire = false;
    protected boolean stealthPrev = false;
    protected int currentBlockingTime = 0;
    protected int blockingTime = 60;
    protected LivingEntity pickupEntity;

    protected int nextPriorityGoalIndex;
    protected int nextDistractionGoalIndex;
    protected int nextCombatGoalIndex;
    protected int nextTravelGoalIndex;
    protected int nextIdleGoalIndex;
    protected int nextReactTargetIndex;
    protected int nextSpecialTargetIndex;
    protected int nextFindTargetIndex;
    protected boolean firstSpawn = true;
    protected boolean needsInitialLevel = true;

    protected boolean isLavaCreature = false;
    protected boolean spawnedRare = false;
    protected boolean spawnedAsBoss = false;
    private DirectNavigator directNavigator;

    protected boolean hasAttackSound = false;
    protected boolean hasStepSound = true;
    protected boolean hasJumpSound = false;
    protected int flySoundSpeed = 0;
    protected float onlyRenderTicks = -1;

    private final List<ItemDrop> drops = new ArrayList<>();
    private final List<ItemDrop> savedDrops = new ArrayList<>();
    private FindAttackTargetGoal aiTargetPlayer = null;
    private RevengeGoal aiDefendAnimals = null;
    protected float flyingSpeed = 0.02F;
    protected int mobLevel = 1;
    protected int experience = 0;
    protected int attackCooldownMax = 5;
    protected int attackCooldown = 0;
    protected int growingAge;
    protected List<EntityType> hostileTargets = new ArrayList<>();
    protected List<Class<? extends Entity>> hostileTargetClasses = new ArrayList<>();
    private LivingEntity masterTarget;
    private LivingEntity parentTarget;
    private LivingEntity avoidTarget;
    private LivingEntity fixateTarget;
    private LivingEntity perchTarget;

    /** The Creature's relationships, for advanced memory and taming. Ported in Phase 5e. */
    protected CreatureRelationships relationships;

    protected BaseCreatureEntity(EntityType<? extends BaseCreatureEntity> entityType, Level world) {
        super(entityType, world);
        this.relationships = new CreatureRelationships(this);

        // Movement (Phase 5g): vanilla has no createMoveController() hook, the official source assigns it here.
        this.moveControl = this.createMoveController();
        this.initializePathing();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide) {
            this.applyDynamicAttributes();
        }
    }

    public static AttributeSupplier.Builder registerCustomAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(DEFENSE)
                .add(Attributes.ATTACK_DAMAGE)
                .add(Attributes.ATTACK_SPEED)
                .add(RANGED_SPEED)
                .add(Attributes.FOLLOW_RANGE);
    }

    @Override
    public EntityType getType() {
        if (this.creatureInfo == null) {
            return super.getType();
        }
        return this.creatureInfo.getEntityType();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        this.creatureInfo = CreatureManager.getInstance().getCreature(this.getClass());
        super.defineSynchedData(builder);
        builder.define(TARGET, (byte) 0);
        builder.define(ATTACK_PHASE, (byte) 0);
        builder.define(ANIMATION_STATE, (byte) 0);
        builder.define(ANIMATION_ATTACK_COOLDOWN_MAX, 0);
        builder.define(STEALTH, 0.0F);
        builder.define(BABY, false);
        builder.define(COLOR, (byte) 0);
        builder.define(CLIMBING, (byte) 0);
        builder.define(LEVEL, 1);
        builder.define(EXPERIENCE, 0);
        builder.define(SUBSPECIES, (byte) 0);
        builder.define(VARIANT, (byte) 0);

        this.loadCreatureFlags();
        this.creatureSize = EntityDimensions.scalable((float) this.creatureInfo.getWidth(), (float) this.creatureInfo.getHeight());

        this.creatureStats = new CreatureStats(this);
        this.extraMobBehaviour = new ExtraMobBehaviour(this);
        this.directNavigator = new DirectNavigator(this);

        this.nextPriorityGoalIndex = 10;
        this.nextDistractionGoalIndex = 30;
        this.nextCombatGoalIndex = 50;
        this.nextTravelGoalIndex = 70;
        this.nextIdleGoalIndex = 90;

        this.nextReactTargetIndex = 10;
        this.nextSpecialTargetIndex = 30;
        this.nextFindTargetIndex = 50;
    }

    public void applyDynamicAttributes() {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.creatureStats.getHealth());
        this.getAttribute(DEFENSE).setBaseValue(this.creatureStats.getDefense());
        this.getAttribute(Attributes.ARMOR).setBaseValue(this.creatureStats.getArmor());
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(this.creatureStats.getSpeed());
        this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(this.creatureStats.getKnockbackResistance());
        this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(this.creatureStats.getSight());
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(this.creatureStats.getDamage());
        this.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(this.creatureStats.getAttackSpeed());
        this.getAttribute(RANGED_SPEED).setBaseValue(this.creatureStats.getRangedSpeed());
    }

    public void refreshAttributes() {
        this.applyDynamicAttributes();
        this.setHealth(this.getMaxHealth());
    }

    public static int getBossDamageLimit() {
        return BOSS_DAMAGE_LIMIT;
    }

    public static void setBossDamageLimit(int bossDamageLimit) {
        BOSS_DAMAGE_LIMIT = bossDamageLimit;
    }

    public CreatureInfo getCreatureInfo() {
        return this.creatureInfo;
    }

    public CreatureStats getCreatureStats() {
        return this.creatureStats;
    }

    public ExtraMobBehaviour getExtraMobBehaviour() {
        return this.extraMobBehaviour;
    }

    public int getTamingReputation() {
        return this.creatureInfo.getTamingReputation();
    }

    public int getFriendlyReputation() {
        return this.creatureInfo.getFriendlyReputation();
    }

    public CreatureRelationships getRelationships() {
        return this.relationships;
    }

    @Nullable
    public CreatureRelationshipEntry getRelationshipEntry(LivingEntity entity) {
        if (this.relationships == null) {
            return null;
        }
        return this.relationships.getEntry(entity);
    }

    public CreatureRelationshipEntry getOrCreateRelationshipEntry(Player player) {
        return this.relationships.getOrCreateEntry(player);
    }

    public CreatureType getCreatureType() {
        return this.creatureInfo.getCreatureType();
    }

    public Component getCreatureTitle() {
        return this.creatureInfo.getTitle();
    }

    public int getExperienceForNextLevel() {
        return this.creatureStats.getExperienceForNextLevel();
    }

    public double getSizeScale() {
        return this.sizeScale;
    }

    public byte getAttackPhaseMax() {
        return this.attackPhaseMax;
    }

    public float getRenderTick() {
        return this.renderTick;
    }

    public void advanceRenderTick(float partialTick) {
        this.renderTick += partialTick;
    }

    public float getOnlyRenderTicks() {
        return this.onlyRenderTicks;
    }

    public void setOnlyRenderTicks(float onlyRenderTicks) {
        this.onlyRenderTicks = onlyRenderTicks;
    }

    public String getCreatureDefinitionName() {
        return this.creatureInfo.getName();
    }

    public boolean canEat(ItemStack itemStack) {
        return this.creatureInfo.canEat(itemStack);
    }

    public boolean isFarmableCreature() {
        return this.creatureInfo.isFarmable();
    }

    public long getUpdateTick() {
        return this.updateTick;
    }

    public boolean isUpdateTickMultiple(int interval) {
        return interval > 0 && this.updateTick % interval == 0;
    }

    public boolean isFirstSpawn() {
        return this.firstSpawn;
    }

    public void markNotFirstSpawn() {
        this.firstSpawn = false;
    }

    public String getSpawnEventType() {
        return this.spawnEventType;
    }

    public boolean hasSpawnEvent() {
        return !"".equals(this.spawnEventType);
    }

    public void applySpawnEvent(String spawnEventType, int spawnEventCount) {
        this.spawnEventType = spawnEventType != null ? spawnEventType : "";
        this.spawnEventCount = spawnEventCount;
    }

    public void inheritSpawnEventFrom(BaseCreatureEntity source) {
        this.spawnEventType = source.spawnEventType;
        this.spawnEventCount = source.spawnEventCount;
    }

    // NOTE: original backed this with a forceNoDespawn field (dropped during the Phase 5 trim,
    // set via applySpawnerSpawnState()/setPersistenceRequired() which weren't ported) - always
    // false here; AgeableCreatureEntity's override (hasBeenFarmed) is still meaningful on top.
    public boolean isPersistant() {
        return false;
    }

    /**
     * Phase 5e: the official despawn system (despawnCheck/canDespawnNaturally) isn't ported, so route this port's
     * isPersistant() (true for tamed creatures, see TameableCreatureEntity) into vanilla's persistence check -
     * otherwise tamed pets would despawn like wild mobs.
     **/
    @Override
    public boolean requiresCustomPersistence() {
        return this.isPersistant() || super.requiresCustomPersistence();
    }

    public void configureExtraBehaviourGoals(boolean attackPlayers, boolean defendAnimals) {
        this.targetSelector.removeGoal(this.aiTargetPlayer);
        if (attackPlayers) {
            if (this.aiTargetPlayer == null) {
                this.aiTargetPlayer = new FindAttackTargetGoal(this).addTargets(EntityType.PLAYER);
            }
            this.targetSelector.addGoal(9, this.aiTargetPlayer);
        }

        this.targetSelector.removeGoal(this.aiDefendAnimals);
        if (defendAnimals) {
            if (this.aiDefendAnimals == null) {
                this.aiDefendAnimals = new RevengeGoal(this).setHelpClasses(Animal.class);
            }
            this.targetSelector.addGoal(10, this.aiDefendAnimals);
        }
    }

    public boolean setDirectNavigationTarget(BlockPos targetPosition, double speedModifier) {
        return this.directNavigator.setTargetPosition(targetPosition, speedModifier);
    }

    public boolean clearDirectNavigationTarget(double speedModifier) {
        return this.directNavigator.clearTargetPosition(speedModifier);
    }

    public boolean isDirectNavigationAtTarget() {
        return this.directNavigator.atTargetPosition();
    }

    public boolean isDirectNavigationTargetValid() {
        return this.directNavigator.isTargetPositionValid();
    }

    public void loadCreatureFlags() {
    }

    public int claimPriorityGoalIndex() {
        return this.nextPriorityGoalIndex++;
    }

    public int claimDistractionGoalIndex() {
        return this.nextDistractionGoalIndex++;
    }

    public int claimTravelGoalIndex() {
        return this.nextTravelGoalIndex++;
    }

    public int claimCombatGoalIndex() {
        return this.nextCombatGoalIndex++;
    }

    public int claimIdleGoalIndex() {
        return this.nextIdleGoalIndex++;
    }

    public int claimFindTargetGoalIndex() {
        return this.nextFindTargetIndex++;
    }

    public int claimReactTargetGoalIndex() {
        return this.nextReactTargetIndex++;
    }

    public int claimSpecialTargetGoalIndex() {
        return this.nextSpecialTargetIndex++;
    }

    /**
     * Registers all AI Goals for this entity.
     * Trimmed: PaddleGoal/StayByWaterGoal/AvoidGoal/TemptGoal/FindFuseTargetGoal/FollowFuseGoal
     * (IFusable not ported)/FindGroupAttackTargetGoal/FindGroupAvoidTargetGoal/FollowMasterGoal/
     * WatchClosestGoal are not ported yet - see PORT_PLAN.md Phase 6.
     */
    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new AvoidIfHitGoal(this).setHelpCall(true));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new RevengeGoal(this).setHelpCall(true).setCheckSight(true));

        super.registerGoals();

        this.goalSelector.addGoal(this.claimIdleGoalIndex(), new WanderGoal(this));
        this.goalSelector.addGoal(this.claimIdleGoalIndex(), new LookIdleGoal(this));
    }

    /**
     * The final setup stage when constructing this entity, should be called last by the constructors of each specific entity class.
     * NOTE: original also built a CreatureInventory + equipment-part drops here; CreatureInventory
     * (container subsystem) isn't ported yet.
     */
    public void setupMob() {
        this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.5D);
        this.loadItemDrops();
        this.setAttackCooldownMax(this.attackCooldownMax);
    }

    public void loadItemDrops() {
        this.drops.addAll(this.creatureInfo.getDrops());
    }

    public void addSavedItemDrop(ItemDrop itemDrop) {
        this.drops.add(itemDrop);
        this.savedDrops.add(itemDrop);
    }

    // NOTE: full actual drop-on-death subsystem (dropCustomDeathLoot iterating this.drops) isn't
    // ported yet - this exists only so subclasses (AgeableCreatureEntity's adult-only drop gate)
    // have something to override.
    public boolean canDropItem(ItemDrop itemDrop) {
        return true;
    }

    private void initializePathing() {
        if (!this.canBurn()) {
            this.setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
            if (this.canBreatheUnderlava()) {
                this.setPathfindingMalus(PathType.LAVA, 1.0F);
                if (!this.canBreatheAir()) {
                    this.setPathfindingMalus(PathType.LAVA, 8.0F);
                }
            }
        }

        if (this.waterDamage()) {
            this.setPathfindingMalus(PathType.WATER, -1.0F);
        } else if (this.creatureCanBreatheUnderwater()) {
            this.setPathfindingMalus(PathType.WATER, 1.0F);
            if (!this.canBreatheAir()) {
                this.setPathfindingMalus(PathType.WATER, 8.0F);
            }
        }

        if (this.canWade() && this.getNavigation() instanceof CreaturePathNavigator pathNavigator) {
            pathNavigator.setCanFloat(true);
        }
    }

    // NOTE: LivingEntity.getExperienceReward(ServerLevel, Entity) is now final in 1.21.1
    // (XP drop routed through EventHooks.getExperienceDrop) - can no longer override it here.
    // Kept as a plain helper for future use (e.g. custom drop logic elsewhere).
    public int computeExperienceReward() {
        float scaledExp = this.creatureInfo.getExperience();
        if (this.getVariant() != null) {
            if ("uncommon".equals(this.getVariant().getRarity())) {
                scaledExp = Math.round((float) (this.creatureInfo.getExperience() * Variant.getUncommonExperienceScale()));
            } else if ("rare".equals(this.getVariant().getRarity())) {
                scaledExp = Math.round((float) (this.creatureInfo.getExperience() * Variant.getRareExperienceScale()));
            }
        }
        return Math.round(scaledExp);
    }

    @Override
    public Component getName() {
        if (this.hasCustomName()) {
            return this.getCustomName();
        }
        return this.getFullName();
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.clearCachedFullName();
    }

    public Component getFullName() {
        if (this.cachedFullName != null) {
            return this.cachedFullName.copy();
        }

        String nameFormatting = Component.translatable("entity.lycanitesmobs.creature.name.format").getString();
        String[] nameParts = nameFormatting.split("\\|");
        if (nameParts.length < 4 || nameFormatting.equals("entity.lycanitesmobs.creature.name.format")) {
            nameParts = new String[]{"age", "variant", "subspecies", "species", "level"};
        }

        MutableComponent name = Component.literal("");
        List<Component> nameComponents = new ArrayList<>();
        for (String namePart : nameParts) {
            switch (namePart) {
                case "age":
                    nameComponents.add(this.getAgeName());
                    break;
                case "variant":
                    nameComponents.add(this.getVariantName());
                    break;
                case "subspecies":
                    nameComponents.add(this.getSubspeciesName());
                    break;
                case "species":
                    nameComponents.add(this.getSpeciesName());
                    break;
                case "level":
                    nameComponents.add(this.getLevelName());
                    break;
            }
        }

        boolean first = true;
        for (Component nameComponent : nameComponents) {
            if (nameComponent.getString().isEmpty()) {
                continue;
            }
            if (!first) {
                name.append(" ");
            }
            first = false;
            name.append(nameComponent);
        }

        this.cachedFullName = name;
        return name.copy();
    }

    private void clearCachedFullName() {
        this.cachedFullName = null;
    }

    public Component getSpeciesName() {
        return this.creatureInfo.getTitle();
    }

    public Component getAgeName() {
        return Component.literal("");
    }

    public Component getVariantName() {
        if (this.getVariant() != null) {
            return this.getVariant().getTitle();
        }
        return Component.literal("");
    }

    public Component getSubspeciesName() {
        if (this.getSubspecies() != null) {
            return this.getSubspecies().getTitle();
        }
        return Component.literal("");
    }

    public Component getLevelName() {
        if (this.getMobLevel() < 2) {
            return Component.literal("");
        }
        return Component.translatable("entity.level").append(" " + this.getMobLevel());
    }

    // NOTE: networked advanced-sync (MessageCreature) not ported - no-ops for now.
    public void queueSync() {
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == LEVEL || key == SUBSPECIES || key == VARIANT) {
            this.clearCachedFullName();
        }
    }

    public boolean getBoolFromDataManager(EntityDataAccessor<Boolean> key) {
        try {
            return this.getEntityData().get(key);
        } catch (Exception e) {
            return false;
        }
    }

    public byte getByteFromDataManager(EntityDataAccessor<Byte> key) {
        try {
            return this.getEntityData().get(key);
        } catch (Exception e) {
            return 0;
        }
    }

    public int getIntFromDataManager(EntityDataAccessor<Integer> key) {
        try {
            return this.getEntityData().get(key);
        } catch (Exception e) {
            return 0;
        }
    }

    public float getFloatFromDataManager(EntityDataAccessor<Float> key) {
        try {
            return this.getEntityData().get(key);
        } catch (Exception e) {
            return 0;
        }
    }

    // NOTE: full natural-spawn eligibility chain (light level, biome, group/boss-proximity
    // limits) not ported yet - always allows. Fine for /summon or spawn-egg testing.
    @Override
    public boolean checkSpawnRules(LevelAccessor world, MobSpawnType spawnReason) {
        return true;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType spawnReason, @Nullable SpawnGroupData livingEntityData) {
        return super.finalizeSpawn(world, difficultyInstance, spawnReason, livingEntityData);
    }

    public boolean isLavaCreature() {
        return this.isLavaCreature;
    }

    public boolean wasSpawnedRare() {
        return this.spawnedRare;
    }

    public void setSpawnedRare(boolean spawnedRare) {
        this.spawnedRare = spawnedRare;
    }

    public boolean wasSpawnedAsBoss() {
        return this.spawnedAsBoss;
    }

    public void setSpawnedAsBoss(boolean spawnedAsBoss) {
        this.spawnedAsBoss = spawnedAsBoss;
    }

    /**
     * Returns whether or not this mob is a boss.
     * NOTE: boss health bar UI (ServerBossEvent) not ported - this only affects damage
     * scaling/sound volume/targeting checks for now, no visible bar.
     */
    public boolean isBoss() {
        return this.isBossAlways() || this.spawnedAsBoss;
    }

    public boolean isBossAlways() {
        return this.creatureInfo.isBoss();
    }

    @Override
    public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
        return !this.isBoss();
    }

    // NOTE: LivingEntity.getDimensions(Pose) is now final in 1.21.1 (computes
    // getDefaultDimensions(pose).scale(getScale())) - override this hook instead.
    @Nonnull
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        if (this.creatureSize == null) {
            this.creatureSize = this.getType().getDimensions();
        }
        return this.creatureSize;
    }

    public void setSizeScale(double scale) {
        this.sizeScale = scale;
        this.refreshDimensions();
    }

    @Override
    public float getScale() {
        return (float) this.sizeScale * (float) this.creatureInfo.getSizeScale();
    }

    public int getMobLevel() {
        if (this.getCommandSenderWorld().isClientSide) {
            return this.getIntFromDataManager(LEVEL);
        }
        return this.mobLevel;
    }

    public void applyLevel(int level) {
        this.needsInitialLevel = false;
        this.setLevel(level);
        this.refreshAttributes();
    }

    public void setLevel(int level) {
        this.mobLevel = level;
        this.getEntityData().set(LEVEL, level);
        this.clearCachedFullName();
    }

    public void addLevel(int level) {
        this.applyLevel(this.mobLevel + level);
    }

    public void updateLevelExperience() {
        if (!this.getCommandSenderWorld().isClientSide) {
            this.getEntityData().set(EXPERIENCE, this.experience);
        }
        if (this.getExperience() >= this.creatureStats.getExperienceForNextLevel()) {
            this.setExperience(this.getExperience() - this.creatureStats.getExperienceForNextLevel());
            this.addLevel(1);
        }
    }

    public int getExperience() {
        if (this.getCommandSenderWorld().isClientSide) {
            return this.getIntFromDataManager(EXPERIENCE);
        }
        return this.experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
        this.updateLevelExperience();
    }

    public int getMeleeCooldown() {
        return Math.round((float) (1.0D / this.getAttribute(Attributes.ATTACK_SPEED).getValue() * 20.0D));
    }

    public void applyVariant(int variantIndex) {
        this.setVariant(variantIndex);
        this.refreshAttributes();
    }

    public Subspecies getSubspecies() {
        if (this.subspecies == null) {
            this.subspecies = this.creatureInfo.getSubspecies(0);
        }
        return this.subspecies;
    }

    public void setSubspecies(int subspeciesIndex) {
        this.subspecies = this.creatureInfo.getSubspecies(subspeciesIndex);
        this.clearCachedFullName();
    }

    @Nullable
    public Variant getVariant() {
        return this.variant;
    }

    public void setVariant(int variantIndex) {
        this.variant = this.getSubspecies().getVariant(variantIndex);
        this.clearCachedFullName();
    }

    public int getSubspeciesIndex() {
        return this.getSubspecies().getIndex();
    }

    /**
     * Returns this creature's main texture. Also checks for subspecies. Used by CreatureRenderer.
     **/
    public ResourceLocation getTexture() {
        return this.getTexture("");
    }

    public ResourceLocation getTexture(String suffix) {
        String textureName = this.getTextureName();
        if (this.getSubspecies().getName() != null) {
            textureName += "_" + this.getSubspecies().getName();
        }
        if (this.getVariant() != null) {
            textureName += "_" + this.getVariant().getColor();
        }
        if (!"".equals(suffix)) {
            textureName += "_" + suffix;
        }
        return AssetHelper.entityTexture(textureName);
    }

    /**
     * Gets the name of this creature's texture, normally links to its code name but can be overridden by subspecies and alpha creatures.
     **/
    public String getTextureName() {
        return this.creatureInfo.getName();
    }

    public int getVariantIndex() {
        return this.getVariant() != null ? this.getVariant().getIndex() : 0;
    }

    public boolean isRareVariant() {
        return this.getVariant() != null && "rare".equals(this.getVariant().getRarity());
    }

    public int getAge() {
        if (this.level().isClientSide) {
            return this.getBoolFromDataManager(BABY) ? -1 : 1;
        }
        return this.growingAge;
    }

    public float getBlockPathWeight(int x, int y, int z) {
        return 0.0F;
    }

    public boolean useDirectNavigator() {
        return false;
    }

    /**
     * Returns true if this entity should use swimming movement.
     **/
    public boolean shouldSwim() {
        if (!this.isInWater() && !this.isInLava()) {
            return false;
        }
        if (this.canWade() && this.creatureCanBreatheUnderwater()) {
            boolean targetInWater = true;
            if (this.getTarget() != null) {
                targetInWater = this.getTarget().isInWater();
            } else if (this.getParentTarget() != null) {
                targetInWater = this.getParentTarget().isInWater();
            } else if (this.getMasterTarget() != null) {
                targetInWater = this.getMasterTarget().isInWater();
            }
            if (!targetInWater) {
                BlockState blockState = this.getCommandSenderWorld().getBlockState(this.blockPosition().above());
                if (blockState.isAir()) {
                    return false;
                }
            }
            return true;
        }
        return this.isStrongSwimmer();
    }

    /**
     * Moves the entity, redirects to the direct navigator, swimming or flying movement when appropriate.
     **/
    @Override
    public void travel(Vec3 direction) {
        if (this.useDirectNavigator()) {
            this.directNavigator.flightMovement(direction.x(), direction.z());
            this.updateLimbSwing();
            return;
        }

        if (this.shouldSwim()) {
            this.travelSwimming(direction);
        } else if (this.isFlying()) {
            this.travelFlying(direction);
        } else {
            super.travel(direction);
        }
    }

    public void travelFlying(Vec3 direction) {
        double flightDampening = 0.91F;
        if (this.onGround()) {
            BlockPos below = this.blockPosition().below();
            flightDampening = this.getCommandSenderWorld().getBlockState(below).getFriction(this.getCommandSenderWorld(), below, this) * 0.91F;
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().multiply(flightDampening, flightDampening, flightDampening));
        this.updateLimbSwing();
    }

    public void travelSwimming(Vec3 direction) {
        this.moveRelative(0.1F, direction);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        if (!this.isMoving() && this.getTarget() == null && !this.isFlying()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
        }
        this.updateLimbSwing();
    }

    /**
     * Called when this entity is constructed for initial navigator.
     **/
    @Override
    protected PathNavigation createNavigation(Level world) {
        return new CreaturePathNavigator(this, world);
    }

    /**
     * Called from the constructor for the initial move controller (not a vanilla hook).
     **/
    protected MoveControl createMoveController() {
        return new CreatureMoveController(this);
    }

    public void updateLimbSwing() {
        double distanceX = this.position().x() - this.xo;
        double distanceZ = this.position().z() - this.zo;
        float distance = LMHelperClass.convertToFloat(Math.sqrt(distanceX * distanceX + distanceZ * distanceZ) * 4.0F);
        if (distance > 1.0F) {
            distance = 1.0F;
        }
        this.walkAnimation.update(distance, 0.4F);
    }

    public void clearMovement() {
        if (!this.useDirectNavigator() && this.getNavigation() != null) {
            this.getNavigation().stop();
        } else {
            this.clearDirectNavigationTarget(1.0D);
        }
    }

    public boolean rollLookChance() {
        return this.getRandom().nextFloat() < 0.02F;
    }

    public boolean rollWanderChance() {
        if (this.getBbWidth() >= 3) {
            return this.getRandom().nextDouble() <= 0.0005D;
        }
        return this.getRandom().nextDouble() <= 0.008D;
    }

    // NOTE: leashing was refactored into the Leashable interface in 1.21.1 (Mob implements
    // it directly now, tickLeash()/canBeLeashed(Player) no longer exist to override) - custom
    // leash-restriction AI (leashMoveTowardsRestrictionAI) dropped, vanilla Leashable behavior
    // is used unmodified.

    @Override
    public boolean isPushedByFluid() {
        return !this.isStrongSwimmer() && !this.isBoss();
    }

    public boolean isMoving() {
        if (!this.useDirectNavigator()) {
            return this.getNavigation().getPath() != null;
        }
        return !this.isDirectNavigationAtTarget();
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    // ==================================================
    //                      Damage
    // ==================================================
    /**
     * Trimmed (Phase 5e): the official hurt() also clears dropsRequirePlayerDamage, calls onDamage() and tracks
     * boss player damage - none of those exist in this port yet. Only the relationship reputation hit is kept.
     **/
    @Override
    public boolean hurt(DamageSource damageSrc, float damageAmount) {
        if (super.hurt(damageSrc, damageAmount)) {
            this.updateAttackerReputation(damageSrc);
            return true;
        }
        return false;
    }

    private void updateAttackerReputation(DamageSource damageSrc) {
        Entity entity = damageSrc.getDirectEntity();
        if (entity instanceof ThrowableProjectile projectile) {
            entity = projectile.getOwner();
        }

        if (entity instanceof LivingEntity livingEntity && this.getRider() != entity && this.getVehicle() != entity) {
            if (entity != this) {
                this.setLastHurtByMob(livingEntity);

                int reputationAmount = 50 + this.getRandom().nextInt(50);
                this.relationships.getOrCreateEntry(entity).decreaseReputation(reputationAmount);
            }
        }
    }

    // ==================================================
    //                    Interaction
    // ==================================================
    // Ported in Phase 5e (the taming/pet command chain runs through this). Changes from the official
    // source: canBeLeashed(Player) -> 1.21's no-arg canBeLeashed(); the Soulgazer command is omitted
    // (TODO(port): add back with ItemSoulgazer / the Beastiary knowledge system).

    /**
     * The main interact method that is called when a player right clicks this entity.
     **/
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.hasPerchTarget()) {
            return InteractionResult.FAIL;
        }
        ItemStack itemStack = player.getItemInHand(hand);
        if (this.assessInteractCommand(this.getInteractCommands(player, itemStack), player, itemStack, hand)) {
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Performs the best possible command and returns true or false if there isn't one.
     **/
    public boolean assessInteractCommand(HashMap<Integer, String> commands, Player player, ItemStack itemStack, InteractionHand hand) {
        Integer priority = this.getTopInteractCommandPriority(commands);
        if (priority == null) {
            return false;
        }
        return this.performCommand(commands.get(priority), player, itemStack, hand);
    }

    private Integer getTopInteractCommandPriority(HashMap<Integer, String> commands) {
        if (commands.isEmpty()) {
            return null;
        }
        int priority = 100;
        for (int testPriority : commands.keySet()) {
            if (testPriority < priority) {
                priority = testPriority;
            }
        }
        return commands.containsKey(priority) ? priority : null;
    }

    /**
     * Gets a map of all possible interact events with the key being the priority, lower is better.
     **/
    public HashMap<Integer, String> getInteractCommands(Player player, @Nonnull ItemStack itemStack) {
        HashMap<Integer, String> commands = new HashMap<>();

        if (!itemStack.isEmpty()) {
            if (itemStack.getItem() == Items.LEAD && this.canBeLeashed()) {
                commands.put(COMMAND_PIORITIES.ITEM_USE.id, "Leash");
            }

            if (itemStack.getItem() == Items.NAME_TAG) {
                if (this.canNameTag(player)) {
                    return new HashMap<>();
                }
                commands.put(COMMAND_PIORITIES.ITEM_USE.id, "Name Tag");
            }

            if (this.canBeColored(player) && itemStack.getItem() instanceof DyeItem) {
                commands.put(COMMAND_PIORITIES.ITEM_USE.id, "Color");
            }
        }

        return commands;
    }

    /**
     * Performs the given interact command. Could be used outside of the interact method if needed.
     *
     * @return True if the player's item should not activate, false if it should.
     */
    public boolean performCommand(String command, Player player, ItemStack itemStack, InteractionHand hand) {
        if ("Leash".equals(command)) {
            this.setLeashedTo(player, true);
            this.consumePlayersItem(player, itemStack);
            return true;
        }

        if ("Color".equals(command) && itemStack.getItem() instanceof DyeItem dye) {
            DyeColor color = dye.getDyeColor();
            if (color != this.getColor()) {
                this.setColor(color);
                this.consumePlayersItem(player, itemStack);
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if this mob can be given a new name with a name tag by the provided player entity.
     **/
    public boolean canNameTag(Player player) {
        return true;
    }

    /**
     * Returns true if this mob can be dyed by the provided player.
     **/
    public boolean canBeColored(Player player) {
        return false;
    }

    /**
     * Consumes 1 item from the the item stack currently held by the specified player.
     **/
    public void consumePlayersItem(Player player, ItemStack itemStack) {
        this.consumePlayersItem(player, itemStack, 1);
    }

    /**
     * Consumes the specified amount from the item stack currently held by the specified player.
     **/
    public void consumePlayersItem(Player player, ItemStack itemStack, int amount) {
        if (!player.getAbilities().invulnerable) {
            itemStack.shrink(amount);
        }
    }

    /**
     * Replaces 1 of the specified itemstack with a new itemstack.
     **/
    public void replacePlayersItem(Player player, InteractionHand hand, ItemStack itemStack, ItemStack newStack) {
        player.setItemInHand(hand, ItemUtils.createFilledResult(itemStack, player, newStack));
    }

    /**
     * Replaces the specified itemstack and amount with a new itemstack.
     **/
    public void replacePlayersItem(Player player, InteractionHand hand, ItemStack itemStack, int amount, ItemStack newStack) {
        if (!player.getAbilities().invulnerable) {
            itemStack.shrink(amount);
        }

        if (itemStack.isEmpty()) {
            player.setItemInHand(hand, newStack);
        } else if (!player.getInventory().add(newStack)) {
            player.drop(newStack, false);
        }
    }

    /**
     * Called by pet/creature GUIs via a network packet. TODO(port): GUI refresh scheduling (guiViewers)
     * comes with the creature GUIs in Phase 8.
     **/
    public void performGUICommand(Player player, int guiCommandID) {
    }

    public enum COMMAND_PIORITIES {
        OVERRIDE(0), IMPORTANT(1), EQUIPPING(2), ITEM_USE(3), EMPTY_HAND(4), MAIN(5);
        public final int id;

        COMMAND_PIORITIES(int value) {
            this.id = value;
        }

        public int getValue() {
            return id;
        }
    }

    /**
     * A list of GUI command IDs to be used by pet or creature GUIs via a network packet.
     **/
    public enum GUI_COMMAND {
        CLOSE((byte) 0), SITTING((byte) 1), FOLLOWING((byte) 2), PASSIVE((byte) 3), STANCE((byte) 4), PVP((byte) 5), TELEPORT((byte) 6), SPAWNING((byte) 7), RELEASE((byte) 8);
        public final byte id;

        GUI_COMMAND(byte i) {
            id = i;
        }
    }

    // NOTE: 1.21.1's vanilla Mob now has its own restrictCenter/restrictRadius/restrictTo()/
    // getRestrictCenter()/getRestrictRadius()/hasRestriction() - the original 1.20.1 port's
    // custom homePosition/homeDistanceMax fields duplicated this, so they're dropped in favor
    // of the vanilla ones. getHomeDistanceMax() kept as a thin compat wrapper since the ported
    // goal classes (RandomPositionGenerator, MoveRestrictionGoal) already call it.
    public float getHomeDistanceMax() {
        return this.getRestrictRadius();
    }

    public boolean hasHome() {
        return this.hasRestriction();
    }

    public boolean positionNearHome(int x, int y, int z) {
        if (!this.hasHome()) {
            return true;
        }
        return this.getDistanceFromHome(x, y, z) < this.getRestrictRadius();
    }

    /** Distance from this creature's current position to its home (vanilla restriction center), 0 if it has none. */
    public double getDistanceFromHome() {
        return this.getDistanceFromHome(this.getBlockX(), this.getBlockY(), this.getBlockZ());
    }

    public double getDistanceFromHome(int x, int y, int z) {
        if (!this.hasHome()) {
            return 0;
        }
        return Math.sqrt(this.getRestrictCenter().distSqr(new Vec3i(x, y, z)));
    }

    public BlockPos getWanderPosition(BlockPos wanderPosition) {
        return wanderPosition;
    }

    @Override
    public boolean canAttackType(EntityType<?> entityType) {
        return true;
    }

    public boolean canAttack(LivingEntity targetEntity) {
        if (this.getCommandSenderWorld().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL && targetEntity instanceof Player) {
            return false;
        }

        if (!Targeting.isValidTarget(this, targetEntity)) {
            return false;
        }

        if (targetEntity instanceof Player targetPlayer && targetPlayer.getAbilities().invulnerable) {
            return false;
        }

        if (this.isAlliedTo(targetEntity)) {
            return false;
        }

        CreatureRelationshipEntry relationshipEntry = this.relationships.getEntry(targetEntity);
        if (relationshipEntry != null && !relationshipEntry.canAttack()) {
            return false;
        }

        if (targetEntity instanceof BaseCreatureEntity targetCreature) {
            if (!this.canAttackOwnSpecies() && targetCreature.creatureInfo == this.creatureInfo && !targetCreature.isTamed()) {
                return false;
            }

            if (targetCreature.getMasterTarget() == this) {
                return false;
            }

            if (!this.isTamed()) {
                if (targetCreature.isBoss()) {
                    return false;
                }
                if (this.isRareVariant()) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean canAttackOwnSpecies() {
        return false;
    }

    public double getPhysicalRange() {
        double range = this.getDimensions(Pose.STANDING).width() + 1.5D;
        if (this.isFlying()) {
            range += this.getFlightOffset();
        }
        return range * range;
    }

    public double getMeleeAttackRange(LivingEntity attackTarget, double additionalReach) {
        double creatureRange = this.getPhysicalRange();
        double targetSize = 1;
        if (attackTarget != null) {
            targetSize = (attackTarget.getDimensions(Pose.STANDING).width() + 1) * (attackTarget.getDimensions(Pose.STANDING).width() + 1);
        }
        return creatureRange + targetSize + additionalReach;
    }

    public boolean shouldCreatureGroupRevenge(LivingEntity target) {
        boolean shouldRevenge = this.creatureInfo.getGroups().isEmpty();
        boolean shouldPackHunt = false;
        for (CreatureGroup group : this.creatureInfo.getGroups()) {
            if (group.shouldRevenge(target)) {
                shouldRevenge = true;
            }
            if (group.shouldPackHunt(target)) {
                shouldPackHunt = true;
            }
        }
        boolean canPackHunt = shouldPackHunt && this.isInPack();
        return shouldRevenge || canPackHunt;
    }

    public LivingEntity getMasterAttackTarget() {
        if (this.getMasterTarget() instanceof Mob mobTarget) {
            return mobTarget.getTarget();
        }
        return null;
    }

    /**
     * Used to make this entity perform a melee attack on the target entity with the given damage scale.
     */
    public boolean attackMelee(Entity target, double damageScale) {
        if (this.isBlocking() && !this.canAttackWhileBlocking()) {
            return false;
        }

        if (!this.attackEntityAsMob(target, damageScale)) {
            return false;
        }

        this.applyContactAttackEffects(target, true);
        this.finishAttackAction();
        return true;
    }

    // NOTE: fire-spread and on-hit status effect application (applyDebuffs) not ported yet.
    private void applyContactAttackEffects(Entity target, boolean includeFireSpread) {
    }

    private void finishAttackAction() {
        this.triggerAttackCooldown();
        this.playAttackSound();
    }

    public byte getAttackPhase() {
        return this.getByteFromDataManager(ATTACK_PHASE);
    }

    public void setAttackPhase(byte setAttackPhase) {
        this.attackPhase = setAttackPhase;
    }

    public float getAttackDamage(double damageScale) {
        float damage = (float) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        damage *= damageScale;
        return damage;
    }

    public net.minecraft.world.damagesource.DamageSource getDamageSource(net.minecraft.world.damagesource.DamageSource nestedDamageSource) {
        if (nestedDamageSource != null) {
            return nestedDamageSource;
        }
        return this.level().damageSources().mobAttack(this);
    }

    public boolean canInteruptShields(boolean checkAbility) {
        return false;
    }

    /**
     * Called when attacking and makes this entity actually deal damage to the target entity.
     * Trimmed: enchantment knockback/fire-aspect/shield-interrupt handling dropped (dead code
     * paths anyway since canInteruptShields() always returns false), pierce damage kept.
     */
    public boolean attackEntityAsMob(Entity target, double damageScale) {
        if (!this.isAlive() || target == null || !this.hasLineOfSight(target)) {
            return false;
        }

        float damage = this.getAttackDamage(damageScale);
        double pierceDamage = this.creatureStats.getPierce();

        boolean attackSuccess;
        if (damage <= pierceDamage) {
            attackSuccess = target.hurt(this.getDamageSource(null), damage);
        } else {
            if (pierceDamage > 0) {
                int hurtResistantTimeBefore = target.invulnerableTime;
                target.hurt(this.getDamageSource(null), (float) pierceDamage);
                target.invulnerableTime = hurtResistantTimeBefore;
                damage -= pierceDamage;
            }
            attackSuccess = target.hurt(this.getDamageSource(null), damage);
        }

        return attackSuccess;
    }

    public boolean isAggressive() {
        if (this.extraMobBehaviour != null && this.extraMobBehaviour.aggressiveOverride()) {
            return true;
        }
        return this.isAggressiveByDefault;
    }

    public boolean isHostileTo(Entity target) {
        if (target == null) {
            return false;
        }
        if (this.hostileTargets.contains(target.getType())) {
            return true;
        }
        if (this.hostileTargetClasses.contains(target.getClass())) {
            return true;
        }
        for (CreatureGroup group : this.creatureInfo.getGroups()) {
            if (group.shouldHunt(target) || group.shouldPackHunt(target)) {
                return true;
            }
        }
        return false;
    }

    public void setHostileTo(EntityType targetType) {
        if (this.hostileTargets == null) {
            this.hostileTargets = new ArrayList<>();
        }
        if (this.hostileTargets.contains(targetType)) {
            return;
        }
        this.hostileTargets.add(targetType);
    }

    public void setHostileTo(Class<? extends Entity> targetClass) {
        if (this.hostileTargetClasses == null) {
            this.hostileTargetClasses = new ArrayList<>();
        }
        if (this.hostileTargetClasses.contains(targetClass)) {
            return;
        }
        this.hostileTargetClasses.add(targetClass);
    }

    public boolean isProtective(Entity entity) {
        return entity.getClass() == this.getClass();
    }

    public boolean hasAttackTarget() {
        return this.getTarget() != null;
    }

    public LivingEntity getMasterTarget() {
        return this.masterTarget;
    }

    public void setMasterTarget(LivingEntity setTarget) {
        this.masterTarget = setTarget;
    }

    public LivingEntity getParentTarget() {
        return this.parentTarget;
    }

    public void setParentTarget(LivingEntity setTarget) {
        this.parentTarget = setTarget;
    }

    public boolean hasParent() {
        return this.getParentTarget() != null;
    }

    public LivingEntity getAvoidTarget() {
        return this.avoidTarget;
    }

    public void setAvoidTarget(LivingEntity setTarget) {
        this.currentFleeTime = this.fleeTime;
        this.avoidTarget = setTarget;
    }

    public boolean hasAvoidTarget() {
        return this.getAvoidTarget() != null;
    }

    public LivingEntity getFixateTarget() {
        return this.fixateTarget;
    }

    public void setFixateTarget(LivingEntity target) {
        this.fixateTarget = target;
    }

    public boolean hasFixateTarget() {
        return this.getFixateTarget() != null;
    }

    public LivingEntity getPerchTarget() {
        return this.perchTarget;
    }

    public void setPerchTarget(LivingEntity setTarget) {
        this.perchTarget = setTarget;
    }

    public boolean hasPerchTarget() {
        return this.getPerchTarget() != null;
    }

    public boolean rollAttackTargetChance(LivingEntity target) {
        return true;
    }

    @Override
    public boolean hasLineOfSight(Entity target) {
        return super.hasLineOfSight(target);
    }

    public LivingEntity getRider() {
        return this.getControllingPassenger();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        if (this.getPassengers().isEmpty()) return null;
        if (this.getPassengers().get(0) instanceof LivingEntity firstPassenger) {
            return firstPassenger;
        }
        return null;
    }

    public boolean canBeControlledByRider() {
        return false;
    }

    public boolean isInPack() {
        int packSize = this.creatureInfo.getPackSize();
        if (packSize <= 1) {
            return true;
        }

        int packCheckInterval = 10;
        if (this.packCheckTick != Long.MIN_VALUE && this.updateTick - this.packCheckTick < packCheckInterval) {
            return this.cachedInPack;
        }

        this.cachedInPack = this.countAllies(10) >= packSize;
        this.packCheckTick = this.updateTick;
        return this.cachedInPack;
    }

    public int countAllies(double range) {
        return this.getNearbyEntities(Entity.class, entity -> entity.getType() == this.getType(), range).size();
    }

    public <T extends Entity> List<T> getNearbyEntities(Class<? extends T> clazz, Predicate<Entity> predicate, double range) {
        return (List<T>) this.getCommandSenderWorld().getEntitiesOfClass(clazz, this.getBoundingBox().inflate(range, range, range), predicate != null ? predicate : (e) -> true);
    }

    public int getBattlePhase() {
        return this.battlePhase;
    }

    public void setBattlePhase(int phase) {
        if (this.getBattlePhase() == phase) {
            return;
        }
        this.battlePhase = phase;
    }

    public boolean isTamed() {
        return false;
    }

    public boolean canMove() {
        return !this.isBlocking();
    }

    public boolean canWalk() {
        return true;
    }

    public boolean canWade() {
        return true;
    }

    @Override
    public boolean isUnderWater() {
        if (this.isLavaCreature) {
            return this.isInLava();
        }
        return super.isUnderWater();
    }

    public boolean isStrongSwimmer() {
        return this.extraMobBehaviour != null && this.extraMobBehaviour.swimmingOverride();
    }

    public boolean canClimb() {
        return false;
    }

    /**
     * S202 fix (not in the official source): true when in water, or in lava for lava creatures. The official
     * navigator only checked isInWater(), so lava creatures never pathed in lava. Lava fish still don't move -
     * open issue, see PORT_PLAN.md "Phase 5g".
     **/
    public boolean isInSwimmableFluid() {
        return this.isInWater() || (this.isLavaCreature() && this.isInLava());
    }

    /**
     * Returns true if this entity should swim to the liquid surface when pathing, by default entities that can't breathe underwater will try to surface.
     **/
    public boolean shouldFloat() {
        return !this.creatureCanBreatheUnderwater() && !this.canBreatheUnderlava();
    }

    /**
     * Returns true if this entity should dive underwater/underlava when pathing, by default entities that can breathe underwater or underlava will try to dive.
     **/
    public boolean shouldDive() {
        return this.creatureCanBreatheUnderwater() || this.canBreatheUnderlava();
    }

    /**
     * Returns true if this mob should be damaged by the sun.
     **/
    public boolean daylightBurns() {
        return false;
    }

    /**
     * Returns true if this mob should be damaged by extreme cold such as from ooze.
     **/
    public boolean canFreeze() {
        for (com.lycanitesmobs.core.data.info.element.ElementInfo element : this.getElements()) {
            if (!element.canFreeze()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if this entity is climbing a ladder or wall, can be used for animation.
     **/
    @Override
    public boolean onClimbable() {
        if (this.isFlying() || (this.isStrongSwimmer() && this.isInWater())) {
            return false;
        }
        if (this.canClimb()) {
            return this.isBesideClimbableBlock();
        }
        return super.onClimbable();
    }

    /**
     * Flying creatures take no fall damage. Trimmed (Phase 5g): TODO(port) the official getFallResistance()
     * reduction for non-flyers.
     **/
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        if (this.isFlying()) {
            return false;
        }
        return super.causeFallDamage(fallDistance, damageMultiplier, source);
    }

    /**
     * Returns whether or not this mob is next to a climbable block.
     **/
    public boolean isBesideClimbableBlock() {
        return (this.getByteFromDataManager(CLIMBING) & 1) != 0;
    }

    /**
     * Used to set whether this mob is climbing up a block or not.
     **/
    public void setBesideClimbableBlock(boolean collided) {
        if (this.canClimb()) {
            byte climbing = this.getByteFromDataManager(CLIMBING);
            if (collided) {
                climbing = (byte) (climbing | 1);
            } else {
                climbing &= -2;
            }
            this.getEntityData().set(CLIMBING, climbing);
        }
    }

    public java.util.List<com.lycanitesmobs.core.data.info.element.ElementInfo> getElements() {
        return this.creatureInfo.getElements(this.getSubspecies());
    }

    public boolean hasElement(com.lycanitesmobs.core.data.info.element.ElementInfo element) {
        return this.getElements().contains(element);
    }

    public boolean canBurn() {
        if (this.extraMobBehaviour != null && this.extraMobBehaviour.fireImmunityOverride()) {
            return false;
        }
        for (com.lycanitesmobs.core.data.info.element.ElementInfo element : this.getElements()) {
            if (!element.canBurn()) {
                return false;
            }
        }
        return true;
    }

    public boolean waterDamage() {
        return false;
    }

    public boolean canBreatheAir() {
        return true;
    }

    // NOTE: LivingEntity.canBreatheUnderwater() is final in 1.21.1 (now tag-driven, deprecated
    // in favor of NeoForge's canDrownInFluidType) - renamed to avoid the signature collision.
    public boolean creatureCanBreatheUnderwater() {
        return this.extraMobBehaviour != null && this.extraMobBehaviour.waterBreathingOverride();
    }

    public boolean canBreatheUnderlava() {
        return true;
    }

    /**
     * Phase 5g fix: 1.21 routes drowning through NeoForge's canDrownInFluidType() instead of an overridable
     * canBreatheUnderwater(), so without this every water-breathing creature drowned in water.
     **/
    @Override
    public boolean canDrownInFluidType(FluidType type) {
        if (type == NeoForgeMod.WATER_TYPE.value()) {
            return !this.creatureCanBreatheUnderwater();
        }
        if (type == NeoForgeMod.LAVA_TYPE.value()) {
            return !this.canBreatheUnderlava();
        }
        return super.canDrownInFluidType(type);
    }

    /**
     * Returns the amount of air gained for the tick. Drowning in water is handled by LivingEntity and this isn't called in that case.
     **/
    @Override
    protected int increaseAirSupply(int currentAir) {
        if (this.creatureCanBreatheUnderwater() && this.waterContact()) {
            return super.increaseAirSupply(currentAir);
        }
        if (this.canBreatheUnderlava() && this.lavaContact()) {
            return super.increaseAirSupply(currentAir);
        }
        if (this.canBreatheAir()) {
            return super.increaseAirSupply(currentAir);
        }
        return this.decreaseAirSupply(currentAir);
    }

    /**
     * Trimmed (Phase 5g) from the official tickEnvironmentalState(): water damage and suffocation out of water for
     * creatures that can't breathe air. TODO(port): daylight burning (tickDaylightBurn).
     **/
    void tickEnvironmentalState(boolean isClient) {
        if (isClient) {
            return;
        }

        if (this.waterDamage() && this.isInWaterOrRain() && !this.isInLava()) {
            this.hurt(this.level().damageSources().drown(), 1.0F);
        }

        if (this.isAlive() && !this.canBreatheAir()) {
            this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
            if (this.getAirSupply() <= -200) {
                this.setAirSupply(-160);
                this.hurt(this.level().damageSources().drown(), 1.0F);
            }
        }
    }

    public boolean lavaContact() {
        return this.isInLava();
    }

    /**
     * Returns true if this mob is in contact with water in any way (in it, raining on it, etc).
     * Restored 2026-09 - a generic hook several creatures ported from official source need
     * (pathing/speed decisions), not specific to any one creature. Simplified vs. the original:
     * drops the leaf/plant-canopy "is this rain actually reaching me" nuance in favor of a
     * plain canSeeSkyFromBelowWater check, same trim style as testLightLevel().
     */
    public boolean waterContact() {
        if (this.isInWaterRainOrBubble()) {
            return true;
        }
        BlockPos pos = this.blockPosition();
        return this.getCommandSenderWorld().isRaining() && this.getCommandSenderWorld().canSeeSkyFromBelowWater(pos);
    }

    /**
     * The local speed modifier of this mob, AI classes multiply their own modifier by this one.
     * Restored 2026-09 alongside waterContact() - same generic-hook reasoning. Wired into
     * setSpeed() below so creature overrides of this actually affect movement, not just compile.
     */
    public float getAISpeedModifier() {
        if (!this.canWalk() && !this.isInWater() && !this.isFlying()) {
            return 0.1F;
        }
        return 1.0F;
    }

    @Override
    public void setSpeed(float speed) {
        super.setSpeed(speed * this.getAISpeedModifier());
    }

    public BlockPos getFacingPosition(double distance) {
        return this.getFacingPosition(this, distance, 0D);
    }

    public BlockPos getFacingPosition(Entity entity, double distance, double angleOffset) {
        return this.getFacingPosition(entity.position().x(), entity.position().y(), entity.position().z(), distance, entity.yRotO + angleOffset);
    }

    public BlockPos getFacingPosition(double x, double y, double z, double distance, double angle) {
        double angleRadians = Math.toRadians(angle);
        return new BlockPos((int) Math.floor(x + (distance * this.getFacingXAmount(angleRadians))), (int) Math.floor(y), (int) Math.floor(z + (distance * this.getFacingZAmount(angleRadians))));
    }

    public Vector3d getFacingPositionDouble(double x, double y, double z, double distance, double angle) {
        if (distance == 0) {
            distance = 1;
        }
        double angleRadians = Math.toRadians(angle);
        return new Vector3d(x + (distance * this.getFacingXAmount(angleRadians)), y, z + (distance * this.getFacingZAmount(angleRadians)));
    }

    private double getFacingXAmount(double angleRadians) {
        return -Math.sin(angleRadians);
    }

    private double getFacingZAmount(double angleRadians) {
        return Math.cos(angleRadians);
    }

    // NOTE: trimmed - original also adjusted for water-surface-Y/ground-Y here (getWaterSurfaceY/
    // getGroundY, dropped earlier as unused dead weight); fine for the day/night aggression
    // check concapede uses this for, not accurate enough for underwater/cave spawn-light checks.
    public byte testLightLevel() {
        return this.testLightLevel(this.blockPosition());
    }

    public byte testLightLevel(BlockPos pos) {
        if (pos.getY() < this.getCommandSenderWorld().getMinBuildHeight()) {
            return 0;
        }
        int rawLight = this.getCommandSenderWorld().getMaxLocalRawBrightness(pos);
        if (rawLight == 0) return 0;
        if (rawLight <= 8) return 1;
        if (rawLight < 15) return 2;
        return 3;
    }

    public boolean isDaytime() {
        if (!this.getCommandSenderWorld().isClientSide) {
            return this.getCommandSenderWorld().isDay();
        }
        long time = this.getCommandSenderWorld().getDayTime();
        if (time < 12500) {
            return true;
        }
        if (time >= 12542 && time < 23460) {
            return false;
        }
        return true;
    }

    public boolean isFlying() {
        return this.extraMobBehaviour != null && this.extraMobBehaviour.flightOverride();
    }

    /**
     * When called, this entity will strafe sideways with the given distance and height.
     * This is very sensitive, a large distance or height can cause the entity to zoom off for thousands of blocks!
     * A distance of 1.0D is around 10 blocks sideways, a height of 0.5D is about 10 blocks up.
     * Tip: Use a negative height for flying and swimming mobs so that they can swoop down in the air or water.
     * Ported verbatim from the official source (needed by EntityBanshee).
     **/
    public void strafe(double distance, double leapHeight) {
        boolean opposite = false;
        if (distance < 0) {
            distance = -distance;
            opposite = true;
        }
        float yaw = this.yRotO + (opposite ? -90F : 90F);
        float pitch = this.xRotO;
        double angle = Math.toRadians(yaw);
        double xAmount = -Math.sin(angle);
        double yAmount = leapHeight;
        double zAmount = Math.cos(angle);
        if (this.isFlying()) {
            yAmount = Math.sin(Math.toRadians(pitch)) * distance + this.getDeltaMovement().y() * 0.2D;
        }
        this.push(
                xAmount * distance + this.getDeltaMovement().x() * 0.2D,
                yAmount,
                zAmount * distance + this.getDeltaMovement().z() * 0.2D
        );
    }

    public int getFlyingHeight() {
        if (!this.isFlying()) {
            return 20;
        }
        return 0;
    }

    public double getFlightOffset() {
        return 0D;
    }

    public double getFallingMod() {
        return 1.0D;
    }

    public void setBlocking() {
        this.currentBlockingTime = this.blockingTime;
    }

    public boolean isBlocking() {
        if (this.getCommandSenderWorld().isClientSide) {
            return (this.getByteFromDataManager(ANIMATION_STATE) & ANIMATION_STATE_BITS.BLOCKING.id) > 0;
        }
        return this.currentBlockingTime > 0;
    }

    public boolean canAttackWhileBlocking() {
        return false;
    }

    public int getBlockingMultiplier() {
        return 4;
    }

    void tickBlockingState() {
        if (this.currentBlockingTime > 0) {
            this.currentBlockingTime--;
        }
        if (this.currentBlockingTime < 0) {
            this.currentBlockingTime = 0;
        }
    }

    public boolean hasPickupEntity() {
        return this.pickupEntity != null;
    }

    public boolean canAttackWithPickup() {
        return false;
    }

    private void tickTargetRuntime() {
        if (this.hasFixateTarget()) {
            this.setTarget(this.getFixateTarget());
        }
        if (this.hasAttackTarget() && this.getTarget() instanceof Player targetPlayer && targetPlayer.getAbilities().invulnerable) {
            this.setTarget(null);
        }
        if (this.hasAvoidTarget() && this.currentFleeTime-- <= 0) {
            this.setAvoidTarget(null);
        }
    }

    /**
     * Called once, the first time this creature spawns naturally/via egg/etc (not on chunk
     * reload - see the `firstSpawn` field). Override to run one-time setup (e.g. spawning
     * connected entities); callers must chain to super() to clear the flag.
     */
    public void onFirstSpawn() {
        this.firstSpawn = false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.creatureInfo.isDummy()) {
            return;
        }
        if (!this.getCommandSenderWorld().isClientSide && this.firstSpawn) {
            this.onFirstSpawn();
        }
        this.tickBlockingState();
        this.tickTargetRuntime();
        this.tickMovementRuntime(this.getCommandSenderWorld().isClientSide);
        this.tickEnvironmentalState(this.getCommandSenderWorld().isClientSide);
        this.updateTick++;
    }

    /**
     * Trimmed (Phase 5g) from the official tickMovementRuntime(): fire clearing, land-lock for non-walkers and the
     * climbing flag. TODO(port): fly sounds (playFlySound) and the flyer attack leap (leap()).
     **/
    void tickMovementRuntime(boolean isClient) {
        if (this.isOnFire() && !this.canBurn()) {
            this.clearFire();
        }

        if ((!this.canWalk() && !this.isFlying() && !this.isInWater() && this.isMoving()) || !this.canMove()) {
            this.clearMovement();
        }

        if (!isClient || this.isControlledByLocalInstance()) {
            this.setBesideClimbableBlock(this.horizontalCollision);
        }
    }

    /**
     * Used when loading this mob from a saved chunk.
     * Trimmed: relationships/inventory/saved-drops/extra-behaviour/constraint/fixate/minion
     * persistence not ported - only progression (level/experience/subspecies/variant/size).
     */
    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (this.creatureInfo.isDummy()) {
            return;
        }

        this.firstSpawn = !nbt.contains("FirstSpawn") || nbt.getBoolean("FirstSpawn");
        this.relationships.load(nbt);
        if (nbt.contains("Size")) {
            this.setSizeScale(nbt.getDouble("Size"));
        }
        if (nbt.contains("Subspecies")) {
            this.setSubspecies(nbt.getByte("Subspecies"));
        }
        if (nbt.contains("Variant")) {
            if (this.firstSpawn) {
                this.applyVariant(nbt.getByte("Variant"));
            } else {
                this.setVariant(nbt.getByte("Variant"));
            }
        }
        if (nbt.contains("MobLevel")) {
            if (this.firstSpawn) {
                this.applyLevel(nbt.getInt("MobLevel"));
            } else {
                this.setLevel(nbt.getInt("MobLevel"));
            }
        }
        if (nbt.contains("Experience")) {
            this.setExperience(nbt.getInt("Experience"));
        }
        if (nbt.contains("Color")) {
            this.setColor(DyeColor.byId(nbt.getByte("Color")));
        }
        if (nbt.contains("SpawnedAsBoss")) {
            this.setSpawnedAsBoss(nbt.getBoolean("SpawnedAsBoss"));
        }
        if (nbt.contains("SpawnedRare")) {
            this.setSpawnedRare(nbt.getBoolean("SpawnedRare"));
        }
        if (nbt.contains("HomeX") && nbt.contains("HomeY") && nbt.contains("HomeZ") && nbt.contains("HomeDistanceMax")) {
            this.restrictTo(new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ")), (int) nbt.getFloat("HomeDistanceMax"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.creatureInfo.isDummy()) {
            return;
        }

        nbt.putBoolean("FirstSpawn", this.firstSpawn);
        this.relationships.save(nbt);
        nbt.putByte("Subspecies", (byte) this.getSubspeciesIndex());
        nbt.putByte("Variant", (byte) this.getVariantIndex());
        nbt.putDouble("Size", this.sizeScale);
        nbt.putInt("MobLevel", this.getMobLevel());
        nbt.putInt("Experience", this.getExperience());
        nbt.putByte("Color", (byte) this.getColor().getId());
        nbt.putBoolean("SpawnedAsBoss", this.wasSpawnedAsBoss());
        nbt.putBoolean("SpawnedRare", this.wasSpawnedRare());
        if (this.hasHome()) {
            BlockPos homePos = this.getRestrictCenter();
            nbt.putInt("HomeX", homePos.getX());
            nbt.putInt("HomeY", homePos.getY());
            nbt.putInt("HomeZ", homePos.getZ());
            nbt.putFloat("HomeDistanceMax", this.getHomeDistanceMax());
        }
    }

    public int getAttackCooldown() {
        return this.attackCooldown;
    }

    public boolean isAttackOnCooldown() {
        return this.getAttackCooldown() > 0;
    }

    public void triggerAttackCooldown() {
        this.attackCooldown = this.getAttackCooldownMax();
    }

    public int getAttackCooldownMax() {
        if (!this.getCommandSenderWorld().isClientSide) {
            return this.attackCooldownMax;
        }
        return this.getIntFromDataManager(ANIMATION_ATTACK_COOLDOWN_MAX);
    }

    public void setAttackCooldownMax(int cooldownMax) {
        this.attackCooldownMax = cooldownMax;
        if (!this.getCommandSenderWorld().isClientSide) {
            this.getEntityData().set(ANIMATION_ATTACK_COOLDOWN_MAX, this.attackCooldownMax);
        }
    }

    public DyeColor getColor() {
        int colorId = this.getByteFromDataManager(COLOR) & 15;
        return DyeColor.byId(colorId);
    }

    public void setColor(DyeColor color) {
        if (!this.getCommandSenderWorld().isClientSide) {
            this.getEntityData().set(COLOR, (byte) (color.getId() & 15));
        }
    }

    public float getStealth() {
        return this.getFloatFromDataManager(STEALTH);
    }

    public void setStealth(float setStealth) {
        setStealth = Math.min(setStealth, 1);
        setStealth = Math.max(setStealth, 0);
        if (!this.getCommandSenderWorld().isClientSide) {
            this.getEntityData().set(STEALTH, setStealth);
        }
    }

    public boolean isStealthed() {
        return this.getStealth() >= 1.0F;
    }

    @Override
    protected float getSoundVolume() {
        if (this.isBoss()) {
            return 4.0F;
        }
        if (this.isRareVariant()) {
            return 2.0F;
        }
        return 1.0F;
    }

    public String getSoundName() {
        String soundSuffix = "";
        if (this.getSubspecies() != null && this.getSubspecies().getName() != null) {
            soundSuffix += "." + this.getSubspecies().getName();
        }
        return this.creatureInfo.getName() + soundSuffix;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ObjectManager.getSound(this.getSoundName() + "_say");
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return ObjectManager.getSound(this.getSoundName() + "_hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ObjectManager.getSound(this.getSoundName() + "_death");
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState block) {
        if (this.isFlying()) {
            return;
        }
        if (!this.hasStepSound) {
            super.playStepSound(pos, block);
            return;
        }
        this.playSound(ObjectManager.getSound(this.getSoundName() + "_step"), this.getSoundVolume(), this.randomizedSoundPitch());
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.HOSTILE_SWIM;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.HOSTILE_SPLASH;
    }

    public void playJumpSound() {
        if (!this.hasJumpSound) {
            return;
        }
        this.playSound(ObjectManager.getSound(this.getSoundName() + "_jump"), this.getSoundVolume(), this.randomizedSoundPitch());
    }

    public void playFlySound() {
        if (!this.isFlying() || this.hasPerchTarget()) {
            return;
        }
        this.playSound(ObjectManager.getSound(this.getSoundName() + "_fly"), this.getSoundVolume(), this.randomizedSoundPitch());
    }

    public void playAttackSound() {
        if (!this.hasAttackSound) {
            return;
        }
        this.playSound(ObjectManager.getSound(this.getSoundName() + "_attack"), this.getSoundVolume(), this.randomizedSoundPitch());
    }

    public float getMeleeAttackAnim(float pt) {
        int max = Math.max(1, this.getAttackCooldownMax());
        float cur = this.attackCooldown;
        float t = (max - (cur - pt)) / (float) max;
        return Mth.clamp(t, 0F, 1F);
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (sound == null) {
            LMHelperClass.logErrorMessageOnce("Null Sound trying to be played by: " + this.getType());
            return;
        }
        super.playSound(sound, volume, pitch);
    }

    @Override
    public float getFlyingSpeed() {
        return this.flyingSpeed;
    }

    private float randomizedSoundPitch() {
        return 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F);
    }

    public enum TARGET_BITS {
        ATTACK((byte) 1), MASTER((byte) 2), PARENT((byte) 4), AVOID((byte) 8), RIDER((byte) 16), PICKUP((byte) 32), PERCH((byte) 64);
        public final byte id;

        TARGET_BITS(byte value) {
            this.id = value;
        }
    }

    public enum ANIMATION_STATE_BITS {
        ATTACKED((byte) 1), GROUNDED((byte) 2), IN_WATER((byte) 4), BLOCKING((byte) 8), MINION((byte) 16), EXTRA01((byte) 32), BOSS((byte) 64);
        public final byte id;

        ANIMATION_STATE_BITS(byte value) {
            this.id = value;
        }
    }
}
