package com.astralarcana.entity;

import com.astralarcana.client.ClientFx;
import com.astralarcana.network.FxPacket;
import com.astralarcana.network.ModNetwork;
import com.astralarcana.spell.Spells;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Serpente cosmica voadora (chefe). O corpo e uma corrente de segmentos que seguem o rastro da cabeca;
 * o cliente anima ondulacao, espinhos, aneis e estrelas proceduralmente.
 */
public class CosmicSerpentEntity extends Mob {
    public static final int SEG_COUNT = 36;
    public static final double SPACING = 1.5;
    public static final int ORBIT = 0, DIVE = 1, BREATH = 2, STORM = 3;

    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(CosmicSerpentEntity.class, EntityDataSerializers.INT);

    public final Vec3[] segs = new Vec3[SEG_COUNT];
    public final Vec3[] segsO = new Vec3[SEG_COUNT];
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private boolean initialized;

    /** tickCount em que a fase atual comecou (valido no cliente e no servidor). */
    public int phaseStart;
    private int phaseTicks;
    private int phaseDuration = 100;
    private float orbitAngle;
    private int orbitDir = 1;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.astralarcana.cosmic_serpent"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public CosmicSerpentEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        Arrays.fill(segs, Vec3.ZERO);
        Arrays.fill(segsO, Vec3.ZERO);
        this.noPhysics = true;
        this.noCulling = true;
        this.xpReward = 500;
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.FOLLOW_RANGE, 100.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** Raio do corpo no segmento i (cabeca grossa, cauda fina). */
    public static float radius(int i) {
        float t = i / (float) (SEG_COUNT - 1);
        return 0.1f + 0.9f * (float) Math.pow(1.0 - t, 0.8);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PHASE, ORBIT);
    }

    public int getPhase() { return this.entityData.get(PHASE); }

    public Vec3 center() { return position().add(0, getBbHeight() / 2.0, 0); }

    public Vec3 getForward() {
        Vec3 f = segs[0].subtract(segs[1]);
        return f.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : f.normalize();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PHASE.equals(key)) {
            phaseStart = tickCount;
            if (level().isClientSide && getPhase() != ORBIT) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.serpentRoar(this));
            }
        }
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        for (int i = 0; i < SEG_COUNT; i++) segsO[i] = segs[i];
        super.tick();
        updateSegments();
        if (!initialized) {
            for (int i = 0; i < SEG_COUNT; i++) segsO[i] = segs[i];
            initialized = true;
        }
        if (level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.serpentTick(this));
        }
    }

    private void updateSegments() {
        Vec3 head = center();
        if (trail.isEmpty()) {
            Vec3 back = Vec3.directionFromRotation(0, getYRot()).scale(-1);
            for (int i = 0; i < 160; i++) trail.addLast(head.add(back.scale(i * 0.5)));
        }
        Vec3 last = trail.peekFirst();
        if (last == null || last.distanceToSqr(head) > 0.04) trail.addFirst(head);

        segs[0] = head;
        double walked = 0, nextAt = SPACING;
        Vec3 prev = head;
        int idx = 1, used = 0;
        for (Vec3 p : trail) {
            used++;
            double d = prev.distanceTo(p);
            while (idx < SEG_COUNT && walked + d >= nextAt) {
                double t = d <= 1.0E-6 ? 0 : (nextAt - walked) / d;
                segs[idx++] = prev.lerp(p, t);
                nextAt += SPACING;
            }
            walked += d;
            prev = p;
            if (idx >= SEG_COUNT) break;
        }
        while (idx < SEG_COUNT) segs[idx++] = prev;
        while (trail.size() > used + 2) trail.removeLast();
    }

    // ------------------------------------------------------------------ IA (servidor)
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        ServerPlayer target = findTarget();
        Vec3 pos = center();
        if (target == null) {
            setDeltaMovement(getDeltaMovement().scale(0.95).add(0, Math.sin(tickCount * 0.05) * 0.01, 0));
            faceMotion();
            return;
        }
        Vec3 tpos = target.position().add(0, target.getBbHeight() / 2.0, 0);
        phaseTicks++;

        switch (getPhase()) {
            case ORBIT -> {
                orbitAngle += 0.025f * orbitDir;
                Vec3 desired = tpos.add(Math.cos(orbitAngle) * 22, 11 + Math.sin(tickCount * 0.04) * 4, Math.sin(orbitAngle) * 22);
                steer(desired, 1.0, 0.07);
                if (phaseTicks > phaseDuration) nextAttack();
            }
            case DIVE -> {
                steer(tpos, 1.8, 0.1);
                if (phaseTicks > phaseDuration || pos.distanceTo(tpos) < 3.5) toOrbit(pos, tpos);
            }
            case BREATH -> {
                steer(pos.add(tpos.subtract(pos).normalize().scale(2)), 0.25, 0.15);
                if (phaseTicks == 35) {
                    Vec3 mouth = pos.add(getForward().scale(2.6));
                    Vec3 dir = tpos.subtract(mouth).normalize();
                    Spells.fireBeam((ServerLevel) level(), this, mouth, dir, mouth, 110, 18f, 3.5f, 0.8f);
                }
                if (phaseTicks > phaseDuration) toOrbit(pos, tpos);
            }
            default -> { // STORM
                steer(tpos.add(0, 16, 0), 0.6, 0.08);
                if (phaseTicks % 5 == 0 && phaseTicks < phaseDuration - 5) {
                    Vec3 land = target.position().add((random.nextDouble() - 0.5) * 16, 0, (random.nextDouble() - 0.5) * 16);
                    Vec3 dir = new Vec3((random.nextDouble() - 0.5) * 0.6, -1.7, (random.nextDouble() - 0.5) * 0.6);
                    Spells.dropMeteor((ServerLevel) level(), land, dir, 40 + random.nextDouble() * 30, 1.2f + random.nextFloat() * 0.8f, this);
                }
                if (phaseTicks > phaseDuration) toOrbit(pos, tpos);
            }
        }
        faceMotion();
        touchDamage();
    }

    private void steer(Vec3 desired, double maxSpeed, double accel) {
        Vec3 want = desired.subtract(center());
        double d = want.length();
        Vec3 dv = d < 1.0E-3 ? Vec3.ZERO : want.scale(1.0 / d * Math.min(maxSpeed, d * 0.2 + 0.15));
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.add(dv.subtract(v).scale(accel)));
    }

    private void faceMotion() {
        Vec3 v = getDeltaMovement();
        if (v.horizontalDistanceSqr() > 1.0E-4) {
            float ty = (float) (Mth.atan2(-v.x, v.z) * Mth.RAD_TO_DEG);
            float tp = (float) (-(Mth.atan2(v.y, v.horizontalDistance()) * Mth.RAD_TO_DEG));
            setYRot(Mth.approachDegrees(getYRot(), ty, 15f));
            setXRot(Mth.approachDegrees(getXRot(), tp, 10f));
            this.yBodyRot = getYRot();
            this.yHeadRot = getYRot();
        }
    }

    private void setPhase(int p, int duration) {
        this.entityData.set(PHASE, p);
        this.phaseStart = tickCount;
        this.phaseTicks = 0;
        this.phaseDuration = duration;
        if (p != ORBIT) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 8.0f, 0.6f + random.nextFloat() * 0.2f);
        }
    }

    private void toOrbit(Vec3 pos, Vec3 tpos) {
        orbitAngle = (float) Mth.atan2(pos.z - tpos.z, pos.x - tpos.x);
        orbitDir = random.nextBoolean() ? 1 : -1;
        float hp = getHealth() / getMaxHealth();
        setPhase(ORBIT, (int) ((70 + random.nextInt(60)) * (0.6 + 0.4 * hp)));
    }

    private void nextAttack() {
        float hp = getHealth() / getMaxHealth();
        int roll = random.nextInt(100);
        if (roll < 35) setPhase(DIVE, 70);
        else if (roll < (hp < 0.5f ? 70 : 65)) setPhase(BREATH, 75);
        else setPhase(STORM, 55);
    }

    @Nullable
    private ServerPlayer findTarget() {
        ServerPlayer best = null;
        double bd = 100.0 * 100.0;
        for (ServerPlayer p : ((ServerLevel) level()).players()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            double d = p.distanceToSqr(this);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    /** Dano de contato: qualquer ser vivo encostando em algum segmento do corpo. */
    private void touchDamage() {
        if (tickCount % 4 != 0) return;
        double minX = 1e9, minY = 1e9, minZ = 1e9, maxX = -1e9, maxY = -1e9, maxZ = -1e9;
        for (Vec3 s : segs) {
            minX = Math.min(minX, s.x); maxX = Math.max(maxX, s.x);
            minY = Math.min(minY, s.y); maxY = Math.max(maxY, s.y);
            minZ = Math.min(minZ, s.z); maxZ = Math.max(maxZ, s.z);
        }
        AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(2.5);
        float dmg = getPhase() == DIVE ? 16f : 9f;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, en -> en != this && en.isAlive() && !(en instanceof CosmicSerpentEntity))) {
            Vec3 c = e.position().add(0, e.getBbHeight() / 2.0, 0);
            for (int i = 0; i < SEG_COUNT; i++) {
                double rr = radius(i) + e.getBbWidth() / 2.0 + 0.4;
                if (segs[i].distanceToSqr(c) < rr * rr) {
                    e.hurt(damageSources().mobAttack(this), dmg);
                    Vec3 push = c.subtract(segs[i]);
                    push = push.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : push.normalize();
                    e.setDeltaMovement(push.scale(1.1).add(0, 0.35, 0));
                    e.hurtMarked = true;
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ resto
    @Override
    public void travel(Vec3 input) {
        if (isDeadOrDying()) setDeltaMovement(getDeltaMovement().scale(0.9));
        this.move(MoverType.SELF, getDeltaMovement());
        this.fallDistance = 0;
    }

    @Override
    protected void tickDeath() {
        if (deathTime == 19 && !level().isClientSide) {
            Vec3 c = segs[0];
            ModNetwork.sendFx((ServerLevel) level(), FxPacket.Type.SUPERNOVA, c, c, 1.0f);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 8.0f, 0.5f);
        }
        super.tickDeath();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource src) {
        return super.isInvulnerableTo(src)
                || src.getDirectEntity() instanceof MeteorEntity
                || src.is(DamageTypes.IN_WALL) || src.is(DamageTypes.DROWN) || src.is(DamageTypes.FALL);
    }

    @Override public boolean causeFallDamage(float dist, float mult, DamageSource src) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeLeashed(Player p) { return false; }
    @Override public boolean canChangeDimensions() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 400.0 * 400.0; }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bossEvent.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bossEvent.removePlayer(p);
    }
}
