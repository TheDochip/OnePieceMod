package com.onepiece.newworld.abilities;

import com.onepiece.newworld.combat.SwordTechniques;
import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.core.ModRegistries;
import com.onepiece.newworld.fruits.gomu.GomuAbilities;
import com.onepiece.newworld.fruits.hie.HieAbilities;
import com.onepiece.newworld.fruits.mera.MeraAbilities;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Реестр способностей. Сами способности описаны в пакетах своих фруктов,
 * а здесь только регистрируются — так все записи гарантированно попадают в реестр вовремя.
 */
public final class ModAbilities {
    public static final DeferredRegister<Ability> ABILITIES =
            DeferredRegister.create(ModRegistries.ABILITY_KEY, OnePieceNewWorld.MODID);
    public static final Registry<Ability> REGISTRY = ABILITIES.makeRegistry(builder -> builder.sync(true));

    // Гому Гому
    public static final DeferredHolder<Ability, Ability> GOMU_PISTOL = ABILITIES.register("gomu_pistol", GomuAbilities::pistol);
    public static final DeferredHolder<Ability, Ability> GOMU_ROCKET = ABILITIES.register("gomu_rocket", GomuAbilities::rocket);
    public static final DeferredHolder<Ability, Ability> GOMU_GATLING = ABILITIES.register("gomu_gatling", GomuAbilities::gatling);

    // Мера Мера
    public static final DeferredHolder<Ability, Ability> MERA_FIRE_BULLET = ABILITIES.register("mera_fire_bullet", MeraAbilities::fireBullet);
    public static final DeferredHolder<Ability, Ability> MERA_FIRE_FIST = ABILITIES.register("mera_fire_fist", MeraAbilities::fireFist);
    public static final DeferredHolder<Ability, Ability> MERA_FIRE_PILLAR = ABILITIES.register("mera_fire_pillar", MeraAbilities::firePillar);

    // Хиэ Хиэ
    public static final DeferredHolder<Ability, Ability> HIE_ICE_SHARD = ABILITIES.register("hie_ice_shard", HieAbilities::iceShard);
    public static final DeferredHolder<Ability, Ability> HIE_ICE_AGE = ABILITIES.register("hie_ice_age", HieAbilities::iceAge);
    public static final DeferredHolder<Ability, Ability> HIE_ICE_SPEARS = ABILITIES.register("hie_ice_spears", HieAbilities::iceSpears);

    // Техники меча (клавиши Z/X/C с мечом в руке)
    public static final DeferredHolder<Ability, Ability> SWORD_FLYING_SLASH = ABILITIES.register("sword_flying_slash", SwordTechniques::flyingSlash);
    public static final DeferredHolder<Ability, Ability> SWORD_VERTICAL_SLASH = ABILITIES.register("sword_vertical_slash", SwordTechniques::verticalSlash);
    public static final DeferredHolder<Ability, Ability> SWORD_STORM_SLASH = ABILITIES.register("sword_storm_slash", SwordTechniques::stormSlash);

    private ModAbilities() {
    }
}
