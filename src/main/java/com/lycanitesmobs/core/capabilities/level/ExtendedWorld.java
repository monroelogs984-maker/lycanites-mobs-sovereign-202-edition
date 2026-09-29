package com.lycanitesmobs.core.capabilities.level;

import com.lycanitesmobs.core.capabilities.util.BossEntry;
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
 * Port scope (Phase 6b, spawners): spawner tick bookkeeping, world day base time, boss tracking (boss block
 * protection + spawner boss-proximity checks) and the saved world-event name/count fields. The mob event runtime
 * (MobEventPlayerServer, starting/stopping events, client sync) and dungeon instances are NOT ported yet - they come
 * with the mob event and dungeon phases. Until then there is never an active world event: getWorldEvent() is always
 * null and getMobEventPlayerServer() always returns null, so event-gated spawners stay off.
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

    // Mob Events (saved state only, see class comment):
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

        // TODO(port): restoreSavedWorldEvent() - restarts the saved world event (mob event phase).
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

    /**
     * The active world mob event. Always null until mob events are ported (the official returns
     * MobEventManager.getMobEvent(worldEventName)), so this returns a plain Object for now.
     */
    public Object getWorldEvent() {
        return null;
    }

    public int getWorldEventCount() {
        return this.worldEventCount;
    }

    public long getConfiguredDayBaseTime(Level world) {
        return this.useTotalWorldTime ? world.getGameTime() : world.getDayTime();
    }

    /**
     * The running mob event with this name. Always null until mob events are ported (official type:
     * MobEventPlayerServer).
     */
    public Object getMobEventPlayerServer(String mobEventName) {
        return null;
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
        // TODO(port): dungeon instances (dungeon phase).
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
