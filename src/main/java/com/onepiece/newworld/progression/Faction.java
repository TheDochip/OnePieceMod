package com.onepiece.newworld.progression;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

/** Путь персонажа. Выбирается один раз в экране персонажа (клавиша K). */
public enum Faction implements StringRepresentable {
    /** Ещё не выбрал путь. Получает награду за голову за преступления, как пират. */
    NONE,
    /** Пират: копит награду за голову, на него охотится Дозор. */
    PIRATE,
    /** Дозорный: получает заслуги за поимку пиратов и повышается в звании. */
    MARINE;

    public static final Codec<Faction> CODEC = StringRepresentable.fromEnum(Faction::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public MutableComponent displayName() {
        return Component.translatable("faction.onepiece_newworld." + getSerializedName());
    }

    public static Faction byId(int id) {
        Faction[] values = values();
        return id >= 0 && id < values.length ? values[id] : NONE;
    }
}
