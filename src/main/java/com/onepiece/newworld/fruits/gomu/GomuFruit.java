package com.onepiece.newworld.fruits.gomu;

import com.onepiece.newworld.abilities.Ability;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.fruits.FruitType;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/** Гому Гому: резиновое тело — нет урона от падения и молнии. */
public class GomuFruit extends DevilFruit {
    public GomuFruit(List<Supplier<? extends Ability>> abilities) {
        super(FruitType.PARAMECIA, 0xB06CFF, 10, abilities);
    }

    @Override
    public boolean isImmuneTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_LIGHTNING);
    }
}
