package com.astralarcana.engine.cinematic;

import com.astralarcana.engine.core.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de camera/cinematica: tremor por ruido suave em camadas (com decaimento), curvas de camera por sequencia
 * (yaw/pitch/roll/FOV), barras de cinema, fade e disparo de eventos temporizados (VFX, audio, shader).
 * Limite tecnico: a API de camera do Forge 1.20.1 permite alterar angulos e FOV, nao a posicao da camera.
 */
public final class CinematicEngine {
    private CinematicEngine() {}

    private static final class Shake {
        float amp;
        int total, age;
        float seed;
    }

    private static final class Instance {
        final Sequence seq;
        final Vec3 origin;
        final float k;
        int age, next;

        Instance(Sequence seq, Vec3 origin, float k) {
            this.seq = seq;
            this.origin = origin;
            this.k = k;
        }
    }

    private static final List<Shake> SHAKES = new ArrayList<>();
    private static final List<Instance> RUNNING = new ArrayList<>();

    // ------------------------------------------------------------------ tremor
    public static void shake(float amp, int ticks) {
        if (amp <= 0.01f || ticks <= 0) return;
        if (SHAKES.size() >= 8) {
            Shake weakest = SHAKES.get(0);
            for (Shake s : SHAKES) if (s.amp * (1f - (float) s.age / s.total) < weakest.amp * (1f - (float) weakest.age / weakest.total)) weakest = s;
            SHAKES.remove(weakest);
        }
        Shake s = new Shake();
        s.amp = amp;
        s.total = ticks;
        s.seed = (float) (Math.random() * 1000.0);
        SHAKES.add(s);
    }

    public static float currentShake() {
        return currentShake(0f);
    }

    /** Energia combinada (raiz da soma dos quadrados) das camadas ativas, com decaimento quadratico. */
    public static float currentShake(float pt) {
        float sum = 0f;
        for (Shake s : SHAKES) {
            float t = Mth.clamp((s.age + pt) / s.total, 0f, 1f);
            float a = s.amp * (1f - t) * (1f - t);
            sum += a * a;
        }
        return Math.min(8f, (float) Math.sqrt(sum)) * Settings.shakeScale;
    }

    // ------------------------------------------------------------------ sequencias
    public static void play(Sequence seq, Vec3 origin, float k) {
        if (seq == null) return;
        if (RUNNING.size() >= 6) RUNNING.remove(0);
        RUNNING.add(new Instance(seq, origin, Mth.clamp(k, 0f, 1f)));
    }

    public static int running() {
        return RUNNING.size();
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        for (Shake s : SHAKES) s.age++;
        SHAKES.removeIf(s -> s.age >= s.total);
        if (level == null) {
            RUNNING.clear();
            return;
        }
        for (Instance in : RUNNING) {
            in.age++;
            Sequence.Ctx ctx = new Sequence.Ctx(level, in.origin, in.k);
            while (in.next < in.seq.events.size() && in.seq.events.get(in.next).tick <= in.age) {
                in.seq.events.get(in.next).action.run(ctx);
                in.next++;
            }
        }
        RUNNING.removeIf(in -> in.age > in.seq.duration);
    }

    public static void clear() {
        SHAKES.clear();
        RUNNING.clear();
    }

    private static float eval(Sequence.Prop p, float pt, boolean scaled) {
        float v = 0f;
        for (Instance in : RUNNING) {
            com.astralarcana.engine.anim.Track t = in.seq.tracks.get(p);
            if (t == null) continue;
            v += t.eval(in.age + pt) * (scaled ? in.k : 1f);
        }
        return v;
    }

    public static float fovOffset() {
        return eval(Sequence.Prop.FOV, 0f, true);
    }

    public static float bars() {
        return Mth.clamp(eval(Sequence.Prop.BARS, 0f, false), 0f, 1f);
    }

    public static float fadeAlpha() {
        return Mth.clamp(eval(Sequence.Prop.FADE, 0f, false), 0f, 1f);
    }

    public static float[] fadeColor() {
        return RUNNING.isEmpty() ? new float[]{0f, 0f, 0f} : RUNNING.get(RUNNING.size() - 1).seq.fadeColor;
    }

    // ------------------------------------------------------------------ camera
    private static float hash(int n) {
        n = (n << 13) ^ n;
        return 1f - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824f;
    }

    /** Ruido de valor suave em [-1, 1]. */
    static float noise(float x, int seed) {
        int i = (int) Math.floor(x);
        float f = x - i;
        f = f * f * (3f - 2f * f);
        return Mth.lerp(f, hash(i + seed * 131), hash(i + 1 + seed * 131));
    }

    public static void applyCamera(ViewportEvent.ComputeCameraAngles e, float charge) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = (float) e.getPartialTick();
        float s = currentShake(pt) + charge * 0.12f * Settings.shakeScale;
        float yaw = eval(Sequence.Prop.YAW, pt, true);
        float pitch = eval(Sequence.Prop.PITCH, pt, true);
        float roll = eval(Sequence.Prop.ROLL, pt, true);
        if (s > 0.001f) {
            float t = mc.level.getGameTime() + pt;
            yaw += noise(t * 2.3f, 1) * s * 1.2f;
            pitch += noise(t * 2.1f, 2) * s * 1.2f;
            roll += noise(t * 2.7f, 3) * s * 1.4f;
        }
        e.setYaw(e.getYaw() + yaw);
        e.setPitch(e.getPitch() + pitch);
        e.setRoll(e.getRoll() + roll);
    }
}
