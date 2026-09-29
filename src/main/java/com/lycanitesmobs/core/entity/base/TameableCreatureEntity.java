package com.lycanitesmobs.core.entity.base;

import net.minecraft.world.entity.projectile.ThrowableProjectile;
import com.lycanitesmobs.core.item.consumable.utility.ItemSoulstone;
import com.lycanitesmobs.core.data.info.creature.CreatureKnowledge;
import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.data.info.creature.CreatureType;
import com.lycanitesmobs.core.entity.damagesources.MinionEntityDamageSource;
import com.lycanitesmobs.core.entity.goals.actions.BegGoal;
import com.lycanitesmobs.core.entity.goals.actions.FollowOwnerGoal;
import com.lycanitesmobs.core.entity.goals.actions.StayGoal;
import com.lycanitesmobs.core.entity.goals.targeting.CopyOwnerAttackTargetGoal;
import com.lycanitesmobs.core.entity.goals.targeting.DefendOwnerGoal;
import com.lycanitesmobs.core.entity.goals.targeting.RevengeOwnerGoal;
import com.lycanitesmobs.core.entity.util.CreatureRelationshipEntry;
import com.lycanitesmobs.core.item.consumable.entity.CreatureTreatItem;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * Phase 5e: ownership, taming, pet behaviour (sit/follow/stances/PvP), hunger and stamina on top of
 * AgeableCreatureEntity. Ported from the official 1,494-line class.
 *
 * <p>Deliberately left out until their systems are ported (each marked TODO(port) where it would go):
 * <ul>
 *   <li>summonMinion() owner copy.</li>
 *   <li>Projectiles (doRangedDamage owner credit), ChargeItem (charge
 *       command - also moot since S202 drops creature levels), perching,
 *       mob-event spawn tracking, boss health bars, portal-time clamp.</li>
 * </ul>
 * Breeding hooks (onCreateBaby/createChild owner copy) are omitted: S202 cuts breeding.
 */
public abstract class TameableCreatureEntity extends AgeableCreatureEntity implements OwnableEntity {
    // Stats:
    protected float hunger = this.getCreatureHungerMax();
    protected float stamina = this.getStaminaMax();
    protected float staminaRecovery = 0.5F;
    protected float sittingGuardRange = 16F;

    // Owner:
    protected UUID ownerUUID;

