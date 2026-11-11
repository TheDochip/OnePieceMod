package com.onepiece.newworld.haki;

import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

/** Три вида Хаки: Наблюдение, Вооружение и Королевское (только у избранных, см. ConquerorService). */
public enum HakiType implements StringRepresentable {
    OBSERVATION,
    ARMAMENT,
    CONQUEROR;

    public static final Codec<HakiType> CODEC = StringRepresentable.fromEnum(HakiType::values);

    /** Опыт для уровней 1..5. Уровень 0 — Хаки не пробуждено. */
    private static final int[] THRESHOLDS = {100, 300, 700, 1500, 3000};
    public static final int MAX_LEVEL = THRESHOLDS.length;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public MutableComponent displayName() {
        return Component.translatable("haki.onepiece_newworld." + getSerializedName());
    }

    /** Опыт, с которого начинается уровень 1..5. */
    public static int thresholdFor(int level) {
        return level <= 0 ? 0 : THRESHOLDS[Math.min(level, MAX_LEVEL) - 1];
    }

    public static int levelFor(int xp) {
        int level = 0;
        for (int threshold : THRESHOLDS) {
            if (xp >= threshold) {
                level++;
            }
        }
        return level;
    }

    /** Опыт до следующего уровня или -1 на максимальном. */
    public static int nextThreshold(int xp) {
        for (int threshold : THRESHOLDS) {
            if (xp < threshold) {
                return threshold;
            }
        }
        return -1;
    }
}
