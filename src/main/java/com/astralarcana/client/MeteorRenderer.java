package com.astralarcana.client;

import com.astralarcana.entity.MeteorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class MeteorRenderer extends EntityRenderer<MeteorEntity> {
    public MeteorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity e) {
        return ClientFx.SOLID;
    }

    @Override
    public boolean shouldRender(MeteorEntity e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.getBoundingBox().inflate(e.getSize() * 3.0));
    }

    @Override
    public void render(MeteorEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float s = e.getSize();
        float t = e.tickCount + pt;
        ps.pushPose();
        ps.translate(0, e.getBbHeight() / 2.0, 0);

        VertexConsumer glow = buf.getBuffer(RenderType.eyes(ClientFx.SOLID));
        double dist = Math.sqrt(this.entityRenderDispatcher.distanceToSqr(e));
        int st = com.astralarcana.engine.perf.PerformanceEngine.lod(10, dist, s * 1.6);
        FxRender.sphere(ps, glow, s * 1.6f, st, st + 4, 1f, 0.45f, 0.1f, 0.25f);
        FxRender.sphere(ps, glow, s * 2.4f, Math.max(4, st - 2), st + 2, 1f, 0.3f, 0.05f, 0.12f);

        var blocks = Minecraft.getInstance().getBlockRenderer();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(t * 9f));
        ps.mulPose(Axis.XP.rotationDegrees(t * 6f));
        ps.scale(s * 1.3f, s * 1.3f, s * 1.3f);
        ps.translate(-0.5, -0.5, -0.5);
        blocks.renderSingleBlock(Blocks.MAGMA_BLOCK.defaultBlockState(), ps, buf, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(-t * 11f));
        ps.mulPose(Axis.YP.rotationDegrees(t * 4f));
        ps.translate(s * 0.7, s * 0.4, 0);
        ps.scale(s * 0.7f, s * 0.7f, s * 0.7f);
        ps.translate(-0.5, -0.5, -0.5);
        blocks.renderSingleBlock(Blocks.BLACKSTONE.defaultBlockState(), ps, buf, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        ps.popPose();

        ps.popPose();
    }
}
