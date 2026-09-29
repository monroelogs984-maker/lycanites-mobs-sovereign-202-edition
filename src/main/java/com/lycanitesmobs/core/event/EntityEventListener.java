package com.lycanitesmobs.core.event;

import com.lycanitesmobs.core.capabilities.entity.ExtendedEntity;
import com.lycanitesmobs.core.data.info.item.ItemConfig;
import com.lycanitesmobs.core.entity.item.CustomItemEntity;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.base.RideableCreatureEntity;
import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The entity parts of the official GameEventListener (the player/boss block parts are in PlayerEventListener):
 * ExtendedEntity updates, forced entity removal, target vetoes, mount/carry damage protection, seasonal treat drops,
 * mob spawner group limits and projectile immunity.
 *
 * Port (NeoForge 1.21.1) event mapping: LivingTickEvent -> EntityTickEvent.Post, EntityConstructing (force removal of
 * non-living entities) -> EntityJoinLevelEvent, LivingHurtEvent -> LivingIncomingDamageEvent (the cancellable one).
 * Not ported: onBucketFill (vanilla BucketPickup already fills the custom fluid buckets from source blocks),
 * onEntityMount (disabled in the official with "|| true") and the equipment left-click hooks (equipment system).
 */
public class EntityEventListener {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EntityEventListener::onEntityUpdate);
        NeoForge.EVENT_BUS.addListener(EntityEventListener::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(EntityEventListener::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, EntityEventListener::onAttackTarget);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, EntityEventListener::onLivingHurt);
        NeoForge.EVENT_BUS.addListener(EntityEventListener::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(EntityEventListener::onCheckSpawn);
        NeoForge.EVENT_BUS.addListener(EntityEventListener::onProjectileImpact);
    }

    public static void onEntityUpdate(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity entity) {
            ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(entity);
            if (extendedEntity != null) {
                extendedEntity.onUpdate();
            }
        }
    }

    /** Force Remove Entity (non-living; living entities are removed by ExtendedEntity after a short delay). **/
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide || entity instanceof LivingEntity || ExtendedEntity.getForceRemoveEntityIds().isEmpty()) {
            return;
        }
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        for (String forceRemoveID : ExtendedEntity.getForceRemoveEntityIds()) {
            if (forceRemoveID.equalsIgnoreCase(entityId)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        ExtendedEntity extendedEntity = ExtendedEntity.getForEntity(event.getEntity());
        if (extendedEntity != null) {
            extendedEntity.onDeath();
        }
    }

    public static void onAttackTarget(LivingChangeTargetEvent event) {
        LivingEntity targetEntity = event.getNewAboutToBeSetTarget();
        if (targetEntity == null) {
            return;
        }

        // Better Invisibility: (official quirk kept - checks whether the *attacker* has invisibility)
        if (!event.getEntity().hasEffect(MobEffects.INVISIBILITY) && targetEntity.isInvisible()) {
            event.setCanceled(true);
            return;
        }

        // Can Be Targeted:
        if (event.getEntity() instanceof Mob && targetEntity instanceof BaseCreatureEntity targetCreature) {
            if (!targetCreature.canBeTargetedBy(event.getEntity())) {
                event.setCanceled(true);
            }
        }
    }

    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        DamageSource damageSource = event.getSource();
        LivingEntity damagedEntity = event.getEntity();
        ExtendedEntity damagedEntityExt = ExtendedEntity.getForEntity(damagedEntity);

        // True Source Extended Entity:
        if (damageSource.getEntity() instanceof LivingEntity attacker) {
            ExtendedEntity attackerExtendedEntity = ExtendedEntity.getForEntity(attacker);
            if (attackerExtendedEntity != null) {
                attackerExtendedEntity.setLastAttackedEntity(damagedEntity);
            }
        }

        // Mounted Protection:
        if (damagedEntity.getVehicle() instanceof RideableCreatureEntity creatureRideable) {
            if (creatureRideable.isBlocking() || damageSource.is(DamageTypes.IN_WALL) || creatureRideable.isInvulnerableTo(damageSource)) {
                event.setCanceled(true);
                return;
            }
        }

        // Carried entities don't suffocate inside their carrier:
        if (damagedEntityExt != null && damagedEntityExt.isPickedUp() && damageSource.is(DamageTypes.IN_WALL)) {
            event.setCanceled(true);
        }
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (ItemConfig.getSeasonalItemDropChance() <= 0) {
            return;
        }
        if (!LMHelperClass.isHalloween() && !LMHelperClass.isYuletide() && !LMHelperClass.isNewYear()) {
            return;
        }
        if (entity instanceof BaseCreatureEntity creature && creature.isMinion()) {
            return;
        }
        if (entity.getRandom().nextFloat() >= ItemConfig.getSeasonalItemDropChance()) {
            return;
        }

        Level world = entity.getCommandSenderWorld();
        Item seasonalItem = null;
        if (LMHelperClass.isHalloween()) {
            seasonalItem = ObjectManager.getItem("halloweentreat");
        } else if (LMHelperClass.isYuletide()) {
            seasonalItem = LMHelperClass.isYuletidePeak() && world.random.nextBoolean() ? ObjectManager.getItem("wintergiftlarge") : ObjectManager.getItem("wintergift");
        }
        if (seasonalItem == null) {
            return;
        }
        CustomItemEntity entityItem = new CustomItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(seasonalItem, 1));
        entityItem.setPickUpDelay(10);
        event.getDrops().add(entityItem);
    }

    /** Vanilla mob spawner blocks respect the Lycanites group limit (16 blocks around the spawner). **/
    public static void onCheckSpawn(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.SPAWNER || !(event.getEntity() instanceof BaseCreatureEntity creature)) {
            return;
        }
        BaseSpawner spawner = event.getSpawner();
        BlockPos originPos = null;
        if (spawner != null && spawner.getOwner() != null) {
            // 1.21 (NeoForge IOwnedSpawner): the owner is either the spawner block entity or a spawner minecart.
            originPos = spawner.getOwner().map(blockEntity -> blockEntity.getBlockPos(), Entity::blockPosition);
        }
        if (originPos == null) {
            originPos = creature.blockPosition();
        }
        if (!creature.checkSpawnGroupLimit(event.getLevel().getLevel(), originPos, 16)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    /**
     * Projectiles from a shooter the creature is invulnerable to stop without damage. Official:
     * ImpactResult.STOP_AT_CURRENT_NO_DAMAGE; NeoForge's event is only cancellable (which lets the projectile fly
     * through), so it is cancelled and the projectile removed.
     **/
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof BaseCreatureEntity targetCreature)) {
            return;
        }
        if (targetCreature.level().isClientSide) {
            return;
        }
        if (event.getProjectile().getOwner() instanceof LivingEntity shooter && targetCreature.isInvulnerableTo(targetCreature.level().damageSources().mobAttack(shooter))) {
            event.setCanceled(true);
            event.getProjectile().discard();
        }
    }
}
