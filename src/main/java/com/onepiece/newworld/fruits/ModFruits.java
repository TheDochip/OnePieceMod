package com.onepiece.newworld.fruits;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.abilities.ModAbilities;
import com.onepiece.newworld.core.ModRegistries;
import com.onepiece.newworld.fruits.gomu.GomuFruit;
import com.onepiece.newworld.fruits.hie.HieFruit;
import com.onepiece.newworld.fruits.mera.MeraFruit;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Реестр дьявольских фруктов. */
public final class ModFruits {
    public static final DeferredRegister<DevilFruit> FRUITS =
            DeferredRegister.create(ModRegistries.DEVIL_FRUIT_KEY, OnePieceNewWorld.MODID);
    public static final Registry<DevilFruit> REGISTRY = FRUITS.makeRegistry(builder -> builder.sync(true));

    public static final DeferredHolder<DevilFruit, GomuFruit> GOMU = FRUITS.register("gomu",
            () -> new GomuFruit(List.of(ModAbilities.GOMU_PISTOL, ModAbilities.GOMU_ROCKET, ModAbilities.GOMU_GATLING)));
    public static final DeferredHolder<DevilFruit, MeraFruit> MERA = FRUITS.register("mera",
            () -> new MeraFruit(List.of(ModAbilities.MERA_FIRE_BULLET, ModAbilities.MERA_FIRE_FIST, ModAbilities.MERA_FIRE_PILLAR)));
    public static final DeferredHolder<DevilFruit, HieFruit> HIE = FRUITS.register("hie",
            () -> new HieFruit(List.of(ModAbilities.HIE_ICE_SHARD, ModAbilities.HIE_ICE_AGE, ModAbilities.HIE_ICE_SPEARS)));

    private ModFruits() {
    }

    @Nullable
    public static DevilFruit get(@Nullable ResourceLocation id) {
        return id == null ? null : REGISTRY.get(id);
    }
}
