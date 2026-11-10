package com.onepiece.newworld.gametest;

import com.mojang.serialization.DataResult;
import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.combat.CombatService;
import com.onepiece.newworld.combat.CombatState;
import com.onepiece.newworld.combat.CombatStyle;
import com.onepiece.newworld.combat.CutPower;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModEffects;
import com.onepiece.newworld.core.ModItems;
import com.onepiece.newworld.entities.SlashProjectile;
import com.onepiece.newworld.haki.HakiType;
import com.onepiece.newworld.progression.PlayerData;
import com.onepiece.newworld.progression.Progression;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Автотесты боевой системы (фаза 2): стили, цепочки ударов, парирование, блок, уклонение,
 * летящие удары, режущие блоки, и сила разреза.
 */
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class CombatGameTests {
    private static final String NS = OnePieceNewWorld.MODID;
    private static final String TEMPLATE = "empty";

    private CombatGameTests() {
    }

    @SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(CombatGameTests.class);
    }

    /** Игрок на земле в точке (2.5, 2, 1.5) теста, смотрит вдоль +Z. */
    private static Player fighter(GameTestHelper helper, ItemStack mainHand) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(2.5, 2, 1.5));
        player.moveTo(pos.x, pos.y, pos.z, 0f, 0f);
        player.setYHeadRot(0f);
        player.setOnGround(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        player.setHealth(20f);
        return player;
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void styleDependsOnHands(GameTestHelper helper) {
        Player player = fighter(helper, ItemStack.EMPTY);
        helper.assertTrue(CombatStyle.of(player) == CombatStyle.FISTS, "Пустая рука — кулаки");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KATANA.get()));
        helper.assertTrue(CombatStyle.of(player) == CombatStyle.KATANA, "Катана в руке — стиль катаны");
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(CombatStyle.of(player) == CombatStyle.KATANA, "Два меча без третьего — ещё не три меча");
        player.getInventory().setItem(3, new ItemStack(ModItems.WAZAMONO_KATANA.get()));
        helper.assertTrue(CombatStyle.of(player) == CombatStyle.THREE_SWORDS, "Третий меч на панели — стиль трёх мечей");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
        helper.assertTrue(CombatStyle.of(player) == null, "С хлебом в руке — обычный бой Minecraft");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void comboChainHitsAndAdvances(GameTestHelper helper) {
        Player player = fighter(helper, new ItemStack(ModItems.KATANA.get()));
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 2, 2, 3);
        CombatState state = ModAttachments.combat(player);

        helper.assertTrue(CombatService.attack(player, false), "Первый удар не выполнен");
        helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "Первый удар не попал по цели перед игроком");
        helper.assertTrue(state.comboIndex() == 1, "Цепочка не продвинулась: " + state.comboIndex());
        helper.assertTrue(!CombatService.attack(player, false), "Удар раньше восстановления должен отклоняться");

        float afterFirst = pig.getHealth();
        helper.runAfterDelay(9, () -> {
            helper.assertTrue(CombatService.attack(player, false), "Второй удар после восстановления не выполнен");
            helper.assertTrue(pig.getHealth() < afterFirst, "Второй удар цепочки не попал (неуязвимость не сброшена?)");
            helper.assertTrue(state.comboIndex() == 2, "Цепочка должна быть на третьем ударе: " + state.comboIndex());
            helper.assertTrue(state.hitStreak() == 2, "Счётчик попаданий: " + state.hitStreak());
            helper.assertTrue(ModAttachments.get(player).getWeaponMastery(CombatStyle.KATANA.id()) > 0, "Мастерство катаны не растёт");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void parryStunsAttackerAndGivesCounter(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        Player player = fighter(helper, new ItemStack(ModItems.KATANA.get()));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 2, 3);

        helper.assertTrue(CombatService.startBlocking(player), "Блок не начался");
        player.hurt(player.damageSources().mobAttack(zombie), 5f);
        helper.assertTrue(player.getHealth() == 20f, "Парирование должно полностью гасить удар");
        helper.assertTrue(zombie.hasEffect(ModEffects.STUNNED), "Нападающий должен быть оглушён");
        helper.assertTrue(ModAttachments.combat(player).hasCounter(helper.getLevel().getGameTime()), "Нет окна контратаки");

        // Оглушённый не может ударить даже без блока.
        CombatService.stopBlocking(player);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(zombie), 5f);
        helper.assertTrue(player.getHealth() == 20f, "Оглушённый зомби не должен наносить урон");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void blockReducesFrontalDamage(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        Player player = fighter(helper, new ItemStack(ModItems.KATANA.get()));
        Zombie front = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 2, 3);
        Zombie behind = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 2, 0);
        CombatService.startBlocking(player);

        helper.runAfterDelay(CombatService.PARRY_WINDOW + 2, () -> {
            PlayerData data = ModAttachments.get(player);
            float energyBefore = data.getEnergy();
            player.hurt(player.damageSources().mobAttack(front), 10f);
            float taken = 20f - player.getHealth();
            helper.assertTrue(Math.abs(taken - 10f * CombatStyle.KATANA.blockLeak()) < 0.01f,
                    "Блок катаны должен пропускать 30% урона, прошло " + taken);
            helper.assertTrue(data.getEnergy() < energyBefore, "Блок должен тратить энергию");

            player.setHealth(20f);
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().mobAttack(behind), 10f);
            helper.assertTrue(player.getHealth() == 10f, "Удар в спину блоком не закрывается: " + player.getHealth());
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void dodgeGivesInvulnerabilityAndHasCooldown(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        Player player = fighter(helper, ItemStack.EMPTY);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 2, 3);
        PlayerData data = ModAttachments.get(player);
        float energyBefore = data.getEnergy();

        helper.assertTrue(CombatService.dodge(player, 1f, 0f), "Уклонение не сработало");
        helper.assertTrue(data.getEnergy() == energyBefore - CombatService.DODGE_COST, "Уклонение должно тратить энергию");
        helper.assertTrue(player.getDeltaMovement().x > 0.5, "Рывок должен быть в сторону +X");
        player.hurt(player.damageSources().mobAttack(zombie), 6f);
        helper.assertTrue(player.getHealth() == 20f, "Во время уклонения урон не должен проходить");
        helper.assertTrue(!CombatService.dodge(player, 1f, 0f), "Повторное уклонение до перезарядки");

        helper.runAfterDelay(CombatService.DODGE_INVULNERABLE + 1, () -> {
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().mobAttack(zombie), 6f);
            helper.assertTrue(player.getHealth() < 20f, "После кадров неуязвимости урон должен проходить");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE, timeoutTicks = 60)
    public static void slashCutsStoneButNotBedrock(GameTestHelper helper) {
        for (int x = 1; x <= 3; x++) {
            helper.setBlock(x, 2, 3, Blocks.STONE);
            helper.setBlock(x, 2, 4, Blocks.BEDROCK);
        }
        Player player = fighter(helper, new ItemStack(ModItems.KATANA.get()));
        SlashProjectile.launch(player, helper.absoluteVec(new Vec3(2.5, 2.5, 0.5)), new Vec3(0, 0, 1),
                0.5f, 2.5f, false, 4f, 3, 20, false);
        helper.succeedWhen(() -> {
            for (int x = 1; x <= 3; x++) {
                helper.assertBlockPresent(Blocks.AIR, x, 2, 3);
                helper.assertBlockPresent(Blocks.BEDROCK, x, 2, 4);
            }
            helper.assertBlockPresent(Blocks.STONE, 2, 1, 3);
        });
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE, timeoutTicks = 40)
    public static void weakSlashDoesNotCutStone(GameTestHelper helper) {
        helper.setBlock(2, 2, 3, Blocks.STONE);
        Player player = fighter(helper, new ItemStack(Items.WOODEN_SWORD));
        SlashProjectile slash = SlashProjectile.launch(player, helper.absoluteVec(new Vec3(2.5, 2.5, 0.5)), new Vec3(0, 0, 1),
                0.5f, 1f, false, 4f, 1, 20, false);
        helper.succeedWhen(() -> {
            helper.assertTrue(slash.isRemoved(), "Слабый удар должен погаснуть о камень");
            helper.assertBlockPresent(Blocks.STONE, 2, 2, 3);
        });
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void cutPowerFromSwordMasteryAndHaki(GameTestHelper helper) {
        Player player = fighter(helper, new ItemStack(ModItems.KATANA.get()));
        helper.assertTrue(CutPower.of(player, CombatStyle.KATANA) == 1, "Обычная катана — сила 1");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.O_WAZAMONO_KATANA.get()));
        helper.assertTrue(CutPower.of(player, CombatStyle.KATANA) == 3, "О-вазамоно — сила 3");
        PlayerData data = ModAttachments.get(player);
        data.setHakiXp(HakiType.ARMAMENT, 750);
        data.setArmamentActive(true);
        helper.assertTrue(CutPower.of(player, CombatStyle.KATANA) == 5, "О-вазамоно + Вооружение 3 + включено = 5");
        helper.assertTrue(CutPower.canCut(Blocks.STONE.defaultBlockState(), 1.5f, 2), "Сила 2 режет камень");
        helper.assertTrue(!CutPower.canCut(Blocks.OBSIDIAN.defaultBlockState(), 50f, 4), "Сила 4 не режет обсидиан");
        helper.assertTrue(!CutPower.canCut(Blocks.BEDROCK.defaultBlockState(), -1f, 5), "Бедрок не режется никогда");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void weaponMasterySurvivesSave(GameTestHelper helper) {
        PlayerData data = new PlayerData();
        data.addWeaponMastery(CombatStyle.KATANA.id(), 320);
        DataResult<Tag> encoded = PlayerData.CODEC.encodeStart(NbtOps.INSTANCE, data);
        PlayerData loaded = PlayerData.CODEC.parse(NbtOps.INSTANCE, encoded.getOrThrow()).getOrThrow();
        helper.assertTrue(loaded.getWeaponMastery(CombatStyle.KATANA.id()) == 320, "Мастерство потеряно при сохранении");
        helper.assertTrue(data.copy().getWeaponMastery(CombatStyle.KATANA.id()) == 320, "Мастерство потеряно при отправке");
        helper.assertTrue(Progression.weaponTier(320) == 2, "320 очков — ступень 2 (с нуля)");
        helper.succeed();
    }
}
