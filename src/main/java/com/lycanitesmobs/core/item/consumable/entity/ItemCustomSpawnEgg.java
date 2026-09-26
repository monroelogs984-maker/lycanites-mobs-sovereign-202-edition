package com.lycanitesmobs.core.item.consumable.entity;

import com.lycanitesmobs.core.data.info.creature.CreatureInfo;
import com.lycanitesmobs.core.data.info.creature.CreatureType;
import com.lycanitesmobs.core.entity.dispenser.SpawnEggDispenseBehaviour;
import com.lycanitesmobs.core.item.base.CreatureTypeItem;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.DeferredLevelActionManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * One spawn egg item per creature type, the creature it spawns is stored on the stack.
 *
 * <p>1.21.1: stack NBT is gone, the official {@code CreatureInfoSpawnEgg: {creaturename: ...}} compound now lives in
 * the {@code minecraft:custom_data} component, so the same structure works in give commands:
 * {@code /give @s lycanitesmobs:spawn_beast[custom_data={CreatureInfoSpawnEgg:{creaturename:"warg"}}]}.
 */
public class ItemCustomSpawnEgg extends CreatureTypeItem {

    public ItemCustomSpawnEgg(Item.Properties properties, CreatureType creatureType) {
        super(properties, creatureType.getSpawnEggName(), creatureType);
        this.setup();
        DispenserBlock.registerBehavior(this, new SpawnEggDispenseBehaviour());
        LMHelperClass.logDebug("Creature Type", "Created Creature Type Spawn Egg: " + this.itemName);
    }

    @Override
    public Component getName(ItemStack itemStack) {
        MutableComponent displayName = Component.translatable("creaturetype.spawn")
                .append(" ")
                .append(this.creatureType.getTitle())
                .append(": ");
        CreatureInfo creatureInfo = this.getCreatureInfo(itemStack);
        if (creatureInfo != null)
            displayName.append(creatureInfo.getTitle());
        else
            displayName.append("Missing Creature NBT");
        return displayName;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Component description = this.getDescription(stack, context, tooltip, flag);
        if (!"".equalsIgnoreCase(description.getString())) {
            tooltip.add(description);
        }
    }

    @Override
    public Component getDescription(ItemStack itemStack, @Nullable Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CreatureInfo creatureInfo = this.getCreatureInfo(itemStack);
        if (creatureInfo == null) {
            String creatureName = this.getCreatureName(itemStack);
            return Component.literal("Unable to get Creature Info for id: '" + creatureName + "' this spawn egg may have been created by a give command without custom data.");
        }
        return creatureInfo.getDescription().plainCopy().withStyle(ChatFormatting.GREEN);
    }

