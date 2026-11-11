package com.astralarcana.engine.vfx;

import com.astralarcana.client.FxRender;
import com.astralarcana.engine.anim.Easing;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Rachaduras incandescentes que se abrem a partir do impacto, acompanhando o relevo. Apenas visual: nao altera blocos. */
public final class GroundCrackEffect extends VfxEffect {
    private final List<Vec3[]> cracks = new ArrayList<>();
    private final float radius;

    public GroundCrackEffect(ClientLevel l, Vec3 center, float radius, int life) {
        super(center, life);
        this.radius = radius;
        RandomSource r = RandomSource.create((long) (center.x * 31 + center.z * 17 + life));
        int main = 9;
        for (int i = 0; i < main; i++) {
            double ang = Math.PI * 2 * i / main + r.nextDouble() * 0.5;
            buildCrack(l, r, center, ang, radius * (0.7 + r.nextDouble() * 0.3), 0.0);
        }
    }

    private void buildCrack(ClientLevel l, RandomSource r, Vec3 start, double ang, double len, double depth) {
        int segs = 8;
        Vec3[] pts = new Vec3[segs + 1];
        double x = start.x, z = start.z;
        pts[0] = new Vec3(x, groundY(l, x, z, start.y), z);
        for (int i = 1; i <= segs; i++) {
            ang += (r.nextDouble() - 0.5) * 0.7;
            double step = len / segs;
            x += Math.cos(ang) * step;
            z += Math.sin(ang) * step;
            pts[i] = new Vec3(x, groundY(l, x, z, start.y), z);
            if (depth < 1.0 && i == 3 + r.nextInt(3) && r.nextFloat() < 0.8f) {
                buildCrack(l, r, pts[i], ang + (r.nextBoolean() ? 0.8 : -0.8), len * 0.4, depth + 1.0);
            }
        }
        cracks.add(pts);
    }

    private static double groundY(ClientLevel l, double x, double z, double fallback) {
        try {
            int h = l.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            if (h > l.getMinBuildHeight() && Math.abs(h - fallback) < 12) return h + 0.04;
        } catch (Exception ignored) {
            // chunk nao carregado: usa a altura do impacto
        }
        return fallback + 0.04;
    }

    @Override
    public AABB bounds() {
        return new AABB(pos, pos).inflate(radius + 2).expandTowards(0, 2, 0);
    }

    @Override
    public void render(Ctx c) {
        float t = progress(c.pt);
        float reveal = Easing.OUT_EXPO.apply(Math.min(1f, t * 4f));
        float fade = (float) Math.pow(1f - t, 1.5);
        if (fade <= 0.02f) return;
        float heat = Math.max(0f, 1f - t * 2f);
        float cr = 1f, cg = 0.35f + 0.55f * heat, cb = 0.05f + 0.6f * heat * heat;
        for (Vec3[] pts : cracks) {
            int upTo = Math.max(1, (int) Math.ceil((pts.length - 1) * reveal));
            for (int i = 0; i < upTo && i < pts.length - 1; i++) {
                float w0 = 0.14f * (1f - i / (float) pts.length), w1 = 0.14f * (1f - (i + 1) / (float) pts.length);
                FxRender.cone(c.ps, c.vc, pts[i], pts[i + 1], w0, w1, 4, cr, cg, cb, 0.9f * fade, false);
                FxRender.cone(c.ps, c.vc, pts[i], pts[i + 1], w0 * 3f, w1 * 3f, 4, 1f, 0.3f, 0.05f, 0.25f * fade, false);
            }
        }
    }
}
