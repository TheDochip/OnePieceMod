package com.onepiece.newworld.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.onepiece.newworld.OnePieceNewWorld;
import com.onepiece.newworld.entities.SlashProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Полумесяц летящего удара: плоский светящийся квадрат с текстурой дуги,
 * повёрнутый по направлению полёта (у вертикального удара — на 90° вокруг оси полёта).
 * Обычный удар — бело-голубой, с Хаки Вооружения — чёрный с фиолетовым отливом.
 */
public class SlashRenderer extends EntityRenderer<SlashProjectile> {
    private static final ResourceLocation TEXTURE = OnePieceNewWorld.id("textures/entity/slash.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public SlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SlashProjectile slash, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float age = slash.tickCount + partialTick;
        int life = Math.max(1, slash.life());
        float alpha = Mth.clamp((life - age) / 6f, 0f, 1f) * 0.9f;
        if (alpha <= 0.01f) {
            return;
        }
        float half = slash.width() / 2f;
        float depth = half * 0.5f;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, slash.yRotO, slash.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, slash.xRotO, slash.getXRot())));
        if (slash.isVertical()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(90f));
        }
        float r;
        float g;
        float b;
        if (slash.isBlack()) {
            r = 0.12f;
            g = 0.04f;
            b = 0.2f;
        } else {
            r = 0.85f;
            g = 0.95f;
            b = 1f;
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        // Лицевая сторона.
        vertex(consumer, pose, -half, -depth, 0f, 1f, r, g, b, alpha);
        vertex(consumer, pose, half, -depth, 1f, 1f, r, g, b, alpha);
        vertex(consumer, pose, half, depth, 1f, 0f, r, g, b, alpha);
        vertex(consumer, pose, -half, depth, 0f, 0f, r, g, b, alpha);
        // Обратная сторона, чтобы удар был виден снизу и сбоку.
        vertex(consumer, pose, -half, depth, 0f, 0f, r, g, b, alpha);
        vertex(consumer, pose, half, depth, 1f, 0f, r, g, b, alpha);
        vertex(consumer, pose, half, -depth, 1f, 1f, r, g, b, alpha);
        vertex(consumer, pose, -half, -depth, 0f, 1f, r, g, b, alpha);
        poseStack.popPose();
        super.render(slash, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float z, float u, float v,
                               float r, float g, float b, float a) {
        consumer.addVertex(pose, x, 0f, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0f, 1f, 0f);
    }

    @Override
    public ResourceLocation getTextureLocation(SlashProjectile slash) {
        return TEXTURE;
    }
}
