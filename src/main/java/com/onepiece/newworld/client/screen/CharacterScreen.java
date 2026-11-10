package com.onepiece.newworld.client.screen;

import com.onepiece.newworld.abilities.Ability;
import com.onepiece.newworld.client.ModKeys;
import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.fruits.DevilFruit;
import com.onepiece.newworld.fruits.ModFruits;
import com.onepiece.newworld.haki.HakiType;
import com.onepiece.newworld.networking.ChooseFactionPayload;
import com.onepiece.newworld.progression.Faction;
import com.onepiece.newworld.progression.PlayerData;
import com.onepiece.newworld.progression.Progression;
import com.onepiece.newworld.progression.ProgressionService;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Экран персонажа (клавиша K): уровень, путь, награда/звание, фрукт и его способности, Хаки.
 * Если путь ещё не выбран, внизу кнопки «Стать пиратом» и «Вступить в Дозор».
 */
public class CharacterScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int PANEL_H = 250;
    private static final int COLOR_TITLE = 0xFFFFC94A;
    private static final int COLOR_TEXT = 0xFFE0E0E0;
    private static final int COLOR_DIM = 0xFF9A9A9A;

    private Button pirateButton;
    private Button marineButton;

    public CharacterScreen() {
        super(Component.translatable("screen.onepiece_newworld.character"));
    }

    private int left() {
        return (width - PANEL_W) / 2;
    }

    private int top() {
        return (height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int y = top() + PANEL_H - 26;
        pirateButton = addRenderableWidget(Button.builder(Component.translatable("screen.onepiece_newworld.become_pirate"),
                b -> choose(Faction.PIRATE)).bounds(left() + 10, y, 155, 20).build());
        marineButton = addRenderableWidget(Button.builder(Component.translatable("screen.onepiece_newworld.join_marines"),
                b -> choose(Faction.MARINE)).bounds(left() + PANEL_W - 165, y, 155, 20).build());
        updateButtons();
    }

    private void choose(Faction faction) {
        PacketDistributor.sendToServer(new ChooseFactionPayload(faction));
    }

    private PlayerData data() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? new PlayerData() : ModAttachments.get(mc.player);
    }

    private void updateButtons() {
        boolean choosing = data().getFaction() == Faction.NONE;
        pirateButton.visible = choosing;
        marineButton.visible = choosing;
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int x = left();
        int y = top();
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xE0101820);
        graphics.fill(x, y, x + PANEL_W, y + 1, 0xFFFFC94A);
        graphics.fill(x + PANEL_W / 2, y + 24, x + PANEL_W / 2 + 1, y + PANEL_H - 32, 0x40FFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        PlayerData data = data();
        int x = left();
        int y = top();
        String name = minecraft != null && minecraft.player != null ? minecraft.player.getName().getString() : "";
        graphics.drawCenteredString(font, Component.literal(name).withStyle(ChatFormatting.BOLD), x + PANEL_W / 2, y + 8, COLOR_TITLE);

        renderCharacterColumn(graphics, data, x + 10, y + 28, PANEL_W / 2 - 20);
        renderFruitColumn(graphics, data, x + PANEL_W / 2 + 10, y + 28, PANEL_W / 2 - 20);

        if (data.getFaction() == Faction.NONE) {
            graphics.drawCenteredString(font, Component.translatable("screen.onepiece_newworld.choose_path"),
                    x + PANEL_W / 2, y + PANEL_H - 40, COLOR_DIM);
        }
    }

    private void renderCharacterColumn(GuiGraphics g, PlayerData data, int x, int y, int w) {
        int cy = y;
        cy = line(g, Component.translatable("screen.onepiece_newworld.level", data.getLevel()), x, cy, COLOR_TITLE);
        if (data.getLevel() < Progression.MAX_LEVEL) {
            int need = Progression.xpToNextLevel(data.getLevel());
            cy = line(g, Component.translatable("screen.onepiece_newworld.xp", data.getXp(), need), x, cy, COLOR_DIM);
            g.fill(x, cy, x + w, cy + 3, 0xFF202020);
            g.fill(x, cy, x + (int) (w * Math.min(1f, (float) data.getXp() / need)), cy + 3, 0xFF7ED957);
            cy += 7;
        }
        cy = line(g, Component.translatable("screen.onepiece_newworld.energy", (int) data.getMaxEnergy()), x, cy, COLOR_TEXT);
        cy = line(g, Component.translatable("screen.onepiece_newworld.health_bonus",
                String.format(Locale.ROOT, "%.0f", Progression.bonusHealth(data.getLevel()) / 2)), x, cy, COLOR_TEXT);
        cy += 4;

        cy = line(g, Component.translatable("screen.onepiece_newworld.path", data.getFaction().displayName()), x, cy, COLOR_TITLE);
        if (data.getFaction() == Faction.MARINE) {
            cy = line(g, Component.translatable("screen.onepiece_newworld.rank", Progression.marineRankName(data.getMerit())), x, cy, COLOR_TEXT);
            int next = Progression.nextRankMerit(data.getMerit());
            cy = line(g, next < 0
                    ? Component.translatable("screen.onepiece_newworld.merit_max", data.getMerit())
                    : Component.translatable("screen.onepiece_newworld.merit", data.getMerit(), next), x, cy, COLOR_DIM);
        } else {
            cy = line(g, Component.translatable("screen.onepiece_newworld.bounty",
                    ProgressionService.formatBerry(data.getBounty())), x, cy, COLOR_TEXT);
        }
        cy += 4;

        cy = line(g, Component.translatable("screen.onepiece_newworld.haki"), x, cy, COLOR_TITLE);
        for (HakiType type : HakiType.values()) {
            int xp = data.getHakiXp(type);
            int level = HakiType.levelFor(xp);
            int next = HakiType.nextThreshold(xp);
            Component text;
            if (type == HakiType.CONQUEROR && level == 0) {
                text = type.displayName().append(": ").append(Component.translatable("screen.onepiece_newworld.haki_unknown"));
            } else if (level == 0) {
                text = type.displayName().append(": ").append(Component.translatable("screen.onepiece_newworld.haki_dormant", xp, next));
            } else if (next < 0) {
                text = type.displayName().append(": ").append(Component.translatable("screen.onepiece_newworld.haki_max", level));
            } else {
                text = type.displayName().append(": ").append(Component.translatable("screen.onepiece_newworld.haki_level", level, xp, next));
            }
            cy = wrapped(g, text, x, cy, w, level > 0 ? COLOR_TEXT : COLOR_DIM);
        }
    }

    private void renderFruitColumn(GuiGraphics g, PlayerData data, int x, int y, int w) {
        DevilFruit fruit = ModFruits.get(data.getFruitId());
        int cy = y;
        if (fruit == null) {
            cy = line(g, Component.translatable("screen.onepiece_newworld.no_fruit"), x, cy, COLOR_TITLE);
            wrapped(g, Component.translatable("screen.onepiece_newworld.no_fruit_hint"), x, cy, w, COLOR_DIM);
            return;
        }
        cy = line(g, fruit.displayName(), x, cy, 0xFF000000 | fruit.color());
        cy = line(g, fruit.type().displayName(), x, cy, COLOR_DIM);
        cy = line(g, Component.translatable("screen.onepiece_newworld.mastery",
                data.getMastery(fruit.id()), Progression.MAX_MASTERY), x, cy, COLOR_TEXT);
        cy += 4;
        List<Ability> abilities = fruit.abilities();
        for (int i = 0; i < abilities.size(); i++) {
            Ability ability = abilities.get(i);
            String key = i < ModKeys.ABILITY_SLOTS.length ? "[" + ModKeys.label(ModKeys.ABILITY_SLOTS[i]) + "] " : "";
            cy = line(g, Component.literal(key).append(ability.displayName()), x, cy, COLOR_TEXT);
            cy = line(g, Component.translatable("screen.onepiece_newworld.ability_stats",
                    (int) ability.energyCost(), String.format(Locale.ROOT, "%.1f", ability.baseCooldown() / 20f)), x + 6, cy, COLOR_DIM);
            cy = wrapped(g, ability.description(), x + 6, cy, w - 6, COLOR_DIM);
            cy += 2;
        }
    }

    private int line(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
        return y + 11;
    }

    private int wrapped(GuiGraphics g, Component text, int x, int y, int width, int color) {
        int cy = y;
        for (FormattedCharSequence part : font.split(text, width)) {
            g.drawString(font, part, x, cy, color, false);
            cy += 10;
        }
        return cy;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.CHARACTER.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
