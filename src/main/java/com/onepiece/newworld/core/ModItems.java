package com.onepiece.newworld.core;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.entities.ModEntities;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.fruits.DevilFruitItem;
import com.onepiece.newworld.fruits.ModFruits;
import com.onepiece.newworld.weapons.KairosekiCuffsItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OnePieceNewWorld.MODID);

    /** Всё, что сделано из морского камня: подавляет силы фрукта у того, кто это носит. */
    public static final TagKey<Item> KAIROSEKI_TAG = TagKey.create(Registries.ITEM, OnePieceNewWorld.id("kairoseki"));

    /** Все предметы-фрукты: из этого списка выбирается лут в сундуках. */
    public static final List<DeferredItem<DevilFruitItem>> FRUIT_ITEMS = new ArrayList<>();

    public static final DeferredItem<DevilFruitItem> GOMU_FRUIT = fruit("gomu_fruit", ModFruits.GOMU);
    public static final DeferredItem<DevilFruitItem> MERA_FRUIT = fruit("mera_fruit", ModFruits.MERA);
    public static final DeferredItem<DevilFruitItem> HIE_FRUIT = fruit("hie_fruit", ModFruits.HIE);

    public static final DeferredItem<Item> KAIROSEKI_SHARD = ITEMS.registerSimpleItem("kairoseki_shard");

    public static final DeferredItem<SwordItem> KAIROSEKI_JITTE = ITEMS.register("kairoseki_jitte",
            () -> new SwordItem(Tiers.IRON, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.2f))));

    public static final DeferredItem<KairosekiCuffsItem> KAIROSEKI_CUFFS = ITEMS.register("kairoseki_cuffs",
            () -> new KairosekiCuffsItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<BlockItem> KAIROSEKI_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.KAIROSEKI_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_KAIROSEKI_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_KAIROSEKI_ORE);
    public static final DeferredItem<BlockItem> KAIROSEKI_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.KAIROSEKI_BLOCK);

    public static final DeferredItem<DeferredSpawnEggItem> PIRATE_GRUNT_SPAWN_EGG = ITEMS.register("pirate_grunt_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.PIRATE_GRUNT, 0x7A2E22, 0xE8C170, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> MARINE_RECRUIT_SPAWN_EGG = ITEMS.register("marine_recruit_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.MARINE_RECRUIT, 0xF2F2F2, 0x2B5BA8, new Item.Properties()));

    private ModItems() {
    }

    private static DeferredItem<DevilFruitItem> fruit(String name, DeferredHolder<DevilFruit, ? extends DevilFruit> fruit) {
        DeferredItem<DevilFruitItem> item = ITEMS.register(name, () -> new DevilFruitItem(fruit, new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .food(new FoodProperties.Builder()
                        .nutrition(2)
                        .saturationModifier(0.1f)
                        .alwaysEdible()
                        .build())));
        FRUIT_ITEMS.add(item);
        return item;
    }
}
