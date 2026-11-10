package com.onepiece.newworld.combat;

import com.onepiece.newworld.core.ModItems;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * Боевой стиль определяется тем, что у игрока в руках:
 * <ul>
 *     <li>пустая основная рука — {@link #FISTS};</li>
 *     <li>меч в основной руке — {@link #KATANA};</li>
 *     <li>мечи в обеих руках и ещё один на панели быстрого доступа — {@link #THREE_SWORDS}.</li>
 * </ul>
 * Любой другой предмет в руке — обычный бой Minecraft.
 *
 * <p>Цепочки ударов задаются здесь же. Имена анимаций уникальны для всего мода
 * (Player Animation Library берёт id из ключа анимации внутри JSON).</p>
 */
public enum CombatStyle {
    FISTS("fists", 0.5f,
            List.of(
                    AttackDefinition.light("fists_light_1", 1.0f, 0.0f, 0.25f, 5),
                    AttackDefinition.light("fists_light_2", 1.0f, 0.0f, 0.25f, 5),
                    AttackDefinition.light("fists_light_3", 1.1f, 0.0f, 0.3f, 6),
                    AttackDefinition.finisher("fists_light_4", 1.6f, 0.0f, 1.1f, 12, 10)),
            AttackDefinition.heavy("fists_uppercut", 1.8f, 0.0f, 0.3f, 0.95f, 16),
            AttackDefinition.light("fists_air_attack", 1.2f, 0.0f, 0.2f, 7)),

    KATANA("katana", 0.3f,
            List.of(
                    AttackDefinition.light("katana_light_1", 1.0f, 0.5f, 0.3f, 7),
                    AttackDefinition.light("katana_light_2", 1.0f, 0.5f, 0.3f, 7),
                    AttackDefinition.finisher("katana_light_3", 1.5f, 0.6f, 1.0f, 14, 12)),
            AttackDefinition.heavy("katana_heavy", 2.0f, 0.8f, 1.4f, 0.1f, 20),
            AttackDefinition.light("common_air_attack", 1.3f, 0.5f, 0.3f, 9)),

    THREE_SWORDS("three_swords", 0.3f,
            List.of(
                    AttackDefinition.light("three_swords_light_1", 0.9f, 0.6f, 0.3f, 6),
                    AttackDefinition.light("three_swords_light_2", 0.9f, 0.6f, 0.3f, 6),
                    AttackDefinition.finisher("three_swords_light_3", 1.7f, 0.8f, 1.3f, 15, 12)),
            AttackDefinition.heavy("three_swords_great_slash", 2.3f, 1.0f, 1.6f, 0.15f, 22),
            AttackDefinition.light("three_swords_air_attack", 1.4f, 0.6f, 0.3f, 9));

    private final String id;
    /** Доля урона, которая проходит сквозь блок (кулаки держат удар хуже меча). */
    private final float blockLeak;
    private final List<AttackDefinition> chain;
    private final AttackDefinition heavy;
    private final AttackDefinition air;

    CombatStyle(String id, float blockLeak, List<AttackDefinition> chain, AttackDefinition heavy, AttackDefinition air) {
        this.id = id;
        this.blockLeak = blockLeak;
        this.chain = chain;
        this.heavy = heavy;
        this.air = air;
    }

    public String id() {
        return id;
    }

    public float blockLeak() {
        return blockLeak;
    }

    public List<AttackDefinition> chain() {
        return chain;
    }

    public AttackDefinition heavy() {
        return heavy;
    }

    public AttackDefinition air() {
        return air;
    }

    public boolean usesSword() {
        return this != FISTS;
    }

    public static boolean isSword(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModItems.SWORDS_TAG) || stack.getItem() instanceof SwordItem);
    }

    /** Стиль по тому, что у игрока в руках, или null — тогда работает обычный бой. */
    @Nullable
    public static CombatStyle of(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.isEmpty()) {
            return FISTS;
        }
        if (!isSword(main)) {
            return null;
        }
        if (isSword(player.getOffhandItem()) && hasSpareSword(player)) {
            return THREE_SWORDS;
        }
        return KATANA;
    }

    /** Третий меч должен лежать на панели быстрого доступа (не в руке). */
    private static boolean hasSpareSword(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (slot != inventory.selected && isSword(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static CombatStyle byId(String id) {
        for (CombatStyle style : values()) {
            if (style.id.equals(id)) {
                return style;
            }
        }
        return null;
    }
}
