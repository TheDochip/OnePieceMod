package com.onepiece.newworld.client;

import net.minecraft.Util;

/** Состояние боя на стороне клиента: только для интерфейса и чтобы не слать лишние пакеты. */
public final class ClientCombat {
    /** Сколько миллисекунд показывать счётчик комбо после последнего попадания. */
    public static final long COMBO_SHOW_MS = 2000;

    static boolean blocking;
    static long lastAttackSentTick = Long.MIN_VALUE;

    private static int combo;
    private static long comboAt;

    private ClientCombat() {
    }

    public static void onServerAnimation(int newCombo) {
        if (newCombo > 0) {
            combo = newCombo;
            comboAt = Util.getMillis();
        }
    }

    /** Текущий счётчик попаданий подряд или 0, если его пора скрыть. */
    public static int visibleCombo() {
        return Util.getMillis() - comboAt < COMBO_SHOW_MS ? combo : 0;
    }

    /** Насколько счётчик ещё «свежий»: 1 — только что, 0 — пора скрыть. */
    public static float comboFreshness() {
        return Math.max(0f, 1f - (Util.getMillis() - comboAt) / (float) COMBO_SHOW_MS);
    }

    static void reset() {
        blocking = false;
        combo = 0;
    }
}
