package com.lycanitesmobs.core.entity.base;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import com.lycanitesmobs.core.entity.IFusable;
import com.lycanitesmobs.core.entity.goals.targeting.*;
import com.lycanitesmobs.core.entity.goals.actions.*;
import net.minecraft.world.entity.Mob;
import com.lycanitesmobs.core.capabilities.entity.ExtendedEntity;
import com.lycanitesmobs.core.item.equipment.ItemEquipmentPart;
import com.lycanitesmobs.core.network.message.MessageCreature;
import com.lycanitesmobs.core.container.provider.CreatureContainerProvider;
import com.lycanitesmobs.core.container.creature.CreatureContainer;
import net.minecraft.world.entity.Pose;
import com.lycanitesmobs.core.container.creature.CreatureInventory;
import com.lycanitesmobs.core.data.info.creature.CreatureKnowledge;
import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.entity.pets.PetEntry;
import java.util.Collection;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.nbt.ListTag;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.entity.vehicle.Boat;
import com.lycanitesmobs.core.entity.IGroupHeavy;
import com.lycanitesmobs.core.entity.IGroupBoss;
import com.lycanitesmobs.core.entity.item.CustomItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.CommonHooks;
import net.minecraft.world.item.ShieldItem;
import com.lycanitesmobs.core.data.info.element.ElementInfo;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import com.lycanitesmobs.core.manager.ProjectileManager;
import com.lycanitesmobs.core.data.info.projectile.ProjectileInfo;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.Difficulty;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.AABB;
import java.util.Collections;
import com.lycanitesmobs.core.capabilities.level.ExtendedWorld;
import com.lycanitesmobs.core.data.info.creature.CreatureGroup;
import com.lycanitesmobs.core.entity.spawner.SpawnerTriggerDispatcher;
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
    protected static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<ItemStack> EQUIPMENT_HEAD = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> EQUIPMENT_CHEST = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> EQUIPMENT_LEGS = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> EQUIPMENT_FEET = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> EQUIPMENT_BAG = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> EQUIPMENT_SADDLE = SynchedEntityData.defineId(BaseCreatureEntity.class, EntityDataSerializers.ITEM_STACK);

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
    protected int fleeTime = 200;
    protected int currentFleeTime = 0;
    protected float renderTick = 0;
    protected boolean isAggressiveByDefault = true;
    protected boolean spreadFire = false;
    // Minions / bosses (ported 2026-09-26 for the boss batch):
    private final List<LivingEntity> minions = new ArrayList<>();
    protected boolean isMinion = false;
    /** The pet entry (soulbound pet, mount, summoned minion, familiar) this creature belongs to, if any. **/
    protected PetEntry petEntry;
    /** The Summoning Pedestal that summoned this creature, null if not summoned via a pedestal. **/
    protected com.lycanitesmobs.core.block.blockentity.TileEntitySummoningPedestal summoningPedestal;
    /** Set when a creature saved as a bound pet is loaded without its pet entry - it's discarded, as the entry respawns its own. **/
    protected boolean boundPetOrphan = false;
    /** If true, this mob is temporary and will despawn once temporaryDuration reaches 0. **/
    protected boolean isTemporary = false;
    protected int temporaryDuration = 0;
    /** Centre of a boss arena, if this creature has one. */
    protected BlockPos arenaCenter = null;
    /** Maximum damage taken from a single hit (0 = no cap). */
    protected int damageMax = 0;
    /** Maximum damage taken per second (0 = no cap). */
    protected float damageLimit = 0;
    private float healthLastTick = -1;
    protected boolean extraAnimation01 = false;
    /** Current boss battle phase (0-based), advanced by bosses in updateBattlePhase(). */
    protected int battlePhase = 0;
    private ServerBossEvent bossInfo;
    private boolean forceBossHealthBar = false;

    /** If true, this creature's spawn check ignores block collision (e.g. Cinder spawning in fire). */
    protected boolean spawnsInBlock = false;
    /** Health percentage below which this creature flees (0 = never). Read by flee/avoid AI. */
    protected float fleeHealthPercent = 0;
    /** If true, other entities collide with this creature as if it were solid. */
    protected boolean solidCollision = false;
    /** Damage taken during the current second, used by some creatures' abilities. */
    public float damageTakenThisSec = 0;
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
    /** Set by spawners (forceNoDespawn) and setPersistenceRequired(), see isPersistant(). **/
    protected boolean forceNoDespawn = false;
    /** Can this mob spawn where it can't see the sky above? **/
    protected boolean spawnsUnderground = true;
    /** Can this mob spawn on land (not in liquids)? **/
    protected boolean spawnsOnLand = true;
    /** Does this mob spawn inside liquids? **/
    protected boolean spawnsInWater = false;
    private DirectNavigator directNavigator;

    protected boolean hasAttackSound = false;
    protected boolean hasStepSound = true;
    protected boolean hasJumpSound = false;
    protected int flySoundSpeed = 0;
    protected float onlyRenderTicks = -1;

    private final List<ItemDrop> drops = new ArrayList<>();
    private final List<ItemDrop> savedDrops = new ArrayList<>();
    /** True once this creature has dropped its death loot, prevents double drops. **/
    protected boolean hasDropped = false;
    /** If true, this creature drops no loot or experience until it is damaged by a player. **/
    protected boolean dropsRequirePlayerDamage = false;
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

    /** Equipment (armor/saddle/bag) and bag item storage. */
    protected CreatureInventory inventory;

    protected BaseCreatureEntity(EntityType<? extends BaseCreatureEntity> entityType, Level world) {
        super(entityType, world);
        this.relationships = new CreatureRelationships(this);
        this.inventory = new CreatureInventory(this.creatureInfo.getName(), this);

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
        builder.define(SIZE, 1.0F);
        CreatureInventory.registerData(builder);

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

    public void applySpawnLifecycleState(boolean firstSpawn) {
        this.firstSpawn = firstSpawn;
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

    public boolean hasSpawnEventType(String eventType) {
        return this.spawnEventType.equalsIgnoreCase(eventType);
    }

    public void applySpawnEvent(String spawnEventType, int spawnEventCount) {
        this.spawnEventType = spawnEventType != null ? spawnEventType : "";
        this.spawnEventCount = spawnEventCount;
    }

    public void inheritSpawnEventFrom(BaseCreatureEntity source) {
        this.spawnEventType = source.spawnEventType;
        this.spawnEventCount = source.spawnEventCount;
    }

    public void clearSpawnEventTracking() {
        this.spawnEventType = "";
        this.spawnEventCount = -1;
    }

    /**
     * Returns true if this mob should not despawn in unloaded chunks (official: set by spawners with forceNoDespawn
     * and by setPersistenceRequired()). Subclasses add tamed/farmed/master checks.
     **/
    public boolean isPersistant() {
        return this.forceNoDespawn;
    }

    @Override
    public void setPersistenceRequired() {
        super.setPersistenceRequired();
        this.forceNoDespawn = true;
    }

    @Override
    public boolean isPersistenceRequired() {
        if (!this.canDespawnNaturally()) {
            return true;
        }
        return super.isPersistenceRequired();
    }

    /**
     * Returns whether this mob should despawn overtime or not. Config defined forced despawns override everything except tamed creatures and tagged creatures.
     **/
    protected boolean canDespawnNaturally() {
        if (this.creatureInfo.getCreatureSpawn().forcesDespawn()) {
            return true;
        }
        if (!this.creatureInfo.getCreatureSpawn().despawnsNaturally()) {
            return false;
        }
        if (this.creatureInfo.isBoss() || (this.isRareVariant() && !Variant.isRareDespawning())) {
            return false;
        }
        return !this.isPersistant() && !this.isLeashed() && !(this.hasCustomName() && "".equals(this.spawnEventType));
    }

    public void applySpawnerSpawnState(boolean forceNoDespawn, boolean spawnedRare) {
        // The official only sets the field, which isn't saved; also set vanilla's saved flag so it survives a reload.
        if (forceNoDespawn) {
            this.setPersistenceRequired();
        }
        this.spawnedRare = spawnedRare;
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

    public boolean hasDirectNavigationTarget() {
        return this.directNavigator.hasTargetPosition();
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

    /** Returns the current (unclaimed) combat goal index without claiming it, for goals that share a slot. */
    protected int currentCombatGoalIndex() {
        return this.nextCombatGoalIndex;
    }

    /** Returns the current (unclaimed) idle goal index without claiming it, for goals that share a slot. */
    protected int currentIdleGoalIndex() {
        return this.nextIdleGoalIndex;
    }

    public int claimReactTargetGoalIndex() {
        return this.nextReactTargetIndex++;
    }

    public int claimSpecialTargetGoalIndex() {
        return this.nextSpecialTargetIndex++;
    }

    /**
     * Registers all AI Goals for this entity (restored to the official set 2026-09-28).
     */
    @Override
    protected void registerGoals() {
        if (this instanceof IFusable) {
            this.targetSelector.addGoal(this.claimSpecialTargetGoalIndex(), new FindFuseTargetGoal(this));
        }
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new AvoidIfHitGoal(this).setHelpCall(true));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new RevengeGoal(this).setHelpCall(true).setCheckSight(true));

        this.goalSelector.addGoal(this.claimPriorityGoalIndex(), new PaddleGoal(this));
        this.goalSelector.addGoal(this.claimPriorityGoalIndex(), new StayByWaterGoal(this));
        this.goalSelector.addGoal(this.claimPriorityGoalIndex(), new AvoidGoal(this).setNearSpeed(1.3D).setFarSpeed(1.2D).setNearDistance(5.0D).setFarDistance(20.0D));
        this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new TemptGoal(this).setTemptDistanceMin(4.0D));
        if (this instanceof IFusable) {
            this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new FollowFuseGoal(this).setLostDistance(16));
        }

        super.registerGoals();

        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindGroupAttackTargetGoal(this));
        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindGroupAvoidTargetGoal(this).setTameTargetting(false));

        this.goalSelector.addGoal(this.claimTravelGoalIndex(), new FollowMasterGoal(this).setStrayDistance(12.0D));
        this.goalSelector.addGoal(this.claimIdleGoalIndex(), new WanderGoal(this));
        this.goalSelector.addGoal(this.claimIdleGoalIndex(), new WatchClosestGoal(this).setTargetClass(Player.class));
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
        // Equipment parts drop as materials (placeholders until the equipment system, see ItemEquipmentPart).
        if (ItemEquipmentPart.hasMobPartDrops(this.creatureInfo.getEntityId())) {
            for (ItemEquipmentPart itemEquipmentPart : ItemEquipmentPart.getMobPartDrops(this.creatureInfo.getEntityId())) {
                ItemDrop partDrop = new ItemDrop(LycanitesMobs.MODID + ":" + itemEquipmentPart.itemName, itemEquipmentPart.getDropChance()).setMaxAmount(1);
                partDrop.setBonusAmount(false);
                partDrop.setAmountMultiplier(false);
                this.drops.add(partDrop);
            }
        }
        this.setAttackCooldownMax(this.attackCooldownMax);
    }

    public void loadItemDrops() {
        this.drops.addAll(this.creatureInfo.getDrops());
    }

    public void addSavedItemDrop(ItemDrop itemDrop) {
        this.drops.add(itemDrop);
        this.savedDrops.add(itemDrop);
    }

    /**
     * Cycles through all of this entity's drops and drops random loot on death. Minions, bound pets and creatures
     * that still require player damage drop nothing.
     *
     * <p>1.21.1: the official override of dropAllDeathLoot() did its own drop capturing + ForgeHooks.onLivingDrops;
     * vanilla's dropAllDeathLoot() now does both (and the XP drop) and calls this hook inside the capture, so only
     * the drop rolling is kept here. Looting is a data-driven enchantment now, read via the registry holder.
     **/
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (this.isMinion() || this.isBoundPet() || this.dropsRequirePlayerDamage || this.hasDropped) {
            return;
        }
        this.hasDropped = true;

        int lootingLevel = 0;
        if (damageSource.getEntity() instanceof LivingEntity killer) {
            lootingLevel = level.registryAccess().registry(Registries.ENCHANTMENT)
                    .flatMap(registry -> registry.getHolder(Enchantments.LOOTING))
                    .map(looting -> EnchantmentHelper.getEnchantmentLevel(looting, killer))
                    .orElse(0);
        }

        int variantScale = 1;
        if (this.isRareVariant()) {
            variantScale = Variant.getRareDropScale();
        } else if (this.getVariant() != null && "uncommon".equals(this.getVariant().getRarity())) {
            variantScale = Variant.getUncommonDropScale();
        }

        for (ItemDrop itemDrop : this.drops) {
            if (!this.canDropItem(itemDrop)) {
                continue;
            }
            int multiplier = 1;
            if (itemDrop.getVariantIndex() < 0) {
                multiplier *= variantScale;
            }
            if (this.extraMobBehaviour != null && this.extraMobBehaviour.itemDropMultiplierOverride() != 1) {
                multiplier = Math.round((float) multiplier * (float) this.extraMobBehaviour.itemDropMultiplierOverride());
            }
            int quantity = itemDrop.getQuantity(this.getRandom(), lootingLevel, multiplier);
            if (quantity <= 0) {
                continue;
            }
            this.dropItem(itemDrop.getEntityDropItemStack(this, quantity));
        }
    }

    /**
     * 1.21.1: getExperienceReward() is final now, the variant-scaled experience goes through this hook instead.
     **/
    @Override
    protected int getBaseExperienceReward() {
        if (this.isMinion() || this.isBoundPet() || this.dropsRequirePlayerDamage) {
            return 0;
        }
        return this.computeExperienceReward();
    }

    public boolean canDropItem(ItemDrop itemDrop) {
        if (itemDrop.getSubspeciesIndex() >= 0 && itemDrop.getSubspeciesIndex() != this.getSubspeciesIndex()) {
            return false;
        }
        if (itemDrop.getVariantIndex() >= 0 && itemDrop.getVariantIndex() != this.getVariantIndex()) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if this mob has a pet entry and is thus bound to another entity.
     **/
    public boolean isBoundPet() {
        return this.hasPetEntry();
    }

    /**
     * Gets the entity that owns this creature, overridden by tameable creatures.
     **/
    public LivingEntity getOwner() {
        return null;
    }

    public boolean hasPetEntry() {
        return this.getPetEntry() != null;
    }

    public PetEntry getPetEntry() {
        return this.petEntry;
    }

    /**
     * Sets the pet entry for this mob. Mobs with Pet Entries are removed when the world is reloaded, as the Pet Entry
     * spawns a new instance of them on load.
     **/
    public void setPetEntry(PetEntry petEntry) {
        this.petEntry = petEntry;
    }

    /**
     * Make this mob temporary where it will despawn once the specified duration (in ticks) reaches 0.
     **/
    public void setTemporary(int duration) {
        this.temporaryDuration = duration;
        this.isTemporary = true;
    }

    public void unsetTemporary() {
        this.isTemporary = false;
        this.temporaryDuration = 0;
    }

    public boolean isTemporary() {
        return this.isTemporary;
    }

    public int getTemporaryDuration() {
        return this.temporaryDuration;
    }

    boolean shouldDropInventoryOnDespawn() {
        return !this.isBoundPet() || this.isTemporary;
    }

    /**
     * Removes this mob when despawnCheck() says so (temporary minions, peaceful difficulty, disabled creatures, stale
     * mob event spawns). Official: in tick(); here in aiStep with the rest of the per-tick runtime.
     **/
    private boolean tickTemporaryDespawn() {
        if (!this.despawnCheck()) {
            return false;
        }
        if (this.shouldDropInventoryOnDespawn()) {
            this.inventory.dropInventory();
        }
        this.remove(Entity.RemovalReason.DISCARDED);
        return true;
    }

    private boolean discardIfOrphanedBoundPet() {
        if (this.boundPetOrphan && !this.hasPetEntry()) {
            this.discard();
            return true;
        }
        return false;
    }

    private boolean handleFirstSpawnPetEntry() {
        if (!this.hasPetEntry()) {
            return false;
        }
        PetEntry petEntry = this.getPetEntry();
        if (petEntry.getSummonSet() != null && petEntry.getSummonSet().getPlayerExt() != null) {
            petEntry.getSummonSet().getPlayerExt().sendPetEntryToPlayer(petEntry);
        }
        return true;
    }

    // ==================================================
    //                 Beastiary Knowledge
    // ==================================================
    /**
     * Scales Beastiary knowledge experience gained from this creature (bosses and variants give more).
     **/
    public int scaleKnowledgeExperience(int knowledgeExperience) {
        if (this.isBoss()) {
            knowledgeExperience = Math.round((float) CreatureManager.getInstance().getConfig().creatureBossKnowledgeScale() * knowledgeExperience);
        } else if (this.getVariant() != null) {
            knowledgeExperience = Math.round((float) CreatureManager.getInstance().getConfig().creatureVariantKnowledgeScale() * knowledgeExperience);
        }
        return knowledgeExperience;
    }

    /**
     * Players within 10 blocks discover this creature (rank 1 knowledge) if they don't know it yet.
     **/
    private void tickBeastiaryProximityDiscovery(Level world, boolean isClient) {
        if (isClient || this.updateTick % 20 != 0) {
            return;
        }
        for (Player player : world.players()) {
            if (this.distanceToSqr(player) > 10.0 * 10.0) {
                continue;
            }
            ExtendedPlayer extendedPlayer = ExtendedPlayer.getForPlayer(player);
            if (extendedPlayer == null) {
                continue;
            }
            CreatureKnowledge creatureKnowledge = extendedPlayer.getBeastiary().getCreatureKnowledge(this.creatureInfo.getName());
            if (creatureKnowledge == null || creatureKnowledge.getRank() < 1) {
                extendedPlayer.studyCreature(this, CreatureManager.getInstance().getConfig().creatureProximityKnowledge(), false, false);
            }
        }
    }

    private void studyCreatureKillForPlayer(Player player) {
        if (this.isTamed()) {
            return;
        }
        ExtendedPlayer extendedPlayer = ExtendedPlayer.getForPlayer(player);
        if (extendedPlayer != null) {
            extendedPlayer.studyCreature(this, CreatureManager.getInstance().getConfig().creatureKillKnowledge(), false, false);
        }
    }

    /**
     * Sets if this creature should no longer drop items until it takes damage from a source belonging to a player.
     **/
    public void setDropsRequirePlayerDamage(boolean requiresPlayerDamage) {
        this.dropsRequirePlayerDamage = requiresPlayerDamage;
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
    /** Set when a creature subsystem (e.g. relationships) needs an advanced sync on the next update. **/
    protected boolean syncQueued = true;

    /**
     * Queues an advanced network sync (per-player reputation) of this entity for the next update tick. Ignored client side.
     */
    public void queueSync() {
        if (this.getCommandSenderWorld().isClientSide()) {
            return;
        }
        this.syncQueued = true;
    }

    /**
     * Sends each player with a relationship to this creature their reputation (for the taming bar).
     */
    public void doSync() {
        this.syncQueued = false;
        if (this.getCommandSenderWorld().isClientSide()) {
            return;
        }
        for (Player player : this.relationships.getPlayers()) {
            CreatureRelationshipEntry relationshipEntry = this.relationships.getEntry(player);
            if (relationshipEntry != null && player instanceof ServerPlayer serverPlayer) {
                LycanitesMobs.PACKET_MANAGER.sendToPlayer(new MessageCreature(this, relationshipEntry.getReputation()), serverPlayer);
            }
        }
    }

    /**
     * Syncs targets, attack phase, animation state and subspecies/variant/size between server and client. Ported from the
     * official onSyncUpdate() - the port had defined these data slots but never written them, so clients never saw a
     * creature's variant (base textures only), size, targets or attack animation state.
     * TODO(port): the ARENA (boss arena centre) slot isn't synced.
     **/
    public void onSyncUpdate() {
        if (this.syncQueued) {
            this.doSync();
        }

        if (!this.getCommandSenderWorld().isClientSide) {
            this.getEntityData().set(TARGET, this.getTargetMask());
            this.getEntityData().set(ATTACK_PHASE, this.attackPhase);
            this.getEntityData().set(ANIMATION_STATE, this.getServerAnimationMask());
        } else {
            byte animationState = this.getByteFromDataManager(ANIMATION_STATE);
            this.applyClientAnimationState(animationState);
            this.isMinion = (animationState & ANIMATION_STATE_BITS.MINION.id) > 0;
        }

        this.syncProgressionState();
    }

    private void syncProgressionState() {
        if (!this.getCommandSenderWorld().isClientSide) {
            this.getEntityData().set(SUBSPECIES, (byte) this.getSubspeciesIndex());
            this.getEntityData().set(VARIANT, (byte) this.getVariantIndex());
            this.getEntityData().set(SIZE, (float) this.sizeScale);
            return;
        }
        if (this.getSubspeciesIndex() != this.getByteFromDataManager(SUBSPECIES)) {
            this.setSubspecies(this.getByteFromDataManager(SUBSPECIES));
        }
        if (this.getVariantIndex() != this.getByteFromDataManager(VARIANT)) {
            this.applyVariant(this.getByteFromDataManager(VARIANT));
        }
        if (this.sizeScale != this.getFloatFromDataManager(SIZE)) {
            this.setSizeScale(this.getFloatFromDataManager(SIZE));
        }
    }

    private byte getTargetMask() {
        byte targets = 0;
        if (this.getTarget() != null) {
            targets += TARGET_BITS.ATTACK.id;
        }
        if (this.getMasterTarget() != null) {
            targets += TARGET_BITS.MASTER.id;
        }
        if (this.getParentTarget() != null) {
            targets += TARGET_BITS.PARENT.id;
        }
        if (this.getAvoidTarget() != null) {
            targets += TARGET_BITS.AVOID.id;
        }
        if (this.getControllingPassenger() != null) {
            targets += TARGET_BITS.RIDER.id;
        }
        if (this.getPickupEntity() != null) {
            targets += TARGET_BITS.PICKUP.id;
        }
        if (this.getPerchTarget() != null) {
            targets += TARGET_BITS.PERCH.id;
        }
        return targets;
    }

    private byte getServerAnimationMask() {
        byte animations = 0;
        if (this.isAttackOnCooldown()) {
            animations += ANIMATION_STATE_BITS.ATTACKED.id;
        }
        if (this.onGround()) {
            animations += ANIMATION_STATE_BITS.GROUNDED.id;
        }
        if (this.wasTouchingWater) {
            animations += ANIMATION_STATE_BITS.IN_WATER.id;
        }
        if (this.isBlocking()) {
            animations += ANIMATION_STATE_BITS.BLOCKING.id;
        }
        if (this.isMinion()) {
            animations += ANIMATION_STATE_BITS.MINION.id;
        }
        if (this.extraAnimation01()) {
            animations += ANIMATION_STATE_BITS.EXTRA01.id;
        }
        if (this.wasSpawnedAsBoss()) {
            animations += ANIMATION_STATE_BITS.BOSS.id;
        }
        return animations;
    }

    private void applyClientAnimationState(byte animationState) {
        if ((animationState & ANIMATION_STATE_BITS.ATTACKED.id) > 0) {
            if (!this.isAttackOnCooldown()) {
                this.triggerAttackCooldown();
            }
        } else {
            this.resetAttackCooldown();
        }
        this.setOnGround((animationState & ANIMATION_STATE_BITS.GROUNDED.id) > 0);
        this.wasTouchingWater = (animationState & ANIMATION_STATE_BITS.IN_WATER.id) > 0;
        this.extraAnimation01 = (animationState & ANIMATION_STATE_BITS.EXTRA01.id) > 0;
        this.setSpawnedAsBoss((animationState & ANIMATION_STATE_BITS.BOSS.id) > 0);
    }

    private boolean hasTargetBit(TARGET_BITS targetBit) {
        return (this.getByteFromDataManager(TARGET) & targetBit.id) > 0;
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

    // ==================================================
    //                     Spawning
    // ==================================================
    /**
     * Checks if the creature is able to spawn at it's initial position.
     **/
    @Override
    public boolean checkSpawnRules(LevelAccessor world, MobSpawnType spawnReason) {
        return this.checkSpawnVanilla(this.getCommandSenderWorld(), spawnReason, this.blockPosition());
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return this.creatureInfo.getCreatureSpawn().getSpawnGroupMax();
    }

    /**
     * Performs checks when spawned by a vanilla spawner or possibly another modded spawner if they use the vanilla checks.
     **/
    public boolean checkSpawnVanilla(Level world, MobSpawnType spawnReason, BlockPos pos) {
        if (world.isClientSide) {
            return false;
        }
        if (spawnReason != MobSpawnType.NATURAL && spawnReason != MobSpawnType.SPAWNER) {
            return true;
        }

        LMHelperClass.logDebug("MobSpawns", "Vanilla Spawn Check: " + this.creatureInfo.getName() + " at " + pos);
        if (!this.creatureInfo.isEnabled() || !this.creatureInfo.getCreatureSpawn().isEnabled()) {
            return false;
        }
        if (!this.creatureInfo.isPeaceful() && world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (!world.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return false;
        }
        if (!this.fixedSpawnCheck(world, pos)) {
            return false;
        }
        if (spawnReason == MobSpawnType.SPAWNER) {
            return true;
        }
        if (!CreatureManager.getInstance().getSpawnConfig().isAllowedGlobal(world)) {
            return false;
        }
        return this.environmentSpawnCheck(world, pos);
    }

    /**
     * First stage checks for vanilla spawning, if this check fails the creature will not spawn.
     **/
    public boolean fixedSpawnCheck(Level world, BlockPos pos) {
        if (pos.getY() < this.creatureInfo.getCreatureSpawn().getSpawnMinY()) {
            return false;
        }
        if (!this.checkSpawnLightLevel(world, pos)) {
            return false;
        }
        if (!this.checkSpawnCollision(world, pos)) {
            return false;
        }
        return this.checkSpawnGroupLimit(world, pos, CreatureManager.getInstance().getSpawnConfig().spawnLimitRange());
    }

    /**
     * Second stage checks for vanilla spawning, this check is ignored if there is a valid monster spawner nearby.
     **/
    public boolean environmentSpawnCheck(Level world, BlockPos pos) {
        if (this.creatureInfo.getCreatureSpawn().getWorldDayMin() > 0) {
            int currentDay = (int) Math.floor(world.getGameTime() / 24000D);
            if (currentDay < this.creatureInfo.getCreatureSpawn().getWorldDayMin()) {
                return false;
            }
        }
        if (!this.isNativeDimension(world)) {
            return false;
        }
        if (!this.spawnsInWater && world.containsAnyLiquid(this.getBoundingBox())) {
            return false;
        } else if (!this.spawnsOnLand && !world.containsAnyLiquid(this.getBoundingBox())) {
            return false;
        }
        if (!this.spawnsUnderground && this.isBlockUnderground(pos.getX(), pos.getY() + 1, pos.getZ())) {
            return false;
        }
        return this.checkSpawnBoss(world, pos);
    }

    public boolean isNativeDimension(Level world) {
        return this.creatureInfo.getCreatureSpawn().isAllowedDimension(world);
    }

    /**
     * Returns true if there is no collision stopping this mob from spawning.
     **/
    public boolean checkSpawnCollision(Level world, BlockPos pos) {
        double radius = this.creatureInfo.getWidth();
        double height = this.creatureInfo.getHeight();
        AABB spawnBoundries = new AABB(pos.getX() - radius, pos.getY(), pos.getZ() - radius, pos.getX() + radius, pos.getY() + height, pos.getZ() + radius);
        return this.spawnsInBlock || world.noCollision(spawnBoundries);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader world) {
        if (this.spawnsInWater) {
            return world.isUnobstructed(this);
        }
        return super.checkSpawnObstruction(world);
    }

    /**
     * Returns true if the light level is valid for spawning.
     **/
    public boolean checkSpawnLightLevel(Level world, BlockPos pos) {
        if (this.creatureInfo.getCreatureSpawn().spawnsInDark() && this.creatureInfo.getCreatureSpawn().spawnsInLight()) {
            return true;
        }
        if (!this.creatureInfo.getCreatureSpawn().spawnsInDark() && !this.creatureInfo.getCreatureSpawn().spawnsInLight()) {
            return false;
        }

        byte light = this.testLightLevel(pos);
        if (this.creatureInfo.getCreatureSpawn().spawnsInDark() && light <= 1) {
            return true;
        }
        return this.creatureInfo.getCreatureSpawn().spawnsInLight() && light >= 2;
    }

    /**
     * Checks for nearby entities of this type, mobs use this so that too many don't spawn in the same area. Returns true if the mob should spawn.
     **/
    public boolean checkSpawnGroupLimit(Level world, BlockPos pos, double range) {
        if (range <= 0) {
            return true;
        }
        return this.countNearbySpawnLimits(range).withinGroupLimits();
    }

    /**
     * Checks for nearby bosses, mobs usually shouldn't randomly spawn near a boss.
     **/
    public boolean checkSpawnBoss(Level world, BlockPos pos) {
        CreatureGroup bossGroup = CreatureManager.getInstance().getCreatureGroup("boss");
        if (bossGroup == null) {
            return true;
        }
        List<?> bosses = this.getNearbyEntities(BaseCreatureEntity.class, bossGroup::hasEntity, CreatureManager.getInstance().getSpawnConfig().spawnLimitRange());
        return bosses.isEmpty();
    }

    /**
     * Combined spawn limit check - boss proximity AND group limit checks with a single entity scan.
     *
     * @param world           The world to check in.
     * @param pos             The position to check around.
     * @param groupLimitRange The range for group limit checks.
     * @return True if the mob is allowed to spawn (no boss nearby, within group limits).
     */
    public boolean checkSpawnLimits(Level world, BlockPos pos, double groupLimitRange) {
        CreatureGroup bossGroup = CreatureManager.getInstance().getCreatureGroup("boss");
        double bossRange = CreatureManager.getInstance().getSpawnConfig().spawnLimitRange();
        double range = Math.max(groupLimitRange, bossRange);
        if (range <= 0 && bossGroup == null) {
            return true;
        }

        NearbySpawnLimitCounts counts = this.countNearbySpawnLimits(range);
        if (bossGroup != null && bossRange > 0 && counts.hasBossWithin(bossRange)) {
            return false;
        }
        if (groupLimitRange <= 0) {
            return true;
        }
        return counts.withinGroupLimits(groupLimitRange);
    }

    private NearbySpawnLimitCounts countNearbySpawnLimits(double range) {
        int typesLimit = CreatureManager.getInstance().getSpawnConfig().typeSpawnLimit();
        int speciesLimit = this.creatureInfo.getCreatureSpawn().getSpawnAreaLimit();
        CreatureGroup bossGroup = CreatureManager.getInstance().getCreatureGroup("boss");
        if (typesLimit <= 0 && speciesLimit <= 0 && bossGroup == null) {
            return NearbySpawnLimitCounts.empty(this, typesLimit, speciesLimit);
        }

        List<BaseCreatureEntity> nearby = this.getNearbyEntities(BaseCreatureEntity.class, entity -> entity instanceof BaseCreatureEntity, range);
        return new NearbySpawnLimitCounts(this, nearby, bossGroup, typesLimit, speciesLimit);
    }

    private record NearbySpawnLimitCounts(
            BaseCreatureEntity owner,
            List<BaseCreatureEntity> nearby,
            CreatureGroup bossGroup,
            int typesLimit,
            int speciesLimit
    ) {
        private static NearbySpawnLimitCounts empty(BaseCreatureEntity owner, int typesLimit, int speciesLimit) {
            return new NearbySpawnLimitCounts(owner, Collections.emptyList(), null, typesLimit, speciesLimit);
        }

        private boolean hasBossWithin(double bossRange) {
            for (BaseCreatureEntity target : this.nearby) {
                if (target.distanceTo(this.owner) <= bossRange && this.bossGroup.hasEntity(target)) {
                    return true;
                }
            }
            return false;
        }

        private boolean withinGroupLimits() {
            return this.withinGroupLimits(Double.MAX_VALUE);
        }

        private boolean withinGroupLimits(double range) {
            if (this.typesLimit <= 0 && this.speciesLimit <= 0) {
                return true;
            }

            int typesFound = 0;
            int speciesFound = 0;
            for (BaseCreatureEntity target : this.nearby) {
                if (target.distanceTo(this.owner) > range) {
                    continue;
                }
                if (target.creatureInfo.isPeaceful() == this.owner.creatureInfo.isPeaceful()) {
                    typesFound++;
                }
                if (this.owner.creatureInfo.matchesEntityClass(target.getClass())) {
                    speciesFound++;
                }
            }
            if (this.typesLimit > 0 && typesFound >= this.typesLimit) {
                return false;
            }
            return this.speciesLimit <= 0 || speciesFound < this.speciesLimit;
        }
    }

    /**
     * Checks if the specified block is underground (unable to see the sky above it). This checks through leaves, plants, grass and vine materials.
     **/
    public boolean isBlockUnderground(int x, int y, int z) {
        if (this.getCommandSenderWorld().canSeeSkyFromBelowWater(new BlockPos(x, y, z))) {
            return false;
        }
        for (int j = y; j < this.getCommandSenderWorld().getMaxBuildHeight(); j++) {
            BlockState blockState = this.getCommandSenderWorld().getBlockState(new BlockPos(x, j, z));
            boolean isLeaves = LMHelperClass.hasTag(blockState, BlockTags.LEAVES);
            boolean replaceablePlant = LMHelperClass.hasTag(blockState, BlockTags.REPLACEABLE);
            // Official quirk kept: only plant blocks count as cover here.
            if (blockState.getBlock() != Blocks.AIR
                    && !isLeaves
                    && LMHelperClass.Materials.isPlant(blockState.getBlock())
                    && !replaceablePlant) {
                return true;
            }
        }
        return false;
    }

    /**
     * A check that is constantly done, if this returns true, this entity will be removed, used normally for peaceful difficulty removal and temporary minions.
     **/
    public boolean despawnCheck() {
        if (this.getCommandSenderWorld().isClientSide) {
            return false;
        }

        if (!this.creatureInfo.isEnabled()) {
            return true;
        }

        if (this.isTemporary && this.temporaryDuration-- <= 0) {
            return true;
        }

        if (!this.creatureInfo.isPeaceful() && this.getCommandSenderWorld().getDifficulty() == Difficulty.PEACEFUL && !this.hasCustomName()) {
            return true;
        }

        ExtendedWorld worldExt = ExtendedWorld.getForWorld(this.getCommandSenderWorld());
        if (worldExt != null && !"".equals(this.spawnEventType) && this.spawnEventCount >= 0 && this.spawnEventCount != worldExt.getWorldEventCount()) {
            if (this.isLeashed() || this.isPersistant()) {
                this.spawnEventType = "";
                this.spawnEventCount = -1;
                return false;
            }
            return true;
        }
        return false;
    }

    public int getBossNearbyRange() {
        return this.creatureInfo.getBossNearbyRange();
    }

    private void tickBossArena(Level world) {
        if (!world.isClientSide && this.isBossAlways()) {
            ExtendedWorld extendedWorld = ExtendedWorld.getForWorld(world);
            if (extendedWorld != null) {
                extendedWorld.bossUpdate(this);
            }
        }
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

    /** S202: the level system is scrapped, so every creature stays at level 1 (saved levels are ignored too). **/
    public void setLevel(int level) {
        level = 1;
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
        // S202: no level ups, the level system is scrapped.
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
     * Returns a sub texture of this creature (not subspecies/variant specific), e.g. calpod_effect.
     **/
    public ResourceLocation getSubTexture(String subName) {
        return AssetHelper.entityTexture(this.getTextureName() + "_" + subName.toLowerCase());
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

    @Override
    protected void customServerAiStep() {
        if (this.useDirectNavigator()) {
            this.directNavigator.updateFlight();
        }
        super.customServerAiStep();
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
        damageAmount *= this.getDamageModifier(damageSrc);
        if (super.hurt(damageSrc, damageAmount)) {
            if (this.dropsRequirePlayerDamage && damageSrc.getEntity() instanceof Player) {
                this.dropsRequirePlayerDamage = false;
            }
            this.onDamage(damageSrc, damageAmount);
            if (this.isBoss() && damageSrc.getEntity() instanceof Player player) {
                this.addPlayerTarget(player);
            }
            this.updateAttackerReputation(damageSrc);
            return true;
        }
        return false;
    }

    /**
     * Applies this creature's Defense stat (boosted while blocking), the boss/rare-variant 75% reduction against
     * non-player damage and the damage cap. Official: resolveIncomingDamage() inside an actuallyHurt() override, after
     * armor and magic absorption. NeoForge 1.21 routes reductions through a damage container and ignores the magic
     * step's return value, so this hooks the armor step (whose return value is used) - one step earlier than the
     * official, which only differs at the 1-damage minimum.
     **/
    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource damageSrc, float damageAmount) {
        damageAmount = super.getDamageAfterArmorAbsorb(damageSrc, damageAmount);
        damageAmount = this.getDamageAfterDefense(damageAmount);
        if ((this.isBoss() || this.isRareVariant()) && !(damageSrc.getEntity() instanceof Player)) {
            damageAmount *= 0.25F;
        }
        return damageAmount;
    }

    public float getDamageAfterDefense(float damage) {
        float defense = (float) this.creatureStats.getDefense();
        if (this.isBlocking()) {
            if (defense <= 0) {
                defense = 1;
            }
            defense *= this.getBlockingMultiplier();
        }
        damage = Math.max(damage - defense, 1);
        if (this.damageMax > 0) {
            damage = Math.min(damage, this.damageMax);
        }
        return Math.max(damage, 0F);
    }

    public boolean canTargetBlockDamageSource(LivingEntity target, DamageSource damageSource) {
        Entity entity = damageSource.getDirectEntity();
        boolean arrowPierce = entity instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow && arrow.getPierceLevel() > 0;
        if (!damageSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) && target.isBlocking() && !arrowPierce) {
            Vec3 sourcePosition = damageSource.getSourcePosition();
            if (sourcePosition != null) {
                Vec3 viewVector = target.getViewVector(1.0F);
                Vec3 toTarget = sourcePosition.vectorTo(target.position()).normalize();
                toTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);
                return toTarget.dot(viewVector) < 0.0D;
            }
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
        this.scheduleGUIRefresh();
    }

    // ==================================================
    //                   Creature GUI
    // ==================================================
    private final java.util.List<Player> guiViewers = new java.util.ArrayList<>();
    /** Counts from guiRefreshTime down to 0 when a GUI refresh has been scheduled. **/
    private int guiRefreshTick = 0;
    private final int guiRefreshTime = 2;

    /**
     * Adds the player as a GUI viewer and opens the creature GUI (inventory + pet commands) for them.
     **/
    public void openGUI(Player player) {
        if (this.getCommandSenderWorld().isClientSide) {
            return;
        }
        this.addGUIViewer(player);
        this.refreshGUIViewers();
        this.openGUIToPlayer(player);
    }

    public void addGUIViewer(Player player) {
        if (!this.getCommandSenderWorld().isClientSide && !this.guiViewers.contains(player)) {
            this.guiViewers.add(player);
        }
    }

    public void removeGUIViewer(Player player) {
        if (!this.getCommandSenderWorld().isClientSide) {
            this.guiViewers.remove(player);
        }
    }

    /**
     * Re-opens the GUI for every player still viewing it (after a GUI command or inventory change). Use scheduleGUIRefresh().
     **/
    public void refreshGUIViewers() {
        if (this.getCommandSenderWorld().isClientSide || this.guiViewers.isEmpty()) {
            return;
        }
        for (Player player : this.guiViewers.toArray(new Player[0])) {
            if (player.containerMenu instanceof CreatureContainer container) {
                if (container.getCreature() == this) {
                    this.openGUIToPlayer(player);
                } else {
                    this.removeGUIViewer(player);
                }
            }
        }
    }

    /**
     * Opens the menu for the player. 1.21: Player.openMenu(provider, extraData) replaces Forge's NetworkHooks.openScreen.
     **/
    public void openGUIToPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new CreatureContainerProvider(this), buf -> buf.writeInt(this.getId()));
        }
    }

    /**
     * Schedules a GUI refresh, normally takes 2 ticks for everything to update for display.
     **/
    public void scheduleGUIRefresh() {
        this.guiRefreshTick = this.guiRefreshTime + 1;
    }

    private void tickGuiRefresh(boolean isClient) {
        if (isClient) {
            return;
        }
        if (this.guiViewers.isEmpty()) {
            this.guiRefreshTick = 0;
        }
        if (this.guiRefreshTick > 0 && --this.guiRefreshTick <= 0) {
            this.refreshGUIViewers();
            this.guiRefreshTick = 0;
        }
    }

    /**
     * Pet command IDs sent by the pet/creature GUIs via MessageEntityGUICommand (official numbering, shared by
     * client and server).
     **/
    public enum PET_COMMAND_ID {
        ACTIVE((byte) 0), TELEPORT((byte) 1), PVP((byte) 2), RELEASE((byte) 3), PASSIVE((byte) 4), DEFENSIVE((byte) 5), ASSIST((byte) 6), AGGRESSIVE((byte) 7), FOLLOW((byte) 8), WANDER((byte) 9), SIT((byte) 10), FLEE((byte) 11);
        public final byte id;

        PET_COMMAND_ID(byte i) {
            id = i;
        }
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
    /**
     * Restored (Phase 6a): was an empty stub, so melee hits applied no element debuffs at all.
     **/
    private void applyContactAttackEffects(Entity target, boolean includeFireSpread) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        if (livingTarget.isBlocking() && livingTarget.getUseItem().getItem() instanceof ShieldItem) {
            return;
        }
        if (includeFireSpread && this.spreadFire && this.isOnFire() && this.getRandom().nextFloat() < this.creatureStats.getEffect()) {
            target.igniteForSeconds(this.getEffectDuration(4) / 20F);
        }
        if (this.creatureStats.getAmplifier() >= 0) {
            this.applyDebuffs(livingTarget, 1, 1);
        }
    }

    // ==================================================
    //          Creature helpers (ported 2026-09-26)
    // ==================================================
    // Needed by the remaining creature batches; bodies from the official source unless noted.

    /**
     * Returns how much fall distance this creature ignores; 100+ means immune to fall damage.
     **/
    public float getFallResistance() {
        return 0;
    }

    /**
     * Called whenever this creature takes damage (after the damage is accepted).
     **/
    public void onDamage(DamageSource damageSrc, float damage) {
        this.damageTakenThisSec += damage;
    }

    /**
     * Returns a multiplier applied to incoming damage from the given source.
     **/
    public float getDamageModifier(DamageSource damageSrc) {
        return 1.0F;
    }

    public float getBrightness() {
        return LMHelperClass.getBrightness(this);
    }

    /**
     * Returns true if the provided entity may target this creature. Bosses can only be targeted by player-owned pets.
     **/
    public boolean canBeTargetedBy(LivingEntity entity) {
        if (this.isBoss() && entity instanceof BaseCreatureEntity entityCreature) {
            return entityCreature instanceof TameableCreatureEntity entityTameable && entityTameable.getPlayerOwner() != null;
        }
        return true;
    }

    /**
     * Returns true if the target entity is looking at this entity.
     **/
    public boolean isLookingAtMe(Entity targetEntity) {
        if (targetEntity == null) {
            return false;
        }
        Vec3 targetViewVector = targetEntity.getViewVector(1.0F).normalize();
        Vec3 distance = new Vec3(this.getX() - targetEntity.getX(), this.getEyeY() - targetEntity.getEyeY(), this.getZ() - targetEntity.getZ());
        double distanceStraight = distance.length();
        distance = distance.normalize();
        double lookDistance = targetViewVector.dot(distance);
        double lookRange = 1.5D;
        double comparison = 1.0D - (lookRange / distanceStraight);
        if (targetEntity instanceof Player player) {
            return lookDistance > comparison && player.hasLineOfSight(this);
        }
        return lookDistance > comparison;
    }

    /**
     * Returns true if this creature is a pet of the given PetEntry type ("pet", "mount", "minion", "familiar").
     **/
    public boolean isPetType(String type) {
        if (!this.hasPetEntry())
            return false;
        return type.equals(this.getPetEntry().getType());
    }

    public LivingEntity getPickupEntity() {
        return this.pickupEntity;
    }

    /**
     * Drops the entity this creature is carrying (and clears the carried entity's ExtendedEntity link).
     **/
    public void dropPickupEntity() {
        ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(this.getPickupEntity());
        if (extendedEntity != null) {
            extendedEntity.setPickedUpByEntity(null);
        }
        this.pickupEntity = null;
    }

    /**
     * Returns true if this creature can safely land from its current position.
     **/
    public boolean isSafeToLand() {
        if (this.onGround()) {
            return true;
        }
        if (this.getCommandSenderWorld().getBlockState(this.blockPosition().below()).isSolid()) {
            return true;
        }
        return this.getCommandSenderWorld().getBlockState(this.blockPosition().below(2)).isSolid();
    }

    public int getGroundY(BlockPos pos) {
        int y = pos.getY();
        if (y <= 0) {
            return 0;
        }
        BlockState startBlock = this.getCommandSenderWorld().getBlockState(pos);
        if (startBlock.isAir()) {
            for (int possibleGroundY = Math.max(0, y - 1); possibleGroundY >= 0; possibleGroundY--) {
                BlockState possibleGroundBlock = this.getCommandSenderWorld().getBlockState(new BlockPos(pos.getX(), possibleGroundY, pos.getZ()));
                if (possibleGroundBlock.isAir()) {
                    y = possibleGroundY;
                } else {
                    break;
                }
            }
        }
        return y;
    }

    public int getAirY(BlockPos pos) {
        int y = pos.getY();
        int yMax = this.getCommandSenderWorld().getMaxBuildHeight() - 1;
        if (y >= yMax) {
            return yMax;
        }
        if (this.getCommandSenderWorld().canSeeSkyFromBelowWater(pos)) {
            return yMax;
        }
        BlockState startBlock = this.getCommandSenderWorld().getBlockState(pos);
        if (startBlock.isAir()) {
            for (int possibleAirY = Math.min(yMax, y + 1); possibleAirY <= yMax; possibleAirY++) {
                BlockState possibleGroundBlock = this.getCommandSenderWorld().getBlockState(new BlockPos(pos.getX(), possibleAirY, pos.getZ()));
                if (possibleGroundBlock.isAir()) {
                    y = possibleAirY;
                } else {
                    break;
                }
            }
        }
        return y;
    }

    /**
     * Returns a random Y between minY and maxY blocks above the ground at the given position, capped by open air.
     **/
    public int restrictYHeightFromGround(BlockPos coords, int minY, int maxY) {
        int groundY = this.getGroundY(coords);
        int airYMax = Math.min(this.getAirY(coords), groundY + maxY);
        int airYMin = Math.min(airYMax, groundY + minY);
        if (airYMin >= airYMax) {
            return airYMin;
        }
        return airYMin + this.getRandom().nextInt(airYMax - airYMin);
    }

    @FunctionalInterface
    private interface DestroyAreaBlockConsumer {
        void accept(BlockPos pos, BlockState state);
    }

    private void forEachDestroyAreaBlock(int x, int y, int z, int range, boolean expandOnly, DestroyAreaBlockConsumer blockConsumer) {
        int width = (int) Math.ceil(this.getBbWidth());
        int minHorizontal = expandOnly ? -(width + range) : -(width - range);
        int maxHorizontal = width + range;
        int height = (int) Math.ceil(this.getBbHeight());
        for (int w = minHorizontal; w <= maxHorizontal; w++) {
            for (int d = minHorizontal; d <= maxHorizontal; d++) {
                for (int h = 0; h <= height; h++) {
                    BlockPos breakPos = new BlockPos(x + w, y + h, z + d);
                    if (this.getCommandSenderWorld().getBlockEntity(breakPos) != null) {
                        continue;
                    }
                    blockConsumer.accept(breakPos, this.getCommandSenderWorld().getBlockState(breakPos));
                }
            }
        }
    }

    public void destroyArea(int x, int y, int z, float strength, boolean drop) {
        this.destroyArea(x, y, z, strength, drop, 0);
    }

    public void destroyArea(int x, int y, int z, float strength, boolean drop, int range) {
        this.destroyArea(x, y, z, strength, drop, range, null, 0);
    }

    /**
     * Destroys blocks around the given position that are weaker than strength. Callers check the mobGriefing rule.
     * Broken blocks fire block spawn triggers (chain + 1 limits spawner loops).
     **/
    public void destroyArea(int x, int y, int z, float strength, boolean drop, int range, Player player, int chain) {
        int adjustedRange = Math.max(range - 1, 0);
        this.forEachDestroyAreaBlock(x, y, z, adjustedRange, false, (breakPos, blockState) -> {
            float hardness = blockState.getDestroySpeed(this.getCommandSenderWorld(), breakPos);
            Block material = blockState.getBlock();
            if (hardness < 0 || strength < hardness || strength < blockState.getBlock().getExplosionResistance() || material == Blocks.WATER || material == Blocks.LAVA) {
                return;
            }
            if (player != null && breakPos.getX() == x && breakPos.getY() == y && breakPos.getZ() == z) {
                return;
            }
            SpawnerTriggerDispatcher.getInstance().onBlockBreak(this.getCommandSenderWorld(), breakPos, blockState, player, chain);
            this.getCommandSenderWorld().destroyBlock(breakPos, drop);
        });
    }

    /** Destroys blocks of the given class around the position (official signature kept; WoodType never matches a block). **/
    public void destroyAreaBlock(int x, int y, int z, Class<?> blockClass, boolean drop, int range) {
        this.forEachDestroyAreaBlock(x, y, z, range, true, (breakPos, blockState) -> {
            if (blockClass.isInstance(blockState.getBlock())) {
                this.getCommandSenderWorld().destroyBlock(breakPos, drop);
            }
        });
    }

    /**
     * Called by EatBlockGoal after this creature eats a block (e.g. Yale regrowing wool).
     **/
    public void onEat() {
    }

    /**
     * Applies effects to an item this creature drops (e.g. fire immunity for items dropped by fire creatures).
     **/
    public void applyDropEffects(CustomItemEntity entityItem) {
    }

    public void dropItem(ItemStack itemStack) {
        this.spawnAtLocation(itemStack, 0.0F);
    }

    // ==================================================
    //                  Inventory / Equipment
    // ==================================================
    public CreatureInventory getCreatureInventory() {
        return this.inventory;
    }

    /**
     * Returns the current size of this mob's inventory. (Some mob inventories can vary in size such as mounts with and without bag items equipped.)
     **/
    public int getInventorySize() {
        return this.inventory.getContainerSize();
    }

    /**
     * Returns the maximum possible size of this mob's inventory. (The creature inventory is not actually resized, instead some slots are locked and made unavailable.)
     **/
    public int getInventorySizeMax() {
        return Math.max(this.getNoBagSize(), this.getBagSize());
    }

    public boolean hasBag() {
        return !this.inventory.getEquipmentStack("bag").isEmpty();
    }

    /**
     * Returns the size of this mob's inventory when it doesn't have a bag item equipped.
     **/
    public int getNoBagSize() {
        if (this.extraMobBehaviour != null && this.extraMobBehaviour.inventorySizeOverride() > 0) {
            return this.extraMobBehaviour.inventorySizeOverride();
        }
        return 0;
    }

    /**
     * Returns the size that this mob's inventory increases by when it is provided with a bag item.
     **/
    public int getBagSize() {
        if (this.creatureInfo != null) {
            return this.creatureInfo.getBagSize();
        }
        return 5;
    }

    /** Official: item pickup is opt-in per creature (extra mob behaviour / overrides like Kobold). **/
    public boolean canPickupItems() {
        return this.extraMobBehaviour != null && this.extraMobBehaviour.itemPickupOverride();
    }

    public void pickupItems() {
        List<ItemEntity> nearbyItems = this.getCommandSenderWorld().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.0D, 0.0D, 1.0D));
        for (ItemEntity entityItem : nearbyItems) {
            if (entityItem.isAlive() && !entityItem.getItem().isEmpty()) {
                ItemStack itemStack = entityItem.getItem();
                int space = this.getSpaceForStack(itemStack);
                if (space > 0) {
                    this.onPickupStack(itemStack);
                    this.doItemPickup(entityItem);
                }
            }
        }
    }

    /** Called when this mob picks up an item entity, provides the itemStack it has picked up. **/
    public void onPickupStack(ItemStack itemStack) {
    }

    public void doItemPickup(ItemEntity entityItem) {
        if (entityItem.isAlive() && !entityItem.getItem().isEmpty()) {
            ItemStack leftoverStack = this.inventory.autoInsertStack(entityItem.getItem());
            if (leftoverStack != null) {
                entityItem.setItem(leftoverStack);
            } else {
                entityItem.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    public int getSpaceForStack(ItemStack pickupStack) {
        return this.inventory.getSpaceForStack(pickupStack);
    }

    /**
     * Returns true if the player is allowed to equip this creature with items such as armor or saddles.
     **/
    public boolean canEquip() {
        return this.creatureInfo.isTameable();
    }

    /**
     * Returns the equipment grade name for a slot (e.g. "chestIron"), used for armor texturing.
     **/
    public String getEquipmentName(String type) {
        if (this.inventory.getEquipmentGrade(type) != null) {
            return type + this.inventory.getEquipmentGrade(type);
        }
        return null;
    }

    @Override
    public int getArmorValue() {
        return super.getArmorValue() + this.inventory.getArmorValue();
    }

    /**
     * Returns the texture for an equipment layer, e.g. "saddle" -> textures/entity/warg_saddle.png (subspecies aware).
     **/
    public ResourceLocation getEquipmentTexture(String equipmentName) {
        if (!this.canEquip()) {
            return this.getTexture();
        }
        if (this.getSubspecies() != null && this.getSubspecies().getName() != null) {
            equipmentName = this.getSubspecies().getName() + "_" + equipmentName;
        }
        return this.getSubTexture(equipmentName);
    }

    // ==================================================
    //                   Mount Offsets
    // ==================================================
    // 1.21.1 dropped Entity.getPassengersRidingOffset()/getMyRidingOffset() (replaced by entity attachment
    // points). These are kept as Lycanites' own methods, used by RideableCreatureEntity.positionRider(), so the
    // per-creature offsets tuned for the original still apply unchanged.

    /**
     * A Y Offset used to position the mob that is riding this mob.
     **/
    public double getPassengersRidingOffset() {
        return (double) this.getDimensions(Pose.STANDING).height() * this.getMountOffset().y();
    }

    /**
     * A Z Offset used to position the mob that is riding this mob.
     **/
    public double getMountedZOffset() {
        return (double) this.getDimensions(Pose.STANDING).width() * this.getMountOffset().z();
    }

    private Vector3d getMountOffset() {
        Subspecies subspecies = this.getSubspecies();
        if (subspecies != null && subspecies.getMountOffset() != null) {
            return subspecies.getMountOffset();
        }
        return this.creatureInfo.getMountOffset();
    }

    protected void moveWithDirectNavigator(double strafe, double forward) {
        this.directNavigator.flightMovement(strafe, forward);
    }

    /**
     * The vanilla item drop method, overridden to make use of the CustomItemEntity class (see applyDropEffects).
     **/
    @Override
    public ItemEntity spawnAtLocation(ItemStack itemStack, float heightOffset) {
        if (itemStack.isEmpty() || this.level().isClientSide) {
            return null;
        }
        CustomItemEntity entityItem = new CustomItemEntity(this.level(), this.getX(), this.getY() + (double) heightOffset, this.getZ(), itemStack);
        entityItem.setDefaultPickUpDelay();
        this.applyDropEffects(entityItem);

        Collection<ItemEntity> capturedDrops = this.captureDrops();
        if (capturedDrops != null) {
            capturedDrops.add(entityItem);
        } else {
            this.level().addFreshEntity(entityItem);
        }
        return entityItem;
    }

    public boolean canStealth() {
        return this.extraMobBehaviour != null && this.extraMobBehaviour.stealthOverride();
    }

    /**
     * Called by StealthGoal when this creature starts stealthing.
     **/
    public void startStealth() {
    }

    /**
     * Returns true if this creature can be lured with its treat (TemptGoal).
     **/
    public boolean canBeTempted() {
        if (this.isRareVariant() || this.spawnedAsBoss) {
            return false;
        }
        if (this.creatureInfo.isFarmable()) {
            return true;
        }
        if (this.isInPack() && !CreatureManager.getInstance().getConfig().packTreatLuring()) {
            return false;
        }
        return this.creatureInfo.isTameable();
    }

    /**
     * Returns true if this creature should flee the target based on its creature groups.
     **/
    public boolean shouldCreatureGroupHunt(LivingEntity target) {
        boolean shouldFlee = false;
        boolean shouldHunt = false;
        boolean shouldPackHunt = false;
        for (CreatureGroup group : this.creatureInfo.getGroups()) {
            if (group.shouldFlee(target)) {
                shouldFlee = true;
            }
            if (group.shouldHunt(target)) {
                shouldHunt = true;
            }
            if (group.shouldPackHunt(target)) {
                shouldPackHunt = true;
            }
        }
        boolean canPackHunt = shouldPackHunt && this.isInPack();
        if (shouldFlee && !canPackHunt) {
            return false;
        }
        return shouldHunt || shouldPackHunt;
    }

    /** The attack target of this creature's parent (used by CopyParentAttackTargetGoal). **/
    public LivingEntity getParentAttackTarget() {
        if (this.getParentTarget() instanceof Mob mobTarget) {
            return mobTarget.getTarget();
        }
        return null;
    }

    public int getDirectNavigationTargetY() {
        return this.directNavigator.getTargetPositionY();
    }

    public void setDirectNavigationSpeedModifier(double speedModifier) {
        this.directNavigator.setSpeedModifier(speedModifier);
    }

    /** Called by PlaceBlockGoal after this creature places a block (e.g. Vespids building their hive). **/
    public void onBlockPlaced(BlockPos blockPos, BlockState blockState) {
    }

    /**
     * Called when this creature kills an entity. The official defined this (a 1.12-era hook) but never called it, so
     * the Geist/Ghoul/Cryptkeeper on-kill behaviours were dead; it is wired to vanilla's killedEntity() here.
     **/
    protected void onKillEntity(LivingEntity entityLivingBase) {
    }

    /**
     * Turns a villager this creature killed into a zombie villager (Geist/Ghoul/Cryptkeeper). 1.21: mirrors vanilla
     * Zombie.killedEntity() - the official 1.20 code built the zombie villager by hand with the old finalizeSpawn/
     * trade offer tag APIs.
     **/
    protected void convertVillagerToZombie(net.minecraft.world.entity.npc.Villager villager) {
        if (!(this.level() instanceof ServerLevel level) || !net.neoforged.neoforge.event.EventHooks.canLivingConvert(villager, EntityType.ZOMBIE_VILLAGER, timer -> {})) {
            return;
        }
        net.minecraft.world.entity.monster.ZombieVillager zombieVillager = villager.convertTo(EntityType.ZOMBIE_VILLAGER, false);
        if (zombieVillager == null) {
            return;
        }
        zombieVillager.finalizeSpawn(level, level.getCurrentDifficultyAt(zombieVillager.blockPosition()), MobSpawnType.CONVERSION, new net.minecraft.world.entity.monster.Zombie.ZombieGroupData(false, true));
        zombieVillager.setVillagerData(villager.getVillagerData());
        zombieVillager.setGossips(villager.getGossips().store(net.minecraft.nbt.NbtOps.INSTANCE));
        zombieVillager.setTradeOffers(villager.getOffers().copy());
        zombieVillager.setVillagerXp(villager.getVillagerXp());
        net.neoforged.neoforge.event.EventHooks.onLivingConvert(villager, zombieVillager);
        if (!this.isSilent()) {
            level.levelEvent(null, 1026, this.blockPosition(), 0);
        }
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity killed) {
        boolean result = super.killedEntity(level, killed);
        this.onKillEntity(killed);
        return result;
    }

    /** Sets the home position and the distance this creature may stray from it (vanilla restriction). **/
    public void setHome(int x, int y, int z, float distance) {
        this.restrictTo(new BlockPos(x, y, z), (int) distance);
    }

    /** Sets the home position (vanilla restriction centre), keeping the current home distance. **/
    public void setHomePosition(int x, int y, int z) {
        this.restrictTo(new BlockPos(x, y, z), (int) Math.max(this.getRestrictRadius(), 1));
    }

    @Override
    public boolean shouldShowName() {
        if (this.getVariant() != null && !this.hasCustomName()) {
            return this.renderVariantNameTag();
        }
        return super.shouldShowName();
    }

    /** Gets whether this mob should always display its nametag if it's a variant. **/
    public boolean renderVariantNameTag() {
        return CreatureManager.getInstance().getConfig().subspeciesTags();
    }

    public boolean shouldCreatureGroupFlee(LivingEntity target) {
        if (this.isBoss() || this.isRareVariant() || this.isTamed()) {
            return false;
        }
        boolean shouldFlee = false;
        boolean shouldPackHunt = false;
        for (CreatureGroup group : this.creatureInfo.getGroups()) {
            if (group.shouldFlee(target)) {
                shouldFlee = true;
            }
            if (group.shouldPackHunt(target)) {
                shouldPackHunt = true;
            }
        }
        return shouldFlee && !(shouldPackHunt && this.isInPack());
    }

    public boolean hasRiderTarget() {
        return this.getControllingPassenger() != null;
    }

    public void clearPlayerTargets() {
        this.playerTargets.clear();
    }

    /**
     * Returns the nearest entity of the given class within range, optionally only ones this creature can attack.
     **/
    @Nullable
    public <T extends Entity> T getNearestEntity(Class<? extends T> clazz, com.google.common.base.Predicate<Entity> predicate, double range, boolean canAttack) {
        List<T> aoeTargets = this.getNearbyEntities(clazz, predicate, range);
        double nearestDistance = range + 10;
        T nearestEntity = null;
        for (T targetEntity : aoeTargets) {
            if (targetEntity == this || !(targetEntity instanceof LivingEntity livingEntity)) {
                continue;
            }
            if (canAttack && !this.canAttack(livingEntity)) {
                continue;
            }
            if (targetEntity == this.getControllingPassenger()) {
                continue;
            }
            double distance = this.distanceTo(targetEntity);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestEntity = targetEntity;
            }
        }
        return nearestEntity;
    }

    /**
     * Offset (x, y, z) at which a picked-up entity is held.
     **/
    public double[] getPickupOffset(Entity entity) {
        return new double[]{0, 0, 0};
    }

    /**
     * Picks up the given entity. The target's ExtendedEntity pickedUpByEntity is what carries it along.
     **/
    public void pickupEntity(LivingEntity entity) {
        ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(entity);
        if (extendedEntity != null) {
            extendedEntity.setPickedUpByEntity(this);
        }
        this.pickupEntity = entity;
        this.clearMovement();
    }

    /**
     * Returns true if this creature may pick up the entity.
     **/
    public boolean canPickupEntity(LivingEntity entity) {
        if (this.getPickupEntity() == entity || entity instanceof IGroupBoss || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof Player player && player.isCreative()) {
            return false;
        }
        if (entity instanceof BaseCreatureEntity targetCreature && targetCreature.hasPickupEntity()) {
            return false;
        }
        CreatureGroup bossGroup = CreatureManager.getInstance().getCreatureGroup("boss");
        if (bossGroup != null && bossGroup.hasEntity(entity)) {
            return false;
        }
        boolean heavyTarget = entity instanceof IGroupHeavy || entity.getBbHeight() >= 4 || entity.getBbWidth() >= 4;
        if (heavyTarget && !(this instanceof IGroupHeavy)) {
            return false;
        }
        if ((entity.getVehicle() != null && !(entity.getVehicle() instanceof Boat) && !(entity.getVehicle() instanceof Minecart)) || entity.getControllingPassenger() != null) {
            return false;
        }
        Holder<MobEffect> weight = ObjectManager.getEffectHolder("weight");
        if (weight != null && entity.hasEffect(weight)) {
            return false;
        }
        Holder<MobEffect> repulsion = ObjectManager.getEffectHolder("repulsion");
        if (repulsion != null && entity.hasEffect(repulsion)) {
            return false;
        }
        ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(entity);
        return extendedEntity != null && !extendedEntity.isPickedUp();
    }

    /**
     * Transforms this creature into another entity type (elemental fusion, or a solo transformation). Restored to the
     * official version 2026-09-28 (temporary/minion/master state, fusion level maths, partner ownership).
     **/
    @Nullable
    public LivingEntity transform(EntityType<? extends LivingEntity> transformType, Entity partner, boolean destroyPartner) {
        if (transformType == null) {
            return null;
        }
        LivingEntity transformedEntity = transformType.create(this.getCommandSenderWorld());
        if (transformedEntity == null) {
            return null;
        }

        if (transformedEntity instanceof BaseCreatureEntity transformedCreature) {
            this.copyBaseTransformState(transformedCreature);
            if (partner instanceof BaseCreatureEntity partnerCreature) {
                this.copyFusionTransformState(transformedCreature, partnerCreature);
            } else {
                this.copySoloTransformState(transformedCreature);
            }
        }

        transformedEntity.moveTo(this.getX(), this.getY(), this.getZ(), this.yRotO, this.xRotO);
        DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, transformedEntity, () -> {
            this.remove(Entity.RemovalReason.DISCARDED);
            if (partner != null && destroyPartner) {
                partner.remove(Entity.RemovalReason.DISCARDED);
            }
        });
        return transformedEntity;
    }

    private void copyBaseTransformState(BaseCreatureEntity transformedCreature) {
        transformedCreature.firstSpawn = false;
        if (this.isTemporary) {
            transformedCreature.setTemporary(this.temporaryDuration);
        }
        if (this.isMinion()) {
            transformedCreature.setMinion(true);
        }
        if (this.hasMaster()) {
            transformedCreature.setMasterTarget(this.getMasterTarget());
        }
    }

    private void copyFusionTransformState(BaseCreatureEntity transformedCreature, BaseCreatureEntity partnerCreature) {
        Variant fusionVariant = transformedCreature.getSubspecies() != null
                ? transformedCreature.getSubspecies().getChildVariant(this, this.getVariant(), partnerCreature.getVariant()) : null;
        transformedCreature.setSubspecies(this.getSubspeciesIndex());
        transformedCreature.applyVariant(fusionVariant != null ? fusionVariant.getIndex() : 0);
        transformedCreature.setSizeScale(this.sizeScale + partnerCreature.sizeScale);
        partnerCreature.registerTransformedPedestalMinion(transformedCreature);
        this.registerTransformedPedestalMinion(transformedCreature);

        int transformedLevel = this.getFusionTransformLevel(partnerCreature);
        transformedCreature.applyLevel(Math.round(transformedLevel * (float) CreatureManager.getInstance().getConfig().elementalFusionLevelMultiplier()));
        this.copyFusionTransformTamingState(transformedCreature, partnerCreature, transformedLevel);
    }

    /** Hands a fused creature over to the summoning pedestal that owned its parts (if any). **/
    public void registerTransformedPedestalMinion(BaseCreatureEntity transformedCreature) {
        if (this.summoningPedestal != null) {
            this.summoningPedestal.registerMinion(transformedCreature);
        }
    }

    public void bindSummoningPedestal(com.lycanitesmobs.core.block.blockentity.TileEntitySummoningPedestal summoningPedestal) {
        this.summoningPedestal = summoningPedestal;
    }

    public boolean hasSummoningPedestal() {
        return this.summoningPedestal != null;
    }

    public int getSummonCost() {
        return this.creatureInfo.getSummonCost();
    }

    private int getFusionTransformLevel(BaseCreatureEntity partnerCreature) {
        int transformedLevel = this.getMobLevel();
        String fusionLevelMix = CreatureManager.getInstance().getConfig().elementalFusionLevelMix();
        if ("lowest".equalsIgnoreCase(fusionLevelMix)) {
            return Math.min(transformedLevel, partnerCreature.getMobLevel());
        }
        if ("highest".equalsIgnoreCase(fusionLevelMix)) {
            return Math.max(transformedLevel, partnerCreature.getMobLevel());
        }
        return transformedLevel + partnerCreature.getMobLevel();
    }

    private void copyFusionTransformTamingState(BaseCreatureEntity transformedCreature, BaseCreatureEntity partnerCreature, int transformedLevel) {
        if (!(transformedCreature instanceof TameableCreatureEntity fusionTameable)) {
            return;
        }

        if (this instanceof TameableCreatureEntity tameableSource) {
            Player owner = tameableSource.getPlayerOwner();
            if (owner != null) {
                transformedCreature.applyLevel(transformedLevel);
                fusionTameable.setPlayerOwner(owner);
                tameableSource.copyPetBehaviourTo(fusionTameable);
            }
            return;
        }

        if (partnerCreature instanceof TameableCreatureEntity tameablePartner) {
            Player partnerOwner = tameablePartner.getPlayerOwner();
            if (partnerOwner != null) {
                transformedCreature.applyLevel(transformedLevel);
                fusionTameable.setPlayerOwner(partnerOwner);
                tameablePartner.copyPetBehaviourTo(fusionTameable);
                if (partnerCreature.isTemporary) {
                    transformedCreature.setTemporary(partnerCreature.temporaryDuration);
                }
                transformedCreature.setMinion(partnerCreature.isMinion());
                if (partnerCreature.hasMaster()) {
                    transformedCreature.setMasterTarget(partnerCreature.getMasterTarget());
                }
            }
        }
    }

    private void copySoloTransformState(BaseCreatureEntity transformedCreature) {
        transformedCreature.setSubspecies(this.getSubspeciesIndex());
        transformedCreature.applyVariant(this.getVariantIndex());
        transformedCreature.setSizeScale(this.sizeScale);
        transformedCreature.applyLevel(this.getMobLevel());

        if (transformedCreature instanceof TameableCreatureEntity fusionTameable && this.getOwner() instanceof Player owner) {
            fusionTameable.setPlayerOwner(owner);
            if (this instanceof TameableCreatureEntity tameableSource) {
                tameableSource.copyPetBehaviourTo(fusionTameable);
            }
        }
    }

    // ==================================================
    //                 Minions and Bosses
    // ==================================================

    /**
     * Spawns the provided minion around this creature at the given angle and distance and registers it.
     **/
    public void summonMinion(LivingEntity minion, double angle, double distance) {
        double angleRadians = Math.toRadians(angle);
        double x = this.getX() + ((this.getBbWidth() + distance) * Math.cos(angleRadians) - Math.sin(angleRadians));
        double y = this.getY() + 1;
        if (minion instanceof BaseCreatureEntity creatureMinion && creatureMinion.isFlying()) {
            y += this.getBbHeight() / 2;
        }
        double z = this.getZ() + ((this.getBbWidth() + distance) * Math.sin(angleRadians) + Math.cos(angleRadians));
        minion.moveTo(x, y, z, this.getRandom().nextFloat() * 360.0F, 0.0F);
        DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, minion, () -> {
            if (minion instanceof BaseCreatureEntity creatureMinion) {
                creatureMinion.setMinion(true);
                if (this.isTemporary) {
                    creatureMinion.setTemporary(this.temporaryDuration);
                }
                if (!this.isRareVariant()) {
                    creatureMinion.applyVariant(this.getVariantIndex());
                }
                creatureMinion.setSubspecies(this.getSubspeciesIndex());
                creatureMinion.setMasterTarget(this);
                creatureMinion.spawnEventType = this.spawnEventType;
                creatureMinion.onFirstSpawn();
            }
            if (this.getTarget() != null) {
                minion.setLastHurtByMob(this.getTarget());
            }
            this.addMinion(minion);
        });
    }

    public void setMinion(boolean minion) {
        this.isMinion = minion;
    }

    /**
     * Returns true if this mob has a Master Target (the synced TARGET bits on the client).
     **/
    public boolean hasMaster() {
        if (!this.getCommandSenderWorld().isClientSide) {
            return this.getMasterTarget() != null;
        }
        return this.hasTargetBit(TARGET_BITS.MASTER);
    }

    public boolean isMinion() {
        return this.isMinion;
    }

    public boolean hasMinion(LivingEntity minion) {
        return this.minions.contains(minion);
    }

    public List<LivingEntity> getMinions(EntityType filterType) {
        if (filterType == null) {
            return new ArrayList<>(this.minions);
        }
        List<LivingEntity> filteredMinions = new ArrayList<>();
        for (LivingEntity minion : this.minions) {
            if (minion.getType() == filterType) {
                filteredMinions.add(minion);
            }
        }
        return filteredMinions;
    }

    public boolean addMinion(LivingEntity minion) {
        if (this.minions.contains(minion)) {
            return false;
        }
        this.minions.add(minion);
        return true;
    }

    /** Called by minions every tick while alive. */
    public void onMinionUpdate(LivingEntity minion, long tick) {
    }

    /** Called by minions when they die. */
    public void onMinionDeath(LivingEntity minion, DamageSource damageSource) {
    }

    /** Called by AI goals that attempt to damage minions. */
    public void onTryToDamageMinion(LivingEntity minion, float damageAmount) {
    }

    void tickMinionLifecycle() {
        if (!this.minions.isEmpty()) {
            this.minions.removeIf(minion -> !minion.isAlive());
        }
        if (this.getMasterTarget() instanceof BaseCreatureEntity masterCreature) {
            masterCreature.onMinionUpdate(this, this.updateTick);
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide && this.isBossAlways()) {
            ExtendedWorld extendedWorld = ExtendedWorld.getForWorld(this.level());
            if (extendedWorld != null) {
                extendedWorld.bossRemoved(this);
            }
        }
        if (this.getMasterTarget() instanceof BaseCreatureEntity masterCreature) {
            masterCreature.onMinionDeath(this, damageSource);
        }
        if (!this.level().isClientSide && damageSource.getEntity() instanceof Player killer) {
            this.studyCreatureKillForPlayer(killer);
        }
        if (!this.level().isClientSide && !this.isBoundPet()) {
            this.inventory.dropInventory();
        }
        super.die(damageSource);
    }

    /**
     * Caps damage taken per second at damageLimit by clamping health loss between ticks (official).
     **/
    private void enforceDamageLimit(boolean isClient) {
        if (this.damageLimit <= 0) {
            return;
        }
        if (this.healthLastTick < 0) {
            this.healthLastTick = this.getHealth();
        }
        if (this.healthLastTick - this.getHealth() > this.damageLimit) {
            this.setHealth(this.healthLastTick - this.damageLimit);
        }
        this.healthLastTick = this.getHealth();
        if (!isClient && this.updateTick % 20 == 0) {
            this.damageTakenThisSec = 0;
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (this.damageLimit > 0 && this.damageTakenThisSec >= this.damageLimit) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    public BlockPos getArenaCenter() {
        return this.arenaCenter;
    }

    public void setArenaCenter(BlockPos pos) {
        this.arenaCenter = pos;
    }

    public boolean hasArenaCenter() {
        return this.getArenaCenter() != null;
    }

    public int getPlayerTargetCount() {
        return this.playerTargets.size();
    }

    public boolean extraAnimation01() {
        return this.extraAnimation01;
    }

    protected int currentFindTargetGoalIndex() {
        return this.nextFindTargetIndex;
    }

    public int nearbyCreatureCount(EntityType targetType, double range) {
        return this.getNearbyEntities(Entity.class, entity -> entity.getType() == targetType, range).size();
    }

    /**
     * An instant ranged attack that hits the target directly (no projectile).
     **/
    public boolean attackHitscan(Entity target, double damageScale) {
        if (this.isBlocking() && !this.canAttackWhileBlocking()) {
            return false;
        }
        if (target == null || !this.hasLineOfSight(target)) {
            return false;
        }
        if (!this.attackEntityAsMob(target, damageScale)) {
            return false;
        }
        this.applyContactAttackEffects(target, false);
        this.finishAttackAction();
        return true;
    }

    // ========== Battle Phases / Boss Health Bar (ported 2026-09-26) ==========

    /** Called every tick; bosses override this to switch phases based on health. */
    public void updateBattlePhase() {
    }

    public int getBattlePhase() {
        return this.battlePhase;
    }

    public void setBattlePhase(int phase) {
        if (this.getBattlePhase() == phase) {
            return;
        }
        this.battlePhase = phase;
        this.refreshBossHealthName();
        this.playPhaseSound();
    }

    public void playPhaseSound() {
        SoundEvent sound = ObjectManager.getSound(this.getSoundName() + "_phase");
        if (sound == null) {
            return;
        }
        this.playSound(sound, this.getSoundVolume() * 2, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    /** Returns whether or not this mob is always a boss (some mobs are only custom-spawned as bosses). */
    public boolean isBossAlways() {
        return this.creatureInfo.isBoss();
    }

    public boolean hasPlayerTargets() {
        return !this.playerTargets.isEmpty();
    }

    public boolean showBossInfo() {
        if (this.forceBossHealthBar || this.isBoss()) {
            return true;
        }
        if (this.isRareVariant()) {
            return Variant.showsRareHealthBars();
        }
        return false;
    }

    public void forceBossHealthBar() {
        this.forceBossHealthBar = true;
    }

    public void createBossInfo(BossEvent.BossBarColor color, boolean darkenSky) {
        this.bossInfo = (ServerBossEvent) (new ServerBossEvent(this.getBossHealthName(), color, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(darkenSky);
    }

    @Nullable
    public BossEvent getBossInfo() {
        if (this.bossInfo == null && this.showBossInfo() && !this.getCommandSenderWorld().isClientSide) {
            this.createBossInfo(this.isBoss() ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN, false);
        }
        return this.bossInfo;
    }

    public void refreshBossHealthName() {
        if (this.bossInfo != null) {
            this.bossInfo.setName(this.getBossHealthName());
        }
    }

    private MutableComponent getBossHealthName() {
        MutableComponent name = this.getName().copy();
        if (this.isBossAlways()) {
            name.append(" (").append(Component.translatable("entity.phase")).append(" " + (this.getBattlePhase() + 1) + ")");
        }
        return name;
    }

    private void tickBossHealth(boolean isClient) {
        if (!isClient && this.isBoss() && this.updateTick % 20 == 0 && !this.hasPlayerTargets()) {
            this.heal(1);
        }
        if (this.bossInfo != null) {
            this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.getBossInfo() != null) {
            this.bossInfo.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (this.bossInfo != null) {
            this.bossInfo.removePlayer(player);
        }
    }

    public enum TARGET_TYPES {
        ENEMY((byte) 1), ALLY((byte) 2), SELF((byte) 4);
        public final byte id;

        TARGET_TYPES(byte value) {
            this.id = value;
        }
    }

    /**
     * Returns true if this creature isn't slowed by webs (cobweb, quickweb, frostweb).
     **/
    public boolean webProof() {
        return false;
    }

    @Override
    public void makeStuckInBlock(BlockState blockState, Vec3 motionMultiplier) {
        if (this.webProof() && (blockState.getBlock() == Blocks.COBWEB
                || blockState.getBlock() == ObjectManager.getBlock("quickweb")
                || blockState.getBlock() == ObjectManager.getBlock("frostweb"))) {
            return;
        }
        super.makeStuckInBlock(blockState, motionMultiplier);
    }

    /**
     * Leaps forwards in the facing direction.
     **/
    public void leap(double distance, double leapHeight) {
        if (!this.isFlying()) {
            this.playJumpSound();
        }
        double angle = Math.toRadians(this.yRotO);
        double xAmount = -Math.sin(angle);
        double yAmount = leapHeight;
        double zAmount = Math.cos(angle);
        if (this.isFlying()) {
            yAmount = Math.sin(Math.toRadians(this.xRotO)) * distance + this.getDeltaMovement().y() * 0.2D;
        }
        this.push(
                xAmount * distance + this.getDeltaMovement().x() * 0.2D,
                yAmount,
                zAmount * distance + this.getDeltaMovement().z() * 0.2D
        );
        CommonHooks.onLivingJump(this);
        if (!this.getCommandSenderWorld().isClientSide) {
            this.hurtMarked = true;
        }
    }

    /**
     * Leaps towards the target entity if it's within range.
     **/
    public void leap(float range, double leapHeight, Entity target) {
        if (target == null) {
            return;
        }
        this.leap(range, leapHeight, target.blockPosition());
    }

    /**
     * Leaps towards the target position if it's between 2 blocks and range away.
     **/
    public void leap(float range, double leapHeight, BlockPos targetPos) {
        if (targetPos == null) {
            return;
        }
        if (!this.isFlying()) {
            this.playJumpSound();
        }
        double distance = targetPos.distSqr(this.blockPosition());
        if (distance > 2.0F * 2.0F && distance <= range * range) {
            double xDist = targetPos.getX() - this.blockPosition().getX();
            double zDist = targetPos.getZ() - this.blockPosition().getZ();
            if (xDist == 0) {
                xDist = 0.05D;
            }
            if (zDist == 0) {
                zDist = 0.05D;
            }
            double xzDist = Math.sqrt(xDist * xDist + zDist * zDist);
            float targetYaw = (float) (Math.atan2(zDist, xDist) * 180.0D / Math.PI) - 90.0F;
            this.setYRot(targetYaw);
            this.yRotO = targetYaw;
            this.yBodyRot = targetYaw;
            this.yHeadRot = targetYaw;
            this.push(
                    xDist / xzDist * 0.5D * 0.8D + this.getDeltaMovement().x() * 0.2D,
                    leapHeight,
                    zDist / xzDist * 0.5D * 0.8D + this.getDeltaMovement().z() * 0.2D
            );
            CommonHooks.onLivingJump(this);
            if (!this.getCommandSenderWorld().isClientSide) {
                this.hurtMarked = true;
            }
        }
    }

    // ==================================================
    //                  Ranged Attacks
    // ==================================================
    // Ported in Phase 6a from the official source.

    /**
     * Returns the cooldown time in ticks between ranged attacks.
     **/
    /**
     * Resets the attack cooldown so the next attack can happen immediately.
     **/
    public void resetAttackCooldown() {
        this.attackCooldown = 0;
        this.setAttackCooldownMax(this.attackCooldownMax);
    }

    /** Players that have damaged this creature while it's a boss (official trackBossPlayerDamage/addPlayerTarget). */
    protected final java.util.Set<Player> playerTargets = new java.util.HashSet<>();

    public void addPlayerTarget(Player player) {
        this.playerTargets.add(player);
    }

    public void forEachPlayerTarget(java.util.function.Consumer<Player> action) {
        this.playerTargets.removeIf(player -> !player.isAlive() || player.isRemoved());
        this.playerTargets.forEach(action);
    }

    /**
     * Advances the attack phase, looping back to 0 after attackPhaseMax.
     **/
    public void nextAttackPhase() {
        if (++this.attackPhase > (this.attackPhaseMax - 1)) {
            this.attackPhase = 0;
        }
    }

    public int getRangedCooldown() {
        return Math.round((float) ((1.0D / this.getAttribute(RANGED_SPEED).getValue()) * 20.0D));
    }

    /**
     * When given a base time (in seconds) this will return the scaled time in ticks with stats taken into account.
     **/
    public int getEffectDuration(int seconds) {
        return Math.round(seconds * (float) this.creatureStats.getEffect() * 20);
    }

    /**
     * Returns the default amplifier to use for effects.
     **/
    public int getEffectAmplifier(float scale) {
        return Math.round((float) this.creatureStats.getAmplifier());
    }

    /**
     * When given a base effect strength value such as a life drain amount, returns it scaled by stats.
     **/
    public int getEffectStrength(float value) {
        return Math.round((value * (float) (this.creatureStats.getAmplifier())));
    }

    /**
     * Applies all element debuffs to the target entity.
     **/
    public void applyDebuffs(LivingEntity entity, int duration, int amplifier) {
        for (ElementInfo element : this.getElements()) {
            element.debuffEntity(entity, this.getEffectDuration(duration), this.getEffectAmplifier(amplifier));
        }
    }

    /**
     * Applies all element buffs to the target entity.
     **/
    public void applyBuffs(LivingEntity entity, int duration, int amplifier) {
        if (this.creatureStats.getAmplifier() >= 0) {
            for (ElementInfo element : this.getElements()) {
                element.buffEntity(entity, this.getEffectDuration(duration), this.getEffectAmplifier(amplifier));
            }
        }
    }

    /**
     * Used to make this entity fire a ranged attack at the target entity, range is also passed which can be used.
     * Creatures override this to fire their projectile, then call super to finish the attack action.
     **/
    public void attackRanged(Entity target, float range) {
        if (this.isBlocking() && !this.canAttackWhileBlocking()) {
            return;
        }
        this.finishAttackAction();
    }

    /**
     * Deals damage to target entity from a projectile fired by this entity.
     *
     * @param noPierce If true, this creature's piercing stat will be ignored, used for when blocked by a shield, etc.
     * @return True if damage is dealt.
     */
    public boolean doRangedDamage(Entity target, ThrowableProjectile projectile, float damage, boolean noPierce) {
        damage *= (float) (this.creatureStats.getDamage() / 2);
        double pierceDamage = noPierce ? 0 : this.creatureStats.getPierce();

        boolean success;
        if (damage <= pierceDamage) {
            success = target.hurt(this.getDamageSource(target.level().damageSources().thrown(projectile, this)), damage);
        } else {
            int hurtResistantTimeBefore = target.invulnerableTime;
            if (pierceDamage > 0) {
                target.hurt(this.getDamageSource(target.level().damageSources().thrown(projectile, this)), (float) pierceDamage);
            }
            target.invulnerableTime = hurtResistantTimeBefore;
            damage -= (float) pierceDamage;
            success = target.hurt(this.getDamageSource(target.level().damageSources().thrown(projectile, this)), damage);
        }

        if (success && target instanceof LivingEntity livingTarget && this.creatureStats.getAmplifier() >= 0) {
            this.applyDebuffs(livingTarget, 1, 1);
        }

        return success;
    }

    /**
     * Fires a projectile from this mob by Projectile Info name.
     *
     * @param target     The target entity to fire at. If null, the projectile is fired from the facing direction instead.
     * @param angle      The angle offset away from the target in degrees.
     * @param offset     The xyz offset to fire from. Note that the Y offset is relative to 75% of this mob's height.
     * @return The newly created projectile, or null if the projectile isn't known.
     */
    @Nullable
    public BaseProjectileEntity fireProjectile(String projectileName, Entity target, float range, float angle, Vector3d offset, float velocity, float scale, float inaccuracy) {
        ProjectileInfo projectileInfo = ProjectileManager.getInstance().getProjectile(projectileName);
        if (projectileInfo == null) {
            return null;
        }
        return this.fireProjectile(projectileInfo.createProjectile(this.getCommandSenderWorld(), this), target, range, angle, offset, velocity, scale, inaccuracy);
    }

    /**
     * Fires a hardcoded ("old") projectile class from this mob. TODO(port): no old projectiles are registered yet
     * (lasers, hellfire), so this currently returns null.
     */
    @Nullable
    public BaseProjectileEntity fireProjectile(Class<? extends BaseProjectileEntity> projectileClass, Entity target, float range, float angle, Vector3d offset, float velocity, float scale, float inaccuracy) {
        BaseProjectileEntity projectile = ProjectileManager.getInstance().createOldProjectile(projectileClass, this.getCommandSenderWorld(), this);
        return this.fireProjectile(projectile, target, range, angle, offset, velocity, scale, inaccuracy);
    }

    /**
     * Fires the provided projectile instance from this mob.
     */
    @Nullable
    public BaseProjectileEntity fireProjectile(BaseProjectileEntity projectile, Entity target, float range, float angle, Vector3d offset, float velocity, float scale, float inaccuracy) {
        if (projectile == null) {
            return null;
        }

        projectile.setPos(
                projectile.getX() + offset.x * this.sizeScale,
                projectile.getY() + (this.getBbHeight() / 2) + (offset.y * this.sizeScale),
                projectile.getZ() + offset.z * this.sizeScale
        );
        projectile.setProjectileScale(scale);

        Vector3d projectileVector = this.resolveProjectileVector(projectile, target, range, angle, offset);
        projectile.shoot(projectileVector.x, projectileVector.y, projectileVector.z, velocity, inaccuracy);
        DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, projectile, () -> {
            if (projectile.getLaunchSound() != null) {
                this.playSound(projectile.getLaunchSound(), 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            }
        });

        return projectile;
    }

    private Vector3d resolveProjectileVector(BaseProjectileEntity projectile, Entity target, float range, float angle, Vector3d offset) {
        Vector3d facing = this.getFacingPositionDouble(this.getX(), this.getY(), this.getZ(), range, angle);
        double distanceX = facing.x - this.getX();
        double distanceZ = facing.z - this.getZ();
        double distanceXZ = Math.sqrt(distanceX * distanceX + distanceZ * distanceZ) * 0.1D;
        double distanceY = distanceXZ;
        if (target != null) {
            double targetX = target.getX() - this.getX();
            double targetZ = target.getZ() - this.getZ();
            double newX = targetX * Math.cos(angle) - targetZ * Math.sin(angle);
            double newY = targetX * Math.sin(angle) + targetZ * Math.cos(angle);
            targetX = newX + this.getX();
            targetZ = newY + this.getZ();

            distanceX = targetX - this.getX();
            distanceY = target.getBoundingBox().minY + (target.getBbHeight() * 0.5D) - projectile.getY() + offset.y;
            distanceZ = targetZ - this.getZ();
        }
        return new Vector3d(distanceX, distanceY, distanceZ);
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
        if (!this.getCommandSenderWorld().isClientSide) {
            return this.getTarget() != null;
        }
        return this.hasTargetBit(TARGET_BITS.ATTACK);
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
        if (!this.getCommandSenderWorld().isClientSide) {
            return this.getParentTarget() != null;
        }
        return this.hasTargetBit(TARGET_BITS.PARENT);
    }

    public LivingEntity getAvoidTarget() {
        return this.avoidTarget;
    }

    public void setAvoidTarget(LivingEntity setTarget) {
        this.currentFleeTime = this.fleeTime;
        this.avoidTarget = setTarget;
    }

    public boolean hasAvoidTarget() {
        if (!this.getCommandSenderWorld().isClientSide) {
            return this.getAvoidTarget() != null;
        }
        return this.hasTargetBit(TARGET_BITS.AVOID);
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

    /** Perches this creature on the target (e.g. a pet on its owner's shoulder), or stops perching with null. **/
    public void perchOnEntity(LivingEntity target) {
        if (target == null) {
            this.clearPerchTarget();
            return;
        }

        ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(target);
        if (extendedEntity == null) {
            return;
        }
        this.setPerchTarget(target);
        extendedEntity.setPerchedByEntity(this);
    }

    void tickPerchState() {
        LivingEntity perchTarget = this.getPerchTarget();
        if (perchTarget == null) {
            return;
        }

        ExtendedEntity perchEntityExt = ExtendedEntity.getForEntity(perchTarget);
        if (perchEntityExt != null) {
            org.joml.Vector3d perchPosition = perchEntityExt.getPerchPosition();
            this.setPos(perchPosition.x, perchPosition.y, perchPosition.z);
            this.setDeltaMovement(perchTarget.getDeltaMovement());
            this.yRotO = perchTarget.yRotO;
        }
        if (perchTarget instanceof Player playerTarget) {
            ExtendedPlayer perchPlayerExt = ExtendedPlayer.getForPlayer(playerTarget);
            if (perchPlayerExt != null && perchPlayerExt.isControlActive(ExtendedPlayer.CONTROL_ID.MOUNT_DISMOUNT)) {
                this.perchOnEntity(null);
            }
        }
    }

    private void clearPerchTarget() {
        if (this.getPerchTarget() != null) {
            ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(this.getPerchTarget());
            if (extendedEntity != null) {
                extendedEntity.setPerchedByEntity(null);
            }
        }
        this.setPerchTarget(null);
    }

    /** Drops the carried entity if it died or got too far away. **/
    void tickPickupState() {
        if (this.getCommandSenderWorld().isClientSide) {
            return;
        }
        LivingEntity pickupEntity = this.getPickupEntity();
        if (pickupEntity == null) {
            return;
        }
        if (!pickupEntity.isAlive() || this.distanceToSqr(pickupEntity) > 32D * 32D) {
            this.dropPickupEntity();
        }
    }

    public boolean hasPerchTarget() {
        if (!this.getCommandSenderWorld().isClientSide) {
            return this.getPerchTarget() != null;
        }
        return this.hasTargetBit(TARGET_BITS.PERCH);
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
     * Flying creatures take no fall damage; others have their fall distance reduced by getFallResistance().
     **/
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        if (this.isFlying()) {
            return false;
        }
        // Official getFallResistance(): reduces the fall distance, 100+ means no fall damage at all.
        fallDistance -= this.getFallResistance();
        if (this.getFallResistance() >= 100 || fallDistance <= 0) {
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
     * creatures that can't breathe air, and daylight burning (tickDaylightBurn).
     **/
    void tickEnvironmentalState(boolean isClient) {
        // Stealth runs on both sides (invisibility + target clearing server side, start/onStealth hooks both).
        this.tickStealthState(isClient);
        if (isClient) {
            return;
        }

        float brightness = this.getBrightness();
        if (this.daylightBurns() && this.getCommandSenderWorld().isDay()) {
            this.tickDaylightBurn(this.getCommandSenderWorld(), brightness);
        }
        this.applyLightSpawnPressure(brightness);

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

    /** Undead-style creatures burn in daylight unless they wear a helmet (which takes the damage instead). **/
    private void tickDaylightBurn(Level world, float brightness) {
        if (brightness <= 0.5F || this.getRandom().nextFloat() * 30.0F >= (brightness - 0.4F) * 2.0F || !world.canSeeSkyFromBelowWater(this.blockPosition())) {
            return;
        }

        ItemStack helmet = this.inventory.getEquipmentStack("head");
        if (!helmet.isEmpty()) {
            if (helmet.isDamageableItem()) {
                helmet.setDamageValue(helmet.getDamageValue() + this.getRandom().nextInt(2));
                if (helmet.getDamageValue() >= helmet.getMaxDamage()) {
                    this.inventory.setEquipmentStack("head", ItemStack.EMPTY);
                }
            }
            return;
        }
        this.igniteForSeconds(8);
    }

    /** Creatures in light they wouldn't spawn in (or dark, for light-only ones) build up despawn pressure. **/
    private void applyLightSpawnPressure(float brightness) {
        if (!this.creatureInfo.getCreatureSpawn().spawnsInLight() && brightness > 0.5F) {
            this.noActionTime += 2;
        } else if (!this.creatureInfo.getCreatureSpawn().spawnsInDark() && brightness <= 0.5F) {
            this.noActionTime += 2;
        }
    }

    private void tickStealthState(boolean isClient) {
        if (!isClient) {
            if (this.isStealthed() && !this.isInvisible()) {
                this.setInvisible(true);
            } else if (!this.isStealthed() && this.isInvisible() && !this.hasEffect(MobEffects.INVISIBILITY)) {
                this.setInvisible(false);
            }
        }
        if (this.isStealthed()) {
            if (this.stealthPrev != this.isStealthed()) {
                this.startStealth();
            }
            this.onStealth();
        } else if (this.isInvisible() && !this.hasEffect(MobEffects.INVISIBILITY) && !isClient) {
            this.setInvisible(false);
        }
        this.stealthPrev = this.isStealthed();
    }

    /** While stealthed, whatever this creature is targeting loses track of its own target. **/
    public void onStealth() {
        if (!this.getCommandSenderWorld().isClientSide) {
            if (this.getTarget() instanceof Mob mobTarget && mobTarget.getTarget() != null) {
                mobTarget.setTarget(null);
            }
        }
    }

    /** Gliders (getFallingMod() < 1) fall slowly. **/
    private void applyGlidingSlowdown() {
        if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1, this.getFallingMod(), 1));
        }
    }

    @Override
    public void lavaHurt() {
        if (!this.canBurn()) {
            return;
        }
        super.lavaHurt();
    }

    @Override
    public void igniteForTicks(int ticks) {
        if (!this.canBurn()) {
            return;
        }
        super.igniteForTicks(ticks);
    }

    /** Elements make creatures immune to some effects (e.g. poison for plant/poison elements). **/
    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        for (ElementInfo element : this.getElements()) {
            if (!element.isEffectApplicable(effectInstance)) {
                return false;
            }
        }
        return super.canBeAffected(effectInstance);
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

    public byte testLightLevel() {
        return this.testLightLevel(this.blockPosition());
    }

    /**
     * Returns a light rating for the light level the specified XYZ position.
     * Dark enough for spawnsInDarkness: 0 = Dark, 1 = Dim
     * Light enough for spawnsInLight: 2 = Light, 3 = Bright
     * Official conventions kept: below y=0 is always dark, water is measured at its surface (config), air at the
     * ground below it.
     **/
    public byte testLightLevel(BlockPos pos) {
        BlockState spawnBlockState = this.getCommandSenderWorld().getBlockState(pos);
        if (pos.getY() < 0) {
            return 0;
        }
        if (com.lycanitesmobs.core.block.Material.WATER.contains(spawnBlockState.getBlock()) && CreatureManager.getInstance().getSpawnConfig().useSurfaceLightLevel()) {
            pos = new BlockPos(pos.getX(), this.getWaterSurfaceY(pos), pos.getZ());
        } else {
            pos = new BlockPos(pos.getX(), this.getGroundY(pos), pos.getZ());
        }

        int rawLight = this.getCommandSenderWorld().getMaxLocalRawBrightness(pos);
        if (rawLight == 0) return 0;
        if (rawLight <= 8) return 1;
        if (rawLight < 15) return 2;
        return 3;
    }




    /**
     * Returns the Y position of the water surface (first air block searching up, max 24 blocks), or the highest
     * water block if covered.
     **/
    public int getWaterSurfaceY(BlockPos pos) {
        int y = pos.getY();
        if (y <= 0) {
            return 0;
        }
        int yMax = this.getCommandSenderWorld().getMaxBuildHeight() - 1;
        if (y >= yMax) {
            return yMax;
        }
        yMax = Math.min(yMax, y + 24);
        BlockState startBlock = this.getCommandSenderWorld().getBlockState(pos);
        if (startBlock.getBlock() == Blocks.WATER) {
            int possibleSurfaceY = y;
            for (possibleSurfaceY += 1; possibleSurfaceY <= yMax; possibleSurfaceY++) {
                BlockState possibleSurfaceBlock = this.getCommandSenderWorld().getBlockState(new BlockPos(pos.getX(), possibleSurfaceY, pos.getZ()));
                if (possibleSurfaceBlock.isAir()) {
                    return possibleSurfaceY;
                } else if (possibleSurfaceBlock.getBlock() != Blocks.WATER) {
                    return possibleSurfaceY - 1;
                }
            }
            return Math.max(possibleSurfaceY - 1, y);
        }
        return y;
    }

    public boolean isSwimmable(int x, int y, int z) {
        BlockState blockState = this.getCommandSenderWorld().getBlockState(new BlockPos(x, y, z));
        if (this.isLavaCreature && com.lycanitesmobs.core.block.Material.LAVA.contains(blockState.getBlock())) {
            return true;
        }
        return com.lycanitesmobs.core.block.Material.WATER.contains(blockState.getBlock());
    }

    @Override
    protected float getWaterSlowDown() {
        if (!this.isPushedByFluid()) {
            return 1F;
        }
        return 0.8F;
    }

    /** Official: riders are never dumped off Lycanites mounts underwater (aquatic mounts). **/
    @Override
    public boolean dismountsUnderwater() {
        return false;
    }

    @Override
    protected boolean canRide(Entity entity) {
        if (this.isBoss()) {
            return false;
        }
        return super.canRide(entity);
    }

    public boolean canCarryItems() {
        return this.getInventorySize() > 0;
    }

    @Override
    public int getAmbientSoundInterval() {
        return CreatureManager.getInstance().getConfig().idleSoundTicks();
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

    /** Fixate target saved by UUID, resolved to the entity once it's loaded after a reload. **/
    private UUID fixateUUID = null;

    private void tickTargetRuntime() {
        if (!this.getCommandSenderWorld().isClientSide && !this.hasFixateTarget() && this.fixateUUID != null
                && this.getCommandSenderWorld() instanceof ServerLevel serverLevel) {
            Entity foundEntity = serverLevel.getEntity(this.fixateUUID);
            if (foundEntity instanceof LivingEntity livingTarget && foundEntity != this) {
                this.setFixateTarget(livingTarget);
            }
            this.fixateUUID = null;
        }
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
    /**
     * Called on this creature's first server tick. Restored from the official source 2026-09-26 - the earlier port had
     * trimmed it to just clearing the flag, so no creature ever got its starting level (which refreshes stats and heals
     * to the new max health - every creature spawned at 20 HP, bosses included), no uncommon/rare variant ever spawned
     * naturally, and sizes never varied.
     **/
    public void onFirstSpawn() {
        this.firstSpawn = false;
        if (this.handleFirstSpawnPetEntry()) {
            return;
        }
        if (this.isMinion()) {
            return;
        }
        if (this.needsInitialLevel) {
            this.applyLevel(this.getStartingLevel());
        }
        if (this.getSubspeciesIndex() == 0 && this.getVariantIndex() == 0) {
            this.getRandomSubspecies();
            if (CreatureManager.getInstance().getConfig().variantsSpawn() && !this.creatureInfo.getCreatureSpawn().disablesVariants()) {
                this.getRandomVariant();
            }
        }
        if (this.sizeScale == 1.0D && CreatureManager.getInstance().getConfig().randomSizes()) {
            this.getRandomSize();
        }
    }

    private void readBindingFlags(CompoundTag nbt) {
        if (nbt.contains("IsMinion")) {
            this.setMinion(nbt.getBoolean("IsMinion"));
        }
        if (nbt.contains("IsTemporary") && nbt.getBoolean("IsTemporary") && nbt.contains("TemporaryDuration")) {
            this.setTemporary(nbt.getInt("TemporaryDuration"));
        } else {
            this.unsetTemporary();
        }
        if (nbt.contains("IsBoundPet") && nbt.getBoolean("IsBoundPet") && !this.hasPetEntry()) {
            this.boundPetOrphan = true;
        }
    }

    private void writeBindingFlags(CompoundTag nbt) {
        nbt.putBoolean("IsMinion", this.isMinion());
        nbt.putBoolean("IsTemporary", this.isTemporary);
        nbt.putInt("TemporaryDuration", this.temporaryDuration);
        nbt.putBoolean("IsBoundPet", this.isBoundPet());
    }

    /**
     * S202: creature levels are scrapped for spawned creatures (Power Scale handles scaling), so every creature starts at
     * level 1. The startingLevel/levelPerDay/levelPerLocalDifficulty configs are ignored. Explicit levels (dungeon
     * bosses' mobLevel, pet experience, fusion) still apply.
     */
    public int getStartingLevel() {
        return 1;
    }

    public void getRandomSubspecies() {
        if (!this.isMinion()) {
            this.subspecies = this.creatureInfo.getRandomSubspecies(this);
        }
    }

    public void getRandomVariant() {
        if (!this.isMinion()) {
            Variant randomVariant = this.getSubspecies().getRandomVariant(this, this.spawnedRare);
            this.applyVariant(randomVariant != null ? randomVariant.getIndex() : 0);
        }
    }

    public void getRandomSize() {
        double range = CreatureManager.getInstance().getConfig().randomSizeMax() - CreatureManager.getInstance().getConfig().randomSizeMin();
        double scale = CreatureManager.getInstance().getConfig().randomSizeMin() + range * this.getRandom().nextDouble();
        if (this.getVariant() != null) {
            scale *= this.getVariant().getScale();
        }
        this.setSizeScale(scale);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.creatureInfo.isDummy()) {
            return;
        }
        this.onSyncUpdate();
        if (!this.getCommandSenderWorld().isClientSide && this.firstSpawn) {
            this.onFirstSpawn();
        }
        this.tickBlockingState();
        this.tickTargetRuntime();
        this.applyGlidingSlowdown();
        this.tickMovementRuntime(this.getCommandSenderWorld().isClientSide);
        this.tickMinionLifecycle();
        this.updateBattlePhase();
        if (this.tickTemporaryDespawn() || this.discardIfOrphanedBoundPet()) {
            return;
        }
        this.tickBossHealth(this.getCommandSenderWorld().isClientSide);
        this.tickBossArena(this.getCommandSenderWorld());
        this.tickPerchState();
        this.tickPickupState();
        if (this.tickCount % 20 == 0 && !this.getCommandSenderWorld().isClientSide && this.isAlive() && this.canPickupItems()) {
            this.pickupItems();
        }
        this.tickBeastiaryProximityDiscovery(this.getCommandSenderWorld(), this.getCommandSenderWorld().isClientSide);
        this.tickGuiRefresh(this.getCommandSenderWorld().isClientSide);
        this.enforceDamageLimit(this.getCommandSenderWorld().isClientSide);
        this.tickEnvironmentalState(this.getCommandSenderWorld().isClientSide);
        this.updateTick++;
    }

    /**
     * Fire clearing, land-lock for non-walkers, the climbing flag, fly sounds and the flyer attack leap (restored
     * 2026-09-30; Phase 5g had trimmed the last two).
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
            if (!this.onGround() && this.flySoundSpeed > 0 && this.tickCount % 20 == 0) {
                this.playFlySound();
            }
        }
        if (!isClient && this.isFlying() && this.hasAttackTarget() && this.updateTick % 40 == 0) {
            this.leap(0, 0.4D);
        }
    }

    /**
     * Used when loading this mob from a saved chunk.
     * Restored to the official set of saved data (2026-09-28).
     */
    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (this.creatureInfo.isDummy()) {
            return;
        }

        this.firstSpawn = !nbt.contains("FirstSpawn") || nbt.getBoolean("FirstSpawn");
        this.relationships.load(nbt);
        this.inventory.load(nbt);
        this.readBindingFlags(nbt);
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
        if (nbt.contains("DropsRequirePlayerDamage")) {
            this.dropsRequirePlayerDamage = nbt.getBoolean("DropsRequirePlayerDamage");
        }
        if (nbt.contains("Drops")) {
            ListTag nbtDropList = nbt.getList("Drops", 10);
            for (int i = 0; i < nbtDropList.size(); i++) {
                this.addSavedItemDrop(new ItemDrop(nbtDropList.getCompound(i)));
            }
        }
        if (nbt.contains("SpawnedRare")) {
            this.setSpawnedRare(nbt.getBoolean("SpawnedRare"));
        }
        if (nbt.contains("HomeX") && nbt.contains("HomeY") && nbt.contains("HomeZ") && nbt.contains("HomeDistanceMax")) {
            this.restrictTo(new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ")), (int) nbt.getFloat("HomeDistanceMax"));
        }
        // Restored 2026-09-28 (official read*Data methods):
        if (nbt.contains("SpawnEventType")) {
            this.spawnEventType = nbt.getString("SpawnEventType");
        }
        if (nbt.contains("SpawnEventCount")) {
            this.spawnEventCount = nbt.getInt("SpawnEventCount");
        }
        if (nbt.contains("ForceNoDespawn") && nbt.getBoolean("ForceNoDespawn")) {
            this.setPersistenceRequired();
        }
        if (nbt.contains("Stealth")) {
            this.setStealth(nbt.getFloat("Stealth"));
        }
        if (nbt.contains("ArenaX") && nbt.contains("ArenaY") && nbt.contains("ArenaZ")) {
            this.setArenaCenter(new BlockPos(nbt.getInt("ArenaX"), nbt.getInt("ArenaY"), nbt.getInt("ArenaZ")));
        }
        if (nbt.contains("FixateUUIDMost") && nbt.contains("FixateUUIDLeast")) {
            this.fixateUUID = new UUID(nbt.getLong("FixateUUIDMost"), nbt.getLong("FixateUUIDLeast"));
        }
        if (nbt.contains("ExtraBehaviour")) {
            this.extraMobBehaviour.read(nbt.getCompound("ExtraBehaviour"));
        }
        // Official quirk kept: minions are saved by runtime entity id, so this only relinks within a session.
        if (nbt.contains("MinionIds")) {
            ListTag minionIds = nbt.getList("MinionIds", 10);
            for (int i = 0; i < minionIds.size(); i++) {
                CompoundTag minionId = minionIds.getCompound(i);
                if (minionId.contains("ID") && this.getCommandSenderWorld().getEntity(minionId.getInt("ID")) instanceof LivingEntity livingEntity) {
                    this.addMinion(livingEntity);
                }
            }
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
        this.inventory.save(nbt);
        this.writeBindingFlags(nbt);
        nbt.putByte("Subspecies", (byte) this.getSubspeciesIndex());
        nbt.putByte("Variant", (byte) this.getVariantIndex());
        nbt.putDouble("Size", this.sizeScale);
        nbt.putInt("MobLevel", this.getMobLevel());
        nbt.putInt("Experience", this.getExperience());
        nbt.putByte("Color", (byte) this.getColor().getId());
        nbt.putBoolean("SpawnedAsBoss", this.wasSpawnedAsBoss());
        nbt.putBoolean("DropsRequirePlayerDamage", this.dropsRequirePlayerDamage);
        ListTag nbtDropList = new ListTag();
        for (ItemDrop drop : this.savedDrops) {
            CompoundTag dropNBT = new CompoundTag();
            if (drop.writeToNBT(dropNBT)) {
                nbtDropList.add(dropNBT);
            }
        }
        nbt.put("Drops", nbtDropList);
        nbt.putBoolean("SpawnedRare", this.wasSpawnedRare());
        if (this.hasHome()) {
            BlockPos homePos = this.getRestrictCenter();
            nbt.putInt("HomeX", homePos.getX());
            nbt.putInt("HomeY", homePos.getY());
            nbt.putInt("HomeZ", homePos.getZ());
            nbt.putFloat("HomeDistanceMax", this.getHomeDistanceMax());
        }
        // Restored 2026-09-28 (official write*Data methods):
        nbt.putString("SpawnEventType", this.spawnEventType);
        nbt.putInt("SpawnEventCount", this.spawnEventCount);
        nbt.putBoolean("ForceNoDespawn", this.isPersistant());
        nbt.putFloat("Stealth", this.getStealth());
        if (this.hasArenaCenter()) {
            BlockPos arenaPos = this.getArenaCenter();
            nbt.putInt("ArenaX", arenaPos.getX());
            nbt.putInt("ArenaY", arenaPos.getY());
            nbt.putInt("ArenaZ", arenaPos.getZ());
        }
        if (this.getFixateTarget() != null) {
            nbt.putLong("FixateUUIDMost", this.getFixateTarget().getUUID().getMostSignificantBits());
            nbt.putLong("FixateUUIDLeast", this.getFixateTarget().getUUID().getLeastSignificantBits());
        }
        CompoundTag extTagCompound = new CompoundTag();
        this.extraMobBehaviour.write(extTagCompound);
        nbt.put("ExtraBehaviour", extTagCompound);
        ListTag minionIds = new ListTag();
        for (LivingEntity minion : this.minions) {
            CompoundTag minionId = new CompoundTag();
            minionId.putInt("ID", minion.getId());
            minionIds.add(minionId);
        }
        nbt.put("MinionIds", minionIds);
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
