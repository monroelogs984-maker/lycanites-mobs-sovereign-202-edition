package com.lycanitesmobs.core.altar;

import com.lycanitesmobs.core.capabilities.level.ExtendedWorld;
import com.lycanitesmobs.core.data.config.ConfigMobEvent;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.event.mobevent.trigger.AltarMobEventTrigger;
import com.lycanitesmobs.core.manager.ObjectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * S202 boss altars (replaces the official AltarInfo block formations; the 8 rare-variant altars are cut).
 *
 * Each boss has a pedestal (the old soulcube blocks, textures kept) and its own soulkey (the three old soulkeys, by
 * colour). Using the matching key on the pedestal teleports the player straight into that boss's arena, in the
 * dimension its boss event requires, and starts the event there (the event's StructureBuilder builds the arena and
 * spawns the boss, as in the official). When the boss dies, everyone who came through a pedestal is sent back to it.
 *
 * Arena placement: Rahovart - the Nether at the pedestal's coordinates / 8; Asmodeus - the outer End, 1000 blocks out
 * in the pedestal's direction; Amalgalich - the pedestal's own dimension, at least 520 blocks from world spawn (its
 * event needs 500+). If the pedestal is already in a valid spot, the arena is placed 90 blocks ahead of the player so
 * the pedestal isn't swallowed by the arena floor.
 */
public class BossAltar {
    /** The builders put the arena centre this far east (+X) of the event origin. **/
    private static final int BUILDER_X_OFFSET = 20;
    /** How far from the arena centre (towards -Z) the player arrives. **/
    private static final int ARRIVAL_DISTANCE = 20;
    private static final int RETURN_DELAY_TICKS = 10 * 20;
    private static final List<BossAltar> ALTARS = new ArrayList<>();

    public static final BossAltar RAHOVART = register(new BossAltar("rahovart", "RahovartAltar", "soulcubedemonic", "soulkeydiamond", Level.NETHER));
    public static final BossAltar ASMODEUS = register(new BossAltar("asmodeus", "AsmodeusAltar", "soulcubeaberrant", "soulkey", Level.END));
    public static final BossAltar AMALGALICH = register(new BossAltar("amalgalich", "AmalgalichAltar", "soulcubeundead", "soulkeyemerald", null));

    /** Players who came through a pedestal, by player UUID. **/
    private static final Map<UUID, ReturnPoint> RETURN_POINTS = new HashMap<>();
    /** Arena fights whose boss died, counting down to sending everyone home. **/
    private static final List<PendingReturn> PENDING_RETURNS = new ArrayList<>();

    private record ReturnPoint(ResourceKey<Level> dimension, Vec3 pos, float yRot, ResourceKey<Level> arenaDimension, BlockPos arenaCenter, String bossName) {}
    private record PendingReturn(ResourceKey<Level> arenaDimension, BlockPos arenaCenter, int[] ticksLeft) {}

    public final String bossName;
    public final String altarName;
    public final String pedestalBlockName;
    public final String soulkeyName;
    /** The dimension the fight happens in, null for the pedestal's own dimension. **/
    @Nullable
    public final ResourceKey<Level> arenaDimension;

    private BossAltar(String bossName, String altarName, String pedestalBlockName, String soulkeyName, @Nullable ResourceKey<Level> arenaDimension) {
        this.bossName = bossName;
        this.altarName = altarName;
        this.pedestalBlockName = pedestalBlockName;
        this.soulkeyName = soulkeyName;
        this.arenaDimension = arenaDimension;
    }

    private static BossAltar register(BossAltar altar) {
        ALTARS.add(altar);
        return altar;
    }

    public static void registerListeners() {
        NeoForge.EVENT_BUS.addListener(BossAltar::onBossDeath);
        NeoForge.EVENT_BUS.addListener(BossAltar::onServerTick);
    }

    @Nullable
    public static BossAltar forSoulkey(String soulkeyName) {
        for (BossAltar altar : ALTARS) {
            if (altar.soulkeyName.equals(soulkeyName)) {
                return altar;
            }
        }
        return null;
    }

    @Nullable
    public static BossAltar forPedestal(Block block) {
        for (BossAltar altar : ALTARS) {
            if (block == ObjectManager.getBlock(altar.pedestalBlockName)) {
                return altar;
            }
        }
        return null;
    }


