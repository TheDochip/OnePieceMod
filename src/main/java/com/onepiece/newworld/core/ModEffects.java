package com.onepiece.newworld.core;

import com.onepiece.newworld.OnePieceNewWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, OnePieceNewWorld.MODID);

    /** «Морской камень»: блокирует силы фрукта, замедляет и ослабляет. */
    public static final DeferredHolder<MobEffect, MobEffect> SEASTONE = MOB_EFFECTS.register("seastone",
            () -> new SeastoneEffect()
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, OnePieceNewWorld.id("effect.seastone_slow"),
                            -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, OnePieceNewWorld.id("effect.seastone_weak"),
                            -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Оглушение: нельзя двигаться, прыгать и атаковать. Накладывается парированием и пробитием блока. */
    public static final DeferredHolder<MobEffect, MobEffect> STUNNED = MOB_EFFECTS.register("stunned",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0xF2D64B)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, OnePieceNewWorld.id("effect.stunned_slow"),
                            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, OnePieceNewWorld.id("effect.stunned_jump"),
                            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /**
     * Потеря сознания от волны Королевского Хаки: нельзя двигаться, атаковать и пользоваться силами,
     * мобы теряют цель. Логика — в {@link com.onepiece.newworld.haki.ConquerorService}.
     */
    public static final DeferredHolder<MobEffect, MobEffect> KNOCKED_OUT = MOB_EFFECTS.register("knocked_out",
            () -> new KnockedOutEffect()
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, OnePieceNewWorld.id("effect.knocked_out_slow"),
                            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH, OnePieceNewWorld.id("effect.knocked_out_jump"),
                            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Подавление Королевским Хаки: силы фрукта заблокированы, как от морского камня, но без слабости. */
    public static final DeferredHolder<MobEffect, MobEffect> HAKI_SUPPRESSION = MOB_EFFECTS.register("haki_suppression",
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, 0x8B1E3F));

    private ModEffects() {
    }

    public static final class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    /** Каждый тик сбрасывает цель и путь моба, чтобы он «лежал без сознания». */
    public static final class KnockedOutEffect extends MobEffect {
        KnockedOutEffect() {
            super(MobEffectCategory.HARMFUL, 0x3A0A14);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public boolean applyEffectTick(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
            if (entity instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
                mob.setJumping(false);
            }
            return true;
        }
    }

    /** Отдельный класс: конструктор MobEffect защищён. */
    public static final class SeastoneEffect extends MobEffect {
        SeastoneEffect() {
            super(MobEffectCategory.HARMFUL, 0x2E6F8E);
        }
    }
}
