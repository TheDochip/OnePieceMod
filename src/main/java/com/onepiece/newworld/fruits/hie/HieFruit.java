package com.onepiece.newworld.fruits.hie;

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

/** Хиэ Хиэ: тело изо льда. Не мёрзнет, вода под ногами замерзает. */
public class HieFruit extends DevilFruit {
    public HieFruit(List<Supplier<? extends Ability>> abilities) {
        super(FruitType.LOGIA, 0x8FE3FF, 6, abilities);
    }

    @Override
    public boolean isImmuneTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FREEZING);
    }

    @Override
    public void passiveTick(ServerPlayer player, PlayerData data) {
        // Раз в 2 тика, только стоя на земле и не крадучись (Shift — чтобы можно было не замораживать воду).
        if (player.tickCount % 2 == 0 && player.onGround() && !player.isShiftKeyDown()) {
            IceHelper.freezeWater(player.serverLevel(), player.blockPosition().below(), 3, 0, 60, 120, player.getRandom());
        }
    }

    @Override
    public ParticleOptions elementParticle() {
        return ParticleTypes.SNOWFLAKE;
    }
}
