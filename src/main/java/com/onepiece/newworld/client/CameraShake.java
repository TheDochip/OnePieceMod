package com.onepiece.newworld.client;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.networking.ClientPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Тряска камеры (волна и столкновение Королевского Хаки). Затухает к концу. */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID, value = Dist.CLIENT)
public final class CameraShake {
    private static final RandomSource RANDOM = RandomSource.create();
    private static float intensity;
    private static int ticksLeft;
    private static int duration = 1;

    private CameraShake() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientPayloads.setShakeHandler(payload -> Minecraft.getInstance().execute(() -> start(payload.intensity(), payload.ticks())));
    }

    public static void start(float strength, int ticks) {
        if (ticks <= 0) {
            return;
        }
        // Более сильная тряска перекрывает слабую.
        if (ticksLeft <= 0 || strength >= intensity) {
            intensity = Mth.clamp(strength, 0f, 3f);
            duration = ticks;
            ticksLeft = ticks;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ticksLeft > 0) {
            ticksLeft--;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (ticksLeft <= 0) {
            return;
        }
        float fade = (float) ticksLeft / duration;
        float amount = intensity * fade;
        event.setYaw(event.getYaw() + (RANDOM.nextFloat() - 0.5f) * amount);
        event.setPitch(event.getPitch() + (RANDOM.nextFloat() - 0.5f) * amount);
        event.setRoll(event.getRoll() + (RANDOM.nextFloat() - 0.5f) * amount * 1.5f);
    }
}
