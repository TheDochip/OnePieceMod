package com.onepiece.newworld.entities;

import com.onepiece.newworld.OnePieceNewWorld;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/** Атрибуты и правила появления сущностей (шина мода). */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class EntityEvents {
    private EntityEvents() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.PIRATE_GRUNT.get(), PirateGrunt.createAttributes().build());
        event.put(ModEntities.MARINE_RECRUIT.get(), MarineRecruit.createAttributes().build());
    }

    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Пираты высаживаются на пляжи и днём, поэтому проверка освещённости не нужна — только не на «мирной» сложности.
        event.register(ModEntities.PIRATE_GRUNT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getDifficulty() != Difficulty.PEACEFUL
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                        && level.canSeeSky(pos),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
