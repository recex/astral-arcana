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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MeteorEntity extends Entity {
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.FLOAT);
    private UUID owner;

    public MeteorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SIZE, 1.5F);
    }

    public float getSize() { return this.entityData.get(SIZE); }
    public void setSize(float s) { this.entityData.set(SIZE, s); }
    public void setOwner(@Nullable Entity e) { this.owner = e == null ? null : e.getUUID(); }

    @Nullable
    public Entity getOwner() {
        if (owner != null && level() instanceof ServerLevel sl) return sl.getEntity(owner);
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 vel = getDeltaMovement();
        Vec3 from = position();
        Vec3 to = from.add(vel);
        if (!level().isClientSide) {
            Vec3 probe = to.add(vel.normalize().scale(getSize()));
            HitResult hit = level().clip(new ClipContext(from, probe, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) {
                impact(hit.getLocation());
                return;
            }
            if (tickCount > 600 || getY() < level().getMinBuildHeight() - 10) {
                discard();
                return;
            }
        } else {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.meteorTrail(this));
        }
        setPos(to);
    }

    private void impact(Vec3 pos) {
        ServerLevel sl = (ServerLevel) level();
        float power = 2.2f + getSize() * 1.3f;
        sl.explode(this, pos.x, pos.y, pos.z, power, Level.ExplosionInteraction.MOB);
        ModNetwork.sendFx(sl, FxPacket.Type.METEOR_IMPACT, pos, pos, getSize());
        discard();
    }

    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 400.0 * 400.0; }
    @Override public boolean ignoreExplosion() { return true; }
    @Override public boolean isPickable() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setSize(tag.getFloat("Size"));
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Size", getSize());
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
