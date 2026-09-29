package com.lycanitesmobs.core.entity.creature.anthronian;

import com.lycanitesmobs.core.entity.goals.actions.BreakDoorGoal;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Trimmed - BreakDoorGoal dropped (see EntityEttin's note), getFallResistance/equipment/
 * pet-control dropped (not real hooks / not ported). The "Gooderness" custom-name easter-egg
 * texture kept - self-contained, uses already-ported AssetHelper.entityTexture().
 */
public class EntityWildkin extends TameableCreatureEntity implements Enemy {

    public EntityWildkin(EntityType<? extends EntityWildkin> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0.01D;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimDistractionGoalIndex(), new BreakDoorGoal(this));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(false));

        if (this.getNavigation() instanceof GroundPathNavigation) {
            GroundPathNavigation pathNavigateGround = (GroundPathNavigation) this.getNavigation();
            pathNavigateGround.setCanOpenDoors(true);
        }
    }

    @Override
    public double getFallingMod() {
        return 0.8D;
    }

    @Override
    public ResourceLocation getTexture() {
        if (!this.hasCustomName() || !"Gooderness".equals(this.getCustomName().getString()))
            return super.getTexture();

        String textureName = this.getTextureName() + "_gooderness";
        return AssetHelper.entityTexture(textureName);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    @Override
    public int getBagSize() {
        return this.creatureInfo.getBagSize();
    }

    // ==================================================
    //                     Immunities
    // ==================================================
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

    // ==================================================
    //                     Pet Control
    // ==================================================
    public boolean petControlsEnabled() {
        return true;
    }
}
