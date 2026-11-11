package com.astralarcana.engine.vfx;

import com.astralarcana.client.FxRender;
import com.astralarcana.engine.perf.PerformanceEngine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/** Campo de energia esferico: casca translucida + grade de latitudes/longitudes animada (pulso e rotacao). */
public final class EnergyFieldEffect extends VfxEffect {
    private final Supplier<Vec3> follow;
    private final float radius;
    private final float[] col;
    private Vec3 current;

    public EnergyFieldEffect(Vec3 pos, Supplier<Vec3> follow, float radius, int life, float[] col) {
        super(pos, life);
        this.follow = follow;
        this.radius = radius;
        this.col = col;
        this.current = pos;
    }

    @Override
    public void tick(net.minecraft.client.multiplayer.ClientLevel level) {
        if (follow != null) {
            Vec3 p = follow.get();
            if (p != null) current = p;
        }
    }

    @Override
    public AABB bounds() {
        return new AABB(current, current).inflate(radius + 1);
    }

    @Override
    public void render(Ctx c) {
        float t = progress(c.pt);
        float ramp = Math.min(1f, Math.min(t * 8f, (1f - t) * 8f));
        if (ramp <= 0.02f) return;
        c.ps.pushPose();
        c.ps.translate(current.x, current.y, current.z);
        drawGrid(c.ps, c.vc, radius * ramp, age + c.pt, col[0], col[1], col[2], 0.5f * ramp, c.cam.distanceTo(current));
        c.ps.popPose();
    }

    /** Desenha a grade de energia centrada na origem da pose atual. */
    public static void drawGrid(PoseStack ps, VertexConsumer vc, float R, float time, float r, float g, float b, float alpha, double dist) {
        int detail = PerformanceEngine.lod(16, dist, R);
        int lat = Math.max(3, detail / 3), lon = Math.max(4, detail / 2), seg = Math.max(8, detail * 2);
        float pulse = 0.75f + 0.25f * (float) Math.sin(time * 0.25);
        FxRender.sphere(ps, vc, R, Math.max(6, detail), Math.max(8, detail + 4), r, g, b, 0.06f * alpha * pulse);
        float tube = 0.025f * Mth.clamp(R / 5f, 0.6f, 2f);
        for (int j = 1; j < lat; j++) {
            double phi = Math.PI * j / lat;
            double y = Math.cos(phi) * R, rad = Math.sin(phi) * R;
            for (int i = 0; i < seg; i++) {
                double a0 = Math.PI * 2 * i / seg + time * 0.02, a1 = Math.PI * 2 * (i + 1) / seg + time * 0.02;
                FxRender.cone(ps, vc, new Vec3(Math.cos(a0) * rad, y, Math.sin(a0) * rad), new Vec3(Math.cos(a1) * rad, y, Math.sin(a1) * rad),
                        tube, tube, 3, r, g, b, alpha * pulse, false);
            }
        }
        for (int k = 0; k < lon; k++) {
            double lam = Math.PI * 2 * k / lon + time * 0.03;
            for (int i = 0; i < seg / 2; i++) {
                double p0 = Math.PI * i / (seg / 2), p1 = Math.PI * (i + 1) / (seg / 2);
                Vec3 a = new Vec3(Math.sin(p0) * Math.cos(lam) * R, Math.cos(p0) * R, Math.sin(p0) * Math.sin(lam) * R);
                Vec3 bb = new Vec3(Math.sin(p1) * Math.cos(lam) * R, Math.cos(p1) * R, Math.sin(p1) * Math.sin(lam) * R);
                FxRender.cone(ps, vc, a, bb, tube, tube, 3, r, g, b, alpha * pulse * 0.8f, false);
            }
        }
    }
}
