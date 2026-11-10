package com.onepiece.newworld.client;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.client.render.HumanoidNpcRenderer;
import com.onepiece.newworld.entities.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Регистрация клиентских вещей: клавиши, слой интерфейса, рендеры сущностей. */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : ModKeys.ALL) {
            event.register(key);
        }
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, OnePieceNewWorld.id("pirate_hud"), PirateHud::render);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ELEMENTAL_PROJECTILE.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0f, true));
        event.registerEntityRenderer(ModEntities.PIRATE_GRUNT.get(),
                ctx -> new HumanoidNpcRenderer<>(ctx, OnePieceNewWorld.id("textures/entity/pirate_grunt.png")));
        event.registerEntityRenderer(ModEntities.MARINE_RECRUIT.get(),
                ctx -> new HumanoidNpcRenderer<>(ctx, OnePieceNewWorld.id("textures/entity/marine_recruit.png")));
    }
}
