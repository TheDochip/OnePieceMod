package com.onepiece.newworld.networking;

import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.progression.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlayerSync {
    private PlayerSync() {
    }

    /** Немедленно отправить игроку его данные. */
    public static void sync(ServerPlayer player) {
        PlayerData data = ModAttachments.get(player);
        data.consumeDirty();
        data.consumeEnergyDirty();
        PacketDistributor.sendToPlayer(player, new SyncPlayerDataPayload(data.copy()));
    }
}
