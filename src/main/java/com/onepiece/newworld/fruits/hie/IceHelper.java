package com.onepiece.newworld.fruits.hie;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

/** Общие ледяные эффекты Хиэ Хиэ. */
public final class IceHelper {
    private IceHelper() {
    }

    /**
     * Превращает стоячую воду в тающий лёд (как зачарование «Ледоход»).
     *
     * @param center центр на уровне воды
     * @param radius радиус по горизонтали
     * @param depth  на сколько блоков вниз тоже замораживать
     */
    public static void freezeWater(ServerLevel level, BlockPos center, int radius, int depth, int minMelt, int maxMelt, RandomSource random) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -depth, -radius), center.offset(radius, 0, radius))) {
            if (pos.distSqr(center) > (double) radius * radius) {
                continue;
            }
            if (level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource()
                    && level.getBlockState(pos.above()).isAir()) {
                BlockPos frozen = pos.immutable();
                level.setBlockAndUpdate(frozen, Blocks.FROSTED_ICE.defaultBlockState());
                level.scheduleTick(frozen, Blocks.FROSTED_ICE, Mth.nextInt(random, minMelt, maxMelt));
            }
        }
    }

    /** Замораживает цель: эффект замерзания и сильное замедление. */
    public static void freeze(LivingEntity target, int extraTicks, int slownessLevel) {
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + extraTicks));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, extraTicks, slownessLevel));
    }
}
