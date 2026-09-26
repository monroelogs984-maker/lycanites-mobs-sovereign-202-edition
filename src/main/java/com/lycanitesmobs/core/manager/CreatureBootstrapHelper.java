package com.lycanitesmobs.core.manager;

import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.data.info.creature.CreatureSpawnConfig;
import com.lycanitesmobs.core.data.info.creature.Subspecies;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Owns creature startup/bootstrap side effects that are not part of JSON definition parsing.
 */
public final class CreatureBootstrapHelper {
    private CreatureBootstrapHelper() {
    }

    public static void registerEntityTypeSuppliers(Collection<CreatureInfo> creatures) {
        for (CreatureInfo creatureInfo : creatures) {
            EntityType.Builder<?> entityTypeBuilder = createEntityTypeBuilder(creatureInfo);
            ObjectManager.addEntityType(creatureInfo.getName(), entityTypeBuilder);
        }
    }

    public static void bindRegisteredValues(Collection<CreatureInfo> creatures, CreatureSpawnConfig spawnConfig) {
        for (CreatureInfo creatureInfo : creatures) {
            ResourceLocation id = AssetHelper.modResource(creatureInfo.getName());
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (entityType == null || !id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entityType))) {
                continue;
            }
            creatureInfo.bindEntityType((EntityType<? extends LivingEntity>) entityType);
            // NOTE: the original also registered this entity type with EntityFactory here, an
            // additional side-registry for custom client spawn-packet creation. EntityFactory
            // depends on Forge's PlayMessages, which doesn't exist in NeoForge, and isn't ported
            // (see entity/util - deferred). Not needed for basic spawning: the constructor
            // lambda passed into EntityType.Builder.of() below already handles both server and
            // default client-side entity creation.
            initializeCreature(creatureInfo, spawnConfig);
        }
    }

    public static void initializeCreature(CreatureInfo creatureInfo, CreatureSpawnConfig spawnConfig) {
        if (creatureInfo.isDummy()) {
            return;
        }

        registerSounds(creatureInfo, "");
        for (Subspecies subspecies : creatureInfo.getSubspeciesEntries()) {
            registerSubspeciesSounds(creatureInfo, subspecies);
        }

        creatureInfo.getCreatureSpawn().registerVanillaSpawns(creatureInfo, spawnConfig);

        LMHelperClass.logDebug("Creature", "Creature Loaded: " + creatureInfo.getName() + " - " + creatureInfo.getEntityClass() + " (" + creatureInfo.getModInfo().name + ")");
    }

    private static void registerSubspeciesSounds(CreatureInfo creatureInfo, Subspecies subspecies) {
        if (subspecies.getName() != null && !creatureInfo.hasLoadedSubspeciesSkin(subspecies.getName())) {
            registerSounds(creatureInfo, "." + subspecies.getName());
            creatureInfo.markSubspeciesSkinLoaded(subspecies.getName());
        }
    }

    private static void registerSounds(CreatureInfo creatureInfo, String suffix) {
        ObjectManager.addSound(creatureInfo.getName() + suffix + "_say", "entity." + creatureInfo.getName() + suffix + ".say");
        ObjectManager.addSound(creatureInfo.getName() + suffix + "_hurt", "entity." + creatureInfo.getName() + suffix + ".hurt");
        ObjectManager.addSound(creatureInfo.getName() + suffix + "_death", "entity." + creatureInfo.getName() + suffix + ".death");
        ObjectManager.addSound(creatureInfo.getName() + suffix + "_step", "entity." + creatureInfo.getName() + suffix + ".step");
        ObjectManager.addSound(creatureInfo.getName() + suffix + "_attack", "entity." + creatureInfo.getName() + suffix + ".attack");
        ObjectManager.addSound(creatureInfo.getName() + suffix + "_jump", "entity." + creatureInfo.getName() + suffix + ".jump");
        ObjectManager.addSound(creatureInfo.getName() + suffix + "_fly", "entity." + creatureInfo.getName() + suffix + ".fly");
        // NOTE: TameableCreatureEntity not ported yet - dropped from this check for now.
        if (creatureInfo.isSummonable() || creatureInfo.isTameable()) {
            ObjectManager.addSound(creatureInfo.getName() + suffix + "_tame", "entity." + creatureInfo.getName() + suffix + ".tame");
            ObjectManager.addSound(creatureInfo.getName() + suffix + "_beg", "entity." + creatureInfo.getName() + suffix + ".beg");
        }
        if (creatureInfo.isTameable())
            ObjectManager.addSound(creatureInfo.getName() + suffix + "_eat", "entity." + creatureInfo.getName() + suffix + ".eat");
        if (creatureInfo.isMountable())
            ObjectManager.addSound(creatureInfo.getName() + suffix + "_mount", "entity." + creatureInfo.getName() + suffix + ".mount");
        if (creatureInfo.isBoss())
            ObjectManager.addSound(creatureInfo.getName() + suffix + "_phase", "entity." + creatureInfo.getName() + suffix + ".phase");
    }

    @NotNull
    private static EntityType.Builder<?> createEntityTypeBuilder(CreatureInfo creatureInfo) {
        EntityType.Builder<?> entityTypeBuilder = EntityType.Builder.of((entityType, level) -> creatureInfo.createEntity(level), creatureInfo.isPeaceful() ? MobCategory.CREATURE : MobCategory.MONSTER);
        entityTypeBuilder.setTrackingRange(creatureInfo.isBoss() ? 32 : 10);
        entityTypeBuilder.setUpdateInterval(3);
        entityTypeBuilder.setShouldReceiveVelocityUpdates(false);
        entityTypeBuilder.sized((float) creatureInfo.getWidth(), (float) creatureInfo.getHeight());
        return entityTypeBuilder;
    }
}
