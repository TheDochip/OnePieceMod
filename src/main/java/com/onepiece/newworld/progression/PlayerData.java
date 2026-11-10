package com.onepiece.newworld.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.onepiece.newworld.core.ModConfig;
import com.onepiece.newworld.haki.HakiType;
import io.netty.buffer.Unpooled;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Все данные персонажа: фрукт, энергия, уровень, Хаки, путь (пират/дозорный), награда, заслуги,
 * мастерство фруктов и перезарядки способностей.
 *
 * <p>Хранится как attachment игрока: сохраняется в мир и переживает смерть.
 * Изменяет данные только сервер; клиент получает копию через {@link com.onepiece.newworld.networking.SyncPlayerDataPayload}.</p>
 *
 * <p>Перезарядки хранятся как «игровое время окончания», поэтому клиент сам считает
 * оставшиеся секунды и серверу не нужно слать пакет каждый тик.</p>
 */
public class PlayerData {
    public static final float BASE_MAX_ENERGY = 100f;

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ResourceLocation.CODEC.optionalFieldOf("fruit").forGetter(d -> Optional.ofNullable(d.fruit)),
            Codec.FLOAT.optionalFieldOf("energy", BASE_MAX_ENERGY).forGetter(d -> d.energy),
            Codec.INT.optionalFieldOf("level", 1).forGetter(d -> d.level),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(d -> d.xp),
            Codec.unboundedMap(HakiType.CODEC, Codec.INT).optionalFieldOf("haki", Map.of()).forGetter(d -> d.hakiXp),
            Faction.CODEC.optionalFieldOf("faction", Faction.NONE).forGetter(d -> d.faction),
            Codec.LONG.optionalFieldOf("bounty", 0L).forGetter(d -> d.bounty),
            Codec.INT.optionalFieldOf("merit", 0).forGetter(d -> d.merit),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).optionalFieldOf("mastery", Map.of()).forGetter(d -> d.mastery),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG).optionalFieldOf("cooldowns", Map.of()).forGetter(d -> d.cooldownEnds)
    ).apply(inst, PlayerData::fromSaved));

    public static final StreamCodec<FriendlyByteBuf, PlayerData> STREAM_CODEC =
            StreamCodec.of((buf, data) -> data.write(buf), PlayerData::read);

    @Nullable
    private ResourceLocation fruit;
    private float energy = BASE_MAX_ENERGY;
    private int level = 1;
    private int xp;
    private final EnumMap<HakiType, Integer> hakiXp = new EnumMap<>(HakiType.class);
    private Faction faction = Faction.NONE;
    private long bounty;
    private int merit;
    private final Map<ResourceLocation, Integer> mastery = new HashMap<>();
    private final Map<ResourceLocation, Long> cooldownEnds = new HashMap<>();

    /** Не сохраняется: после перезахода и смерти Хаки Вооружения выключено. */
    private boolean armamentActive;

    /** Нужно отправить клиенту при ближайшей возможности. */
    private boolean dirty = true;
    /** Изменилась только энергия — можно отправить чуть позже, пачкой. */
    private boolean energyDirty;

    public PlayerData() {
    }

    private static PlayerData fromSaved(Optional<ResourceLocation> fruit, float energy, int level, int xp,
                                        Map<HakiType, Integer> haki, Faction faction, long bounty, int merit,
                                        Map<ResourceLocation, Integer> mastery, Map<ResourceLocation, Long> cooldowns) {
        PlayerData data = new PlayerData();
        data.fruit = fruit.orElse(null);
        data.energy = energy;
        data.level = Math.max(1, Math.min(Progression.MAX_LEVEL, level));
        data.xp = Math.max(0, xp);
        data.hakiXp.putAll(haki);
        data.faction = faction;
        data.bounty = Math.max(0, bounty);
        data.merit = Math.max(0, merit);
        data.mastery.putAll(mastery);
        data.cooldownEnds.putAll(cooldowns);
        return data;
    }

    // ===================== сеть =====================

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(fruit != null);
        if (fruit != null) {
            buf.writeResourceLocation(fruit);
        }
        buf.writeFloat(energy);
        buf.writeVarInt(level);
        buf.writeVarInt(xp);
        for (HakiType type : HakiType.values()) {
            buf.writeVarInt(getHakiXp(type));
        }
        buf.writeBoolean(armamentActive);
        buf.writeVarInt(faction.ordinal());
        buf.writeVarLong(bounty);
        buf.writeVarInt(merit);
        buf.writeVarInt(mastery.size());
        mastery.forEach((id, value) -> {
            buf.writeResourceLocation(id);
            buf.writeVarInt(value);
        });
        buf.writeVarInt(cooldownEnds.size());
        cooldownEnds.forEach((id, end) -> {
            buf.writeResourceLocation(id);
            buf.writeVarLong(end);
        });
    }

    private static PlayerData read(FriendlyByteBuf buf) {
        PlayerData data = new PlayerData();
        data.fruit = buf.readBoolean() ? buf.readResourceLocation() : null;
        data.energy = buf.readFloat();
        data.level = buf.readVarInt();
        data.xp = buf.readVarInt();
        for (HakiType type : HakiType.values()) {
            int value = buf.readVarInt();
            if (value > 0) {
                data.hakiXp.put(type, value);
            }
        }
        data.armamentActive = buf.readBoolean();
        data.faction = Faction.byId(buf.readVarInt());
        data.bounty = buf.readVarLong();
        data.merit = buf.readVarInt();
        int masteryCount = buf.readVarInt();
        for (int i = 0; i < masteryCount; i++) {
            data.mastery.put(buf.readResourceLocation(), buf.readVarInt());
        }
        int cooldownCount = buf.readVarInt();
        for (int i = 0; i < cooldownCount; i++) {
            data.cooldownEnds.put(buf.readResourceLocation(), buf.readVarLong());
        }
        return data;
    }

    /**
     * Независимая копия. Нужна для отправки клиенту: в одиночной игре пакеты не сериализуются,
     * и без копии клиент и сервер делили бы один объект.
     */
    public PlayerData copy() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            write(buf);
            return read(buf);
        } finally {
            buf.release();
        }
    }

    // ===================== фрукт =====================

    @Nullable
    public ResourceLocation getFruitId() {
        return fruit;
    }

    public boolean hasFruit() {
        return fruit != null;
    }

    public void setFruit(@Nullable ResourceLocation fruit) {
        this.fruit = fruit;
        cooldownEnds.clear();
        markDirty();
    }

    public int getMastery(@Nullable ResourceLocation fruitId) {
        return fruitId == null ? 0 : mastery.getOrDefault(fruitId, 0);
    }

    public void addMastery(ResourceLocation fruitId, int amount) {
        mastery.merge(fruitId, amount, (a, b) -> Math.min(Progression.MAX_MASTERY, a + b));
        markDirty();
    }

    public void setMastery(ResourceLocation fruitId, int value) {
        mastery.put(fruitId, Math.max(0, Math.min(Progression.MAX_MASTERY, value)));
        markDirty();
    }

    // ===================== энергия =====================

    public float getEnergy() {
        return energy;
    }

    /** Требует загруженного конфига сервера (на клиенте он приходит при входе в мир). */
    public float getMaxEnergy() {
        double perLevel;
        try {
            perLevel = ModConfig.ENERGY_PER_LEVEL.get();
        } catch (IllegalStateException notLoadedYet) {
            perLevel = ModConfig.ENERGY_PER_LEVEL.getDefault();
        }
        return (float) (BASE_MAX_ENERGY + (level - 1) * perLevel);
    }

    public void setEnergy(float value) {
        float clamped = Math.max(0f, Math.min(getMaxEnergy(), value));
        if (clamped != energy) {
            energy = clamped;
            energyDirty = true;
        }
    }

    public boolean trySpendEnergy(float cost) {
        if (energy < cost) {
            return false;
        }
        setEnergy(energy - cost);
        return true;
    }

    // ===================== уровень =====================

    public int getLevel() {
        return level;
    }

    public int getXp() {
        return xp;
    }

    /** @return сколько уровней получено */
    public int addXp(int amount) {
        if (amount <= 0 || level >= Progression.MAX_LEVEL) {
            return 0;
        }
        xp += amount;
        int gained = 0;
        while (level < Progression.MAX_LEVEL && xp >= Progression.xpToNextLevel(level)) {
            xp -= Progression.xpToNextLevel(level);
            level++;
            gained++;
        }
        if (level >= Progression.MAX_LEVEL) {
            xp = 0;
        }
        markDirty();
        return gained;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(Progression.MAX_LEVEL, level));
        this.xp = 0;
        markDirty();
    }

    // ===================== Хаки =====================

    public int getHakiXp(HakiType type) {
        return hakiXp.getOrDefault(type, 0);
    }

    public int getHakiLevel(HakiType type) {
        return HakiType.levelFor(getHakiXp(type));
    }

    public void setHakiXp(HakiType type, int value) {
        hakiXp.put(type, Math.max(0, value));
        markDirty();
    }

    /** @return true, если после начисления вырос уровень */
    public boolean addHakiXp(HakiType type, int amount) {
        int before = getHakiLevel(type);
        setHakiXp(type, getHakiXp(type) + amount);
        return getHakiLevel(type) > before;
    }

    public boolean isArmamentActive() {
        return armamentActive;
    }

    public void setArmamentActive(boolean active) {
        if (armamentActive != active) {
            armamentActive = active;
            markDirty();
        }
    }

    // ===================== путь, награда, заслуги =====================

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
        markDirty();
    }

    public long getBounty() {
        return bounty;
    }

    public void setBounty(long bounty) {
        this.bounty = Math.max(0L, bounty);
        markDirty();
    }

    public int getMerit() {
        return merit;
    }

    public void setMerit(int merit) {
        this.merit = Math.max(0, merit);
        markDirty();
    }

    // ===================== перезарядки =====================

    /** Сколько тиков осталось до готовности способности. */
    public int cooldownRemaining(ResourceLocation abilityId, long gameTime) {
        Long end = cooldownEnds.get(abilityId);
        return end == null ? 0 : (int) Math.max(0, end - gameTime);
    }

    public void startCooldown(ResourceLocation abilityId, long gameTime, int ticks) {
        cooldownEnds.put(abilityId, gameTime + Math.max(0, ticks));
        markDirty();
    }

    /** Удаляет истёкшие записи, чтобы сохранение не разрасталось. */
    public void pruneCooldowns(long gameTime) {
        cooldownEnds.values().removeIf(end -> end <= gameTime);
    }

    public void clearCooldowns() {
        cooldownEnds.clear();
        markDirty();
    }

    /** После смерти: полная энергия, Хаки выключено. */
    public void onRespawn() {
        armamentActive = false;
        energy = getMaxEnergy();
        markDirty();
    }

    // ===================== синхронизация =====================

    public void markDirty() {
        dirty = true;
    }

    /** @return true, если нужна немедленная отправка (сбрасывает флаг) */
    public boolean consumeDirty() {
        boolean was = dirty;
        dirty = false;
        if (was) {
            energyDirty = false;
        }
        return was;
    }

    /** @return true, если изменилась энергия (сбрасывает флаг) */
    public boolean consumeEnergyDirty() {
        boolean was = energyDirty;
        energyDirty = false;
        return was;
    }
}