    // Datawatcher:
    protected static final EntityDataAccessor<Byte> TAMED = SynchedEntityData.defineId(TameableCreatureEntity.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Optional<UUID>> OWNER_ID = SynchedEntityData.defineId(TameableCreatureEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    protected static final EntityDataAccessor<Float> HUNGER = SynchedEntityData.defineId(TameableCreatureEntity.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> STAMINA = SynchedEntityData.defineId(TameableCreatureEntity.class, EntityDataSerializers.FLOAT);

    protected TameableCreatureEntity(EntityType<? extends AgeableCreatureEntity> entityType, Level world) {
        super(entityType, world);
    }

    /**
     * Used for the TAMED data value, this holds a series of booleans that describe the tamed status as well as instructed behaviour.
     **/
    public enum TAMED_ID {
        IS_TAMED((byte) 1), MOVE_SIT((byte) 2), MOVE_FOLLOW((byte) 4),
        STANCE_PASSIVE((byte) 8), STANCE_AGGRESSIVE((byte) 16), STANCE_ASSIST((byte) 32), PVP((byte) 64);
        public final byte id;

        TAMED_ID(byte value) {
            this.id = value;
        }

        public byte getValue() {
            return id;
        }
    }

    // ==================================================
    //                       Init
    // ==================================================
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TAMED, (byte) 0);
        builder.define(OWNER_ID, Optional.empty());
        builder.define(HUNGER, this.getCreatureHungerMax());
        builder.define(STAMINA, this.getStaminaMax());
    }

    @Override
    protected void registerGoals() {
        // Greater Targeting:
        this.targetSelector.addGoal(this.claimReactTargetGoalIndex(), new RevengeOwnerGoal(this));
        this.targetSelector.addGoal(this.claimReactTargetGoalIndex(), new CopyOwnerAttackTargetGoal(this));

        // Greater Actions:
        this.goalSelector.addGoal(this.claimTravelGoalIndex(), new StayGoal(this));

        super.registerGoals();

        // Lesser Targeting:
        this.targetSelector.addGoal(this.claimSpecialTargetGoalIndex(), new DefendOwnerGoal(this));

        // Lesser Actions:
        this.goalSelector.addGoal(this.claimTravelGoalIndex(), new FollowOwnerGoal(this).setStrayDistance(CreatureManager.getInstance().getConfig().petFollowDistance()).setLostDistance(32).setSpeed(1D));
        this.goalSelector.addGoal(this.claimIdleGoalIndex(), new BegGoal(this));
    }

    // ========== Name ==========
    @Override
    public Component getName() {
        if (!this.isTamed() || !CreatureManager.getInstance().getConfig().ownerTags()) {
            return super.getName();
        }

        MutableComponent ownedName = Component.literal("");
        boolean customName = this.hasCustomName();
        if (customName) {
            ownedName.append(super.getName()).append(" (");
        }

        MutableComponent ownerName = this.getOwnerName().copy();
        String ownerSuffix = "'s ";
        String ownerFormatted = ownerName.getString();
        if (!ownerFormatted.isEmpty()) {
            if ("s".equalsIgnoreCase(ownerFormatted.substring(ownerFormatted.length() - 1)))
                ownerSuffix = "' ";
        }
        ownedName.append(ownerName).append(ownerSuffix).append(this.getFullName());
        if (customName) {
            ownedName.append(")");
        }

        return ownedName;
    }

    // ==================================================
    //                     Spawning
    // ==================================================
    /**
     * The official despawnCheck() bound-pet rules (the despawn system itself isn't ported): a bound pet is removed if its
     * entry has spawned a different entity, or if the entry's owner is gone (saving the entity's data to the entry first).
     **/
    protected boolean checkBoundPetDespawn() {
        if (this.getCommandSenderWorld().isClientSide || this.getPetEntry() == null)
            return false;
        if (this.getPetEntry().getEntity() != this && this.getPetEntry().getEntity() != null)
            return true;
        if (this.getPetEntry().getOwner() == null || !this.getPetEntry().getOwner().isAlive()) {
            this.getPetEntry().saveEntityNBT();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPersistant() {
        return this.isTamed() || super.isPersistant();
    }

    // ==================================================
    //                     Movement
    // ==================================================
    // 1.21: canBeLeashed() is no-arg and has no player. Tamed creatures can be leashed (the official only
    // allowed the owner - TODO(port): restrict to the owner via getInteractCommands if that matters).
    // TODO(port): official testLeash() dropped the leash of a sitting pet stretched past 10 blocks.
    @Override
    public boolean canBeLeashed() {
        return this.isTamed() || super.canBeLeashed();
    }

    // ==================================================
    //                      Update
    // ==================================================
    @Override
    public void aiStep() {
        if (this.checkBoundPetDespawn()) {
            this.remove(RemovalReason.DISCARDED);
            return;
        }
        super.aiStep();
        this.staminaUpdate();

        // Owner Buffs:
        this.updateOwnerEffects();
    }

    public void staminaUpdate() {
        if (this.getCommandSenderWorld().isClientSide)
            return;
        if (this.stamina < this.getStaminaMax() && this.staminaRecovery >= this.getStaminaRecoveryMax() / 2)
            this.setStamina(Math.min(this.stamina + this.staminaRecovery, this.getStaminaMax()));
        if (this.staminaRecovery < this.getStaminaRecoveryMax())
            this.staminaRecovery = Math.min(this.staminaRecovery + (this.getStaminaRecoveryMax() / this.getStaminaRecoveryWarmup()), this.getStaminaRecoveryMax());
    }

    private void updateOwnerEffects() {
        if (this.getCommandSenderWorld().isClientSide || this.updateTick % 20 != 0 || !this.isPet()) {
            return;
        }
        Player owner = this.getPlayerOwner();
        if (owner == null || this.distanceToSqr(owner) > 64) {
            return;
        }
        this.ownerEffects(owner);
    }

    /**
     * Grants constant effects to the owner while nearby: fire resistance from fire-immune pets, and removal of
     * effects this pet is immune to.
     */
    private void ownerEffects(Player owner) {
        if (!this.canBurn()) {
            owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, (5 * 20) + 5, 1));
        }
        for (MobEffectInstance effectInstance : owner.getActiveEffects().toArray(new MobEffectInstance[0])) {
            if (!this.canBeAffected(effectInstance)) {
                owner.removeEffect(effectInstance.getEffect());
            }
        }
    }

    /**
     * Returns true if this is a standard pet: tamed with a player owner, not a mount, minion or other pet entry type.
     */
    public boolean isPet() {
        if (!this.isTamed() || this.getPlayerOwner() == null) {
            return false;
        }
        if (this.creatureInfo.isMountable()) {
            return false;
        }
        if (this.isTemporary()) {
            return false;
        }
        return this.getPetEntry() == null || this.isPetType("pet");
    }

    /**
     * Copies this creature's behaviour to the provided tamed creature.
     */
    public void copyPetBehaviourTo(TameableCreatureEntity target) {
        target.setPVP(this.isPVP());
        target.setPassive(this.isPassive());
        target.setAssist(this.isAssisting());
        target.setAggressive(this.isAggressive());

        target.setSitting(this.isSitting());
        target.setFollowing(this.isFollowing());
    }

    // ==================================================
    //                     Interact
    // ==================================================
    @Override
    public HashMap<Integer, String> getInteractCommands(Player player, @Nonnull ItemStack itemStack) {
        HashMap<Integer, String> commands = new HashMap<>(super.getInteractCommands(player, itemStack));
        this.addTameableInteractCommands(commands, player, itemStack);
        return commands;
    }

    @Override
    public boolean performCommand(String command, Player player, ItemStack itemStack, InteractionHand hand) {
        Boolean result = this.performTameableCommand(command, player, itemStack);
        if (result != null) {
            return result;
        }
        return super.performCommand(command, player, itemStack, hand);
    }

    private void addTameableInteractCommands(HashMap<Integer, String> commands, Player player, ItemStack itemStack) {
        if (this.canPerch(player) && !player.isShiftKeyDown() && !this.getCommandSenderWorld().isClientSide) {
            commands.put(BaseCreatureEntity.COMMAND_PIORITIES.MAIN.id, "Perch");
        } else if (!this.getCommandSenderWorld().isClientSide && player.isShiftKeyDown() && this.isTamed() && player == this.getPlayerOwner()) {
            commands.put(BaseCreatureEntity.COMMAND_PIORITIES.MAIN.id, "GUI");
        }

        if (this.getCommandSenderWorld().isClientSide || itemStack.isEmpty() || player.isShiftKeyDown()) {
            return;
        }

        if (!this.isTamed() && this.isTamingItem(itemStack) && CreatureManager.getInstance().getConfig().tamingEnabled()) {
            commands.put(BaseCreatureEntity.COMMAND_PIORITIES.IMPORTANT.id, "Tame");
        }

        if (this.isTamed() && this.isHealingItem(itemStack) && this.getHealth() < this.getMaxHealth()) {
            commands.put(BaseCreatureEntity.COMMAND_PIORITIES.ITEM_USE.id, "Feed");
        }

        // Equipment (saddles, horse armor, bags):
        if (this.isTamed() && !this.isBaby() && this.canEquip() && player == this.getPlayerOwner()) {
            String equipSlot = this.inventory.getSlotForEquipment(itemStack);
            if (equipSlot != null && this.inventory.getEquipmentStack(equipSlot).getItem() != itemStack.getItem()) {
                commands.put(BaseCreatureEntity.COMMAND_PIORITIES.EQUIPPING.id, "Equip Item");
            }
        }

        // Soulstone: claims the interaction so the soulstone item's own interactLivingEntity binds the pet.
        if (itemStack.getItem() instanceof ItemSoulstone && this.isTamed()) {
            commands.put(BaseCreatureEntity.COMMAND_PIORITIES.ITEM_USE.id, "Soulstone");
        }

        // TODO(port): "Charge" (ChargeItem). Unequipping needs the creature GUI.
    }

    // ==================================================
    //                       Perching
    // ==================================================
    public boolean canPerch(LivingEntity target) {
        if (!this.creatureInfo.isPerchable()) {
            return false;
        }
        return this.getPlayerOwner() == target;
    }

    private Boolean performTameableCommand(String command, Player player, ItemStack itemStack) {
        if ("Perch".equals(command)) {
            this.playTameSound();
            this.perchOnEntity(player);
            return true;
        }

        if ("GUI".equals(command)) {
            this.playTameSound();
            this.openGUI(player);
            return true;
        }

        if ("Tame".equals(command)) {
            this.tame(player);
            this.consumePlayersItem(player, itemStack);
            return true;
        }

        if ("Feed".equals(command)) {
            this.heal((float) this.getHealAmount(itemStack));
            this.playEatSound();
            this.spawnFeedParticles();
            this.consumePlayersItem(player, itemStack);
            return true;
        }

        if ("Soulstone".equals(command)) {
            return false;
        }

        if ("Equip Item".equals(command)) {
            this.equipHeldItem(itemStack);
            this.consumePlayersItem(player, itemStack);
            return true;
        }

        if ("Sit".equals(command)) {
            this.playTameSound();
            this.setTarget(null);
            this.clearMovement();
            this.setSitting(!this.isSitting());
            this.jumping = false;
            return true;
        }

        return null;
    }

    private void equipHeldItem(ItemStack itemStack) {
        String equipSlot = this.inventory.getSlotForEquipment(itemStack);
        ItemStack equippedItem = this.inventory.getEquipmentStack(equipSlot);
        if (!equippedItem.isEmpty()) {
            this.dropItem(equippedItem);
        }
        ItemStack equipStack = itemStack.copy();
        equipStack.setCount(1);
        this.inventory.setEquipmentStack(equipStack);
    }

    private int getHealAmount(ItemStack itemStack) {
        FoodProperties food = itemStack.getFoodProperties(this);
        if (food != null) {
            return food.nutrition();
        }
        return 4;
    }

    private void spawnFeedParticles() {
        if (!this.getCommandSenderWorld().isClientSide) {
            return;
        }
        this.spawnHeartsOrSmoke(ParticleTypes.HEART, 25);
    }

    private void spawnHeartsOrSmoke(ParticleOptions particle, int count) {
        float width = this.getBbWidth();
        float height = this.getBbHeight();
        for (int i = 0; i < count; i++) {
            double d0 = this.getRandom().nextGaussian() * 0.02D;
            double d1 = this.getRandom().nextGaussian() * 0.02D;
            double d2 = this.getRandom().nextGaussian() * 0.02D;
            this.getCommandSenderWorld().addParticle(
                    particle,
                    this.getX() + (double) (this.getRandom().nextFloat() * width * 2.0F) - (double) width,
                    this.getY() + 0.5D + (double) (this.getRandom().nextFloat() * height),
                    this.getZ() + (double) (this.getRandom().nextFloat() * width * 2.0F) - (double) width,
                    d0, d1, d2);
        }
    }

    @Override
    public boolean canBeControlledByRider() {
        return this.isTamed();
    }

    @Override
    public boolean canNameTag(Player player) {
        if (!this.isTamed() || player == this.getPlayerOwner()) {
            return super.canNameTag(player);
        }
        return false;
    }

    @Override
    public void performGUICommand(Player player, int guiCommandID) {
        if (!this.petControlsEnabled()) {
            return;
        }
        if (player != this.getOwner()) {
            return;
        }

        if (guiCommandID == PET_COMMAND_ID.PVP.id) {
            this.setPVP(!this.isPVP());
        } else if (guiCommandID == PET_COMMAND_ID.PASSIVE.id) {
            this.setPassive(true);
        } else if (guiCommandID == PET_COMMAND_ID.DEFENSIVE.id) {
            this.setPassive(false);
            this.setAssist(false);
            this.setAggressive(false);
        } else if (guiCommandID == PET_COMMAND_ID.ASSIST.id) {
            this.setPassive(false);
            this.setAssist(true);
            this.setAggressive(false);
        } else if (guiCommandID == PET_COMMAND_ID.AGGRESSIVE.id) {
            this.setPassive(false);
            this.setAssist(true);
            this.setAggressive(true);
        } else if (guiCommandID == PET_COMMAND_ID.FOLLOW.id) {
            this.setSitting(false);
            this.setFollowing(true);
        } else if (guiCommandID == PET_COMMAND_ID.WANDER.id) {
            this.setSitting(false);
            this.setFollowing(false);
        } else if (guiCommandID == PET_COMMAND_ID.SIT.id) {
            this.setSitting(true);
            this.setFollowing(false);
        }

        if (this.petEntry != null && this.petEntry.getSummonSet() != null) {
            this.petEntry.getSummonSet().updateBehaviour(this);
        }

        super.performGUICommand(player, guiCommandID);
    }

    // ==================================================
    //                      Targets
    // ==================================================
    @Nullable
    @Override
    public PlayerTeam getTeam() {
        if (this.isTamed()) {
            Entity owner = this.getOwner();
            if (owner != null) {
                return owner.getTeam();
            }
        }
        return super.getTeam();
    }

    /**
     * Returns if this creature is on the same team as the target entity. If PvP is disabled and this creature is
     * tamed then it is considered on the same team as all players and their tames.
     */
    @Override
    public boolean isAlliedTo(Entity target) {
        if (this.getCommandSenderWorld().isClientSide || !this.isTamed()) {
            return super.isAlliedTo(target);
        }

        if (target == this.getPlayerOwner() || target == this.getOwner()) {
            return true;
        }

        if (target instanceof Player && (!this.isServerPvpAllowed() || !this.isPVP())) {
            return true;
        }

        if (target instanceof TameableCreatureEntity tamedTarget) {
            if (tamedTarget.isTamed() && (!this.isServerPvpAllowed() || !this.isPVP() || tamedTarget.getPlayerOwner() == this.getPlayerOwner())) {
                return true;
            }
        } else if (target instanceof OwnableEntity tamedTarget) {
            if (tamedTarget.getOwner() != null && (!this.isServerPvpAllowed() || !this.isPVP() || tamedTarget.getOwner() == this.getOwner())) {
                return true;
            }
        }

        Entity owner = this.getPlayerOwner() != null ? this.getPlayerOwner() : this.getOwner();
        if (owner == null) {
            return false;
        }
        if (owner.getVehicle() == target) {
            return true;
        }
        return owner.isAlliedTo(target);
    }

    // ==================================================
    //                      Attacks
    // ==================================================
    /** Owner kill credit for ranged attacks too: a killing projectile hit is dealt as the owner's minion. **/
    @Override
    public boolean doRangedDamage(Entity target, ThrowableProjectile projectile, float damage, boolean noPierce) {
        float totalDamage = damage * ((float) this.creatureStats.getDamage() / 2);
        if (target instanceof Mob mobTarget && this.getOwner() instanceof Player
                && mobTarget.getHealth() > 0 && mobTarget.getHealth() - totalDamage <= 0) {
            DamageSource creditSource = new MinionEntityDamageSource(this.getDamageSource(null).typeHolder(), this.getOwner());
            return target.hurt(creditSource, totalDamage);
        }
        return super.doRangedDamage(target, projectile, damage, noPierce);
    }

    /** Tamed (non-temporary) creatures never despawn, even on Peaceful. **/
    @Override
    public boolean despawnCheck() {
        if (this.getCommandSenderWorld().isClientSide) {
            return false;
        }
        if (this.isTamed() && !this.isTemporary()) {
            return false;
        }
        return super.despawnCheck();
    }

    /** Minions summoned by a tamed creature belong to its owner and copy its pet behaviour. **/
    @Override
    public void summonMinion(LivingEntity minion, double angle, double distance) {
        if (this.getPlayerOwner() != null && minion instanceof TameableCreatureEntity tameableMinion) {
            tameableMinion.setPlayerOwner(this.getPlayerOwner());
            this.copyPetBehaviourTo(tameableMinion);
        }
        super.summonMinion(minion, angle, distance);
    }

    @Override
    public AgeableCreatureEntity createChild(AgeableCreatureEntity partner) {
        AgeableCreatureEntity spawnedBaby = super.createChild(partner);
        if (this.getOwnerId() != null && spawnedBaby instanceof TameableCreatureEntity tameableBaby) {
            tameableBaby.setOwnerId(this.getOwnerId());
        }
        return spawnedBaby;
    }

    @Override
    public void onCreateBaby(AgeableCreatureEntity partner, AgeableCreatureEntity baby) {
        if (this.isTamed() && this.getOwner() instanceof Player owner && partner instanceof TameableCreatureEntity partnerTameable && baby instanceof TameableCreatureEntity babyTameable) {
            if (partnerTameable.getPlayerOwner() == this.getPlayerOwner()) {
                babyTameable.setPlayerOwner(owner);
            }
        }
        super.onCreateBaby(partner, baby);
    }

    @Override
    public boolean attackEntityAsMob(Entity target, double damageScale) {
        if (!this.isAlive() || target == null || !this.hasLineOfSight(target)) {
            return false;
        }

        // Owner kill credit: if this hit kills a mob, deal it as the owner's minion so the player gets the kill.
        float totalDamage = this.getAttackDamage(damageScale);
        if (target instanceof Mob mobTarget && this.getOwner() instanceof Player
                && mobTarget.getHealth() > 0 && mobTarget.getHealth() - totalDamage <= 0) {
            DamageSource creditSource = new MinionEntityDamageSource(this.getDamageSource(null).typeHolder(), this.getOwner());
            return target.hurt(creditSource, totalDamage);
        }

        return super.attackEntityAsMob(target, damageScale);
    }

    @Override
    public boolean canAttackType(EntityType<?> targetType) {
        if (this.isPassive()) {
            return false;
        }
        if (this.isTamed()) {
            return true;
        }
        return super.canAttackType(targetType);
    }

    @Override
    public boolean canAttack(LivingEntity targetEntity) {
        if (this.isPassive()) {
            return false;
        }
        if (!this.isTamed()) {
            return super.canAttack(targetEntity);
        }

        if (this.getOwner() == targetEntity || this.getPlayerOwner() == targetEntity) {
            return false;
        }
        if (!this.getCommandSenderWorld().isClientSide) {
            boolean canPVP = this.isServerPvpAllowed() && this.isPVP();
            if (targetEntity instanceof Player && !canPVP) {
                return false;
            }
            if (targetEntity instanceof TameableCreatureEntity targetTameable && targetTameable.isTamed()) {
                if (!canPVP) {
                    return false;
                }
                if (targetTameable.getPlayerOwner() == this.getPlayerOwner()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Official: wakeFromAcceptedAttack() stood a sitting pet back up when an attack actually landed.
     **/
    @Override
    public boolean hurt(DamageSource damageSrc, float damageAmount) {
        boolean hurt = super.hurt(damageSrc, damageAmount);
        if (hurt && !this.isPassive()) {
            this.setSitting(false);
        }
        return hurt;
    }

    // ==================================================
    //                    Immunities
    // ==================================================
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (this.blocksOwnerOrPlayerDamage(source.getEntity())) {
            return true;
        }
        if (source.is(DamageTypes.IN_WALL) && this.isTamed()) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    private boolean isServerPvpAllowed() {
        return this.getCommandSenderWorld().getServer() != null && this.getCommandSenderWorld().getServer().isPvpAllowed();
    }

    private boolean blocksOwnerOrPlayerDamage(Entity entity) {
        if (!this.isTamed()) {
            return false;
        }
        if (entity instanceof Player && !this.isServerPvpAllowed()) {
            return true;
        }
        return entity == this.getPlayerOwner();
    }

    // ==================================================
    //                       Owner
    // ==================================================

    /**
     * Sets the owner of this entity via the unique id of the owner entity. Also updates the tamed status of this entity.
     */
    public void setOwnerId(@Nullable UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
        this.getEntityData().set(OWNER_ID, Optional.ofNullable(ownerUUID));
        this.setTamed(ownerUUID != null);
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.getOwnerId();
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        UUID uuid = this.getOwnerId();
        if (uuid == null) {
            return null;
        }
        return this.getCommandSenderWorld().getPlayerByUUID(uuid);
    }

    /**
     * Sets the owner of this entity to the provided player entity. Also updates the tamed status of this entity.
     */
    public void setPlayerOwner(Player player) {
        this.setOwnerId(player.getUUID());
    }

    @Nullable
    public UUID getOwnerId() {
        if (this.getCommandSenderWorld().isClientSide) {
            return this.getEntityData().get(OWNER_ID).orElse(null);
        }
        return this.ownerUUID;
    }

    /**
     * Returns the owner of this entity as a player or null if there is no player owner.
     */
    @Nullable
    public Player getPlayerOwner() {
        if (this.getCommandSenderWorld().isClientSide) {
            return this.getOwner() instanceof Player player ? player : null;
        }
        if (this.ownerUUID == null) {
            return null;
        }
        return this.getCommandSenderWorld().getPlayerByUUID(this.ownerUUID);
    }

    /**
     * Gets the display name of the entity that owns this entity or an empty component if none.
     */
    public Component getOwnerName() {
        Entity owner = this.getOwner();
        if (owner != null) {
            return owner.getDisplayName();
        }
        return Component.literal("");
    }

    // ==================================================
    //                      Taming
    // ==================================================
    @Override
    public boolean isTamed() {
        return (this.getEntityData().get(TAMED) & TAMED_ID.IS_TAMED.id) != 0;
    }

    public void setTamed(boolean isTamed) {
        byte tamed = this.behaviourBitMask();
        if (isTamed) {
            this.getEntityData().set(TAMED, (byte) (tamed | TAMED_ID.IS_TAMED.id));
            this.clearSpawnEventTracking();
        } else {
            this.getEntityData().set(TAMED, (byte) (tamed - (tamed & TAMED_ID.IS_TAMED.id)));
        }
        this.setCustomNameVisible(isTamed);
    }

    public boolean isTamingItem(ItemStack itemstack) {
        CreatureType creatureType = this.creatureInfo.getCreatureType();
        if (itemstack.isEmpty() || creatureType == null || this.isBoss()) {
            return false;
        }
        if (itemstack.getItem() instanceof CreatureTreatItem itemTreat && itemTreat.getCreatureType() == creatureType) {
            return this.creatureInfo.isTameable();
        }
        return false;
    }

    /**
     * Attempts to tame this entity to the provided player. Each treat raises the player's reputation with this
     * creature; it's tamed once reputation reaches the creature's taming reputation.
     *
     * @return True if the entity is tamed, false on failure.
     */
    public boolean tame(Player player) {
        if (this.getCommandSenderWorld().isClientSide || this.isRareVariant() || this.isBoss()) {
            return this.isTamed();
        }

        ExtendedPlayer extendedPlayer = ExtendedPlayer.getForPlayer(player);
        if (extendedPlayer == null) {
            return this.isTamed();
        }

        // Each treat studies the creature; taming needs rank 2 knowledge of it.
        extendedPlayer.studyCreature(this, CreatureManager.getInstance().getConfig().creatureTreatKnowledge(), true, true);

        if (this.isTamed()) {
            return true;
        }

        CreatureKnowledge creatureKnowledge = extendedPlayer.getBeastiary().getCreatureKnowledge(this.creatureInfo.getName());
        if (creatureKnowledge == null || creatureKnowledge.getRank() < 2) {
            return this.isTamed();
        }

        CreatureRelationshipEntry relationshipEntry = this.relationships.getOrCreateEntry(player);
        int reputationAmount = 50 + this.getRandom().nextInt(50);
        relationshipEntry.increaseReputation(reputationAmount);

        if (this.creatureInfo.isTameable() && relationshipEntry.getReputation() >= this.creatureInfo.getTamingReputation()) {
            this.setPlayerOwner(player);
            this.onTamedByPlayer();
            this.unsetTemporary();
            MutableComponent tameMessage = Component.translatable("message.pet.tamed.prefix")
                    .append(" ")
                    .append(this.getSpeciesName())
                    .append(" ")
                    .append(Component.translatable("message.pet.tamed.suffix"));
            player.displayClientMessage(tameMessage, false);
        }

        // Entity event 7 = hearts, 6 = smoke, played client side by handleEntityEvent().
        this.getCommandSenderWorld().broadcastEntityEvent(this, this.isTamed() ? (byte) 7 : (byte) 6);
        return this.isTamed();
    }

    /**
     * Called when this creature is first tamed by a player: clears movement and targets and sets default pet behaviour.
     */
    public void onTamedByPlayer() {
        this.refreshAttributes();
        this.clearMovement();
        this.setTarget(null);
        this.setSitting(false);
        this.setFollowing(true);
        this.setPassive(false);
        this.setAggressive(false);
        this.setPVP(true);
        this.playTameSound();
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 7) {
            this.playTameEffect(true);
        } else if (status == 6) {
            this.playTameEffect(false);
        } else {
            super.handleEntityEvent(status);
        }
    }

    /**
     * Determines if the provided itemstack can be consumed to heal this entity.
     */
    public boolean isHealingItem(ItemStack itemStack) {
        return this.creatureInfo.canEat(itemStack);
    }

    // ==================================================
    //                    Pet Control
    // ==================================================
    public boolean petControlsEnabled() {
        return true;
    }

    public byte behaviourBitMask() {
        return this.getEntityData().get(TAMED);
    }

    // ========== Sitting ==========
    public boolean isSitting() {
        if (!this.isTamed()) {
            return false;
        }
        return this.hasBehaviourBit(TAMED_ID.MOVE_SIT.id);
    }

    public void setSitting(boolean set) {
        if (!this.petControlsEnabled()) {
            set = false;
        }
        if (set) {
            this.enableBehaviourBit(TAMED_ID.MOVE_SIT.id);
            // Official setHome(); 1.21 vanilla Mob restriction replaces the custom home system in this port.
            this.restrictTo(this.blockPosition(), (int) this.sittingGuardRange);
        } else {
            this.disableBehaviourBit(TAMED_ID.MOVE_SIT.id);
            this.clearRestriction();
        }
    }

    // ========== Following ==========
    public boolean isFollowing() {
        if (!this.isTamed()) {
            return false;
        }
        if (this.getLeashHolder() instanceof LeashFenceKnotEntity) {
            return false;
        }
        return this.hasBehaviourBit(TAMED_ID.MOVE_FOLLOW.id);
    }

    public void setFollowing(boolean set) {
        if (!this.petControlsEnabled()) {
            set = false;
        }
        if (set) {
            this.enableBehaviourBit(TAMED_ID.MOVE_FOLLOW.id);
        } else {
            this.disableBehaviourBit(TAMED_ID.MOVE_FOLLOW.id);
        }
    }

    // ========== Passiveness ==========
    public boolean isPassive() {
        if (!this.isTamed()) {
            return false;
        }
        return this.hasBehaviourBit(TAMED_ID.STANCE_PASSIVE.id);
    }

    public void setPassive(boolean set) {
        if (!this.petControlsEnabled()) {
            set = false;
        }
        if (set) {
            this.enableBehaviourBit(TAMED_ID.STANCE_PASSIVE.id);
            this.setTarget(null);
            this.setStealth(0);
        } else {
            this.disableBehaviourBit(TAMED_ID.STANCE_PASSIVE.id);
        }
    }

    // ========== Aggressiveness ==========
    @Override
    public boolean isAggressive() {
        if (!this.isTamed()) {
            return super.isAggressive();
        }
        return this.hasBehaviourBit(TAMED_ID.STANCE_AGGRESSIVE.id);
    }

    @Override
    public void setAggressive(boolean set) {
        if (!this.petControlsEnabled()) {
            set = false;
        }
        if (set) {
            this.enableBehaviourBit(TAMED_ID.STANCE_AGGRESSIVE.id);
        } else {
            this.disableBehaviourBit(TAMED_ID.STANCE_AGGRESSIVE.id);
        }
    }

    // ========== Assist ==========
    public boolean isAssisting() {
        if (!this.isTamed()) {
            return false;
        }
        return this.hasBehaviourBit(TAMED_ID.STANCE_ASSIST.id);
    }

    public void setAssist(boolean set) {
        if (!this.petControlsEnabled()) {
            set = true;
        }
        if (set) {
            this.enableBehaviourBit(TAMED_ID.STANCE_ASSIST.id);
        } else {
            this.disableBehaviourBit(TAMED_ID.STANCE_ASSIST.id);
        }
    }

    // ========== PvP ==========
    public boolean isPVP() {
        return this.hasBehaviourBit(TAMED_ID.PVP.id);
    }

    public void setPVP(boolean set) {
        if (!this.petControlsEnabled()) {
            set = false;
        }
        if (set) {
            this.enableBehaviourBit(TAMED_ID.PVP.id);
        } else {
            this.setTarget(null);
            this.disableBehaviourBit(TAMED_ID.PVP.id);
        }
    }

    private boolean hasBehaviourBit(byte bit) {
        return (this.behaviourBitMask() & bit) != 0;
    }

    private void enableBehaviourBit(byte bit) {
        byte tamedStatus = this.behaviourBitMask();
        this.getEntityData().set(TAMED, (byte) (tamedStatus | bit));
    }

    private void disableBehaviourBit(byte bit) {
        byte tamedStatus = this.behaviourBitMask();
        this.getEntityData().set(TAMED, (byte) (tamedStatus - (tamedStatus & bit)));
    }

    // ==================================================
    //                       Hunger
    // ==================================================
    public float getCreatureHunger() {
        if (this.getCommandSenderWorld() == null)
            return this.getCreatureHungerMax();
        if (!this.getCommandSenderWorld().isClientSide)
            return this.hunger;
        return this.getEntityData().get(HUNGER);
    }

    public void setCreatureHunger(float setHunger) {
        this.hunger = setHunger;
    }

    public float getCreatureHungerMax() {
        return 20;
    }

    // ==================================================
    //                      Stamina
    // ==================================================
    public float getStamina() {
        if (this.getCommandSenderWorld() != null && this.getCommandSenderWorld().isClientSide) {
            this.stamina = this.getEntityData().get(STAMINA);
        }
        return this.stamina;
    }

    public void setStamina(float setStamina) {
        this.stamina = setStamina;
        if (this.getCommandSenderWorld() != null && !this.getCommandSenderWorld().isClientSide) {
            this.getEntityData().set(STAMINA, setStamina);
        }
    }

    public float getStaminaMax() {
        return 100;
    }

    public float getStaminaRecoveryMax() {
        return 1F;
    }

    public int getStaminaRecoveryWarmup() {
        return 10 * 20;
    }

    public float getStaminaCost() {
        return 1;
    }

    public void applyStaminaCost() {
        float newStamina = this.getStamina() - this.getStaminaCost();
        if (newStamina < 0)
            newStamina = 0;
        this.setStamina(newStamina);
        this.staminaRecovery = 0;
    }

    public float getStaminaPercent() {
        return this.getStamina() / this.getStaminaMax();
    }

    // "energy" = Usual blue-orange bar. "toggle" = Solid purple bar for on and off.
    public String getStaminaType() {
        return "energy";
    }

    // ==================================================
    //                      Client
    // ==================================================
    protected void playTameEffect(boolean success) {
        this.spawnHeartsOrSmoke(success ? ParticleTypes.HEART : ParticleTypes.SMOKE, 7);
    }

    // ==================================================
    //                      Visuals
    // ==================================================

    /**
     * Returns true if this mob can be dyed. Only the owner may dye a tamed creature; null = can be dyed in general.
     */
    @Override
    public boolean canBeColored(Player player) {
        if (player == null) return true;
        return this.isTamed() && player == this.getPlayerOwner();
    }

    /** Tamed creatures (even boss-type ones) don't show a boss health bar. */
    @Override
    public boolean showBossInfo() {
        if (this.isTamed()) {
            return false;
        }
        return super.showBossInfo();
    }

    // ==================================================
    //                        NBT
    // ==================================================
    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if (nbt.hasUUID("OwnerId")) {
            this.setOwnerId(nbt.getUUID("OwnerId"));
        } else {
            this.setOwnerId(null);
        }

        this.setSitting(nbt.contains("Sitting") ? nbt.getBoolean("Sitting") : false);
        this.setFollowing(nbt.contains("Following") ? nbt.getBoolean("Following") : true);
        this.setPassive(nbt.contains("Passive") ? nbt.getBoolean("Passive") : false);
        this.setAggressive(nbt.contains("Aggressive") ? nbt.getBoolean("Aggressive") : false);
        this.setPVP(nbt.contains("PVP") ? nbt.getBoolean("PVP") : true);

        if (nbt.contains("Hunger")) {
            this.setCreatureHunger(nbt.getFloat("Hunger"));
        } else {
            this.setCreatureHunger(this.getCreatureHungerMax());
        }

        if (nbt.contains("Stamina")) {
            this.setStamina(nbt.getFloat("Stamina"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.getOwnerId() != null) {
            nbt.putUUID("OwnerId", this.getOwnerId());
        }
        nbt.putBoolean("Sitting", this.isSitting());
        nbt.putBoolean("Following", this.isFollowing());
        nbt.putBoolean("Passive", this.isPassive());
        nbt.putBoolean("Aggressive", this.isAggressive());
        nbt.putBoolean("PVP", this.isPVP());
        nbt.putFloat("Hunger", this.getCreatureHunger());
        nbt.putFloat("Stamina", this.getStamina());
    }

    // ==================================================
    //                      Sounds
    // ==================================================
    @Override
    public int getAmbientSoundInterval() {
        if (this.isTamed())
            return 600;
        return super.getAmbientSoundInterval();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        String sound = "_say";
        if (this.isTamed() && this.getHealth() < this.getMaxHealth())
            sound = "_beg";
        return ObjectManager.getSound(this.getSoundName() + sound);
    }

    public void playTameSound() {
        SoundEvent sound = ObjectManager.getSound(this.getSoundName() + "_tame");
        if (sound != null) {
            this.playSound(sound, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }

    public void playEatSound() {
        SoundEvent sound = ObjectManager.getSound(this.getSoundName() + "_eat");
        if (sound != null) {
            this.playSound(sound, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }
}
