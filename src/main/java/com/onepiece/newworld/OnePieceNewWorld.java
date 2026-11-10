package com.onepiece.newworld;

import com.mojang.logging.LogUtils;
import com.onepiece.newworld.abilities.ModAbilities;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModBlocks;
import com.onepiece.newworld.core.ModConfig;
import com.onepiece.newworld.core.ModCreativeTabs;
import com.onepiece.newworld.core.ModEffects;
import com.onepiece.newworld.core.ModItems;
import com.onepiece.newworld.entities.ModEntities;
import com.onepiece.newworld.fruits.ModFruits;
import com.onepiece.newworld.networking.ModNetwork;
import com.onepiece.newworld.world.ModLootModifiers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Точка входа мода One Piece: New World.
 * Здесь только подключаются модули; вся логика живёт в своих пакетах.
 */
@Mod(OnePieceNewWorld.MODID)
public final class OnePieceNewWorld {
    public static final String MODID = "onepiece_newworld";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OnePieceNewWorld(IEventBus modBus, ModContainer container) {
        // Собственные реестры должны регистрироваться первыми: на них ссылаются фрукты и предметы.
        ModAbilities.ABILITIES.register(modBus);
        ModFruits.FRUITS.register(modBus);

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModEffects.MOB_EFFECTS.register(modBus);
        ModAttachments.ATTACHMENT_TYPES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modBus);

        modBus.addListener(ModNetwork::register);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, ModConfig.SPEC);

        LOGGER.info("One Piece: New World загружен");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
