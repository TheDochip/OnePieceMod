package com.onepiece.newworld.abilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Поиск целей и простые формы эффектов. Количество частиц везде ограничено,
 * чтобы эффекты не нагружали клиент (п. 17 ТЗ).
 */
public final class Targeting {
    private Targeting() {
    }

    /** Точка, где взгляд игрока упирается в блок (или конец дистанции). */
    public static Vec3 rayEnd(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    /** Живые существа на линии взгляда до первой стены, от ближних к дальним. */
    public static List<LivingEntity> onRay(ServerPlayer player, double range, double width) {
        Vec3 start = player.getEyePosition();
        Vec3 end = rayEnd(player, range);
        AABB area = new AABB(start, end).inflate(width + 1.0);
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator())) {
            if (entity.getBoundingBox().inflate(width).clip(start, end).isPresent()) {
                result.add(entity);
            }
        }
        result.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        return result;
    }

    /** Существа в конусе перед игроком. minDot: 1.0 — точно по центру, 0.5 — примерно ±60°. */
    public static List<LivingEntity> inCone(ServerPlayer player, double range, double minDot) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range), e -> e != player && e.isAlive() && !e.isSpectator())) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist <= range && dist > 0.01 && to.normalize().dot(look) >= minDot && player.hasLineOfSight(entity)) {
                result.add(entity);
            }
        }
        return result;
    }

    /** Существа в радиусе вокруг точки, кроме самого игрока. */
    public static List<LivingEntity> around(ServerPlayer player, Vec3 center, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator() && e.position().distanceToSqr(center) <= radius * radius);
    }

    public static void knockAway(LivingEntity from, LivingEntity target, double strength) {
        target.knockback(strength, from.getX() - target.getX(), from.getZ() - target.getZ());
    }

    /** Линия частиц. Шаг не меньше 0.25 блока — не больше ~4 частиц на блок. */
    public static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step, int perPoint, double spread) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.01) {
            return;
        }
        Vec3 dir = delta.normalize();
        double safeStep = Math.max(0.25, step);
        for (double d = 0; d <= length; d += safeStep) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(particle, p.x, p.y, p.z, perPoint, spread, spread, spread, 0.01);
        }
    }

    /** Кольцо частиц. Не больше 96 точек. */
    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
        int safePoints = Math.min(96, points);
        for (int i = 0; i < safePoints; i++) {
            double angle = Math.PI * 2 * i / safePoints;
            level.sendParticles(particle, center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius,
                    1, 0.05, 0.1, 0.05, 0.02);
        }
    }
}
