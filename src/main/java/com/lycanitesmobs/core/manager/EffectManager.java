package com.lycanitesmobs.core.manager;

import com.lycanitesmobs.core.data.info.creature.CreatureGroup;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;

/**
 * Creates Lycanites' custom mob effects (registered in RegistryEvents.registerEffects()). Ported 2026-09-26.
 *
 * <p>Effect behaviour lives in MobEventListener (as in the official). Still missing: fear's haunting (needs the
 * unported EntityFear); the official constructor also registered this class on the event bus - not needed.
 */
public class EffectManager {

    private static EffectManager INSTANCE;

    private boolean disableNausea = false;

    public static EffectManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new EffectManager();
        }
        return INSTANCE;
    }

    /**
     * Creates every custom effect. Must run during mod construction, before RegisterEvent.
     */
    public void createEffects() {
        ObjectManager.addPotionEffect("paralysis", true, 0xFFFF00, false);
        ObjectManager.addPotionEffect("penetration", true, 0x222222, false);
        ObjectManager.addPotionEffect("recklessness", true, 0xFF0044, false); // TODO Implement (official)
        ObjectManager.addPotionEffect("rage", true, 0xFF4400, false); // TODO Implement (official)
        ObjectManager.addPotionEffect("weight", true, 0x000022, false);
        ObjectManager.addPotionEffect("fear", true, 0x220022, false);
        ObjectManager.addPotionEffect("decay", true, 0x110033, false);
        ObjectManager.addPotionEffect("insomnia", true, 0x002222, false);
        ObjectManager.addPotionEffect("instability", true, 0x004422, false);
        ObjectManager.addPotionEffect("lifeleak", true, 0x0055FF, false);
        ObjectManager.addPotionEffect("bleed", true, 0xFF2222, false);
        ObjectManager.addPotionEffect("plague", true, 0x220066, false);
        ObjectManager.addPotionEffect("aphagia", true, 0xFFDDDD, false);
        ObjectManager.addPotionEffect("smited", true, 0xDDDDFF, false);
        ObjectManager.addPotionEffect("smouldering", true, 0xDD0000, false);

        ObjectManager.addPotionEffect("leech", false, 0x00FF99, true);
        ObjectManager.addPotionEffect("swiftswimming", false, 0x0000FF, true);
        ObjectManager.addPotionEffect("fallresist", false, 0xDDFFFF, true);
        ObjectManager.addPotionEffect("rejuvenation", false, 0x99FFBB, true);
        ObjectManager.addPotionEffect("immunization", false, 0x66FFBB, true);
        ObjectManager.addPotionEffect("cleansed", false, 0x66BBFF, true);
        ObjectManager.addPotionEffect("repulsion", false, 0xBC532E, true);
        ObjectManager.addPotionEffect("heataura", false, 0x996600, true); // TODO Implement (official)
        ObjectManager.addPotionEffect("staticaura", false, 0xFFBB551, true); // TODO Implement (official)
        ObjectManager.addPotionEffect("freezeaura", false, 0x55BBFF, true); // TODO Implement (official)
        ObjectManager.addPotionEffect("envenom", false, 0x44DD66, true); // TODO Implement (official)

        // Effect Sounds:
        ObjectManager.addSound("effect_fear", "effect.fear");
        ObjectManager.addSound("effect_heartbeat", "effect.heartbeat");
    }

    public void setNauseaDisabled(boolean disableNausea) {
        this.disableNausea = disableNausea;
    }

    public boolean isNauseaDisabled() {
        return this.disableNausea;
    }

    /**
     * Determines if the provided entity is considered a boss.
     */
    public boolean isBoss(Entity entity) {
        if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
            return true;
        }
        if (entity instanceof BaseCreatureEntity creature) {
            return creature.isBoss();
        }
        CreatureGroup bossGroup = CreatureManager.getInstance().getCreatureGroup("boss");
        return bossGroup != null && bossGroup.hasEntity(entity);
    }
}
