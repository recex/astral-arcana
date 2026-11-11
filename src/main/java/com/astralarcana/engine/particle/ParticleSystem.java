package com.astralarcana.engine.particle;

import com.astralarcana.client.particle.GlowParticle;
import com.astralarcana.client.particle.OrbitParticle;
import com.astralarcana.engine.perf.PerformanceEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

/** Ponto unico de criacao de particulas do mod: orcamento por tick, escala de qualidade e LOD por distancia. */
public final class ParticleSystem {
    private ParticleSystem() {}

    private static int used;
    private static double camX, camY, camZ;
    private static int lastBudget;
    private static int rng = 0x2545F491;

    public static void beginTick() {
        lastBudget = used;
        used = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer != null && mc.gameRenderer.getMainCamera() != null) {
            Vec3 c = mc.gameRenderer.getMainCamera().getPosition();
            camX = c.x;
            camY = c.y;
            camZ = c.z;
        }
    }

    public static int usedLastTick() {
        return lastBudget;
    }

    private static float rand() {
        rng ^= rng << 13;
        rng ^= rng >>> 17;
        rng ^= rng << 5;
        return (rng & 0xFFFFFF) / (float) 0x1000000;
    }

    /** Decide se uma particula nesta posicao pode ser criada (orcamento + escala + distancia). */
    public static boolean allow(double x, double y, double z) {
        if (used >= PerformanceEngine.particleBudget()) return false;
        double dx = x - camX, dy = y - camY, dz = z - camZ;
        double d2 = dx * dx + dy * dy + dz * dz;
        float keep = PerformanceEngine.particleScale();
        if (d2 > 48.0 * 48.0) keep *= (float) Math.min(1.0, (48.0 * 48.0) / d2 * 2.0);
        if (keep < 1f && rand() > keep) return false;
        used++;
        return true;
    }

    public static void glow(ClientLevel l, double x, double y, double z, double vx, double vy, double vz,
                            float[] c, float size, int life, float grav, float drag) {
        if (GlowParticle.SPRITES == null || !allow(x, y, z)) return;
        Minecraft.getInstance().particleEngine.add(new GlowParticle(l, x, y, z, vx, vy, vz, c[0], c[1], c[2], size, life, grav, drag));
    }

    public static void orbit(ClientLevel l, Vec3 c, double ang, double radius, double angSpeed, double inward,
                             double tilt, double rise, float[] col, float size, int life) {
        if (GlowParticle.SPRITES == null || !allow(c.x, c.y, c.z)) return;
        OrbitParticle p = new OrbitParticle(l, c.x, c.y, c.z, ang, radius, angSpeed, inward, tilt, col, size, life);
        if (rise != 0) p.rise(rise);
        Minecraft.getInstance().particleEngine.add(p);
    }
}
