package com.onepiece.newworld.haki;

import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.progression.PlayerData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Сила воли — число, которое сравнивается в любом столкновении Хаки.
 * <ul>
 *     <li>игрок: уровень + 20 × уровень Королевского Хаки + 5 × уровень Вооружения;</li>
 *     <li>босс или особый NPC: своё значение ({@link Holder});</li>
 *     <li>обычный моб: максимум здоровья / 2.</li>
 * </ul>
 */
public final class Willpower {
    private Willpower() {
    }

    /** Существа с заданной вручную волей (боссы, канонные персонажи). */
    public interface Holder {
        int willpower();
    }

    public static int of(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerData data = ModAttachments.get(player);
            return data.getLevel() + 20 * data.getHakiLevel(HakiType.CONQUEROR) + 5 * data.getHakiLevel(HakiType.ARMAMENT);
        }
        if (entity instanceof Holder holder) {
            return holder.willpower();
        }
        return Math.max(1, (int) (entity.getMaxHealth() / 2f));
    }
}
