package com.onepiece.newworld.client;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.combat.CombatStyle;
import com.onepiece.newworld.core.ModEffects;
import com.onepiece.newworld.networking.CombatActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Перехват ЛКМ/ПКМ для боевой системы, пока в руке меч или ничего:
 * <ul>
 *     <li>ЛКМ — удар цепочки, Shift + ЛКМ — тяжёлый удар (по блоку ЛКМ по-прежнему ломает блок);</li>
 *     <li>зажатая ПКМ — блок, нажатие в момент удара врага — парирование;</li>
 *     <li>Left Alt — уклонение в сторону движения (в воздухе — один рывок).</li>
 * </ul>
 * Клиент только сообщает о нажатии — всё остальное решает сервер.
 */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID, value = Dist.CLIENT)
public final class CombatInput {
    /** Не чаще одного пакета атаки за столько тиков (сервер всё равно проверяет сам). */
    private static final int ATTACK_SEND_INTERVAL = 3;

    private CombatInput() {
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.isSpectator()) {
            return;
        }
        CombatStyle style = CombatStyle.of(player);
        if (style == null) {
            return;
        }
        HitResult hit = mc.hitResult;
        if (event.isAttack()) {
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                return;
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            long tick = mc.level.getGameTime();
            if (player.hasEffect(ModEffects.STUNNED) || tick - ClientCombat.lastAttackSentTick < ATTACK_SEND_INTERVAL) {
                return;
            }
            ClientCombat.lastAttackSentTick = tick;
            send(new CombatActionPayload(player.isShiftKeyDown()
                    ? CombatActionPayload.Action.HEAVY_ATTACK : CombatActionPayload.Action.ATTACK));
        } else if (event.isUseItem()) {
            if (!player.getOffhandItem().isEmpty() && !CombatStyle.isSword(player.getOffhandItem())) {
                return;
            }
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                return;
            }
            if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof AbstractVillager) {
                return;
            }
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!ClientCombat.blocking) {
                ClientCombat.blocking = true;
                send(new CombatActionPayload(CombatActionPayload.Action.BLOCK_START));
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        if (ClientCombat.blocking && (!mc.options.keyUse.isDown() || mc.screen != null || CombatStyle.of(player) == null)) {
            ClientCombat.blocking = false;
            send(new CombatActionPayload(CombatActionPayload.Action.BLOCK_STOP));
        }
        while (ModKeys.DODGE.consumeClick()) {
            if (mc.screen == null && !player.isSpectator()) {
                float forward = player.input.forwardImpulse;
                float left = player.input.leftImpulse;
                float yaw = player.getYRot() * Mth.DEG_TO_RAD;
                float sin = Mth.sin(yaw);
                float cos = Mth.cos(yaw);
                float x = left * cos - forward * sin;
                float z = forward * cos + left * sin;
                send(new CombatActionPayload(CombatActionPayload.Action.DODGE, x, z));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCombat.reset();
    }

    private static void send(CombatActionPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
