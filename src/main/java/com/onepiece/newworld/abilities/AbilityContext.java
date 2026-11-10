package com.onepiece.newworld.abilities;

import com.onepiece.newworld.core.ModConfig;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.progression.PlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Всё, что нужно способности в момент срабатывания.
 *
 * @param power множитель силы: растёт с уровнем персонажа и мастерством фрукта
 */
public record AbilityContext(ServerPlayer player, PlayerData data, DevilFruit fruit, float power) {

    public ServerLevel level() {
        return player.serverLevel();
    }

    /** Итоговый урон с учётом силы игрока и настроек сервера. */
    public float damage(float base) {
        return (float) (base * power * ModConfig.DAMAGE_MULTIPLIER.get());
    }

    /** Нанести урон как атака игрока: так к нему применяются Хаки, броня и правила Логии. */
    public boolean hurt(LivingEntity target, float baseDamage) {
        return target.hurt(player.damageSources().playerAttack(player), damage(baseDamage));
    }

    public void sound(SoundEvent sound, float volume, float pitch) {
        level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }
}