    // ==================================================
    //                     Activation
    // ==================================================
    /**
     * Teleports the player into this boss's arena and starts the boss event there.
     * @return True if the fight started (the soulkey should be consumed).
     */
    public boolean activate(ServerPlayer player, ServerLevel pedestalLevel, BlockPos pedestalPos) {
        if (!ConfigMobEvent.INSTANCE.altarsEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.soulkey.disabled"), false);
            return false;
        }
        List<AltarMobEventTrigger> triggers = AltarMobEventTrigger.getTriggers(this.altarName);
        if (triggers.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.soulkey.none"), false);
            return false;
        }

        MinecraftServer server = pedestalLevel.getServer();
        ServerLevel arenaLevel = this.arenaDimension != null ? server.getLevel(this.arenaDimension) : pedestalLevel;
        if (arenaLevel == null) {
            player.displayClientMessage(Component.translatable("message.soulkey.badlocation"), false);
            return false;
        }
        ExtendedWorld arenaExt = ExtendedWorld.getForWorld(arenaLevel);
        if (arenaExt == null || arenaExt.getMobEventPlayerServer(this.bossName) != null) {
            player.displayClientMessage(Component.translatable("message.soulkey.busy"), false);
            return false;
        }

        BlockPos arenaCenter = this.findArenaCenter(player, pedestalLevel, pedestalPos, arenaLevel);
        BlockPos arrival = arenaCenter.offset(0, 0, -ARRIVAL_DISTANCE);

        // Arrival pocket: the arena floor is only built a few seconds into the event, so don't land inside rock.
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                arenaLevel.setBlock(arrival.offset(x, -1, z), Blocks.OBSIDIAN.defaultBlockState(), 2);
                for (int y = 0; y <= 2; y++) {
                    arenaLevel.setBlock(arrival.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }

        ResourceKey<Level> homeDimension = player.level().dimension();
        Vec3 homePos = player.position();
        float homeYRot = player.getYRot();
        player.teleportTo(arenaLevel, arrival.getX() + 0.5D, arrival.getY(), arrival.getZ() + 0.5D, 0F, 0F); // yaw 0 = facing +Z, the arena centre

        BlockPos eventOrigin = arenaCenter.offset(-BUILDER_X_OFFSET, -1, 0);
        for (AltarMobEventTrigger trigger : triggers) {
            if (trigger.onActivate(player, arenaLevel, eventOrigin, -1)) {
                RETURN_POINTS.put(player.getUUID(), new ReturnPoint(homeDimension, homePos, homeYRot, arenaLevel.dimension(), arenaCenter, this.bossName));
                return true;
            }
        }

        // The event refused to start (conditions): send the player back, key not consumed.
        ServerLevel homeLevel = server.getLevel(homeDimension);
        if (homeLevel != null) {
            player.teleportTo(homeLevel, homePos.x, homePos.y, homePos.z, homeYRot, player.getXRot());
        }
        player.displayClientMessage(Component.translatable("message.soulkey.badlocation"), false);
        return false;
    }

    /** Where the arena centre (boss spawn, floor level + 1) goes. **/
    private BlockPos findArenaCenter(ServerPlayer player, ServerLevel pedestalLevel, BlockPos pedestalPos, ServerLevel arenaLevel) {
        double x;
        double z;
        boolean sameLevel = arenaLevel == pedestalLevel;
        Vec3 facing = Vec3.directionFromRotation(0, player.getYRot());
        double aheadX = pedestalPos.getX() + facing.x * 90;
        double aheadZ = pedestalPos.getZ() + facing.z * 90;

        if (this == RAHOVART) {
            if (sameLevel) {
                x = aheadX;
                z = aheadZ;
            } else {
                double scale = pedestalLevel.dimensionType().coordinateScale() / arenaLevel.dimensionType().coordinateScale();
                x = pedestalPos.getX() * scale;
                z = pedestalPos.getZ() * scale;
            }
        } else if (this == ASMODEUS) {
            // The event needs 500+ blocks from the End's centre (the outer islands).
            if (sameLevel && Math.sqrt(aheadX * aheadX + aheadZ * aheadZ) >= 520) {
                x = aheadX;
                z = aheadZ;
            } else {
                double length = Math.sqrt(pedestalPos.getX() * (double) pedestalPos.getX() + pedestalPos.getZ() * (double) pedestalPos.getZ());
                double dirX = length > 1 ? pedestalPos.getX() / length : 1;
                double dirZ = length > 1 ? pedestalPos.getZ() / length : 0;
                x = dirX * 1000;
                z = dirZ * 1000;
            }
        } else {
            // The event needs 500+ blocks from world spawn.
            BlockPos spawn = arenaLevel.getSharedSpawnPos();
            double dx = aheadX - spawn.getX();
            double dz = aheadZ - spawn.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance >= 520) {
                x = aheadX;
                z = aheadZ;
            } else {
                double dirX = distance > 1 ? dx / distance : 1;
                double dirZ = distance > 1 ? dz / distance : 0;
                x = spawn.getX() + dirX * 600;
                z = spawn.getZ() + dirZ * 600;
            }
        }

        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        int y;
        if (arenaLevel.dimension() == Level.NETHER || arenaLevel.dimension() == Level.END) {
            y = 64;
        } else {
            arenaLevel.getChunk(blockX >> 4, blockZ >> 4); // generate it so the heightmap is real
            y = arenaLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ);
        }
        // Same clamp as the builders (floor at least at 5, 40 blocks of headroom).
        int floorY = Math.max(5, Math.min(y, arenaLevel.getMaxBuildHeight() - 41));
        return new BlockPos(blockX, floorY + 1, blockZ);
    }


    // ==================================================
    //                   Return Trip
    // ==================================================
    private static void onBossDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof BaseCreatureEntity creature) || creature.level().isClientSide || !creature.isBoss()) {
            return;
        }
        BlockPos arenaCenter = creature.getArenaCenter();
        if (arenaCenter == null) {
            return;
        }
        String bossName = creature.getCreatureInfo().getName();
        for (ReturnPoint returnPoint : RETURN_POINTS.values()) {
            if (returnPoint.bossName.equals(bossName) && returnPoint.arenaDimension == creature.level().dimension()
                    && returnPoint.arenaCenter.closerThan(arenaCenter, 32)) {
                PENDING_RETURNS.add(new PendingReturn(returnPoint.arenaDimension, returnPoint.arenaCenter, new int[] {RETURN_DELAY_TICKS}));
                for (ServerPlayer player : ((ServerLevel) creature.level()).players()) {
                    ReturnPoint playerReturn = RETURN_POINTS.get(player.getUUID());
                    if (playerReturn != null && playerReturn.arenaCenter.equals(returnPoint.arenaCenter)) {
                        player.displayClientMessage(Component.translatable("message.soulkey.return", RETURN_DELAY_TICKS / 20), false);
                    }
                }
                return;
            }
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING_RETURNS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        Iterator<PendingReturn> iterator = PENDING_RETURNS.iterator();
        while (iterator.hasNext()) {
            PendingReturn pending = iterator.next();
            if (--pending.ticksLeft[0] > 0) {
                continue;
            }
            iterator.remove();
            Iterator<Map.Entry<UUID, ReturnPoint>> returnIterator = RETURN_POINTS.entrySet().iterator();
            while (returnIterator.hasNext()) {
                Map.Entry<UUID, ReturnPoint> entry = returnIterator.next();
                ReturnPoint returnPoint = entry.getValue();
                if (returnPoint.arenaDimension != pending.arenaDimension || !returnPoint.arenaCenter.equals(pending.arenaCenter)) {
                    continue;
                }
                returnIterator.remove();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                ServerLevel homeLevel = server.getLevel(returnPoint.dimension);
                // Only players still in (or near) the arena are sent home; anyone who died or left is already gone.
                if (player == null || homeLevel == null || player.level().dimension() != returnPoint.arenaDimension
                        || !player.blockPosition().closerThan(returnPoint.arenaCenter, 150)) {
                    continue;
                }
                player.teleportTo(homeLevel, returnPoint.pos.x, returnPoint.pos.y, returnPoint.pos.z, returnPoint.yRot, player.getXRot());
            }
        }
    }
}
