package com.astralarcana.client;

import com.astralarcana.AstralArcana;
import com.astralarcana.client.particle.GlowParticle;
import com.astralarcana.client.particle.OrbitParticle;
import com.astralarcana.entity.BlackHoleEntity;
import com.astralarcana.entity.MeteorEntity;
import com.astralarcana.item.CosmicStaffItem;
import com.astralarcana.network.FxPacket;
import com.astralarcana.entity.CosmicSerpentEntity;
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
    static final float[][] SPELL_COL = {ORANGE, VIOLET, CYAN, YELLOW};
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
        final Vec3 a, b; final float power; final int life = 16;
        int age;
        Beam(Vec3 a, Vec3 b, float power) { this.a = a; this.b = b; this.power = power; }
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
        if (GlowParticle.SPRITES == null) return;
        Minecraft.getInstance().particleEngine.add(new GlowParticle(l, x, y, z, vx, vy, vz, c[0], c[1], c[2], size, life, grav, drag));
    }

    private static void orbit(ClientLevel l, Vec3 c, double ang, double radius, double angSpeed, double inward,
                              double tilt, float[] col, float size, int life) {
        if (GlowParticle.SPRITES == null) return;
        Minecraft.getInstance().particleEngine.add(new OrbitParticle(l, c.x, c.y, c.z, ang, radius, angSpeed, inward, tilt, col, size, life));
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
        return shakeLeft > 0 ? shakeAmp * shakeLeft / shakeTotal : 0;
    }

    private static void shake(float amp, int ticks, float near) {
        amp *= near;
        if (amp > curShake()) {
            shakeAmp = amp; shakeTotal = ticks; shakeLeft = ticks;
        }
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
        }
    }

    private static void meteorImpact(ClientLevel l, Vec3 c, float size, float near) {
        RandomSource r = l.random;
        shake(1.8f * size, 30, near);
        ripple(c, 0.035f * size * near, 22);
        warp(0.08f * near);
        flash(ORANGE, 0.5f * near, 0.06f);
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
        shake(3.2f, 60, near);
        ripple(c, 0.09f * Math.max(near, 0.35f), 50);
        warp(0.35f * Math.max(near, 0.35f));
        flash(WHITE, 1.0f * Math.max(near, 0.35f), 0.018f);
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
        shake(2.8f, 45, near);
        ripple(c, 0.1f * Math.max(near, 0.3f), 44);
        warp(0.3f * Math.max(near, 0.3f));
        flash(WHITE, 0.85f * Math.max(near, 0.3f), 0.03f);
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
            return;
        }
        if (mc.isPaused()) return;
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
        for (Ripple rp : ripples) rp.age++;
        ripples.removeIf(rp -> rp.age >= rp.life);
        for (Shock s : shocks) s.age++;
        shocks.removeIf(s -> s.age >= s.life);
        for (Beam b : beams) {
            b.age++;
            if (b.age <= 8) emitBeam(l, b);
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float pt = (float) e.getPartialTick();
        float s = shakeLeft > 0 ? shakeAmp * Math.max(0f, shakeLeft - pt) / shakeTotal : 0f;
        s += charge * 0.12f;
        if (s <= 0.001f) return;
        float t = mc.level.getGameTime() + pt;
        e.setPitch(e.getPitch() + (float) (Math.sin(t * 1.9) + Math.sin(t * 3.1 + 1)) * 0.5f * s);
        e.setYaw(e.getYaw() + (float) (Math.sin(t * 2.3 + 2) + Math.sin(t * 3.7)) * 0.5f * s);
        e.setRoll(e.getRoll() + (float) (Math.sin(t * 2.7) + Math.sin(t * 4.1 + .5)) * 0.7f * s);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        if (!e.usedConfiguredFov()) return;
        double f = 1.0 - 0.22 * charge + 0.012 * curShake();
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
        if (bars > 0.01f) {
            int bh = (int) (h * 0.09f * bars);
            g.fill(0, 0, w, bh, 0xFF000000);
            g.fill(0, h - bh, w, h, 0xFF000000);
        }
        if (flashA > 0.01f) {
            g.fill(0, 0, w, h, argb(flashA, flashR, flashG, flashB));
        }
    }

    // ---- mundo: ondas de choque e raios ----
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (shocks.isEmpty() && beams.isEmpty()) return;
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
            ps.pushPose();
            ps.translate(s.pos.x, s.pos.y, s.pos.z);
            if (s.sphere) {
                FxRender.sphere(ps, vc, R, 12, 20, s.col[0], s.col[1], s.col[2], fade);
                FxRender.sphere(ps, vc, R * 0.8f, 10, 16, 1f, 1f, 1f, fade * 0.5f);
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
            FxRender.tube(ps, vc, b.a, b.b, 0.14f * k * pulse, 8, 1f, 1f, 1f, 0.95f * k);
            FxRender.tube(ps, vc, b.a, b.b, 0.42f * k * pulse, 10, 0.3f, 0.8f, 1f, 0.45f * k);
            FxRender.tube(ps, vc, b.a, b.b, 1.0f * k * pulse, 12, 0.45f, 0.3f, 1f, 0.18f * k);
        }
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
        if (distortShader == null) return;
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
        float chroma = warpAmt * 0.012f + charge * 0.003f + curShake() * 0.0012f;
        if (lensS <= 0f && waveStr <= 0f && blur < 0.004f && chroma < 0.0005f) return;

        RenderTarget main = mc.getMainRenderTarget();
        if (copyTarget == null) {
            copyTarget = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        } else if (copyTarget.width != main.width || copyTarget.height != main.height) {
            copyTarget.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        copyTarget.setFilterMode(9729);

        // copia a imagem atual da tela e redesenha distorcida por cima
        GlStateManager._glBindFramebuffer(36008, main.frameBufferId);
        GlStateManager._glBindFramebuffer(36009, copyTarget.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, copyTarget.width, copyTarget.height, 16384, 9728);
        main.bindWrite(false);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> distortShader);
        RenderSystem.setShaderTexture(0, copyTarget.getColorTextureId());

        distortShader.safeGetUniform("ScreenSize").set((float) main.width, (float) main.height);
        distortShader.safeGetUniform("Lens").set(lensX, lensY, lensS, lensR);
        distortShader.safeGetUniform("Horizon").set(horizon);
        distortShader.safeGetUniform("Wave").set(waveX, waveY, waveR, waveW);
        distortShader.safeGetUniform("WaveStr").set(waveStr);
        distortShader.safeGetUniform("Blur").set(lensS > 0f ? lensX : 0.5f, lensS > 0f ? lensY : 0.5f, blur);
        distortShader.safeGetUniform("Chroma").set(chroma);

        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(-1.0, -1.0, 0.0).uv(0f, 0f).endVertex();
        bb.vertex(1.0, -1.0, 0.0).uv(1f, 0f).endVertex();
        bb.vertex(1.0, 1.0, 0.0).uv(1f, 1f).endVertex();
        bb.vertex(-1.0, 1.0, 0.0).uv(0f, 1f).endVertex();
        BufferUploader.drawWithShader(bb.end());

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
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

    public static void serpentRoar(CosmicSerpentEntity e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Vec3 head = e.segs[0];
        float near = (float) Mth.clamp(1.0 - mc.player.position().distanceTo(head) / 90.0, 0.0, 1.0);
        ripple(head, 0.035f * near, 26);
        shake(0.9f, 18, near);
        warp(0.08f * near);
    }
}
