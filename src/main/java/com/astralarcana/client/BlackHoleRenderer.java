package com.astralarcana.client;

import com.astralarcana.entity.BlackHoleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
    public BlackHoleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity e) {
        return ClientFx.SOLID;
    }

    @Override
    public boolean shouldRender(BlackHoleEntity e, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(BlackHoleEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        float grow = Mth.clamp(age / 20f, 0f, 1f);
        float fade = Mth.clamp((e.getMaxLife() - age) / 20f, 0f, 1f);
        float k = Mth.sin(grow * fade * Mth.HALF_PI);
        if (k <= 0.01f) return;
        float R = 1.7f * k;

        ps.pushPose();
        ps.translate(0, e.getBbHeight() / 2.0, 0);

        VertexConsumer core = buf.getBuffer(RenderType.entityCutoutNoCull(ClientFx.SOLID));
        FxRender.sphere(ps, core, R, 14, 20, 0f, 0f, 0f, 1f);

        VertexConsumer glow = buf.getBuffer(RenderType.eyes(ClientFx.SOLID));
        FxRender.sphere(ps, glow, R * 1.1f, 12, 18, 0.9f, 0.5f, 1f, 0.35f);
        FxRender.sphere(ps, glow, R * 1.5f, 10, 16, 0.4f, 0.2f, 1f, 0.14f);
        FxRender.sphere(ps, glow, R * 2.3f, 8, 14, 0.3f, 0.1f, 0.8f, 0.07f);

        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(20f));
        FxRender.annulus(ps, glow, R * 1.25f, R * 2.2f, 48, 1f, 0.95f, 0.8f, 0.8f, 0.0f);
        FxRender.annulus(ps, glow, R * 2.0f, R * 5.5f, 48, 1f, 0.5f, 0.15f, 0.35f, 0.0f);
        ps.popPose();

        ps.popPose();
    }
}
