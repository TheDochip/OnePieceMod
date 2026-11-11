package com.onepiece.newworld.world;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Очередь блоков, которые нужно вернуть на место (режим разрушения RESTORE).
 * Хранится в папке data измерения, поэтому переживает перезапуск сервера.
 *
 * <p>Блок возвращается, только если место всё ещё пустое: постройки игроков не затираются.</p>
 */
public class DestructionData extends SavedData {
    private static final String NAME = "onepiece_newworld_destruction";
    private static final int MAX_RESTORES_PER_TICK = 256;
    /** Если чанк не загружен, повторная попытка через 10 секунд. */
    private static final int RETRY_DELAY = 200;

    /** @param placed что поставлено на место блока (null — воздух); блок вернётся, только если там всё ещё это */
    private record Entry(BlockPos pos, BlockState state, @Nullable BlockState placed, long restoreAt) {
    }

    private final Deque<Entry> queue = new ArrayDeque<>();

    public static DestructionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DestructionData::new, DestructionData::load, null), NAME);
    }

    public void remember(BlockPos pos, BlockState state, long restoreAt) {
        remember(pos, state, null, restoreAt);
    }

    /** Запомнить блок, на место которого временно поставлен другой (например, потрескавшаяся земля). */
    public void remember(BlockPos pos, BlockState state, @Nullable BlockState placed, long restoreAt) {
        queue.addLast(new Entry(pos.immutable(), state, placed, restoreAt));
        setDirty();
    }

    public int pending() {
        return queue.size();
    }

    void tick(ServerLevel level) {
        if (queue.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        int restored = 0;
        while (!queue.isEmpty() && queue.peekFirst().restoreAt() <= now && restored < MAX_RESTORES_PER_TICK) {
            Entry entry = queue.pollFirst();
            restored++;
            if (!level.isLoaded(entry.pos())) {
                queue.addLast(new Entry(entry.pos(), entry.state(), entry.placed(), now + RETRY_DELAY));
                continue;
            }
            BlockState current = level.getBlockState(entry.pos());
            if (current.isAir() || current.canBeReplaced() || current == entry.placed()) {
                level.setBlock(entry.pos(), entry.state(), Block.UPDATE_ALL);
            }
        }
        if (restored > 0) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry entry : queue) {
            CompoundTag item = new CompoundTag();
            item.putLong("pos", entry.pos().asLong());
            item.put("state", NbtUtils.writeBlockState(entry.state()));
            item.putLong("at", entry.restoreAt());
            if (entry.placed() != null) {
                item.put("placed", NbtUtils.writeBlockState(entry.placed()));
            }
            list.add(item);
        }
        tag.put("blocks", list);
        return tag;
    }

    private static DestructionData load(CompoundTag tag, HolderLookup.Provider registries) {
        DestructionData data = new DestructionData();
        HolderGetter<Block> blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (Tag raw : tag.getList("blocks", Tag.TAG_COMPOUND)) {
            CompoundTag item = (CompoundTag) raw;
            BlockState placed = item.contains("placed") ? NbtUtils.readBlockState(blocks, item.getCompound("placed")) : null;
            data.queue.addLast(new Entry(BlockPos.of(item.getLong("pos")),
                    NbtUtils.readBlockState(blocks, item.getCompound("state")), placed, item.getLong("at")));
        }
        return data;
    }
}
