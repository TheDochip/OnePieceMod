package com.onepiece.newworld.combat;

import com.onepiece.newworld.OnePieceNewWorld;
import net.minecraft.resources.ResourceLocation;

/**
 * Один удар боевой системы.
 *
 * @param animation    id анимации игрока (onepiece_newworld:&lt;ключ&gt;)
 * @param damage       множитель к урону оружия
 * @param extraReach   прибавка к дальности удара, блоков
 * @param knockback    сила отбрасывания
 * @param launch       вертикальный подброс цели (апперкот)
 * @param recovery     тиков до следующего удара
 * @param stun         тиков оглушения цели (0 — нет)
 * @param finisher     последний удар цепочки: казнь врага с малым здоровьем
 * @param minDot       ширина дуги: косинус половины угла (0.5 ≈ ±60°)
 */
public record AttackDefinition(ResourceLocation animation, float damage, float extraReach, float knockback,
                               float launch, int recovery, int stun, boolean finisher, double minDot) {

    static AttackDefinition light(String anim, float damage, float reach, float knockback, int recovery) {
        return new AttackDefinition(OnePieceNewWorld.id(anim), damage, reach, knockback, 0f, recovery, 0, false, 0.55);
    }

    static AttackDefinition finisher(String anim, float damage, float reach, float knockback, int recovery, int stun) {
        return new AttackDefinition(OnePieceNewWorld.id(anim), damage, reach, knockback, 0f, recovery, stun, true, 0.45);
    }

    static AttackDefinition heavy(String anim, float damage, float reach, float knockback, float launch, int recovery) {
        return new AttackDefinition(OnePieceNewWorld.id(anim), damage, reach, knockback, launch, recovery, 10, true, 0.4);
    }
}
