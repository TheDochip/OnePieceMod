package com.onepiece.newworld.combat;

import com.onepiece.newworld.core.ModEffects;
import net.minecraft.world.entity.LivingEntity;

/** Существо не может действовать: оглушено ударом или без сознания от Королевского Хаки. */
public final class Incapacity {
    private Incapacity() {
    }

    public static boolean check(LivingEntity entity) {
        return entity.hasEffect(ModEffects.STUNNED) || entity.hasEffect(ModEffects.KNOCKED_OUT);
    }
}
