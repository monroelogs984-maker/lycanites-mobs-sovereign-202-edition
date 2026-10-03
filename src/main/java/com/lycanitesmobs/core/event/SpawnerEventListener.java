package com.lycanitesmobs.core.event;

import com.lycanitesmobs.core.entity.spawner.SpawnerTriggerDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * NeoForge event adapter for the spawner runtime dispatcher.
 *
 * Port notes (1.21.1): Forge's TickEvent phases became LevelTickEvent.Pre / PlayerTickEvent.Post, the harvest hook
 * (official: a second BreakEvent listener) is NeoForge's BlockDropsEvent, which carries the breaking tool, sleep
 * interruption is CanPlayerSleepEvent.setProblem, the "entity spawned" hook is FinalizeSpawnEvent, and fresh chunks
 * come from ChunkEvent.Load#isNewChunk instead of a no-op world gen feature (ChunkSpawnFeature).
 */
public class SpawnerEventListener {
    private static SpawnerEventListener INSTANCE;
    private static boolean testOnCreative = false;

    public static SpawnerEventListener getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SpawnerEventListener();
        }
        return INSTANCE;
    }

    public static void register() {
        SpawnerEventListener listener = getInstance();
        NeoForge.EVENT_BUS.addListener(listener::onWorldUpdate);
        NeoForge.EVENT_BUS.addListener(listener::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(listener::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(listener::onEntityDeath);
        NeoForge.EVENT_BUS.addListener(listener::onEntitySpawn);
        NeoForge.EVENT_BUS.addListener(listener::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(listener::onHarvestDrops);
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent event) -> listener.onBlockBreak(event));
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> listener.onBlockPlace(event));
        NeoForge.EVENT_BUS.addListener(listener::onSleep);
        NeoForge.EVENT_BUS.addListener(listener::onFished);
        NeoForge.EVENT_BUS.addListener(listener::onExplosion);
        NeoForge.EVENT_BUS.addListener(listener::onLavaMix);
    }

    public static boolean shouldTestCreative() {
        return testOnCreative;
    }

    public static void setTestOnCreative(boolean testOnCreative) {
        SpawnerEventListener.testOnCreative = testOnCreative;
    }

    public void onWorldUpdate(LevelTickEvent.Pre event) {
        SpawnerTriggerDispatcher.getInstance().onWorldTick(event.getLevel());
    }

    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.getCommandSenderWorld() == null || player.getCommandSenderWorld().isClientSide) {
            return;
        }
        SpawnerTriggerDispatcher.getInstance().onPlayerTick(player);
    }

    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SpawnerTriggerDispatcher.getInstance().onPlayerLoggedOut(event.getEntity());
    }

    public void onEntityDeath(LivingDeathEvent event) {
        LivingEntity killedEntity = event.getEntity();
        if (killedEntity == null || killedEntity.getCommandSenderWorld().isClientSide || event.isCanceled()) {
            return;
        }
        SpawnerTriggerDispatcher.getInstance().onEntityDeath(killedEntity, event.getSource().getEntity());
    }

    public void onEntitySpawn(FinalizeSpawnEvent event) {
        LivingEntity spawnedEntity = event.getEntity();
        if (spawnedEntity == null || spawnedEntity.getCommandSenderWorld().isClientSide || event.isCanceled()) {
            return;
        }
        SpawnerTriggerDispatcher.getInstance().onEntitySpawned(spawnedEntity);
    }

    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.isNewChunk() && event.getLevel() instanceof ServerLevel level) {
            this.onChunkGenerate(level.dimension().location().toString(), event.getChunk().getPos());
        }
    }

    public void onChunkGenerate(String dimensionId, ChunkPos chunkPos) {
        SpawnerTriggerDispatcher.getInstance().onChunkGenerate(dimensionId, chunkPos);
    }

    public void onHarvestDrops(BlockDropsEvent event) {
        if (event.isCanceled() || event.getState() == null) {
            return;
        }
        // Only a player's harvest counts, as officially (the official hooked BlockEvent.BreakEvent). BlockDropsEvent also
        // fires for blocks popping off on their own (arena building, explosions, water, trampling), and passing those
        // through turned every broken grass tuft near a boss arena into a Spriggan roll.
        if (!(event.getBreaker() instanceof Player player)) {
            return;
        }
        SpawnerTriggerDispatcher.getInstance().onHarvestDrops(event.getLevel(), player, event.getPos(), event.getState(), event.getTool());
    }

    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getState() == null || !(event.getLevel() instanceof Level world) || event.getLevel().isClientSide() || event.isCanceled()) {
            return;
        }
        this.onBlockBreak(world, event.getPos(), event.getState(), event.getPlayer(), 0);
    }

    public void onBlockBreak(Level world, BlockPos blockPos, BlockState blockState, Player player, int chain) {
        SpawnerTriggerDispatcher.getInstance().onBlockBreak(world, blockPos, blockState, player, chain);
    }

    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getState() == null || !(event.getLevel() instanceof Level world) || event.getLevel().isClientSide() || event.isCanceled()) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            this.onBlockPlace(world, event.getPos(), event.getState(), player, 0);
        }
    }

    public void onBlockPlace(Level world, BlockPos blockPos, BlockState blockState, Player player, int chain) {
        SpawnerTriggerDispatcher.getInstance().onBlockPlace(world, blockPos, blockState, player, chain);
    }

    public void onSleep(CanPlayerSleepEvent event) {
        Player player = event.getEntity();
        // Only interrupt a sleep vanilla would allow (no other problem already set).
        if (player == null || player.getCommandSenderWorld().isClientSide || event.getProblem() != null) {
            return;
        }

        Level world = player.getCommandSenderWorld();
        if (world.isClientSide || world.isDay()) {
            return;
        }

        BlockPos spawnPos = player.blockPosition().offset(0, 0, 1);
        if (SpawnerTriggerDispatcher.getInstance().onSleep(world, player, spawnPos)) {
            event.setProblem(Player.BedSleepingProblem.NOT_SAFE);
        }
    }

    public void onFished(ItemFishedEvent event) {
        Player player = event.getEntity();
        if (player == null || player.getCommandSenderWorld().isClientSide || event.isCanceled()) {
            return;
        }
        SpawnerTriggerDispatcher.getInstance().onFished(player.getCommandSenderWorld(), player, event.getHookEntity());
    }

    public void onExplosion(ExplosionEvent.Detonate event) {
        Explosion explosion = event.getExplosion();
        if (explosion == null || event.getLevel().isClientSide) {
            return;
        }

        Player player = null;
        if (explosion.getDirectSourceEntity() instanceof Player owner) {
            player = owner;
        }

        SpawnerTriggerDispatcher.getInstance().onExplosion(event.getLevel(), player, explosion);
    }

    public void onLavaMix(BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof Level world) || world.isClientSide) {
            return;
        }
        if (event.getOriginalState().getBlock() == Blocks.LAVA
                && event.getNewState().getBlock() == Blocks.OBSIDIAN) {
            SpawnerTriggerDispatcher.getInstance().onLavaMix(world, event.getOriginalState(), event.getLiquidPos());
        }
    }
}
