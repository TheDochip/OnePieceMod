package com.onepiece.newworld.combat;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.abilities.Ability;
import com.onepiece.newworld.abilities.AbilityContext;
import com.onepiece.newworld.abilities.ModAbilities;
import com.onepiece.newworld.abilities.SimpleAbility;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModConfig;
import com.onepiece.newworld.entities.SlashProjectile;
import com.onepiece.newworld.networking.PlayerSync;
import com.onepiece.newworld.progression.PlayerData;
import com.onepiece.newworld.progression.Progression;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Техники меча на клавишах Z / X / C, пока в руке меч (с Shift те же клавиши — способности фрукта).
 * Все три — летящие удары, которые режут врагов и блоки; сила разреза — {@link CutPower}.
 */
public final class SwordTechniques {
    private SwordTechniques() {
    }

    // ---------- сами техники (регистрируются в ModAbilities) ----------

    /** Горизонтальный летящий разрез. */
    public static Ability flyingSlash() {
        return new SimpleAbility(30, 12f, ctx -> fire(ctx, 0));
    }

    /** Вертикальный разрез: шире и сильнее, режет стены сверху вниз. */
    public static Ability verticalSlash() {
        return new SimpleAbility(60, 20f, ctx -> fire(ctx, 1));
    }

    /** Веер из трёх разрезов; с тремя мечами — один огромный удар и два вертикальных. */
    public static Ability stormSlash() {
        return new SimpleAbility(120, 35f, ctx -> fire(ctx, 2));
    }

    public static List<Ability> all() {
        return List.of(ModAbilities.SWORD_FLYING_SLASH.get(), ModAbilities.SWORD_VERTICAL_SLASH.get(),
                ModAbilities.SWORD_STORM_SLASH.get());
    }

    private static boolean fire(AbilityContext ctx, int kind) {
        ServerPlayer player = ctx.player();
        CombatStyle style = CombatStyle.of(player);
        if (style == null || !style.usesSword()) {
            return false;
        }
        PlayerData data = ctx.data();
        float base = CombatService.baseDamage(player, style, data) * ctx.power();
        int power = CutPower.of(player, style);
        boolean black = data.isArmamentActive();
        Vec3 look = player.getLookAngle();
        Vec3 origin = player.getEyePosition().add(look.scale(0.8)).subtract(0, 0.35, 0);

        switch (kind) {
            case 0 -> SlashProjectile.launch(player, origin, look, 1.6f, 2.5f, false, base * 1.2f, power, 25, black);
            case 1 -> SlashProjectile.launch(player, origin, look, 1.4f, 3.5f, true, base * 1.8f, power, 30, black);
            default -> {
                if (style == CombatStyle.THREE_SWORDS) {
                    SlashProjectile.launch(player, origin, look, 1.3f, 6f, false, base * 2.6f, Math.min(CutPower.MAX, power + 1), 35, black);
                    SlashProjectile.launch(player, origin, look.yRot((float) Math.toRadians(12)), 1.5f, 3f, true, base * 1.2f, power, 28, black);
                    SlashProjectile.launch(player, origin, look.yRot((float) Math.toRadians(-12)), 1.5f, 3f, true, base * 1.2f, power, 28, black);
                } else {
                    for (int angle = -15; angle <= 15; angle += 15) {
                        SlashProjectile.launch(player, origin, look.yRot((float) Math.toRadians(angle)), 1.5f, 2.5f, false,
                                base * 1.3f, power, 25, black);
                    }
                }
            }
        }
        ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, kind == 2 ? 0.5f : 0.7f);
        String prefix = style == CombatStyle.THREE_SWORDS ? "three_swords_" : "katana_";
        String anim = switch (kind) {
            case 0 -> "flying_slash";
            case 1 -> "vertical_slash";
            default -> style == CombatStyle.THREE_SWORDS ? "tornado" : "storm_slash";
        };
        CombatService.playAnimation(player, OnePieceNewWorld.id(prefix + anim), 0);
        return true;
    }

    // ---------- запуск с проверками ----------

    /** Серверная проверка и запуск техники в слоте 0..2. */
    public static void tryUse(ServerPlayer player, CombatStyle style, int slot) {
        List<Ability> techniques = all();
        if (slot < 0 || slot >= techniques.size() || !player.isAlive() || Incapacity.check(player)) {
            return;
        }
        Ability ability = techniques.get(slot);
        PlayerData data = ModAttachments.get(player);
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
        int tier = Progression.weaponTier(data.getWeaponMastery(style.id()));
        float power = Progression.abilityPower(data.getLevel(), 0) * (1f + 0.05f * tier);
        if (!ability.activate(new AbilityContext(player, data, null, power))) {
            return;
        }
        if (!free) {
            data.trySpendEnergy(ability.energyCost());
        }
        int cooldown = Math.round(ability.baseCooldown() * (1f - 0.06f * tier)
                * ModConfig.get(ModConfig.COOLDOWN_MULTIPLIER).floatValue());
        data.startCooldown(ability.id(), now, cooldown);
        CombatService.grantWeaponXp(player, style, 2);
        PlayerSync.sync(player);
    }
}
