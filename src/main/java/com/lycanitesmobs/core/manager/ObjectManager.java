package com.lycanitesmobs.core.manager;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.entity.effect.EffectBase;
import com.lycanitesmobs.core.data.info.ModInfo;
import com.lycanitesmobs.core.data.info.ObjectLists;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.lycanitesmobs.core.tabs.LMItemsGroup.itemNames;


public class ObjectManager {

    // ========== Items/Blocks ==========
    private static final Map<String, Supplier<? extends Item>> items = new HashMap<>();
    private static final Map<String, Lazy<? extends BlockItem>> blockItems = new HashMap<>();
    private static final Map<String, Lazy<? extends Block>> blocks = new HashMap<>();
    private static final Map<String, Lazy<? extends LiquidBlock>> liquidBlocks = new HashMap<>();
    // ========== Entities ==========
    private static final Map<String, Lazy<? extends EntityType<?>>> entityTypes = new HashMap<>();
    private static ObjectManager INSTANCE;
    private static ModInfo currentModInfo;
    private static final Map<String, Class> tileEntities = new HashMap<>();
    private static final Map<Class<? extends BlockEntity>, BlockEntityType<? extends BlockEntity>> tileEntityTypes = new HashMap<>();
    private static final Map<String, BaseFlowingFluid> fluids = new HashMap<>();
    private static final Map<Block, Item> buckets = new HashMap<>();
    // ========== Creative Mode Tabs ==========
    private static final Map<Item, ModInfo> itemGroups = new HashMap<>();
    // ========== Damage Sources/Effects ==========
    private static final Map<String, EffectBase> effects = new HashMap<>();
    // ========== Sounds ==========
    private static final Map<String, ResourceLocation> soundNames = new HashMap<>();
    private static final Map<String, Lazy<? extends SoundEvent>> sounds = new HashMap<>();
    private static final Map<String, Class<? extends Entity>> specialEntities = new HashMap<>();
    private static final Map<Class<? extends Entity>, EntityType<? extends Entity>> specialEntityTypes = new HashMap<>();

    private static final Map<String, ResourceKey<DamageType>> damageTypeKeys = new HashMap<>();


    /**
     * The next available network id for special entities to register by.
     **/
    protected static int nextSpecialEntityNetworkId = 0;

