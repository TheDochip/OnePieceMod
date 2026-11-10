package com.onepiece.newworld.fruits;

import com.onepiece.newworld.abilities.Ability;
import com.onepiece.newworld.progression.PlayerData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Дьявольский фрукт: категория, набор способностей и пассивные свойства.
 *
 * <p>Чтобы добавить фрукт: создайте подкласс (или используйте этот класс напрямую),
 * зарегистрируйте способности в {@link com.onepiece.newworld.abilities.ModAbilities},
 * сам фрукт — в {@link ModFruits}, а предмет — в {@link com.onepiece.newworld.core.ModItems}.</p>
 */
public class DevilFruit {
    private final FruitType type;
    private final int color;
    private final int lootWeight;
    private final List<Supplier<? extends Ability>> abilities;

    /**
     * @param color      цвет названия в интерфейсе (RGB)
     * @param lootWeight относительная частота в сундуках: чем больше, тем чаще
     * @param abilities  способности в порядке слотов (клавиши Z, X, C, ...)
     */
    public DevilFruit(FruitType type, int color, int lootWeight, List<Supplier<? extends Ability>> abilities) {
        this.type = type;
        this.color = color;
        this.lootWeight = lootWeight;
        this.abilities = List.copyOf(abilities);
    }

    public FruitType type() {
        return type;
    }

    public int color() {
        return color;
    }

    public int lootWeight() {
        return lootWeight;
    }

    public int abilityCount() {
        return abilities.size();
    }

    public Ability ability(int slot) {
        return abilities.get(slot).get();
    }

    public List<Ability> abilities() {
        return abilities.stream().<Ability>map(Supplier::get).toList();
    }

    /** Полная неуязвимость к этому урону, пока силы фрукта не подавлены. */
    public boolean isImmuneTo(DamageSource source) {
        return false;
    }

    /** Пассивный эффект на сервере, вызывается раз в тик, пока силы не подавлены. */
    public void passiveTick(ServerPlayer player, PlayerData data) {
    }

    /** Частицы, которыми рассыпается тело Логии при уклонении. */
    public ParticleOptions elementParticle() {
        return ParticleTypes.CLOUD;
    }

    public ResourceLocation id() {
        ResourceLocation id = ModFruits.REGISTRY.getKey(this);
        if (id == null) {
            throw new IllegalStateException("Фрукт не зарегистрирован: " + getClass().getName());
        }
        return id;
    }

    public MutableComponent displayName() {
        return Component.translatable(id().toLanguageKey("fruit"));
    }

    public MutableComponent description() {
        return Component.translatable(id().toLanguageKey("fruit", "desc"));
    }
}
