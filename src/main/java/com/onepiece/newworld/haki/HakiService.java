package com.onepiece.newworld.haki;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.abilities.Targeting;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModConfig;
import com.onepiece.newworld.networking.PlayerSync;
import com.onepiece.newworld.progression.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Хаки Наблюдения и Вооружения (базовые версии). Пробуждаются в бою:
 * Вооружение — за удары врукопашную, Наблюдение — за полученные удары.
 */
public final class HakiService {
    public static final float OBSERVATION_COST = 25f;
    public static final int OBSERVATION_COOLDOWN = 20 * 20;
    /** Id перезарядки импульса Наблюдения в общей таблице перезарядок. */
    public static final ResourceLocation OBSERVATION_COOLDOWN_ID = OnePieceNewWorld.id("haki/observation_pulse");
    public static final float ARMAMENT_MIN_ENERGY = 10f;

    private HakiService() {
    }

    /** Импульс Наблюдения: подсвечивает всех существ в радиусе, растущем с уровнем. */
    public static void observationPulse(ServerPlayer player) {
        PlayerData data = ModAttachments.get(player);
        int level = data.getHakiLevel(HakiType.OBSERVATION);
        if (level <= 0) {
            hint(player, "message.onepiece_newworld.haki_locked.observation", ChatFormatting.GRAY);
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (data.cooldownRemaining(OBSERVATION_COOLDOWN_ID, now) > 0) {
            return;
        }
        if (!player.isCreative() && !data.trySpendEnergy(OBSERVATION_COST)) {
            hint(player, "message.onepiece_newworld.no_energy", ChatFormatting.RED);
            return;
        }
        double radius = 16 + 8 * level;
        int duration = 20 * (6 + 2 * level);
        int found = 0;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive())) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false));
            found++;
        }
        Targeting.ring(player.serverLevel(), ParticleTypes.END_ROD, player.position().add(0, 1, 0), 2.5, 24);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.4f);
        player.displayClientMessage(Component.translatable("message.onepiece_newworld.observation_pulse", found)
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        data.startCooldown(OBSERVATION_COOLDOWN_ID, now, OBSERVATION_COOLDOWN);
        PlayerSync.sync(player);
    }

    /** Включить/выключить Хаки Вооружения. Пока включено, тратит энергию каждый тик. */
    public static void toggleArmament(ServerPlayer player) {
        PlayerData data = ModAttachments.get(player);
        if (data.getHakiLevel(HakiType.ARMAMENT) <= 0) {
            hint(player, "message.onepiece_newworld.haki_locked.armament", ChatFormatting.GRAY);
            return;
        }
        boolean enable = !data.isArmamentActive();
        if (enable && data.getEnergy() < ARMAMENT_MIN_ENERGY) {
            hint(player, "message.onepiece_newworld.no_energy", ChatFormatting.RED);
            return;
        }
        data.setArmamentActive(enable);
        player.level().playSound(null, player.blockPosition(),
                enable ? SoundEvents.ANVIL_PLACE : SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, enable ? 1.6f : 2.0f);
        hint(player, enable ? "message.onepiece_newworld.armament_on" : "message.onepiece_newworld.armament_off",
                ChatFormatting.DARK_GRAY);
        PlayerSync.sync(player);
    }

    /** Тик Вооружения: расход энергии и автоотключение. */
    public static void tickArmament(ServerPlayer player, PlayerData data) {
        if (!data.isArmamentActive()) {
            return;
        }
        if (!player.isCreative()) {
            data.setEnergy(data.getEnergy() - ModConfig.ARMAMENT_DRAIN.get().floatValue());
        }
        if (data.getEnergy() <= 0) {
            data.setArmamentActive(false);
            hint(player, "message.onepiece_newworld.armament_exhausted", ChatFormatting.RED);
        }
    }

    /** Начислить опыт Хаки и сообщить о новом уровне. */
    public static void grantXp(ServerPlayer player, HakiType type, int baseAmount) {
        PlayerData data = ModAttachments.get(player);
        int amount = Math.max(1, (int) Math.round(baseAmount * ModConfig.HAKI_XP_MULTIPLIER.get()));
        if (data.addHakiXp(type, amount)) {
            int level = data.getHakiLevel(type);
            String key = level == 1
                    ? "message.onepiece_newworld.haki_awakened." + type.getSerializedName()
                    : "message.onepiece_newworld.haki_level." + type.getSerializedName();
            player.displayClientMessage(Component.translatable(key, level).withStyle(ChatFormatting.GOLD), false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.8f);
            PlayerSync.sync(player);
        }
    }

    private static void hint(ServerPlayer player, String key, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(key).withStyle(color), true);
    }
}
