package com.onepiece.newworld.core;

import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.combat.CombatState;
import com.onepiece.newworld.progression.PlayerData;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, OnePieceNewWorld.MODID);

    /** Данные персонажа. Сохраняются в мир и копируются при возрождении. */
    public static final Supplier<AttachmentType<PlayerData>> PLAYER_DATA = ATTACHMENT_TYPES.register("player_data",
            () -> AttachmentType.builder(() -> new PlayerData())
                    .serialize(PlayerData.CODEC)
                    .copyOnDeath()
                    .build());

    /** Состояние боя (комбо, блок, уклонение). Живёт только в памяти: после перезахода сбрасывается. */
    public static final Supplier<AttachmentType<CombatState>> COMBAT_STATE =
            ATTACHMENT_TYPES.register("combat_state",
                    () -> AttachmentType.builder(() -> new CombatState()).build());

    private ModAttachments() {
    }

    public static PlayerData get(Player player) {
        return player.getData(PLAYER_DATA);
    }

    public static CombatState combat(Player player) {
        return player.getData(COMBAT_STATE);
    }
}
