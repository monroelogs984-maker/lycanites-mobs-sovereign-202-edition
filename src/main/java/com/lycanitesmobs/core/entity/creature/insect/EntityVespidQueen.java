package com.lycanitesmobs.core.entity.creature.insect;

import com.lycanitesmobs.core.data.info.creature.CreatureType;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.creature.beast.EntityConba;
import com.lycanitesmobs.core.entity.goals.actions.StayByHomeGoal;
import com.lycanitesmobs.core.entity.util.CreatureStructure;
import com.lycanitesmobs.core.item.consumable.entity.CreatureTreatItem;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import com.lycanitesmobs.core.manager.DungeonManager;
import com.lycanitesmobs.core.util.ContextUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Difficulty;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.base.AgeableCreatureEntity;
import com.lycanitesmobs.core.entity.goals.actions.AttackMeleeGoal;
import com.lycanitesmobs.core.entity.goals.targeting.FindAttackTargetGoal;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Heavily trimmed: the original extends TameableCreatureEntity (not ported) and its whole
 * identity is the hive/dungeon-structure system (DungeonManager/CreatureStructure building a
 * vespid_hive theme, StayByHomeGoal, home-position tracking, ally-spawning worker vespids,
 * cross-reference to EntityConba's infection mechanic, and taming via CreatureTreatItem). None
 * of that (tame system, dungeon structures, ally spawning) is ported, so this is reduced to a
 * bigger, tougher flying melee attacker with no hive/home/taming behavior at all - see
 * EntityVespid.java for the same trim on the worker side.
 * PHASE 5e UPDATE (2026-09-26): re-parented to its official superclass now that TameableCreatureEntity is
 * ported (taming/ownership/pet behaviour work; RideableCreatureEntity is still a stub). Any wording above
 * about extending Base/AgeableCreatureEntity or taming being unported is outdated.
 */
public class EntityVespidQueen extends TameableCreatureEntity implements Enemy {
    // Fields restored from official (2026-09-28 method audit):
    private final CreatureStructure creatureStructure;
    protected int swarmLimit = 10;


    public EntityVespidQueen(EntityType<? extends EntityVespidQueen> entityType, Level world) {
        super(entityType, world);
        this.hasAttackSound = true;
        this.canGrow = true;
        this.babySpawnChance = 0D;
        this.setAttackCooldownMax(10);
        this.solidCollision = true;
        this.setupMob();

        this.creatureStructure = new CreatureStructure(this, DungeonManager.getInstance().getTheme("vespid_hive"));
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(this.claimCombatGoalIndex(), new AttackMeleeGoal(this).setLongMemory(true));
        this.goalSelector.addGoal(this.claimTravelGoalIndex(), new StayByHomeGoal(this));

        this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(this.getType()));
        EntityType conbaType = CreatureManager.getInstance().getEntityType("conba");
        if (conbaType != null)
            this.targetSelector.addGoal(this.claimFindTargetGoalIndex(), new FindAttackTargetGoal(this).addTargets(conbaType));
    }

    @Override
    public boolean canAttackOwnSpecies() {
        return true;
    }

    @Override
    public boolean isPersistant() {
        if (this.hasHome() && this.getCommandSenderWorld().getDifficulty() != Difficulty.PEACEFUL)
            return true;
        return super.isPersistant();
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (source.is(DamageTypes.IN_WALL)) return true;
        return super.isInvulnerableTo(source);
    }


    // ==================================================
    //   Restored from official 2026-09-28 (method audit)
    // ==================================================
    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getCommandSenderWorld().isClientSide()) {
            return;
        }

        if (this.updateTick > 0 && this.updateTick % 20 == 0) {
            // Hive Structure:
            if (!this.hasHome()) {
                this.creatureStructure.setOrigin(this.blockPosition());
            }
            boolean structureStarted = this.creatureStructure.isPhaseComplete(0);
            if (!structureStarted || this.updateTick % 200 == 0) {
                this.creatureStructure.refreshBuildTasks();
            }
            if (structureStarted && !this.hasHome()) {
                this.setHome(this.creatureStructure.getOrigin().getX(), this.creatureStructure.getOrigin().getY(), this.creatureStructure.getOrigin().getZ(), 8F);
            }

            // Spawn Babies:
            if (structureStarted && this.creatureStructure.getFinalPhaseBuildTaskSize() <= 10 && this.updateTick % 60 == 0) {
                this.allyUpdate();
            }
        }

        // Don't Keep Infected Conbas Targeted:
        if (this.getTarget() instanceof EntityConba) {
            if (((EntityConba) this.getTarget()).isVespidInfected()) {
                this.setTarget(null);
            }
        }
    }

    public void allyUpdate() {
        if (this.getCommandSenderWorld().isClientSide)
            return;

        // Spawn Babies:
        if (this.swarmLimit > 0 && this.nearbyCreatureCount(CreatureManager.getInstance().getCreature("vespid").getEntityType(), 32D) < this.swarmLimit) {
            float random = this.random.nextFloat();
            if (random <= 0.05F) {
                LivingEntity minion = this.spawnAlly(this.position().x() - 2 + (random * 4), this.position().y(), this.position().z() - 2 + (random * 4));
                if (minion instanceof AgeableCreatureEntity) {
                    AgeableCreatureEntity ageableMinion = (AgeableCreatureEntity) minion;
                    ageableMinion.setGrowingAge(ageableMinion.getInitialGrowthTime());
                }
            }
        }
    }

    @Override
    public boolean attackMelee(Entity target, double damageScale) {
        if (!super.attackMelee(target, damageScale))
            return false;

        if (target instanceof EntityConba) {
            ((EntityConba) target).infectWithVespids();
            return true;
        }

        return true;
    }

    @Override
    public boolean canAttack(LivingEntity targetEntity) {
        if (targetEntity instanceof EntityConba)
            if (((EntityConba) targetEntity).isVespidInfected())
                return false;
        if (targetEntity instanceof EntityVespid) {
            if (!((EntityVespid) targetEntity).hasMaster() || ((EntityVespid) targetEntity).getMasterTarget() == this)
                return false;
        }
        return super.canAttack(targetEntity);
    }

    @Override
    public boolean canBeTempted() {
        return true;
    }

    public void completeBuildTask(ContextUtils.CreatureBuildTask creatureBuildTask) {
        this.creatureStructure.completeBuildTask(creatureBuildTask);
    }

    @Override
    public int getBagSize() {
        return this.creatureInfo.getBagSize();
    }

    public ContextUtils.CreatureBuildTask getBuildTaskFor(EntityVespid vespid) {
        return this.creatureStructure.getBuildTask(vespid);
    }

    @Override
    public int getNoBagSize() {
        return 0;
    }

    @Override
    public boolean isTamingItem(ItemStack itemStack) {
        CreatureType creatureType = this.creatureInfo.getCreatureType();
        if (itemStack.isEmpty() || creatureType == null) {
            return false;
        }

        if (itemStack.getItem() instanceof CreatureTreatItem) {
            CreatureTreatItem itemTreat = (CreatureTreatItem) itemStack.getItem();
            if (itemTreat.getCreatureType() == creatureType) {
                return true;
            }
        }

        return super.isTamingItem(itemStack);
    }

    @Override
    public void loadCreatureFlags() {
        this.swarmLimit = this.creatureInfo.getFlag("swarmLimit", this.swarmLimit);
    }

    @Override
    public boolean rollWanderChance() {
        if (this.hasHome()) {
            return false;
        }
        return this.getRandom().nextDouble() <= 0.0008D;
    }

    @Override
    public void setHomePosition(int x, int y, int z) {
        super.setHomePosition(x, y, z);
        this.creatureStructure.setOrigin(new BlockPos(x, y, z));
    }

    public LivingEntity spawnAlly(double x, double y, double z) {
        LivingEntity minion = CreatureManager.getInstance().getCreature("vespid").createEntity(this.getCommandSenderWorld());
        minion.moveTo(x, y, z, this.random.nextFloat() * 360.0F, 0.0F);
        if (minion instanceof BaseCreatureEntity) {
            ((BaseCreatureEntity) minion).applyVariant(this.getVariantIndex());
        }
        DeferredLevelActionManager.spawnEntity(this.getCommandSenderWorld(), this.blockPosition(), null, minion);
        if (this.getTarget() != null)
            minion.setLastHurtByMob(this.getTarget());
        return minion;
    }

    // Restored from official 2026-09-29 (method audit) - hooked in BaseCreatureEntity.hurt().
    @Override
    public float getDamageModifier(DamageSource damageSrc) {
        if (damageSrc.is(DamageTypeTags.IS_FIRE))
            return 2.0F;
        return super.getDamageModifier(damageSrc);
    }
}
