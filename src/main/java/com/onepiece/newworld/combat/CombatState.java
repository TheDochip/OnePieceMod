package com.onepiece.newworld.combat;

/**
 * Состояние боя одного игрока на сервере. Не сохраняется: все времена — игровое время мира (тики).
 */
public class CombatState {
    /** Индекс следующего удара в цепочке. */
    int comboIndex;
    /** До какого времени можно продолжить цепочку. */
    long comboExpires;
    /** Раньше этого времени новый удар не принимается. */
    long nextAttackAt;
    /** Сколько ударов подряд попало (для счётчика на экране). */
    int hitStreak;

    /** Время начала блока или -1, если игрок не блокирует. */
    long blockStartedAt = -1;

    /** До какого времени действует контратака после парирования. */
    long counterUntil;

    /** До какого времени действуют кадры неуязвимости уклонения. */
    long dodgeInvulnerableUntil;
    /** Раньше этого времени нельзя уклониться снова. */
    long nextDodgeAt;
    /** Рывок в воздухе уже использован (сбрасывается на земле). */
    boolean airDashUsed;

    public int comboIndex() {
        return comboIndex;
    }

    public int hitStreak() {
        return hitStreak;
    }

    public boolean isBlocking() {
        return blockStartedAt >= 0;
    }

    public long blockStartedAt() {
        return blockStartedAt;
    }

    public boolean hasCounter(long now) {
        return now < counterUntil;
    }

    public boolean isDodging(long now) {
        return now < dodgeInvulnerableUntil;
    }
}
