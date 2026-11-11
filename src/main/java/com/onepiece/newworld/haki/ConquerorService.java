package com.onepiece.newworld.haki;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModConfig;
import com.onepiece.newworld.core.ModEffects;
import com.onepiece.newworld.core.ServerScheduler;
import com.onepiece.newworld.fruits.FruitPowers;
import com.onepiece.newworld.networking.PlayerSync;
import com.onepiece.newworld.networking.ScreenShakePayload;
import com.onepiece.newworld.progression.PlayerData;
import com.onepiece.newworld.world.GroundCracks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * Королевское Хаки.
 *
 * <ul>
 *     <li>Даётся с шансом из конфига при первом входе в мир или командой. Пока спит, игрок о нём не знает;
 *     первое пробуждение — когда здоровье в бою падает ниже 20%.</li>
 *     <li>Волна (клавиша H): кто слабее вдвое по воле — теряет сознание; с уровня 2 — давление
 *     (замедление и слабость) на тех, кто устоял; с уровня 3 — подавление сил фрукта.</li>
 *     <li>Уровень 4: покрытие — вместе с Вооружением удары дальше и сильнее, пробивают блок слабых.
 *     Уровень 5: любой приём пробивает Логию и оглушает.</li>
 *     <li>Два владельца 4+ уровня, бьющие друг друга покрытыми ударами, вызывают столкновение.</li>
 * </ul>
 */
