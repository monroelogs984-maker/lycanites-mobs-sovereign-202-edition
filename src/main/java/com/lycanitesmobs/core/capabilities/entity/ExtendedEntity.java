package com.lycanitesmobs.core.capabilities.entity;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.data.config.ConfigAdmin;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.network.message.MessageEntityPerched;
import com.lycanitesmobs.core.network.message.MessageEntityPickedUp;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.joml.Vector3d;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Per-entity runtime data for every LivingEntity: projectile cooldowns (equipment), last attacked entity, being
 * picked up (carried by Roc/Raiko/etc) or perched on, safe drop position and forced removal.
 *
 * Port: a NeoForge data attachment instead of a Forge capability. Not serialized - the official's NBT methods were
 * no-ops, so none of this ever survived a reload. Client entities get their own attachment (the official kept a
 * separate client map), filled by the picked-up/perched sync messages.
 */
public class ExtendedEntity {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LycanitesMobs.MODID);
    public static final Supplier<AttachmentType<ExtendedEntity>> EXTENDED_ENTITY = ATTACHMENT_TYPES.register("extended_entity",
            () -> AttachmentType.builder(holder -> new ExtendedEntity()).build());

    protected static List<? extends String> forceRemoveEntityIds;
    private static long forceRemoveConfigRefreshTick = Long.MIN_VALUE;
    protected static final int FORCE_REMOVE_ENTITY_TICKS = 40;

    // Entity Instance:
    protected LivingEntity entity;

    // Projectiles:
    protected Map<String, Integer> projectileCooldownsPrimary = new HashMap<>();
    protected Map<String, Integer> projectileCooldownsSecondary = new HashMap<>();

    // Safe Position:
    /** The last coordinates the entity was at where it wasn't inside an opaque block. (Helps prevent suffocation). **/
    Vector3d lastSafePos;
    private boolean playerAllowFlyingSnapshot;
    private boolean playerIsFlyingSnapshot;

    // Last Attacked:
    protected LivingEntity lastAttackedEntity;
    protected int lastAttackedTime = 0;

    // Picked Up:
    protected Entity pickedUpByEntity;

    // Percher
    protected Entity perchedByEntity;
    protected boolean perchedEntityNoclip = false;

    // Force Remove:
    boolean forceRemoveChecked = false;
    boolean forceRemove = false;
    int forceRemoveTicks = FORCE_REMOVE_ENTITY_TICKS;

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    // ==================================================
    //                   Get for Entity
    // ==================================================
    public static ExtendedEntity getForEntity(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        ExtendedEntity extendedEntity = entity.getData(EXTENDED_ENTITY);
        if (extendedEntity.getEntity() != entity) {
            extendedEntity.setEntity(entity);
        }
        return extendedEntity;
    }

    public ExtendedEntity() {
    }

    // ==================================================
    //                      Entity
    // ==================================================
    public void setEntity(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return this.entity;
    }

    public void setLastAttackedEntity(LivingEntity target) {
        this.lastAttackedEntity = target;
        this.lastAttackedTime = this.entity.tickCount;
    }

    public static List<? extends String> getForceRemoveEntityIds() {
        return forceRemoveEntityIds != null ? Collections.unmodifiableList(forceRemoveEntityIds) : Collections.emptyList();
    }

    public LivingEntity getLastAttackedEntity() {
        return this.lastAttackedEntity;
    }

    public int getLastAttackedTime() {
        return this.lastAttackedTime;
    }

    public boolean hasLastAttackedEntity() {
        return this.lastAttackedEntity != null;
    }

    // ==================================================
    //                      Update
    // ==================================================
    public void onUpdate() {
        if (this.entity == null)
            return;

        // Projectiles:
        this.tickProjectileCooldowns(this.projectileCooldownsPrimary);
        this.tickProjectileCooldowns(this.projectileCooldownsSecondary);

        // Force Remove Entity:
        boolean serverSide = !this.entity.getCommandSenderWorld().isClientSide;
        if (serverSide) {
            refreshForceRemoveConfig(this.entity);
        }
        if (serverSide && !ExtendedEntity.getForceRemoveEntityIds().isEmpty() && !this.forceRemoveChecked) {
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(this.entity.getType()).toString();
            for (String forceRemoveID : ExtendedEntity.getForceRemoveEntityIds()) {
                if (forceRemoveID.equalsIgnoreCase(entityId)) {
                    LMHelperClass.logDebug("ForceRemoveEntity", "Forced entity removal: " + this.entity.getName().getString());
                    this.forceRemove = true;
                    break;
                }
            }
            this.forceRemoveChecked = true;
        }
        if (this.forceRemove && this.forceRemoveTicks-- <= 0)
            this.entity.remove(Entity.RemovalReason.DISCARDED);

        this.updateSafePosition();

        // Picked Up By Entity:
        try {
            this.updatePickedUpByEntity();
        } catch (Exception e) {
        }

        // Perched By Entity:
        try {
            this.updatedPerchedByEntity();
        } catch (Exception e) {
        }
    }

    private static void refreshForceRemoveConfig(LivingEntity entity) {
        long gameTime = entity.level().getGameTime();
        if (forceRemoveConfigRefreshTick == gameTime) {
            return;
        }
        forceRemoveEntityIds = ConfigAdmin.INSTANCE.forceRemoveEntityIds.get();
        forceRemoveConfigRefreshTick = gameTime;
    }

    private void tickProjectileCooldowns(Map<String, Integer> cooldowns) {
        if (cooldowns.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
            int cooldownValue = entry.getValue();
            if (cooldownValue > 0) {
                entry.setValue(cooldownValue - 1);
            }
        }
    }

    private void updateSafePosition() {
        int safePositionUpdateInterval = 5;
        if (this.lastSafePos == null) {
            this.lastSafePos = new Vector3d(this.entity.position().x(), this.entity.position().y(), this.entity.position().z());
        }
        if (this.entity.tickCount % safePositionUpdateInterval != 0) {
            return;
        }
        if (!this.entity.getCommandSenderWorld().getBlockState(this.entity.blockPosition()).isSolid()) {
            this.lastSafePos.set(Math.floor(this.entity.position().x()) + 0.5D, this.entity.blockPosition().getY(), Math.floor(this.entity.position().z()) + 0.5D);
        }
    }

    // ==================================================
    //                       Death
    // ==================================================
    public void onDeath() {
        this.setPickedUpByEntity(null);
    }

    // ==================================================
    //                 Picked Up By Entity
    // ==================================================
    public void updatePickedUpByEntity() {
        if (this.pickedUpByEntity == null)
            return;

        // Check:
        if (!this.entity.getCommandSenderWorld().isClientSide) {
            if (!this.pickedUpByEntity.isAlive()) {
                this.setPickedUpByEntity(null);
                return;
            }
            if (this.pickedUpByEntity instanceof LivingEntity livingPickedUpBy && livingPickedUpBy.getHealth() <= 0) {
                this.setPickedUpByEntity(null);
                return;
            }
            if (hasEffect(this.entity, "weight")) {
                this.setPickedUpByEntity(null);
                return;
            }
            if (this.entity.distanceTo(this.pickedUpByEntity) > 32D) {
                this.setPickedUpByEntity(null);
                return;
            }
        }

        // Movement:
        if (this.pickedUpByEntity != null) {
            double[] pickupOffset = this.getPickedUpOffset();
            this.entity.setPos(this.pickedUpByEntity.position().x() + pickupOffset[0], this.pickedUpByEntity.position().y() + pickupOffset[1], this.pickedUpByEntity.position().z() + pickupOffset[2]);
            this.entity.setDeltaMovement(this.pickedUpByEntity.getDeltaMovement());
            this.entity.fallDistance = 0;
            if (!this.entity.getCommandSenderWorld().isClientSide && this.entity instanceof Player player) {
                player.getAbilities().mayfly = true;
                this.entity.noPhysics = true;
            }
        }
    }

    public void setPickedUpByEntity(Entity pickedUpByEntity) {
        if (this.pickedUpByEntity == pickedUpByEntity || this.entity == null) {
            return;
        }

        this.pickedUpByEntity = pickedUpByEntity;

        // Server Side:
        if (!this.entity.getCommandSenderWorld().isClientSide) {

            // Player Flying:
            if (this.entity instanceof Player player) {
                if (pickedUpByEntity != null) {
                    this.playerAllowFlyingSnapshot = player.getAbilities().mayfly;
                    this.playerIsFlyingSnapshot = player.getAbilities().flying;
                } else {
                    player.getAbilities().mayfly = this.playerAllowFlyingSnapshot;
                    player.getAbilities().flying = this.playerIsFlyingSnapshot;
                    this.entity.noPhysics = false;
                }
                player.onUpdateAbilities();
            }

            // Teleport To Initial Pickup Position:
            if (this.pickedUpByEntity != null && !(this.entity instanceof Player)) {
                double[] pickupOffset = this.getPickedUpOffset();
                this.entity.teleportTo(this.pickedUpByEntity.position().x() + pickupOffset[0], this.pickedUpByEntity.position().y() + pickupOffset[1], this.pickedUpByEntity.position().z() + pickupOffset[2]);
            }

            LycanitesMobs.PACKET_MANAGER.sendToWorld(new MessageEntityPickedUp(this.entity, pickedUpByEntity), this.entity.getCommandSenderWorld());
        }

        // Safe Drop Position:
        if (pickedUpByEntity == null) {
            if (this.lastSafePos != null) {
                this.entity.setPos(this.lastSafePos.x(), this.lastSafePos.y(), this.lastSafePos.z());
            }
            this.entity.setDeltaMovement(0, 0, 0);
            this.entity.fallDistance = 0;
        }
    }

    public double[] getPickedUpOffset() {
        double[] pickupOffset = new double[]{0, 0, 0};
        if (this.pickedUpByEntity instanceof BaseCreatureEntity pickupCreature) {
            pickupOffset = pickupCreature.getPickupOffset(this.entity);
        }
        if (CreatureManager.getInstance().getConfig().disablePickupOffsets() && this.entity instanceof Player) {
            return new double[]{0, 0, 0};
        }
        return pickupOffset;
    }

    public boolean isPickedUp() {
        return this.pickedUpByEntity != null;
    }

    public Entity getPickedUpByEntity() {
        return this.pickedUpByEntity;
    }

    public boolean isFeared() {
        return this.entity != null && hasEffect(this.entity, "fear");
    }

    /** True if the entity has the named Lycanites effect (1.21: effects are checked through registry holders). **/
    private static boolean hasEffect(LivingEntity entity, String effectName) {
        Holder<MobEffect> holder = ObjectManager.getEffectHolder(effectName);
        return holder != null && entity.hasEffect(holder);
    }

    // ==================================================
    //                     Perched
    // ==================================================
    /**
     * Sets the entity that is perching on this entity.
     *
     * @param perchedByEntity The entity to perch on this entity or null to clear.
     */
    public void setPerchedByEntity(Entity perchedByEntity) {
        if (this.perchedByEntity != null) {
            this.perchedByEntity.noPhysics = this.perchedEntityNoclip;
            if (this.perchedByEntity instanceof BaseCreatureEntity perchedCreature) {
                perchedCreature.setPerchTarget(null);
            }
        }

        this.perchedByEntity = perchedByEntity;
        if (perchedByEntity != null) {
            this.perchedEntityNoclip = perchedByEntity.noPhysics;
            perchedByEntity.noPhysics = true;
            if (perchedByEntity instanceof BaseCreatureEntity perchedCreature) {
                perchedCreature.setPerchTarget(this.entity);
            }
        }

        if (!this.entity.getCommandSenderWorld().isClientSide) {
            LycanitesMobs.PACKET_MANAGER.sendToWorld(new MessageEntityPerched(this.entity, this.perchedByEntity), this.entity.getCommandSenderWorld());
        }
    }

    public Entity getPerchedByEntity() {
        return this.perchedByEntity;
    }

    /**
     * Returns the xyz position that an entity should perch on this entity at.
     */
    public Vector3d getPerchPosition() {
        double entityWidth = this.entity.getDimensions(this.entity.getPose()).width();
        double entityHeight = this.entity.getDimensions(this.entity.getPose()).height();

        // Official quirk kept: degrees-to-radians then + 90 (radians).
        double angle = Math.toRadians(this.entity.getYRot()) + 90;
        double xPerchPos = this.entity.position().x();
        double zPerchPos = this.entity.position().z();
        double distance = entityWidth * 0.7D;
        if (distance != 0) {
            xPerchPos += distance * -Math.sin(angle);
            zPerchPos += distance * Math.cos(angle);
        }

        return new Vector3d(xPerchPos, this.entity.position().y() + entityHeight * 0.78D, zPerchPos);
    }

    /**
     * Updates the position of an entity that is perching on this entity.
     */
    public void updatedPerchedByEntity() {
        Entity perchedByEntity = this.getPerchedByEntity();
        if (perchedByEntity != null) {
            Vector3d perchPosition = this.getPerchPosition();
            perchedByEntity.setPos(perchPosition.x(), perchPosition.y(), perchPosition.z());
            perchedByEntity.setDeltaMovement(this.entity.getDeltaMovement());
            perchedByEntity.setYRot(this.entity.getYRot());
            perchedByEntity.noPhysics = true;
        }
    }

    // ==================================================
    //                     Projectiles
    // ==================================================
    /**
     * Returns the projectile firing cooldown of the provided type (1 primary, 2 secondary) and projectile name.
     */
    public int getProjectileCooldown(int type, String projectileName) {
        if (type == 1) {
            return this.projectileCooldownsPrimary.getOrDefault(projectileName, 0);
        }
        return this.projectileCooldownsSecondary.getOrDefault(projectileName, 0);
    }

    /**
     * Set the projectile firing cooldown of the provided type (1 primary, 2 secondary) and projectile name.
     */
    public void setProjectileCooldown(int type, String projectileName, int cooldown) {
        if (type == 1) {
            this.projectileCooldownsPrimary.put(projectileName, cooldown);
        } else if (type == 2) {
            this.projectileCooldownsSecondary.put(projectileName, cooldown);
        }
    }

    // ==================================================
    //                       Remove
    // ==================================================
    public void onEntityRemoved() {
        // Official: dropped the client map entry. Attachments live on the entity, so nothing to clean up.
    }
}
