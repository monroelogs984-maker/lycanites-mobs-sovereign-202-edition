package com.lycanitesmobs.core.entity.spawner;

import com.lycanitesmobs.core.data.config.ConfigCreatureSpawning;
import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.manager.SpawnerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * S202 spawn budget (design/SPAWN_BUDGET.md): natural spawning keeps 6-10 (config) wild Lycanites on the surface and
 * 3-5 in caves and water around each player or group of nearby players (two pools, so caves and water never fill the
 * surface's share), instead of every world spawner firing on its own timer. When an area is under budget, one
 * shared weighted pick runs over every creature that any world spawner could place there right now (each spawner's
 * conditions, location rules, the creature's dimension/climate/light), so a creature's chance no longer depends on which
 * spawner it belongs to or that spawner's batch size. The spawners now only decide where a creature goes.
 * Trigger spawners, mob events, dungeons, structures and bosses don't count and aren't limited.
 */
public class SpawnBudget {
    /** One recent budget decision, for /lm spawning stats. **/
    public record Record(long gameTime, String creature, int spawned, String spawner, int countBefore, double millis) {}

    private static final Deque<Record> HISTORY = new ArrayDeque<>();

    /** Natural spawns queued but not in the world yet (spawns are deferred a tick or more), so bursts can't overshoot. **/
    private record Pending(UUID id, net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos, boolean surface, long expires) {}
    private static final List<Pending> PENDING = new ArrayList<>();

    /** Called by Spawner when it queues a natural spawn. **/
    public static void addPending(Level world, UUID id, BlockPos pos) {
        PENDING.add(new Pending(id, world.dimension(), pos, isSurface(world, pos), world.getGameTime() + 100));
    }

    /** Queued natural spawns in the pool near the position that haven't joined the world yet. **/
    private static int countPending(Level world, BlockPos center, boolean surface) {
        long now = world.getGameTime();
        double range = getRange();
        PENDING.removeIf(pending -> pending.expires() < now);
        int count = 0;
        for (Pending pending : PENDING) {
            if (pending.surface() == surface && pending.dimension() == world.dimension() && pending.pos().distSqr(center) <= range * range
                    && world instanceof net.minecraft.server.level.ServerLevel serverLevel && serverLevel.getEntity(pending.id()) == null) {
                count++;
            }
        }
        return count;
    }

    /** True for open-air spots (sky above, no water or lava): the surface pool. Everything else is caves and water. **/
    public static boolean isSurface(Level world, BlockPos pos) {
        return world.canSeeSky(pos) && world.getFluidState(pos).isEmpty();
    }

    /** Naturally spawned creatures in the pool around the position, plus queued natural spawns there. **/
    public static int getCount(Level world, BlockPos center, boolean surface) {
        int count = 0;
        for (BaseCreatureEntity creature : getNaturalCreatures(world, center)) {
            if (isSurface(world, creature.blockPosition()) == surface) {
                count++;
            }
        }
        return count + countPending(world, center, surface);
    }
    private static final int HISTORY_SIZE = 20;
    private static long attemptStart;

    /** A creature that can spawn here right now, with every spawner/biome combination that could place it. **/
    private static class Candidate {
        final String key;
        int weight = 0;
        final List<Option> options = new ArrayList<>();

        Candidate(String key) {
            this.key = key;
        }
    }

    private record Option(Spawner spawner, MobSpawn mobSpawn, List<BlockPos> positions) {}

    public static boolean isEnabled() {
        return ConfigCreatureSpawning.INSTANCE != null && ConfigCreatureSpawning.INSTANCE.spawnBudgetEnabled.get();
    }

    public static int getMin(boolean surface) {
        return surface ? ConfigCreatureSpawning.INSTANCE.spawnBudgetMin.get() : ConfigCreatureSpawning.INSTANCE.spawnBudgetBelowMin.get();
    }

    public static int getMax(boolean surface) {
        int max = surface ? ConfigCreatureSpawning.INSTANCE.spawnBudgetMax.get() : ConfigCreatureSpawning.INSTANCE.spawnBudgetBelowMax.get();
        return Math.max(getMin(surface), max);
    }

    public static double getRange() {
        return ConfigCreatureSpawning.INSTANCE.spawnBudgetRange.get();
    }

    /** Why placements failed since the last reset (for /lm spawning stats and tuning). **/
    private static final Map<String, Integer> FAILURES = new TreeMap<>();

    public static void noteFailure(String reason) {
        FAILURES.merge(reason, 1, Integer::sum);
    }

    public static Map<String, Integer> getFailures() {
        return new TreeMap<>(FAILURES);
    }

    public static void resetFailures() {
        FAILURES.clear();
    }

    /** Body parts that aren't creatures in their own right (they come with their head), so they don't count. **/
    private static boolean isBodyPart(BaseCreatureEntity creature) {
        return "concapedesegment".equals(creature.getCreatureInfo().getName());
    }

    public static List<Record> getHistory() {
        return new ArrayList<>(HISTORY);
    }

    /** The naturally spawned, still wild creatures within the budget range. **/
    public static List<BaseCreatureEntity> getNaturalCreatures(Level world, BlockPos center) {
        double range = getRange();
        AABB area = new AABB(center).inflate(range);
        return world.getEntitiesOfClass(BaseCreatureEntity.class, area, creature -> creature.isAlive() && creature.isNaturalSpawn()
                && !isBodyPart(creature) && !creature.isTamed() && !creature.isMinion() && creature.distanceToSqr(center.getX(), center.getY(), center.getZ()) <= range * range);
    }

    public static boolean hasRoom(Level world, BlockPos center) {
        return getCount(world, center, true) < getMin(true) || getCount(world, center, false) < getMin(false);
    }

    /** Called from the spawner world tick with one position per group of nearby players. **/
    public static void tick(Level world, List<BlockPos> centers, long tick) {
        if (tick % ConfigCreatureSpawning.INSTANCE.spawnBudgetInterval.get() != 0) {
            return;
        }
        for (BlockPos center : centers) {
            attempt(world, center);
        }
    }

    /**
     * Adds one group around the position to a pool that's under budget: the surface first, then caves and water (also
     * when the surface has nothing placeable right now). Returns true if anything spawned.
     */
    public static boolean attempt(Level world, BlockPos center) {
        attemptStart = System.nanoTime();
        blockScansThisCheck = 0;
        for (boolean surface : new boolean[] {true, false}) {
            int count = getCount(world, center, surface);
            if (count < getMin(surface) && attemptPool(world, center, surface, count, getMax(surface) - count)) {
                return true;
            }
        }
        return false;
    }

    private static boolean attemptPool(Level world, BlockPos center, boolean surface, int count, int room) {
        String pool = surface ? "surface" : "caves/water";
        // Gather every creature any world spawner could place in this pool here right now.
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        for (Spawner spawner : SpawnerManager.getInstance().getSpawners()) {
            if (!spawner.isWorldSpawner() || !spawner.isEnabled(world, null) || !spawner.canSpawn(world, null, center)) {
                continue;
            }
            List<BlockPos> positions = new ArrayList<>();
            for (BlockPos pos : getSpawnPositions(world, spawner, center)) {
                if (isSurface(world, pos) == surface) {
                    positions.add(pos);
                }
            }
            if (positions.isEmpty()) {
                noteFailure("no positions: " + spawner.getName());
                continue;
            }
            Map<Biome, List<BlockPos>> byBiome = new LinkedHashMap<>();
            if (spawner.ignoresBiomes()) {
                byBiome.put(null, positions);
            } else {
                for (BlockPos pos : positions) {
                    byBiome.computeIfAbsent(world.getBiomeManager().getBiome(pos).value(), biome -> new ArrayList<>()).add(pos);
                }
            }
            for (Map.Entry<Biome, List<BlockPos>> entry : byBiome.entrySet()) {
                for (MobSpawn mobSpawn : spawner.getViableMobSpawns(world, null, entry.getValue().size(), entry.getKey())) {
                    if (mobSpawn.getWeight() <= 0) {
                        continue;
                    }
                    String key = mobSpawn.hasCreatureInfo() ? mobSpawn.getCreatureInfo().getName() : mobSpawn.getMobId();
                    Candidate candidate = candidates.computeIfAbsent(key, Candidate::new);
                    // One entry per creature: its chance doesn't grow with the number of spawners it belongs to.
                    candidate.weight = Math.max(candidate.weight, mobSpawn.getWeight());
                    candidate.options.add(new Option(spawner, mobSpawn, entry.getValue()));
                }
            }
        }
        if (candidates.isEmpty()) {
            record(world, "(no " + pool + " candidates)", 0, "-", count);
            return false;
        }

        // Weighted pick (common/rare weights only). A creature that can't be placed right now (light, room, limits) is
        // dropped and the pick runs again, so a dark-only creature picked at noon doesn't waste the turn.
        for (int attemptCount = 0; attemptCount < 10 && !candidates.isEmpty(); attemptCount++) {
            Candidate chosen = pick(world, candidates.values());
            if (chosen == null) {
                return false;
            }
            Option option = chosen.options.get(world.random.nextInt(chosen.options.size()));

            // Group size from the creature (packs stay packs), cut to what the budget has room for.
            int groupSize = 1;
            if (option.mobSpawn().hasCreatureInfo()) {
                CreatureInfo creatureInfo = option.mobSpawn().getCreatureInfo();
                int min = Math.max(1, creatureInfo.getCreatureSpawn().getSpawnGroupMin());
                int max = Math.max(min, creatureInfo.getCreatureSpawn().getSpawnGroupMax());
                groupSize = min + world.random.nextInt(max - min + 1);
            }
            groupSize = Math.max(1, Math.min(groupSize, room));

            // Place the group around one random spot from that spawner's positions.
            List<BlockPos> positions = new ArrayList<>(option.positions());
            BlockPos anchor = positions.get(world.random.nextInt(positions.size()));
            positions.sort(Comparator.comparingDouble(pos -> pos.distSqr(anchor)));
            int spawned = 0;
            for (BlockPos pos : positions) {
                if (spawned >= groupSize) {
                    break;
                }
                if (option.spawner().spawnMob(world, option.mobSpawn(), pos, null, true)) {
                    spawned++;
                }
            }
            if (spawned > 0) {
                record(world, chosen.key, spawned, option.spawner().getName() + ", " + pool, count);
                return true;
            }
            candidates.remove(chosen.key);
        }
        record(world, "(nothing placeable, " + pool + ")", 0, "-", count);
        return false;
    }

    /**
     * Block-scanning spawners (lava, fire, ooze, portal, flowers...) read a 65-block cube each, which is far too slow to
     * redo on every budget check (officially each ran every 30-40 s at most). Their positions are cached per area for
     * BLOCK_SCAN_TICKS and reused while the check stays within BLOCK_SCAN_REUSE_RANGE blocks of the scanned spot.
     */
    private static final int BLOCK_SCAN_TICKS = 600;
    private static final int BLOCK_SCAN_REUSE_RANGE = 16;
    private record BlockScan(Spawner spawner, net.minecraft.resources.ResourceKey<Level> dimension, BlockPos center, long expires, List<BlockPos> positions) {}
    private static final List<BlockScan> BLOCK_SCANS = new ArrayList<>();

    /** At most this many block scans run per budget check; other expired scans are reused, and spawners with no scan for
     * the area yet sit the check out, so travelling never pays for all of them at once. **/
    private static final int BLOCK_SCANS_PER_CHECK = 2;
    private static int blockScansThisCheck = 0;

    private static List<BlockPos> getSpawnPositions(Level world, Spawner spawner, BlockPos center) {
        if (!spawner.scansBlocks()) {
            return spawner.getSpawnPositions(world, null, center);
        }
        long now = world.getGameTime();
        // Forget scans long past their expiry (areas nobody is in any more).
        BLOCK_SCANS.removeIf(scan -> scan.expires() + BLOCK_SCAN_TICKS < now);
        BlockScan found = null;
        for (BlockScan scan : BLOCK_SCANS) {
            if (scan.spawner() == spawner && scan.dimension() == world.dimension()
                    && scan.center().distSqr(center) <= BLOCK_SCAN_REUSE_RANGE * BLOCK_SCAN_REUSE_RANGE) {
                found = scan;
                break;
            }
        }
        if (found != null && (found.expires() >= now || blockScansThisCheck >= BLOCK_SCANS_PER_CHECK)) {
            return found.positions();
        }
        if (blockScansThisCheck >= BLOCK_SCANS_PER_CHECK) {
            return List.of(); // New area: this spawner joins in on a later check (all 13 are covered within ~20 s).
        }
        blockScansThisCheck++;
        BLOCK_SCANS.remove(found);
        List<BlockPos> positions = List.copyOf(spawner.getSpawnPositions(world, null, center));
        // Spread the expiry so the block scans don't all redo on the same check.
        BLOCK_SCANS.add(new BlockScan(spawner, world.dimension(), center, now + BLOCK_SCAN_TICKS + world.random.nextInt(BLOCK_SCAN_TICKS / 2), positions));
        return positions;
    }

    private static Candidate pick(Level world, Collection<Candidate> candidates) {
        int totalWeight = 0;
        for (Candidate candidate : candidates) {
            totalWeight += candidate.weight;
        }
        if (totalWeight <= 0) {
            return null;
        }
        int roll = world.random.nextInt(totalWeight);
        for (Candidate candidate : candidates) {
            roll -= candidate.weight;
            if (roll < 0) {
                return candidate;
            }
        }
        return null;
    }

    private static void record(Level world, String creature, int spawned, String spawner, int countBefore) {
        HISTORY.addFirst(new Record(world.getGameTime(), creature, spawned, spawner, countBefore, (System.nanoTime() - attemptStart) / 1e6));
        while (HISTORY.size() > HISTORY_SIZE) {
            HISTORY.removeLast();
        }
    }
}
