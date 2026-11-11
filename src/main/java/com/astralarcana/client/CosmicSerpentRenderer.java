package com.astralarcana.client;

import com.astralarcana.entity.CosmicSerpentEntity;
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

/** Renderiza a serpente cosmica proceduralmente: corpo ondulante, espinhos, aneis, estrelas, cabeca com mandibula animada. */
public class CosmicSerpentRenderer extends EntityRenderer<CosmicSerpentEntity> {
    private static final int N = CosmicSerpentEntity.SEG_COUNT;

    public CosmicSerpentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(CosmicSerpentEntity e) {
        return ClientFx.SOLID;
    }

    @Override
    public boolean shouldRender(CosmicSerpentEntity e, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(e.bodyBounds().inflate(6.0));
    }

    private static float hash(int n) {
        double v = Math.sin(n * 12.9898) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    @Override
    public void render(CosmicSerpentEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float time = e.tickCount + pt;
        Vec3 base = e.getPosition(pt);

        // ---- posicoes (relativas a entidade), direcoes e ondulacao ----
        Vec3[] P = new Vec3[N];
        for (int i = 0; i < N; i++) P[i] = e.segsO[i].lerp(e.segs[i], pt).subtract(base);

        Vec3[] dir = new Vec3[N], side = new Vec3[N], up = new Vec3[N];
        for (int i = 0; i < N; i++) {
            Vec3 d = i == 0 ? P[0].subtract(P[1]) : P[i - 1].subtract(P[Math.min(i + 1, N - 1)]);
            if (d.lengthSqr() < 1.0E-8) d = new Vec3(0, 0, 1);
            dir[i] = d.normalize();
            Vec3 sd = dir[i].cross(new Vec3(0, 1, 0));
            if (sd.lengthSqr() < 1.0E-6) sd = new Vec3(1, 0, 0);
            side[i] = sd.normalize();
            up[i] = side[i].cross(dir[i]).normalize();
        }
        for (int i = 1; i < N; i++) {
            float amp = 0.9f * Math.min(1f, i / 8f);
            P[i] = P[i].add(side[i].scale(Math.sin(time * 0.28 - i * 0.5) * amp))
                    .add(up[i].scale(Math.cos(time * 0.21 - i * 0.4) * amp * 0.5));
        }

        Vec3 f = dir[0];
        Vec3 hc = P[0];
        Vec3 u0 = up[0], s0 = side[0];
        int phase = e.getPhase();
        float phaseT = e.tickCount - e.phaseStart + pt;
        float open;
        if (phase == CosmicSerpentEntity.BREATH) open = Mth.clamp(phaseT / 30f, 0f, 1f);
        else if (phase == CosmicSerpentEntity.DIVE) open = 0.5f;
        else open = 0.12f + 0.06f * (float) Math.sin(time * 0.15);
        float chargeRatio = phase == CosmicSerpentEntity.BREATH ? Mth.clamp(phaseT / 35f, 0f, 1f) : 0f;
        float deathF = e.deathTime > 0 ? (e.deathTime + pt) / 20f : 0f;

        ps.pushPose();

        // ================= CAMADA SOLIDA =================
        VertexConsumer solid = buf.getBuffer(RenderType.entityCutoutNoCull(ClientFx.SOLID));

        for (int i = 0; i < N - 1; i++) {
            float t = i / (float) N;
            float r = 0.04f + 0.06f * t, g = 0.05f - 0.02f * t, b = 0.22f + 0.06f * t;
            FxRender.cone(ps, solid, P[i], P[i + 1], CosmicSerpentEntity.radius(i), CosmicSerpentEntity.radius(i + 1), 12, r, g, b, 1f, true);
        }

        // espinhos dorsais e laterais
        for (int i = 2; i < N - 1; i++) {
            float t = i / (float) N;
            float rad = CosmicSerpentEntity.radius(i);
            float len = 1.3f * (float) Math.pow(1f - t, 0.7) + 0.25f;
            float sr = 0.22f * (1f - t) + 0.05f;
            Vec3 baseP = P[i].add(up[i].scale(rad * 0.8));
            Vec3 tip = P[i].add(up[i].scale(rad + len)).subtract(dir[i].scale(len * 0.7));
            FxRender.cone(ps, solid, baseP, tip, sr, 0f, 5, 0.55f, 0.85f, 1f, 1f, false);
            if (i % 2 == 0) {
                for (int sgn = -1; sgn <= 1; sgn += 2) {
                    Vec3 sb = P[i].add(side[i].scale(sgn * rad * 0.8)).add(up[i].scale(rad * 0.3));
                    Vec3 st = sb.add(up[i].scale(len * 0.5)).add(side[i].scale(sgn * len * 0.8)).subtract(dir[i].scale(len * 0.5));
                    FxRender.cone(ps, solid, sb, st, sr * 0.8f, 0f, 4, 0.45f, 0.7f, 1f, 1f, false);
                }
            }
        }

        // cabeca: cranio, mandibulas, chifres, franjas
        FxRender.cone(ps, solid, hc.subtract(f.scale(0.8)), hc.add(f.scale(2.0)), 1.0f, 0.55f, 12, 0.07f, 0.08f, 0.32f, 1f, true);
        FxRender.cone(ps, solid, hc.add(f.scale(1.0)), hc.add(f.scale(2.8)).add(u0.scale(0.15)), 0.5f, 0.12f, 8, 0.1f, 0.12f, 0.4f, 1f, true);
        FxRender.cone(ps, solid, hc.subtract(f.scale(0.2)).subtract(u0.scale(0.5)),
                hc.add(f.scale(2.4)).subtract(u0.scale(0.5 + open * 1.3)), 0.4f, 0.08f, 8, 0.06f, 0.07f, 0.28f, 1f, true);
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Vec3 hb = hc.add(u0.scale(0.7)).add(s0.scale(0.55 * sgn)).subtract(f.scale(0.3));
            Vec3 ht = hb.add(u0.scale(1.1)).add(s0.scale(0.9 * sgn)).subtract(f.scale(2.2));
            FxRender.cone(ps, solid, hb, ht, 0.28f, 0f, 6, 0.6f, 0.9f, 1f, 1f, false);
            Vec3 fb = hc.subtract(f.scale(0.4)).add(s0.scale(0.8 * sgn));
            Vec3 ft = fb.add(s0.scale(1.2 * sgn)).subtract(f.scale(1.1)).add(u0.scale(0.4));
            FxRender.cone(ps, solid, fb, ft, 0.22f, 0f, 5, 0.3f, 0.45f, 0.9f, 1f, false);
        }

        // ================= CAMADA BRILHANTE (aditiva) =================
        VertexConsumer glow = buf.getBuffer(RenderType.eyes(ClientFx.SOLID));

        for (int i = 0; i < N - 1; i++) {
            float t = i / (float) N;
            float r = 0.25f + 0.4f * t, g = 0.85f - 0.55f * t, b = 1f;
            FxRender.cone(ps, glow, P[i], P[i + 1], CosmicSerpentEntity.radius(i) * 1.18f, CosmicSerpentEntity.radius(i + 1) * 1.18f, 12, r, g, b, 0.16f, false);
        }

        // aneis luminosos ao redor do corpo
        float pulse = 0.7f + 0.3f * (float) Math.sin(time * 0.2);
        for (int i = 3; i < N - 2; i++) {
            if (i % 4 == 2) {
                float rad = CosmicSerpentEntity.radius(i);
                Vec3 a = P[i].subtract(dir[i].scale(0.18)), b = P[i].add(dir[i].scale(0.18));
                FxRender.cone(ps, glow, a, b, rad * 1.35f, rad * 1.35f, 14, 0.55f, 0.4f, 1f, 0.5f * pulse, false);
                FxRender.cone(ps, glow, a, b, rad * 1.6f, rad * 1.6f, 14, 0.4f, 0.8f, 1f, 0.2f * pulse, false);
            }
            if (i % 6 == 3) {
                float rad = CosmicSerpentEntity.radius(i);
                Vec3 a = P[i].subtract(dir[i].scale(0.06)), b = P[i].add(dir[i].scale(0.06));
                FxRender.cone(ps, glow, a, b, rad * 3.0f, rad * 3.0f, 18, 0.5f, 0.35f, 1f, 0.28f, false);
            }
        }

        // estrelas na pele (cintilando)
        for (int i = 1; i < N - 1; i++) {
            float rad = CosmicSerpentEntity.radius(i);
            for (int k = 0; k < 2; k++) {
                int h = i * 7 + k * 13;
                double ang = hash(h) * Math.PI * 2;
                Vec3 sp = P[i].add(side[i].scale(Math.cos(ang) * rad * 1.03)).add(up[i].scale(Math.sin(ang) * rad * 1.03));
                float tw = 0.4f + 0.6f * (0.5f + 0.5f * (float) Math.sin(time * 0.2 + i * 1.7 + k * 3));
                ps.pushPose();
                ps.translate(sp.x, sp.y, sp.z);
                FxRender.sphere(ps, glow, 0.09f + hash(h + 3) * 0.08f, 3, 5, 0.8f, 0.95f, 1f, tw);
                ps.popPose();
            }
        }

        // olhos
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            Vec3 ep = hc.add(f.scale(1.0)).add(u0.scale(0.35)).add(s0.scale(0.55 * sgn));
            ps.pushPose();
            ps.translate(ep.x, ep.y, ep.z);
            FxRender.sphere(ps, glow, 0.17f, 5, 8, 0.4f, 1f, 1f, 0.95f);
            FxRender.sphere(ps, glow, 0.3f, 5, 8, 0.3f, 0.8f, 1f, 0.25f);
            ps.popPose();
        }

