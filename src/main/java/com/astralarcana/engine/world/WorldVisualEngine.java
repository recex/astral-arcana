package com.astralarcana.engine.world;

import com.astralarcana.engine.anim.Easing;
import com.astralarcana.engine.core.Quality;
import com.astralarcana.engine.core.Settings;
import com.astralarcana.engine.shader.ShaderProgram;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Efeitos que interagem com o ambiente SEM alterar o mundo: luzes dinamicas (re-iluminacao por profundidade),
 * ondas de choque 3D, neblina atmosferica e orbes volumetricos. Tudo vira uniforms do shader 'cinematic'.
 * Itens "hold" (keep*) sao renovados a cada tick por quem os possui e somem sozinhos ao parar de ser renovados.
 */
public final class WorldVisualEngine {
    private WorldVisualEngine() {}

    private static final int HOLD_TTL = 3;

    public static final class Light {
        Vec3 pos; final float[] col; final float radius, intensity; final int life; int age; final String key;
        Light(Vec3 pos, float[] col, float radius, float intensity, int life, String key) {
            this.pos = pos; this.col = col; this.radius = radius; this.intensity = intensity; this.life = life; this.key = key;
        }
        boolean hold() { return key != null; }
        float env(float pt) {
            if (hold()) return age >= life - 1 ? 0.5f : 1f;
            float t = Mth.clamp((age + pt) / life, 0f, 1f);
            return (1f - t) * (1f - t);
        }
    }

    public static final class Shock3D {
        final Vec3 center; final float maxR, width, strength; final float[] col; final int life; int age;
        Shock3D(Vec3 c, float maxR, int life, float width, float[] col, float strength) {
            this.center = c; this.maxR = maxR; this.life = life; this.width = width; this.col = col; this.strength = strength;
        }
    }

    public static final class Orb {
        Vec3 pos; float radius; final float[] col; final float density, turb, intensity; final int type, life; int age; final String key;
        Orb(Vec3 pos, float radius, float[] col, float density, float turb, int type, float intensity, int life, String key) {
            this.pos = pos; this.radius = radius; this.col = col; this.density = density; this.turb = turb;
            this.type = type; this.intensity = intensity; this.life = life; this.key = key;
        }
        boolean hold() { return key != null; }
        float env(float pt) {
            if (hold()) return age >= life - 1 ? 0.5f : 1f;
            float t = Mth.clamp((age + pt) / life, 0f, 1f);
            float grow = Easing.OUT_CUBIC.apply(Math.min(1f, t * 3f));
            return grow * (1f - Easing.IN_QUAD.apply(t));
        }
        float radiusScale(float pt) {
            if (hold()) return 1f;
            float t = Mth.clamp((age + pt) / life, 0f, 1f);
            return 0.4f + 0.6f * Easing.OUT_CUBIC.apply(Math.min(1f, t * 2.5f));
        }
    }

    public static final class Fog {
        final float[] col; final float density; final int life; int age; final String key;
        Fog(float[] col, float density, int life, String key) {
            this.col = col; this.density = density; this.life = life; this.key = key;
        }
        boolean hold() { return key != null; }
        float env(float pt) {
            if (hold()) return age >= life - 1 ? 0.5f : 1f;
            float t = Mth.clamp((age + pt) / life, 0f, 1f);
            return Easing.SMOOTHSTEP.apply(Math.min(1f, t * 6f)) * (1f - Easing.IN_CUBIC.apply(t));
        }
    }

    private static final List<Light> LIGHTS = new ArrayList<>();
    private static final List<Shock3D> SHOCKS = new ArrayList<>();
    private static final List<Orb> ORBS = new ArrayList<>();
    private static final List<Fog> FOGS = new ArrayList<>();

    // buffers reutilizados (sem alocacao por frame)
    private static final float[] LPA = new float[16], LCA = new float[16], LPB = new float[16], LCB = new float[16];
    private static final float[] OP = new float[16], OC = new float[16], OPAR = new float[16];
    private static final int[] PICK = new int[8];
    private static float time;

    // ---------------- API ----------------
    public static void addLight(Vec3 pos, float[] col, float radius, float intensity, int life) {
        if (LIGHTS.size() > 48) LIGHTS.remove(0);
        LIGHTS.add(new Light(pos, col, radius, intensity, life, null));
    }

    public static void keepLight(String key, Vec3 pos, float[] col, float radius, float intensity) {
        for (Light l : LIGHTS) {
            if (key.equals(l.key)) { l.pos = pos; l.age = 0; return; }
        }
        if (LIGHTS.size() > 48) LIGHTS.remove(0);
        LIGHTS.add(new Light(pos, col, radius, intensity, HOLD_TTL, key));
    }

