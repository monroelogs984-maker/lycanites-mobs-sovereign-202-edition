package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - dropped MoveVillageGoal/BreakDoorGoal (not ported) and the random-lunge aiStep
 * (there's no generic leap() hook on BaseCreatureEntity). Kept vanilla door-opening navigation
 * config (harmless, no Lycanites dependency) and the "Rudolph" custom-name texture override
 * (self-contained, uses already-ported getTexture()/getTextureName()/AssetHelper). Dropped the
 * MobType.UNDEFINED attribute assignment and getNoBagSize/getBagSize (bag subsystem not ported).
 */
public class EntityJabberwock extends TameableCreatureEntity implements Enemy {

    public EntityJabberwock(EntityType<? extends EntityJabberwock> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.canGrow = false;
        this.babySpawnChance = 0.01D;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));

        if (this.getNavigation() instanceof GroundPathNavigation pathNavigateGround) {
            pathNavigateGround.setCanOpenDoors(true);
        }
    }

    /**
     * Returns this creature's main texture. Also checks for subspecies.
     **/
    public ResourceLocation getTexture() {
        if (!this.hasCustomName() || !"Rudolph".equals(this.getCustomName().getString()))
            return super.getTexture();

        String textureName = this.getTextureName() + "_rudolph";
        return AssetHelper.entityTexture(textureName);
    }
}
