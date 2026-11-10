package com.onepiece.newworld.networking;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.progression.PlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Сервер → клиент: копия данных персонажа для интерфейса. */
public record SyncPlayerDataPayload(PlayerData data) implements CustomPacketPayload {
    public static final Type<SyncPlayerDataPayload> TYPE = new Type<>(OnePieceNewWorld.id("sync_player_data"));
    public static final StreamCodec<FriendlyByteBuf, SyncPlayerDataPayload> STREAM_CODEC =
            PlayerData.STREAM_CODEC.map(SyncPlayerDataPayload::new, SyncPlayerDataPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
