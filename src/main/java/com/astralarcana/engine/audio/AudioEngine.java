package com.astralarcana.engine.audio;

import com.astralarcana.engine.anim.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Audio cinematografico: um Cue e um conjunto de camadas sonoras (cada uma com atraso, volume e glissando de pitch)
 * tocadas em posicao 3D. Atrasos extras simulam a velocidade do som com a distancia. Usa os sons do jogo,
 * combinados e alterados em pitch (o mod nao embute arquivos de audio).
 */
public final class AudioEngine {
    private AudioEngine() {}

    public static final class Layer {
        final SoundEvent sound;
        final float vol, pitch, pitchEnd;
        final int delay, life;

        Layer(SoundEvent sound, float vol, float pitch, float pitchEnd, int delay, int life) {
            this.sound = sound;
            this.vol = vol;
            this.pitch = pitch;
            this.pitchEnd = pitchEnd;
            this.delay = delay;
            this.life = life;
        }
    }

    public static final class Cue {
        final String name;
        final List<Layer> layers;

        private Cue(String name, List<Layer> layers) {
            this.name = name;
            this.layers = layers;
        }

        public static Builder named(String name) {
            return new Builder(name);
        }

        public static final class Builder {
            private final String name;
            private final List<Layer> layers = new ArrayList<>();

            private Builder(String name) {
                this.name = name;
            }

            public Builder layer(SoundEvent s, float vol, float pitch, int delay) {
                layers.add(new Layer(s, vol, pitch, pitch, delay, 1));
                return this;
            }

            /** Camada com glissando de pitch ao longo de 'life' ticks (riser/queda). */
            public Builder glide(SoundEvent s, float vol, float pitch, float pitchEnd, int delay, int life) {
                layers.add(new Layer(s, vol, pitch, pitchEnd, delay, life));
                return this;
            }

            public Cue build() {
                return new Cue(name, layers);
            }
        }
    }

    private static final class Pending {
        final Layer layer;
        final Vec3 pos;
        final float scale;
        int left;

        Pending(Layer layer, Vec3 pos, float scale, int left) {
            this.layer = layer;
            this.pos = pos;
            this.scale = scale;
            this.left = left;
        }
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    public static void play(Cue cue, Vec3 pos, float scale) {
        Minecraft mc = Minecraft.getInstance();
        if (cue == null || mc.level == null || mc.player == null) return;
        int travel = (int) Math.min(20.0, mc.player.position().distanceTo(pos) / 40.0);
        for (Layer l : cue.layers) {
            int d = l.delay + travel;
            if (d <= 0) start(l, pos, scale);
            else PENDING.add(new Pending(l, pos, scale, d));
        }
    }

    private static void start(Layer l, Vec3 pos, float scale) {
        Minecraft.getInstance().getSoundManager().play(
                new FadingSound(l.sound, SoundSource.PLAYERS, pos, l.vol * scale, l.vol * scale, l.pitch, l.pitchEnd, l.life, Easing.LINEAR));
    }

    public static void tick() {
        for (Pending p : PENDING) {
            if (--p.left <= 0) start(p.layer, p.pos, p.scale);
        }
        PENDING.removeIf(p -> p.left <= 0);
    }

    public static void clear() {
        PENDING.clear();
    }

    public static int pending() {
        return PENDING.size();
    }

    /** Biblioteca de cues usada pelas sequencias. */
    public static final class Cues {
        private Cues() {}

        public static final Cue SUPERNOVA = Cue.named("supernova")
                .layer(SoundEvents.GENERIC_EXPLODE, 5f, 0.5f, 0)
                .layer(SoundEvents.WARDEN_SONIC_BOOM, 3f, 0.55f, 2)
                .layer(SoundEvents.LIGHTNING_BOLT_THUNDER, 6f, 0.7f, 4)
                .glide(SoundEvents.BEACON_DEACTIVATE, 3f, 0.8f, 0.5f, 6, 60)
                .layer(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.2f, 24)
                .layer(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.6f, 30)
                .build();

        public static final Cue COLLAPSE = Cue.named("collapse")
                .glide(SoundEvents.END_PORTAL_SPAWN, 4f, 0.5f, 0.5f, 0, 40)
                .layer(SoundEvents.GENERIC_EXPLODE, 5f, 0.6f, 3)
                .layer(SoundEvents.WARDEN_SONIC_BOOM, 2.5f, 0.5f, 5)
                .build();

        public static final Cue ROAR = Cue.named("roar")
                .glide(SoundEvents.ENDER_DRAGON_GROWL, 5f, 0.65f, 0.5f, 0, 40)
                .layer(SoundEvents.WARDEN_ROAR, 3f, 0.7f, 4)
                .build();

        public static final Cue RISER = Cue.named("riser")
                .glide(SoundEvents.BEACON_POWER_SELECT, 2f, 0.5f, 1.6f, 0, 30)
                .glide(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.7f, 1.8f, 4, 24)
                .build();

        public static final Cue TIMESTOP = Cue.named("timestop")
                .glide(SoundEvents.BEACON_DEACTIVATE, 3f, 0.6f, 0.5f, 0, 40)
                .layer(SoundEvents.END_PORTAL_SPAWN, 1.5f, 1.5f, 6)
                .layer(SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.6f, 10)
                .build();

        public static final Cue IMPACT = Cue.named("impact")
                .layer(SoundEvents.GENERIC_EXPLODE, 4f, 0.7f, 0)
                .layer(SoundEvents.TRIDENT_THUNDER, 2f, 0.6f, 3)
                .build();
    }
}
