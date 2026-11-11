package com.astralarcana.client;

import com.astralarcana.entity.SpellZoneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ZoneRenderer extends EntityRenderer<SpellZoneEntity> {
    public ZoneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(SpellZoneEntity e) {
        return ClientFx.SOLID;
    }

    @Override
    public boolean shouldRender(SpellZoneEntity e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(new net.minecraft.world.phys.AABB(e.position(), e.position()).inflate(22.0).expandTowards(0, 20, 0));
    }

    @Override
    public void render(SpellZoneEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        float ramp = Mth.clamp(age / 12f, 0f, 1f) * Mth.clamp((e.getMaxLife() - age) / 12f, 0f, 1f);
        if (ramp <= 0.02f) return;
        VertexConsumer glow = buf.getBuffer(RenderType.eyes(ClientFx.SOLID));
        switch (e.getKind()) {
            case SpellZoneEntity.TORNADO -> {
                FxRender.cone(ps, glow, Vec3.ZERO, new Vec3(0, 18, 0), 1.5f * ramp, 7.5f * ramp, 18, 0.8f, 0.9f, 1f, 0.10f * ramp, false);
                FxRender.cone(ps, glow, Vec3.ZERO, new Vec3(0, 18, 0), 1.0f * ramp, 5.0f * ramp, 18, 0.5f, 0.8f, 1f, 0.08f * ramp, false);
            }
            case SpellZoneEntity.ORBITAL -> {
                float prog = Mth.clamp(age / 50f, 0f, 1f);
                for (int i = 0; i < 7; i++) {
                    int fireAt = i == 0 ? 64 : 50 + (i - 1) * 2;
                    if (e.tickCount > fireAt) continue;
                    Vec3 p = e.pillarPos(i).subtract(e.position());
                    float w = 0.08f + 0.15f * prog;
                    FxRender.cone(ps, glow, p, p.add(0, 70, 0), w, w, 6, 1f, 0.85f, 0.4f, 0.25f + 0.5f * prog, false);
                }
            }
            case SpellZoneEntity.SHIELD -> {
                ps.pushPose();
                ps.translate(0, 1, 0);
                double sd = Math.sqrt(this.entityRenderDispatcher.distanceToSqr(e));
                com.astralarcana.engine.vfx.EnergyFieldEffect.drawGrid(ps, glow, 5.5f * ramp, age, 0.4f, 0.8f, 1f, 0.55f * ramp, sd);
                ps.popPose();
            }
            case SpellZoneEntity.BLADES -> {
                for (int k = 0; k < 3; k++) {
                    for (int j = 0; j < 6; j++) {
                        double an = age * 0.35 + k * Math.PI * 2 / 3 - j * 0.14;
                        float fade = 1f - j / 6f;
                        Vec3 end = new Vec3(Math.cos(an) * 8, 1, Math.sin(an) * 8);
                        Vec3 start = new Vec3(Math.cos(an) * 1.0, 1, Math.sin(an) * 1.0);
                        FxRender.tube(ps, glow, start, end, 0.12f * fade, 6, 1f, 1f, 1f, 0.9f * fade * ramp);
                        FxRender.tube(ps, glow, start, end, 0.4f * fade, 6, 1f, 0.3f, 0.55f, 0.3f * fade * ramp);
                    }
                }
            }
            case SpellZoneEntity.SANCTUARY -> {
                ps.pushPose();
                ps.translate(0, 0.1, 0);
                FxRender.annulus(ps, glow, 7.4f, 8f, 48, 1f, 0.95f, 0.45f, 0.5f * ramp, 0.5f * ramp);
                FxRender.annulus(ps, glow, 0f, 7.4f, 48, 1f, 0.95f, 0.5f, 0.12f * ramp, 0.05f * ramp);
                FxRender.wall(ps, glow, 8f, 6f, 40, 0.9f, 0.95f, 0.5f, 0.18f * ramp, 0f);
                ps.popPose();
            }
            case SpellZoneEntity.BLIZZARD -> FxRender.wall(ps, glow, 14f, 10f, 48, 0.8f, 0.9f, 1f, 0.08f * ramp, 0f);
            default -> { }
        }
    }
}
