package com.astralarcana.engine.vfx;

import com.astralarcana.client.FxRender;
import com.astralarcana.engine.perf.PerformanceEngine;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.function.Supplier;

/** Fita/rastro 3D que segue uma fonte (face sempre para a camera), com afinamento, fade e suavizacao Catmull-Rom. */
public final class RibbonEffect extends VfxEffect {
    private final Supplier<Vec3> source;
    private final ArrayDeque<Vec3> pts = new ArrayDeque<>();
    private final int maxPts;
    private final float width;
    private final float[] c0, c1;
    private boolean ended;

    public RibbonEffect(Supplier<Vec3> source, int maxPoints, float width, float[] colHead, float[] colTail) {
        super(Vec3.ZERO, Integer.MAX_VALUE);
        this.source = source;
        this.maxPts = Math.max(4, maxPoints);
        this.width = width;
        this.c0 = colHead;
        this.c1 = colTail;
    }

    @Override
    public void tick(net.minecraft.client.multiplayer.ClientLevel level) {
        if (!ended) {
            Vec3 p = source.get();
            if (p == null) {
                ended = true;
            } else if (pts.isEmpty() || pts.peekFirst().distanceToSqr(p) > 0.0025) {
                pts.addFirst(p);
                while (pts.size() > maxPts) pts.removeLast();
            }
        }
        if (ended && !pts.isEmpty()) {
            pts.removeLast();
            if (!pts.isEmpty()) pts.removeLast();
        }
    }

    @Override
    public boolean isDone() {
        return ended && pts.size() < 2;
    }

    @Override
    public AABB bounds() {
        double minX = 1e18, minY = 1e18, minZ = 1e18, maxX = -1e18, maxY = -1e18, maxZ = -1e18;
        for (Vec3 p : pts) {
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
            minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
        }
        if (pts.isEmpty()) return new AABB(0, 0, 0, 0, 0, 0);
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(width + 1.0);
    }

    private static Vec3 catmull(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t2 = t * t, t3 = t2 * t;
        return new Vec3(
                0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3),
                0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3),
                0.5 * ((2 * p1.z) + (-p0.z + p2.z) * t + (2 * p0.z - 5 * p1.z + 4 * p2.z - p3.z) * t2 + (-p0.z + 3 * p1.z - 3 * p2.z + p3.z) * t3));
    }

    @Override
    public void render(Ctx c) {
        int n = pts.size();
        if (n < 2) return;
        Vec3[] raw = pts.toArray(new Vec3[0]);
        int sub = PerformanceEngine.quality().ordinal() >= 3 ? 3 : 1;
        int total = (n - 1) * sub + 1;
        Vec3[] path = new Vec3[total];
        for (int i = 0; i < n - 1; i++) {
            Vec3 p0 = raw[Math.max(i - 1, 0)], p1 = raw[i], p2 = raw[i + 1], p3 = raw[Math.min(i + 2, n - 1)];
            for (int s = 0; s < sub; s++) path[i * sub + s] = sub == 1 ? p1 : catmull(p0, p1, p2, p3, s / (double) sub);
        }
        path[total - 1] = raw[n - 1];

        Vec3[] left = new Vec3[total], right = new Vec3[total];
        float[] alpha = new float[total];
        for (int i = 0; i < total; i++) {
            float k = i / (float) (total - 1);
            Vec3 dir = path[Math.min(i + 1, total - 1)].subtract(path[Math.max(i - 1, 0)]);
            if (dir.lengthSqr() < 1.0E-8) dir = new Vec3(0, 1, 0);
            Vec3 toCam = c.cam.subtract(path[i]);
            Vec3 side = dir.cross(toCam);
            side = side.lengthSqr() < 1.0E-8 ? new Vec3(1, 0, 0) : side.normalize();
            double w = width * 0.5 * Math.pow(1.0 - k, 0.7);
            left[i] = path[i].add(side.scale(w));
            right[i] = path[i].subtract(side.scale(w));
            alpha[i] = (float) Math.pow(1.0 - k, 1.2);
        }
        for (int i = 0; i < total - 1; i++) {
            float k = i / (float) (total - 1);
            float r = Mth.lerp(k, c0[0], c1[0]), g = Mth.lerp(k, c0[1], c1[1]), b = Mth.lerp(k, c0[2], c1[2]);
            FxRender.quad4(c.ps, c.vc, left[i], right[i], right[i + 1], left[i + 1], r, g, b, alpha[i] * 0.9f, alpha[i] * 0.9f, alpha[i + 1] * 0.9f, alpha[i + 1] * 0.9f);
        }
    }
}
