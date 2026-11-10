package com.onepiece.newworld.world;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.combat.CutPower;
import com.onepiece.newworld.core.ModConfig;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Разрушение блоков ударами. Единая точка для всех будущих разрушительных способностей:
 * учитывает режим из конфига, защиту спавна и приватов (BlockEvent.BreakEvent),
 * правило mobGriefing для мобов и общий лимит блоков за тик на измерение.
 */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class DestructionManager {
    /** Блоки, которые не режутся никогда (кайросеки и т. п.). */
    public static final TagKey<Block> UNCUTTABLE = TagKey.create(Registries.BLOCK, OnePieceNewWorld.id("uncuttable"));

    public enum CutResult {
        /** Пусто (воздух, жидкость) — удар летит дальше. */
        EMPTY,
        /** Блок разрублен. */
        CUT,
        /** Блок не поддался. */
        BLOCKED,
        /** Исчерпан лимит блоков на этот тик. */
        LIMIT
    }

    /** Измерение → {игровое время, сколько блоков уже срезано в этот тик}. */
    private static final Map<ResourceKey<Level>, long[]> BUDGET = new HashMap<>();

    private DestructionManager() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            DestructionData.get(level).tick(level);
        }
    }

    /**
     * Попробовать разрубить блок.
     *
     * @param particles показывать ли частицы разрушения (для части блоков, чтобы не нагружать клиент)
     */
    public static CutResult tryCut(ServerLevel level, BlockPos pos, int power, @Nullable Entity owner, boolean particles) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            return CutResult.EMPTY;
        }
        boolean solid = !state.getCollisionShape(level, pos).isEmpty();
        ModConfig.DestructionMode mode = ModConfig.get(ModConfig.DESTRUCTION_MODE);
        if (mode == ModConfig.DestructionMode.OFF || state.is(UNCUTTABLE) || state.hasBlockEntity()) {
            return solid ? CutResult.BLOCKED : CutResult.EMPTY;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (!CutPower.canCut(state, hardness, power) || !mayBreak(level, pos, state, owner)) {
            return solid ? CutResult.BLOCKED : CutResult.EMPTY;
        }
        if (!consumeBudget(level)) {
            return CutResult.LIMIT;
        }
        if (mode == ModConfig.DestructionMode.RESTORE) {
            DestructionData.get(level).remember(pos, state,
                    level.getGameTime() + ModConfig.get(ModConfig.RESTORE_SECONDS) * 20L);
        } else if (ModConfig.get(ModConfig.DROP_BLOCKS)) {
            Block.dropResources(state, level, pos, null, owner, ItemStack.EMPTY);
        }
        if (particles) {
            level.levelEvent(2001, pos, Block.getId(state));
        }
        // Затопленный блок оставляет после себя воду.
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        return CutResult.CUT;
    }

    private static boolean mayBreak(ServerLevel level, BlockPos pos, BlockState state, @Nullable Entity owner) {
        if (owner instanceof Player player) {
            if (!level.mayInteract(player, pos) || !player.mayBuild()) {
                return false;
            }
            return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled();
        }
        return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    private static boolean consumeBudget(ServerLevel level) {
        long[] budget = BUDGET.computeIfAbsent(level.dimension(), key -> new long[2]);
        long now = level.getGameTime();
        if (budget[0] != now) {
            budget[0] = now;
            budget[1] = 0;
        }
        if (budget[1] >= ModConfig.get(ModConfig.BLOCKS_PER_TICK)) {
            return false;
        }
        budget[1]++;
        return true;
    }
}
