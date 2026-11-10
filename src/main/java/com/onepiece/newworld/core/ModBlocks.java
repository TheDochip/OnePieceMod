package com.onepiece.newworld.core;

import com.onepiece.newworld.OnePieceNewWorld;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OnePieceNewWorld.MODID);

    /** Руда кайросеки (морского камня). Генерируется под океанами. */
    public static final DeferredBlock<DropExperienceBlock> KAIROSEKI_ORE = BLOCKS.register("kairoseki_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_KAIROSEKI_ORE = BLOCKS.register("deepslate_kairoseki_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE)));
    public static final DeferredBlock<Block> KAIROSEKI_BLOCK = BLOCKS.register("kairoseki_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));

    private ModBlocks() {
    }
}
