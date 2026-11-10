package com.onepiece.newworld.core;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.abilities.Ability;
import com.onepiece.newworld.fruits.DevilFruit;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Ключи собственных реестров мода. Другие моды и аддоны могут добавлять в них свои фрукты и способности.
 */
public final class ModRegistries {
    public static final ResourceKey<Registry<Ability>> ABILITY_KEY =
            ResourceKey.createRegistryKey(OnePieceNewWorld.id("ability"));
    public static final ResourceKey<Registry<DevilFruit>> DEVIL_FRUIT_KEY =
            ResourceKey.createRegistryKey(OnePieceNewWorld.id("devil_fruit"));

    private ModRegistries() {
    }
}
