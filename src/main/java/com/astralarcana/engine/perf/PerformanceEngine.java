package com.astralarcana.engine.perf;

import com.astralarcana.engine.core.Quality;
import com.astralarcana.engine.core.Settings;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;

/** Mede o tempo de frame, ajusta a qualidade e oferece culling/LOD para o resto do motor. */
public final class PerformanceEngine {
    private PerformanceEngine() {}

    private static long last;
    private static double emaMs = 16.7;
    private static Quality level = Quality.HIGH;
    private static int goodChecks;
    private static int tickN;

    /** Chamar uma vez por frame renderizado. */
    public static void frame() {
        long now = System.nanoTime();
        if (last != 0) {
            double ms = (now - last) / 1.0E6;
            if (ms < 250.0) emaMs += (ms - emaMs) * 0.05;
        }
        last = now;
    }

    /** Chamar uma vez por tick de cliente. */
    public static void tick() {
        Quality cfg = Settings.quality;
        Quality cap = cfg == Quality.AUTO ? Quality.ULTRA : cfg;
        if (!Settings.adaptive) {
            level = cfg == Quality.AUTO ? Quality.HIGH : cfg;
            return;
        }
        if (level.ordinal() > cap.ordinal()) level = cap;
        if (++tickN % 40 != 0) return;
        double target = 1000.0 / Math.max(20, Settings.targetFps);
        if (emaMs > target * 1.25 && level.ordinal() > Quality.LOW.ordinal()) {
            level = Quality.values()[level.ordinal() - 1];
            goodChecks = 0;
        } else if (emaMs < target * 0.8) {
            if (++goodChecks >= 3 && level.ordinal() < cap.ordinal()) {
                level = Quality.values()[level.ordinal() + 1];
                goodChecks = 0;
            }
        } else {
            goodChecks = 0;
        }
    }

    /** Qualidade efetiva atual (nunca AUTO). */
    public static Quality quality() {
        return level;
    }

    public static void force(Quality q) {
        level = q == Quality.AUTO ? Quality.HIGH : q;
    }

    public static float particleScale() {
        return level.particleScale;
    }

    public static int particleBudget() {
        return level.particleBudget;
    }

    /** Numero de segmentos de uma malha conforme tamanho aparente (raio/distancia) e qualidade. */
    public static int lod(int base, double dist, double radius) {
        double apparent = radius / Math.max(dist, 1.0);
        double f = Math.min(1.0, Math.max(0.35, apparent * 10.0)) * level.detail;
        return Math.max(4, (int) Math.round(base * f));
    }

    public static boolean visible(Frustum frustum, AABB box) {
        return frustum == null || frustum.isVisible(box);
    }

    public static double frameMs() {
        return emaMs;
    }

    public static String stats() {
        return String.format("frame=%.1fms (%.0f fps) qualidade=%s (config=%s, adaptativa=%s)",
                emaMs, 1000.0 / Math.max(emaMs, 0.1), level, Settings.quality, Settings.adaptive);
    }
}