    public static void addShock3D(Vec3 center, float maxR, int life, float width, float[] col, float strength) {
        if (SHOCKS.size() > 8) SHOCKS.remove(0);
        SHOCKS.add(new Shock3D(center, maxR, life, width, col, strength));
    }

    public static void addOrb(Vec3 pos, float radius, float[] col, float density, float turb, int type, float intensity, int life) {
        if (ORBS.size() > 16) ORBS.remove(0);
        ORBS.add(new Orb(pos, radius, col, density, turb, type, intensity, life, null));
    }

    public static void keepOrb(String key, Vec3 pos, float radius, float[] col, float density, float turb, int type, float intensity) {
        for (Orb o : ORBS) {
            if (key.equals(o.key)) { o.pos = pos; o.radius = radius; o.age = 0; return; }
        }
        if (ORBS.size() > 16) ORBS.remove(0);
        ORBS.add(new Orb(pos, radius, col, density, turb, type, intensity, HOLD_TTL, key));
    }

    public static void addFog(float[] col, float density, int life) {
        if (FOGS.size() > 8) FOGS.remove(0);
        FOGS.add(new Fog(col, density, life, null));
    }

    public static void keepFog(String key, float[] col, float density) {
        for (Fog f : FOGS) {
            if (key.equals(f.key)) { f.age = 0; return; }
        }
        if (FOGS.size() > 8) FOGS.remove(0);
        FOGS.add(new Fog(col, density, HOLD_TTL, key));
    }

    // ---------------- ciclo de vida ----------------
    public static void tick() {
        time += 1f / 20f;
        for (Light l : LIGHTS) l.age++;
        LIGHTS.removeIf(l -> l.age >= l.life);
        for (Shock3D s : SHOCKS) s.age++;
        SHOCKS.removeIf(s -> s.age >= s.life);
        for (Orb o : ORBS) o.age++;
        ORBS.removeIf(o -> o.age >= o.life);
        for (Fog f : FOGS) f.age++;
        FOGS.removeIf(f -> f.age >= f.life);
    }

    public static void clear() {
        LIGHTS.clear();
        SHOCKS.clear();
        ORBS.clear();
        FOGS.clear();
    }

    public static boolean active() {
        return !LIGHTS.isEmpty() || !SHOCKS.isEmpty() || !ORBS.isEmpty() || !FOGS.isEmpty();
    }

    public static boolean needsScenePass() {
        return active();
    }

    /** Intensidade geral de brilho, usada para alimentar o bloom. */
    public static float glow() {
        float g = 0f;
        for (Light l : LIGHTS) g += l.intensity * l.env(0f) * 0.15f;
        for (Orb o : ORBS) g += o.intensity * o.env(0f) * 0.25f;
        for (Shock3D s : SHOCKS) g += s.strength * (1f - (float) s.age / s.life) * 0.2f;
        return Math.min(g, 1.2f);
    }

    public static String stats() {
        return "luzes=" + LIGHTS.size() + " ondas3D=" + SHOCKS.size() + " orbes=" + ORBS.size() + " neblinas=" + FOGS.size();
    }

    // ---------------- empacotamento para o shader ----------------
    private static int pickTop(int n, int max) {
        int count = 0;
        boolean[] used = new boolean[n];
        for (int k = 0; k < max; k++) {
            int best = -1;
            float bs = 0f;
            for (int i = 0; i < n; i++) {
                if (!used[i] && SCORE_TMP[i] > bs) { bs = SCORE_TMP[i]; best = i; }
            }
            if (best < 0) break;
            used[best] = true;
            PICK[count++] = best;
        }
        return count;
    }

    private static float[] SCORE_TMP = new float[64];

