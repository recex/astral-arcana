package com.astralarcana.engine.cinematic;

import com.astralarcana.engine.anim.Easing;
import com.astralarcana.engine.anim.Track;
import com.astralarcana.engine.audio.AudioEngine;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Sequencia cinematografica declarativa: eventos temporizados (em ticks) + curvas de camera por keyframe.
 * Exemplo: Sequence.named("x", 60).shake(0, 2f, 30).key(Prop.FOV, 0, 0, LINEAR).key(Prop.FOV, 5, .3f, OUT_EXPO).build()
 */
public final class Sequence {
    public enum Prop { YAW, PITCH, ROLL, FOV, BARS, FADE }

    public interface Action {
        void run(Ctx c);
    }

    /** Contexto entregue a cada evento: origem no mundo e intensidade (0..1, ex.: proximidade). */
    public static final class Ctx {
        public final ClientLevel level;
        public final Vec3 origin;
        public final float k;

        Ctx(ClientLevel level, Vec3 origin, float k) {
            this.level = level;
            this.origin = origin;
            this.k = k;
        }
    }

    static final class Timed {
        final int tick;
        final Action action;

        Timed(int tick, Action action) {
            this.tick = tick;
            this.action = action;
        }
    }

    public final String name;
    public final int duration;
    final List<Timed> events;
    final Map<Prop, Track> tracks;
    final float[] fadeColor;

    private Sequence(String name, int duration, List<Timed> events, Map<Prop, Track> tracks, float[] fadeColor) {
        this.name = name;
        this.duration = duration;
        this.events = events;
        this.tracks = tracks;
        this.fadeColor = fadeColor;
    }

    public static Builder named(String name, int duration) {
        return new Builder(name, duration);
    }

    public static final class Builder {
        private final String name;
        private final int duration;
        private final List<Timed> events = new ArrayList<>();
        private final Map<Prop, Track> tracks = new EnumMap<>(Prop.class);
        private float[] fadeColor = {0f, 0f, 0f};

        private Builder(String name, int duration) {
            this.name = name;
            this.duration = duration;
        }

        public Builder at(int tick, Action a) {
            events.add(new Timed(tick, a));
            return this;
        }

        public Builder shake(int tick, float amp, int dur) {
            return at(tick, c -> CinematicEngine.shake(amp * c.k, dur));
        }

        public Builder sound(int tick, AudioEngine.Cue cue, float volume) {
            return at(tick, c -> AudioEngine.play(cue, c.origin, volume));
        }

        public Builder key(Prop p, float tick, float value, Easing e) {
            tracks.computeIfAbsent(p, k -> new Track()).key(tick, value, e);
            return this;
        }

        public Builder fadeColor(float r, float g, float b) {
            this.fadeColor = new float[]{r, g, b};
            return this;
        }

        public Sequence build() {
            events.sort(Comparator.comparingInt(t -> t.tick));
            return new Sequence(name, duration, events, tracks, fadeColor);
        }
    }
}
