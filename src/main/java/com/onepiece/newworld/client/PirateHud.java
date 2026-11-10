package com.onepiece.newworld.client;

import com.onepiece.newworld.combat.CombatStyle;
import com.onepiece.newworld.combat.SwordTechniques;
import com.onepiece.newworld.abilities.Ability;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.fruits.FruitPowers;
import com.onepiece.newworld.fruits.ModFruits;
import com.onepiece.newworld.haki.HakiService;
import com.onepiece.newworld.haki.HakiType;
import com.onepiece.newworld.progression.Faction;
import com.onepiece.newworld.progression.PlayerData;
import com.onepiece.newworld.progression.Progression;
import com.onepiece.newworld.progression.ProgressionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Компактная панель слева по центру экрана: уровень, энергия, способности с перезарядкой,
 * Хаки и награда/звание. Пока персонаж «пустой», показывается только подсказка про экран персонажа.
 */
public final class PirateHud {
    private static final int WIDTH = 156;
    private static final int LINE = 11;
    private static final int PADDING = 4;

    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DIM = 0xFF8A8A8A;
    private static final int COLOR_SUPPRESSED = 0xFF5FC9E8;
    private static final int COLOR_GOLD = 0xFFFFC94A;
    private static final int COLOR_HAKI_ON = 0xFFE24A4A;
    private static final int COLOR_OBSERVATION = 0xFFC48CFF;
    private static final int COLOR_MARINE = 0xFF7FB2FF;
    private static final int COLOR_STYLE = 0xFFE0E0E0;