    public static void fill(ShaderProgram sp, Vec3 cam, float pt, Quality q) {
        java.util.Arrays.fill(LPA, 0f); java.util.Arrays.fill(LCA, 0f);
        java.util.Arrays.fill(LPB, 0f); java.util.Arrays.fill(LCB, 0f);
        java.util.Arrays.fill(OP, 0f); java.util.Arrays.fill(OC, 0f); java.util.Arrays.fill(OPAR, 0f);

        // luzes
        if (Settings.dynamicLights && !LIGHTS.isEmpty()) {
            int n = Math.min(LIGHTS.size(), SCORE_TMP.length);
            for (int i = 0; i < n; i++) {
                Light l = LIGHTS.get(i);
                double dx = l.pos.x - cam.x, dy = l.pos.y - cam.y, dz = l.pos.z - cam.z;
                SCORE_TMP[i] = (float) (l.intensity * l.env(pt) * l.radius * l.radius / (dx * dx + dy * dy + dz * dz + 25.0));
            }
            int c = pickTop(n, Math.min(8, q.maxLights));
            for (int k = 0; k < c; k++) {
                Light l = LIGHTS.get(PICK[k]);
                float[] pos = k < 4 ? LPA : LPB, col = k < 4 ? LCA : LCB;
                int o = (k % 4) * 4;
                pos[o] = (float) (l.pos.x - cam.x); pos[o + 1] = (float) (l.pos.y - cam.y); pos[o + 2] = (float) (l.pos.z - cam.z);
                pos[o + 3] = l.radius;
                col[o] = l.col[0]; col[o + 1] = l.col[1]; col[o + 2] = l.col[2]; col[o + 3] = l.intensity * l.env(pt);
            }
        }
        sp.setMat("LightPosA", LPA); sp.setMat("LightColA", LCA);
        sp.setMat("LightPosB", LPB); sp.setMat("LightColB", LCB);

        // orbes
        if (!ORBS.isEmpty()) {
            int n = Math.min(ORBS.size(), SCORE_TMP.length);
            for (int i = 0; i < n; i++) {
                Orb ob = ORBS.get(i);
                double dx = ob.pos.x - cam.x, dy = ob.pos.y - cam.y, dz = ob.pos.z - cam.z;
                SCORE_TMP[i] = (float) (ob.radius * ob.radius * ob.env(pt) / (dx * dx + dy * dy + dz * dz + 25.0));
            }
            int c = pickTop(n, Math.min(4, q.maxOrbs));
            for (int k = 0; k < c; k++) {
                Orb ob = ORBS.get(PICK[k]);
                int o = k * 4;
                OP[o] = (float) (ob.pos.x - cam.x); OP[o + 1] = (float) (ob.pos.y - cam.y); OP[o + 2] = (float) (ob.pos.z - cam.z);
                OP[o + 3] = ob.radius * ob.radiusScale(pt);
                OC[o] = ob.col[0]; OC[o + 1] = ob.col[1]; OC[o + 2] = ob.col[2]; OC[o + 3] = ob.density;
                OPAR[o] = time + pt / 20f; OPAR[o + 1] = ob.turb; OPAR[o + 2] = ob.type; OPAR[o + 3] = ob.intensity * ob.env(pt);
            }
        }
        sp.setMat("OrbPos", OP); sp.setMat("OrbCol", OC); sp.setMat("OrbPar", OPAR);

        // ondas 3D (as 2 mais fortes)
        float[] s0 = {0, 0, 0, 0, 0, 0, 0, 0}, s1 = {0, 0, 0, 0, 0, 0, 0, 0};
        int b0 = -1, b1 = -1;
        float v0 = 0f, v1 = 0f;
        for (int i = 0; i < SHOCKS.size(); i++) {
            Shock3D s = SHOCKS.get(i);
            float t = Mth.clamp((s.age + pt) / s.life, 0f, 1f);
            float v = s.strength * (float) Math.pow(1f - t, 1.5);
            if (v > v0) { b1 = b0; v1 = v0; b0 = i; v0 = v; }
            else if (v > v1) { b1 = i; v1 = v; }
        }
        float w0 = 1f, w1 = 1f;
        if (b0 >= 0) { pack(SHOCKS.get(b0), cam, pt, s0); w0 = SHOCKS.get(b0).width; }
        if (b1 >= 0) { pack(SHOCKS.get(b1), cam, pt, s1); w1 = SHOCKS.get(b1).width; }
        sp.set4("ShockP0", s0[0], s0[1], s0[2], s0[3]); sp.set4("ShockC0", s0[4], s0[5], s0[6], s0[7]);
        sp.set4("ShockP1", s1[0], s1[1], s1[2], s1[3]); sp.set4("ShockC1", s1[4], s1[5], s1[6], s1[7]);
        sp.set2("ShockW", w0, w1);

        // neblina: soma ponderada de todas as camadas
        float dens = 0f, r = 0f, g = 0f, b = 0f;
        for (Fog f : FOGS) {
            float d = f.density * f.env(pt);
            dens += d;
            r += f.col[0] * d; g += f.col[1] * d; b += f.col[2] * d;
        }
        if (dens > 1.0E-5f) { r /= dens; g /= dens; b /= dens; }
        sp.set4("Atmos", r, g, b, Math.min(dens, 0.08f));
        sp.set4("AtmosB", 0.05f, (float) cam.y - 8f, 0.85f, 0f);
    }

    private static void pack(Shock3D s, Vec3 cam, float pt, float[] out) {
        float t = Mth.clamp((s.age + pt) / s.life, 0f, 1f);
        out[0] = (float) (s.center.x - cam.x); out[1] = (float) (s.center.y - cam.y); out[2] = (float) (s.center.z - cam.z);
        out[3] = s.maxR * Easing.OUT_CUBIC.apply(t);
        out[4] = s.col[0]; out[5] = s.col[1]; out[6] = s.col[2];
        out[7] = s.strength * (float) Math.pow(1f - t, 1.5);
    }
}
