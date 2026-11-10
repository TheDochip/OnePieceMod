package com.onepiece.newworld.core;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.fruits.FruitPowers;
import com.onepiece.newworld.haki.HakiService;
import com.onepiece.newworld.networking.PlayerSync;
import com.onepiece.newworld.progression.PlayerData;
import com.onepiece.newworld.progression.ProgressionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Ежетиковая логика игрока и синхронизация.
 *
 * <p>Оптимизация: тяжёлые проверки (инвентарь на кайросеки, эффекты) — раз в секунду,
 * энергия отправляется клиенту не чаще раза в 10 тиков, перезарядки клиент считает сам.</p>
 */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class PlayerLifecycle {
    private static final int ENERGY_SYNC_INTERVAL = 10;

    private PlayerLifecycle() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        PlayerData data = ModAttachments.get(player);
        boolean exempt = player.isCreative() || player.isSpectator();

        // Пользователь фрукта тонет «как молот». Работает на обеих сторонах, чтобы движение было плавным.
        if (data.hasFruit() && player.isInWater() && !exempt) {
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * 0.6, Math.min(v.y, 0.0) - 0.03, v.z * 0.6);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (data.isArmamentActive()) {
            HakiService.tickArmament(serverPlayer, data);
        } else {
            data.setEnergy(data.getEnergy() + ModConfig.ENERGY_REGEN.get().floatValue());
        }

        DevilFruit fruit = FruitPowers.fruitOf(player);
        if (fruit != null) {
            if (!exempt && player.tickCount % 20 == 0) {
                if (player.isUnderWater()) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false));
                }
                if (FruitPowers.carriesKairoseki(player)) {
                    player.addEffect(new MobEffectInstance(ModEffects.SEASTONE, 40, 0, false, true));
                }
            }
            if (!FruitPowers.isSuppressed(player)) {
                fruit.passiveTick(serverPlayer, data);
            }
        }

        if (player.tickCount % 200 == 0) {
            data.pruneCooldowns(serverPlayer.serverLevel().getGameTime());
        }

        if (data.consumeDirty()) {
            PlayerSync.sync(serverPlayer);
        } else if (player.tickCount % ENERGY_SYNC_INTERVAL == 0 && data.consumeEnergyDirty()) {
            PlayerSync.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProgressionService.applyLevelHealth(player, ModAttachments.get(player));
            PlayerSync.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerData data = ModAttachments.get(player);
            data.onRespawn();
            ProgressionService.applyLevelHealth(player, data);
            player.setHealth(player.getMaxHealth());
            PlayerSync.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerSync.sync(player);
        }
    }
}
