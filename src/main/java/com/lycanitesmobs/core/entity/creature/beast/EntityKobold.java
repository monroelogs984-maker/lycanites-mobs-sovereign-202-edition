package com.lycanitesmobs.core.entity.creature.beast;

import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Trimmed - dropped GetItemGoal/GetBlockGoal (not ported - the "theivery"/"griefing" item- and
 * torch-stealing abilities) and the torch-looting aiStep block that went with them. Kept the
 * `theivery` flag wired into vanilla's own canPickupItems() (self-contained) and the
 * health-threshold canAttack()/shouldCreatureGroupRevenge() tweaks (self-contained). Dropped
 * onRemovedFromWorld's bag-drop (bag subsystem not ported), shouldCreatureGroupHunt/Flee (no
 * such hooks in this port's BaseCreatureEntity, only shouldCreatureGroupRevenge), and the
 * MobType.UNDEFINED attribute assignment.
 */
public class EntityKobold extends TameableCreatureEntity implements Enemy {
    protected boolean griefing = true;
    protected boolean theivery = true;

    public EntityKobold(EntityType<? extends EntityKobold> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.spreadFire = false;
        this.canGrow = false;
        this.babySpawnChance = 0.1D;
        this.setupMob();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setTargetClass(Player.class).setLongMemory(false));
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this));
    }

    @Override
    public void loadCreatureFlags() {
        this.griefing = this.creatureInfo.getFlag("griefing", this.griefing);
        this.theivery = this.creatureInfo.getFlag("theivery", this.theivery);
    }

    @Override
    public boolean shouldCreatureGroupRevenge(LivingEntity target) {
        if (target instanceof Player && (target.getHealth() / target.getMaxHealth()) <= 0.5F)
            return true;
        return super.shouldCreatureGroupRevenge(target);
    }

    @Override
    public boolean canAttack(LivingEntity targetEntity) {
        if (!this.isTamed() && (targetEntity.getHealth() / targetEntity.getMaxHealth()) > 0.5F)
            return false;
        return super.canAttack(targetEntity);
    }

    @Override
    public boolean canPickUpLoot() {
        return this.theivery;
    }
}
