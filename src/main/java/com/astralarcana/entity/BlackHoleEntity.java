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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BlackHoleEntity extends Entity {
    public static final double RADIUS = 16.0;
    private static final EntityDataAccessor<Integer> MAX_LIFE =
            SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.INT);
    private UUID owner;

    public BlackHoleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MAX_LIFE, 200);
    }

    public int getMaxLife() { return this.entityData.get(MAX_LIFE); }
    public void setMaxLife(int v) { this.entityData.set(MAX_LIFE, v); }
    public void setOwner(@Nullable Entity e) { this.owner = e == null ? null : e.getUUID(); }

    @Nullable
    public Entity getOwner() {
        if (owner != null && level() instanceof ServerLevel sl) return sl.getEntity(owner);
        return null;
    }

    public Vec3 center() {
        return position().add(0, getBbHeight() / 2.0, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.blackHoleTick(this));
            return;
        }
        int max = getMaxLife();
        float ramp = Math.min(1f, tickCount / 20f) * Math.min(1f, (max - tickCount) / 20f);
        Vec3 c = center();
        Entity ownerEntity = getOwner();

        if (tickCount % 40 == 1) {
            level().playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 3.0f, 0.5f);
        }

        AABB box = new AABB(c, c).inflate(RADIUS);
        for (Entity e : level().getEntities(this, box, en -> en.isAlive() && !(en instanceof BlackHoleEntity) && !(en instanceof MeteorEntity))) {
            if (e == ownerEntity) continue;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 d = c.subtract(e.position().add(0, e.getBbHeight() / 2.0, 0));
            double dist = d.length();
            if (dist > RADIUS || dist < 0.01) continue;
            double f = 1.0 - dist / RADIUS;
            Vec3 pull = d.normalize().scale((0.04 + 0.22 * f * f) * ramp);
            Vec3 v = e.getDeltaMovement().scale(0.94).add(pull);
            if (v.length() > 1.6) v = v.normalize().scale(1.6);
            e.setDeltaMovement(v);
            e.hurtMarked = true;
            e.fallDistance = 0;
            if (e instanceof LivingEntity le && dist < 3.5 && tickCount % 8 == 0) {
                le.hurt(level().damageSources().indirectMagic(this, ownerEntity), 4.0f);
            }
        }

        if (tickCount >= max) collapse(c);
    }

    private void collapse(Vec3 c) {
        ServerLevel sl = (ServerLevel) level();
        Entity ownerEntity = getOwner();
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(RADIUS), en -> en != ownerEntity && en.isAlive())) {
            Vec3 out = e.position().add(0, e.getBbHeight() / 2.0, 0).subtract(c);
            double dist = Math.max(0.5, out.length());
            double f = Math.max(0.0, 1.0 - dist / RADIUS);
            e.hurt(sl.damageSources().indirectMagic(this, ownerEntity), (float) (6 + 20 * f));
            e.setDeltaMovement(out.normalize().scale(0.8 + 2.2 * f).add(0, 0.5, 0));
            e.hurtMarked = true;
        }
        sl.explode(this, c.x, c.y, c.z, 5.0f, Level.ExplosionInteraction.MOB);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.END_GATEWAY_SPAWN, SoundSource.HOSTILE, 4.0f, 0.6f);
        ModNetwork.sendFx(sl, FxPacket.Type.BLACKHOLE_COLLAPSE, c, c, 1.0f);
        discard();
    }

    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 400.0 * 400.0; }
    @Override public boolean ignoreExplosion() { return true; }
    @Override public boolean isPickable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setMaxLife(tag.getInt("MaxLife"));
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("MaxLife", getMaxLife());
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
