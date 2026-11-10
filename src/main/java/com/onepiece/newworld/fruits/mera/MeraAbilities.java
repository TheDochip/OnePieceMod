package com.onepiece.newworld.fruits.mera;

import com.onepiece.newworld.abilities.Ability;
import com.onepiece.newworld.abilities.SimpleAbility;
import com.onepiece.newworld.abilities.Targeting;
import com.onepiece.newworld.entities.ElementalProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Способности Мера Мера: огненные снаряды, лучи и взрывы. */
public final class MeraAbilities {
    private MeraAbilities() {
    }

    /** Огненный снаряд: летит прямо и взрывается пламенем при попадании. */
    public static Ability fireBullet() {
        return new SimpleAbility(40, 15f, ctx -> {
            ElementalProjectile.shoot(ctx.player(), ElementalProjectile.Element.FIRE, ctx.damage(7f), 1.8f);
            ctx.sound(SoundEvents.BLAZE_SHOOT, 1f, 1.1f);
            return true;
        });
    }

    /** Огненный кулак: огненный луч, прошивающий всех на пути. */
    public static Ability fireFist() {
        return new SimpleAbility(100, 30f, ctx -> {
            ServerPlayer player = ctx.player();
            ServerLevel level = ctx.level();
            Vec3 from = player.getEyePosition().add(0, -0.2, 0);
            Vec3 to = Targeting.rayEnd(player, 26);
            Targeting.line(level, ParticleTypes.FLAME, from, to, 0.35, 3, 0.25);
            Targeting.line(level, ParticleTypes.LAVA, from, to, 2.0, 1, 0.2);
            for (LivingEntity target : Targeting.onRay(player, 26, 1.0)) {
                ctx.hurt(target, 10f);
                target.igniteForSeconds(6);
            }
            ctx.sound(SoundEvents.BLAZE_SHOOT, 1f, 0.7f);
            ctx.sound(SoundEvents.FIRECHARGE_USE, 1f, 0.8f);
            return true;
        });
    }

    /** Огненный столп: волна пламени вокруг игрока и колонна огня вверх. */
    public static Ability firePillar() {
        return new SimpleAbility(160, 35f, ctx -> {
            ServerPlayer player = ctx.player();
            ServerLevel level = ctx.level();
            Vec3 center = player.position().add(0, 0.3, 0);
            for (double r = 1.5; r <= 7; r += 1.5) {
                Targeting.ring(level, ParticleTypes.FLAME, center, r, (int) (r * 10));
            }
            for (double y = 0; y < 6; y += 0.5) {
                level.sendParticles(ParticleTypes.FLAME, center.x, center.y + y, center.z, 4, 0.4, 0.1, 0.4, 0.02);
            }
            level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 12, 2, 0.5, 2, 0);
            for (LivingEntity target : Targeting.around(player, center, 7)) {
                ctx.hurt(target, 7f);
                target.igniteForSeconds(8);
                Targeting.knockAway(player, target, 0.7);
                target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45, 0));
                target.hurtMarked = true;
            }
            ctx.sound(SoundEvents.FIRECHARGE_USE, 1.2f, 0.6f);
            return true;
        });
    }
}
