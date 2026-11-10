package com.onepiece.newworld.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Серверные настройки баланса. Файл: saves/&lt;мир&gt;/serverconfig/onepiece_newworld-server.toml.
 * Сервер автоматически передаёт эти значения клиентам.
 */
public final class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("energy");
    }

    public static final ModConfigSpec.DoubleValue ENERGY_REGEN = BUILDER
            .comment("Восстановление энергии за тик (20 тиков = 1 секунда)")
            .defineInRange("regenPerTick", 0.35, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue ENERGY_PER_LEVEL = BUILDER
            .comment("Прибавка к максимуму энергии за каждый уровень персонажа")
            .defineInRange("maxPerLevel", 2.0, 0.0, 50.0);

    static {
        BUILDER.pop().push("abilities");
    }

    public static final ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER = BUILDER
            .comment("Общий множитель урона способностей фруктов")
            .defineInRange("damageMultiplier", 1.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue COOLDOWN_MULTIPLIER = BUILDER
            .comment("Общий множитель перезарядки способностей")
            .defineInRange("cooldownMultiplier", 1.0, 0.05, 10.0);

    static {
        BUILDER.pop().push("haki");
    }

    public static final ModConfigSpec.DoubleValue ARMAMENT_DRAIN = BUILDER
            .comment("Расход энергии за тик, пока включено Хаки Вооружения")
            .defineInRange("armamentDrainPerTick", 0.6, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue HAKI_XP_MULTIPLIER = BUILDER
            .comment("Множитель опыта Хаки")
            .defineInRange("xpMultiplier", 1.0, 0.0, 100.0);

    static {
        BUILDER.pop().push("progression");
    }

    public static final ModConfigSpec.DoubleValue XP_MULTIPLIER = BUILDER
            .comment("Множитель опыта персонажа")
            .defineInRange("xpMultiplier", 1.0, 0.0, 100.0);
    public static final ModConfigSpec.LongValue MARINE_AGGRO_BOUNTY = BUILDER
            .comment("С какой награды за голову дозорные-NPC нападают на пирата")
            .defineInRange("marineAggroBounty", 5_000_000L, 0L, Long.MAX_VALUE);
    public static final ModConfigSpec.BooleanValue MARINE_PATROLS = BUILDER
            .comment("Высылать отряд Дозора, когда награда пирата достигает новой отметки")
            .define("marinePatrols", true);

    static {
        BUILDER.pop().push("loot");
    }

    public static final ModConfigSpec.DoubleValue FRUIT_CHANCE_MULTIPLIER = BUILDER
            .comment("Множитель шанса найти дьявольский фрукт в сундуке")
            .defineInRange("fruitChanceMultiplier", 1.0, 0.0, 100.0);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ModConfig() {
    }
}