    public static ObjectManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new ObjectManager();
        return INSTANCE;
    }

    // ==================================================
    //                        Setup
    // ==================================================
    public static void setCurrentModInfo(ModInfo group) {
        currentModInfo = group;
    }

    /**
     * Generates the next available special entity network id to register with.
     *
     * @return The next special entity network id.
     */
    public static int getNextSpecialEntityNetworkId() {
        return nextSpecialEntityNetworkId++;
    }

    // ==================================================
    //                        Add
    // ==================================================
    // ========== Block ==========
    public static void addBlock(String name, Supplier<? extends Block> block, boolean isLiquid) {
        if (isLiquid) {
            liquidBlocks.put(name, Lazy.of(() -> (LiquidBlock) block.get()));
        } else {
            Item.Properties blockItemProperties = new Item.Properties();
            blocks.put(name, Lazy.of(block));
            blockItems.put(name, Lazy.of(() -> new BlockItem(blocks.get(name).get(), blockItemProperties)));
        }
    }

    // ========== Fluid ==========
    public static Fluid addFluid(String name, BaseFlowingFluid fluid) {
        fluids.put(name, fluid);
        return fluid;
    }

    // ========== Item ==========
    public static void addItem(String name, Supplier<? extends Item> itemSupplier) {
        if (!items.containsKey(name)) {
            DeferredHolder<Item, Item> item = LycanitesMobs.ITEMS.register(name, itemSupplier);
            items.put(name, item);
            itemNames.add(name);
        }
    }

    // ========== Tile Entity ==========
    public static Class addTileEntity(String name, Class tileEntityClass) {
        name = name.toLowerCase();
        tileEntities.put(name, tileEntityClass);
        return tileEntityClass;
    }

    // ========== Potion Effect ==========
    public static EffectBase addPotionEffect(String name, boolean isBad, int color, boolean goodEffect) {
        EffectBase effect = new EffectBase(name, isBad, color);
        effects.put(name, effect);
        ObjectLists.addEffect(goodEffect ? "buffs" : "debuffs", effect, name);

        return effect;
    }

    // ========== Special Entity ==========
    public static void addSpecialEntity(String name, Class<? extends Entity> entityClass) {
        specialEntities.put(name, entityClass);
    }

    public static void addEntityType(String name, EntityType.Builder<?> builder) {
        entityTypes.put(name, Lazy.of(() -> builder.build(name)));
    }

    public static void registerTileEntityType(Class<? extends BlockEntity> tileEntityClass, BlockEntityType<? extends BlockEntity> tileEntityType) {
        tileEntityTypes.put(tileEntityClass, tileEntityType);
    }

    // ========== Damage Source ==========
    public static void addDamageType(String name) {
        ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, AssetHelper.modResource(name.toLowerCase()));
        damageTypeKeys.put(name.toLowerCase(), key);
    }

    // ========== Sound ==========
    public static void addSound(String name, String path) {
        name = name.toLowerCase();
        ResourceLocation resourceLocation = AssetHelper.modResource(path);
        soundNames.put(name, resourceLocation);
        sounds.put(name, Lazy.of(() -> SoundEvent.createVariableRangeEvent(resourceLocation)));
    }

    // ==================================================
    //                        Get
    // ==================================================
    // ========== Block ==========
    public static Block getBlock(String name) {
        name = name.toLowerCase();
        if (!blocks.containsKey(name))
            return null;
        return blocks.get(name).get();
    }

    public static LiquidBlock getFluidBlock(String name) {
        name = name.toLowerCase();
        if (!liquidBlocks.containsKey(name))
            return null;
        return liquidBlocks.get(name).get();
    }

    // ========== Item ==========
    public static Item getItem(String name) {
        name = name.toLowerCase();
        if (!items.containsKey(name))
            return null;
        return items.get(name).get();
    }

    public static boolean hasItem(String name) {
        return items.containsKey(name.toLowerCase());
    }

    // ========== Fluid ==========
    public static BaseFlowingFluid getFluid(String name) {
        name = name.toLowerCase();
        if (!fluids.containsKey(name))
            return null;
        return fluids.get(name);
    }

    public static Item getBucket(Block block) {
        return buckets.get(block);
    }

    // ========== Tile Entity ==========
    public static Class getTileEntity(String name) {
        name = name.toLowerCase();
        if (!tileEntities.containsKey(name)) return null;
        return tileEntities.get(name);
    }

    public static BlockEntityType<?> getTileEntityType(Class<? extends BlockEntity> tileEntityClass) {
        return tileEntityTypes.get(tileEntityClass);
    }

    // ========== Potion Effect ==========
    public static EffectBase getEffect(String name) {
        name = name.toLowerCase();
        if (!effects.containsKey(name)) return null;
        return effects.get(name);
    }

    // ========== Damage Source ==========
    public static DamageSource getDamageSource(Level level, String name) {
        ResourceKey<DamageType> key = damageTypeKeys.get(name.toLowerCase());
        if (key == null) {
            LMHelperClass.logWarning("ObjectManager", "Unknown damage type: " + name);
            return level.damageSources().generic();
        }
        Holder<DamageType> holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
        return new DamageSource(holder);
    }

    public static DamageSource getDamageSource(Level level, String name, @Nullable Entity direct, @Nullable Entity causing) {
        ResourceKey<DamageType> key = damageTypeKeys.get(name.toLowerCase());
        if (key == null) {
            LMHelperClass.logWarning("ObjectManager", "Unknown damage type: " + name);
            return level.damageSources().generic();
        }
        Holder<DamageType> holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
        return new DamageSource(holder, direct, causing);
    }

    // ========== Sound ==========
    public static SoundEvent getSound(String name) {
        name = name.toLowerCase();
        if (!sounds.containsKey(name))
            return null;
        return sounds.get(name).get();
    }

    public static EntityType<? extends Entity> getSpecialEntityType(Class<? extends Entity> entityClass) {
        return specialEntityTypes.get(entityClass);
    }

    public static Collection<Class<? extends Entity>> getSpecialEntityClasses() {
        return Collections.unmodifiableCollection(specialEntities.values());
    }

    public static Set<Map.Entry<String, Lazy<? extends EntityType<?>>>> getEntityTypeEntries() {
        return Collections.unmodifiableSet(entityTypes.entrySet());
    }

    public static Collection<Lazy<? extends Block>> getBlockSuppliers() {
        return Collections.unmodifiableCollection(blocks.values());
    }

    public static Collection<Lazy<? extends LiquidBlock>> getLiquidBlockSuppliers() {
        return Collections.unmodifiableCollection(liquidBlocks.values());
    }

    public static Set<Map.Entry<String, Lazy<? extends BlockItem>>> getBlockItemEntries() {
        return Collections.unmodifiableSet(blockItems.entrySet());
    }

    public static Set<Map.Entry<String, Supplier<? extends Item>>> getItemSupplierEntries() {
        return Collections.unmodifiableSet(items.entrySet());
    }

    public static Collection<EffectBase> getEffects() {
        return Collections.unmodifiableCollection(effects.values());
    }

    public static Collection<BaseFlowingFluid> getFluids() {
        return Collections.unmodifiableCollection(fluids.values());
    }


    @Nonnull
    public static EntityType<? extends LivingEntity> getEntityType(String name) {
        LMHelperClass.environmentRunnable(false, () -> {
            LMHelperClass.logInfoMessage("EntityType: " + name);
        });
        return (EntityType<? extends LivingEntity>) entityTypes.get(name).get();
    }

    // NOTE: registerContainers(IEventBus) was dropped here during the NeoForge 1.21.1 port - it
    // registers the 5 custom menu/container types (CreatureContainer, SummoningPedestalContainer,
    // EquipmentForgeContainer, EquipmentInfuserContainer, EquipmentStationContainer), none of
    // which are ported yet (Phase 6/8). Port it back in alongside those.
}