    private PirateHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.options.hideGui || player.isSpectator() || mc.screen != null) {
            return;
        }
        PlayerData data = ModAttachments.get(player);
        Font font = mc.font;
        renderCombo(graphics, font);
        CombatStyle style = CombatStyle.of(player);
        boolean swordMode = style != null && style.usesSword() && !player.isShiftKeyDown();
        DevilFruit fruit = ModFruits.get(data.getFruitId());
        int armament = data.getHakiLevel(HakiType.ARMAMENT);
        int observation = data.getHakiLevel(HakiType.OBSERVATION);
        boolean usesEnergy = fruit != null || armament > 0 || observation > 0 || (style != null && style.usesSword());

        if (!usesEnergy && data.getFaction() == Faction.NONE && data.getBounty() == 0) {
            Component hint = Component.translatable("hud.onepiece_newworld.character_hint", ModKeys.label(ModKeys.CHARACTER));
            graphics.drawString(font, hint, 4, graphics.guiHeight() / 2, COLOR_DIM, true);
            return;
        }

        long now = mc.level.getGameTime();
        List<Row> rows = new ArrayList<>();
        rows.add(Row.text(Component.translatable("hud.onepiece_newworld.level", data.getLevel())
                .append(" · ").append(data.getFaction().displayName()), COLOR_TEXT));
        rows.add(Row.bar(Row.Kind.XP));

        boolean suppressed = fruit != null && FruitPowers.isSuppressed(player);
        if (fruit != null) {
            Component title = suppressed
                    ? fruit.displayName().append(Component.translatable("hud.onepiece_newworld.suppressed"))
                    : fruit.displayName();
            rows.add(Row.text(title, suppressed ? COLOR_SUPPRESSED : 0xFF000000 | fruit.color()));
        }
        if (usesEnergy) {
            rows.add(Row.bar(Row.Kind.ENERGY));
        }
        if (style != null) {
            int mastery = data.getWeaponMastery(style.id());
            rows.add(Row.text(Component.translatable("hud.onepiece_newworld.style." + style.id())
                    .append(Component.translatable("hud.onepiece_newworld.mastery_tier", Progression.weaponTier(mastery) + 1)), COLOR_STYLE));
        }
        if (swordMode) {
            List<Ability> techniques = SwordTechniques.all();
            for (int i = 0; i < techniques.size(); i++) {
                Ability ability = techniques.get(i);
                int cd = data.cooldownRemaining(ability.id(), now);
                int color = cd > 0 || data.getEnergy() < ability.energyCost() ? COLOR_DIM : COLOR_TEXT;
                rows.add(new Row(Row.Kind.TEXT, keyed(ModKeys.ABILITY_SLOTS[i], ability.displayName()), seconds(cd), color));
            }
        } else if (fruit != null) {
            int count = Math.min(fruit.abilityCount(), ModKeys.ABILITY_SLOTS.length);
            for (int i = 0; i < count; i++) {
                Ability ability = fruit.ability(i);
                int cd = data.cooldownRemaining(ability.id(), now);
                int color = suppressed ? COLOR_SUPPRESSED
                        : cd > 0 || data.getEnergy() < ability.energyCost() ? COLOR_DIM : COLOR_TEXT;
                rows.add(new Row(Row.Kind.TEXT, keyed(ModKeys.ABILITY_SLOTS[i], ability.displayName()), seconds(cd), color));
            }
        }
        if (armament > 0) {
            boolean on = data.isArmamentActive();
            rows.add(new Row(Row.Kind.TEXT, keyed(ModKeys.ARMAMENT, Component.translatable("hud.onepiece_newworld.armament", armament)),
                    on ? Component.translatable("hud.onepiece_newworld.on") : null, on ? COLOR_HAKI_ON : COLOR_TEXT));
        }
        if (observation > 0) {
            int cd = data.cooldownRemaining(HakiService.OBSERVATION_COOLDOWN_ID, now);
            rows.add(new Row(Row.Kind.TEXT, keyed(ModKeys.OBSERVATION, Component.translatable("hud.onepiece_newworld.observation", observation)),
                    seconds(cd), cd > 0 ? COLOR_DIM : COLOR_OBSERVATION));
        }
        if (data.getFaction() == Faction.MARINE) {
            rows.add(Row.text(Progression.marineRankName(data.getMerit()), COLOR_MARINE));
        } else if (data.getBounty() > 0) {
            rows.add(Row.text(Component.translatable("hud.onepiece_newworld.bounty",
                    ProgressionService.formatBerry(data.getBounty())), COLOR_GOLD));
        }

        int height = rows.stream().mapToInt(Row::height).sum() + PADDING * 2 - 2;
        int x = 4;
        int y = graphics.guiHeight() / 2 - height / 2;
        graphics.fill(x, y, x + WIDTH, y + height, 0x80000000);

        int cursor = y + PADDING;
        for (Row row : rows) {
            switch (row.kind()) {
                case XP -> drawXp(graphics, x + PADDING, cursor, WIDTH - PADDING * 2, data);
                case ENERGY -> drawEnergy(graphics, font, x + PADDING, cursor, WIDTH - PADDING * 2, data);
                case TEXT -> {
                    graphics.drawString(font, row.text(), x + PADDING, cursor, row.color(), true);
                    if (row.right() != null) {
                        graphics.drawString(font, row.right(), x + WIDTH - PADDING - font.width(row.right()), cursor, row.color(), true);
                    }
                }
            }
            cursor += row.height();
        }
    }

    /** Счётчик попаданий подряд справа от прицела. */
    private static void renderCombo(GuiGraphics graphics, Font font) {
        int combo = ClientCombat.visibleCombo();
        if (combo < 2) {
            return;
        }
        int alpha = Math.max(0x10, (int) (0xFF * ClientCombat.comboFreshness()));
        Component text = Component.translatable("hud.onepiece_newworld.combo", combo);
        int x = graphics.guiWidth() / 2 + 14;
        int y = graphics.guiHeight() / 2 - 4;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        float scale = combo >= 10 ? 1.5f : 1.2f;
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(font, text, 0, 0, (alpha << 24) | (combo >= 10 ? 0xFF5533 : 0xFFD24A), true);
        graphics.pose().popPose();
    }

    private static void drawXp(GuiGraphics graphics, int x, int y, int width, PlayerData data) {
        float ratio = data.getLevel() >= Progression.MAX_LEVEL ? 1f
                : (float) data.getXp() / Progression.xpToNextLevel(data.getLevel());
        graphics.fill(x, y, x + width, y + 2, 0xFF202020);
        graphics.fill(x, y, x + (int) (width * ratio), y + 2, 0xFF7ED957);
    }

    private static void drawEnergy(GuiGraphics graphics, Font font, int x, int y, int width, PlayerData data) {
        int height = 8;
        float max = data.getMaxEnergy();
        float ratio = max <= 0 ? 0 : data.getEnergy() / max;
        graphics.fill(x, y, x + width, y + height, 0xFF202020);
        int fillColor = data.isArmamentActive() ? 0xFF9B2B2B : 0xFF2F8FD8;
        graphics.fill(x + 1, y + 1, x + 1 + (int) ((width - 2) * ratio), y + height - 1, fillColor);
        String value = (int) data.getEnergy() + "/" + (int) max;
        graphics.drawString(font, value, x + width / 2 - font.width(value) / 2, y, COLOR_TEXT, true);
    }

    private static Component keyed(KeyMapping key, Component name) {
        return Component.literal("[" + ModKeys.label(key) + "] ").append(name);
    }

    @Nullable
    private static Component seconds(int ticks) {
        return ticks <= 0 ? null : Component.literal(String.format(Locale.ROOT, "%.1fs", ticks / 20f));
    }

    private record Row(Kind kind, Component text, @Nullable Component right, int color) {
        enum Kind { TEXT, XP, ENERGY }

        static Row text(Component text, int color) {
            return new Row(Kind.TEXT, text, null, color);
        }

        static Row bar(Kind kind) {
            return new Row(kind, Component.empty(), null, 0);
        }

        int height() {
            return switch (kind) {
                case TEXT -> LINE;
                case XP -> 5;
                case ENERGY -> 11;
            };
        }
    }
}
