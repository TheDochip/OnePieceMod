package com.onepiece.newworld.combat;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModConfig;
import com.onepiece.newworld.core.ModEffects;
import com.onepiece.newworld.haki.ConquerorService;
import com.onepiece.newworld.networking.PlayAnimationPayload;
import com.onepiece.newworld.progression.PlayerData;
import com.onepiece.newworld.progression.Progression;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ближний бой: цепочки ударов, тяжёлый удар, удар в воздухе, блок и уклонение.
 *
 * <p>Клиент только сообщает «нажата атака / блок / уклонение». Попадания, урон, время между ударами,
 * энергия и перезарядки считаются здесь, на сервере.</p>
 */
public final class CombatService {
    /** Сколько тиков после восстановления удара ещё можно продолжить цепочку. */
    public static final int COMBO_WINDOW = 14;
    /** Окно парирования после начала блока, тиков. */
    public static final int PARRY_WINDOW = 6;
    /** Оглушение того, чей удар парировали. */
    public static final int PARRY_STUN = 30;
    /** Сколько длится усиленная контратака после парирования. */
    public static final int COUNTER_WINDOW = 20;
    public static final float COUNTER_BONUS = 1.5f;
    /** Казнь: добивающий удар по врагу с малым здоровьем. */
    public static final float EXECUTE_HEALTH = 0.15f;
    public static final float EXECUTE_BONUS = 2.0f;

    public static final int DODGE_INVULNERABLE = 8;
    public static final int DODGE_COOLDOWN = 16;
    public static final float DODGE_COST = 15f;
    /** Энергия за каждую единицу урона, принятую на блок. */
    public static final float BLOCK_COST_PER_DAMAGE = 1.5f;
    public static final int GUARD_BREAK_STUN = 20;

    private static final ResourceLocation BLOCK_SLOW_ID = OnePieceNewWorld.id("combat.block_slow");

    private CombatService() {
    }

    // ===================== атака =====================

    /**
     * Удар игрока. {@code heavy} — тяжёлый удар (атака с зажатым Shift).
     *
     * @return true, если удар выполнен (даже если никого не задел)
     */
    public static boolean attack(Player player, boolean heavy) {
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator() || !player.isAlive()
                || Incapacity.check(player)) {
            return false;
        }
        CombatStyle style = CombatStyle.of(player);
        if (style == null) {
            return false;
        }
        CombatState state = ModAttachments.combat(player);
        long now = level.getGameTime();
        if (now < state.nextAttackAt) {
            return false;
        }
        if (state.isBlocking()) {
            stopBlocking(player);
        }
        if (now > state.comboExpires) {
            state.comboIndex = 0;
            state.hitStreak = 0;
        }

        boolean airborne = !player.onGround() && !player.isInWater() && !player.onClimbable();
        AttackDefinition attack;
        if (heavy) {
            attack = style.heavy();
            state.comboIndex = 0;
        } else if (airborne) {
            attack = style.air();
        } else {
            List<AttackDefinition> chain = style.chain();
            attack = chain.get(state.comboIndex % chain.size());
            state.comboIndex = (state.comboIndex + 1) % chain.size();
        }
        state.nextAttackAt = now + attack.recovery();
        state.comboExpires = state.nextAttackAt + COMBO_WINDOW;

        PlayerData data = ModAttachments.get(player);
        boolean counter = state.hasCounter(now);
        state.counterUntil = 0;