    /**
     * Applies creature info to a spawn egg item stack.
     *
     * @param itemStack    The spawn egg item stack top apply to.
     * @param creatureInfo The creature info to apply.
     */
    public void applyCreatureInfoToItemStack(ItemStack itemStack, CreatureInfo creatureInfo) {
        CustomData.update(DataComponents.CUSTOM_DATA, itemStack, tag -> {
            CompoundTag spawnEggNBT = new CompoundTag();
            spawnEggNBT.putString("creaturename", creatureInfo.getName());
            tag.put("CreatureInfoSpawnEgg", spawnEggNBT);
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        BlockState blockState = world.getBlockState(pos);

        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        CreatureInfo creatureInfo = this.getCreatureInfo(itemStack);
        if (creatureInfo == null) {
            return InteractionResult.FAIL;
        }
        if (player != null && !player.mayUseItemAt(pos.relative(context.getClickedFace()), context.getClickedFace(), itemStack)) {
            return InteractionResult.FAIL;
        }

        // Edit Spawner:
        if (blockState.is(Blocks.SPAWNER)) {
            BlockEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof SpawnerBlockEntity spawner) {
                EntityType<?> entityType = creatureInfo.getEntityType();
                spawner.setEntityId(entityType, world.getRandom());
                tileEntity.setChanged();
                world.sendBlockUpdated(pos, blockState, blockState, 3);
                this.consume(player, itemStack);
                return InteractionResult.SUCCESS;
            }
        }

        // Spawn Mob:
        pos = pos.relative(context.getClickedFace());
        double yOffset = 0.0D;
        if (context.getClickedFace() == Direction.UP && blockState.getBlock() instanceof FenceBlock) {
            yOffset = 0.5D;
        }

        LivingEntity entity = this.spawnCreature(world, itemStack, (double) pos.getX() + 0.5D, (double) pos.getY() + yOffset, (double) pos.getZ() + 0.5D);
        if (entity != null) {
            this.consume(player, itemStack);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (world.isClientSide) {
            return InteractionResultHolder.pass(itemStack);
        }

        HitResult rayTraceResult = getPlayerPOVHitResult(world, player, ClipContext.Fluid.SOURCE_ONLY);
        if (rayTraceResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemStack);
        }

        BlockHitResult blockRayTraceResult = (BlockHitResult) rayTraceResult;
        BlockPos pos = blockRayTraceResult.getBlockPos();
        if (!world.mayInteract(player, pos)) {
            return InteractionResultHolder.fail(itemStack);
        }
        if (!player.mayUseItemAt(pos, blockRayTraceResult.getDirection(), itemStack)) {
            return InteractionResultHolder.pass(itemStack);
        }

        // Spawning into fluids (water creatures, lava creatures):
        if (!world.getFluidState(pos).isEmpty()) {
            LivingEntity entity = this.spawnCreature(world, itemStack, (double) pos.getX() + 0.5D, (double) pos.getY(), (double) pos.getZ() + 0.5D);
            if (entity != null) {
                this.consume(player, itemStack);
            }
        }

        return InteractionResultHolder.success(itemStack);
    }

    protected void consume(@Nullable Player player, ItemStack itemStack) {
        if (player == null || !player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
    }

    /**
     * Get Creature Info
     *
     * @param itemStack The spawn egg item stack to get the creature from.
     * @return The Creature Info of the stack spawn egg or null if unknown.
     */
    @Nullable
    public CreatureInfo getCreatureInfo(ItemStack itemStack) {
        String creatureName = this.getCreatureName(itemStack);
        if (creatureName == null) {
            return null;
        }
        return CreatureManager.getInstance().getCreature(creatureName);
    }

    /**
     * Get Creature Name
     *
     * @param itemStack The spawn egg item stack to get the creature name from.
     * @return The name of the creature that the spawn egg item stack should spawn.
     */
    @Nullable
    public String getCreatureName(ItemStack itemStack) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag itemStackNBT = customData.copyTag();
        if (!itemStackNBT.contains("CreatureInfoSpawnEgg", 10)) {
            return null;
        }
        CompoundTag spawnEggNBT = itemStackNBT.getCompound("CreatureInfoSpawnEgg");
        return !spawnEggNBT.contains("creaturename", 8) ? null : spawnEggNBT.getString("creaturename");
    }

    /**
     * Spawn Creature
     *
     * @param world     The world to spawn in.
     * @param itemStack The spawn egg itemstack to spawn from.
     * @param x         X spawn coordinate.
     * @param y         Y spawn coordinate.
     * @param z         Z spawn coordinate.
     * @return The spawned entity instance.
     */
    @Nullable
    public LivingEntity spawnCreature(Level world, ItemStack itemStack, double x, double y, double z) {
        CreatureInfo creatureInfo = this.getCreatureInfo(itemStack);
        if (creatureInfo == null || !(world instanceof ServerLevel)) {
            return null;
        }
        LivingEntity entity = creatureInfo.createEntity(world);
        if (entity == null) {
            return null;
        }

        entity.moveTo(x, y, z, LMHelperClass.convertToFloat(LMHelperClass.wrapDegrees(world.random.nextFloat() * 360.0F)), 0.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yBodyRot = entity.getYRot();

        if (itemStack.has(DataComponents.CUSTOM_NAME)) {
            entity.setCustomName(itemStack.getHoverName());
        }

        if (entity instanceof Mob mobEntity) {
            mobEntity.finalizeSpawn((ServerLevelAccessor) world, world.getCurrentDifficultyAt(mobEntity.blockPosition()), MobSpawnType.SPAWN_EGG, null);
            mobEntity.playAmbientSound();
        }

        DeferredLevelActionManager.spawnEntity(world, BlockPos.containing(x, y, z), "spawn_egg:" + entity.getUUID(), entity);
        return entity;
    }
}
