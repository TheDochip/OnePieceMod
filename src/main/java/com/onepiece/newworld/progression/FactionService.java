package com.onepiece.newworld.progression;

import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.networking.PlayerSync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Выбор пути: пират или дозорный. */
public final class FactionService {
    private FactionService() {
    }

    /** Выбрать путь можно один раз. Сменить его может только оператор командой /onepiece faction. */
    public static void choose(ServerPlayer player, Faction faction) {
        PlayerData data = ModAttachments.get(player);
        if (faction == Faction.NONE || data.getFaction() != Faction.NONE) {
            return;
        }
        if (faction == Faction.MARINE && data.getBounty() > 0) {
            player.displayClientMessage(Component.translatable("message.onepiece_newworld.marine_refused")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        data.setFaction(faction);
        player.displayClientMessage(Component.translatable("message.onepiece_newworld.faction_chosen." + faction.getSerializedName())
                .withStyle(ChatFormatting.GOLD), false);
        player.level().playSound(null, player.blockPosition(),
                faction == Faction.PIRATE ? SoundEvents.PILLAGER_CELEBRATE : SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.8f, 1f);
        PlayerSync.sync(player);
    }
}
