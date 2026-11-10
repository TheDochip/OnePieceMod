package com.onepiece.newworld.fruits.hie;

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

/** Способности Хиэ Хиэ: заморозка целей и воды. */
public final class HieAbilities {
    private HieAbilities() {
    }

    /** Ледяной осколок: снаряд, замораживающий цель. */
    public static Ability iceShard() {
        return new SimpleAbility(40, 15f, ctx -> {
            ElementalProjectile.shoot(ctx.player(), ElementalProjectile.Element.ICE, ctx.damage(6f), 2.0f);
            ctx.sound(SoundEvents.SNOW_GOLEM_SHOOT, 1f, 0.7f);
            return true;
        });
    }

    /** Ледниковый период: замораживает воду и всех вокруг. */
    public static Ability iceAge() {
        return new SimpleAbility(300, 45f, ctx -> {
            ServerPlayer player = ctx.player();
            ServerLevel level = ctx.level();
            int radius = 10;
            IceHelper.freezeWater(level, player.blockPosition(), radius, 3, 200, 400, player.getRandom());
            Vec3 c = player.position().add(0, 0.2, 0);
            for (double r = 1.5; r <= radius; r += 2) {
                Targeting.ring(level, ParticleTypes.SNOWFLAKE, c, r, (int) (r * 8));
            }
            for (LivingEntity target : Targeting.around(player, c, radius)) {
                ctx.hurt(target, 5f);
                IceHelper.freeze(target, 160, 4);
            }
            ctx.sound(SoundEvents.GLASS_BREAK, 1f, 0.5f);
            ctx.sound(SoundEvents.PLAYER_HURT_FREEZE, 1f, 0.6f);
            return true;
        });
    }

    /** Ледяные копья: залп пик в конусе перед игроком. */
    public static Ability iceSpears() {
        return new SimpleAbility(120, 30f, ctx -> {
            ServerPlayer player = ctx.player();
            ServerLevel level = ctx.level();
            Vec3 eye = player.getEyePosition();
            for (int i = 0; i < 6; i++) {
                Vec3 dir = player.getLookAngle().add(player.getRandom().nextGaussian() * 0.15,
                        player.getRandom().nextGaussian() * 0.08, player.getRandom().nextGaussian() * 0.15).normalize();
                Targeting.line(level, ParticleTypes.SNOWFLAKE, eye, eye.add(dir.scale(9)), 0.5, 1, 0.03);
            }
            for (LivingEntity target : Targeting.inCone(player, 9, 0.75)) {
                ctx.hurt(target, 9f);
                IceHelper.freeze(target, 60, 2);
            }
            ctx.sound(SoundEvents.GLASS_BREAK, 1f, 0.9f);
            ctx.sound(SoundEvents.SNOW_GOLEM_SHOOT, 1f, 1.2f);
            return true;
        });
    }
}
