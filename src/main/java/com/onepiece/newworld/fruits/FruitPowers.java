package com.onepiece.newworld.fruits;

import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModEffects;
import com.onepiece.newworld.core.ModItems;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/** Общие правила для всех пользователей фруктов: вода и морской камень подавляют силы. */
public final class FruitPowers {
    private FruitPowers() {
    }

    @Nullable
    public static DevilFruit fruitOf(Player player) {
        return ModFruits.get(ModAttachments.get(player).getFruitId());
    }

    /** Силы заблокированы: игрок в воде, под морским камнем или подавлением Королевского Хаки, без сознания или носит кайросеки. */
    public static boolean isSuppressed(Player player) {
        return player.isInWater() || player.hasEffect(ModEffects.SEASTONE) || player.hasEffect(ModEffects.HAKI_SUPPRESSION)
                || player.hasEffect(ModEffects.KNOCKED_OUT) || carriesKairoseki(player);
    }

    public static boolean carriesKairoseki(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(ModItems.KAIROSEKI_TAG)) {
                return true;
            }
        }
        return false;
    }
}