public final class ConquerorService {
    public static final ResourceLocation WAVE_COOLDOWN_ID = OnePieceNewWorld.id("haki/conqueror_wave");
    public static final float WAVE_COST = 40f;
    public static final int WAVE_COOLDOWN = 30 * 20;
    /** Порог здоровья для первого пробуждения. */
    public static final float AWAKEN_HEALTH = 0.2f;
    public static final int COATING_LEVEL = 4;
    public static final int MASTER_LEVEL = 5;
    /** Покрытие: урон ближнего боя и дальность без касания. */
    public static final float COATING_DAMAGE = 1.25f;
    public static final double COATING_REACH = 2.0;
    /** Окно, в которое оба должны ударить друг друга покрытыми ударами. */
    private static final int CLASH_WINDOW = 20;
    private static final int CLASH_COOLDOWN = 40;

    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.85f, 0.05f, 0.1f), 1.4f);
    private static final DustParticleOptions BLACK = new DustParticleOptions(new Vector3f(0.05f, 0.0f, 0.05f), 1.6f);

    /** Последний покрытый удар игрока: по кому и когда. */
    private record CoatedHit(UUID target, long time) {
    }

    private static final Map<UUID, CoatedHit> LAST_COATED_HIT = new HashMap<>();
    private static final Map<UUID, Long> LAST_CLASH = new HashMap<>();

    private ConquerorService() {
    }

    public static int level(Player player) {
        return ModAttachments.get(player).getHakiLevel(HakiType.CONQUEROR);
    }

    /** Покрытие: Королевское 4+ и включённое Вооружение. */
    public static boolean hasCoating(Player player) {
        PlayerData data = ModAttachments.get(player);
        return data.isArmamentActive() && data.getHakiLevel(HakiType.CONQUEROR) >= COATING_LEVEL;
    }

    /** Королевское 5: каждый приём несёт волю владельца (пока включено Вооружение). */
    public static boolean hasMasterCoating(Player player) {
        PlayerData data = ModAttachments.get(player);
        return data.isArmamentActive() && data.getHakiLevel(HakiType.CONQUEROR) >= MASTER_LEVEL;
    }

    // ===================== получение =====================

    /** Один раз за жизнь персонажа разыграть шанс. @return true, если выпало */
    public static boolean rollOnFirstJoin(Player player) {
        PlayerData data = ModAttachments.get(player);
        if (data.isConquerorRolled()) {
            return false;
        }
        data.setConquerorRolled(true);
        boolean lucky = player.getRandom().nextDouble() < ModConfig.get(ModConfig.CONQUEROR_CHANCE);
        if (lucky) {
            data.setConquerorInnate(true);
            OnePieceNewWorld.LOGGER.info("{} рождён с Королевским Хаки", player.getGameProfile().getName());
        }
        return lucky;
    }

    /**
     * Первое пробуждение: спящее Королевское Хаки просыпается, когда здоровье падает ниже 20% в бою.
     * Сразу выпускает бесплатную волну.
     */
    public static boolean tryAwaken(Player player) {
        PlayerData data = ModAttachments.get(player);
        if (!data.hasConqueror() || data.getHakiLevel(HakiType.CONQUEROR) > 0
                || player.getHealth() > player.getMaxHealth() * AWAKEN_HEALTH || !player.isAlive()) {
            return false;
        }
        data.setHakiXp(HakiType.CONQUEROR, HakiType.thresholdFor(1));
        player.displayClientMessage(Component.translatable("message.onepiece_newworld.haki_awakened.conqueror")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
        release(player, 1);
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerSync.sync(serverPlayer);
        }
        return true;
    }

    // ===================== волна =====================

    /** Клавиша H: проверки, расход и перезарядка, затем волна. */
    public static void wave(ServerPlayer player) {
        PlayerData data = ModAttachments.get(player);
        int level = data.getHakiLevel(HakiType.CONQUEROR);
        if (level <= 0) {
            // Спящее Хаки ничем себя не выдаёт.
            if (!data.hasConqueror()) {
                player.displayClientMessage(Component.translatable("message.onepiece_newworld.haki_locked.conqueror")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        if (!player.isAlive() || player.hasEffect(ModEffects.KNOCKED_OUT)) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (data.cooldownRemaining(WAVE_COOLDOWN_ID, now) > 0) {
            return;
        }
        if (!player.isCreative() && !data.trySpendEnergy(WAVE_COST)) {
            player.displayClientMessage(Component.translatable("message.onepiece_newworld.no_energy")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        release(player, level);
        data.startCooldown(WAVE_COOLDOWN_ID, now, WAVE_COOLDOWN);
        PlayerSync.sync(player);
    }

    /**
     * Сама волна. Без проверок — их делает {@link #wave}; вызывается и при пробуждении.
     *
     * @return сколько существ потеряли сознание
     */
    public static int release(Player player, int level) {
        if (!(player.level() instanceof ServerLevel serverLevel) || level <= 0) {
            return 0;
        }
        double radius = 8 + 4 * level;
        int myWill = Willpower.of(player);
        int knockedOut = 0;
        Vec3 center = player.position();
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator() && !player.isAlliedTo(e)
                        && e.distanceToSqr(center) <= radius * radius)) {
            if (target instanceof Player other && level(other) > 0 && level(other) >= level) {
                // Равная или большая воля: волна не берёт, сильные сталкиваются.
                if (level >= COATING_LEVEL) {
                    clash(player, other);
                }
                continue;
            }
            int will = Willpower.of(target);
            if (will * 2 < myWill) {
                knockOut(target, 100 + serverLevel.getRandom().nextInt(101));
                knockedOut++;
            } else if (level >= 2 && will < myWill) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1), player);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0), player);
            }
            if (level >= 3 && will < myWill && target instanceof Player victim && FruitPowers.fruitOf(victim) != null) {
                int duration = Math.min(12 * 20, 3 * 20 + 20 * ((myWill - will) / 10));
                victim.addEffect(new MobEffectInstance(ModEffects.HAKI_SUPPRESSION, duration, 0, false, true), player);
                victim.displayClientMessage(Component.translatable("message.onepiece_newworld.conqueror_suppressed")
                        .withStyle(ChatFormatting.DARK_RED), true);
            }
        }
        waveVisuals(serverLevel, center, radius);
        shake(serverLevel, center, radius * 2, 0.5f + 0.2f * level, 12);
        serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS,
                1.2f, 0.6f);
        if (player instanceof ServerPlayer serverPlayer) {
            HakiService.grantXp(serverPlayer, HakiType.CONQUEROR, 10 + 2 * knockedOut);
            serverPlayer.displayClientMessage(Component.translatable("message.onepiece_newworld.conqueror_wave", knockedOut)
                    .withStyle(ChatFormatting.DARK_RED), true);
        }
        return knockedOut;
    }

    public static void knockOut(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(ModEffects.KNOCKED_OUT, ticks, 0, false, true));
        if (target instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, false, false));
            player.displayClientMessage(Component.translatable("message.onepiece_newworld.knocked_out")
                    .withStyle(ChatFormatting.DARK_RED), true);
        } else if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getEyeY(), target.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        }
    }

    /** Кольцо волны расходится за 10 тиков, по 15 частиц за тик (150 всего). */
    private static void waveVisuals(ServerLevel level, Vec3 center, double radius) {
        for (int step = 0; step < 10; step++) {
            double r = radius * (step + 1) / 10.0;
            int finalStep = step;
            ServerScheduler.schedule(level, step, () -> {
                for (int i = 0; i < 15; i++) {
                    double angle = Math.PI * 2 * (i + finalStep * 0.5) / 15;
                    ParticleOptions particle = i % 2 == 0 ? RED : BLACK;
                    level.sendParticles(particle, center.x + Math.cos(angle) * r, center.y + 0.6, center.z + Math.sin(angle) * r,
                            1, 0, 0.2, 0, 0);
                }
            });
        }
    }

    /** Потрясти камеру у всех игроков в радиусе. */
    public static void shake(ServerLevel level, Vec3 center, double radius, float intensity, int ticks) {
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(center) <= radius * radius) {
                PacketDistributor.sendToPlayer(viewer, new ScreenShakePayload(intensity, ticks));
            }
        }
    }

    // ===================== покрытие и столкновения =====================

    /**
     * Покрытый удар {@code attacker} по {@code victim}. Если жертва тоже бьёт покрытыми ударами
     * и ударила в ответ в течение секунды — столкновение.
     */
    public static void onCoatedHit(Player attacker, Player victim) {
        long now = attacker.level().getGameTime();
        CoatedHit answer = LAST_COATED_HIT.get(victim.getUUID());
        if (answer != null && answer.target().equals(attacker.getUUID()) && now - answer.time() <= CLASH_WINDOW
                && hasCoating(victim)) {
            LAST_COATED_HIT.remove(victim.getUUID());
            clash(attacker, victim);
            return;
        }
        LAST_COATED_HIT.put(attacker.getUUID(), new CoatedHit(victim.getUUID(), now));
    }

    /** Столкновение двух Королевских Хаки. @return true, если произошло (не на перезарядке) */
    public static boolean clash(Player a, Player b) {
        if (!(a.level() instanceof ServerLevel level) || a.level() != b.level()) {
            return false;
        }
        long now = level.getGameTime();
        if (now - LAST_CLASH.getOrDefault(a.getUUID(), Long.MIN_VALUE / 2) < CLASH_COOLDOWN
                || now - LAST_CLASH.getOrDefault(b.getUUID(), Long.MIN_VALUE / 2) < CLASH_COOLDOWN) {
            return false;
        }
        LAST_CLASH.put(a.getUUID(), now);
        LAST_CLASH.put(b.getUUID(), now);

        // 1. Оба замирают, между ними — чёрно-красные молнии.
        a.addEffect(new MobEffectInstance(ModEffects.STUNNED, 10, 0, false, false));
        b.addEffect(new MobEffectInstance(ModEffects.STUNNED, 10, 0, false, false));
        for (int t = 0; t < 10; t += 2) {
            ServerScheduler.schedule(level, t, () -> lightning(level, a.getEyePosition(), b.getEyePosition()));
        }

        // 2. Ударная волна: всех вокруг отбрасывает, слабые теряют сознание.
        Vec3 mid = a.position().add(b.position()).scale(0.5);
        int weaker = Math.min(Willpower.of(a), Willpower.of(b));
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(mid, mid).inflate(10),
                e -> e != a && e != b && e.isAlive() && !e.isSpectator() && e.distanceToSqr(mid) <= 100)) {
            other.knockback(1.2, mid.x - other.getX(), mid.z - other.getZ());
            if (Willpower.of(other) * 2 < weaker) {
                knockOut(other, 100);
            }
        }

        // 3. Трещины, тряска камеры, звук.
        GroundCracks.crack(level, BlockPos.containing(mid), 6, 40, a);
        shake(level, mid, 32, 2.0f, 20);
        level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2f, 0.5f);
        level.playSound(null, mid.x, mid.y, mid.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.4f);

        // 4. Итог: воля выше более чем на 10% — победа, иначе отлетают оба.
        int willA = Willpower.of(a);
        int willB = Willpower.of(b);
        if (willA > willB * 1.1) {
            repel(a, b, true);
        } else if (willB > willA * 1.1) {
            repel(b, a, true);
        } else {
            repel(a, b, false);
            repel(b, a, false);
        }
        for (Player player : new Player[]{a, b}) {
            if (player instanceof ServerPlayer serverPlayer) {
                HakiService.grantXp(serverPlayer, HakiType.CONQUEROR, 30);
                serverPlayer.displayClientMessage(Component.translatable("message.onepiece_newworld.conqueror_clash")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            }
        }
        return true;
    }

    private static void repel(Player winner, Player loser, boolean stun) {
        loser.knockback(1.5, winner.getX() - loser.getX(), winner.getZ() - loser.getZ());
        loser.hurtMarked = true;
        if (stun) {
            loser.addEffect(new MobEffectInstance(ModEffects.STUNNED, 30, 0, false, true), winner);
        }
    }

    /** Ломаная линия частиц между двумя точками. */
    private static void lightning(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int segments = Math.max(4, (int) (delta.length() * 2));
        Vec3 previous = from;
        for (int i = 1; i <= segments; i++) {
            Vec3 point = from.add(delta.scale((double) i / segments));
            if (i < segments) {
                point = point.add(level.getRandom().nextGaussian() * 0.25, level.getRandom().nextGaussian() * 0.25,
                        level.getRandom().nextGaussian() * 0.25);
            }
            Vec3 step = point.subtract(previous);
            for (int k = 0; k < 3; k++) {
                Vec3 p = previous.add(step.scale(k / 3.0));
                level.sendParticles(k % 2 == 0 ? BLACK : RED, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
            previous = point;
        }
    }

    /** Забыть игрока при выходе. */
    public static void forget(Player player) {
        LAST_COATED_HIT.remove(player.getUUID());
        LAST_CLASH.remove(player.getUUID());
    }
}
