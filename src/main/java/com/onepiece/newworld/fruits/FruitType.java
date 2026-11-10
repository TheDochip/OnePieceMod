package com.onepiece.newworld.fruits;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Категории фруктов из ТЗ. Древние и мифические Зоан — отдельные категории. */
public enum FruitType {
    PARAMECIA,
    ZOAN,
    ANCIENT_ZOAN,
    MYTHICAL_ZOAN,
    LOGIA;

    public boolean isLogia() {
        return this == LOGIA;
    }

    public MutableComponent displayName() {
        return Component.translatable("fruit_type.onepiece_newworld." + name().toLowerCase(Locale.ROOT));
    }
}
