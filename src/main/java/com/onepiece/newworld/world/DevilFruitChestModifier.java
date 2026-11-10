package com.onepiece.newworld.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onepiece.newworld.core.ModConfig;
import com.onepiece.newworld.core.ModItems;
import com.onepiece.newworld.fruits.DevilFruitItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Кладёт дьявольский фрукт в сундуки ванильных структур. Чаще всего — в кладах и на затонувших кораблях.
 * Какой фрукт выпадет, зависит от его редкости ({@link com.onepiece.newworld.fruits.DevilFruit#lootWeight()}).
 */
public class DevilFruitChestModifier extends LootModifier {
    public static final MapCodec<DevilFruitChestModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).apply(inst, DevilFruitChestModifier::new));

    public DevilFruitChestModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        ResourceLocation table = context.getQueriedLootTableId();
        if (!table.getNamespace().equals("minecraft") || !table.getPath().startsWith("chests/")) {
            return loot;
        }
        float chance = (float) (baseChance(table.getPath()) * ModConfig.FRUIT_CHANCE_MULTIPLIER.get());
        if (context.getRandom().nextFloat() < chance) {
            ItemStack fruit = pickFruit(context.getRandom());
            if (!fruit.isEmpty()) {
                loot.add(fruit);
            }
        }
        return loot;
    }

    private static float baseChance(String path) {
        if (path.equals("chests/buried_treasure")) {
            return 0.35f;
        }
        if (path.startsWith("chests/shipwreck")) {
            return 0.12f;
        }
        if (path.contains("bastion") || path.contains("end_city") || path.contains("ancient_city")
                || path.contains("woodland_mansion") || path.contains("stronghold")) {
            return 0.06f;
        }
        return 0.015f;
    }

    private static ItemStack pickFruit(RandomSource random) {
        int total = 0;
        for (DeferredItem<DevilFruitItem> item : ModItems.FRUIT_ITEMS) {
            total += item.get().fruit().get().lootWeight();
        }
        if (total <= 0) {
            return ItemStack.EMPTY;
        }
        int roll = random.nextInt(total);
        for (DeferredItem<DevilFruitItem> item : ModItems.FRUIT_ITEMS) {
            roll -= item.get().fruit().get().lootWeight();
            if (roll < 0) {
                return new ItemStack(item.get());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
