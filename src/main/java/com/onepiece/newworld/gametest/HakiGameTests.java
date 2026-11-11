package com.onepiece.newworld.gametest;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.combat.CombatService;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModEffects;
import com.onepiece.newworld.fruits.FruitPowers;
import com.onepiece.newworld.fruits.ModFruits;
import com.onepiece.newworld.haki.ConquerorService;
import com.onepiece.newworld.haki.HakiType;
import com.onepiece.newworld.haki.Willpower;
import com.onepiece.newworld.progression.PlayerData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Автотесты Хаки (фаза 3): сила воли, розыгрыш и пробуждение Королевского Хаки, волна,
 * подавление фруктов, покрытие и столкновение.
 *
 * <p>Тесты с волной и столкновением задевают всё вокруг, поэтому идут отдельной партией
 * ({@code batch = "conqueror"}) — после остальных, чтобы не оглушить мобов из чужих тестов.</p>
 */
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class HakiGameTests {
    private static final String NS = OnePieceNewWorld.MODID;
    private static final String TEMPLATE = "empty";
    private static final String LOUD = "conqueror";

    private HakiGameTests() {
    }

    @SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(HakiGameTests.class);
    }

    private static Player player(GameTestHelper helper, double x, double z) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(new Vec3(x, 2, z));
        player.moveTo(pos.x, pos.y, pos.z, 0f, 0f);
        player.setOnGround(true);
        player.setHealth(20f);
        return player;
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void willpowerFormula(GameTestHelper helper) {
        Player player = player(helper, 2.5, 2.5);
        PlayerData data = ModAttachments.get(player);
        data.setLevel(10);
        data.setHakiXp(HakiType.CONQUEROR, 700);
        data.setHakiXp(HakiType.ARMAMENT, 300);
        helper.assertTrue(Willpower.of(player) == 10 + 20 * 3 + 5 * 2, "Воля игрока: " + Willpower.of(player));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 1);
        helper.assertTrue(Willpower.of(zombie) == 10, "Воля моба = здоровье / 2: " + Willpower.of(zombie));
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void conquerorIsRolledOnceAndGrantedByXp(GameTestHelper helper) {
        Player player = player(helper, 2.5, 2.5);
        PlayerData data = ModAttachments.get(player);
        ConquerorService.rollOnFirstJoin(player);
        helper.assertTrue(data.isConquerorRolled(), "Шанс не разыгран");
        helper.assertTrue(!ConquerorService.rollOnFirstJoin(player), "Шанс разыгрывается только один раз");

        data.removeConqueror();
        helper.assertTrue(!data.hasConqueror(), "Королевское не убрано");
        data.setHakiXp(HakiType.CONQUEROR, 300);
        helper.assertTrue(data.hasConqueror() && data.getHakiLevel(HakiType.CONQUEROR) == 2, "Опыт должен давать Королевское");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE, batch = LOUD)
    public static void dormantConquerorAwakensBelowTwentyPercent(GameTestHelper helper) {
        Player player = player(helper, 2.5, 2.5);
        PlayerData data = ModAttachments.get(player);
        data.setConquerorInnate(true);
        player.setHealth(10f);
        helper.assertTrue(!ConquerorService.tryAwaken(player), "При половине здоровья пробуждаться рано");
        player.setHealth(3f);
        helper.assertTrue(ConquerorService.tryAwaken(player), "Ниже 20% здоровья Королевское должно проснуться");
        helper.assertTrue(data.getHakiLevel(HakiType.CONQUEROR) == 1, "После пробуждения уровень 1");
        helper.assertTrue(!ConquerorService.tryAwaken(player), "Второй раз пробуждаться нечему");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE, batch = LOUD)
    public static void waveKnocksOutWeakPressesMiddleSparesStrong(GameTestHelper helper) {
        Player player = player(helper, 2.5, 2.5);
        PlayerData data = ModAttachments.get(player);
        data.setHakiXp(HakiType.CONQUEROR, 1500); // уровень 4 → воля 1 + 80 = 81, радиус 24
        Zombie weak = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 1);       // воля 10
        IronGolem middle = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 3, 2, 3); // воля 50
        Warden strong = helper.spawnWithNoFreeWill(EntityType.WARDEN, 1, 2, 3);      // воля 250

        int knocked = ConquerorService.release(player, data.getHakiLevel(HakiType.CONQUEROR));
        helper.assertTrue(knocked >= 1, "Никто не потерял сознание");
        helper.assertTrue(weak.hasEffect(ModEffects.KNOCKED_OUT), "Слабый (воля вдвое ниже) должен потерять сознание");
        helper.assertTrue(!middle.hasEffect(ModEffects.KNOCKED_OUT), "Голем с волей 50 из 81 должен устоять");
        helper.assertTrue(middle.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Устоявший слабее вас получает давление");
        helper.assertTrue(!strong.hasEffect(ModEffects.KNOCKED_OUT) && !strong.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "Волна не действует на тех, чья воля сильнее");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void suppressionAndKnockoutBlockFruitAndActions(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        Player player = player(helper, 2.5, 1.5);
        ModAttachments.get(player).setFruit(ModFruits.MERA.getId());
        helper.assertTrue(!FruitPowers.isSuppressed(player), "Силы подавлены без причины");
        player.addEffect(new MobEffectInstance(ModEffects.HAKI_SUPPRESSION, 60));
        helper.assertTrue(FruitPowers.isSuppressed(player), "Подавление волей должно блокировать фрукт");
        player.removeEffect(ModEffects.HAKI_SUPPRESSION);

        ConquerorService.knockOut(player, 60);
        helper.assertTrue(FruitPowers.isSuppressed(player), "Без сознания силы фрукта не работают");
        helper.assertTrue(!CombatService.attack(player, false), "Без сознания нельзя бить");
        helper.assertTrue(!CombatService.dodge(player, 1f, 0f), "Без сознания нельзя уклоняться");

        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 2, 3);
        ConquerorService.knockOut(zombie, 60);
        Player victim = player(helper, 2.5, 2.0);
        victim.hurt(victim.damageSources().mobAttack(zombie), 5f);
        helper.assertTrue(victim.getHealth() == 20f, "Моб без сознания не должен наносить урон");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void coatingPiercesBlockOfWeakerWill(GameTestHelper helper) {
        Player attacker = player(helper, 2.5, 3.5);
        PlayerData attackerData = ModAttachments.get(attacker);
        attackerData.setHakiXp(HakiType.CONQUEROR, 1500);
        attackerData.setHakiXp(HakiType.ARMAMENT, 100);
        attackerData.setArmamentActive(true);
        helper.assertTrue(ConquerorService.hasCoating(attacker), "Королевское 4 + Вооружение = покрытие");

        Player defender = player(helper, 2.5, 1.5); // смотрит на +Z, то есть на нападающего
        CombatService.startBlocking(defender);
        helper.runAfterDelay(CombatService.PARRY_WINDOW + 2, () -> {
            defender.hurt(defender.damageSources().playerAttack(attacker), 10f);
            helper.assertTrue(defender.getHealth() == 10f, "Покрытый удар должен пробить блок: здоровье " + defender.getHealth());

            attackerData.setArmamentActive(false);
            defender.setHealth(20f);
            defender.invulnerableTime = 0;
            defender.hurt(defender.damageSources().playerAttack(attacker), 10f);
            helper.assertTrue(defender.getHealth() > 10f, "Без покрытия блок снова держит удар");
            helper.succeed();
        });
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE, batch = LOUD)
    public static void clashRepelsTheWeakerWill(GameTestHelper helper) {
        Player strong = player(helper, 1.5, 2.5);
        Player weak = player(helper, 3.5, 2.5);
        ModAttachments.get(strong).setLevel(60);
        ModAttachments.get(strong).setHakiXp(HakiType.CONQUEROR, 3000);
        ModAttachments.get(weak).setHakiXp(HakiType.CONQUEROR, 1500);

        helper.assertTrue(ConquerorService.clash(strong, weak), "Столкновение не произошло");
        helper.assertTrue(!ConquerorService.clash(strong, weak), "Повторное столкновение сразу же — на перезарядке");
        MobEffectInstance stun = weak.getEffect(ModEffects.STUNNED);
        helper.assertTrue(stun != null && stun.getDuration() > 10, "Проигравший должен быть оглушён дольше общей заминки");
        MobEffectInstance winnerStun = strong.getEffect(ModEffects.STUNNED);
        helper.assertTrue(winnerStun != null && winnerStun.getDuration() <= 10, "Победитель замирает только на миг");
        helper.succeed();
    }
}
