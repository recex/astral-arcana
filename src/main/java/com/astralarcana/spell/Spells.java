package com.astralarcana.spell;

import com.astralarcana.Reg;
import com.astralarcana.entity.BlackHoleEntity;
import com.astralarcana.entity.CosmicSerpentEntity;
import com.astralarcana.entity.MeteorEntity;
import com.astralarcana.entity.SpellZoneEntity;
import com.astralarcana.network.FxPacket;
import com.astralarcana.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class Spells {
    private Spells() {}

    public static final int COUNT = 31;
    public static final int[] COOLDOWN = {120, 200, 50, 320, 220, 400, 140, 420, 120, 90, 300,
            160, 140, 160, 100, 110, 200, 120, 200, 300, 360, 360, 80, 400, 400, 300, 260, 360, 400, 320, 500};
    public static final ChatFormatting[] COLOR = {ChatFormatting.GOLD, ChatFormatting.LIGHT_PURPLE, ChatFormatting.AQUA, ChatFormatting.YELLOW,
            ChatFormatting.DARK_AQUA, ChatFormatting.WHITE, ChatFormatting.DARK_PURPLE, ChatFormatting.RED,
            ChatFormatting.BLUE, ChatFormatting.DARK_RED, ChatFormatting.GREEN,
            ChatFormatting.DARK_PURPLE, ChatFormatting.AQUA, ChatFormatting.GOLD, ChatFormatting.GRAY, ChatFormatting.AQUA,
            ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_GREEN, ChatFormatting.AQUA, ChatFormatting.GOLD,
            ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_AQUA, ChatFormatting.RED, ChatFormatting.DARK_PURPLE, ChatFormatting.LIGHT_PURPLE,
            ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.WHITE, ChatFormatting.GOLD, ChatFormatting.GREEN};

    public static void cast(ServerLevel level, ServerPlayer p, int spell, float power) {
        switch (spell) {
            case 0 -> meteorStorm(level, p, power);
            case 1 -> singularity(level, p, power);
            case 2 -> astralBeam(level, p, power);
            case 3 -> supernova(level, p, power);
            case 4 -> tornado(level, p, power);
            case 5 -> timeStop(level, p, power);
            case 6 -> blink(level, p, power);
            case 7 -> orbitalStrike(level, p, power);
            case 8 -> chainLightning(level, p, power);
            case 9 -> dimensionalSlash(level, p, power);
            case 10 -> rebirth(level, p, power);
            case 11 -> gravityCrush(level, p, power);
            case 12 -> frostNova(level, p, power);
            case 13 -> solarNova(level, p, power);
            case 14 -> sonicScream(level, p, power);
            case 15 -> starVolley(level, p, power);
            case 16 -> skyJudgement(level, p, power);
            case 17 -> dimensionalSwap(level, p, power);
            case 18 -> stellarImpulse(level, p, power);
            case 19 -> stellarVision(level, p, power);
            case 20 -> colossalMeteor(level, p, power);
            case 21 -> singularityTrio(level, p, power);
            case 22 -> tractorBeam(level, p, power);
            case 23 -> stellarFury(level, p, power);
            case 24 -> stellarVeil(level, p, power);
            case 25 -> zoneSpell(level, p, SpellZoneEntity.SHIELD, 200, false);
            case 26 -> zoneSpell(level, p, SpellZoneEntity.BLADES, 100, false);
            case 27 -> zoneSpell(level, p, SpellZoneEntity.STORM, 120, true);
            case 28 -> zoneSpell(level, p, SpellZoneEntity.BLIZZARD, 200, true);
            case 29 -> zoneSpell(level, p, SpellZoneEntity.QUAKE, 80, false);
            default -> zoneSpell(level, p, SpellZoneEntity.SANCTUARY, 200, false);
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

    // =================================================================== PODERES 5 a 11
    private static Vec3 groundTarget(ServerLevel level, ServerPlayer p, double range) {
        BlockHitResult hit = clip(level, p, range);
        if (hit.getType() == HitResult.Type.MISS) {
            Vec3 end = p.getEyePosition().add(p.getLookAngle().scale(range));
            int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(end.x), (int) Math.floor(end.z));
            return new Vec3(end.x, gy, end.z);
        }
        return hit.getLocation();
    }

    private static void tornado(ServerLevel level, ServerPlayer p, float power) {
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : dir.normalize();
        Vec3 start = p.position().add(dir.scale(5));
        SpellZoneEntity z = new SpellZoneEntity(Reg.ZONE.get(), level);
        z.setKind(SpellZoneEntity.TORNADO);
        z.setMaxLife(120 + (int) (120 * power));
        z.setOwner(p);
        z.setPos(start.x, start.y, start.z);
        z.setDeltaMovement(dir.scale(0.35 + 0.1 * power));
        level.addFreshEntity(z);
        ModNetwork.sendFx(level, FxPacket.Type.TORNADO, start, start, power);
        level.playSound(null, start.x, start.y, start.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 4.0f, 0.5f);
    }

    private static void timeStop(ServerLevel level, ServerPlayer p, float power) {
        Vec3 c = p.position().add(0, 1, 0);
        double radius = 20 + 8 * power;
        int dur = 80 + (int) (80 * power);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(radius), en -> en != p && en.isAlive())) {
            if (e.position().distanceTo(c) > radius) continue;
            if (e instanceof CosmicSerpentEntity s) {
                s.frozenTicks = dur;
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 9, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.JUMP, dur, 128, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 9, false, false));
            e.setDeltaMovement(Vec3.ZERO);
            e.hurtMarked = true;
        }
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 1));
        ModNetwork.sendFx(level, FxPacket.Type.TIMESTOP, c, new Vec3(dur, 0, 0), power);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 4.0f, 0.5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 2.0f, 1.4f);
    }

    private static void blink(ServerLevel level, ServerPlayer p, float power) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double range = 25 + 25 * power;
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double dist = hit.getType() == HitResult.Type.MISS ? range : Math.max(0, hit.getLocation().distanceTo(eye) - 0.8);
        Vec3 dest = null;
        for (double d = dist; d >= 0; d -= 1.0) {
            Vec3 feet = eye.add(look.scale(d)).subtract(0, p.getEyeHeight(), 0);
            AABB box = p.getBoundingBox().move(feet.x - p.getX(), feet.y - p.getY(), feet.z - p.getZ());
            if (level.noCollision(p, box)) { dest = feet; break; }
        }
        if (dest == null) return;
        Vec3 newEye = dest.add(0, p.getEyeHeight(), 0);
        DamageSource src = level.damageSources().indirectMagic(p, p);
        for (Entity e : level.getEntities(p, new AABB(eye, newEye).inflate(1.5), en -> en instanceof LivingEntity && en.isAlive())) {
            AABB bb = e.getBoundingBox().inflate(0.8);
            if (bb.clip(eye, newEye).isPresent()) {
                e.hurt(src, 8 + 10 * power);
                e.setDeltaMovement(e.getDeltaMovement().add(look.scale(1.0).add(0, 0.4, 0)));
                e.hurtMarked = true;
            }
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 2.0f, 0.7f);
        p.connection.teleport(dest.x, dest.y, dest.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 4, false, false));
        ModNetwork.sendFx(level, FxPacket.Type.BLINK, eye, newEye, power);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0f, 0.6f);
    }

    private static void orbitalStrike(ServerLevel level, ServerPlayer p, float power) {
        Vec3 t = groundTarget(level, p, 70 + 30 * power);
        SpellZoneEntity z = new SpellZoneEntity(Reg.ZONE.get(), level);
        z.setKind(SpellZoneEntity.ORBITAL);
        z.setMaxLife(75);
        z.setOwner(p);
        z.setPos(t.x, t.y, t.z);
        level.addFreshEntity(z);
        level.playSound(null, t.x, t.y, t.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 4.0f, 0.5f);
    }

    private static void chainLightning(ServerLevel level, ServerPlayer p, float power) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 start = eye.add(look.scale(1.2)).add(0, -0.35, 0);
        Vec3 end = eye.add(look.scale(45));
        LivingEntity first = null;
        double bestD = 1.0E18;
        for (Entity e : level.getEntities(p, new AABB(eye, end).inflate(2), en -> en instanceof LivingEntity && en.isAlive())) {
            Optional<Vec3> c = e.getBoundingBox().inflate(0.8).clip(eye, end);
            if (c.isPresent()) {
                double d = eye.distanceToSqr(c.get());
                if (d < bestD) { bestD = d; first = (LivingEntity) e; }
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0f, 1.4f);
        if (first == null) {
            BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            Vec3 pt = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
            ModNetwork.sendFx(level, FxPacket.Type.CHAIN, start, pt, power);
            return;
        }
        List<LivingEntity> visited = new ArrayList<>();
        DamageSource src = level.damageSources().indirectMagic(p, p);
        Vec3 prev = start;
        LivingEntity cur = first;
        int jumps = 6 + Math.round(6 * power);
        float dmg = 16 + 10 * power;
        for (int j = 0; j < jumps && cur != null; j++) {
            visited.add(cur);
            Vec3 cp = cur.position().add(0, cur.getBbHeight() / 2.0, 0);
            cur.hurt(src, dmg * (float) Math.pow(0.88, j));
            cur.setSecondsOnFire(3);
            ModNetwork.sendFx(level, FxPacket.Type.CHAIN, prev, cp, power);
            prev = cp;
            LivingEntity next = null;
            double nd = 100.0;
            for (LivingEntity c : level.getEntitiesOfClass(LivingEntity.class, cur.getBoundingBox().inflate(10),
                    en -> en != p && en.isAlive() && !visited.contains(en))) {
                double d = c.distanceToSqr(cur);
                if (d < nd) { nd = d; next = c; }
            }
            cur = next;
        }
    }

    private static void dimensionalSlash(ServerLevel level, ServerPlayer p, float power) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double range = 14 + 6 * power;
        float dmg = 14 + 16 * power;
        DamageSource src = level.damageSources().indirectMagic(p, p);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(range), en -> en != p && en.isAlive())) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2.0, 0).subtract(eye);
            double d = to.length();
            if (d > range || d < 0.01) continue;
            if (look.dot(to.scale(1.0 / d)) < 0.34) continue;
            e.hurt(src, dmg);
            e.setDeltaMovement(to.normalize().scale(1.5).add(0, 0.3, 0));
            e.hurtMarked = true;
        }
        ModNetwork.sendFx(level, FxPacket.Type.SLASH, eye, look, power);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 3.0f, 0.5f);
        level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 2.0f, 1.2f);
    }

    private static void rebirth(ServerLevel level, ServerPlayer p, float power) {
        for (Player t : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(12), pl -> pl.isAlive())) {
            t.heal(t == p ? 20f : 12f);
            for (MobEffectInstance mi : new ArrayList<>(t.getActiveEffects())) {
                if (mi.getEffect().getCategory() == MobEffectCategory.HARMFUL) t.removeEffect(mi.getEffect());
            }
            t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100 + (int) (100 * power), 2));
            t.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 2));
            t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
        }
        Vec3 c = p.position();
        ModNetwork.sendFx(level, FxPacket.Type.REBIRTH, c, c, power);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 2.0f, 1.0f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 3.0f, 1.5f);
    }

    // =================================================================== PODERES 12 a 31
    private static void preset(ServerLevel level, int id, Vec3 a, float e1, float e2, float power) {
        ModNetwork.sendFx(level, FxPacket.Type.PRESET, a, new Vec3(id, e1, e2), power);
    }

    @org.jetbrains.annotations.Nullable
    private static LivingEntity lookTarget(ServerLevel level, ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        LivingEntity best = null;
        double bestD = 1.0E18;
        for (Entity e : level.getEntities(p, new AABB(eye, end).inflate(2), en -> en instanceof LivingEntity && en.isAlive())) {
            Optional<Vec3> c = e.getBoundingBox().inflate(0.8).clip(eye, end);
            if (c.isPresent()) {
                double d = eye.distanceToSqr(c.get());
                if (d < bestD) { bestD = d; best = (LivingEntity) e; }
            }
        }
        return best;
    }

    private static void gravityCrush(ServerLevel level, ServerPlayer p, float power) {
        BlockHitResult hit = clip(level, p, 35);
        Vec3 c = hit.getType() == HitResult.Type.MISS
                ? p.getEyePosition().add(p.getLookAngle().scale(35)) : hit.getLocation().add(0, 1, 0);
        double r = 14 + 6 * power;
        DamageSource src = level.damageSources().indirectMagic(p, p);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), en -> en != p && en.isAlive())) {
            Vec3 ec = e.position().add(0, e.getBbHeight() / 2.0, 0);
            double d = ec.distanceTo(c);
            if (d > r) continue;
            Vec3 to = c.subtract(ec);
            e.setDeltaMovement(to.lengthSqr() < 1.0E-4 ? Vec3.ZERO : to.normalize().scale(Math.min(2.5, 0.8 + d * 0.15)));
            e.hurtMarked = true;
            e.hurt(src, (float) (10 + 14 * (1 - d / r)));
        }
        preset(level, 0, c, 0, 0, power);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 3.0f, 0.5f);
    }

    private static void frostNova(ServerLevel level, ServerPlayer p, float power) {
        Vec3 c = p.position().add(0, 1, 0);
        double r = 12 + 6 * power;
        DamageSource src = level.damageSources().indirectMagic(p, p);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), en -> en != p && en.isAlive())) {
            Vec3 out = e.position().add(0, e.getBbHeight() / 2.0, 0).subtract(c);
            if (out.length() > r) continue;
            e.hurt(src, 10 + 8 * power);
            e.setTicksFrozen(e.getTicksRequiredToFreeze() + 200);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
            e.setDeltaMovement(out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize().scale(0.5).add(0, 0.2, 0));
            e.hurtMarked = true;
        }
        preset(level, 1, c, 0, 0, power);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 3.0f, 0.5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 3.0f, 0.6f);
    }

    private static void solarNova(ServerLevel level, ServerPlayer p, float power) {
        Vec3 c = p.position().add(0, 1, 0);
        double r = 14 + 6 * power;
        DamageSource src = level.damageSources().indirectMagic(p, p);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), en -> en != p && en.isAlive())) {
            Vec3 out = e.position().add(0, e.getBbHeight() / 2.0, 0).subtract(c);
            if (out.length() > r) continue;
            e.hurt(src, 12 + 10 * power);
            e.setSecondsOnFire(10);
            e.setDeltaMovement(out.lengthSqr() < 1.0E-4 ? Vec3.ZERO : out.normalize().scale(1.2).add(0, 0.4, 0));
            e.hurtMarked = true;
        }
        preset(level, 2, c, 0, 0, power);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 4.0f, 0.7f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 3.0f, 0.5f);
    }

    private static void sonicScream(ServerLevel level, ServerPlayer p, float power) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        DamageSource src = level.damageSources().indirectMagic(p, p);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(25), en -> en != p && en.isAlive())) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2.0, 0).subtract(eye);
            double d = to.length();
            if (d > 25 || d < 0.01) continue;
            if (look.dot(to.scale(1.0 / d)) < 0.5) continue;
            e.hurt(src, 10 + 8 * power);
            e.setDeltaMovement(look.scale(2.6).add(0, 0.3, 0));
            e.hurtMarked = true;
        }
        preset(level, 3, eye, p.getYRot(), p.getXRot(), power);
        level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 4.0f, 0.7f);
    }

    private static void starVolley(ServerLevel level, ServerPlayer p, float power) {
        Vec3 eye = p.getEyePosition();
        for (int k = -2; k <= 2; k++) {
            Vec3 dir = Vec3.directionFromRotation(p.getXRot(), p.getYRot() + k * 8f);
            Vec3 start = eye.add(dir.scale(1.2)).add(0, -0.35, 0);
            fireBeam(level, p, eye, dir, start, 70, 8 + 6 * power, 1.5f, power * 0.6f);
        }
    }

    private static void skyJudgement(ServerLevel level, ServerPlayer p, float power) {
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(30), en -> en != p && en.isAlive());
        targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        int n = Math.min(targets.size(), 6 + Math.round(6 * power));
        DamageSource src = level.damageSources().indirectMagic(p, p);
        for (int i = 0; i < n; i++) {
            LivingEntity e = targets.get(i);
            Vec3 ep = e.position();
            e.hurt(src, 20 + 10 * power);
            e.setSecondsOnFire(5);
            ModNetwork.sendFx(level, FxPacket.Type.CHAIN, ep.add(0, 60, 0), ep.add(0, e.getBbHeight() / 2.0, 0), power);
        }
        if (n == 0) {
            Vec3 t = groundTarget(level, p, 50);
            ModNetwork.sendFx(level, FxPacket.Type.CHAIN, t.add(0, 60, 0), t, power);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 6.0f, 0.8f);
    }

    private static void dimensionalSwap(ServerLevel level, ServerPlayer p, float power) {
        LivingEntity t = lookTarget(level, p, 40);
        if (t == null) return;
        Vec3 pp = p.position();
        Vec3 tp = t.position();
        Vec3 eyeA = p.getEyePosition();
        Vec3 eyeB = tp.add(0, t.getEyeHeight(), 0);
        if (t instanceof ServerPlayer sp) sp.connection.teleport(pp.x, pp.y, pp.z, sp.getYRot(), sp.getXRot());
        else t.moveTo(pp.x, pp.y, pp.z);
        p.connection.teleport(tp.x, tp.y, tp.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        t.fallDistance = 0;
        t.hurt(level.damageSources().indirectMagic(p, p), 6 + 6 * power);
        ModNetwork.sendFx(level, FxPacket.Type.BLINK, eyeA, eyeB, power);
        level.playSound(null, pp.x, pp.y, pp.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0f, 0.8f);
        level.playSound(null, tp.x, tp.y, tp.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0f, 1.2f);
    }

    private static void stellarImpulse(ServerLevel level, ServerPlayer p, float power) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 3));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 2));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 2));
        p.setDeltaMovement(p.getLookAngle().scale(1.6).add(0, 0.2, 0));
        p.hurtMarked = true;
        Vec3 c = p.position().add(0, 1, 0);
        preset(level, 4, c, 0, 0, power);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 3.0f, 0.6f);
    }

    private static void stellarVision(ServerLevel level, ServerPlayer p, float power) {
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(50), en -> en != p && en.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, false, false));
        }
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false));
        Vec3 c = p.position().add(0, 1, 0);
        preset(level, 5, c, 0, 0, power);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 3.0f, 1.2f);
    }

    private static void colossalMeteor(ServerLevel level, ServerPlayer p, float power) {
        Vec3 t = groundTarget(level, p, 80 + 20 * power);
        dropMeteor(level, t, new Vec3(0.3, -1.7, 0.2), 90, 4.0f, p);
        level.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.0f, 0.5f);
    }

    private static void singularityTrio(ServerLevel level, ServerPlayer p, float power) {
        Vec3 t = groundTarget(level, p, 40 + 20 * power).add(0, 4, 0);
        for (int i = 0; i < 3; i++) {
            double ang = i * Math.PI * 2 / 3;
            Vec3 pos = t.add(Math.cos(ang) * 6, 0, Math.sin(ang) * 6);
            BlackHoleEntity bh = new BlackHoleEntity(Reg.BLACK_HOLE.get(), level);
            bh.setPos(pos.x, pos.y - 0.5, pos.z);
            bh.setOwner(p);
            bh.setMaxLife(120);
            level.addFreshEntity(bh);
            ModNetwork.sendFx(level, FxPacket.Type.BLACKHOLE_BIRTH, pos, pos, power);
        }
        level.playSound(null, t.x, t.y, t.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 3.0f, 0.5f);
    }

    private static void tractorBeam(ServerLevel level, ServerPlayer p, float power) {
        LivingEntity t = lookTarget(level, p, 45);
        if (t == null) return;
        Vec3 eye = p.getEyePosition();
        Vec3 start = eye.add(p.getLookAngle().scale(1.2)).add(0, -0.35, 0);
        Vec3 tc = t.position().add(0, t.getBbHeight() / 2.0, 0);
        Vec3 to = eye.subtract(tc);
        t.setDeltaMovement(to.lengthSqr() < 1.0E-4 ? Vec3.ZERO : to.normalize().scale(2.2).add(0, 0.3, 0));
        t.hurtMarked = true;
        t.hurt(level.damageSources().indirectMagic(p, p), 4 + 4 * power);
        ModNetwork.sendFx(level, FxPacket.Type.LINK, start, tc, power);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2.0f, 1.6f);
    }

    private static void stellarFury(ServerLevel level, ServerPlayer p, float power) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 2));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1));
        Vec3 c = p.position().add(0, 1, 0);
        preset(level, 6, c, 0, 0, power);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 3.0f, 0.6f);
    }

    private static void stellarVeil(ServerLevel level, ServerPlayer p, float power) {
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 300, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
        Vec3 c = p.position().add(0, 1, 0);
        preset(level, 7, c, 0, 0, power);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0f, 0.5f);
    }

    /** Zonas temporarias (barreira, laminas, tempestade, nevasca, terremoto, santuario). */
    private static void zoneSpell(ServerLevel level, ServerPlayer p, int kind, int life, boolean atTarget) {
        Vec3 pos = atTarget ? groundTarget(level, p, 60) : p.position();
        SpellZoneEntity z = new SpellZoneEntity(Reg.ZONE.get(), level);
        z.setKind(kind);
        z.setMaxLife(life);
        z.setOwner(p);
        z.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(z);
        if (kind == SpellZoneEntity.SHIELD) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, life, 2));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, life, 2));
        }
        preset(level, 8, pos, kind, 0, 1f);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 3.0f, 0.8f);
    }
}
