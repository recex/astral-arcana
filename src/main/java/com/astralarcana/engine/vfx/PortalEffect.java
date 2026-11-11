package com.astralarcana.engine.vfx;

import com.astralarcana.client.FxRender;
import com.astralarcana.engine.anim.Easing;
import com.astralarcana.engine.particle.ParticleSystem;
import com.astralarcana.engine.perf.PerformanceEngine;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Portal dimensional: anel brilhante + disco com bracos em espiral. Abre com overshoot e fecha suavemente. */
public final class PortalEffect extends VfxEffect {
    private final Vec3 u, w, normal;
    private final float radius;
    private final float[] colA, colB;

    public PortalEffect(Vec3 pos, Vec3 normal, float radius, int life, float[] colA, float[] colB) {
        super(pos, life);
        Vec3 n = normal.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : normal.normalize();
        Vec3 helper = Math.abs(n.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        this.normal = n;
        this.u = n.cross(helper).normalize();
        this.w = n.cross(u).normalize();
        this.radius = radius;
        this.colA = colA;
        this.colB = colB;
    }

    private float open(float t) {
        if (t < 0.25f) return Easing.OUT_BACK.apply(t / 0.25f);
        if (t > 0.75f) return 1f - Easing.IN_CUBIC.apply((t - 0.75f) / 0.25f);
        return 1f;
    }

    @Override
    public AABB bounds() {
        return new AABB(pos, pos).inflate(radius * 1.5);
    }

    @Override
    public void tick(ClientLevel l) {
        float t = (float) age / life;
        float R = radius * open(t);
        if (R < 0.2f) return;
        double a = l.random.nextDouble() * Math.PI * 2;
        Vec3 p = pos.add(u.scale(Math.cos(a) * R)).add(w.scale(Math.sin(a) * R));
        Vec3 tang = u.scale(-Math.sin(a)).add(w.scale(Math.cos(a))).scale(0.12);
        ParticleSystem.glow(l, p.x, p.y, p.z, tang.x + normal.x * 0.05, tang.y + normal.y * 0.05, tang.z + normal.z * 0.05,
                l.random.nextBoolean() ? colA : colB, 0.35f, 14, 0f, 0.96f);
    }

    private Vec3 at(double ang, double r) {
        return pos.add(u.scale(Math.cos(ang) * r)).add(w.scale(Math.sin(ang) * r));
    }

    @Override
    public void render(Ctx c) {
        float t = progress(c.pt);
        float R = radius * open(t);
        if (R < 0.05f) return;
        float fade = Math.min(1f, Math.min(t * 6f, (1f - t) * 6f));
        int seg = PerformanceEngine.lod(48, c.cam.distanceTo(pos), radius);
        float spin = (age + c.pt) * 0.18f;

        // anel externo
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            FxRender.cone(c.ps, c.vc, at(a0, R), at(a1, R), 0.1f * R * 0.35f, 0.1f * R * 0.35f, 4, 1f, 1f, 1f, 0.9f * fade, false);
            FxRender.cone(c.ps, c.vc, at(a0, R), at(a1, R), 0.3f * R * 0.35f, 0.3f * R * 0.35f, 4, colA[0], colA[1], colA[2], 0.35f * fade, false);
        }
        // disco interno (fan com alfa crescendo para a borda)
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            FxRender.quad4(c.ps, c.vc, pos, at(a0, R * 0.96), at(a1, R * 0.96), pos, colB[0], colB[1], colB[2],
                    0.05f * fade, 0.22f * fade, 0.22f * fade, 0.05f * fade);
        }
        // bracos em espiral
        int arms = 4, steps = Math.max(6, seg / 3);
        for (int k = 0; k < arms; k++) {
            double base = k * Math.PI * 2 / arms + spin;
            for (int s = 0; s < steps; s++) {
                double r0 = R * (0.08 + 0.88 * s / steps), r1 = R * (0.08 + 0.88 * (s + 1) / steps);
                double a0 = base + (s / (double) steps) * 2.4, a1 = base + ((s + 1) / (double) steps) * 2.4;
                double wd0 = R * 0.07 * (1.0 - s / (double) steps * 0.6), wd1 = R * 0.07 * (1.0 - (s + 1) / (double) steps * 0.6);
                Vec3 p0 = at(a0, r0), p1 = at(a1, r1);
                Vec3 s0 = at(a0 + Math.PI / 2, 1).subtract(pos).normalize(), s1 = at(a1 + Math.PI / 2, 1).subtract(pos).normalize();
                float al = (0.25f + 0.7f * (s / (float) steps)) * fade;
                float al1 = (0.25f + 0.7f * ((s + 1) / (float) steps)) * fade;
                FxRender.quad4(c.ps, c.vc, p0.add(s0.scale(wd0)), p0.subtract(s0.scale(wd0)), p1.subtract(s1.scale(wd1)), p1.add(s1.scale(wd1)),
                        colA[0], colA[1], colA[2], al, al, al1, al1);
            }
        }
    }
}
