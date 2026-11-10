package com.onepiece.newworld.networking;

import com.onepiece.newworld.OnePieceNewWorld;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Клиент → сервер: игрок нажал клавишу. Передаётся только «что нажато» —
 * сервер сам решает, можно ли это сделать.
 *
 * @param action одно из {@link Action}
 * @param slot   номер слота способности (для {@link Action#ABILITY})
 */
public record PlayerActionPayload(Action action, int slot) implements CustomPacketPayload {
    public enum Action {
        ABILITY, TOGGLE_ARMAMENT, OBSERVATION_PULSE;

        static Action byId(int id) {
            Action[] values = values();
            return id >= 0 && id < values.length ? values[id] : ABILITY;
        }
    }

    public static final Type<PlayerActionPayload> TYPE = new Type<>(OnePieceNewWorld.id("player_action"));
    public static final StreamCodec<ByteBuf, PlayerActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.action().ordinal(),
            ByteBufCodecs.VAR_INT, PlayerActionPayload::slot,
            (action, slot) -> new PlayerActionPayload(Action.byId(action), slot));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
