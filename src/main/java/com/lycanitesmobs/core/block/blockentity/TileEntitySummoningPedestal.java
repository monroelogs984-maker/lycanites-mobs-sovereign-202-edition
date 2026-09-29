package com.lycanitesmobs.core.block.blockentity;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.block.special.BlockSummoningPedestal;
import com.lycanitesmobs.core.capabilities.entity.ExtendedPlayer;
import com.lycanitesmobs.core.data.config.ConfigExtra;
import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.data.tag.LycanitesItemTags;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.entity.base.TameableCreatureEntity;
import com.lycanitesmobs.core.entity.item.CustomItemEntity;
import com.lycanitesmobs.core.entity.pets.SummonSet;
import com.lycanitesmobs.core.entity.special.PortalEntity;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.manager.ProjectileManager;
import com.lycanitesmobs.core.network.message.MessageSummoningPedestalStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The Summoning Pedestal: keeps its owner's selected summon set summoned around it from a portal above it, burning
 * redstone (the summoning_pedestal_fuel tags) as fuel, limited by portal capacity (summon cost x 100 per minion, 1000 max).
 *
 * Port (NeoForge 1.21.1): a real registered BlockEntityType (the official TileEntityBase reported BlockEntityType.CHEST
 * and swapped the type in getType()); ticked through the block's getTicker; NBT uses the 1.21 registry-aware
 * load/save; stats go to nearby players (within 5 blocks, as the official) with MessageSummoningPedestalStats.
 */
