package com.lycanitesmobs.core.worldgen.structure;

import com.lycanitesmobs.LycanitesMobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The dungeon structure type and its piece type. Port: merges the official ModStructureTypes and ModStructurePieceTypes,
 * which called Registry.register directly (NeoForge's registries are frozen by then, so these are DeferredRegisters).
 */
public class ModStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, LycanitesMobs.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, LycanitesMobs.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<LMDungeonStructure>> LM_DUNGEON =
            STRUCTURE_TYPES.register("lm_dungeon", () -> () -> LMDungeonStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> LM_DUNGEON_PIECE =
            STRUCTURE_PIECES.register("lm_dungeon_piece", () -> (StructurePieceType.ContextlessType) LMDungeonPiece::new);

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
    }
}
