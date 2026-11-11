package com.astralarcana.entity;

import com.astralarcana.client.ClientFx;
import com.astralarcana.network.FxPacket;
import com.astralarcana.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Zona magica temporaria: tornado astral (se move) ou ataque orbital (marca o chao e depois cai uma chuva de pilares). */
public class SpellZoneEntity extends Entity {
    public static final int TORNADO = 0, ORBITAL = 1, SHIELD = 2, BLADES = 3, STORM = 4, BLIZZARD = 5, QUAKE = 6, SANCTUARY = 7;

    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(SpellZoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MAX_LIFE =
            SynchedEntityData.defineId(SpellZoneEntity.class, EntityDataSerializers.INT);
    private UUID owner;

    public SpellZoneEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, TORNADO);
        this.entityData.define(MAX_LIFE, 120);
    }

    public int getKind() { return this.entityData.get(KIND); }
    public void setKind(int k) { this.entityData.set(KIND, k); }
    public int getMaxLife() { return this.entityData.get(MAX_LIFE); }
    public void setMaxLife(int v) { this.entityData.set(MAX_LIFE, v); }
    public void setOwner(@Nullable Entity e) { this.owner = e == null ? null : e.getUUID(); }

    @Nullable
    public Entity getOwner() {
        if (owner != null && level() instanceof ServerLevel sl) return sl.getEntity(owner);
        return null;
    }

    /** Posicao do pilar i do ataque orbital (i = 0 e o centro). Igual no cliente e no servidor. */
    public Vec3 pillarPos(int i) {
        if (i == 0) return position();
        RandomSource r = RandomSource.create(getUUID().getLeastSignificantBits() + i * 7919L);
        double ang = r.nextDouble() * Math.PI * 2;
        double rad = 3 + r.nextDouble() * 6;
        return position().add(Math.cos(ang) * rad, 0, Math.sin(ang) * rad);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (getKind() == TORNADO) {
                Vec3 v = getDeltaMovement();
                setPos(getX() + v.x, getY(), getZ() + v.z);
            }
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.zoneTick(this));
            return;
        }
        if (tickCount >= getMaxLife()) {
            discard();
            return;
        }
        switch (getKind()) {
            case TORNADO -> tickTornado();
            case ORBITAL -> tickOrbital();
            case SHIELD -> tickShield();
            case BLADES -> tickBlades();
            case STORM -> tickStorm();
            case BLIZZARD -> tickBlizzard();
            case QUAKE -> tickQuake();
            default -> tickSanctuary();
        }
    }

    private void tickTornado() {
        ServerLevel sl = (ServerLevel) level();
        Entity ownerEntity = getOwner();
        Vec3 v = getDeltaMovement();
        double nx = getX() + v.x, nz = getZ() + v.z;
        int gy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(nx), (int) Math.floor(nz));
        setPos(nx, Mth.lerp(0.3, getY(), gy), nz);

        if (tickCount % 30 == 1) {
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 4.0f, 0.5f);
        }
        float ramp = Math.min(1f, tickCount / 15f) * Math.min(1f, (getMaxLife() - tickCount) / 15f);
        AABB box = new AABB(getX() - 9, getY() - 2, getZ() - 9, getX() + 9, getY() + 18, getZ() + 9);
        for (Entity e : sl.getEntities(this, box, en -> en.isAlive() && !(en instanceof SpellZoneEntity)
                && !(en instanceof CosmicSerpentEntity) && !(en instanceof MeteorEntity) && !(en instanceof BlackHoleEntity))) {
            if (e == ownerEntity) continue;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 rel = e.position().subtract(position());
            double h = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            if (h > 9) continue;
            Vec3 tang = h < 0.01 ? Vec3.ZERO : new Vec3(-rel.z, 0, rel.x).normalize().scale(0.35 * ramp);
            Vec3 inward = h < 0.01 ? Vec3.ZERO : new Vec3(-rel.x, 0, -rel.z).normalize().scale(0.12 * ramp);
            double lift = (e.getY() - getY() > 14 ? -0.05 : 0.14) * ramp;
            Vec3 nv = e.getDeltaMovement().scale(0.85).add(tang).add(inward).add(0, lift, 0);
            if (nv.length() > 1.4) nv = nv.normalize().scale(1.4);
            e.setDeltaMovement(nv);
            e.hurtMarked = true;
            e.fallDistance = 0;
            if (e instanceof LivingEntity le && tickCount % 10 == 0) {
                le.hurt(sl.damageSources().indirectMagic(this, ownerEntity), 3.0f);
            }
        }
    }

    private void tickOrbital() {
        int t = tickCount;
        if (t == 1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4.0f, 0.5f);
        }
        if (t >= 50 && t <= 60 && (t - 50) % 2 == 0) strike((t - 50) / 2 + 1, 1.0f);
        if (t == 64) strike(0, 2.0f);
    }

    private void strike(int i, float scale) {
        ServerLevel sl = (ServerLevel) level();
        Vec3 p = pillarPos(i);
        Entity ownerEntity = getOwner();
        sl.explode(this, p.x, p.y, p.z, scale > 1f ? 6.0f : 3.2f, Level.ExplosionInteraction.MOB);
        double r = 7.0 * scale;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(r),
                en -> en != ownerEntity && en.isAlive() && en.position().distanceTo(p) <= r)) {
            e.hurt(sl.damageSources().indirectMagic(this, ownerEntity), 18.0f * scale);
        }
        ModNetwork.sendFx(sl, FxPacket.Type.ORBITAL, p.add(0, 160, 0), p, scale);
        sl.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0f, 0.7f);
    }

    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 400.0 * 400.0; }
    @Override public boolean ignoreExplosion() { return true; }
    @Override public boolean isPickable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setKind(tag.getInt("Kind"));
        setMaxLife(tag.getInt("MaxLife"));
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", getKind());
        tag.putInt("MaxLife", getMaxLife());
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // ------------------------------------------------------------------ novas zonas
    private void followOwner() {
        Entity o = getOwner();
        if (o != null) setPos(o.getX(), o.getY(), o.getZ());
    }

    private List<LivingEntity> livingAround(double r, double up, @Nullable Entity skip) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class,
                new AABB(getX() - r, getY() - 3, getZ() - r, getX() + r, getY() + up, getZ() + r),
                en -> en.isAlive() && en != skip && !(en instanceof CosmicSerpentEntity))) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (dx * dx + dz * dz <= r * r) out.add(e);
        }
        return out;
    }

    private void tickShield() {
        followOwner();
        ServerLevel sl = (ServerLevel) level();
        Entity owner = getOwner();
        double R = 5.5;
        Vec3 c = position().add(0, 1, 0);
        for (Entity e : sl.getEntities(this, new AABB(c, c).inflate(R),
                en -> en.isAlive() && !(en instanceof SpellZoneEntity) && !(en instanceof CosmicSerpentEntity))) {
            if (e == owner) continue;
            Vec3 out = e.position().add(0, e.getBbHeight() / 2.0, 0).subtract(c);
            double d = out.length();
            if (d > R) continue;
            Vec3 push = d < 0.01 ? new Vec3(0, 1, 0) : out.normalize();
            e.setDeltaMovement(push.scale(0.9).add(0, 0.15, 0));
            e.hurtMarked = true;
            if (e instanceof LivingEntity le && tickCount % 10 == 0) {
                le.hurt(sl.damageSources().indirectMagic(this, owner), 2.0f);
            }
        }
    }

    private void tickBlades() {
        followOwner();
        if (tickCount % 4 != 0) return;
        ServerLevel sl = (ServerLevel) level();
        Entity owner = getOwner();
        DamageSource src = sl.damageSources().indirectMagic(this, owner);
        for (LivingEntity e : livingAround(8, 3, owner)) {
            e.hurt(src, 6.0f);
            Vec3 rel = e.position().subtract(position());
            Vec3 tang = new Vec3(-rel.z, 0, rel.x);
            tang = tang.lengthSqr() < 1.0E-4 ? Vec3.ZERO : tang.normalize().scale(0.5);
            Vec3 out = rel.lengthSqr() < 1.0E-4 ? Vec3.ZERO : new Vec3(rel.x, 0, rel.z).normalize().scale(0.4);
            e.setDeltaMovement(e.getDeltaMovement().add(tang).add(out).add(0, 0.2, 0));
            e.hurtMarked = true;
        }
    }

    private void tickStorm() {
        if (tickCount % 6 != 0) return;
        ServerLevel sl = (ServerLevel) level();
        Entity owner = getOwner();
        List<LivingEntity> ents = livingAround(12, 8, owner);
        Vec3 p;
        if (!ents.isEmpty() && random.nextFloat() < 0.6f) {
            p = ents.get(random.nextInt(ents.size())).position();
        } else {
            double ang = random.nextDouble() * Math.PI * 2, rad = random.nextDouble() * 12;
            double x = getX() + Math.cos(ang) * rad, z = getZ() + Math.sin(ang) * rad;
            int gy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
            p = new Vec3(x, gy, z);
        }
        DamageSource src = sl.damageSources().indirectMagic(this, owner);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(3.5),
                en -> en != owner && en.isAlive() && en.position().distanceTo(p) <= 3.5)) {
            e.hurt(src, 15.0f);
            e.setSecondsOnFire(4);
        }
        ModNetwork.sendFx(sl, FxPacket.Type.CHAIN, p.add(0, 60, 0), p, 1.0f);
        sl.playSound(null, p.x, p.y, p.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.0f, 1.0f + random.nextFloat() * 0.4f);
    }

    private void tickBlizzard() {
        if (tickCount % 10 != 0) return;
        ServerLevel sl = (ServerLevel) level();
        for (LivingEntity e : livingAround(14, 10, getOwner())) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
            e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 40, e.getTicksFrozen() + 40));
            e.hurt(sl.damageSources().freeze(), 1.5f);
        }
    }

    private void tickQuake() {
        ServerLevel sl = (ServerLevel) level();
        if (tickCount % 16 == 0) {
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0f, 0.5f);
        }
        if (tickCount % 8 != 0) return;
        Entity owner = getOwner();
        DamageSource src = sl.damageSources().indirectMagic(this, owner);
        for (LivingEntity e : livingAround(16, 4, owner)) {
            if (!e.onGround()) continue;
            e.setDeltaMovement(e.getDeltaMovement().add((random.nextDouble() - 0.5) * 0.4, 0.75, (random.nextDouble() - 0.5) * 0.4));
            e.hurtMarked = true;
            e.hurt(src, 5.0f);
        }
    }

    private void tickSanctuary() {
        if (tickCount % 10 != 0) return;
        ServerLevel sl = (ServerLevel) level();
        for (LivingEntity e : livingAround(8, 4, null)) {
            if (e instanceof Player pl) {
                pl.heal(2.0f);
                pl.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, true, true));
                if (tickCount % 50 == 0) {
                    for (MobEffectInstance mi : new ArrayList<>(pl.getActiveEffects())) {
                        if (mi.getEffect().getCategory() == MobEffectCategory.HARMFUL) pl.removeEffect(mi.getEffect());
                    }
                }
            } else if (e instanceof Monster) {
                e.hurt(sl.damageSources().indirectMagic(this, getOwner()), 3.0f);
            }
        }
    }
}
