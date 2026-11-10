package com.onepiece.newworld.abilities;

import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModConfig;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.fruits.FruitPowers;
import com.onepiece.newworld.networking.PlayerSync;
import com.onepiece.newworld.progression.PlayerData;
import com.onepiece.newworld.progression.Progression;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Серверная проверка и запуск способностей. Клиент присылает только номер слота —
 * все проверки (фрукт, перезарядка, энергия, подавление) выполняются здесь.
 */
public final class AbilityService {
    private AbilityService() {
    }

    public static void tryActivate(ServerPlayer player, int slot) {
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        PlayerData data = ModAttachments.get(player);
        DevilFruit fruit = FruitPowers.fruitOf(player);
        if (fruit == null || slot < 0 || slot >= fruit.abilityCount()) {
            return;
        }
        if (FruitPowers.isSuppressed(player)) {
            player.displayClientMessage(Component.translatable("message.onepiece_newworld.suppressed")
                    .withStyle(ChatFormatting.AQUA), true);
            return;
        }
        Ability ability = fruit.ability(slot);
        long now = player.serverLevel().getGameTime();
        if (data.cooldownRemaining(ability.id(), now) > 0) {
            return;
        }
        boolean free = player.isCreative();
        if (!free && data.getEnergy() < ability.energyCost()) {
            player.displayClientMessage(Component.translatable("message.onepiece_newworld.no_energy")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        int mastery = data.getMastery(fruit.id());
        AbilityContext context = new AbilityContext(player, data, fruit,
                Progression.abilityPower(data.getLevel(), mastery));
        if (!ability.activate(context)) {
            return;
        }
        if (!free) {
            data.trySpendEnergy(ability.energyCost());
        }
        int cooldown = Math.round(ability.baseCooldown()
                * Progression.cooldownFactor(mastery)
                * ModConfig.COOLDOWN_MULTIPLIER.get().floatValue());
        data.startCooldown(ability.id(), now, cooldown);
        data.addMastery(fruit.id(), 1);
        PlayerSync.sync(player);
    }
}
