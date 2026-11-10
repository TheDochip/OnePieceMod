package com.onepiece.newworld.networking;

import com.onepiece.newworld.OnePieceNewWorld;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Сервер → клиенты: проиграть анимацию игрока (или остановить, если анимации нет).
 *
 * @param entityId  id игрока-сущности
 * @param animation id анимации Player Animation Library
 * @param combo     счётчик попаданий подряд (для интерфейса самого игрока)
 */
public record PlayAnimationPayload(int entityId, Optional<ResourceLocation> animation, int combo) implements CustomPacketPayload {
    public static final Type<PlayAnimationPayload> TYPE = new Type<>(OnePieceNewWorld.id("play_animation"));
    public static final StreamCodec<ByteBuf, PlayAnimationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayAnimationPayload::entityId,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), PlayAnimationPayload::animation,
            ByteBufCodecs.VAR_INT, PlayAnimationPayload::combo,
            PlayAnimationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
