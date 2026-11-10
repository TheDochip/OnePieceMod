package com.onepiece.newworld.progression;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Формулы прогрессии: опыт уровней, звания Дозора, бонусы от уровня и мастерства. */
public final class Progression {
    public static final int MAX_LEVEL = 100;
    public static final int MAX_MASTERY = 1000;

    /** Заслуги, нужные для званий Дозора (индекс = звание). */
    private static final int[] MARINE_RANK_THRESHOLDS = {0, 50, 150, 400, 900, 1800, 3500, 6000, 10000, 16000};
    private static final String[] MARINE_RANKS = {
            "recruit", "seaman", "petty_officer", "ensign", "lieutenant",
            "commander", "captain", "commodore", "rear_admiral", "vice_admiral"
    };

    private Progression() {
    }

    /** Опыт, нужный чтобы перейти с уровня level на level + 1. */
    public static int xpToNextLevel(int level) {
        return 100 + 40 * (level - 1) + 2 * (level - 1) * (level - 1);
    }

    /** +1 сердце (2 единицы здоровья) за каждые 5 уровней. */
    public static double bonusHealth(int level) {
        return 2.0 * ((level - 1) / 5);
    }

    /** Множитель силы способностей: +1% за уровень и до +25% за мастерство фрукта. */
    public static float abilityPower(int level, int mastery) {
        return 1f + 0.01f * (level - 1) + 0.25f * Math.min(mastery, MAX_MASTERY) / MAX_MASTERY;
    }

    /** Множитель перезарядки: мастерство фрукта сокращает её до 30%. */
    public static float cooldownFactor(int mastery) {
        return 1f - 0.3f * Math.min(mastery, MAX_MASTERY) / MAX_MASTERY;
    }

    public static int marineRank(int merit) {
        int rank = 0;
        for (int i = 0; i < MARINE_RANK_THRESHOLDS.length; i++) {
            if (merit >= MARINE_RANK_THRESHOLDS[i]) {
                rank = i;
            }
        }
        return rank;
    }

    public static MutableComponent marineRankName(int merit) {
        return Component.translatable("rank.onepiece_newworld." + MARINE_RANKS[marineRank(merit)]);
    }

    /** Заслуги до следующего звания или -1 на максимальном. */
    public static int nextRankMerit(int merit) {
        for (int threshold : MARINE_RANK_THRESHOLDS) {
            if (merit < threshold) {
                return threshold;
            }
        }
        return -1;
    }
}
