package com.onepiece.newworld.networking;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.progression.Faction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: выбор пути в экране персонажа. Сервер принимает его только один раз. */
public record ChooseFactionPayload(Faction faction) implements CustomPacketPayload {
    public static final Type<ChooseFactionPayload> TYPE = new Type<>(OnePieceNewWorld.id("choose_faction"));
    public static final StreamCodec<ByteBuf, ChooseFactionPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(id -> new ChooseFactionPayload(Faction.byId(id)), p -> p.faction().ordinal());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
