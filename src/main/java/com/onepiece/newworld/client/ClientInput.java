package com.onepiece.newworld.client;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.client.screen.CharacterScreen;
import com.onepiece.newworld.networking.PlayerActionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Нажатия клавиш. Клиент только сообщает серверу, что нажато; решает всё сервер. */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID, value = Dist.CLIENT)
public final class ClientInput {
    private ClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        for (int slot = 0; slot < ModKeys.ABILITY_SLOTS.length; slot++) {
            while (ModKeys.ABILITY_SLOTS[slot].consumeClick()) {
                send(PlayerActionPayload.Action.ABILITY, slot);
            }
        }
        while (ModKeys.ARMAMENT.consumeClick()) {
            send(PlayerActionPayload.Action.TOGGLE_ARMAMENT, 0);
        }
        while (ModKeys.OBSERVATION.consumeClick()) {
            send(PlayerActionPayload.Action.OBSERVATION_PULSE, 0);
        }
        while (ModKeys.CONQUEROR.consumeClick()) {
            send(PlayerActionPayload.Action.CONQUEROR_WAVE, 0);
        }
        while (ModKeys.CHARACTER.consumeClick()) {
            mc.setScreen(new CharacterScreen());
        }
    }

    private static void send(PlayerActionPayload.Action action, int slot) {
        PacketDistributor.sendToServer(new PlayerActionPayload(action, slot));
    }
}
