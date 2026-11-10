package com.onepiece.newworld.networking;

import com.onepiece.newworld.OnePieceNewWorld;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Клиент → сервер: действие ближнего боя. Для уклонения передаётся желаемое направление
 * в мировых координатах (сервер сам нормализует и проверяет его).
 */
public record CombatActionPayload(Action action, float dirX, float dirZ) implements CustomPacketPayload {
    public enum Action {
        ATTACK, HEAVY_ATTACK, BLOCK_START, BLOCK_STOP, DODGE;

        static Action byId(int id) {
            Action[] values = values();
            return id >= 0 && id < values.length ? values[id] : ATTACK;
        }
    }

    public static final Type<CombatActionPayload> TYPE = new Type<>(OnePieceNewWorld.id("combat_action"));
    public static final StreamCodec<ByteBuf, CombatActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.action().ordinal(),
            ByteBufCodecs.FLOAT, CombatActionPayload::dirX,
            ByteBufCodecs.FLOAT, CombatActionPayload::dirZ,
            (action, x, z) -> new CombatActionPayload(Action.byId(action), x, z));

    public CombatActionPayload(Action action) {
        this(action, 0f, 0f);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
