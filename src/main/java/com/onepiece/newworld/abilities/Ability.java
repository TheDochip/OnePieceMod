package com.onepiece.newworld.abilities;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * Единый базовый класс всех активных способностей (фрукты, позже — боевые стили и Хаки-техники).
 * Способность хранится в собственном реестре {@link ModAbilities#REGISTRY}, поэтому у неё есть id,
 * по которому сервер считает перезарядки, а клиент рисует интерфейс.
 *
 * <p>Все вычисления (урон, цели, расход энергии) выполняются только на сервере.</p>
 */
public abstract class Ability {
    private final int baseCooldown;
    private final float energyCost;

    protected Ability(int baseCooldownTicks, float energyCost) {
        this.baseCooldown = baseCooldownTicks;
        this.energyCost = energyCost;
    }

    /** Базовая перезарядка в тиках (20 тиков = 1 секунда), до множителей мастерства и конфига. */
    public int baseCooldown() {
        return baseCooldown;
    }

    public float energyCost() {
        return energyCost;
    }

    /**
     * Срабатывание способности на сервере.
     *
     * @return true, если способность сработала: тогда списывается энергия и запускается перезарядка
     */
    public abstract boolean activate(AbilityContext context);

    public ResourceLocation id() {
        ResourceLocation id = ModAbilities.REGISTRY.getKey(this);
        if (id == null) {
            throw new IllegalStateException("Способность не зарегистрирована: " + getClass().getName());
        }
        return id;
    }

    public MutableComponent displayName() {
        return Component.translatable(id().toLanguageKey("ability"));
    }

    public MutableComponent description() {
        return Component.translatable(id().toLanguageKey("ability", "desc"));
    }
}
