package com.onepiece.newworld.combat;

import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.haki.HakiType;
import com.onepiece.newworld.progression.PlayerData;
import com.onepiece.newworld.progression.Progression;
import com.onepiece.newworld.weapons.KatanaItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Сила разреза летящего удара (0..5) и какие блоки она берёт.
 *
 * <p>Сила = разряд меча + половина ступени мастерства + 1 за Вооружение 3-го уровня и выше
 * + 1, если Вооружение сейчас включено.</p>
 *
 * <table>
 *     <tr><th>сила</th><th>твёрдость до</th><th>примеры</th></tr>
 *     <tr><td>1</td><td>0.8</td><td>земля, песок, листва, стекло, шерсть</td></tr>
 *     <tr><td>2</td><td>2</td><td>камень, булыжник, дерево</td></tr>
 *     <tr><td>3</td><td>3</td><td>руды, глубинный сланец</td></tr>
 *     <tr><td>4</td><td>5</td><td>железные блоки, наковальни</td></tr>
 *     <tr><td>5</td><td>50</td><td>обсидиан</td></tr>
 * </table>
 * Бедрок и другие неразрушимые блоки, блоки с содержимым (сундуки, печи) и кайросеки не режутся никогда.
 */
public final class CutPower {
    public static final int MAX = 5;
    private static final float[] HARDNESS_LIMIT = {-1f, 0.8f, 2f, 3f, 5f, 50f};

    private CutPower() {
    }

    public static int swordGrade(ItemStack stack) {
        if (stack.getItem() instanceof KatanaItem katana) {
            return katana.grade();
        }
        if (stack.getItem() instanceof SwordItem sword) {
            if (sword.getTier() == Tiers.NETHERITE) {
                return 3;
            }
            if (sword.getTier() == Tiers.DIAMOND) {
                return 2;
            }
            if (sword.getTier() == Tiers.IRON) {
                return 1;
            }
        }
        return 0;
    }

    public static int of(Player player, CombatStyle style) {
        PlayerData data = ModAttachments.get(player);
        int power = swordGrade(player.getMainHandItem());
        power += Progression.weaponTier(data.getWeaponMastery(style.id())) / 2;
        if (data.getHakiLevel(HakiType.ARMAMENT) >= 3) {
            power++;
        }
        if (data.isArmamentActive()) {
            power++;
        }
        return Math.max(0, Math.min(MAX, power));
    }

    /** Максимальная твёрдость блока, которую берёт удар этой силы. */
    public static float hardnessLimit(int power) {
        return HARDNESS_LIMIT[Math.max(0, Math.min(MAX, power))];
    }

    /** Режется ли блок с такой твёрдостью (без учёта защиты территории и тегов). */
    public static boolean canCut(BlockState state, float hardness, int power) {
        return hardness >= 0 && hardness <= hardnessLimit(power) && !state.isAir();
    }
}
