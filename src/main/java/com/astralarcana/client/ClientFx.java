package com.astralarcana.client;

import com.astralarcana.AstralArcana;
import com.astralarcana.client.particle.GlowParticle;
import com.astralarcana.client.particle.OrbitParticle;
import com.astralarcana.entity.BlackHoleEntity;
import com.astralarcana.entity.MeteorEntity;
import com.astralarcana.item.CosmicStaffItem;
import com.astralarcana.network.FxPacket;
import com.astralarcana.engine.cinematic.CinematicEngine;
import com.astralarcana.engine.cinematic.Sequences;
import com.astralarcana.engine.core.Engine;
import com.astralarcana.engine.particle.ParticleSystem;
import com.astralarcana.engine.shader.PostParams;
import com.astralarcana.engine.shader.PostPipeline;
import com.astralarcana.engine.shader.ShaderEngine;
import com.astralarcana.engine.vfx.Debris;
import com.astralarcana.engine.vfx.GroundCrackEffect;
import com.astralarcana.engine.vfx.PortalEffect;
import com.astralarcana.engine.vfx.RibbonEffect;
import com.astralarcana.engine.vfx.VFXEngine;
import com.astralarcana.engine.world.WorldVisualEngine;
import com.astralarcana.entity.CosmicSerpentEntity;
import com.astralarcana.entity.SpellZoneEntity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Todo o estado e logica de efeitos do lado do cliente: tremor, flash, barras, ondas de choque, raios, particulas. */
@Mod.EventBusSubscriber(modid = AstralArcana.MODID, value = Dist.CLIENT)
public final class ClientFx {
    private ClientFx() {}

    public static final ResourceLocation SOLID = AstralArcana.id("textures/fx/solid.png");

    static final float[] ORANGE = {1f, .55f, .15f};
    static final float[] YELLOW = {1f, .9f, .5f};
    static final float[] VIOLET = {.65f, .3f, 1f};
    static final float[] CYAN = {.3f, .85f, 1f};
    static final float[] WHITE = {1f, 1f, 1f};
    static final float[] TEAL = {.4f, 1f, .8f};
    static final float[] SILVER = {.8f, .9f, 1f};
    static final float[] ELEC = {.5f, .7f, 1f};
    static final float[] PINK = {1f, .3f, .55f};
    static final float[] LIME = {.6f, 1f, .5f};
    static final float[] RED = {1f, .15f, .1f};
    static final float[][] SPELL_COL = {ORANGE, VIOLET, CYAN, YELLOW, TEAL, SILVER, VIOLET, YELLOW, ELEC, PINK, LIME,
            VIOLET, CYAN, ORANGE, SILVER, CYAN, ELEC, VIOLET, TEAL, CYAN, ORANGE,
            VIOLET, TEAL, RED, VIOLET, CYAN, PINK, ELEC, SILVER, ORANGE, LIME};
    private static final double TILT = 0.349; // 20 graus, igual ao disco do buraco negro

    // ---- estado ----
    private static float shakeAmp, shakeTotal = 1, shakeLeft;
    private static float flashA, flashDecay, flashR, flashG, flashB;
    private static float charge, bars;
    private static float[] chargeCol = VIOLET;
    private static final List<Shock> shocks = new ArrayList<>();
    private static final List<Beam> beams = new ArrayList<>();

    private static final class Shock {
        final Vec3 pos; final float maxR; final int life; final float[] col; final float alpha; final boolean sphere;
        int age;
        Shock(Vec3 pos, float maxR, int life, float[] col, float alpha, boolean sphere) {
            this.pos = pos; this.maxR = maxR; this.life = life; this.col = col; this.alpha = alpha; this.sphere = sphere;
        }
    }

    private static final class Beam {
        final Vec3 a, b; final float power, thick; final int life; final float[] mid, outer; final boolean particles;
        int age;
        Beam(Vec3 a, Vec3 b, float power) {
            this(a, b, power, 1f, 16, new float[]{.3f, .8f, 1f}, new float[]{.45f, .3f, 1f}, true);
        }
        Beam(Vec3 a, Vec3 b, float power, float thick, int life, float[] mid, float[] outer, boolean particles) {
            this.a = a; this.b = b; this.power = power; this.thick = thick; this.life = life;
            this.mid = mid; this.outer = outer; this.particles = particles;
        }
    }

    // ---- helpers ----
    private static Vec3 randomUnit(RandomSource r) {
        double x, y, z, l;
        do {
            x = r.nextDouble() * 2 - 1; y = r.nextDouble() * 2 - 1; z = r.nextDouble() * 2 - 1;
            l = x * x + y * y + z * z;
        } while (l > 1 || l < 0.001);
        l = Math.sqrt(l);
        return new Vec3(x / l, y / l, z / l);
    }

    private static void glow(ClientLevel l, double x, double y, double z, double vx, double vy, double vz,
                             float[] c, float size, int life, float grav, float drag) {
        ParticleSystem.glow(l, x, y, z, vx, vy, vz, c, size, life, grav, drag);
    }

    private static void orbit(ClientLevel l, Vec3 c, double ang, double radius, double angSpeed, double inward,
                              double tilt, float[] col, float size, int life) {
        ParticleSystem.orbit(l, c, ang, radius, angSpeed, inward, tilt, 0.0, col, size, life);
    }

    private static float[] pick(RandomSource r, float[]... cols) {
        return cols[r.nextInt(cols.length)];
    }

