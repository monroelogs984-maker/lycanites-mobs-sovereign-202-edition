package com.lycanitesmobs.core.entity.pets;

import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * S202 pets rework (design/PETS_REWORK.md): Bond replaces the scrapped creature levels for bound pets and mounts.
 * Bond 1-3 grows from time spent out with the owner plus kills made together (by the pet, or by the owner while the
 * pet is nearby), and is kept through death and respawn. Summoning mastery (Beastiary knowledge rank 3) gives a
 * creature's summoned minions Bond 2.
 */
public class PetBond {
    public static final int MAX_BOND = 3;
    /** Total bond experience needed for Bond 2 and Bond 3 (same steps as equipment part levels: 500, then 625 more). **/
    public static final int BOND_2 = 500;
    public static final int BOND_3 = 1125;
    /** Ticks a pet has to be out (spawned and alive) per bond point: 1 per minute. **/
    public static final int TIME_INTERVAL = 20 * 60;
    /** Bond points per kill made together. **/
    public static final int KILL_POINTS = 5;
    /** How close the pet has to be to its owner for the owner's kills to count. **/
    public static final double ASSIST_RANGE = 16D;
    /** Summoned minions of a mastered creature (knowledge rank 3) fight at this Bond and last this much longer. **/
    public static final int MASTERY_BOND = 2;
    public static final float MASTERY_DURATION_SCALE = 1.5F;

    private static final double[] HEALTH = {1D, 1.25D, 1.5D};
    private static final double[] DAMAGE = {1D, 1.15D, 1.3D};
    private static final double[] DEFENSE = {1D, 1D, 1.1D};

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PetBond::onLivingDeath);
    }

    public static int getBond(int bondExperience) {
        if (bondExperience >= BOND_3) return 3;
        if (bondExperience >= BOND_2) return 2;
        return 1;
    }

    /** Bond experience needed to reach the next Bond from the start of the current one, 0 at max Bond. **/
    public static int getBondStepMax(int bond) {
        return bond == 1 ? BOND_2 : bond == 2 ? BOND_3 - BOND_2 : 0;
    }

    /** Bond experience gathered within the current Bond. **/
    public static int getBondStepProgress(int bondExperience) {
        int bond = getBond(bondExperience);
        return bond == 1 ? bondExperience : bond == 2 ? bondExperience - BOND_2 : 0;
    }

    /** Stat multiplier for the given Bond: health, damage and defense grow, everything else stays. **/
    public static double getMultiplier(int bond, String stat) {
        int index = Math.max(1, Math.min(MAX_BOND, bond)) - 1;
        return switch (stat) {
            case "health" -> HEALTH[index];
            case "damage" -> DAMAGE[index];
            case "defense" -> DEFENSE[index];
            default -> 1D;
        };
    }

    /** Kills worth bond: hostile mobs, wild Lycanites creatures and players (PvP), never the owner's own allies. **/
    private static boolean isBondKill(LivingEntity victim, Player owner) {
        if (victim == owner) {
            return false;
        }
        if (victim instanceof TameableCreatureEntity tameable && tameable.isTamed()) {
            return tameable.getPlayerOwner() != owner;
        }
        return victim instanceof Enemy || victim instanceof BaseCreatureEntity || victim instanceof Player;
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        Entity killer = event.getSource().getEntity();

        // Killed by a bound pet:
        if (killer instanceof TameableCreatureEntity pet && pet.getPetEntry() != null && pet.getPetEntry().usesSpirit()
                && pet.getPlayerOwner() instanceof Player owner && isBondKill(victim, owner)) {
            pet.getPetEntry().addBondExperience(KILL_POINTS);
            return;
        }

        // Killed by the owner with bound pets nearby:
        if (killer instanceof Player owner && isBondKill(victim, owner)) {
            ExtendedPlayer ownerExt = ExtendedPlayer.getForPlayer(owner);
            if (ownerExt == null) {
                return;
            }
            for (PetEntry petEntry : ownerExt.getPetManager().getEntries()) {
                Entity petEntity = petEntry.getEntity();
                if (petEntry.usesSpirit() && petEntity != null && petEntity.isAlive() && petEntity.level() == owner.level()
                        && petEntity.distanceTo(owner) <= ASSIST_RANGE) {
                    petEntry.addBondExperience(KILL_POINTS);
                }
            }
        }
    }
}
