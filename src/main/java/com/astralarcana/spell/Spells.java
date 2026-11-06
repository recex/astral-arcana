package com.astralarcana.spell;

import com.astralarcana.Reg;
import com.astralarcana.entity.BlackHoleEntity;
import com.astralarcana.entity.CosmicSerpentEntity;
import com.astralarcana.entity.MeteorEntity;
import com.astralarcana.network.FxPacket;
import com.astralarcana.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class Spells {
    private Spells() {}

    public static final int COUNT = 4;
    public static final int[] COOLDOWN = {120, 200, 50, 320};
    public static final ChatFormatting[] COLOR = {ChatFormatting.GOLD, ChatFormatting.LIGHT_PURPLE, ChatFormatting.AQUA, ChatFormatting.YELLOW};

    public static void cast(ServerLevel level, ServerPlayer p, int spell, float power) {
        switch (spell) {
            case 0 -> meteorStorm(level, p, power);
            case 1 -> singularity(level, p, power);
            case 2 -> astralBeam(level, p, power);
            default -> supernova(level, p, power);
        }
    }

    private static BlockHitResult clip(ServerLevel level, ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        return level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
    }

    /** Faz um meteoro cair em 'land' vindo de 'height' blocos acima, na direcao 'dir' (dir.y < 0). */
    public static void dropMeteor(ServerLevel level, Vec3 land, Vec3 dir, double height, float size, Entity owner) {
        double maxH = Math.max(20, level.getMaxBuildHeight() - 4 - land.y);
        double h = Math.min(height, maxH);
        double ticks = h / Math.abs(dir.y);
        Vec3 spawn = land.subtract(dir.scale(ticks));
        MeteorEntity m = new MeteorEntity(Reg.METEOR.get(), level);
        m.setPos(spawn.x, spawn.y, spawn.z);
        m.setDeltaMovement(dir);
        m.setSize(size);
        m.setOwner(owner);
        level.addFreshEntity(m);
    }

    private static void meteorStorm(ServerLevel level, ServerPlayer p, float power) {
        double range = 70 + 30 * power;
        BlockHitResult hit = clip(level, p, range);
        Vec3 target;
        if (hit.getType() == HitResult.Type.MISS) {
            Vec3 end = p.getEyePosition().add(p.getLookAngle().scale(range));
            int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(end.x), (int) Math.floor(end.z));
            target = new Vec3(end.x, gy, end.z);
        } else {
            target = hit.getLocation();
        }
        RandomSource r = level.random;
        int count = 3 + Math.round(9 * power);
        double ang = r.nextDouble() * Math.PI * 2;
        Vec3 dir = new Vec3(Math.cos(ang) * 0.55, -1.7, Math.sin(ang) * 0.55);
        for (int i = 0; i < count; i++) {
            Vec3 land = target.add((r.nextDouble() - 0.5) * 14, 0, (r.nextDouble() - 0.5) * 14);
            dropMeteor(level, land, dir, 35 + r.nextDouble() * 45, 1.0f + r.nextFloat() * 0.9f + power * 0.4f, p);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0f, 0.7f);
    }

    private static void singularity(ServerLevel level, ServerPlayer p, float power) {
        BlockHitResult hit = clip(level, p, 40 + 20 * power);
        Vec3 t = hit.getType() == HitResult.Type.MISS
                ? p.getEyePosition().add(p.getLookAngle().scale(40 + 20 * power))
                : hit.getLocation().add(0, 4, 0);
        BlackHoleEntity bh = new BlackHoleEntity(Reg.BLACK_HOLE.get(), level);
        bh.setPos(t.x, t.y - 0.5, t.z);
        bh.setOwner(p);
        bh.setMaxLife(160 + (int) (160 * power));
        level.addFreshEntity(bh);
        ModNetwork.sendFx(level, FxPacket.Type.BLACKHOLE_BIRTH, t, t, power);
        level.playSound(null, t.x, t.y, t.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0f, 0.6f);
    }

    /** Raio de energia generico (usado pelo jogador e pela serpente). */
    public static void fireBeam(ServerLevel level, Entity owner, Vec3 origin, Vec3 look, Vec3 visualStart,
                                double range, float dmg, float boom, float power) {
        Vec3 end = origin.add(look.scale(range));
        BlockHitResult hit = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (hit.getType() != HitResult.Type.MISS) end = hit.getLocation();
        DamageSource src = level.damageSources().indirectMagic(owner, owner);
        for (Entity e : level.getEntities(owner, new AABB(origin, end).inflate(2),
                en -> en instanceof LivingEntity && en.isAlive() && !(en instanceof CosmicSerpentEntity))) {
            AABB bb = e.getBoundingBox().inflate(0.6);
            Optional<Vec3> c = bb.clip(origin, end);
            if (c.isPresent() || bb.contains(origin)) {
                e.hurt(src, dmg);
                e.setSecondsOnFire(6);
                e.setDeltaMovement(e.getDeltaMovement().add(look.scale(1.2 * (0.5 + power)).add(0, 0.4, 0)));
                e.hurtMarked = true;
            }
        }
        level.explode(null, end.x, end.y, end.z, boom, Level.ExplosionInteraction.MOB);
        ModNetwork.sendFx(level, FxPacket.Type.BEAM, visualStart, end, power);
        level.playSound(null, visualStart.x, visualStart.y, visualStart.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 4.0f, 1.0f);
    }

    private static void astralBeam(ServerLevel level, ServerPlayer p, float power) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 start = eye.add(look.scale(1.2)).add(0, -0.35, 0);
        fireBeam(level, p, eye, look, start, 60 + 40 * power, 12 + 18 * power, 2.5f + 2.5f * power, power);
    }

    private static void supernova(ServerLevel level, ServerPlayer p, float power) {
        Vec3 c = p.position().add(0, 1, 0);
        double radius = 18 + 10 * power;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius), en -> en != p && en.isAlive())) {
            Vec3 out = e.position().add(0, e.getBbHeight() / 2.0, 0).subtract(c);
            double d = out.length();
            if (d > radius) continue;
            double f = 1.0 - d / radius;
            e.hurt(level.damageSources().indirectMagic(p, p), (float) (8 + 28 * f * (0.4 + 0.6 * power)));
            e.setDeltaMovement(out.normalize().scale(0.6 + 2.4 * f).add(0, 0.5, 0));
            e.hurtMarked = true;
            e.setSecondsOnFire(8);
        }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4));
        ModNetwork.sendFx(level, FxPacket.Type.SUPERNOVA, c, c, power);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 6.0f, 0.6f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0f, 0.5f);
    }
}
