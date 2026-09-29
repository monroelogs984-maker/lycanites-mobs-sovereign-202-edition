package com.lycanitesmobs.core.entity.spawner;

import com.lycanitesmobs.core.entity.spawner.trigger.MobEventSpawnTrigger;
import com.lycanitesmobs.core.capabilities.level.ExtendedWorld;
import com.lycanitesmobs.core.entity.spawner.trigger.BlockSpawnTrigger;
import com.lycanitesmobs.core.entity.spawner.trigger.ChunkSpawnTrigger;
import com.lycanitesmobs.core.entity.spawner.trigger.EntitySpawnedSpawnTrigger;
import com.lycanitesmobs.core.entity.spawner.trigger.ExplosionSpawnTrigger;
import com.lycanitesmobs.core.entity.spawner.trigger.FishingSpawnTrigger;
import com.lycanitesmobs.core.entity.spawner.trigger.KillSpawnTrigger;
import com.lycanitesmobs.core.entity.spawner.trigger.MixBlockSpawnTrigger;
import com.lycanitesmobs.core.entity.spawner.trigger.PlayerSpawnTrigger;
import com.lycanitesmobs.core.entity.spawner.trigger.SleepSpawnTrigger;
import com.lycanitesmobs.core.entity.spawner.trigger.SpawnTrigger;
import com.lycanitesmobs.core.entity.spawner.trigger.WorldSpawnTrigger;
import com.lycanitesmobs.core.event.SpawnerEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime owner for JSON spawner trigger registration and dispatch state.
 */
public class SpawnerTriggerDispatcher {
    private static SpawnerTriggerDispatcher INSTANCE;
    private static final int MAX_CHUNKS_PER_TICK = 4;
    private static final int MAX_CHUNK_PROBES_PER_TICK = 64;

    private final List<WorldSpawnTrigger> worldSpawnTriggers = new ArrayList<>();
    private final List<PlayerSpawnTrigger> playerSpawnTriggers = new ArrayList<>();
    private final List<KillSpawnTrigger> killSpawnTriggers = new ArrayList<>();
    private final List<EntitySpawnedSpawnTrigger> entitySpawnedSpawnTriggers = new ArrayList<>();
    private final List<ChunkSpawnTrigger> chunkSpawnTriggers = new ArrayList<>();
    private final List<BlockSpawnTrigger> blockSpawnTriggers = new ArrayList<>();
    private final List<SleepSpawnTrigger> sleepSpawnTriggers = new ArrayList<>();
    private final List<FishingSpawnTrigger> fishingSpawnTriggers = new ArrayList<>();
    private final List<ExplosionSpawnTrigger> explosionSpawnTriggers = new ArrayList<>();
    private final List<MobEventSpawnTrigger> mobEventSpawnTriggers = new ArrayList<>();
    private final List<MixBlockSpawnTrigger> mixBlockSpawnTriggers = new ArrayList<>();

    private final Map<String, Set<ChunkPos>> freshChunks = new HashMap<>();
    private final Map<Player, Long> playerUpdateTicks = new HashMap<>();

