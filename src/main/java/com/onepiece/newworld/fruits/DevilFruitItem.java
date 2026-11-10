package com.onepiece.newworld.fruits;

import com.onepiece.newworld.core.ModAttachments;
import com.onepiece.newworld.networking.PlayerSync;
import com.onepiece.newworld.progression.PlayerData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Предмет дьявольского фрукта. Съевший получает силу, но навсегда теряет умение плавать.
 * Попытка съесть второй фрукт смертельна.
 */
public class DevilFruitItem extends Item {
    private final DeferredHolder<DevilFruit, ? extends DevilFruit> fruit;

    public DevilFruitItem(DeferredHolder<DevilFruit, ? extends DevilFruit> fruit, Properties properties) {
        super(properties);
        this.fruit = fruit;
    }

    public Supplier<? extends DevilFruit> fruit() {
        return fruit;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            PlayerData data = ModAttachments.get(player);
            if (data.hasFruit()) {
                player.displayClientMessage(Component.translatable("message.onepiece_newworld.second_fruit")
                        .withStyle(ChatFormatting.DARK_RED), false);
                player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
            } else {
                data.setFruit(fruit.getId());
                PlayerSync.sync(player);
                player.displayClientMessage(Component.translatable("message.onepiece_newworld.ate_fruit",
                        fruit.get().displayName()).withStyle(ChatFormatting.GOLD), false);
                player.serverLevel().sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1, player.getZ(),
                        30, 0.4, 0.6, 0.4, 0.05);
                level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.6f);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!fruit.isBound()) {
            return;
        }
        DevilFruit value = fruit.get();
        tooltip.add(value.type().displayName().withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(value.description().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.onepiece_newworld.fruit_warning").withStyle(ChatFormatting.DARK_RED));
    }
}
