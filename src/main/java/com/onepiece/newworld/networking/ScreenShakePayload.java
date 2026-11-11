package com.onepiece.newworld.networking;

import com.onepiece.newworld.OnePieceNewWorld;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Сервер → клиент: потрясти камеру.
 *
 * @param intensity сила, градусы наклона (0.1–3)
 * @param ticks     длительность
 */
public record ScreenShakePayload(float intensity, int ticks) implements CustomPacketPayload {
    public static final Type<ScreenShakePayload> TYPE = new Type<>(OnePieceNewWorld.id("screen_shake"));
    public static final StreamCodec<ByteBuf, ScreenShakePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ScreenShakePayload::intensity,
            ByteBufCodecs.VAR_INT, ScreenShakePayload::ticks,
            ScreenShakePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
