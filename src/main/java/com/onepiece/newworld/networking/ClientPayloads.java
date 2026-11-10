package com.onepiece.newworld.networking;

import java.util.function.Consumer;

/**
 * Мост к клиентскому коду. Общий код (он есть и на выделенном сервере) не должен ссылаться
 * на клиентские классы, поэтому обработчик подставляет клиент при запуске.
 */
public final class ClientPayloads {
    private static Consumer<PlayAnimationPayload> animationHandler = payload -> {
    };

    private ClientPayloads() {
    }

    public static void setAnimationHandler(Consumer<PlayAnimationPayload> handler) {
        animationHandler = handler;
    }

    static void handleAnimation(PlayAnimationPayload payload) {
        animationHandler.accept(payload);
    }
}
