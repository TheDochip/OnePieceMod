package com.onepiece.newworld.progression;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.entities.MarineRecruit;
import com.onepiece.newworld.entities.PirateGrunt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Награды за победы. Пират копит награду за голову, дозорный — заслуги.
 * Опыт персонажа получают все.
 */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class ProgressionEvents {
    private ProgressionEvents() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(event.getSource().getEntity() instanceof ServerPlayer killer) || killer == victim) {
            return;
        }
        ProgressionService.grantXp(killer, xpFor(victim));

        PlayerData killerData = ModAttachments.get(killer);
        if (killerData.getFaction() == Faction.MARINE) {
            int merit = meritFor(victim);
            if (merit != 0) {
                ProgressionService.addMerit(killer, merit);
            }
        } else {
            ProgressionService.addBounty(killer, bountyFor(victim));
        }
    }

    private static boolean isBoss(LivingEntity entity) {
        return entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    static int xpFor(LivingEntity victim) {
        if (victim instanceof Player player) {
            return 50 + 5 * ModAttachments.get(player).getLevel();
        }
        if (isBoss(victim)) {
            return 500;
        }
        if (victim instanceof PirateGrunt || victim instanceof MarineRecruit) {
            return 25;
        }
        if (victim instanceof Enemy) {
            return Math.max(1, (int) (victim.getMaxHealth() * 0.8f));
        }
        return 1;
    }

    static long bountyFor(LivingEntity victim) {
        if (victim instanceof Player player) {
            return 2_000_000L + ModAttachments.get(player).getBounty() / 2;
        }
        if (isBoss(victim)) {
            return 80_000_000L;
        }
        if (victim instanceof MarineRecruit) {
            return 3_000_000L; // нападение на Дозор — серьёзное преступление
        }
        if (victim instanceof PirateGrunt) {
            return 1_000_000L;
        }
        if (victim instanceof Enemy) {
            return (long) (victim.getMaxHealth() * 2_000L);
        }
        if (victim instanceof AbstractVillager || victim instanceof IronGolem) {
            return 500_000L;
        }
        return 0L;
    }

    static int meritFor(LivingEntity victim) {
        if (victim instanceof Player player) {
            long bounty = ModAttachments.get(player).getBounty();
            return bounty > 0 ? (int) Math.min(2000, 10 + bounty / 1_000_000L) : -20;
        }
        if (isBoss(victim)) {
            return 200;
        }
        if (victim instanceof PirateGrunt) {
            return 5;
        }
        if (victim instanceof MarineRecruit || victim instanceof AbstractVillager || victim instanceof IronGolem) {
            return -20;
        }
        if (victim instanceof Enemy) {
            return 1;
        }
        return 0;
    }
}
