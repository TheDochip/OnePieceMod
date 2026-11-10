package com.onepiece.newworld.entities;

import com.onepiece.newworld.world.DestructionManager;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Летящий удар мечом: полумесяц, который проходит сквозь врагов (каждого задевает один раз)
 * и разрубает блоки по своей плоскости.
 *
 * <p>Ширина, ориентация (горизонтальный/вертикальный) и цвет синхронизируются с клиентом для отрисовки.
 * Урон, сила разреза и запас блоков известны только серверу.</p>
 *
 * <p>Удар гаснет, когда его центр упирается в блок, который он не может разрубить,
 * когда исчерпан запас блоков или кончилось время жизни.</p>
 */
public class SlashProjectile extends Projectile {
    private static final EntityDataAccessor<Float> WIDTH =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> VERTICAL =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BLACK =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFE =
            SynchedEntityData.defineId(SlashProjectile.class, EntityDataSerializers.INT);

    /** Шаг проверки блоков вдоль пути и поперёк удара. */
    private static final double STEP = 0.5;
    /** Сколько блоков на один уровень силы удар может разрубить за всю жизнь. */
    private static final int BLOCKS_PER_POWER = 60;

    private float damage = 6f;
    private int power;
    private int blocksLeft;
    private final IntOpenHashSet alreadyHit = new IntOpenHashSet();
    private int cutCount;

    public SlashProjectile(EntityType<? extends SlashProjectile> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /**
     * Выпустить удар.
     *
     * @param width    ширина полумесяца, блоков
     * @param vertical true — вертикальный разрез, false — горизонтальный
     * @param power    сила разреза 0..5 (см. {@link com.onepiece.newworld.combat.CutPower})
     * @param life     время жизни, тиков (дальность = скорость × время)
     * @param black    чёрный удар (Хаки Вооружения)
     */
    public static SlashProjectile launch(LivingEntity owner, Vec3 origin, Vec3 direction, float speed, float width,
                                         boolean vertical, float damage, int power, int life, boolean black) {
        SlashProjectile slash = new SlashProjectile(ModEntities.SLASH.get(), owner.level());
        slash.setOwner(owner);
        slash.setPos(origin);
        slash.damage = damage;
        slash.power = power;
        slash.blocksLeft = power * BLOCKS_PER_POWER;
        slash.entityData.set(WIDTH, width);
        slash.entityData.set(VERTICAL, vertical);
        slash.entityData.set(BLACK, black);
        slash.entityData.set(LIFE, life);
        Vec3 dir = direction.normalize();
        slash.setDeltaMovement(dir.scale(speed));
        slash.setYRot((float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG));
        slash.setXRot((float) (Mth.atan2(dir.y, dir.horizontalDistance()) * Mth.RAD_TO_DEG));
        slash.yRotO = slash.getYRot();
        slash.xRotO = slash.getXRot();
        owner.level().addFreshEntity(slash);
        return slash;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WIDTH, 2.5f);
        builder.define(VERTICAL, false);
        builder.define(BLACK, false);
        builder.define(LIFE, 30);
    }

    public float width() {
        return entityData.get(WIDTH);
    }

    public boolean isVertical() {
        return entityData.get(VERTICAL);
    }

    public boolean isBlack() {
        return entityData.get(BLACK);
    }

    public int life() {
        return entityData.get(LIFE);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 from = position();
        Vec3 motion = getDeltaMovement();
        Vec3 to = from.add(motion);
        if (level() instanceof ServerLevel level) {
            if (tickCount > life()) {
                discard();
                return;
            }
            hitEntities(level, from, to);
            if (!cutAlong(level, from, to)) {
                level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 10, 0.3, 0.3, 0.3, 0.2);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3f, 2f);
                discard();
                return;
            }
        }
        setPos(to);
    }

    private Vec3 across(Vec3 dir) {
        if (isVertical()) {
            return new Vec3(0, 1, 0);
        }
        Vec3 side = new Vec3(dir.z, 0, -dir.x);
        return side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
    }

    private void hitEntities(ServerLevel level, Vec3 from, Vec3 to) {
        double half = width() / 2.0;
        AABB swept = new AABB(from, to).inflate(half, isVertical() ? half : 0.6, half);
        Entity owner = getOwner();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, swept,
                e -> e != owner && e.isAlive() && !e.isSpectator() && !alreadyHit.contains(e.getId())
                        && !(owner != null && owner.isAlliedTo(e)))) {
            alreadyHit.add(target.getId());
            target.invulnerableTime = 0;
            if (target.hurt(damageSource(), damage)) {
                Vec3 push = getDeltaMovement().normalize();
                target.knockback(0.6, -push.x, -push.z);
            }
        }
    }

    private DamageSource damageSource() {
        Entity owner = getOwner();
        if (owner instanceof LivingEntity living) {
            return damageSources().mobProjectile(this, living);
        }
        return damageSources().magic();
    }

    /** @return false, если удар упёрся в то, что не может разрубить */
    private boolean cutAlong(ServerLevel level, Vec3 from, Vec3 to) {
        if (power <= 0) {
            // Без силы разреза удар не режет блоки и гаснет о первую стену.
            return !level.getBlockState(BlockPos.containing(to)).isCollisionShapeFullBlock(level, BlockPos.containing(to));
        }
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4) {
            return true;
        }
        Vec3 dir = delta.scale(1.0 / length);
        Vec3 side = across(dir);
        double half = width() / 2.0;
        int steps = Math.max(1, Mth.ceil(length / STEP));
        LongOpenHashSet visited = new LongOpenHashSet();
        Entity owner = getOwner();

        for (int s = 1; s <= steps; s++) {
            Vec3 point = from.add(dir.scale(length * s / steps));
            for (double offset = -half; offset <= half + 1.0E-6; offset += STEP) {
                BlockPos pos = BlockPos.containing(point.add(side.scale(offset)));
                if (!visited.add(pos.asLong())) {
                    continue;
                }
                DestructionManager.CutResult result = DestructionManager.tryCut(level, pos, power, owner, cutCount % 3 == 0);
                if (result == DestructionManager.CutResult.CUT) {
                    cutCount++;
                    if (--blocksLeft <= 0) {
                        return false;
                    }
                } else if (result == DestructionManager.CutResult.LIMIT) {
                    return true;
                } else if (result == DestructionManager.CutResult.BLOCKED && Math.abs(offset) < STEP) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128 * 128;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("damage", damage);
        tag.putInt("power", power);
        tag.putInt("blocks_left", blocksLeft);
        tag.putFloat("width", width());
        tag.putBoolean("vertical", isVertical());
        tag.putBoolean("black", isBlack());
        tag.putInt("life", life());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("damage");
        power = tag.getInt("power");
        blocksLeft = tag.getInt("blocks_left");
        entityData.set(WIDTH, tag.contains("width") ? tag.getFloat("width") : 2.5f);
        entityData.set(VERTICAL, tag.getBoolean("vertical"));
        entityData.set(BLACK, tag.getBoolean("black"));
        entityData.set(LIFE, tag.contains("life") ? tag.getInt("life") : 30);
    }

    /** Для тестов и отладки. */
    public int power() {
        return power;
    }
}