    private static void ringBurst(ClientLevel l, Vec3 c, int n, double speed, float[] col, float size, int life) {
        RandomSource r = l.random;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + r.nextDouble() * 0.05;
            double s = speed * (0.9 + r.nextDouble() * 0.2);
            glow(l, c.x, c.y, c.z, Math.cos(a) * s, 0.02, Math.sin(a) * s, col, size, life, 0f, 0.95f);
        }
    }

    private static void dome(ClientLevel l, Vec3 c, int n, double minSpeed, double maxSpeed, float size, int life,
                             float grav, float[]... cols) {
        RandomSource r = l.random;
        for (int i = 0; i < n; i++) {
            Vec3 d = randomUnit(r);
            double sp = minSpeed + r.nextDouble() * (maxSpeed - minSpeed);
            glow(l, c.x, c.y, c.z, d.x * sp, Math.abs(d.y) * sp + 0.05, d.z * sp, pick(r, cols),
                    size * (0.6f + r.nextFloat() * 0.8f), life / 2 + r.nextInt(life / 2 + 1), grav, 0.93f);
        }
    }

    private static void sphere(Vec3 c, float maxR, int life, float[] col, float alpha) {
        shocks.add(new Shock(c, maxR, life, col, alpha, true));
    }

    private static void ring(Vec3 c, float maxR, int life, float[] col) {
        shocks.add(new Shock(c, maxR, life, col, 0.7f, false));
    }

    private static float curShake() {
        return CinematicEngine.currentShake();
    }

    private static void shake(float amp, int ticks, float near) {
        CinematicEngine.shake(amp * near, ticks);
    }

    private static void flash(float[] c, float a, float decay) {
        if (a > flashA) {
            flashA = a; flashDecay = decay; flashR = c[0]; flashG = c[1]; flashB = c[2];
        }
    }

    // ---- recebe pacotes do servidor ----
    public static void handle(FxPacket pk) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || mc.player == null) return;
        Vec3 a = new Vec3(pk.x, pk.y, pk.z);
        Vec3 b = new Vec3(pk.x2, pk.y2, pk.z2);
        float near = (float) Mth.clamp(1.0 - mc.player.position().distanceTo(a) / 110.0, 0.0, 1.0);
        FxPacket.Type[] types = FxPacket.Type.values();
        switch (types[Mth.clamp(pk.type, 0, types.length - 1)]) {
            case METEOR_IMPACT -> meteorImpact(l, a, pk.power, near);
            case SUPERNOVA -> supernova(l, a, pk.power, near);
            case BEAM -> beam(l, a, b, pk.power, near);
            case BLACKHOLE_BIRTH -> blackHoleBirth(l, a, near);
            case BLACKHOLE_COLLAPSE -> blackHoleCollapse(l, a, near);
            case TIMESTOP -> timeStop(l, a, (int) pk.x2, near);
            case BLINK -> blink(l, a, b, near);
            case ORBITAL -> orbitalHit(l, a, b, pk.power, near);
            case CHAIN -> chain(l, a, b, near);
            case SLASH -> slash(l, a, b, pk.power, near);
            case REBIRTH -> rebirth(l, a, near);
            case TORNADO -> tornadoBirth(l, a, near);
            case PRESET -> preset(l, a, b, pk.power, near);
            case LINK -> link(l, a, b, near);
        }
    }

    private static void meteorImpact(ClientLevel l, Vec3 c, float size, float near) {
        RandomSource r = l.random;
        shake(1.8f * size, 30, near);
        ripple(c, 0.035f * size * near, 22);
        warp(0.08f * near);
        flash(ORANGE, 0.5f * near, 0.06f);
        WorldVisualEngine.addLight(c.add(0, 1, 0), ORANGE, 26f * size, 2.4f, 34);
        WorldVisualEngine.addShock3D(c, 38f * size, 16, 2.2f, ORANGE, 1.4f);
        WorldVisualEngine.addOrb(c.add(0, 2.5 * size, 0), 5.5f * size, ORANGE, 1.0f, 1.1f, 0, 1.0f, 26);
        VFXEngine.add(new GroundCrackEffect(l, c, 11f * size, 70));
        Debris.burst(l, c, (int) (40 * size), 0.6);
        com.astralarcana.engine.audio.AudioEngine.play(com.astralarcana.engine.audio.AudioEngine.Cues.IMPACT, c, Math.min(1f, 0.4f + size * 0.3f));
        sphere(c, 6f * size, 12, ORANGE, 0.8f);
        sphere(c, 3.5f * size, 7, WHITE, 0.9f);
        ring(c.add(0, 0.2, 0), 14f * size, 20, ORANGE);
        ring(c.add(0, 0.2, 0), 22f * size, 30, VIOLET);
        dome(l, c.add(0, 0.5, 0), 150, 0.3, 0.3 + 1.5 * size, 0.55f, 55, 0.25f, ORANGE, YELLOW, VIOLET);
        for (int i = 0; i < 70; i++) {
            glow(l, c.x + r.nextGaussian() * 0.8, c.y, c.z + r.nextGaussian() * 0.8,
                    r.nextGaussian() * 0.08, 0.6 + r.nextDouble() * 1.4 * size, r.nextGaussian() * 0.08,
                    pick(r, ORANGE, YELLOW), 0.5f + r.nextFloat() * 0.7f, 30 + r.nextInt(25), 0f, 0.96f);
        }
        ringBurst(l, c.add(0, 0.3, 0), 110, 1.5 * size, ORANGE, 0.6f, 24);
        ringBurst(l, c.add(0, 0.3, 0), 80, 0.9 * size, YELLOW, 0.5f, 28);
        l.addParticle(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 0, 0, 0);
        for (int i = 0; i < 20; i++) l.addParticle(ParticleTypes.LAVA, c.x, c.y + 0.5, c.z, 0, 0, 0);
        for (int i = 0; i < 20; i++) {
            l.addParticle(ParticleTypes.LARGE_SMOKE, c.x + r.nextGaussian(), c.y + 1, c.z + r.nextGaussian(),
                    r.nextGaussian() * 0.1, 0.2, r.nextGaussian() * 0.1);
        }
    }

    private static void supernova(ClientLevel l, Vec3 c, float power, float near) {
        RandomSource r = l.random;
        CinematicEngine.play(Sequences.SUPERNOVA, c, Math.max(near, 0.35f));
        sphere(c, 14f, 10, ORANGE, 0.9f);
        sphere(c, 22f + 6 * power, 20, WHITE, 0.9f);
        sphere(c, 28f + 8 * power, 28, CYAN, 0.6f);
        sphere(c, 36f + 10 * power, 40, VIOLET, 0.5f);
        Vec3 g = c.add(0, -0.9, 0);
        ring(g, 30f + 10 * power, 22, WHITE);
        ring(g, 40f + 12 * power, 32, CYAN);
        ring(g, 50f + 14 * power, 44, VIOLET);
        dome(l, c, 500, 0.5, 2.8, 0.7f, 80, 0f, WHITE, CYAN, VIOLET, YELLOW);
        ringBurst(l, g.add(0, 0.4, 0), 180, 2.2, WHITE, 0.7f, 30);
        ringBurst(l, g.add(0, 0.4, 0), 160, 1.5, CYAN, 0.6f, 36);
        ringBurst(l, g.add(0, 0.4, 0), 140, 0.9, VIOLET, 0.6f, 44);
        for (int i = 0; i < 120; i++) {
            glow(l, c.x + r.nextGaussian() * 0.6, c.y, c.z + r.nextGaussian() * 0.6, r.nextGaussian() * 0.05,
                    0.8 + r.nextDouble() * 2.2, r.nextGaussian() * 0.05, pick(r, WHITE, CYAN), 0.7f, 50, 0f, 0.97f);
        }
        l.addParticle(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 0, 0, 0);
        l.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);
    }

    private static void beam(ClientLevel l, Vec3 a, Vec3 b, float power, float near) {
        beams.add(new Beam(a, b, power));
        WorldVisualEngine.addLight(b, CYAN, 22f, 2.0f, 16);
        WorldVisualEngine.addLight(a, WHITE, 12f, 1.4f, 10);
        WorldVisualEngine.addShock3D(b, 22f, 12, 1.8f, CYAN, 1.2f);
        shake(0.8f, 12, Math.max(near, 0.5f));
        ripple(b, 0.03f * near, 16);
        warp(0.06f * near);
        flash(CYAN, 0.25f, 0.05f);
        sphere(b, 5f + 3 * power, 10, CYAN, 0.8f);
        sphere(a, 2.5f, 6, WHITE, 0.8f);
        dome(l, b, 110, 0.3, 1.6, 0.5f, 34, 0.1f, CYAN, WHITE, VIOLET);
        ringBurst(l, b.add(0, 0.3, 0), 60, 1.2, CYAN, 0.5f, 22);
    }

    private static void blackHoleBirth(ClientLevel l, Vec3 c, float near) {
        RandomSource r = l.random;
        shake(1.0f, 20, near);
        ripple(c, 0.04f * near, 24);
        sphere(c, 8f, 14, VIOLET, 0.6f);
        ring(c, 18f, 24, VIOLET);
        for (int i = 0; i < 160; i++) {
            Vec3 d = randomUnit(r);
            Vec3 p = c.add(d.scale(7));
            glow(l, p.x, p.y, p.z, -d.x * 0.45, -d.y * 0.45, -d.z * 0.45, pick(r, VIOLET, WHITE), 0.4f, 15, 0f, 1f);
        }
    }

    private static void blackHoleCollapse(ClientLevel l, Vec3 c, float near) {
        CinematicEngine.play(Sequences.COLLAPSE, c, Math.max(near, 0.3f));
        sphere(c, 16f, 14, WHITE, 0.9f);
        sphere(c, 28f, 26, VIOLET, 0.6f);
        ring(c.add(0, -1, 0), 32f, 30, VIOLET);
        dome(l, c, 450, 0.5, 2.6, 0.7f, 70, 0f, VIOLET, ORANGE, WHITE);
        ringBurst(l, c, 160, 2.0, VIOLET, 0.6f, 34);
        l.addParticle(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 0, 0, 0);
    }

    // ---- chamados por entidades / itens (todo tick de cliente) ----
    public static void meteorTrail(MeteorEntity e) {
        ClientLevel l = Minecraft.getInstance().level;
        if (l == null) return;
        RandomSource r = l.random;
        Vec3 vel = e.getDeltaMovement();
        if (vel.lengthSqr() < 1.0E-4) return;
        Vec3 back = vel.normalize().scale(-1);
        float s = e.getSize();
        Vec3 c = e.position().add(0, e.getBbHeight() / 2.0, 0);
        RIBBONS.computeIfAbsent(e.getId(), id -> {
            RibbonEffect rb = new RibbonEffect(() -> e.isRemoved() ? null : e.position().add(0, e.getBbHeight() / 2.0, 0),
                    26, 1.6f * s, ORANGE, VIOLET);
            VFXEngine.add(rb);
            return rb;
        });
        WorldVisualEngine.keepLight("m" + e.getId(), c, ORANGE, 24f * s, 1.3f);
        for (int i = 0; i < 10; i++) {
            double d = i * 0.55 * s + r.nextDouble();
            Vec3 p = c.add(back.scale(d)).add(r.nextGaussian() * 0.3 * s, r.nextGaussian() * 0.3 * s, r.nextGaussian() * 0.3 * s);
            glow(l, p.x, p.y, p.z, back.x * 0.05, back.y * 0.05, back.z * 0.05, i % 3 == 0 ? YELLOW : ORANGE,
                    Math.max(0.3f, 1.0f * s * (1f - i / 12f)), 14 + r.nextInt(10), 0f, 0.9f);
        }
        for (int i = 0; i < 3; i++) {
            Vec3 p = c.add(back.scale(r.nextDouble() * 4 * s));
            glow(l, p.x, p.y, p.z, r.nextGaussian() * 0.05, r.nextGaussian() * 0.05, r.nextGaussian() * 0.05, VIOLET, 0.4f, 24, 0f, 0.95f);
        }
        if (r.nextInt(2) == 0) {
            l.addParticle(ParticleTypes.LARGE_SMOKE, c.x + back.x * 2, c.y + back.y * 2, c.z + back.z * 2, 0, 0, 0);
        }
    }

    public static void blackHoleTick(BlackHoleEntity e) {
        ClientLevel l = Minecraft.getInstance().level;
        if (l == null || Minecraft.getInstance().isPaused()) return;
        RandomSource r = l.random;
        int age = e.tickCount, max = e.getMaxLife();
        float ramp = Math.min(1f, age / 20f) * Math.min(1f, (max - age) / 20f);
        if (ramp <= 0.02f) return;
        Vec3 c = e.center();
        String key = "bh" + e.getId();
        WorldVisualEngine.keepOrb(key, c, 9.5f * ramp, VIOLET, 1.0f, 1.2f, 1, 1.0f);
        WorldVisualEngine.keepLight(key, c, ORANGE, 24f * ramp, 1.2f);
        WorldVisualEngine.keepFog(key, VIOLET, 0.006f * ramp);

        // disco de acrecao
        int n = Math.round(10 * ramp);
        for (int i = 0; i < n; i++) {
            double t = Math.pow(r.nextDouble(), 1.5);
            double rad = 2.2 + t * 9.0;
            float[] col = t < 0.25 ? WHITE : (t < 0.6 ? ORANGE : VIOLET);
            double speed = 0.9 / Math.pow(rad, 1.5);
            orbit(l, c, r.nextDouble() * Math.PI * 2, rad, speed, 0.03 + r.nextDouble() * 0.03, TILT, col,
                    0.3f + r.nextFloat() * 0.3f, 34);
        }
        // fluxos de materia espiralando para dentro
        for (int i = 0; i < 2; i++) {
            orbit(l, c, r.nextDouble() * Math.PI * 2, 14, 0.09, 0.32, TILT + (r.nextDouble() - 0.5) * 0.4, pick(r, CYAN, VIOLET), 0.4f, 40);
        }
        // jatos relativisticos
        for (int i = 0; i < 2; i++) {
            double dir = i == 0 ? 1 : -1;
            glow(l, c.x, c.y, c.z, r.nextGaussian() * 0.04, dir * (0.8 + r.nextDouble() * 0.6) * ramp, r.nextGaussian() * 0.04,
                    pick(r, CYAN, WHITE), 0.35f, 22, 0f, 0.99f);
        }
        // tremor leve perto
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            double d = p.position().distanceTo(c);
            if (d < 30) shake(0.35f * ramp, 4, (float) (1 - d / 30));
        }
    }

    public static void chargeTick(LivingEntity user, int spell, int ticks, float ratio) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || mc.isPaused()) return;
        RandomSource r = l.random;
        float[] col = SPELL_COL[Mth.clamp(spell, 0, SPELL_COL.length - 1)];
        if (user == mc.player) chargeCol = col;

        Vec3 tip = user.getEyePosition().add(user.getLookAngle().scale(1.5)).add(0, -0.3, 0);
        int n = 2 + (int) (ratio * 4);
        for (int i = 0; i < n; i++) {
            Vec3 d = randomUnit(r);
            double R = (2.6 - 1.6 * ratio) * (0.6 + r.nextDouble() * 0.6);
            Vec3 p = tip.add(d.scale(R));
            Vec3 v = tip.subtract(p).scale(1.0 / 9.0);
            glow(l, p.x, p.y, p.z, v.x, v.y, v.z, col, 0.22f + 0.15f * ratio, 9, 0f, 1f);
        }
        // circulo magico no chao
        Vec3 feet = new Vec3(user.getX(), user.getY() + 0.08, user.getZ());
        for (int k = 0; k < 3; k++) orbit(l, feet, r.nextDouble() * Math.PI * 2, 2.6, 0.14, 0, 0, col, 0.35f, 10);
        for (int k = 0; k < 2; k++) orbit(l, feet, r.nextDouble() * Math.PI * 2, 1.5, -0.22, 0, 0, WHITE, 0.28f, 10);
        for (int k = 0; k < 2; k++) orbit(l, feet, r.nextDouble() * Math.PI * 2, 3.5, 0.07, 0, 0, col, 0.3f, 10);
        if (ratio > 0.5f) {
            glow(l, feet.x + r.nextGaussian() * 0.4, feet.y, feet.z + r.nextGaussian() * 0.4, 0, 0.5 + r.nextDouble() * 0.5, 0,
                    col, 0.5f, 18, 0f, 0.98f);
        }
        if (ticks == CosmicStaffItem.MAX_CHARGE) {
            dome(l, tip, 50, 0.1, 0.6, 0.4f, 20, 0f, col, WHITE);
        }
    }

    // ---- ticks ----
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null) {
            shocks.clear(); beams.clear();
            shakeLeft = 0; flashA = 0; charge = 0; bars = 0; warpAmt = 0; ripples.clear();
            bolts.clear(); slashes.clear(); emitters.clear(); timeStopLeft = 0; glitch = 0; tintFlash = 0; fovPunch = 0; vigFlash = 0; desatFlash = 0;
            return;
        }
        if (mc.isPaused()) return;
        Engine.clientTick();
        RIBBONS.values().removeIf(RibbonEffect::isDone);
        LocalPlayer p = mc.player;
        float target = 0f;
        if (p != null && p.isUsingItem() && p.getUseItem().getItem() instanceof CosmicStaffItem) {
            target = Math.min(p.getTicksUsingItem(), CosmicStaffItem.MAX_CHARGE) / (float) CosmicStaffItem.MAX_CHARGE;
        }
        charge += (target - charge) * 0.2f;
        bars += ((target > 0.05f ? 1f : 0f) - bars) * 0.12f;
        if (shakeLeft > 0) shakeLeft--;
        flashA = Math.max(0f, flashA - flashDecay);

        warpAmt = warpAmt < 0.002f ? 0f : warpAmt * 0.92f;
        glitch = glitch < 0.01f ? 0f : glitch * 0.85f;
        tintFlash = tintFlash < 0.01f ? 0f : tintFlash * 0.9f;
        vigFlash = vigFlash < 0.01f ? 0f : vigFlash * 0.92f;
        desatFlash = desatFlash < 0.01f ? 0f : desatFlash * 0.92f;
        fovPunch = Math.abs(fovPunch) < 0.003f ? 0f : fovPunch * 0.85f;
        if (timeStopLeft > 0) timeStopLeft--;
        for (Bolt bo : bolts) bo.age++;
        bolts.removeIf(bo -> bo.age >= bo.life);
        for (Slash sl : slashes) sl.age++;
        slashes.removeIf(sl -> sl.age >= sl.life);
        tickEmitters(l);
        for (Ripple rp : ripples) rp.age++;
        ripples.removeIf(rp -> rp.age >= rp.life);
        for (Shock s : shocks) s.age++;
        shocks.removeIf(s -> s.age >= s.life);
        for (Beam b : beams) {
            b.age++;
            if (b.particles && b.age <= 8) emitBeam(l, b);
        }
        beams.removeIf(b -> b.age >= b.life);
    }

    private static void emitBeam(ClientLevel l, Beam bm) {
        Vec3 d = bm.b.subtract(bm.a);
        double len = d.length();
        if (len < 0.5) return;
        Vec3 dir = d.scale(1.0 / len);
        Vec3 helper = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(helper).normalize();
        Vec3 w = dir.cross(u).normalize();
        RandomSource r = l.random;
        int steps = (int) Math.min(140, len / 0.7);
        double stepLen = len / steps;
        for (int i = 0; i < steps; i++) {
            double t = i * stepLen;
            Vec3 p = bm.a.add(dir.scale(t));
            double ang = t * 0.8 + bm.age * 0.9;
            for (int k = 0; k < 2; k++) {
                double an = ang + k * Math.PI;
                Vec3 o = u.scale(Math.cos(an) * 0.7).add(w.scale(Math.sin(an) * 0.7));
                glow(l, p.x + o.x, p.y + o.y, p.z + o.z, 0, 0, 0, k == 0 ? CYAN : VIOLET, 0.3f, 7, 0f, 1f);
            }
            if (i % 2 == 0) {
                glow(l, p.x + r.nextGaussian() * 0.15, p.y + r.nextGaussian() * 0.15, p.z + r.nextGaussian() * 0.15,
                        r.nextGaussian() * 0.03, r.nextGaussian() * 0.03, r.nextGaussian() * 0.03, WHITE, 0.25f, 8, 0f, 1f);
            }
        }
    }

    // ---- camera ----
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        CinematicEngine.applyCamera(e, charge);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (!e.usedConfiguredFov()) return;
        double f = 1.0 - 0.22 * charge + 0.012 * curShake() + fovPunch + CinematicEngine.fovOffset();
        e.setFOV(e.getFOV() * f);
    }

    // ---- HUD: barras de cinema, vinheta, flash ----
    private static int argb(float a, float r, float g, float b) {
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | (Mth.clamp((int) (r * 255), 0, 255) << 16)
                | (Mth.clamp((int) (g * 255), 0, 255) << 8) | Mth.clamp((int) (b * 255), 0, 255);
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post e) {
        GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        if (charge > 0.02f) {
            float a = 0.45f * charge;
            g.fillGradient(0, 0, w, h / 4, argb(a, chargeCol[0], chargeCol[1], chargeCol[2]), argb(0f, chargeCol[0], chargeCol[1], chargeCol[2]));
            g.fillGradient(0, h * 3 / 4, w, h, argb(0f, chargeCol[0], chargeCol[1], chargeCol[2]), argb(a, chargeCol[0], chargeCol[1], chargeCol[2]));
        }
        float barsNow = Math.max(bars, CinematicEngine.bars());
        if (barsNow > 0.01f) {
            int bh = (int) (h * 0.09f * barsNow);
            g.fill(0, 0, w, bh, 0xFF000000);
            g.fill(0, h - bh, w, h, 0xFF000000);
        }
        if (flashA > 0.01f) {
            g.fill(0, 0, w, h, argb(flashA, flashR, flashG, flashB));
        }
        float fadeA = CinematicEngine.fadeAlpha();
        if (fadeA > 0.01f) {
            float[] fc = CinematicEngine.fadeColor();
            g.fill(0, 0, w, h, argb(fadeA, fc[0], fc[1], fc[2]));
        }
    }

    // ---- mundo: ondas de choque e raios ----
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (shocks.isEmpty() && beams.isEmpty() && bolts.isEmpty() && slashes.isEmpty() && !VFXEngine.active()) return;
        Minecraft mc = Minecraft.getInstance();
        PoseStack ps = e.getPoseStack();
        Vec3 cam = e.getCamera().getPosition();
        float pt = e.getPartialTick();
        MultiBufferSource.BufferSource src = mc.renderBuffers().bufferSource();
        RenderType rt = RenderType.eyes(SOLID);
        VertexConsumer vc = src.getBuffer(rt);

        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);
        for (Shock s : shocks) {
            float t = Mth.clamp((s.age + pt) / s.life, 0f, 1f);
            float ease = 1f - (1f - t) * (1f - t) * (1f - t);
            float R = Math.max(0.05f, s.maxR * ease);
            float fade = (float) Math.pow(1f - t, 1.4) * s.alpha;
            if (fade <= 0.02f) continue;
            if (!com.astralarcana.engine.perf.PerformanceEngine.visible(e.getFrustum(), new net.minecraft.world.phys.AABB(s.pos, s.pos).inflate(R + 2))) continue;
            double sdist = cam.distanceTo(s.pos);
            int st = com.astralarcana.engine.perf.PerformanceEngine.lod(12, sdist, R);
            int sl2 = st * 5 / 3;
            ps.pushPose();
            ps.translate(s.pos.x, s.pos.y, s.pos.z);
            if (s.sphere) {
                FxRender.sphere(ps, vc, R, st, sl2, s.col[0], s.col[1], s.col[2], fade);
                FxRender.sphere(ps, vc, R * 0.8f, Math.max(4, st - 2), Math.max(6, sl2 - 4), 1f, 1f, 1f, fade * 0.5f);
            } else {
                FxRender.wall(ps, vc, R, 1.5f + R * 0.12f, 48, s.col[0], s.col[1], s.col[2], fade, 0f);
                FxRender.annulus(ps, vc, Math.max(0f, R - 2.5f), R, 48, s.col[0], s.col[1], s.col[2], 0f, fade * 0.6f);
            }
            ps.popPose();
        }
        for (Beam b : beams) {
            float t = (b.age + pt) / b.life;
            float k = 1f - t * t;
            if (k <= 0.02f) continue;
            float pulse = (1f + 0.15f * (float) Math.sin((b.age + pt) * 2.5f)) * (0.6f + 0.6f * b.power);
            float th = b.thick;
            FxRender.tube(ps, vc, b.a, b.b, 0.14f * k * pulse * th, 8, 1f, 1f, 1f, 0.95f * k);
            FxRender.tube(ps, vc, b.a, b.b, 0.42f * k * pulse * th, 10, b.mid[0], b.mid[1], b.mid[2], 0.45f * k);
            FxRender.tube(ps, vc, b.a, b.b, 1.0f * k * pulse * th, 12, b.outer[0], b.outer[1], b.outer[2], 0.18f * k);
        }
        for (Bolt bo : bolts) {
            float k = 1f - (bo.age + pt) / bo.life;
            if (k <= 0.02f) continue;
            for (int i = 0; i < bo.pts.length - 1; i++) {
                FxRender.tube(ps, vc, bo.pts[i], bo.pts[i + 1], 0.07f * k, 5, 1f, 1f, 1f, 0.95f * k);
                FxRender.tube(ps, vc, bo.pts[i], bo.pts[i + 1], 0.22f * k, 6, ELEC[0], ELEC[1], ELEC[2], 0.4f * k);
            }
        }
        for (Slash sl : slashes) {
            float t = (sl.age + pt) / sl.life;
            float k = 1f - t * t;
            if (k <= 0.02f) continue;
            float ease = 1f - (1f - t) * (1f - t);
            float R = 7f * (0.7f + 0.5f * ease) * (0.8f + 0.4f * sl.power);
            int seg = 24;
            Vec3 prev = slashPoint(sl, -1f, R);
            for (int i = 1; i <= seg; i++) {
                float u0 = -1f + 2f * (i - 1) / seg, u1 = -1f + 2f * i / seg;
                Vec3 cur = slashPoint(sl, u1, R);
                float r0 = 0.5f * (float) Math.pow(Math.max(0f, 1f - Math.abs(u0)), 0.7);
                float r1 = 0.5f * (float) Math.pow(Math.max(0f, 1f - Math.abs(u1)), 0.7);
                FxRender.cone(ps, vc, prev, cur, r0 * k, r1 * k, 6, 1f, 1f, 1f, 0.95f * k, false);
                FxRender.cone(ps, vc, prev, cur, r0 * 2.6f * k, r1 * 2.6f * k, 6, PINK[0], PINK[1], PINK[2], 0.3f * k, false);
                prev = cur;
            }
        }
        VFXEngine.render(ps, vc, cam, pt, e.getFrustum());
        ps.popPose();
        src.endBatch(rt);
    }

    // ===================== DISTORCAO DE TELA (shader) =====================
    public static ShaderInstance distortShader;
    private static RenderTarget copyTarget;
    private static float warpAmt;
    private static final List<Ripple> ripples = new ArrayList<>();

    private static final class Ripple {
        final Vec3 pos; final float strength; final int life;
        int age;
        Ripple(Vec3 pos, float strength, int life) { this.pos = pos; this.strength = strength; this.life = life; }
    }

    private static void ripple(Vec3 p, float strength, int life) {
        if (strength <= 0.001f) return;
        if (ripples.size() >= 6) ripples.remove(0);
        ripples.add(new Ripple(p, strength, life));
    }

    private static void warp(float a) {
        warpAmt = Math.max(warpAmt, a);
    }

    /** Projeta um ponto do mundo para a tela. Retorna {u, v, w} ou null se estiver atras da camera. */
    private static float[] project(Vec3 p, Vec3 cam, Matrix4f pv) {
        Vector4f v = new Vector4f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z), 1f);
        pv.transform(v);
        if (v.w < 0.3f) return null;
        return new float[]{v.x / v.w * 0.5f + 0.5f, v.y / v.w * 0.5f + 0.5f, v.w};
    }

    @SubscribeEvent
    public static void onRenderLevelEnd(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (!ShaderEngine.anyReady()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        float pt = e.getPartialTick();
        Vec3 cam = e.getCamera().getPosition();
        Matrix4f proj = e.getProjectionMatrix();
        Matrix4f pv = new Matrix4f(proj).mul(e.getPoseStack().last().pose());

        // --- lente gravitacional: buraco negro mais proximo ---
        float lensX = 0.5f, lensY = 0.5f, lensS = 0f, lensR = 0.2f, horizon = 0f;
        BlackHoleEntity best = null;
        double bestD = 80.0 * 80.0;
        for (Entity en : mc.level.entitiesForRendering()) {
            if (en instanceof BlackHoleEntity bh) {
                double d = bh.position().distanceToSqr(cam);
                if (d < bestD) { bestD = d; best = bh; }
            }
        }
        if (best != null) {
            float age = best.tickCount + pt;
            float grow = Mth.clamp(age / 20f, 0f, 1f);
            float fade = Mth.clamp((best.getMaxLife() - age) / 20f, 0f, 1f);
            float k = Mth.sin(grow * fade * Mth.HALF_PI);
            Vec3 c = best.getPosition(pt).add(0, best.getBbHeight() / 2.0, 0);
            float[] sp = project(c, cam, pv);
            if (sp != null && k > 0.02f) {
                horizon = Math.min(0.6f, 0.5f * proj.m11() * (1.7f * k) / sp[2]);
                lensX = sp[0];
                lensY = sp[1];
                lensR = Math.max(0.02f, Math.min(1.5f, horizon * 5f));
                lensS = 0.55f * k;
            }
        }

        // --- onda de choque: a mais forte no momento ---
        float waveX = 0.5f, waveY = 0.5f, waveR = 0f, waveW = 0.06f, waveStr = 0f;
        float bestV = 0f;
        for (Ripple rp : ripples) {
            float t = Mth.clamp((rp.age + pt) / rp.life, 0f, 1f);
            float val = rp.strength * (float) Math.pow(1f - t, 1.5);
            if (val > bestV) {
                bestV = val;
                float[] sp = project(rp.pos, cam, pv);
                waveX = sp == null ? 0.5f : sp[0];
                waveY = sp == null ? 0.5f : sp[1];
                waveR = t * 1.1f;
                waveW = 0.05f + 0.05f * t;
                waveStr = val;
            }
        }

        float blur = warpAmt * 0.9f + charge * 0.05f;
        float chroma = warpAmt * 0.012f + charge * 0.003f + curShake() * 0.0012f + glitch * 0.01f;
        // --- redemoinho (tornado) ---
        float swX = 0.5f, swY = 0.5f, swS = 0f, swR = 0.3f;
        SpellZoneEntity tor = null;
        double td = 60.0 * 60.0;
        for (Entity en : mc.level.entitiesForRendering()) {
            if (en instanceof SpellZoneEntity z && z.getKind() == SpellZoneEntity.TORNADO) {
                double d = z.position().distanceToSqr(cam);
                if (d < td) { td = d; tor = z; }
            }
        }
        if (tor != null) {
            float age = tor.tickCount + pt;
            float ramp = Mth.clamp(age / 15f, 0f, 1f) * Mth.clamp((tor.getMaxLife() - age) / 15f, 0f, 1f);
            float[] sp = project(tor.getPosition(pt).add(0, 6, 0), cam, pv);
            if (sp != null && ramp > 0.02f) {
                swX = sp[0];
                swY = sp[1];
                swR = Math.min(1.2f, 0.5f * proj.m11() * 10f / sp[2]);
                swS = 3.0f * ramp * Math.min(1f, swR * 2f);
            }
        }

        // --- color grading: parar o tempo e flashes de cor ---
        float gDesat = 0f, gInv = 0f, gVig = 0f;
        float tR = 1f, tG = 1f, tB = 1f, tA = 0f;
        if (timeStopLeft > 0) {
            float el = timeStopTotal - timeStopLeft + pt;
            float k = Math.min(Mth.clamp(el / 8f, 0f, 1f), Mth.clamp((timeStopLeft - pt) / 10f, 0f, 1f));
            gDesat = 0.9f * k;
            gVig = 0.55f * k;
            gInv = el < 5f ? 1f - el / 5f : 0f;
            tR = 0.6f; tG = 0.9f; tB = 1f; tA = 0.3f * k;
        }
        gVig = Math.max(gVig, vigFlash);
        gDesat = Math.max(gDesat, desatFlash);
        if (tintFlash > tA) {
            tR = tintCol[0]; tG = tintCol[1]; tB = tintCol[2]; tA = tintFlash;
        }

        if (lensS <= 0f && waveStr <= 0f && blur < 0.004f && chroma < 0.0005f
                && swS <= 0f && gDesat <= 0f && gInv <= 0f && gVig <= 0f && glitch < 0.01f && tA < 0.01f
                && shocks.isEmpty() && beams.isEmpty() && bolts.isEmpty() && slashes.isEmpty()
                && !WorldVisualEngine.active() && !VFXEngine.active()) return;

        PostParams pp = PostPipeline.params();
        pp.lensX = lensX; pp.lensY = lensY; pp.lensS = lensS; pp.lensR = lensR; pp.horizon = horizon;
        pp.waveX = waveX; pp.waveY = waveY; pp.waveR = waveR; pp.waveW = waveW; pp.waveStr = waveStr;
        pp.blurX = lensS > 0f ? lensX : 0.5f; pp.blurY = lensS > 0f ? lensY : 0.5f; pp.blur = blur;
        pp.chroma = chroma;
        pp.swX = swX; pp.swY = swY; pp.swS = swS; pp.swR = swR;
        pp.gDesat = gDesat; pp.gInv = gInv; pp.gVig = gVig; pp.glitch = glitch;
        pp.tR = tR; pp.tG = tG; pp.tB = tB; pp.tA = tA;
        pp.motionBlur = Mth.clamp(warpAmt * 1.2f + Math.abs(fovPunch) * 1.5f + charge * 0.3f, 0f, 1f);
        pp.bloom = Mth.clamp(0.3f + warpAmt * 0.8f + flashA * 0.6f + Math.min(1f, beams.size() * 0.3f)
                + (shocks.isEmpty() ? 0f : 0.25f) + WorldVisualEngine.glow(), 0f, 1.5f);
        PostPipeline.run(e, pp);
    }

    // ===================== SERPENTE COSMICA =====================
    public static void serpentTick(CosmicSerpentEntity e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || mc.isPaused()) return;
        RandomSource r = l.random;
        int n = CosmicSerpentEntity.SEG_COUNT;

        // poeira estelar ao longo do corpo
        for (int k = 0; k < 14; k++) {
            int i = r.nextInt(n);
            Vec3 p = e.segs[i];
            Vec3 o = randomUnit(r).scale(CosmicSerpentEntity.radius(i) * (1.1 + r.nextDouble() * 0.8));
            glow(l, p.x + o.x, p.y + o.y, p.z + o.z, r.nextGaussian() * 0.02, r.nextGaussian() * 0.02 + 0.01, r.nextGaussian() * 0.02,
                    pick(r, CYAN, VIOLET, WHITE), 0.18f + r.nextFloat() * 0.2f, 20 + r.nextInt(20), 0f, 0.97f);
        }

        Vec3 head = e.segs[0];
        Vec3 f = e.getForward();
        int phase = e.getPhase();
        RIBBONS.computeIfAbsent(e.getId(), id -> {
            RibbonEffect rb = new RibbonEffect(() -> e.isRemoved() ? null : e.segs[0].add(e.getForward().scale(-1)), 40, 1.8f, CYAN, VIOLET);
            VFXEngine.add(rb);
            return rb;
        });
        RIBBONS.computeIfAbsent(-e.getId() - 1, id -> {
            RibbonEffect rb = new RibbonEffect(() -> e.isRemoved() ? null : e.segs[CosmicSerpentEntity.SEG_COUNT - 1], 30, 0.9f, VIOLET, CYAN);
            VFXEngine.add(rb);
            return rb;
        });
        WorldVisualEngine.keepLight("s" + e.getId(), head, CYAN, 22f, 1.0f);
        int t = e.tickCount - e.phaseStart;
        if (phase == CosmicSerpentEntity.BREATH && t < 36) {
            float ratio = t / 35f;
            Vec3 tip = head.add(f.scale(3.2));
            for (int i = 0; i < 6; i++) {
                Vec3 d = randomUnit(r);
                double R = (5.0 - 3.5 * ratio) * (0.6 + r.nextDouble() * 0.6);
                Vec3 p = tip.add(d.scale(R));
                Vec3 v = tip.subtract(p).scale(1.0 / 9.0);
                glow(l, p.x, p.y, p.z, v.x, v.y, v.z, pick(r, CYAN, WHITE, VIOLET), 0.35f + 0.3f * ratio, 9, 0f, 1f);
            }
        }
        if (phase == CosmicSerpentEntity.DIVE) {
            for (int i = 0; i < 8; i++) {
                Vec3 o = randomUnit(r).scale(1.2);
                glow(l, head.x + o.x, head.y + o.y, head.z + o.z, -f.x * 0.3, -f.y * 0.3, -f.z * 0.3,
                        pick(r, CYAN, WHITE), 0.6f, 18, 0f, 0.95f);
            }
        }
    }

    public static void apiFlash(float[] c, float a, float decay) { flash(c, a, decay); }
    public static void apiTint(float[] c, float a) { flashTint(c, a); }
    public static void apiRipple(Vec3 p, float strength, int life) { ripple(p, strength, life); }
    public static void apiWarp(float a) { warp(a); }
    public static void apiGlitch(float g) { glitch = Math.max(glitch, g); }
    public static void apiVignette(float v) { vigFlash = Math.max(vigFlash, v); }
    public static void apiDesat(float d) { desatFlash = Math.max(desatFlash, d); }
    public static void apiFov(float f) { fovPunch = Math.max(fovPunch, f); }

    private static final java.util.Map<Integer, RibbonEffect> RIBBONS = new java.util.HashMap<>();

    public static void serpentRoar(CosmicSerpentEntity e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Vec3 head = e.segs[0];
        float near = (float) Mth.clamp(1.0 - mc.player.position().distanceTo(head) / 90.0, 0.0, 1.0);
        CinematicEngine.play(Sequences.ROAR, head, near);
    }

    // ===================== PODERES 5 a 11 =====================
    private static int timeStopLeft, timeStopTotal;
    private static float glitch, tintFlash, fovPunch, vigFlash, desatFlash;
    private static float[] tintCol = WHITE;
    private static final List<Bolt> bolts = new ArrayList<>();
    private static final List<Slash> slashes = new ArrayList<>();
    private static final List<Emitter> emitters = new ArrayList<>();
    private static final int E_CLOCK = 0, E_REBIRTH = 1;

    private static final class Bolt {
        final Vec3[] pts; final int life = 9;
        int age;
        Bolt(Vec3[] pts) { this.pts = pts; }
    }

    private static final class Slash {
        final Vec3 pos, dir; final float roll, power; final int life = 12;
        int age;
        Slash(Vec3 pos, Vec3 dir, float roll, float power) { this.pos = pos; this.dir = dir; this.roll = roll; this.power = power; }
    }

    private static final class Emitter {
        final Vec3 pos; final int kind, life;
        int age;
        Emitter(Vec3 pos, int kind, int life) { this.pos = pos; this.kind = kind; this.life = life; }
    }

    private static void flashTint(float[] c, float a) {
        if (a > tintFlash) { tintFlash = a; tintCol = c; }
    }

    private static void orbitRise(ClientLevel l, Vec3 c, double ang, double radius, double angSpeed, double rise,
                                  float[] col, float size, int life) {
        ParticleSystem.orbit(l, c, ang, radius, angSpeed, 0.0, 0.0, rise, col, size, life);
    }

    private static void tickEmitters(ClientLevel l) {
        RandomSource r = l.random;
        for (Emitter em : emitters) {
            em.age++;
            Vec3 c = em.pos;
            if (em.kind == E_CLOCK) {
                for (int k = 0; k < 6; k++) orbit(l, c, r.nextDouble() * Math.PI * 2, 6.0, 0.08, 0, 0, SILVER, 0.3f, 8);
                for (int k = 0; k < 4; k++) orbit(l, c, r.nextDouble() * Math.PI * 2, 9.0, -0.05, 0, 0, CYAN, 0.3f, 8);
                double a = em.age * 0.15, a2 = em.age * 0.0125;
                for (double rr = 2; rr < 9; rr += 0.5) glow(l, c.x + Math.cos(a) * rr, c.y, c.z + Math.sin(a) * rr, 0, 0, 0, WHITE, 0.35f, 3, 0f, 1f);
                for (double rr = 2; rr < 6; rr += 0.5) glow(l, c.x + Math.cos(a2) * rr, c.y, c.z + Math.sin(a2) * rr, 0, 0, 0, SILVER, 0.45f, 3, 0f, 1f);
            } else {
                for (int k = 0; k < 4; k++) orbitRise(l, c, r.nextDouble() * Math.PI * 2, 1.4, 0.3, 0.22, pick(r, YELLOW, LIME, WHITE), 0.35f, 20);
                glow(l, c.x + r.nextGaussian() * 0.3, c.y, c.z + r.nextGaussian() * 0.3, 0, 0.3 + r.nextDouble() * 0.4, 0,
                        pick(r, YELLOW, WHITE), 0.4f, 25, 0f, 0.98f);
            }
        }
        emitters.removeIf(em -> em.age >= em.life);
    }

    private static void timeStop(ClientLevel l, Vec3 c, int dur, float near) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.position().distanceTo(c) < 40) {
            timeStopLeft = timeStopTotal = Math.max(20, dur);
        }
        shake(1.2f, 20, near);
        ripple(c, 0.07f * near, 40);
        glitch = Math.max(glitch, 0.6f * near);
        sphere(c, 24f, 16, SILVER, 0.7f);
        sphere(c, 34f, 26, CYAN, 0.4f);
        ring(c.add(0, -0.9, 0), 30f, 28, SILVER);
        emitters.add(new Emitter(c, E_CLOCK, 60));
        CinematicEngine.play(Sequences.TIMESTOP, c, near);
        ringBurst(l, c.add(0, -0.5, 0), 120, 1.6, SILVER, 0.5f, 30);
        dome(l, c, 150, 0.3, 1.5, 0.4f, 40, 0f, SILVER, CYAN, WHITE);
    }

    private static void blink(ClientLevel l, Vec3 a, Vec3 b, float near) {
        RandomSource r = l.random;
        fovPunch = Math.max(fovPunch, 0.35f);
        warp(0.55f * Math.max(near, 0.3f));
        glitch = Math.max(glitch, 0.3f * near);
        flashTint(VIOLET, 0.35f * near);
        sphere(a, 3.5f, 8, VIOLET, 0.9f);
        sphere(b, 3.5f, 10, WHITE, 0.9f);
        Vec3 travel = b.subtract(a);
        VFXEngine.add(new PortalEffect(a, travel, 2.2f, 26, VIOLET, CYAN));
        VFXEngine.add(new PortalEffect(b, travel, 2.2f, 26, CYAN, VIOLET));
        WorldVisualEngine.addLight(a, VIOLET, 14f, 1.6f, 14);
        WorldVisualEngine.addLight(b, CYAN, 14f, 1.6f, 14);
        ripple(b, 0.05f * near, 26);
        beams.add(new Beam(a, b, 0.2f, 0.5f, 10, VIOLET, CYAN, false));
        Vec3 d = b.subtract(a);
        int n = (int) Math.min(80, d.length() * 1.5);
        for (int i = 0; i < n; i++) {
            Vec3 p = a.add(d.scale(r.nextDouble()));
            glow(l, p.x, p.y, p.z, r.nextGaussian() * 0.08, r.nextGaussian() * 0.08, r.nextGaussian() * 0.08, pick(r, VIOLET, WHITE, CYAN), 0.4f, 16, 0f, 0.95f);
        }
        dome(l, a, 60, 0.2, 1.2, 0.4f, 24, 0f, VIOLET, WHITE);
        dome(l, b, 60, 0.2, 1.2, 0.4f, 24, 0f, VIOLET, WHITE);
    }

    private static void orbitalHit(ClientLevel l, Vec3 start, Vec3 end, float scale, float near) {
        beams.add(new Beam(start, end, 1.0f, 3.2f * scale, 14, YELLOW, ORANGE, false));
        WorldVisualEngine.addLight(end.add(0, 1, 0), YELLOW, 34f * scale, 2.6f, 22);
        WorldVisualEngine.addShock3D(end, 36f * scale, 16, 2.4f, YELLOW, 1.6f);
        WorldVisualEngine.addOrb(end.add(0, 3, 0), 6f * scale, YELLOW, 1.0f, 1.0f, 0, 1.0f, 22);
        VFXEngine.add(new GroundCrackEffect(l, end, 9f * scale, 50));
        sphere(end, 8f * scale, 10, WHITE, 0.9f);
        ring(end.add(0, 0.2, 0), 18f * scale, 18, YELLOW);
        dome(l, end, (int) (90 * scale), 0.3, 1.6 * scale, 0.5f, 40, 0.2f, YELLOW, WHITE, ORANGE);
        shake(2.0f * scale, 30, near);
        flash(YELLOW, 0.5f * near, 0.05f);
        ripple(end, 0.05f * scale * near, 24);
        warp(0.15f * near);
        glitch = Math.max(glitch, 0.25f * near);
    }

    private static void chain(ClientLevel l, Vec3 a, Vec3 b, float near) {
        RandomSource r = l.random;
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.5) return;
        Vec3 dir = d.scale(1.0 / len);
        Vec3 helper = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(helper).normalize();
        Vec3 w = dir.cross(u).normalize();
        int n = Math.max(3, (int) (len / 1.2));
        Vec3[] pts = new Vec3[n + 1];
        for (int i = 0; i <= n; i++) {
            double amp = (i == 0 || i == n) ? 0 : 0.7;
            pts[i] = a.add(dir.scale(len * i / n)).add(u.scale(r.nextGaussian() * amp)).add(w.scale(r.nextGaussian() * amp));
        }
        bolts.add(new Bolt(pts));
        float nn = Math.max(near, 0.3f);
        flash(ELEC, 0.3f * nn, 0.08f);
        glitch = Math.max(glitch, 0.55f * nn);
        shake(0.6f, 8, near);
        sphere(b, 2.2f, 6, ELEC, 0.9f);
        dome(l, b, 40, 0.2, 1.2, 0.35f, 18, 0f, ELEC, WHITE);
        for (Vec3 p : pts) {
            glow(l, p.x, p.y, p.z, r.nextGaussian() * 0.05, r.nextGaussian() * 0.05, r.nextGaussian() * 0.05, ELEC, 0.3f, 8, 0f, 0.95f);
        }
    }

    private static Vec3 slashPoint(Slash s, float u, float R) {
        Vec3 look = s.dir;
        Vec3 helper = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = look.cross(helper).normalize();
        Vec3 up = right.cross(look).normalize();
        double c = Math.cos(s.roll), sn = Math.sin(s.roll);
        Vec3 U = right.scale(c).add(up.scale(sn));
        Vec3 V = up.scale(c).subtract(right.scale(sn));
        double x = u * R;
        double y = 0.25 * R * (1 - u * u) - 0.1 * R;
        return s.pos.add(look.scale(1.5)).add(U.scale(x)).add(V.scale(y));
    }

    private static void slash(ClientLevel l, Vec3 pos, Vec3 dir, float power, float near) {
        RandomSource r = l.random;
        Vec3 d = dir.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : dir.normalize();
        Slash s = new Slash(pos, d, (float) (r.nextDouble() * Math.PI), power);
        slashes.add(s);
        float nn = Math.max(near, 0.3f);
        shake(1.4f, 14, near);
        ripple(pos.add(d.scale(2)), 0.05f * near, 20);
        glitch = Math.max(glitch, 0.7f * nn);
        flash(PINK, 0.25f * nn, 0.07f);
        for (int i = 0; i < 50; i++) {
            Vec3 p = slashPoint(s, (float) (r.nextDouble() * 2 - 1), 8f);
            glow(l, p.x, p.y, p.z, d.x * 0.6 + r.nextGaussian() * 0.1, d.y * 0.6 + r.nextGaussian() * 0.1, d.z * 0.6 + r.nextGaussian() * 0.1,
                    pick(r, PINK, WHITE), 0.45f, 18, 0f, 0.95f);
        }
    }

    private static void rebirth(ClientLevel l, Vec3 c, float near) {
        flash(YELLOW, 0.35f * near, 0.04f);
        flashTint(YELLOW, 0.35f * near);
        shake(0.4f, 12, near);
        sphere(c.add(0, 1, 0), 5f, 14, LIME, 0.6f);
        ring(c.add(0, 0.1, 0), 10f, 20, YELLOW);
        emitters.add(new Emitter(c.add(0, 0.1, 0), E_REBIRTH, 40));
        dome(l, c.add(0, 1, 0), 80, 0.1, 0.9, 0.4f, 40, -0.05f, YELLOW, LIME, WHITE);
    }

    private static void tornadoBirth(ClientLevel l, Vec3 c, float near) {
        ripple(c.add(0, 4, 0), 0.04f * near, 22);
        shake(0.6f, 14, near);
        ring(c.add(0, 0.2, 0), 12f, 18, SILVER);
        dome(l, c.add(0, 0.5, 0), 80, 0.2, 1.0, 0.5f, 30, 0.05f, SILVER, WHITE, CYAN);
    }

    /** Chamado a cada tick de cliente pela zona magica (tornado ou ataque orbital). */
    public static void zoneTick(SpellZoneEntity e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || mc.isPaused()) return;
        RandomSource r = l.random;
        Vec3 c = e.position();
        if (e.getKind() == SpellZoneEntity.TORNADO) {
            float age = e.tickCount;
            float ramp = Mth.clamp(age / 15f, 0f, 1f) * Mth.clamp((e.getMaxLife() - age) / 15f, 0f, 1f);
            for (int h = 0; h < 18; h++) {
                if (r.nextFloat() > ramp) continue;
                double rad = 1.0 + h * 0.42;
                for (int k = 0; k < 2; k++) {
                    orbit(l, c.add(0, h, 0), r.nextDouble() * Math.PI * 2, rad * (0.8 + r.nextDouble() * 0.4),
                            0.38 - h * 0.008, 0, 0, pick(r, WHITE, SILVER, CYAN), 0.3f + r.nextFloat() * 0.25f, 5);
                }
            }
            for (int k = 0; k < 4; k++) {
                if (r.nextFloat() > ramp) continue;
                orbit(l, c.add(0, 0.3, 0), r.nextDouble() * Math.PI * 2, 5 + r.nextDouble() * 3, 0.3, 0, 0, SILVER, 0.5f, 8);
            }
            LocalPlayerHolder.shakeNear(c, 25, 0.4f * ramp);
        } else if (e.getKind() == SpellZoneEntity.ORBITAL) {
            int t = e.tickCount;
            float prog = Mth.clamp(t / 50f, 0f, 1f);
            Vec3 g = c.add(0, 0.12, 0);
            for (int k = 0; k < 3; k++) orbit(l, g, r.nextDouble() * Math.PI * 2, 8.0, 0.10, 0, 0, YELLOW, 0.4f, 8);
            for (int k = 0; k < 2; k++) orbit(l, g, r.nextDouble() * Math.PI * 2, 5.0, -0.16, 0, 0, WHITE, 0.35f, 8);
            for (int k = 0; k < 2; k++) orbit(l, g, r.nextDouble() * Math.PI * 2, 11.0, 0.05, 0, 0, ORANGE, 0.35f, 8);
            for (int i = 0; i < 7; i++) {
                int fireAt = i == 0 ? 64 : 50 + (i - 1) * 2;
                if (t > fireAt) continue;
                Vec3 p = e.pillarPos(i);
                glow(l, p.x, p.y + 0.2, p.z, 0, 0.3 + r.nextDouble() * 0.4, 0, YELLOW, 0.35f + 0.3f * prog, 25, 0f, 1f);
            }
            LocalPlayerHolder.shakeNear(c, 40, 0.6f * prog);
        } else {
            zoneTick2(e, l, r, c);
        }
    }

    /** Pequeno auxiliar: tremor leve quando o jogador local esta perto. */
    private static final class LocalPlayerHolder {
        static void shakeNear(Vec3 c, double range, float amp) {
            LocalPlayer p = Minecraft.getInstance().player;
            if (p == null || amp <= 0.01f) return;
            double d = p.position().distanceTo(c);
            if (d < range) shake(amp, 4, (float) (1 - d / range));
        }
    }

    // ===================== PODERES 12 a 31 =====================
    private static void preset(ClientLevel l, Vec3 c, Vec3 b, float power, float near) {
        RandomSource r = l.random;
        int id = (int) b.x;
        switch (id) {
            case 0 -> { // colapso gravitacional
                ripple(c, 0.07f * near, 30);
                warp(0.35f * near);
                glitch = Math.max(glitch, 0.4f * near);
                shake(1.5f, 25, near);
                flashTint(VIOLET, 0.3f * near);
                sphere(c, 12f, 14, VIOLET, 0.5f);
                for (int i = 0; i < 200; i++) {
                    Vec3 d = randomUnit(r);
                    Vec3 p = c.add(d.scale(10));
                    glow(l, p.x, p.y, p.z, -d.x * 0.6, -d.y * 0.6, -d.z * 0.6, pick(r, VIOLET, WHITE), 0.4f, 16, 0f, 1f);
                }
            }
            case 1 -> { // nova de gelo
                flashTint(SILVER, 0.4f * near);
                vigFlash = Math.max(vigFlash, 0.5f * near);
                desatFlash = Math.max(desatFlash, 0.4f * near);
                shake(0.8f, 14, near);
                sphere(c, 14f, 16, CYAN, 0.6f);
                ring(c.add(0, -0.9, 0), 20f, 24, CYAN);
                dome(l, c, 200, 0.2, 1.2, 0.35f, 50, 0.05f, WHITE, CYAN, SILVER);
            }
            case 2 -> { // nova solar
                flash(ORANGE, 0.6f * near, 0.05f);
                flashTint(ORANGE, 0.4f * near);
                warp(0.3f * near);
                ripple(c, 0.08f * near, 40);
                shake(1.5f, 26, near);
                sphere(c, 16f, 14, YELLOW, 0.8f);
                sphere(c, 22f, 22, ORANGE, 0.5f);
                ring(c.add(0, -0.9, 0), 24f, 22, ORANGE);
                dome(l, c, 250, 0.4, 2.0, 0.6f, 50, -0.05f, ORANGE, YELLOW, RED);
            }
            case 3 -> { // grito sonico (b.y = yaw, b.z = pitch)
                Vec3 dir = Vec3.directionFromRotation((float) b.z, (float) b.y);
                ripple(c.add(dir.scale(2)), 0.06f * near, 20);
                ripple(c.add(dir.scale(7)), 0.05f * near, 26);
                ripple(c.add(dir.scale(12)), 0.04f * near, 32);
                shake(1.0f, 16, near);
                glitch = Math.max(glitch, 0.5f * near);
                warp(0.15f * near);
                for (int i = 0; i < 150; i++) {
                    Vec3 d = dir.add(randomUnit(r).scale(0.35)).normalize();
                    double sp = 0.5 + r.nextDouble() * 1.8;
                    glow(l, c.x + dir.x * 1.5, c.y + dir.y * 1.5, c.z + dir.z * 1.5, d.x * sp, d.y * sp, d.z * sp,
                            pick(r, WHITE, CYAN, SILVER), 0.45f, 20, 0f, 0.95f);
                }
            }
            case 4 -> { // impulso estelar
                fovPunch = Math.max(fovPunch, 0.3f);
                warp(0.4f * near);
                flashTint(TEAL, 0.25f * near);
                shake(0.4f, 10, near);
                dome(l, c, 80, 0.2, 1.0, 0.35f, 24, 0f, TEAL, WHITE);
            }
            case 5 -> { // visao estelar
                flashTint(CYAN, 0.35f * near);
                vigFlash = Math.max(vigFlash, 0.45f * near);
                ripple(c, 0.06f * near, 50);
                sphere(c, 40f, 30, CYAN, 0.25f);
                ringBurst(l, c, 160, 2.0, CYAN, 0.5f, 26);
            }
            case 6 -> { // furia estelar
                flashTint(RED, 0.35f * near);
                vigFlash = Math.max(vigFlash, 0.6f * near);
                shake(1.0f, 16, near);
                glitch = Math.max(glitch, 0.25f * near);
                sphere(c, 8f, 14, ORANGE, 0.6f);
                dome(l, c, 120, 0.3, 1.4, 0.5f, 36, -0.04f, RED, ORANGE, YELLOW);
            }
            case 7 -> { // veu estelar
                desatFlash = Math.max(desatFlash, 0.6f * near);
                flashTint(VIOLET, 0.3f * near);
                glitch = Math.max(glitch, 0.5f * near);
                ring(c.add(0, -0.9, 0), 10f, 20, VIOLET);
                for (int i = 0; i < 120; i++) {
                    Vec3 d = randomUnit(r);
                    Vec3 p = c.add(d.scale(4));
                    glow(l, p.x, p.y, p.z, -d.x * 0.25, -d.y * 0.25, -d.z * 0.25, pick(r, VIOLET, SILVER), 0.4f, 16, 0f, 1f);
                }
            }
            default -> { // nascimento de zona
                ripple(c.add(0, 1, 0), 0.04f * near, 24);
                shake(0.5f, 10, near);
                ring(c.add(0, -0.9, 0), 10f, 18, SILVER);
                dome(l, c, 60, 0.2, 1.0, 0.4f, 24, 0f, WHITE, CYAN);
            }
        }
    }

    private static void link(ClientLevel l, Vec3 a, Vec3 b, float near) {
        RandomSource r = l.random;
        beams.add(new Beam(a, b, 0.2f, 0.4f, 12, CYAN, TEAL, false));
        glitch = Math.max(glitch, 0.2f * Math.max(near, 0.3f));
        shake(0.3f, 6, near);
        sphere(b, 2f, 6, TEAL, 0.8f);
        Vec3 d = b.subtract(a);
        for (int i = 0; i < 40; i++) {
            Vec3 p = a.add(d.scale(r.nextDouble()));
            glow(l, p.x, p.y, p.z, -d.x * 0.03, -d.y * 0.03, -d.z * 0.03, pick(r, TEAL, WHITE), 0.35f, 12, 0f, 1f);
        }
    }

    /** Visuais das zonas novas: barreira, laminas, tempestade, nevasca, terremoto, santuario. */
    private static void zoneTick2(SpellZoneEntity e, ClientLevel l, RandomSource r, Vec3 c) {
        int kind = e.getKind();
        int age = e.tickCount;
        float ramp = Mth.clamp(age / 10f, 0f, 1f) * Mth.clamp((e.getMaxLife() - age) / 10f, 0f, 1f);
        LocalPlayer lp = Minecraft.getInstance().player;
        double dist = lp == null ? 999 : lp.position().distanceTo(c);
        switch (kind) {
            case SpellZoneEntity.SHIELD -> {
                for (int i = 0; i < 6; i++) {
                    Vec3 p = c.add(0, 1, 0).add(randomUnit(r).scale(5.5));
                    glow(l, p.x, p.y, p.z, 0, 0.02, 0, pick(r, CYAN, WHITE), 0.3f, 12, 0f, 0.95f);
                }
                if (age % 20 == 0) ripple(c.add(0, 1, 0), 0.02f, 20);
            }
            case SpellZoneEntity.BLADES -> {
                double a = age * 0.35;
                for (int k = 0; k < 3; k++) {
                    double an = a + k * Math.PI * 2 / 3;
                    for (double rr = 1; rr < 8; rr += 0.8) {
                        glow(l, c.x + Math.cos(an) * rr, c.y + 1, c.z + Math.sin(an) * rr, 0, 0, 0, pick(r, PINK, WHITE), 0.5f, 3, 0f, 1f);
                    }
                    glow(l, c.x + Math.cos(an) * 8, c.y + 1, c.z + Math.sin(an) * 8, 0, 0, 0, WHITE, 1.1f, 3, 0f, 1f);
                }
                if (age % 12 == 0) {
                    ripple(c.add(0, 1, 0), 0.03f, 16);
                    glitch = Math.max(glitch, dist < 20 ? 0.25f : 0f);
                }
                LocalPlayerHolder.shakeNear(c, 20, 0.3f * ramp);
            }
            case SpellZoneEntity.STORM -> {
                for (int i = 0; i < 10; i++) {
                    double ang = r.nextDouble() * Math.PI * 2, rad = r.nextDouble() * 12;
                    glow(l, c.x + Math.cos(ang) * rad, c.y + 24 + r.nextGaussian() * 2, c.z + Math.sin(ang) * rad,
                            r.nextGaussian() * 0.02, 0, r.nextGaussian() * 0.02, pick(r, SILVER, ELEC), 1.5f, 30, 0f, 0.98f);
                }
                for (int i = 0; i < 3; i++) {
                    double ang = r.nextDouble() * Math.PI * 2, rad = r.nextDouble() * 12;
                    glow(l, c.x + Math.cos(ang) * rad, c.y + 0.3, c.z + Math.sin(ang) * rad, 0, 0.1, 0, ELEC, 0.25f, 8, 0f, 0.95f);
                }
                if (dist < 30) flashTint(SILVER, 0.12f * ramp);
            }
            case SpellZoneEntity.BLIZZARD -> {
                for (int i = 0; i < 24; i++) {
                    double ang = r.nextDouble() * Math.PI * 2, rad = r.nextDouble() * 14;
                    glow(l, c.x + Math.cos(ang) * rad, c.y + 4 + r.nextDouble() * 12, c.z + Math.sin(ang) * rad,
                            0.35 + r.nextGaussian() * 0.05, -0.25, r.nextGaussian() * 0.05, WHITE, 0.25f, 25, 0f, 0.99f);
                }
                if (dist < 14) {
                    flashTint(SILVER, 0.28f * ramp);
                    vigFlash = Math.max(vigFlash, 0.4f * ramp);
                    desatFlash = Math.max(desatFlash, 0.3f * ramp);
                }
            }
            case SpellZoneEntity.QUAKE -> {
                if (dist < 40) shake(2.6f * ramp, 4, (float) (1 - dist / 40));
                for (int i = 0; i < 10; i++) {
                    l.addParticle(ParticleTypes.LARGE_SMOKE, c.x + r.nextGaussian() * 8, c.y + 0.2, c.z + r.nextGaussian() * 8, 0, 0.15, 0);
                }
                if (age % 8 == 0) {
                    ringBurst(l, c.add(0, 0.3, 0), 50, 0.9, ORANGE, 0.5f, 18);
                    ripple(c, 0.03f, 16);
                    glitch = Math.max(glitch, dist < 30 ? 0.2f : 0f);
                }
            }
            default -> { // SANCTUARY
                for (int k = 0; k < 3; k++) {
                    orbitRise(l, c.add(0, 0.1, 0), r.nextDouble() * Math.PI * 2, 8.0, 0.05, 0.12, pick(r, YELLOW, LIME, WHITE), 0.3f, 20);
                }
                for (int k = 0; k < 2; k++) {
                    orbitRise(l, c.add(0, 0.1, 0), r.nextDouble() * Math.PI * 2, 2.0, 0.2, 0.18, pick(r, YELLOW, WHITE), 0.3f, 20);
                }
                glow(l, c.x + r.nextGaussian() * 0.4, c.y, c.z + r.nextGaussian() * 0.4, 0, 0.3 + r.nextDouble() * 0.4, 0, YELLOW, 0.4f, 25, 0f, 0.98f);
                if (dist < 8) flashTint(YELLOW, 0.12f * ramp);
            }
        }
    }
}
