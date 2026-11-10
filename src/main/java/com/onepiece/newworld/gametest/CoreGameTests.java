package com.onepiece.newworld.gametest;

import com.mojang.serialization.DataResult;
import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.abilities.Ability;
import com.onepiece.newworld.abilities.ModAbilities;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModItems;
import com.onepiece.newworld.entities.ElementalProjectile;
import com.onepiece.newworld.entities.MarineRecruit;
import com.onepiece.newworld.entities.ModEntities;
import com.onepiece.newworld.entities.PirateGrunt;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.fruits.FruitPowers;
import com.onepiece.newworld.fruits.ModFruits;
import com.onepiece.newworld.haki.HakiType;
import com.onepiece.newworld.progression.Faction;
import com.onepiece.newworld.progression.PlayerData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Автотесты ядра мода. Запуск: {@code ./gradlew runGameTestServer} (это же делает сборка на GitHub).
 * В обычной игре не выполняются: включаются только системным свойством neoforge.enabledGameTestNamespaces.
 */
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class CoreGameTests {
    private static final String NS = OnePieceNewWorld.MODID;
    /** Шаблон 5×4×5 с каменным полом. В координатах теста пол находится на y=1, существа ставятся на y=2. */
    private static final String TEMPLATE = "empty";

    private CoreGameTests() {
    }

    @SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(CoreGameTests.class);
    }

    private static Player mockPlayer(GameTestHelper helper, ResourceLocation fruit) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(2.5, 2, 2.5)));
        ModAttachments.get(player).setFruit(fruit);
        player.setHealth(20f);
        return player;
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void registriesArePopulated(GameTestHelper helper) {
        helper.assertTrue(ModFruits.REGISTRY.size() == 3, "Ожидалось 3 фрукта, а их " + ModFruits.REGISTRY.size());
        helper.assertTrue(ModAbilities.REGISTRY.size() == 9, "Ожидалось 9 способностей, а их " + ModAbilities.REGISTRY.size());
        for (DevilFruit fruit : ModFruits.REGISTRY) {
            helper.assertTrue(fruit.abilityCount() == 3, "У фрукта " + fruit.id() + " не 3 способности");
            for (Ability ability : fruit.abilities()) {
                helper.assertTrue(ability.id() != null && ability.baseCooldown() > 0, "Плохая способность у " + fruit.id());
            }
        }
        helper.assertTrue(ModItems.FRUIT_ITEMS.size() == 3, "Не все фрукты имеют предмет");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void playerDataSurvivesSaveAndLoad(GameTestHelper helper) {
        PlayerData data = new PlayerData();
        data.setFruit(ModFruits.MERA.getId());
        data.addXp(150);
        data.setHakiXp(HakiType.ARMAMENT, 750);
        data.setFaction(Faction.PIRATE);
        data.setBounty(123_456_789L);
        data.addMastery(ModFruits.MERA.getId(), 42);
        data.startCooldown(ModAbilities.MERA_FIRE_FIST.getId(), 1000L, 100);

        DataResult<Tag> encoded = PlayerData.CODEC.encodeStart(NbtOps.INSTANCE, data);
        PlayerData loaded = PlayerData.CODEC.parse(NbtOps.INSTANCE, encoded.getOrThrow()).getOrThrow();
        check(helper, loaded, "после сохранения");
        check(helper, data.copy(), "после копирования для сети");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, PlayerData d, String when) {
        helper.assertTrue(ModFruits.MERA.getId().equals(d.getFruitId()), "Фрукт потерян " + when);
        helper.assertTrue(d.getLevel() == 2, "Уровень неверный " + when + ": " + d.getLevel());
        helper.assertTrue(d.getHakiLevel(HakiType.ARMAMENT) == 3, "Хаки неверное " + when);
        helper.assertTrue(d.getFaction() == Faction.PIRATE, "Путь потерян " + when);
        helper.assertTrue(d.getBounty() == 123_456_789L, "Награда потеряна " + when);
        helper.assertTrue(d.getMastery(ModFruits.MERA.getId()) == 42, "Мастерство потеряно " + when);
        helper.assertTrue(d.cooldownRemaining(ModAbilities.MERA_FIRE_FIST.getId(), 1050L) == 50, "Перезарядка потеряна " + when);
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void gomuIgnoresFallDamage(GameTestHelper helper) {
        Player player = mockPlayer(helper, ModFruits.GOMU.getId());
        player.hurt(player.damageSources().fall(), 6f);
        helper.assertTrue(player.getHealth() == 20f, "Резиновое тело получило урон от падения");

        ModAttachments.get(player).setFruit(null);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().fall(), 6f);
        helper.assertTrue(player.getHealth() < 20f, "Контроль: без фрукта урон от падения должен проходить");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void meraIgnoresFireAndLogiaDodgesHits(GameTestHelper helper) {
        helper.getLevel().getServer().setDifficulty(Difficulty.NORMAL, true);
        Player player = mockPlayer(helper, ModFruits.MERA.getId());
        player.hurt(player.damageSources().inFire(), 4f);
        helper.assertTrue(player.getHealth() == 20f, "Мера Мера получил урон от огня");

        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 1);
        float energyBefore = ModAttachments.get(player).getEnergy();
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(zombie), 4f);
        helper.assertTrue(player.getHealth() == 20f, "Обычный удар не должен задевать Логию");
        helper.assertTrue(ModAttachments.get(player).getEnergy() < energyBefore, "Уклонение Логии должно тратить энергию");

        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KAIROSEKI_JITTE.get()));
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(zombie), 4f);
        helper.assertTrue(player.getHealth() < 20f, "Удар кайросеки должен проходить сквозь Логию");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void kairosekiSuppressesPowers(GameTestHelper helper) {
        Player player = mockPlayer(helper, ModFruits.HIE.getId());
        helper.assertTrue(!FruitPowers.isSuppressed(player), "Силы подавлены без причины");
        player.getInventory().add(new ItemStack(ModItems.KAIROSEKI_SHARD.get()));
        helper.assertTrue(FruitPowers.isSuppressed(player), "Кайросеки в инвентаре должен подавлять силы");
        player.hurt(player.damageSources().freeze(), 3f);
        helper.assertTrue(player.getHealth() < 20f, "С подавленными силами холод должен вредить");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE)
    public static void npcsSpawnArmedAndMarinesHuntWantedPirates(GameTestHelper helper) {
        PirateGrunt pirate = helper.spawnWithNoFreeWill(ModEntities.PIRATE_GRUNT.get(), 1, 2, 1);
        pirate.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(pirate.blockPosition()), MobSpawnType.EVENT, null);
        helper.assertTrue(!pirate.getMainHandItem().isEmpty(), "Пират появился без оружия");

        MarineRecruit marine = helper.spawnWithNoFreeWill(ModEntities.MARINE_RECRUIT.get(), 3, 2, 3);
        marine.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(marine.blockPosition()), MobSpawnType.EVENT, null);
        helper.assertTrue(!marine.getMainHandItem().isEmpty(), "Дозорный появился без оружия");

        Player player = mockPlayer(helper, null);
        PlayerData data = ModAttachments.get(player);
        data.setFaction(Faction.PIRATE);
        data.setBounty(1_000L);
        helper.assertTrue(!MarineRecruit.isWanted(player), "Мелкий пират не должен быть в розыске");
        data.setBounty(50_000_000L);
        helper.assertTrue(MarineRecruit.isWanted(player), "Пират с большой наградой должен быть в розыске");
        data.setFaction(Faction.MARINE);
        helper.assertTrue(!MarineRecruit.isWanted(player), "Дозорный не может быть в розыске");
        helper.succeed();
    }

    @GameTest(templateNamespace = NS, template = TEMPLATE, timeoutTicks = 60)
    public static void fireProjectileHurtsTarget(GameTestHelper helper) {
        // Кадавр (husk) не горит на солнце, поэтому поджог точно от снаряда. Дождь тушит огонь — включаем ясную погоду.
        helper.getLevel().setWeatherParameters(6000, 0, false, false);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.HUSK, 2, 2, 4);
        ElementalProjectile projectile = new ElementalProjectile(ModEntities.ELEMENTAL_PROJECTILE.get(), helper.getLevel());
        Vec3 start = helper.absoluteVec(new Vec3(2.5, 2.8, 0.5));
        projectile.setPos(start.x, start.y, start.z);
        projectile.setElement(ElementalProjectile.Element.FIRE);
        projectile.setNoGravity(true);
        projectile.shoot(0, 0, 1, 1.0f, 0f);
        helper.getLevel().addFreshEntity(projectile);
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "Снаряд ещё не попал");
            helper.assertTrue(target.isOnFire(), "Огненный снаряд должен поджигать: fireTicks=" + target.getRemainingFireTicks()
                    + ", hp=" + target.getHealth() + ", source=" + (target.getLastDamageSource() == null ? "null" : target.getLastDamageSource().getMsgId())
                    + ", projectileAlive=" + projectile.isAlive() + ", tick=" + helper.getTick());
        });
    }
}
