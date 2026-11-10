package com.onepiece.newworld.entities;

import com.onepiece.newworld.abilities.Targeting;
import com.onepiece.newworld.fruits.hie.IceHelper;
import java.util.Locale;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Снаряд стихии, летящий без гравитации. Урон и стихия задаются при выстреле на сервере;
 * клиенту передаётся только стихия (для частиц), урон считает сервер.
 */
public class ElementalProjectile extends ThrowableItemProjectile {
    public enum Element {
        FIRE, ICE;

        static Element byId(int id) {
            Element[] values = values();
            return id >= 0 && id < values.length ? values[id] : FIRE;
        }
    }

    private static final EntityDataAccessor<Integer> ELEMENT =
            SynchedEntityData.defineId(ElementalProjectile.class, EntityDataSerializers.INT);
    private static final int MAX_LIFETIME = 60;

    private float damage = 6f;

    public ElementalProjectile(EntityType<? extends ElementalProjectile> type, Level level) {
        super(type, level);
    }

    /** Выпустить снаряд по направлению взгляда игрока. */
    public static ElementalProjectile shoot(ServerPlayer owner, Element element, float damage, float speed) {
        ElementalProjectile projectile = new ElementalProjectile(ModEntities.ELEMENTAL_PROJECTILE.get(), owner.level());
        projectile.setOwner(owner);
        projectile.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
        projectile.setElement(element);
        projectile.damage = damage;
        projectile.setNoGravity(true);
        projectile.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0f, speed, 0f);
        owner.level().addFreshEntity(projectile);
        return projectile;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ELEMENT, Element.FIRE.ordinal());
    }

    public Element getElement() {
        return Element.byId(entityData.get(ELEMENT));
    }

    public void setElement(Element element) {
        entityData.set(ELEMENT, element.ordinal());
        setItem(new ItemStack(element == Element.FIRE ? Items.FIRE_CHARGE : Items.BLUE_ICE));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Vec3 p = position();
            level().addParticle(getElement() == Element.FIRE ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE,
                    p.x, p.y + 0.2, p.z, 0, 0, 0);
        } else if (tickCount > MAX_LIFETIME || isInWater() && getElement() == Element.FIRE) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }
        Entity target = result.getEntity();
        target.hurt(damageSources().thrown(this, getOwner()), damage);
        if (target instanceof LivingEntity living) {
            if (getElement() == Element.FIRE) {
                living.igniteForSeconds(5);
            } else {
                IceHelper.freeze(living, 100, 3);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel serverLevel) {
            impact(serverLevel);
            discard();
        }
    }

    /** Взрыв стихии в точке попадания: задевает всех рядом, кроме владельца. Блоки не ломает. */
    private void impact(ServerLevel level) {
        Vec3 c = position();
        boolean fire = getElement() == Element.FIRE;
        level.sendParticles(fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE, c.x, c.y, c.z, 20, 0.6, 0.6, 0.6, 0.05);
        level.sendParticles(fire ? ParticleTypes.EXPLOSION : ParticleTypes.ITEM_SNOWBALL, c.x, c.y, c.z, fire ? 1 : 12, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, c.x, c.y, c.z, fire ? SoundEvents.FIRECHARGE_USE : SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 1f, fire ? 0.7f : 1.3f);
        Entity owner = getOwner();
        double radius = 2.5;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                e -> e != owner && e.isAlive() && e.position().distanceToSqr(c) <= radius * radius)) {
            nearby.hurt(damageSources().thrown(this, owner), damage * 0.5f);
            if (fire) {
                nearby.igniteForSeconds(3);
            } else {
                IceHelper.freeze(nearby, 60, 2);
            }
            if (owner instanceof LivingEntity livingOwner) {
                Targeting.knockAway(livingOwner, nearby, 0.4);
            }
        }
        if (!fire) {
            IceHelper.freezeWater(level, blockPosition(), 2, 1, 100, 200, random);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("element", getElement().name().toLowerCase(Locale.ROOT));
        tag.putFloat("damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setElement("ice".equals(tag.getString("element")) ? Element.ICE : Element.FIRE);
        damage = tag.getFloat("damage");
    }
}
