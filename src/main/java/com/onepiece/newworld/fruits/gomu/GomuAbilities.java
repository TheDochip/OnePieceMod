package com.onepiece.newworld.fruits.gomu;

import com.onepiece.newworld.abilities.Ability;
import com.onepiece.newworld.abilities.SimpleAbility;
import com.onepiece.newworld.abilities.Targeting;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Способности Гому Гому: удары за счёт растяжения тела. */
public final class GomuAbilities {
    private GomuAbilities() {
    }

    /** Резиновый пистолет: дальний удар по первой цели на линии взгляда. */
    public static Ability pistol() {
        return new SimpleAbility(30, 12f, ctx -> {
            ServerPlayer player = ctx.player();
            Vec3 from = player.getEyePosition().add(0, -0.3, 0);
            List<LivingEntity> targets = Targeting.onRay(player, 16, 0.6);
            Vec3 to = targets.isEmpty() ? Targeting.rayEnd(player, 16) : targets.get(0).getBoundingBox().getCenter();
            Targeting.line(ctx.level(), ParticleTypes.CLOUD, from, to, 0.6, 1, 0.02);
            ctx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.8f);
            if (!targets.isEmpty()) {
                LivingEntity target = targets.get(0);
                ctx.hurt(target, 8f);
                Targeting.knockAway(player, target, 1.4);
                ctx.level().sendParticles(ParticleTypes.EXPLOSION, to.x, to.y, to.z, 1, 0, 0, 0, 0);
            }
            return true;
        });
    }

    /** Резиновая ракета: игрок выстреливает себя вперёд. Урона от падения нет — тело резиновое. */
    public static Ability rocket() {
        return new SimpleAbility(60, 20f, ctx -> {
            ServerPlayer player = ctx.player();
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * 2.6, Math.max(0.5, look.y * 2.0 + 0.4), look.z * 2.6);
            player.hurtMarked = true;
            player.fallDistance = 0;
            ctx.level().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 16, 0.3, 0.1, 0.3, 0.05);
            ctx.sound(SoundEvents.ENDER_DRAGON_FLAP, 1f, 1.4f);
            return true;
        });
    }

    /** Резиновый гатлинг: шквал ударов по всем в конусе перед игроком. */
    public static Ability gatling() {
        return new SimpleAbility(120, 35f, ctx -> {
            ServerPlayer player = ctx.player();
            ServerLevel level = ctx.level();
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            for (int i = 0; i < 10; i++) {
                Vec3 p = eye.add(look.scale(1.5 + player.getRandom().nextDouble() * 3.5))
                        .add(player.getRandom().nextGaussian() * 0.6, player.getRandom().nextGaussian() * 0.4,
                                player.getRandom().nextGaussian() * 0.6);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            for (LivingEntity target : Targeting.inCone(player, 6, 0.55)) {
                ctx.hurt(target, 11f);
                Targeting.knockAway(player, target, 0.9);
            }
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f);
            ctx.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.7f);
            return true;
        });
    }
}
