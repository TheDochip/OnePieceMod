package com.onepiece.newworld.networking;

import com.onepiece.newworld.abilities.AbilityService;
import com.onepiece.newworld.combat.CombatService;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.haki.HakiService;
import com.onepiece.newworld.progression.FactionService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Регистрация пакетов. Обработчики выполняются в главном потоке игры. */
public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");

        registrar.playToClient(SyncPlayerDataPayload.TYPE, SyncPlayerDataPayload.STREAM_CODEC,
                (payload, context) -> context.player().setData(ModAttachments.PLAYER_DATA, payload.data()));

        registrar.playToServer(PlayerActionPayload.TYPE, PlayerActionPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action()) {
                case ABILITY -> AbilityService.tryActivate(player, payload.slot());
                case TOGGLE_ARMAMENT -> HakiService.toggleArmament(player);
                case OBSERVATION_PULSE -> HakiService.observationPulse(player);
            }
        });

        registrar.playToClient(PlayAnimationPayload.TYPE, PlayAnimationPayload.STREAM_CODEC,
                (payload, context) -> ClientPayloads.handleAnimation(payload));

        registrar.playToServer(CombatActionPayload.TYPE, CombatActionPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action()) {
                case ATTACK -> CombatService.attack(player, false);
                case HEAVY_ATTACK -> CombatService.attack(player, true);
                case BLOCK_START -> CombatService.startBlocking(player);
                case BLOCK_STOP -> CombatService.stopBlocking(player);
                case DODGE -> CombatService.dodge(player, payload.dirX(), payload.dirZ());
            }
        });

        registrar.playToServer(ChooseFactionPayload.TYPE, ChooseFactionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                FactionService.choose(player, payload.faction());
            }
        });
    }
}
