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

    private ModEffects() {
    }

    /** Отдельный класс: конструктор MobEffect защищён. */
    public static final class SeastoneEffect extends MobEffect {
        SeastoneEffect() {
            super(MobEffectCategory.HARMFUL, 0x2E6F8E);
        }
    }
}
