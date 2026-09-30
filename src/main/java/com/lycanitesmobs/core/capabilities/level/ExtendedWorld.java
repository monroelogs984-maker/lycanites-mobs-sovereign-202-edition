package com.lycanitesmobs.core.capabilities.level;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.capabilities.util.BossEntry;
import com.lycanitesmobs.core.event.mobevent.MobEvent;
import com.lycanitesmobs.core.event.mobevent.MobEventPlayerServer;
import com.lycanitesmobs.core.manager.MobEventManager;
import com.lycanitesmobs.core.network.message.MessageMobEvent;
import com.lycanitesmobs.core.network.message.MessageWorldEvent;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-level Lycanites data (the official 1.20.1 class is a SavedData too, this keeps that).
 *
 * Port scope: spawner tick bookkeeping, world day base time, boss tracking (boss block protection + spawner
 * boss-proximity checks) and the mob event runtime (world event + per-area mob event players, saved world event,
 * client sync). Dungeon instances are not ported: they belonged to the legacy dungeon generator, which is disabled
 * upstream; dungeons are structures now (see LMDungeonStructure).
 */
public class ExtendedWorld extends SavedData {
    protected static final String EXT_PROP_NAME = "LycanitesMobs";
    protected static final Map<Level, ExtendedWorld> loadedExtWorlds = new HashMap<>();

    protected Level world;
    protected boolean initialized = false;
    protected long lastSpawnerTime = 0;
    protected long lastEventScheduleTime = 0;
    protected long lastEventUpdateTime = 0;

    // Mob Events World Config:
    protected boolean useTotalWorldTime = true;

    // Mob Events:
    protected Map<String, MobEventPlayerServer> serverMobEventPlayers = new HashMap<>();
    protected MobEventPlayerServer serverWorldEventPlayer = null;
    long worldEventStartTargetTime = 0;
    long worldEventLastStartedTime = 0;
    String worldEventName = "";
    int worldEventCount = -1;

    // Entities:
    protected Map<UUID, BossEntry> bosses = new HashMap<>();


