package com.lycanitesmobs.core.manager;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.block.liquid.AcidLiquidBlock;
import com.lycanitesmobs.core.block.liquid.BaseLiquidBlock;
import com.lycanitesmobs.core.block.liquid.MoglavaLiquidBlock;
import com.lycanitesmobs.core.block.liquid.OozeLiquidBlock;
import com.lycanitesmobs.core.block.liquid.PoisonLiquidBlock;
import com.lycanitesmobs.core.block.liquid.VeshoneyLiquidBlock;
import com.lycanitesmobs.core.util.helpers.AssetHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lycanites' 8 custom fluids (ooze, rabbitooze, moglava, acid, sharacid, poison, vesspoison, veshoney), each with a
 * FluidType, source + flowing fluid, liquid block and bucket.
 *
 * <p>1.21.1 rewrite of the official FluidManager: FluidTypes and fluids go through DeferredRegisters (NeoForge
 * registers fluid types before fluids, and fluids before blocks/items), the liquid block goes through
 * ObjectManager.addBlock(isLiquid) and the bucket through ObjectManager.addItem. Client rendering (textures, tint,
 * fog, translucency) is registered in ClientSetup via RegisterClientExtensionsEvent - FluidType.initializeClient is
 * deprecated in NeoForge 21.1.
 */
public class FluidManager {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, LycanitesMobs.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, LycanitesMobs.MODID);

    private static FluidManager INSTANCE;

    public static FluidManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FluidManager();
        }
        return INSTANCE;
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }

    /** A defined fluid, with everything the client needs to render it. **/
    public record FluidEntry(String name, int color, DeferredHolder<FluidType, FluidType> type,
                             DeferredHolder<Fluid, BaseFlowingFluid> source, DeferredHolder<Fluid, BaseFlowingFluid> flowing) {
        public ResourceLocation stillTexture() {
            return AssetHelper.modResource("block/" + this.name + "_still");
        }

        public ResourceLocation flowingTexture() {
            return AssetHelper.modResource("block/" + this.name + "_flowing");
        }
    }

    @FunctionalInterface
    public interface LiquidBlockFactory {
        BaseLiquidBlock create(FlowingFluid fluid, BlockBehaviour.Properties blockProperties, String fluidName, String elementName, boolean destroyItems);
    }

    private final Map<String, FluidEntry> fluids = new LinkedHashMap<>();
    private boolean fluidsDefined = false;

    public Collection<FluidEntry> getFluids() {
        return Collections.unmodifiableCollection(this.fluids.values());
    }

    /**
     * Defines every fluid. Must run in the mod constructor's call graph (DeferredRegister).
     */
    public void defineFluids() {
        if (this.fluidsDefined) {
            return;
        }
        this.fluidsDefined = true;

        BlockBehaviour.Properties waterBlockProperties = BlockBehaviour.Properties.of().mapColor(MapColor.WATER).noCollission().randomTicks().strength(100).noLootTable().replaceable().liquid();
        BlockBehaviour.Properties waterBrightBlockProperties = BlockBehaviour.Properties.of().mapColor(MapColor.WATER).noCollission().randomTicks().strength(100).noLootTable().lightLevel(s -> 10).replaceable().liquid();
        BlockBehaviour.Properties lavaBlockProperties = BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).noCollission().randomTicks().strength(100).noLootTable().lightLevel(s -> 15).replaceable().liquid();

        this.addFluid("ooze", 0x009F9F, 3000, 3000, 0, 10, false, OozeLiquidBlock::new, waterBrightBlockProperties, "frost", false);
        this.addFluid("rabbitooze", 0x00AFAF, 3000, 3000, 0, 10, true, OozeLiquidBlock::new, waterBrightBlockProperties, "frost", false);
        this.addFluid("moglava", 0xFF5722, 3000, 5000, 1100, 15, true, MoglavaLiquidBlock::new, lavaBlockProperties, "lava", false);
        this.addFluid("acid", 0x8BC34A, 1000, 10, 40, 10, false, AcidLiquidBlock::new, waterBrightBlockProperties, "acid", true);
        this.addFluid("sharacid", 0x8BB35A, 1000, 10, 40, 10, true, AcidLiquidBlock::new, waterBrightBlockProperties, "acid", false);
        this.addFluid("poison", 0x9C27B0, 1000, 8, 20, 0, false, PoisonLiquidBlock::new, waterBlockProperties, "poison", false);
        this.addFluid("vesspoison", 0xAC27A0, 1000, 8, 20, 0, true, PoisonLiquidBlock::new, waterBlockProperties, "poison", false);
        this.addFluid("veshoney", 0xCEBC39, 4000, 4000, 0, 0, false, VeshoneyLiquidBlock::new, waterBlockProperties, "fae", false);
    }

    /**
     * @param multiply If true, two sources make a new source (like water).
     */
    public void addFluid(String fluidName, int fluidColor, int density, int viscosity, int temperature, int luminosity, boolean multiply,
                         LiquidBlockFactory blockFactory, BlockBehaviour.Properties blockProperties, String elementName, boolean destroyItems) {
        int tickRate = (viscosity >= 4000) ? 30 : 5;

        DeferredHolder<FluidType, FluidType> fluidType = FLUID_TYPES.register(fluidName, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("block." + LycanitesMobs.MODID + "." + fluidName)
                .density(density)
                .viscosity(viscosity)
                .temperature(temperature)
                .lightLevel(luminosity)
                .canConvertToSource(multiply)));

        // The fluid properties reference both fluids and the block/bucket, all lazily through holders/suppliers.
        BaseFlowingFluid.Properties[] properties = new BaseFlowingFluid.Properties[1];
        DeferredHolder<Fluid, BaseFlowingFluid> source = FLUIDS.register(fluidName, () -> new BaseFlowingFluid.Source(properties[0]));
        DeferredHolder<Fluid, BaseFlowingFluid> flowing = FLUIDS.register(fluidName + "_flowing", () -> new BaseFlowingFluid.Flowing(properties[0]));
        properties[0] = new BaseFlowingFluid.Properties(fluidType, source, flowing)
                .tickRate(tickRate)
                .bucket(() -> ObjectManager.getItem(fluidName + "_bucket"))
                .block(() -> ObjectManager.getFluidBlock(fluidName));

        this.fluids.put(fluidName, new FluidEntry(fluidName, fluidColor, fluidType, source, flowing));
        ObjectManager.addSound(fluidName, "block." + fluidName);

        Item.Properties bucketProps = new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1);
        ObjectManager.addItem(fluidName + "_bucket", () -> new BucketItem(source.get(), bucketProps));

        ObjectManager.addBlock(fluidName, () -> blockFactory.create(source.get(), blockProperties, fluidName, elementName, destroyItems), true);
    }
}
