package com.onepiece.newworld.combat;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModEffects;
import com.onepiece.newworld.core.ModItems;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.fruits.FruitPowers;
import com.onepiece.newworld.haki.HakiService;
import com.onepiece.newworld.haki.HakiType;
import com.onepiece.newworld.progression.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Правила урона: Хаки Вооружения, морской камень, иммунитеты фруктов,
 * неуязвимость Логии и уклонение Наблюдением. Всё считается на сервере.
 */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class DamageRules {
    /** Энергия, которую Логия тратит, чтобы пропустить удар сквозь тело. */
    public static final float LOGIA_DODGE_COST = 8f;
    /** Бонус урона Вооружения за уровень. */
    public static final float ARMAMENT_BONUS_PER_LEVEL = 0.12f;

    private DamageRules() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        boolean melee = attacker != null && source.getDirectEntity() == attacker;

        if (attacker instanceof ServerPlayer attackerPlayer && attacker != victim) {
            applyAttackerBonuses(event, attackerPlayer, victim, melee);
        }
        if (victim instanceof ServerPlayer victimPlayer) {
            applyVictimRules(event, victimPlayer, source, attacker);
        }
    }

    private static void applyAttackerBonuses(LivingIncomingDamageEvent event, ServerPlayer attacker, LivingEntity victim, boolean melee) {
        PlayerData data = ModAttachments.get(attacker);
        if (data.isArmamentActive()) {
            event.setAmount(event.getAmount() * (1f + ARMAMENT_BONUS_PER_LEVEL * data.getHakiLevel(HakiType.ARMAMENT)));
        }
        if (melee) {
            if (attacker.getMainHandItem().is(ModItems.KAIROSEKI_TAG)) {
                victim.addEffect(new MobEffectInstance(ModEffects.SEASTONE, 100, 0));
            }
            HakiService.grantXp(attacker, HakiType.ARMAMENT, 1 + (int) (event.getAmount() / 5f));
        }
    }

    private static void applyVictimRules(LivingIncomingDamageEvent event, ServerPlayer victim, DamageSource source, Entity attacker) {
        PlayerData data = ModAttachments.get(victim);
        DevilFruit fruit = FruitPowers.fruitOf(victim);
        boolean fromLiving = attacker instanceof LivingEntity && attacker != victim;

        if (fruit != null && !FruitPowers.isSuppressed(victim)) {
            if (fruit.isImmuneTo(source)) {
                event.setCanceled(true);
                return;
            }
            // Логия: удар проходит сквозь тело, если нападающий без Хаки Вооружения и не бьёт морским камнем.
            if (fruit.type().isLogia() && (fromLiving || source.is(DamageTypeTags.IS_EXPLOSION))
                    && !bypassesLogia(attacker) && data.getEnergy() >= LOGIA_DODGE_COST) {
                if (!victim.isCreative()) {
                    data.trySpendEnergy(LOGIA_DODGE_COST);
                }
                victim.serverLevel().sendParticles(fruit.elementParticle(), victim.getX(), victim.getY() + 1, victim.getZ(),
                        20, 0.3, 0.6, 0.3, 0.05);
                event.setCanceled(true);
                return;
            }
        }

        if (fromLiving) {
            int observation = data.getHakiLevel(HakiType.OBSERVATION);
            if (observation > 0 && data.getEnergy() >= 5f && victim.getRandom().nextFloat() < 0.04f * observation) {
                data.trySpendEnergy(5f);
                victim.displayClientMessage(Component.translatable("message.onepiece_newworld.dodged")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
                victim.level().playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1f, 1.5f);
                event.setCanceled(true);
                return;
            }
            HakiService.grantXp(victim, HakiType.OBSERVATION, 2);
        }
    }

    /** Может ли нападающий ударить Логию. */
    public static boolean bypassesLogia(Entity attacker) {
        if (attacker instanceof LivingEntity living && living.getMainHandItem().is(ModItems.KAIROSEKI_TAG)) {
            return true;
        }
        return attacker instanceof Player player && ModAttachments.get(player).isArmamentActive();
    }
}