    public static void register() {
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> unloadWorld(event.getLevel()));
    }

    // ==================================================
    //                   Get for World
    // ==================================================
    public static ExtendedWorld getForWorld(Level world) {
        if (world == null) {
            return null;
        }

        ExtendedWorld worldExt = loadedExtWorlds.get(world);
        if (worldExt != null) {
            return worldExt;
        }

        // Server Side:
        if (world instanceof ServerLevel serverWorld) {
            worldExt = serverWorld.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(ExtendedWorld::new, ExtendedWorld::loadFromTag), EXT_PROP_NAME);
        }

        // Client Side: (Only used as a per world object instance for running events, etc.)
        else {
            worldExt = new ExtendedWorld();
        }

        worldExt.world = world;
        worldExt.init();
        loadedExtWorlds.put(world, worldExt);
        return worldExt;
    }

    public static void unloadWorld(LevelAccessor world) {
        if (world instanceof Level level) {
            loadedExtWorlds.remove(level);
        }
    }

    private static ExtendedWorld loadFromTag(CompoundTag tag, HolderLookup.Provider registries) {
        ExtendedWorld worldExt = new ExtendedWorld();
        worldExt.load(tag);
        return worldExt;
    }


    // ==================================================
    //                     Constructor
    // ==================================================
    public ExtendedWorld() {
        super();
    }

    public Level getWorld() {
        return this.world;
    }


    // ==================================================
    //                        Init
    // ==================================================
    public void init() {
        if (this.initialized) {
            return;
        }
        this.initialized = true;

        // Initial Tick Times:
        this.lastSpawnerTime = this.world.getGameTime() - 1;
        this.lastEventScheduleTime = this.world.getGameTime() - 1;
        this.lastEventUpdateTime = this.world.getGameTime() - 1;

        this.restoreSavedWorldEvent();
    }

    private void restoreSavedWorldEvent() {
        if (this.world.isClientSide || "".equals(this.worldEventName) || this.hasServerWorldEventPlayer()) {
            return;
        }

        long savedLastStartedTime = this.worldEventLastStartedTime;
        this.startMobEvent(this.worldEventName, null, new BlockPos(0, 0, 0), 1, -1);
        MobEventPlayerServer worldEventPlayer = this.getServerWorldEventPlayer();
        if (worldEventPlayer != null) {
            worldEventPlayer.changeStartedWorldTime(savedLastStartedTime);
        }
    }


    // ==================================================
    //                    Get Properties
    // ==================================================
    public long getWorldEventStartTargetTime() {
        return this.worldEventStartTargetTime;
    }

    public long getWorldEventLastStartedTime() {
        return this.worldEventLastStartedTime;
    }

    public String getWorldEventName() {
        return this.worldEventName;
    }

    public MobEvent getWorldEvent() {
        if (this.getWorldEventName() == null || "".equals(this.getWorldEventName())) {
            return null;
        }
        return MobEventManager.getInstance().getMobEvent(this.getWorldEventName());
    }

    public int getWorldEventCount() {
        return this.worldEventCount;
    }

    public long getConfiguredDayBaseTime(Level world) {
        return this.useTotalWorldTime ? world.getGameTime() : world.getDayTime();
    }

    public boolean hasServerWorldEventPlayer() {
        return this.serverWorldEventPlayer != null;
    }

    public MobEventPlayerServer getServerWorldEventPlayer() {
        return this.serverWorldEventPlayer;
    }

    public MobEventPlayerServer[] getServerMobEventPlayerSnapshot() {
        return this.serverMobEventPlayers.values().toArray(new MobEventPlayerServer[0]);
    }

    /**
     * Returns a Mob Event Server Player if an event by the provided event name is currently active, otherwise null.
     **/
    public MobEventPlayerServer getMobEventPlayerServer(String mobEventName) {
        if (mobEventName == null || "".equals(mobEventName)) {
            return null;
        }
        if (mobEventName.equals(this.getWorldEventName())) {
            return this.getServerWorldEventPlayer();
        }
        return this.serverMobEventPlayers.get(mobEventName);
    }

    public boolean markSpawnerTickIfFresh(long gameTime) {
        if (this.lastSpawnerTime == gameTime) {
            return false;
        }
        this.lastSpawnerTime = gameTime;
        return true;
    }

    public long getLastSpawnerTime() {
        return this.lastSpawnerTime;
    }

    public boolean markEventScheduleTickIfFresh(long gameTime) {
        if (this.lastEventScheduleTime == gameTime) {
            return false;
        }
        this.lastEventScheduleTime = gameTime;
        return true;
    }

    public long getLastEventScheduleTime() {
        return this.lastEventScheduleTime;
    }

    public boolean markEventUpdateTickIfFresh(long gameTime) {
        if (this.lastEventUpdateTime == gameTime) {
            return false;
        }
        this.lastEventUpdateTime = gameTime;
        return true;
    }


    // ==================================================
    //                    Set Properties
    // ==================================================
    public void setWorldEventStartTargetTime(long setLong) {
        if (this.worldEventStartTargetTime != setLong) {
            this.setDirty();
        }
        this.worldEventStartTargetTime = setLong;
        if (setLong > 0) {
            LMHelperClass.logDebug("MobEvents", "Next random mob will start after " + ((this.worldEventStartTargetTime - this.world.getGameTime()) / 20) + "secs.");
        }
    }

    public void setWorldEventLastStartedTime(long setLong) {
        if (this.worldEventLastStartedTime != setLong) {
            this.setDirty();
        }
        this.worldEventLastStartedTime = setLong;
    }

    public void setWorldEventName(String setString) {
        if (!this.worldEventName.equals(setString)) {
            this.setDirty();
        }
        this.worldEventName = setString;
    }

    public void increaseMobEventCount() {
        this.worldEventCount++;
        this.setDirty();
    }


    // ==================================================
    //                Random Event Delay
    // ==================================================
    /**
     * Gets a random time until the next random event will start.
     **/
    public int getRandomEventDelay(RandomSource random) {
        int min = Math.max(200, MobEventManager.getInstance().getMinTicksUntilEvent());
        int max = Math.max(200, MobEventManager.getInstance().getMaxTicksUntilEvent());
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min);
    }


    // ==================================================
    //                     World Event
    // ==================================================
    /**
     * Starts the provided Mob Event on the provided world.
     **/
    public void startWorldEvent(MobEvent mobEvent) {
        if (mobEvent == null) {
            LMHelperClass.logWarningMessage("Tried to start a null world event, stopping any event instead.");
            this.stopWorldEvent();
            return;
        }

        boolean extended = false;
        MobEventPlayerServer worldEventPlayer = this.getServerWorldEventPlayer();
        if (worldEventPlayer != null) {
            extended = worldEventPlayer.getMobEvent() == mobEvent;
        }
        if (!extended) {
            worldEventPlayer = mobEvent.getServerEventPlayer(this.world);
            this.serverWorldEventPlayer = worldEventPlayer;
        }
        worldEventPlayer.setExtended(extended);

        this.setWorldEventName(mobEvent.getName());
        this.increaseMobEventCount();
        this.setWorldEventStartTargetTime(0);
        this.setWorldEventLastStartedTime(this.world.getGameTime());
        worldEventPlayer.onStart();
        this.updateAllClientsEvents();
    }

    /**
     * Stops the World Event.
     **/
    public void stopWorldEvent() {
        MobEventPlayerServer worldEventPlayer = this.getServerWorldEventPlayer();
        if (worldEventPlayer != null) {
            worldEventPlayer.onFinish();
            this.setWorldEventName("");
            this.serverWorldEventPlayer = null;
            this.updateAllClientsEvents();
        }
    }


    // ==================================================
    //                     Mob Events
    // ==================================================
    /**
     * Starts a provided Mob Event (provided by INSTANCE) on the provided world.
     **/
    public void startMobEvent(MobEvent mobEvent, Player player, BlockPos pos, int level, int variant) {
        if (mobEvent == null) {
            LMHelperClass.logWarningMessage("Tried to start a null mob event.");
            return;
        }

        MobEventPlayerServer mobEventPlayerServer = mobEvent.getServerEventPlayer(this.world);
        this.serverMobEventPlayers.put(mobEvent.getName(), mobEventPlayerServer);
        mobEventPlayerServer.configure(player, pos, level, variant);
        mobEventPlayerServer.onStart();
        this.updateAllClientsEvents();
    }

    /**
     * Starts a provided Mob Event (provided by name) on the provided world.
     **/
    public MobEvent startMobEvent(String mobEventName, Player player, BlockPos pos, int level, int variant) {
        MobEvent mobEvent = MobEventManager.getInstance().getMobEvent(mobEventName);
        if (mobEvent == null) {
            LMHelperClass.logWarningMessage("Tried to start a mob event with the invalid name: '" + mobEventName + "' on " + (this.world.isClientSide ? "Client" : "Server"));
            return null;
        }
        if (!mobEvent.isEnabled()) {
            LMHelperClass.logWarningMessage("Tried to start a mob event that was disabled with the name: '" + mobEventName + "' on " + (this.world.isClientSide ? "Client" : "Server"));
            return null;
        }

        mobEvent.trigger(this.world, player, pos, level, variant);
        return mobEvent;
    }

    /**
     * Stops a Mob Event.
     **/
    public void stopMobEvent(String mobEventName) {
        MobEventPlayerServer mobEventPlayerServer = this.serverMobEventPlayers.remove(mobEventName);
        if (mobEventPlayerServer != null) {
            mobEventPlayerServer.onFinish();
            this.updateAllClientsEvents();
        }
    }


    // ==================================================
    //                  Update Clients
    // ==================================================
    /**
     * Sends a packet to all clients updating their events for the provided world.
     **/
    public void updateAllClientsEvents() {
        MobEventPlayerServer worldEventPlayer = this.getServerWorldEventPlayer();
        BlockPos pos = worldEventPlayer != null ? worldEventPlayer.getOrigin() : new BlockPos(0, 0, 0);
        int level = worldEventPlayer != null ? worldEventPlayer.getLevel() : 0;
        int subspecies = worldEventPlayer != null ? worldEventPlayer.getVariant() : -1;
        LycanitesMobs.PACKET_MANAGER.sendToWorld(new MessageWorldEvent(this.getWorldEventName(), pos, level, subspecies), this.world);
        for (MobEventPlayerServer mobEventPlayerServer : this.serverMobEventPlayers.values()) {
            LycanitesMobs.PACKET_MANAGER.sendToWorld(new MessageMobEvent(
                    mobEventPlayerServer.getMobEventName(),
                    mobEventPlayerServer.getOrigin(),
                    mobEventPlayerServer.getLevel(),
                    mobEventPlayerServer.getVariant()
            ), this.world);
        }
    }


    // ==================================================
    //                     Entities
    // ==================================================

    /**
     * Called by bosses to let this world know that they are active, this will add them to the boss list if they are not already in it.
     **/
    public void bossUpdate(Entity entity) {
        if (entity.isAlive()) {
            this.bosses.computeIfAbsent(entity.getUUID(), ignored -> new BossEntry()).update(entity);
        }
    }

    /**
     * Overrides the boss nearby range for the provided entity.
     **/
    public void overrideBossRange(Entity entity, int rangeOverride) {
        BossEntry bossEntry = this.bosses.get(entity.getUUID());
        if (bossEntry != null) {
            bossEntry.overrideNearbyRange(rangeOverride);
        }
    }

    /**
     * Called by bosses to let this world know that they are being removed.
     **/
    public void bossRemoved(Entity entity) {
        this.bosses.remove(entity.getUUID());
    }

    /**
     * Returns true if a boss is nearby.
     *
     * @param pos The position to search around.
     * @return True if a boss is present.
     */
    public boolean isBossNearby(Vec3 pos) {
        for (BossEntry bossEntry : this.getBossEntries()) {
            if (bossEntry != null && bossEntry.isNear(pos)) {
                return true;
            }
        }
        return false;
    }

    Collection<BossEntry> getBossEntries() {
        return this.bosses.values();
    }


    // ==================================================
    //                    Read From NBT
    // ==================================================
    public void load(CompoundTag nbtTagCompound) {
        // The official reads these longs with getInt(); getLong() here (they are written as longs).
        if (nbtTagCompound.contains("WorldEventStartTargetTime")) {
            this.worldEventStartTargetTime = nbtTagCompound.getLong("WorldEventStartTargetTime");
        }
        if (nbtTagCompound.contains("WorldEventLastStartedTime")) {
            this.worldEventLastStartedTime = nbtTagCompound.getLong("WorldEventLastStartedTime");
        }
        if (nbtTagCompound.contains("WorldEventName")) {
            this.worldEventName = nbtTagCompound.getString("WorldEventName");
        }
        if (nbtTagCompound.contains("WorldEventCount")) {
            this.worldEventCount = nbtTagCompound.getInt("WorldEventCount");
        }
    }


    // ==================================================
    //                    Write To NBT
    // ==================================================
    @Override
    public CompoundTag save(CompoundTag nbtTagCompound, HolderLookup.Provider registries) {
        nbtTagCompound.putLong("WorldEventStartTargetTime", this.worldEventStartTargetTime);
        nbtTagCompound.putLong("WorldEventLastStartedTime", this.worldEventLastStartedTime);
        nbtTagCompound.putString("WorldEventName", this.worldEventName);
        nbtTagCompound.putInt("WorldEventCount", this.worldEventCount);
        return nbtTagCompound;
    }

    public long getWorldSeed() {
        return this.world.getServer().getWorldData().worldGenOptions().seed();
    }
}