        float base = baseDamage(player, style, data) * attack.damage() * (counter ? COUNTER_BONUS : 1f);
        boolean coated = ConquerorService.hasCoating(player);
        if (coated) {
            base *= ConquerorService.COATING_DAMAGE;
        }
        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) + attack.extraReach()
                + (coated ? ConquerorService.COATING_REACH : 0);
        DamageSource source = player.damageSources().playerAttack(player);
        ItemStack weapon = player.getMainHandItem();

        int hits = 0;
        for (LivingEntity target : targets(player, reach, attack.minDot())) {
            float damage = base;
            if (attack.finisher() && target.getHealth() <= target.getMaxHealth() * EXECUTE_HEALTH) {
                damage *= EXECUTE_BONUS;
            }
            if (!weapon.isEmpty()) {
                damage = EnchantmentHelper.modifyDamage(level, weapon, target, source, damage);
            }
            // Удары цепочки быстрее стандартной неуязвимости после урона — сбрасываем её.
            target.invulnerableTime = 0;
            if (!target.hurt(source, damage)) {
                continue;
            }
            hits++;
            player.setLastHurtMob(target);
            applyImpact(player, target, attack);
            EnchantmentHelper.doPostAttackEffects(level, target, source);
        }

        if (hits > 0) {
            state.hitStreak += hits;
            if (style.usesSword() && !player.getAbilities().instabuild) {
                weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
            grantWeaponXp(player, style, hits);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    style.usesSword() ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 1f, attack.finisher() ? 0.8f : 1.1f);
        } else {
            state.hitStreak = 0;
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_WEAK,
                    SoundSource.PLAYERS, 0.7f, 1.3f);
        }
        if (style.usesSword()) {
            Vec3 look = player.getLookAngle();
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + look.x * 1.2, player.getY(0.6),
                    player.getZ() + look.z * 1.2, 1, 0, 0, 0, 0);
        }
        playAnimation(player, attack.animation(), state.hitStreak);
        return true;
    }

    /** Отбрасывание, подброс, жонглирование в воздухе и оглушение после попадания. */
    private static void applyImpact(Player player, LivingEntity target, AttackDefinition attack) {
        if (attack.knockback() > 0) {
            target.knockback(attack.knockback(), player.getX() - target.getX(), player.getZ() - target.getZ());
        }
        Vec3 motion = target.getDeltaMovement();
        if (attack.launch() > 0) {
            target.setDeltaMovement(motion.x, attack.launch(), motion.z);
            target.hurtMarked = true;
        } else if (!target.onGround() && !player.onGround()) {
            // Жонглирование: пока бьём в воздухе, цель не падает.
            target.setDeltaMovement(motion.x * 0.5, Math.max(motion.y, 0.28), motion.z * 0.5);
            target.hurtMarked = true;
        }
        if (attack.stun() > 0) {
            target.addEffect(new MobEffectInstance(ModEffects.STUNNED, attack.stun(), 0, false, true), player);
        }
    }

    /** Цели в дуге перед игроком, в пределах дальности и прямой видимости. */
    public static List<LivingEntity> targets(Player player, double reach, double minDot) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(reach + 1.0),
                e -> e != player && e.isAlive() && !e.isSpectator() && !player.isAlliedTo(e))) {
            Vec3 closest = closestPoint(entity, eye);
            Vec3 to = closest.subtract(eye);
            double distance = to.length();
            if (distance > reach) {
                continue;
            }
            Vec3 toCenter = entity.getBoundingBox().getCenter().subtract(eye).normalize();
            if (distance > 0.6 && toCenter.dot(look) < minDot) {
                continue;
            }
            if (player.hasLineOfSight(entity)) {
                result.add(entity);
            }
        }
        return result;
    }

    private static Vec3 closestPoint(LivingEntity entity, Vec3 from) {
        var box = entity.getBoundingBox();
        return new Vec3(
                Math.max(box.minX, Math.min(from.x, box.maxX)),
                Math.max(box.minY, Math.min(from.y, box.maxY)),
                Math.max(box.minZ, Math.min(from.z, box.maxZ)));
    }

    /**
     * Базовый урон удара: урон оружия (атрибут игрока), бонус кулаков от уровня,
     * ступень мастерства стиля и множитель из конфига.
     */
    public static float baseDamage(Player player, CombatStyle style, PlayerData data) {
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (style == CombatStyle.FISTS) {
            damage += 2f + 0.04f * data.getLevel();
        } else if (style == CombatStyle.THREE_SWORDS) {
            damage *= 1.15f;
        }
        int tier = Progression.weaponTier(data.getWeaponMastery(style.id()));
        damage *= 1f + 0.06f * tier;
        return (float) (damage * ModConfig.get(ModConfig.MELEE_DAMAGE_MULTIPLIER));
    }

    public static void grantWeaponXp(Player player, CombatStyle style, int amount) {
        int xp = (int) Math.round(amount * ModConfig.get(ModConfig.WEAPON_XP_MULTIPLIER));
        if (xp > 0) {
            ModAttachments.get(player).addWeaponMastery(style.id(), xp);
        }
    }

    // ===================== блок =====================

    public static boolean startBlocking(Player player) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || Incapacity.check(player)) {
            return false;
        }
        CombatStyle style = CombatStyle.of(player);
        CombatState state = ModAttachments.combat(player);
        if (style == null || state.isBlocking()) {
            return false;
        }
        state.blockStartedAt = level.getGameTime();
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(BLOCK_SLOW_ID, -0.6,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        playAnimation(player, OnePieceNewWorld.id(style == CombatStyle.FISTS ? "fists_block" : "katana_block"), 0);
        return true;
    }

    public static void stopBlocking(Player player) {
        CombatState state = ModAttachments.combat(player);
        if (!state.isBlocking()) {
            return;
        }
        state.blockStartedAt = -1;
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(BLOCK_SLOW_ID);
        }
        playAnimation(player, null, 0);
    }

    // ===================== уклонение =====================

    /**
     * Рывок-уклонение в направлении (dirX, dirZ) в мировых координатах. Нулевое направление — назад.
     * Даёт {@link #DODGE_INVULNERABLE} тиков неуязвимости; в воздухе — один рывок до приземления.
     */
    public static boolean dodge(Player player, float dirX, float dirZ) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || player.isSpectator()
                || Incapacity.check(player)) {
            return false;
        }
        CombatState state = ModAttachments.combat(player);
        long now = level.getGameTime();
        boolean airborne = !player.onGround();
        if (now < state.nextDodgeAt || (airborne && state.airDashUsed)) {
            return false;
        }
        PlayerData data = ModAttachments.get(player);
        if (!player.getAbilities().instabuild && !data.trySpendEnergy(DODGE_COST)) {
            return false;
        }
        Vec3 dir = new Vec3(dirX, 0, dirZ);
        if (!Double.isFinite(dir.x) || !Double.isFinite(dir.z) || dir.lengthSqr() < 1.0E-4) {
            Vec3 look = player.getLookAngle();
            dir = new Vec3(-look.x, 0, -look.z);
        }
        if (dir.lengthSqr() < 1.0E-4) {
            dir = new Vec3(0, 0, -1);
        }
        dir = dir.normalize();
        if (state.isBlocking()) {
            stopBlocking(player);
        }
        double strength = airborne ? 0.9 : 1.15;
        player.setDeltaMovement(dir.x * strength, airborne ? 0.15 : 0.22, dir.z * strength);
        player.hurtMarked = true;
        player.fallDistance = 0;
        state.dodgeInvulnerableUntil = now + DODGE_INVULNERABLE;
        state.nextDodgeAt = now + DODGE_COOLDOWN;
        if (airborne) {
            state.airDashUsed = true;
        }
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 6, 0.2, 0.05, 0.2, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
                SoundSource.PLAYERS, 0.8f, 1.6f);
        playAnimation(player, OnePieceNewWorld.id(airborne ? "common_dash" : "common_dodge"), 0);
        return true;
    }

    // ===================== каждый тик =====================

    public static void tick(Player player) {
        CombatState state = ModAttachments.combat(player);
        if (player.onGround()) {
            state.airDashUsed = false;
        }
        if (state.isBlocking() && (CombatStyle.of(player) == null || Incapacity.check(player) || !player.isAlive())) {
            stopBlocking(player);
        }
        if (state.hitStreak > 0 && player.level().getGameTime() > state.comboExpires + 20) {
            state.hitStreak = 0;
        }
    }

    // ===================== анимации =====================

    /**
     * Проиграть анимацию у всех, кто видит игрока (и у него самого).
     * {@code animation == null} — остановить текущую анимацию боя.
     */
    public static void playAnimation(Player player, ResourceLocation animation, int combo) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer,
                    new PlayAnimationPayload(player.getId(), Optional.ofNullable(animation), combo));
        }
    }
}
