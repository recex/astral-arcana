package com.astralarcana.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Geometria procedural (esferas, aneis, tubos) para os efeitos brilhantes. */
public final class FxRender {
    private FxRender() {}

    private static void v(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z,
                          float r, float g, float b, float a) {
        vc.vertex(pose.pose(), x, y, z).color(r, g, b, a).uv(0f, 0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0f, 1f, 0f).endVertex();
    }

    /** Quad desenhado dos dois lados (sem depender de culling). */
    private static void quad(VertexConsumer vc, PoseStack.Pose p,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float r, float g, float b, float a0, float a1, float a2, float a3) {
        v(vc, p, ax, ay, az, r, g, b, a0);
        v(vc, p, bx, by, bz, r, g, b, a1);
        v(vc, p, cx, cy, cz, r, g, b, a2);
        v(vc, p, dx, dy, dz, r, g, b, a3);
        v(vc, p, dx, dy, dz, r, g, b, a3);
        v(vc, p, cx, cy, cz, r, g, b, a2);
        v(vc, p, bx, by, bz, r, g, b, a1);
        v(vc, p, ax, ay, az, r, g, b, a0);
    }

    public static void sphere(PoseStack ps, VertexConsumer vc, float radius, int stacks, int slices,
                              float r, float g, float b, float a) {
        PoseStack.Pose pose = ps.last();
        for (int i = 0; i < stacks; i++) {
            double p0 = Math.PI * i / stacks, p1 = Math.PI * (i + 1) / stacks;
            float y0 = (float) Math.cos(p0) * radius, y1 = (float) Math.cos(p1) * radius;
            float r0 = (float) Math.sin(p0) * radius, r1 = (float) Math.sin(p1) * radius;
            for (int j = 0; j < slices; j++) {
                double t0 = 2 * Math.PI * j / slices, t1 = 2 * Math.PI * (j + 1) / slices;
                float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0);
                float c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
                quad(vc, pose, r0 * c0, y0, r0 * s0, r0 * c1, y0, r0 * s1, r1 * c1, y1, r1 * s1, r1 * c0, y1, r1 * s0,
                        r, g, b, a, a, a, a);
            }
        }
    }

    /** Disco plano no plano XZ, com alfa variando do raio interno ao externo. */
    public static void annulus(PoseStack ps, VertexConsumer vc, float inner, float outer, int seg,
                               float r, float g, float b, float aIn, float aOut) {
        PoseStack.Pose pose = ps.last();
        for (int j = 0; j < seg; j++) {
            double t0 = 2 * Math.PI * j / seg, t1 = 2 * Math.PI * (j + 1) / seg;
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0);
            float c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            quad(vc, pose, inner * c0, 0, inner * s0, outer * c0, 0, outer * s0, outer * c1, 0, outer * s1, inner * c1, 0, inner * s1,
                    r, g, b, aIn, aOut, aOut, aIn);
        }
    }

    /** Parede cilindrica (onda de choque) com fade para o topo. */
    public static void wall(PoseStack ps, VertexConsumer vc, float radius, float height, int seg,
                            float r, float g, float b, float aBottom, float aTop) {
        PoseStack.Pose pose = ps.last();
        for (int j = 0; j < seg; j++) {
            double t0 = 2 * Math.PI * j / seg, t1 = 2 * Math.PI * (j + 1) / seg;
            float c0 = (float) Math.cos(t0) * radius, s0 = (float) Math.sin(t0) * radius;
            float c1 = (float) Math.cos(t1) * radius, s1 = (float) Math.sin(t1) * radius;
            quad(vc, pose, c0, 0, s0, c1, 0, s1, c1, height, s1, c0, height, s0,
                    r, g, b, aBottom, aBottom, aTop, aTop);
        }
    }

    /** Tubo entre dois pontos (raio de energia). */
    public static void tube(PoseStack ps, VertexConsumer vc, Vec3 a, Vec3 b, float radius, int sides,
                            float r, float g, float bl, float alpha) {
        PoseStack.Pose pose = ps.last();
        Vec3 d = b.subtract(a);
        if (d.lengthSqr() < 1.0E-6) return;
        d = d.normalize();
        Vec3 helper = Math.abs(d.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = d.cross(helper).normalize();
        Vec3 w = d.cross(u).normalize();
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides;
            Vec3 o0 = u.scale(Math.cos(a0) * radius).add(w.scale(Math.sin(a0) * radius));
            Vec3 o1 = u.scale(Math.cos(a1) * radius).add(w.scale(Math.sin(a1) * radius));
            Vec3 p = a.add(o0), q = a.add(o1), s = b.add(o1), t = b.add(o0);
            quad(vc, pose, (float) p.x, (float) p.y, (float) p.z, (float) q.x, (float) q.y, (float) q.z,
                    (float) s.x, (float) s.y, (float) s.z, (float) t.x, (float) t.y, (float) t.z,
                    r, g, bl, alpha, alpha, alpha, alpha);
        }
    }

    private static float shadeOf(Vec3 n) {
        double l = n.length();
        if (l < 1.0E-6) return 1f;
        double dt = n.scale(1.0 / l).dot(new Vec3(0.35, 0.9, 0.25).normalize());
        return (float) (0.5 + 0.5 * Math.max(0.0, dt));
    }

    /** Tronco de cone entre dois pontos (ra -> rb). 'shade' escurece as faces viradas para baixo. */
    public static void cone(PoseStack ps, VertexConsumer vc, Vec3 a, Vec3 b, float ra, float rb, int sides,
                            float r, float g, float bl, float alpha, boolean shade) {
        PoseStack.Pose pose = ps.last();
        Vec3 d = b.subtract(a);
        if (d.lengthSqr() < 1.0E-8) return;
        d = d.normalize();
        Vec3 helper = Math.abs(d.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = d.cross(helper).normalize();
        Vec3 w = d.cross(u).normalize();
        for (int i = 0; i < sides; i++) {
            double a0 = 2 * Math.PI * i / sides, a1 = 2 * Math.PI * (i + 1) / sides;
            Vec3 n0 = u.scale(Math.cos(a0)).add(w.scale(Math.sin(a0)));
            Vec3 n1 = u.scale(Math.cos(a1)).add(w.scale(Math.sin(a1)));
            float sh = shade ? shadeOf(n0.add(n1)) : 1f;
            Vec3 p = a.add(n0.scale(ra)), q = a.add(n1.scale(ra)), s = b.add(n1.scale(rb)), t = b.add(n0.scale(rb));
            quad(vc, pose, (float) p.x, (float) p.y, (float) p.z, (float) q.x, (float) q.y, (float) q.z,
                    (float) s.x, (float) s.y, (float) s.z, (float) t.x, (float) t.y, (float) t.z,
                    r * sh, g * sh, bl * sh, alpha, alpha, alpha, alpha);
        }
    }

    /** Quad arbitrario com cor unica e alfa por vertice (fitas, discos de portal). */
    public static void quad4(PoseStack ps, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             float r, float g, float bl, float a0, float a1, float a2, float a3) {
        quad(vc, ps.last(), (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z,
                (float) c.x, (float) c.y, (float) c.z, (float) d.x, (float) d.y, (float) d.z, r, g, bl, a0, a1, a2, a3);
    }
}
