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

    private ModEffects() {
    }

    public static final class SimpleEffect extends MobEffect {
        SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    /** Отдельный класс: конструктор MobEffect защищён. */
    public static final class SeastoneEffect extends MobEffect {
        SeastoneEffect() {
            super(MobEffectCategory.HARMFUL, 0x2E6F8E);
        }
    }
}
