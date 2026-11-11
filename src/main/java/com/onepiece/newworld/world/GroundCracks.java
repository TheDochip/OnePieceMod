package com.onepiece.newworld.world;

import com.onepiece.newworld.core.ModConfig;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Временные трещины в земле от столкновения Королевских Хаки: часть блоков поверхности
 * заменяется «треснувшими» вариантами и возвращается через 30 секунд (если на месте ничего не построили).
 */
public final class GroundCracks {
    public static final int RESTORE_TICKS = 30 * 20;

    private static final Map<Block, Block> CRACKED = Map.ofEntries(
            Map.entry(Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT),
            Map.entry(Blocks.DIRT, Blocks.COARSE_DIRT),
            Map.entry(Blocks.PODZOL, Blocks.COARSE_DIRT),
            Map.entry(Blocks.MYCELIUM, Blocks.COARSE_DIRT),
            Map.entry(Blocks.DIRT_PATH, Blocks.COARSE_DIRT),
            Map.entry(Blocks.STONE, Blocks.COBBLESTONE),
            Map.entry(Blocks.ANDESITE, Blocks.COBBLESTONE),
            Map.entry(Blocks.DIORITE, Blocks.COBBLESTONE),
            Map.entry(Blocks.GRANITE, Blocks.COBBLESTONE),
            Map.entry(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE),
            Map.entry(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS),
            Map.entry(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS),
            Map.entry(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS),
            Map.entry(Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS),
            Map.entry(Blocks.SAND, Blocks.SANDSTONE),
            Map.entry(Blocks.RED_SAND, Blocks.RED_SANDSTONE),
            Map.entry(Blocks.SNOW_BLOCK, Blocks.PACKED_ICE));

    private GroundCracks() {
    }

    @Nullable
    public static Block crackedVariant(Block block) {
        return CRACKED.get(block);
    }

    /**
     * Потрескать землю вокруг точки.
     *
     * @param owner  кто вызвал (для защиты спавна), может быть null
     * @param count  сколько раз пытаться
     * @return сколько блоков потрескалось
     */
    public static int crack(ServerLevel level, BlockPos center, int radius, int count, @Nullable Player owner) {
        if (!ModConfig.get(ModConfig.CONQUEROR_GROUND_CRACKS)
                || ModConfig.get(ModConfig.DESTRUCTION_MODE) == ModConfig.DestructionMode.OFF) {
            return 0;
        }
        RandomSource random = level.getRandom();
        DestructionData data = DestructionData.get(level);
        long restoreAt = level.getGameTime() + RESTORE_TICKS;
        int cracked = 0;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            BlockPos column = center.offset((int) Math.round(Math.cos(angle) * distance), 0, (int) Math.round(Math.sin(angle) * distance));
            BlockPos ground = findGround(level, column);
            if (ground == null || (owner != null && !level.mayInteract(owner, ground))) {
                continue;
            }
            BlockState state = level.getBlockState(ground);
            Block target = crackedVariant(state.getBlock());
            if (target == null) {
                continue;
            }
            BlockState placed = target.defaultBlockState();
            data.remember(ground, state, placed, restoreAt);
            level.setBlock(ground, placed, Block.UPDATE_ALL);
            level.levelEvent(2001, ground, Block.getId(state));
            cracked++;
        }
        return cracked;
    }

    /** Верхний твёрдый блок в столбце рядом с высотой центра (±3), над которым воздух. */
    @Nullable
    private static BlockPos findGround(ServerLevel level, BlockPos column) {
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos pos = column.above(dy);
            if (!level.isLoaded(pos)) {
                return null;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.hasBlockEntity() && level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return null;
    }
}
