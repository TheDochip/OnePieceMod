package com.onepiece.newworld.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.combat.CombatStyle;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.fruits.ModFruits;
import com.onepiece.newworld.haki.HakiType;
import com.onepiece.newworld.networking.PlayerSync;
import com.onepiece.newworld.progression.Faction;
import com.onepiece.newworld.progression.PlayerData;
import com.onepiece.newworld.progression.Progression;
import com.onepiece.newworld.progression.ProgressionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /onepiece — команды для тестов и администраторов (кроме info нужны права оператора).
 * <pre>
 * /onepiece info [игрок]
 * /onepiece fruit &lt;игрок&gt; &lt;id фрукта|none&gt;
 * /onepiece haki &lt;игрок&gt; &lt;observation|armament|conqueror&gt; &lt;опыт&gt;
 * /onepiece conqueror &lt;игрок&gt; &lt;give|awaken|remove&gt;
 * /onepiece level &lt;игрок&gt; &lt;уровень&gt;
 * /onepiece mastery &lt;игрок&gt; &lt;0..1000&gt;
 * /onepiece weapon &lt;игрок&gt; &lt;fists|katana|three_swords&gt; &lt;0..1500&gt;
 * /onepiece bounty &lt;игрок&gt; &lt;сумма&gt;
 * /onepiece merit &lt;игрок&gt; &lt;заслуги&gt;
 * /onepiece faction &lt;игрок&gt; &lt;none|pirate|marine&gt;
 * /onepiece cooldowns &lt;игрок&gt;   — сбросить перезарядки
 * /onepiece patrol &lt;игрок&gt;      — вызвать отряд Дозора
 * </pre>
 */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class ModCommands {
    private ModCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("onepiece");

        root.then(Commands.literal("info")
                .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> info(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> fruitTarget = Commands.argument("target", EntityArgument.player());
        fruitTarget.then(Commands.literal("none").executes(ctx -> setFruit(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), null)));
        for (ResourceLocation id : ModFruits.REGISTRY.keySet()) {
            fruitTarget.then(Commands.literal(id.getPath())
                    .executes(ctx -> setFruit(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"), id)));
        }
        root.then(op("fruit").then(fruitTarget));

        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> hakiTarget = Commands.argument("target", EntityArgument.player());
        for (HakiType type : HakiType.values()) {
            hakiTarget.then(Commands.literal(type.getSerializedName())
                    .then(Commands.argument("xp", IntegerArgumentType.integer(0)).executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                        PlayerData data = ModAttachments.get(target);
                        data.setHakiXp(type, IntegerArgumentType.getInteger(ctx, "xp"));
                        return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.haki_set",
                                type.displayName(), data.getHakiXp(type), data.getHakiLevel(type)));
                    })));
        }
        root.then(op("haki").then(hakiTarget));

        // /onepiece conqueror <игрок> give|awaken|remove — выдать спящее, пробудить или забрать Королевское Хаки
        root.then(op("conqueror").then(Commands.argument("target", EntityArgument.player())
                .then(Commands.literal("give").executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    ModAttachments.get(target).setConquerorInnate(true);
                    ModAttachments.get(target).setConquerorRolled(true);
                    return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.conqueror_given"));
                }))
                .then(Commands.literal("awaken").executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    PlayerData data = ModAttachments.get(target);
                    if (data.getHakiLevel(HakiType.CONQUEROR) == 0) {
                        data.setHakiXp(HakiType.CONQUEROR, HakiType.thresholdFor(1));
                    }
                    return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.haki_set",
                            HakiType.CONQUEROR.displayName(), data.getHakiXp(HakiType.CONQUEROR), data.getHakiLevel(HakiType.CONQUEROR)));
                }))
                .then(Commands.literal("remove").executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    ModAttachments.get(target).removeConqueror();
                    return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.conqueror_removed"));
                }))));

        root.then(op("level").then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("level", IntegerArgumentType.integer(1, Progression.MAX_LEVEL)).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    PlayerData data = ModAttachments.get(target);
                    data.setLevel(IntegerArgumentType.getInteger(ctx, "level"));
                    ProgressionService.applyLevelHealth(target, data);
                    return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.level_set", data.getLevel()));
                }))));

        root.then(op("mastery").then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("value", IntegerArgumentType.integer(0, Progression.MAX_MASTERY)).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    PlayerData data = ModAttachments.get(target);
                    if (!data.hasFruit()) {
                        ctx.getSource().sendFailure(Component.translatable("command.onepiece_newworld.no_fruit"));
                        return 0;
                    }
                    data.setMastery(data.getFruitId(), IntegerArgumentType.getInteger(ctx, "value"));
                    return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.mastery_set",
                            data.getMastery(data.getFruitId())));
                }))));

        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> weaponTarget = Commands.argument("target", EntityArgument.player());
        for (CombatStyle style : CombatStyle.values()) {
            weaponTarget.then(Commands.literal(style.id())
                    .then(Commands.argument("value", IntegerArgumentType.integer(0, Progression.MAX_WEAPON_MASTERY)).executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                        PlayerData data = ModAttachments.get(target);
                        data.setWeaponMastery(style.id(), IntegerArgumentType.getInteger(ctx, "value"));
                        return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.weapon_mastery_set",
                                Component.translatable("hud.onepiece_newworld.style." + style.id()),
                                data.getWeaponMastery(style.id()), Progression.weaponTier(data.getWeaponMastery(style.id())) + 1));
                    })));
        }
        root.then(op("weapon").then(weaponTarget));

        root.then(op("bounty").then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("amount", LongArgumentType.longArg(0)).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    ModAttachments.get(target).setBounty(LongArgumentType.getLong(ctx, "amount"));
                    return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.bounty_set",
                            ProgressionService.formatBerry(ModAttachments.get(target).getBounty())));
                }))));

        root.then(op("merit").then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                    PlayerData data = ModAttachments.get(target);
                    data.setMerit(IntegerArgumentType.getInteger(ctx, "amount"));
                    return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.merit_set",
                            data.getMerit(), Progression.marineRankName(data.getMerit())));
                }))));

        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> factionTarget = Commands.argument("target", EntityArgument.player());
        for (Faction faction : Faction.values()) {
            factionTarget.then(Commands.literal(faction.getSerializedName()).executes(ctx -> {
                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                ModAttachments.get(target).setFaction(faction);
                return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.faction_set", faction.displayName()));
            }));
        }
        root.then(op("faction").then(factionTarget));

        root.then(op("cooldowns").then(Commands.argument("target", EntityArgument.player()).executes(ctx -> {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            ModAttachments.get(target).clearCooldowns();
            return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.cooldowns_reset"));
        })));

        root.then(op("patrol").then(Commands.argument("target", EntityArgument.player()).executes(ctx -> {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
            int spawned = ProgressionService.sendMarinePatrol(target, 3);
            return done(ctx.getSource(), target, Component.translatable("command.onepiece_newworld.patrol_sent", spawned));
        })));

        dispatcher.register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> op(String name) {
        return Commands.literal(name).requires(source -> source.hasPermission(2));
    }

    private static int done(CommandSourceStack source, ServerPlayer target, Component message) {
        PlayerSync.sync(target);
        source.sendSuccess(() -> Component.empty().append(target.getDisplayName()).append(": ").append(message), true);
        return 1;
    }

    private static int setFruit(CommandSourceStack source, ServerPlayer target, ResourceLocation fruitId) {
        ModAttachments.get(target).setFruit(fruitId);
        DevilFruit fruit = ModFruits.get(fruitId);
        Component name = fruit == null ? Component.translatable("fruit.onepiece_newworld.none") : fruit.displayName();
        return done(source, target, Component.translatable("command.onepiece_newworld.fruit_set", name));
    }

    private static int info(CommandSourceStack source, ServerPlayer target) {
        PlayerData data = ModAttachments.get(target);
        DevilFruit fruit = ModFruits.get(data.getFruitId());
        source.sendSuccess(() -> Component.translatable("command.onepiece_newworld.info",
                target.getDisplayName(),
                data.getLevel(),
                fruit == null ? Component.translatable("fruit.onepiece_newworld.none") : fruit.displayName(),
                data.getMastery(data.getFruitId()),
                data.getHakiLevel(HakiType.OBSERVATION),
                data.getHakiLevel(HakiType.ARMAMENT),
                data.getFaction().displayName(),
                ProgressionService.formatBerry(data.getBounty()),
                data.getMerit()), false);
        return 1;
    }
}
