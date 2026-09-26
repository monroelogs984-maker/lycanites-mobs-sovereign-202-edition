package com.lycanitesmobs.core.manager;

import com.lycanitesmobs.LycanitesMobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom creature attributes (defense, ranged attack speed). 1.21.1 requires these be
 * registered through a real DeferredRegister<Attribute> to get a proper Holder<Attribute> -
 * unlike 1.20.1 Forge, where a bare `new RangedAttribute(...)` constant was usable directly.
 */
public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, LycanitesMobs.MODID);

    public static final DeferredHolder<Attribute, Attribute> DEFENSE = ATTRIBUTES.register("generic.defense",
            () -> new RangedAttribute(LycanitesMobs.MODID + ":generic.defense", 4.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> RANGED_SPEED = ATTRIBUTES.register("generic.ranged_speed",
            () -> new RangedAttribute(LycanitesMobs.MODID + ":generic.ranged_speed", 4.0D, 0.0D, 1024.0D).setSyncable(true));
}