    public static SpawnerTriggerDispatcher getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SpawnerTriggerDispatcher();
        }
        return INSTANCE;
    }

    public boolean addTrigger(SpawnTrigger spawnTrigger) {
        if (spawnTrigger instanceof WorldSpawnTrigger worldTrigger && !this.worldSpawnTriggers.contains(spawnTrigger)) {
            this.worldSpawnTriggers.add(worldTrigger);
            return true;
        }
        if (spawnTrigger instanceof PlayerSpawnTrigger playerTrigger && !this.playerSpawnTriggers.contains(spawnTrigger)) {
            this.playerSpawnTriggers.add(playerTrigger);
            return true;
        }
        if (spawnTrigger instanceof KillSpawnTrigger killTrigger && !this.killSpawnTriggers.contains(spawnTrigger)) {
            this.killSpawnTriggers.add(killTrigger);
            return true;
        }
        if (spawnTrigger instanceof EntitySpawnedSpawnTrigger entityTrigger && !this.entitySpawnedSpawnTriggers.contains(spawnTrigger)) {
            this.entitySpawnedSpawnTriggers.add(entityTrigger);
            return true;
        }
        if (spawnTrigger instanceof ChunkSpawnTrigger chunkTrigger && !this.chunkSpawnTriggers.contains(spawnTrigger)) {
            this.chunkSpawnTriggers.add(chunkTrigger);
            return true;
        }
        if (spawnTrigger instanceof MixBlockSpawnTrigger mixTrigger && !this.mixBlockSpawnTriggers.contains(spawnTrigger)) {
            this.mixBlockSpawnTriggers.add(mixTrigger);
            return true;
        }
        if (spawnTrigger instanceof SleepSpawnTrigger sleepTrigger && !this.sleepSpawnTriggers.contains(spawnTrigger)) {
            this.sleepSpawnTriggers.add(sleepTrigger);
            return true;
        }
        if (spawnTrigger instanceof BlockSpawnTrigger blockTrigger && !this.blockSpawnTriggers.contains(spawnTrigger)) {
            this.blockSpawnTriggers.add(blockTrigger);
            return true;
        }
        if (spawnTrigger instanceof FishingSpawnTrigger fishingTrigger && !this.fishingSpawnTriggers.contains(spawnTrigger)) {
            this.fishingSpawnTriggers.add(fishingTrigger);
            return true;
        }
        if (spawnTrigger instanceof ExplosionSpawnTrigger explosionTrigger && !this.explosionSpawnTriggers.contains(spawnTrigger)) {
            this.explosionSpawnTriggers.add(explosionTrigger);
            return true;
        }
        if (spawnTrigger instanceof MobEventSpawnTrigger mobEventTrigger && !this.mobEventSpawnTriggers.contains(spawnTrigger)) {
            this.mobEventSpawnTriggers.add(mobEventTrigger);
            return true;
        }
        return false;
    }

    public void removeTrigger(SpawnTrigger spawnTrigger) {
        this.worldSpawnTriggers.remove(spawnTrigger);
        this.playerSpawnTriggers.remove(spawnTrigger);
        this.killSpawnTriggers.remove(spawnTrigger);
        this.entitySpawnedSpawnTriggers.remove(spawnTrigger);
        this.chunkSpawnTriggers.remove(spawnTrigger);
        this.blockSpawnTriggers.remove(spawnTrigger);
        this.sleepSpawnTriggers.remove(spawnTrigger);
        this.fishingSpawnTriggers.remove(spawnTrigger);
        this.explosionSpawnTriggers.remove(spawnTrigger);
        this.mobEventSpawnTriggers.remove(spawnTrigger);
        this.mixBlockSpawnTriggers.remove(spawnTrigger);
    }

    public void onWorldTick(Level world) {
        if (world.isClientSide) {
            return;
        }
        if (this.worldSpawnTriggers.isEmpty() && this.chunkSpawnTriggers.isEmpty() && this.mobEventSpawnTriggers.isEmpty()) {
            return;
        }
        ExtendedWorld worldExt = ExtendedWorld.getForWorld(world);
        if (worldExt == null) {
            return;
        }
        if (!worldExt.markSpawnerTickIfFresh(world.getGameTime())) {
            return;
        }
        long spawnerTick = worldExt.getLastSpawnerTime();

        if (!this.worldSpawnTriggers.isEmpty()) {
            List<BlockPos> triggerPositions = new ArrayList<>();
            for (Player player : world.players()) {
                if (triggerPositions.isEmpty()) {
                    triggerPositions.add(player.blockPosition());
                    continue;
                }

                boolean nearOtherPlayers = false;
                for (BlockPos triggerPosition : triggerPositions) {
                    if (player.distanceToSqr(Vec3.atLowerCornerOf(triggerPosition)) <= 100 * 100) {
                        nearOtherPlayers = true;
                        break;
                    }
                }
                if (!nearOtherPlayers) {
                    triggerPositions.add(player.blockPosition());
                }
            }

            for (BlockPos triggerPosition : triggerPositions) {
                for (WorldSpawnTrigger spawnTrigger : this.worldSpawnTriggers) {
                    spawnTrigger.onTick(world, triggerPosition, spawnerTick);
                }
            }
        }
        if (!this.chunkSpawnTriggers.isEmpty()) {
            this.checkFreshChunks(world);
        }

        if (!this.mobEventSpawnTriggers.isEmpty() && worldExt.getWorldEvent() != null) {
            for (MobEventSpawnTrigger spawnTrigger : this.mobEventSpawnTriggers) {
                spawnTrigger.onTick(world, worldExt.getServerWorldEventPlayer());
            }
        }

    }

    public void onPlayerTick(Player player) {
        if (this.playerSpawnTriggers.isEmpty()) {
            return;
        }
        long entityUpdateTick = this.playerUpdateTicks.getOrDefault(player, 0L);
        int tickOffset = 0;
        for (PlayerSpawnTrigger spawnTrigger : this.playerSpawnTriggers) {
            spawnTrigger.onTick(player, entityUpdateTick - tickOffset);
            tickOffset += 105;
        }
        this.playerUpdateTicks.put(player, entityUpdateTick + 1);
    }

    public void onPlayerLoggedOut(Player player) {
        this.playerUpdateTicks.remove(player);
        for (WorldSpawnTrigger spawnTrigger : this.worldSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
        for (PlayerSpawnTrigger spawnTrigger : this.playerSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
        for (KillSpawnTrigger spawnTrigger : this.killSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
        for (EntitySpawnedSpawnTrigger spawnTrigger : this.entitySpawnedSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
        for (ChunkSpawnTrigger spawnTrigger : this.chunkSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
        for (BlockSpawnTrigger spawnTrigger : this.blockSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
        for (SleepSpawnTrigger spawnTrigger : this.sleepSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
        for (FishingSpawnTrigger spawnTrigger : this.fishingSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
        for (ExplosionSpawnTrigger spawnTrigger : this.explosionSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
        for (MobEventSpawnTrigger spawnTrigger : this.mobEventSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
        for (MixBlockSpawnTrigger spawnTrigger : this.mixBlockSpawnTriggers) {
            spawnTrigger.getSpawner().clearTriggerCount(player);
        }
    }

    public void onEntityDeath(LivingEntity killedEntity, Entity killerEntity) {
        if (this.killSpawnTriggers.isEmpty()) {
            return;
        }
        if (!(killerEntity instanceof Player player)) {
            return;
        }
        for (KillSpawnTrigger spawnTrigger : this.killSpawnTriggers) {
            spawnTrigger.onKill(player, killedEntity);
        }
    }

    public void onEntitySpawned(LivingEntity spawnedEntity) {
        if (this.entitySpawnedSpawnTriggers.isEmpty()) {
            return;
        }
        for (EntitySpawnedSpawnTrigger spawnTrigger : this.entitySpawnedSpawnTriggers) {
            spawnTrigger.onEntitySpawned(spawnedEntity);
        }
    }

    public void onChunkGenerate(String dimensionId, ChunkPos chunkPos) {
        if (dimensionId == null || chunkPos == null) {
            return;
        }
        Set<ChunkPos> chunks = this.getFreshChunkSet(dimensionId);
        synchronized (chunks) {
            chunks.add(chunkPos);
        }
    }

    public void onHarvestDrops(Level world, Player player, BlockPos blockPos, BlockState blockState, ItemStack tool) {
        if (this.blockSpawnTriggers.isEmpty()) {
            return;
        }
        if (player != null && !SpawnerEventListener.shouldTestCreative() && player.getAbilities().instabuild) {
            return;
        }

        int bonusLevel = 0;
        int silkLevel = 0;
        if (tool != null && !tool.isEmpty()) {
            // 1.21: enchantments are a datapack registry, so resolve the holders from the level.
            var enchantments = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            bonusLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.FORTUNE), tool);
            silkLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.SILK_TOUCH), tool);
        }

        for (BlockSpawnTrigger spawnTrigger : this.blockSpawnTriggers) {
            spawnTrigger.onBlockHarvest(world, player, blockPos, blockState, 0, bonusLevel, silkLevel > 0);
        }
    }

    public void onBlockBreak(Level world, BlockPos blockPos, BlockState blockState, Player player, int chain) {
        if (this.blockSpawnTriggers.isEmpty()) {
            return;
        }
        if (player != null && !SpawnerEventListener.shouldTestCreative() && player.getAbilities().instabuild) {
            return;
        }

        for (BlockSpawnTrigger spawnTrigger : this.blockSpawnTriggers) {
            spawnTrigger.onBlockBreak(world, player, blockPos, blockState, chain);
        }
    }

    public void onBlockPlace(Level world, BlockPos blockPos, BlockState blockState, Player player, int chain) {
        if (this.blockSpawnTriggers.isEmpty()) {
            return;
        }
        if (player != null && !SpawnerEventListener.shouldTestCreative() && player.getAbilities().instabuild) {
            return;
        }

        for (BlockSpawnTrigger spawnTrigger : this.blockSpawnTriggers) {
            spawnTrigger.onBlockPlace(world, player, blockPos, blockState, chain);
        }
    }

    public boolean onSleep(Level world, Player player, BlockPos spawnPos) {
        if (this.sleepSpawnTriggers.isEmpty()) {
            return false;
        }
        boolean interrupted = false;
        for (SleepSpawnTrigger spawnTrigger : this.sleepSpawnTriggers) {
            if (spawnTrigger.onSleep(world, player, spawnPos, world.getBlockState(player.blockPosition()))) {
                interrupted = true;
            }
        }
        return interrupted;
    }

    public void onFished(Level world, Player player, Entity hookEntity) {
        if (this.fishingSpawnTriggers.isEmpty()) {
            return;
        }
        if (!SpawnerEventListener.shouldTestCreative() && player.getAbilities().instabuild) {
            return;
        }
        for (FishingSpawnTrigger spawnTrigger : this.fishingSpawnTriggers) {
            spawnTrigger.onFished(world, player, hookEntity);
        }
    }

    public void onExplosion(Level world, Player player, Explosion explosion) {
        if (this.explosionSpawnTriggers.isEmpty()) {
            return;
        }
        if (player != null && !SpawnerEventListener.shouldTestCreative() && player.getAbilities().instabuild) {
            return;
        }
        for (ExplosionSpawnTrigger spawnTrigger : this.explosionSpawnTriggers) {
            spawnTrigger.onExplosion(world, player, explosion);
        }
    }

    public void onLavaMix(Level world, BlockState originalState, BlockPos liquidPos) {
        if (this.mixBlockSpawnTriggers.isEmpty()) {
            return;
        }
        if (originalState.getBlock() != Blocks.LAVA) {
            return;
        }
        for (MixBlockSpawnTrigger spawnTrigger : this.mixBlockSpawnTriggers) {
            spawnTrigger.onMix(world, originalState, liquidPos);
        }
    }

    private void checkFreshChunks(Level world) {
        String dimensionId = world.dimension().location().toString();
        Set<ChunkPos> chunks = this.getFreshChunkSet(dimensionId);
        List<ChunkPos> chunksToProcess = new ArrayList<>(MAX_CHUNKS_PER_TICK);

        synchronized (chunks) {
            if (chunks.isEmpty()) {
                return;
            }
            if (chunks.size() > 1000) {
                chunks.clear();
                return;
            }

            List<ChunkPos> deferredChunks = new ArrayList<>();
            Iterator<ChunkPos> iterator = chunks.iterator();
            int probes = 0;
            while (iterator.hasNext() && probes < MAX_CHUNK_PROBES_PER_TICK) {
                ChunkPos chunkPos = iterator.next();
                if (chunkPos == null) {
                    iterator.remove();
                    continue;
                }
                if (chunksToProcess.size() >= MAX_CHUNKS_PER_TICK) {
                    break;
                }
                probes++;
                if (!this.isChunkReadyForSpawnProbe(world, chunkPos)) {
                    iterator.remove();
                    deferredChunks.add(chunkPos);
                    continue;
                }
                iterator.remove();
                chunksToProcess.add(chunkPos);
            }
            chunks.addAll(deferredChunks);
        }

        for (ChunkPos chunkPos : chunksToProcess) {
            for (ChunkSpawnTrigger spawnTrigger : this.chunkSpawnTriggers) {
                spawnTrigger.onChunkPopulate(world, chunkPos);
            }
        }
    }

    private Set<ChunkPos> getFreshChunkSet(String dimensionId) {
        synchronized (this.freshChunks) {
            return this.freshChunks.computeIfAbsent(dimensionId, key -> new LinkedHashSet<>());
        }
    }

    private boolean isChunkReadyForSpawnProbe(Level world, ChunkPos chunkPos) {
        if (world instanceof ServerLevel serverLevel) {
            return serverLevel.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z) != null;
        }
        return world.getChunkSource().hasChunk(chunkPos.x, chunkPos.z);
    }
}
