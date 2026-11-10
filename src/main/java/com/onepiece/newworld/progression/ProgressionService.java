package com.onepiece.newworld.progression;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.core.ModConfig;
import com.onepiece.newworld.entities.MarineRecruit;
import com.onepiece.newworld.entities.ModEntities;
import com.onepiece.newworld.networking.PlayerSync;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.levelgen.Heightmap;

/** Опыт, уровни, награда за голову и заслуги Дозора. */
public final class ProgressionService {
    public static final ResourceLocation LEVEL_HEALTH_ID = OnePieceNewWorld.id("level_health");
    private static final long[] BOUNTY_MILESTONES = {
            10_000_000L, 30_000_000L, 100_000_000L, 300_000_000L, 500_000_000L, 1_000_000_000L, 3_000_000_000L
    };

    private ProgressionService() {
    }

    // ===================== опыт =====================

    public static void grantXp(ServerPlayer player, int baseAmount) {
        PlayerData data = ModAttachments.get(player);
        int amount = (int) Math.round(baseAmount * ModConfig.XP_MULTIPLIER.get());
        int gained = data.addXp(amount);
        if (gained > 0) {
            onLevelUp(player, data);
        }
    }

    private static void onLevelUp(ServerPlayer player, PlayerData data) {
        applyLevelHealth(player, data);
        data.setEnergy(data.getMaxEnergy());
        player.setHealth(player.getMaxHealth());
        player.displayClientMessage(Component.translatable("message.onepiece_newworld.level_up", data.getLevel())
                .withStyle(ChatFormatting.GOLD), false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.2f);
        PlayerSync.sync(player);
    }

    /** Бонус к здоровью от уровня. Вызывается при входе, возрождении и повышении уровня. */
    public static void applyLevelHealth(ServerPlayer player, PlayerData data) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.removeModifier(LEVEL_HEALTH_ID);
        double bonus = Progression.bonusHealth(data.getLevel());
        if (bonus > 0) {
            health.addPermanentModifier(new AttributeModifier(LEVEL_HEALTH_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ===================== награда за голову =====================

    public static void addBounty(ServerPlayer player, long amount) {
        if (amount <= 0) {
            return;
        }
        PlayerData data = ModAttachments.get(player);
        long before = data.getBounty();
        long after = before + amount;
        data.setBounty(after);
        for (int i = 0; i < BOUNTY_MILESTONES.length; i++) {
            long milestone = BOUNTY_MILESTONES[i];
            if (before < milestone && after >= milestone) {
                announceWanted(player, after);
                if (ModConfig.MARINE_PATROLS.get()) {
                    sendMarinePatrol(player, Math.min(6, 2 + i));
                }
            }
        }
        PlayerSync.sync(player);
    }

    private static void announceWanted(ServerPlayer player, long bounty) {
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.translatable("message.onepiece_newworld.wanted",
                    player.getDisplayName(), formatBerry(bounty)).withStyle(ChatFormatting.GOLD), false);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1f, 0.8f);
    }

    /** Отряд Дозора высаживается в 12–20 блоках от пирата и сразу начинает погоню. */
    public static int sendMarinePatrol(ServerPlayer player, int count) {
        ServerLevel level = player.serverLevel();
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2;
            double distance = 12 + player.getRandom().nextDouble() * 8;
            BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(),
                    player.getZ() + Math.sin(angle) * distance);
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (!level.getFluidState(pos.below()).isEmpty() || Math.abs(pos.getY() - player.getY()) > 16) {
                continue;
            }
            MarineRecruit recruit = ModEntities.MARINE_RECRUIT.get().create(level);
            if (recruit == null) {
                continue;
            }
            recruit.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getRandom().nextFloat() * 360f, 0f);
            recruit.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            recruit.setTarget(player);
            level.addFreshEntity(recruit);
            spawned++;
        }
        if (spawned > 0) {
            player.displayClientMessage(Component.translatable("message.onepiece_newworld.marine_patrol", spawned)
                    .withStyle(ChatFormatting.AQUA), false);
        }
        return spawned;
    }

    // ===================== заслуги Дозора =====================

    public static void addMerit(ServerPlayer player, int amount) {
        PlayerData data = ModAttachments.get(player);
        int rankBefore = Progression.marineRank(data.getMerit());
        data.setMerit(data.getMerit() + amount);
        int rankAfter = Progression.marineRank(data.getMerit());
        if (rankAfter > rankBefore) {
            player.displayClientMessage(Component.translatable("message.onepiece_newworld.promoted",
                    Progression.marineRankName(data.getMerit())).withStyle(ChatFormatting.AQUA), false);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1f, 1.2f);
        } else if (amount < 0) {
            player.displayClientMessage(Component.translatable("message.onepiece_newworld.merit_lost", -amount)
                    .withStyle(ChatFormatting.RED), true);
        }
        PlayerSync.sync(player);
    }

    public static String formatBerry(long amount) {
        return String.format(Locale.ROOT, "%,d", amount).replace(',', ' ');
    }
}