public class TileEntitySummoningPedestal extends BlockEntity implements Container {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LycanitesMobs.MODID);
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TileEntitySummoningPedestal>> TYPE = BLOCK_ENTITY_TYPES.register("summoningpedestal",
            () -> BlockEntityType.Builder.of(TileEntitySummoningPedestal::new, ObjectManager.getBlock("summoningpedestal")).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    protected long updateTick = 0;

    // Summoning Properties:
    protected PortalEntity summoningPortal;
    protected UUID ownerUUID;
    protected SummonSet summonSet;
    protected int summonAmount = 1;

    // Summoning Stats:
    protected int capacityCharge = 100;
    protected int capacity = 0;
    protected int capacityMax = (this.capacityCharge * 10);
    protected int summonProgress = 0;
    protected int summonProgressMax = 3 * 60;

    // Inventory:
    protected String inventoryName = "";
    protected NonNullList<ItemStack> itemStacks = NonNullList.withSize(3, ItemStack.EMPTY);
    protected int summoningFuel = 0;
    protected int summoningFuelMax;
    protected int summoningFuelAmount = 10 * 60 * 20; // 10 minutes per Redstone Dust

    // Summoned Minions:
    protected List<BaseCreatureEntity> minions = new ArrayList<>();
    protected String[] loadMinionIDs; // Temporary array for initially populating from NBT data in update.

    // Block:
    protected boolean blockStateSet = false;

    public TileEntitySummoningPedestal(BlockPos pos, BlockState state) {
        super(TYPE.get(), pos, state);
        this.summoningFuelMax = ConfigExtra.INSTANCE.summoningPedestalRedstoneTime.get();
        this.summoningFuelAmount = this.summoningFuelMax;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntitySummoningPedestal pedestal) {
        pedestal.tick();
    }

    /**
     * The main update called every tick.
     **/
    public void tick() {
        // Client Side Only:
        if (this.getLevel().isClientSide) {
            // Summoning Progress Animation:
            if (this.summoningFuel > 0) {
                if (this.summonProgress >= this.summonProgressMax)
                    this.summonProgress = 0;
                else if (this.summonProgress > 0)
                    this.summonProgress++;
            }
            return;
        }

        // Load Minion IDs:
        if (this.loadMinionIDs != null) {
            int range = 20;
            List<BaseCreatureEntity> nearbyEntities = this.getLevel().getEntitiesOfClass(BaseCreatureEntity.class,
                    new AABB(this.getBlockPos().getX() - range, this.getBlockPos().getY() - range, this.getBlockPos().getZ() - range,
                            this.getBlockPos().getX() + range, this.getBlockPos().getY() + range, this.getBlockPos().getZ() + range));
            for (BaseCreatureEntity possibleEntity : nearbyEntities) {
                for (String loadMinionID : this.loadMinionIDs) {
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(loadMinionID);
                    } catch (Exception e) {
                    }
                    if (possibleEntity.getUUID().equals(uuid)) {
                        this.registerMinion(possibleEntity);
                        break;
                    }
                }
            }
            this.loadMinionIDs = null;
        }

        // Summoning:
        if (this.summonSet != null && this.summonSet.getCreatureInfo() != null) {
            if (this.summonSet.getFollowing()) {
                this.summonSet.setFollowing(false);
            }

            // Summoning Portal:
            if (this.summoningPortal == null || !this.summoningPortal.isAlive()) {
                this.requestSummoningPortal();
            }

            // Update Minions:
            if (this.updateTick % 100 == 0) {
                this.capacity = 0;
                Iterator<BaseCreatureEntity> minionIterator = this.minions.iterator();
                while (minionIterator.hasNext()) {
                    BaseCreatureEntity minion = minionIterator.next();
                    if (minion == null || !minion.isAlive())
                        minionIterator.remove();
                    else {
                        this.capacity += (minion.getSummonCost() * this.capacityCharge);
                    }
                }
            }

            // Check Capacity:
            if (this.capacity + this.summonSet.getCreatureInfo().getSummonCost() > this.capacityMax) {
                this.summonProgress = 0;
            }

            // Try To Summon:
            else {
                if (this.summoningFuel <= 0) {
                    ItemStack fuelStack = this.getItem(0);
                    if (isSummoningFuel(fuelStack)) {
                        int refuel = this.summoningFuelAmount;
                        if (isDenseSummoningFuel(fuelStack)) {
                            refuel = this.summoningFuelAmount * 9;
                        }
                        fuelStack.split(1);
                        this.summoningFuel = refuel;
                        this.summoningFuelMax = refuel;
                        this.setChanged();
                    }
                }

                if (this.summoningFuel > 0) {
                    this.summoningFuel--;

                    // Summon Minions:
                    if (this.summonProgress++ >= this.summonProgressMax && this.summoningPortal != null) {
                        this.summoningPortal.summonCreatures();
                        this.summonProgress = 0;
                        this.capacity = Math.min(this.capacity + (this.capacityCharge * this.summonSet.getCreatureInfo().getSummonCost()), this.capacityMax);
                    }
                }
            }
        }

        // Block State:
        if (!this.blockStateSet) {
            BlockSummoningPedestal.setState(this.ownerUUID != null ? BlockSummoningPedestal.EnumSummoningPedestal.PLAYER : BlockSummoningPedestal.EnumSummoningPedestal.NONE, this.getLevel(), this.getBlockPos());
            this.blockStateSet = true;
        }

        // Sync To Client:
        if (this.updateTick % 20 == 0 && this.getLevel() instanceof ServerLevel serverLevel) {
            MessageSummoningPedestalStats message = new MessageSummoningPedestalStats(this.getStatsSnapshot(), this.getBlockPos());
            for (ServerPlayer player : serverLevel.players()) {
                if (player.blockPosition().closerThan(this.getBlockPos(), 5)) {
                    LycanitesMobs.PACKET_MANAGER.sendToPlayer(message, player);
                }
            }
        }

        this.updateTick++;
    }


    // ========================================
    //           Summoning Pedestal
    // ========================================
    /** Sets the owner of this block. **/
    public void setOwner(LivingEntity entity) {
        if (entity instanceof Player player) {
            this.ownerUUID = player.getUUID();
            this.setChanged();
        }
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public Component getOwnerName() {
        if (this.getPlayer() != null) {
            return this.getPlayer().getDisplayName();
        }
        return Component.literal("");
    }

    /** Returns the player that this belongs to or null if owned by no player (or they're offline). **/
    @Nullable
    public Player getPlayer() {
        if (this.ownerUUID == null || this.getLevel() == null) {
            return null;
        }
        return this.getLevel().getPlayerByUUID(this.ownerUUID);
    }

    @Nullable
    public SummonSet getSummonSet() {
        return this.summonSet;
    }

    public boolean hasSummonSet() {
        return this.summonSet != null;
    }

    public SummonSet getOrCreateSummonSet(ExtendedPlayer playerExt) {
        if (this.summonSet == null) {
            this.summonSet = new SummonSet(playerExt);
        }
        return this.summonSet;
    }

    public void selectSummonType(ExtendedPlayer playerExt, String summonType) {
        this.getOrCreateSummonSet(playerExt).setSummonType(summonType);
    }

    public void applySummonSetPacket(String summonType, int subspecies, int variant, byte behaviour) {
        this.getOrCreateSummonSet(null).readFromPacket(summonType, subspecies, variant, behaviour);
        this.setChanged();
        this.syncToClients();
    }

    @Nullable
    public EntityType getSummonType() {
        if (this.summonSet == null) {
            return null;
        }
        return this.summonSet.getCreatureType();
    }

    @Nullable
    public CreatureInfo getCreatureInfo() {
        if (this.summonSet == null) {
            return null;
        }
        return this.summonSet.getCreatureInfo();
    }

    public int getSummonAmount() {
        return this.summonAmount;
    }

    /** Sets the Summon Set for this to use. **/
    public void setSummonSet(SummonSet summonSet) {
        if (this.getPlayer() != null && !summonSet.isUseable()) {
            return;
        }
        this.summonSet = new SummonSet(null);
        this.summonSet.setSummonType(summonSet.getSummonType());
        this.summonSet.setMovement(summonSet.getSitting(), false);
        this.summonSet.setStance(summonSet.getPassive(), summonSet.getAssist(), summonSet.getAggressive());
        this.summonSet.setPVP(summonSet.getPVP());
        this.setChanged();
    }


    // ========== Minion Behaviour ==========
    /** Applies the minion behaviour to the summoned player owned minion. **/
    public void applyMinionBehaviour(TameableCreatureEntity minion) {
        if (this.summonSet != null) {
            this.summonSet.applyBehaviour(minion);
            minion.setSubspecies(this.summonSet.getSubspecies());
            minion.applyVariant(this.summonSet.getVariant());
        }
        this.registerMinion(minion);
        minion.setHome(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ(), 20);
    }

    public void registerMinion(BaseCreatureEntity minion) {
        if (minion == null) {
            return;
        }
        if (!this.minions.contains(minion)) {
            this.minions.add(minion);
        }
        minion.bindSummoningPedestal(this);
    }


    // ========================================
    //                Inventory
    // ========================================
    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.itemStacks) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return this.itemStacks.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack removed = ContainerHelper.removeItem(this.itemStacks, index, count);
        this.setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(this.itemStacks, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        this.itemStacks.set(index, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public int getContainerSize() {
        return this.itemStacks.size();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack itemStack) {
        return isSummoningFuel(itemStack);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.isRemoved()) {
            return false;
        }
        return this.getBlockPos().distSqr(player.blockPosition()) < 16F;
    }

    public static boolean isSummoningFuel(ItemStack itemStack) {
        return itemStack.is(LycanitesItemTags.SUMMONING_PEDESTAL_FUEL) || isDenseSummoningFuel(itemStack);
    }

    private static boolean isDenseSummoningFuel(ItemStack itemStack) {
        return itemStack.is(LycanitesItemTags.SUMMONING_PEDESTAL_DENSE_FUEL);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.itemStacks.size(); i++) {
            this.itemStacks.set(i, ItemStack.EMPTY);
        }
    }

    public MutableComponent getName() {
        return Component.translatable("".equals(this.inventoryName) ? "gui.summoningpedestal" : this.inventoryName);
    }


    // ========================================
    //             Network Packets
    // ========================================
    /** Owner + summon set, sent on chunk load and when the block updates. **/
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag syncData = new CompoundTag();
        if (this.summonSet != null) {
            CompoundTag summonSetNBT = new CompoundTag();
            this.summonSet.write(summonSetNBT);
            syncData.put("SummonSet", summonSetNBT);
        }
        if (this.ownerUUID != null) {
            syncData.putString("OwnerUUID", this.ownerUUID.toString());
            syncData.putString("InventoryName", this.inventoryName);
        }
        return syncData;
    }

    @Override
    public void handleUpdateTag(CompoundTag syncData, HolderLookup.Provider registries) {
        if (syncData.contains("OwnerUUID"))
            this.ownerUUID = UUID.fromString(syncData.getString("OwnerUUID"));
        if (syncData.contains("InventoryName"))
            this.inventoryName = syncData.getString("InventoryName");
        if (syncData.contains("SummonSet")) {
            SummonSet summonSet = new SummonSet(null);
            summonSet.read(syncData.getCompound("SummonSet"));
            this.summonSet = summonSet;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void syncToClients() {
        if (this.getLevel() != null && !this.getLevel().isClientSide) {
            this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void sendSummonSetToServer(SummonSet summonSet) {
        LycanitesMobs.PACKET_MANAGER.sendToServer(new com.lycanitesmobs.core.network.message.MessageSummoningPedestalSummonSet(summonSet, this.getBlockPos()));
    }

    public void onBlockRemoved() {
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack itemStack : this.itemStacks) {
            if (!itemStack.isEmpty()) {
                drops.add(itemStack.copy());
            }
        }

        PortalEntity portal = this.summoningPortal;
        BlockPos blockPos = this.getBlockPos();
        this.summoningPortal = null;
        this.clearContent();

        DeferredLevelActionManager.enqueue(serverLevel, blockPos, "pedestal_remove:" + blockPos.asLong(), level -> {
            if (portal != null && portal.isAlive()) {
                portal.remove(Entity.RemovalReason.DISCARDED);
            }
            for (ItemStack drop : drops) {
                CustomItemEntity entityItem = new CustomItemEntity(level, blockPos.getX(), blockPos.getY() + 0.5D, blockPos.getZ(), drop);
                DeferredLevelActionManager.spawnEntityNow(level, entityItem);
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected void requestSummoningPortal() {
        if (!(this.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos blockPos = this.getBlockPos();
        DeferredLevelActionManager.enqueue(serverLevel, blockPos, "pedestal_portal:" + blockPos.asLong(), level -> {
            if (this.isRemoved() || level.getBlockEntity(blockPos) != this) {
                return;
            }
            if (this.summonSet == null || this.summonSet.getCreatureInfo() == null) {
                return;
            }
            if (this.summoningPortal != null && this.summoningPortal.isAlive()) {
                return;
            }

            PortalEntity portal = new PortalEntity((EntityType<? extends PortalEntity>) ProjectileManager.getInstance().getOldProjectileType(PortalEntity.class), level, this);
            portal.setProjectileScale(6);
            this.summoningPortal = portal;
            DeferredLevelActionManager.spawnEntityNow(level, portal);
        });
    }


    // ========== Summoning Stats ==========
    public SummoningPedestalStats getStatsSnapshot() {
        return new SummoningPedestalStats(this.capacity, this.capacityCharge, this.capacityMax, this.summonProgress,
                this.summonProgressMax, this.summoningFuel, this.summoningFuelMax);
    }

    public void applyNetworkStats(int capacity, int summonProgress, int summoningFuel, int summoningFuelMax) {
        this.capacity = capacity;
        this.summonProgress = summonProgress;
        this.summoningFuel = summoningFuel;
        this.summoningFuelMax = summoningFuelMax;
    }

    public float getFuelFillRatio() {
        if (this.summoningFuelMax <= 0) {
            return 0;
        }
        return (float) this.summoningFuel / this.summoningFuelMax;
    }

    public int getCapacityUnits() {
        if (this.capacityCharge <= 0) {
            return 0;
        }
        return this.capacity / this.capacityCharge;
    }

    public float getSummonProgressRatio() {
        if (this.summonProgressMax <= 0) {
            return 0;
        }
        return (float) this.summonProgress / this.summonProgressMax;
    }


    // ========================================
    //                 NBT Data
    // ========================================
    @Override
    protected void loadAdditional(CompoundTag nbtTagCompound, HolderLookup.Provider registries) {
        super.loadAdditional(nbtTagCompound, registries);

        this.ownerUUID = null;
        if (nbtTagCompound.contains("OwnerUUID")) {
            String uuidString = nbtTagCompound.getString("OwnerUUID");
            if (!"".equals(uuidString))
                this.ownerUUID = UUID.fromString(uuidString);
        }

        this.summonSet = null;
        if (nbtTagCompound.contains("SummonSet")) {
            SummonSet summonSet = new SummonSet(null);
            summonSet.read(nbtTagCompound.getCompound("SummonSet"));
            this.summonSet = summonSet;
        }

        if (nbtTagCompound.contains("MinionIDs")) {
            ListTag minionIDs = nbtTagCompound.getList("MinionIDs", 10);
            this.loadMinionIDs = new String[minionIDs.size()];
            for (int i = 0; i < minionIDs.size(); i++) {
                CompoundTag minionID = minionIDs.getCompound(i);
                if (minionID.contains("ID")) {
                    this.loadMinionIDs[i] = minionID.getString("ID");
                }
            }
        }

        // Fuel:
        if (nbtTagCompound.contains("Fuel")) {
            this.summoningFuel = nbtTagCompound.getInt("Fuel");
        }
        if (nbtTagCompound.contains("FuelMax")) {
            this.summoningFuelMax = nbtTagCompound.getInt("FuelMax");
        }
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbtTagCompound, this.itemStacks, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag nbtTagCompound, HolderLookup.Provider registries) {
        super.saveAdditional(nbtTagCompound, registries);

        nbtTagCompound.putString("OwnerUUID", this.ownerUUID == null ? "" : this.ownerUUID.toString());

        if (this.summonSet != null) {
            CompoundTag summonSetNBT = new CompoundTag();
            this.summonSet.write(summonSetNBT);
            nbtTagCompound.put("SummonSet", summonSetNBT);
        }

        if (!this.minions.isEmpty()) {
            ListTag minionIDs = new ListTag();
            for (LivingEntity minion : this.minions) {
                CompoundTag minionID = new CompoundTag();
                minionID.putString("ID", minion.getUUID().toString());
                minionIDs.add(minionID);
            }
            nbtTagCompound.put("MinionIDs", minionIDs);
        }

        // Fuel:
        nbtTagCompound.putInt("Fuel", this.summoningFuel);
        nbtTagCompound.putInt("FuelMax", this.summoningFuelMax);
        ContainerHelper.saveAllItems(nbtTagCompound, this.itemStacks, registries);
    }

    public record SummoningPedestalStats(
            int capacity,
            int capacityCharge,
            int capacityMax,
            int summonProgress,
            int summonProgressMax,
            int summoningFuel,
            int summoningFuelMax
    ) {
    }
}
