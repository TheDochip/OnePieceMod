package com.onepiece.newworld.weapons;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

/**
 * Катана с разрядом. Разряд прибавляется к силе разреза летящих ударов
 * (1 — обычная, 2 — вазамоно, 3 — о-вазамоно).
 */
public class KatanaItem extends SwordItem {
    private final int grade;

    public KatanaItem(Tier tier, int grade, float attackBonus, float attackSpeed, Properties properties) {
        super(tier, properties.attributes(SwordItem.createAttributes(tier, Math.round(attackBonus), attackSpeed)));
        this.grade = grade;
    }

    public int grade() {
        return grade;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.onepiece_newworld.katana_grade." + grade).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.onepiece_newworld.katana_hint").withStyle(ChatFormatting.GRAY));
    }
}