        // brilho na boca + esfera de carga do sopro
        if (open > 0.3f) {
            Vec3 mp = hc.add(f.scale(1.6)).subtract(u0.scale(0.3));
            ps.pushPose();
            ps.translate(mp.x, mp.y, mp.z);
            FxRender.sphere(ps, glow, 0.25f * open, 5, 8, 0.5f, 0.9f, 1f, 0.6f);
            ps.popPose();
        }
        if (chargeRatio > 0.02f) {
            Vec3 cp = hc.add(f.scale(3.2));
            float cr = 0.2f + 1.3f * chargeRatio;
            ps.pushPose();
            ps.translate(cp.x, cp.y, cp.z);
            FxRender.sphere(ps, glow, cr * 0.6f, 8, 12, 1f, 1f, 1f, 0.9f);
            FxRender.sphere(ps, glow, cr, 8, 14, 0.4f, 0.85f, 1f, 0.4f);
            FxRender.sphere(ps, glow, cr * 1.7f, 8, 14, 0.5f, 0.3f, 1f, 0.15f);
            ps.popPose();
        }

        // flash de morte
        if (deathF > 0f) {
            ps.pushPose();
            ps.translate(hc.x, hc.y, hc.z);
            FxRender.sphere(ps, glow, 1f + deathF * 8f, 8, 14, 1f, 1f, 1f, Math.max(0f, 1f - deathF) * 0.8f);
            ps.popPose();
        }

        ps.popPose();
    }
}
