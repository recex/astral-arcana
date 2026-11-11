package com.astralarcana.engine.audio;

import com.astralarcana.engine.anim.Easing;
import com.astralarcana.engine.core.Settings;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Som posicional cujo volume e pitch seguem curvas ao longo do tempo (risers, quedas, fades). */
public final class FadingSound extends AbstractTickableSoundInstance {
    private final float v0, v1, p0, p1;
    private final int life;
    private final Easing ease;
    private int age;

    public FadingSound(SoundEvent ev, SoundSource src, Vec3 pos, float v0, float v1, float p0, float p1, int life, Easing ease) {
        super(ev, src, RandomSource.create());
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.v0 = v0;
        this.v1 = v1;
        this.p0 = p0;
        this.p1 = p1;
        this.life = Math.max(1, life);
        this.ease = ease;
        this.volume = v0 * Settings.audioVolume;
        this.pitch = p0;
        this.looping = false;
    }

    @Override
    public void tick() {
        age++;
        float t = Mth.clamp(age / (float) life, 0f, 1f);
        float k = ease.apply(t);
        this.volume = Mth.lerp(k, v0, v1) * Settings.audioVolume;
        this.pitch = Mth.clamp(Mth.lerp(k, p0, p1), 0.5f, 2.0f);
        if (age > life) stop();
    }
}
