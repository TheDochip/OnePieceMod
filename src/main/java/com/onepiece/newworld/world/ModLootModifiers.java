package com.onepiece.newworld.world;

import com.mojang.serialization.MapCodec;
import com.onepiece.newworld.OnePieceNewWorld;
import java.util.function.Supplier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, OnePieceNewWorld.MODID);

    public static final Supplier<MapCodec<DevilFruitChestModifier>> DEVIL_FRUIT_CHESTS =
            LOOT_MODIFIERS.register("devil_fruit_chests", () -> DevilFruitChestModifier.CODEC);

    private ModLootModifiers() {
    }
}
