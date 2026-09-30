package com.lycanitesmobs.client.manager;

import com.lycanitesmobs.client.model.item.ItemObjModel;
import com.lycanitesmobs.client.model.item.ModelEquipmentPart;
import com.lycanitesmobs.core.item.equipment.ItemEquipmentPart;
import com.lycanitesmobs.client.model.creature.base.CreatureObjModel;
import com.lycanitesmobs.client.model.creature.base.CreatureObjModelOld;
import com.lycanitesmobs.client.model.creature.base.CreatureModel;
import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.data.info.creature.Subspecies;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Trimmed - projectile models (ProjectileManager/ProjectileInfo/ProjectileObjModel) and equipment
 * part models (EquipmentPartManager/ItemEquipmentPart/ModelEquipmentPart/EquipmentModel) aren't
 * ported yet, so all of that has been dropped.
 *
 * createModels() is also made resilient per-creature (try/catch + log + skip) rather than the
 * original's throw-on-first-failure, since most of the 123 creatures don't have a ported model
 * class yet and one missing model shouldn't block every other creature from getting one.
 */
public class ModelManager {
    private static ModelManager INSTANCE;

    public static ModelManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ModelManager();
        }
        return INSTANCE;
    }

    public Map<CreatureInfo, CreatureModel> creatureModels = new HashMap<>();
    public Map<Subspecies, CreatureModel> creatureSubspeciesModels = new HashMap<>();
    public Map<ItemEquipmentPart, ItemObjModel> equipmentPartModels = new HashMap<>();

    public void reloadModels(ResourceManager resourceManager) {
        LMHelperClass.logDebug("Resources", "ModelManager.reloadModels: begin");
        LMHelperClass.logDebug(
                "Resources",
                "ModelManager.reloadModels: base=" + this.creatureModels.size()
                        + " subspecies=" + this.creatureSubspeciesModels.size()
        );

        for (CreatureModel model : this.creatureModels.values()) {
            if (model instanceof CreatureObjModel obj) {
                LMHelperClass.logDebug("Resources", "ModelManager.reloadModels: reloading base model " + obj.getClass().getName());
                obj.reloadModel(resourceManager);
            } else if (model instanceof CreatureObjModelOld old) {
                old.reloadModel(resourceManager);
            }
        }

        for (CreatureModel model : this.creatureSubspeciesModels.values()) {
            if (model instanceof CreatureObjModel obj) {
                LMHelperClass.logDebug("Resources", "ModelManager.reloadModels: reloading subspecies model " + obj.getClass().getName());
                obj.reloadModel(resourceManager);
            } else if (model instanceof CreatureObjModelOld old) {
                old.reloadModel(resourceManager);
            }
        }

        for (ItemObjModel model : this.equipmentPartModels.values()) {
            model.reloadModel(resourceManager);
        }

        for (com.lycanitesmobs.client.model.projectile.base.ProjectileModel model : this.projectileModels.values()) {
            if (model instanceof com.lycanitesmobs.client.model.projectile.base.ProjectileObjModel obj) {
                obj.reloadModel(resourceManager);
            }
        }
        LMHelperClass.logDebug("Resources", "ModelManager.reloadModels: end");
    }

    /**
     * Creates all models to be used. Missing/broken model classes are logged and skipped instead
     * of aborting the whole load, since most creatures don't have a ported model class yet.
     */
    public void createModels() {
        // Projectile Models:
        for (com.lycanitesmobs.core.data.info.projectile.ProjectileInfo projectileInfo : com.lycanitesmobs.core.manager.ProjectileManager.getInstance().getProjectiles()) {
            if (projectileInfo.getModelClassName() == null || this.projectileModels.containsKey(projectileInfo)) {
                continue;
            }
            try {
                this.projectileModels.put(projectileInfo, (com.lycanitesmobs.client.model.projectile.base.ProjectileModel) Class.forName(projectileInfo.getModelClassName()).getConstructor().newInstance());
            } catch (Exception e) {
                LMHelperClass.logWarningMessage("Unable to load Projectile model " + projectileInfo.getModelClassName() + " for " + projectileInfo.getName() + ", falling back to the sprite renderer.");
            }
        }

        for (CreatureInfo creatureInfo : CreatureManager.getInstance().getCreatures()) {
            if (creatureInfo.isDummy()) {
                continue;
            }
            if (creatureInfo.getModelClassName() == null) {
                continue;
            }
            try {
                this.creatureModels.put(creatureInfo, (CreatureModel) Class.forName(creatureInfo.getModelClassName()).getConstructor().newInstance());
            } catch (Exception e) {
                LMHelperClass.logWarningMessage("Unable to load Creature model " + creatureInfo.getModelClassName() + " for " + creatureInfo.getName() + ", falling back to placeholder renderer.");
            }
            for (Subspecies subspecies : creatureInfo.getSubspeciesEntries()) {
                if (subspecies.getModelClassName() == null) {
                    continue;
                }
                try {
                    this.creatureSubspeciesModels.put(subspecies, (CreatureModel) Class.forName(subspecies.getModelClassName()).getConstructor().newInstance());
                } catch (Exception e) {
                    LMHelperClass.logWarningMessage("Unable to load Subspecies model " + subspecies.getModelClassName() + " for " + creatureInfo.getName() + ", falling back to placeholder renderer.");
                }
            }
        }
    }

    public Map<com.lycanitesmobs.core.data.info.projectile.ProjectileInfo, com.lycanitesmobs.client.model.projectile.base.ProjectileModel> projectileModels = new HashMap<>();

    /**
     * Gets the model used by the provided Projectile, or null if it has none (it renders as a sprite).
     */
    @Nullable
    public com.lycanitesmobs.client.model.projectile.base.ProjectileModel getProjectileModel(com.lycanitesmobs.core.data.info.projectile.ProjectileInfo projectileInfo) {
        return this.projectileModels.get(projectileInfo);
    }

    /**
     * Gets the model used by the provided Creature and Subspecies.
     *
     * @param creatureInfo The creature info to get the model for.
     * @param subspecies   The creature's subspecies, can be null for default model.
     * @return The Creature Model.
     */
    public CreatureModel getCreatureModel(CreatureInfo creatureInfo, @Nullable Subspecies subspecies) {
        if (subspecies != null && this.creatureSubspeciesModels.containsKey(subspecies)) {
            return this.creatureSubspeciesModels.get(subspecies);
        }
        if (this.creatureModels.containsKey(creatureInfo)) {
            return this.creatureModels.get(creatureInfo);
        }
        return null;
    }

    /**
     * Removes per-entity animation state once a client entity is no longer in the level.
     */
    public void removeEntityModelState(Entity entity) {
        if (entity == null) {
            return;
        }
        for (CreatureModel model : this.creatureModels.values()) {
            removeEntityModelState(model, entity);
        }
        for (CreatureModel model : this.creatureSubspeciesModels.values()) {
            removeEntityModelState(model, entity);
        }
    }

    private static void removeEntityModelState(CreatureModel model, Entity entity) {
        if (model instanceof CreatureObjModel objModel) {
            objModel.removeModelState(entity);
        }
    }

    /**
     * Gets the OBJ model for an equipment part item, created on first use (by then the client resources are loaded;
     * resource reloads refresh them in reloadModels()).
     */
    public ItemObjModel getEquipmentPartModel(ItemEquipmentPart equipmentPart) {
        return this.equipmentPartModels.computeIfAbsent(equipmentPart, ModelEquipmentPart::new);
    }

    public int getLoadedModelCount() {
        return creatureModels.size();
    }
}
