package com.onepiece.newworld.combat;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModEffects;
import com.onepiece.newworld.progression.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Защита в ближнем бою. Срабатывает раньше остальных правил урона (приоритет HIGH):
 * <ol>
 *     <li>оглушённый не может нанести удар;</li>
 *     <li>во время уклонения урон не проходит;</li>
 *     <li>удар в первые {@link CombatService#PARRY_WINDOW} тиков блока — парирование:
 *     урона нет, нападающий оглушён, следующий удар — усиленная контратака;</li>
 *     <li>обычный блок спереди снижает урон и тратит энергию; без энергии блок пробивается.</li>
 * </ol>
 */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class DefenseRules {
    private DefenseRules() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide) {
            CombatService.tick(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        // Оглушённый не может бить в ближнем бою.
        if (attacker instanceof LivingEntity living && source.getDirectEntity() == attacker
                && living.hasEffect(ModEffects.STUNNED)) {
            event.setCanceled(true);
            return;
        }
        if (!(victim instanceof Player player)) {
            return;
        }
        CombatState state = ModAttachments.combat(player);
        long now = level.getGameTime();

        if (state.isDodging(now) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
            return;
        }
        if (!state.isBlocking() || source.is(DamageTypeTags.BYPASSES_SHIELD) || !isFrontal(player, source)) {
            return;
        }
        CombatStyle style = CombatStyle.of(player);
        if (style == null) {
            CombatService.stopBlocking(player);
            return;
        }

        if (now - state.blockStartedAt() <= CombatService.PARRY_WINDOW) {
            parry(level, player, state, style, attacker, now);
            event.setCanceled(true);
            return;
        }

        float amount = event.getAmount();
        float leaked = amount * style.blockLeak();
        float cost = (amount - leaked) * CombatService.BLOCK_COST_PER_DAMAGE;
        PlayerData data = ModAttachments.get(player);
        if (player.getAbilities().instabuild || data.trySpendEnergy(cost)) {
            event.setAmount(leaked);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 0.9f, style.usesSword() ? 1.4f : 0.9f);
            return;
        }
        // Энергии не хватило — блок пробит.
        CombatService.stopBlocking(player);
        player.addEffect(new MobEffectInstance(ModEffects.STUNNED, CombatService.GUARD_BREAK_STUN, 0, false, true));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BREAK,
                SoundSource.PLAYERS, 1f, 1f);
        player.displayClientMessage(Component.translatable("message.onepiece_newworld.guard_broken")
                .withStyle(ChatFormatting.RED), true);
    }

    private static void parry(ServerLevel level, Player player, CombatState state, CombatStyle style, Entity attacker, long now) {
        state.counterUntil = now + CombatService.COUNTER_WINDOW;
        if (attacker instanceof LivingEntity living && living != player) {
            living.addEffect(new MobEffectInstance(ModEffects.STUNNED, CombatService.PARRY_STUN, 0, false, true), player);
            living.knockback(0.5, player.getX() - living.getX(), player.getZ() - living.getZ());
        }
        Vec3 look = player.getLookAngle();
        level.sendParticles(ParticleTypes.CRIT, player.getX() + look.x, player.getEyeY() - 0.3, player.getZ() + look.z,
                12, 0.2, 0.2, 0.2, 0.3);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.5f, 1.8f);
        player.displayClientMessage(Component.translatable("message.onepiece_newworld.parry")
                .withStyle(ChatFormatting.GOLD), true);
        CombatService.grantWeaponXp(player, style, 3);
        CombatService.playAnimation(player, OnePieceNewWorld.id(style == CombatStyle.FISTS ? "fists_parry" : "katana_parry"), 0);
    }

    /** Удар пришёл спереди (±~78° от взгляда). Источник без позиции (падение, голод) не блокируется. */
    static boolean isFrontal(Player player, DamageSource source) {
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return false;
        }
        Vec3 to = new Vec3(from.x - player.getX(), 0, from.z - player.getZ());
        if (to.lengthSqr() < 1.0E-4) {
            return true;
        }
        Vec3 view = player.getViewVector(1f);
        Vec3 flatView = new Vec3(view.x, 0, view.z);
        if (flatView.lengthSqr() < 1.0E-4) {
            return true;
        }
        return to.normalize().dot(flatView.normalize()) >= 0.2;
    }
}
