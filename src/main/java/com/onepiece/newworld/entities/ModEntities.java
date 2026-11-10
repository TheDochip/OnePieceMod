package com.onepiece.newworld.entities;

import com.onepiece.newworld.OnePieceNewWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, OnePieceNewWorld.MODID);

    /** Снаряд способностей (огонь, лёд). */
    public static final DeferredHolder<EntityType<?>, EntityType<ElementalProjectile>> ELEMENTAL_PROJECTILE =
            ENTITY_TYPES.register("elemental_projectile", () -> EntityType.Builder
                    .<ElementalProjectile>of(ElementalProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("elemental_projectile"));

    /** Летящий удар мечом. */
    public static final DeferredHolder<EntityType<?>, EntityType<SlashProjectile>> SLASH =
            ENTITY_TYPES.register("slash", () -> EntityType.Builder
                    .<SlashProjectile>of(SlashProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("slash"));

    /** Пират-головорез: враждебный NPC, появляется на пляжах. */
    public static final DeferredHolder<EntityType<?>, EntityType<PirateGrunt>> PIRATE_GRUNT =
            ENTITY_TYPES.register("pirate_grunt", () -> EntityType.Builder
                    .of(PirateGrunt::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .build("pirate_grunt"));

    /** Новобранец Дозора: охотится на пиратов с высокой наградой и на пиратов-NPC. */
    public static final DeferredHolder<EntityType<?>, EntityType<MarineRecruit>> MARINE_RECRUIT =
            ENTITY_TYPES.register("marine_recruit", () -> EntityType.Builder
                    .of(MarineRecruit::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .build("marine_recruit"));

    private ModEntities() {
    }
}
