package com.onepiece.newworld.client.anim;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.client.ClientCombat;
import com.onepiece.newworld.networking.ClientPayloads;
import com.onepiece.newworld.networking.PlayAnimationPayload;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.enums.PlayState;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Проигрывание анимаций игрока через Player Animation Library.
 *
 * <p>У каждого игрока на клиенте есть слой {@link #LAYER} с контроллером, который сам ничего не играет
 * ({@link PlayState#STOP}) — анимации запускаются только по команде сервера ({@link PlayAnimationPayload}).
 * Файлы анимаций лежат в {@code assets/onepiece_newworld/player_animations/*.json}; id анимации —
 * {@code onepiece_newworld:<ключ анимации в JSON>}.</p>
 *
 * <p>Если нужной анимации ещё нет (модели и анимации в работе), игрок просто машет рукой как в ванилле.</p>
 */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID, value = Dist.CLIENT)
public final class PlayerAnimBridge {
    public static final ResourceLocation LAYER = OnePieceNewWorld.id("combat");
    /** Выше обычных анимаций других модов, чтобы удар не перебивался ходьбой. */
    private static final int PRIORITY = 1500;

    private PlayerAnimBridge() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, PRIORITY,
                    player -> new PlayerAnimationController(player, (controller, state, setter) -> PlayState.STOP));
            ClientPayloads.setAnimationHandler(PlayerAnimBridge::handle);
        });
    }

    private static void handle(PlayAnimationPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(payload.entityId());
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        if (player == mc.player) {
            ClientCombat.onServerAnimation(payload.combo());
        }
        ResourceLocation animation = payload.animation().orElse(null);
        if (!play(player, animation) && animation != null && !isPose(animation)) {
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    /** Блок и парирование без своей анимации не превращаем во взмах руки. */
    private static boolean isPose(ResourceLocation animation) {
        String path = animation.getPath();
        return path.endsWith("_block") || path.endsWith("_parry") || path.startsWith("common_d");
    }

    /**
     * @param animation id анимации или null, чтобы остановить текущую
     * @return true, если библиотека проиграла (или остановила) анимацию
     */
    public static boolean play(AbstractClientPlayer player, @Nullable ResourceLocation animation) {
        IAnimation layer;
        try {
            layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER);
        } catch (IllegalArgumentException mixinsFailed) {
            return false;
        }
        if (!(layer instanceof PlayerAnimationController controller)) {
            return false;
        }
        if (animation == null) {
            controller.stop();
            return true;
        }
        if (!PlayerAnimResources.hasAnimation(animation)) {
            return false;
        }
        return controller.triggerAnimation(animation);
    }
}
