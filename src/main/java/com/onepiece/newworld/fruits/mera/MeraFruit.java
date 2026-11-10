package com.onepiece.newworld.fruits.mera;

import com.onepiece.newworld.abilities.Ability;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.fruits.FruitType;
import com.onepiece.newworld.progression.PlayerData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/** Мера Мера: тело из огня. Огонь и лава не вредят, горение сразу гаснет. */
public class MeraFruit extends DevilFruit {
    public MeraFruit(List<Supplier<? extends Ability>> abilities) {
        super(FruitType.LOGIA, 0xFF7A2E, 6, abilities);
    }

    @Override
    public boolean isImmuneTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE);
    }

    @Override
    public void passiveTick(ServerPlayer player, PlayerData data) {
        if (player.isOnFire()) {
            player.clearFire();
        }
    }

    @Override
    public ParticleOptions elementParticle() {
        return ParticleTypes.FLAME;
    }
}
