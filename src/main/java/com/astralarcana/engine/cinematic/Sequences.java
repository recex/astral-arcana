package com.astralarcana.engine.cinematic;

import com.astralarcana.engine.anim.Easing;
import com.astralarcana.engine.audio.AudioEngine.Cues;
import com.astralarcana.engine.cinematic.Sequence.Prop;
import com.astralarcana.engine.vfx.Debris;
import com.astralarcana.engine.vfx.Fx;
import com.astralarcana.engine.vfx.GroundCrackEffect;
import com.astralarcana.engine.vfx.PortalEffect;
import com.astralarcana.engine.vfx.VFXEngine;
import com.astralarcana.engine.world.WorldVisualEngine;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/** Biblioteca de sequencias prontas. Cada uma combina camera, shader, luz, volumetria e audio numa linha do tempo. */
public final class Sequences {
    private Sequences() {}

    public static final Sequence SUPERNOVA = Sequence.named("supernova", 110)
            .at(0, c -> {
                Fx.flash(Fx.WHITE, 1.0f * c.k, 0.018f);
                Fx.ripple(c.origin, 0.09f * c.k, 50);
                Fx.warp(0.35f * c.k);
                Fx.glitch(0.4f * c.k);
                WorldVisualEngine.addLight(c.origin, Fx.WHITE, 70f, 3.0f * c.k, 60);
                WorldVisualEngine.addShock3D(c.origin, 80f, 45, 4f, Fx.CYAN, 2.2f * c.k);
                WorldVisualEngine.addOrb(c.origin, 16f, Fx.VIOLET, 1.0f, 1.2f, 0, 1.2f, 70);
                WorldVisualEngine.addFog(Fx.VIOLET, 0.012f * c.k, 90);
            })
            .at(8, c -> WorldVisualEngine.addShock3D(c.origin, 60f, 40, 3f, Fx.WHITE, 1.2f * c.k))
            .at(30, c -> { Fx.desat(0.35f * c.k); Fx.vignette(0.35f * c.k); })
            .shake(0, 3.2f, 60)
            .sound(0, Cues.SUPERNOVA, 1f)
            .key(Prop.FOV, 0, 0f, Easing.LINEAR).key(Prop.FOV, 3, 0.35f, Easing.OUT_EXPO).key(Prop.FOV, 55, 0f, Easing.IN_OUT_SINE)
            .key(Prop.ROLL, 0, 0f, Easing.LINEAR).key(Prop.ROLL, 12, 3.5f, Easing.OUT_QUAD).key(Prop.ROLL, 45, 0f, Easing.IN_OUT_SINE)
            .key(Prop.BARS, 0, 0f, Easing.LINEAR).key(Prop.BARS, 8, 1f, Easing.OUT_CUBIC).key(Prop.BARS, 70, 1f, Easing.LINEAR).key(Prop.BARS, 95, 0f, Easing.IN_CUBIC)
            .build();

    public static final Sequence COLLAPSE = Sequence.named("collapse", 80)
            .at(0, c -> {
                Fx.flash(Fx.WHITE, 0.85f * c.k, 0.03f);
                Fx.ripple(c.origin, 0.1f * c.k, 44);
                Fx.warp(0.3f * c.k);
                WorldVisualEngine.addLight(c.origin, Fx.VIOLET, 45f, 2.4f * c.k, 40);
                WorldVisualEngine.addShock3D(c.origin, 55f, 38, 3.5f, Fx.VIOLET, 2.0f * c.k);
                WorldVisualEngine.addOrb(c.origin, 12f, Fx.VIOLET, 1.0f, 1.4f, 0, 1.0f, 40);
            })
            .shake(0, 2.8f, 45)
            .sound(0, Cues.COLLAPSE, 1f)
            .key(Prop.FOV, 0, 0f, Easing.LINEAR).key(Prop.FOV, 2, 0.25f, Easing.OUT_EXPO).key(Prop.FOV, 40, 0f, Easing.IN_OUT_SINE)
            .build();

    public static final Sequence ROAR = Sequence.named("roar", 45)
            .at(0, c -> {
                Fx.ripple(c.origin, 0.035f * c.k, 26);
                Fx.warp(0.08f * c.k);
                WorldVisualEngine.addShock3D(c.origin, 30f, 24, 2.5f, Fx.CYAN, 0.8f * c.k);
            })
            .shake(0, 0.9f, 18)
            .sound(0, Cues.ROAR, 1f)
            .key(Prop.FOV, 0, 0f, Easing.LINEAR).key(Prop.FOV, 4, 0.08f, Easing.OUT_QUAD).key(Prop.FOV, 30, 0f, Easing.IN_OUT_SINE)
            .build();

    public static final Sequence TIMESTOP = Sequence.named("timestop", 70)
            .at(0, c -> {
                Fx.glitch(0.6f * c.k);
                Fx.ripple(c.origin, 0.07f * c.k, 40);
                WorldVisualEngine.addShock3D(c.origin, 40f, 36, 3f, Fx.SILVER, 1.6f * c.k);
                WorldVisualEngine.addLight(c.origin, Fx.SILVER, 40f, 1.8f * c.k, 40);
                WorldVisualEngine.addFog(Fx.SILVER, 0.01f * c.k, 60);
            })
            .shake(0, 1.2f, 20)
            .sound(0, Cues.TIMESTOP, 1f)
            .key(Prop.FOV, 0, 0f, Easing.LINEAR).key(Prop.FOV, 5, -0.12f, Easing.OUT_QUAD).key(Prop.FOV, 40, 0f, Easing.IN_OUT_SINE)
            .build();

    /** Demonstracao: percorre todos os recursos do motor (use /astral cinematic showcase). */
    public static final Sequence SHOWCASE = Sequence.named("showcase", 240)
            .at(0, c -> {
                WorldVisualEngine.addOrb(c.origin.add(0, 4, 0), 7f, Fx.ORANGE, 1.0f, 1.2f, 0, 1.0f, 70);
                WorldVisualEngine.addLight(c.origin.add(0, 4, 0), Fx.ORANGE, 30f, 2f, 70);
            })
            .at(60, c -> {
                WorldVisualEngine.addShock3D(c.origin, 45f, 40, 3f, Fx.CYAN, 2f);
                Fx.ripple(c.origin, 0.07f, 40);
                Fx.warp(0.3f);
            })
            .at(100, c -> {
                Vec3 f = c.origin.add(6, 0, 0);
                VFXEngine.add(new PortalEffect(f, new Vec3(-1, 0, 0), 3f, 80, Fx.VIOLET, Fx.CYAN));
                WorldVisualEngine.addLight(f, Fx.VIOLET, 22f, 1.6f, 80);
            })
            .at(140, c -> {
                VFXEngine.add(new GroundCrackEffect(c.level, c.origin, 14f, 70));
                Debris.burst(c.level, c.origin, 60, 0.7);
                WorldVisualEngine.addFog(Fx.ORANGE, 0.012f, 80);
                Fx.glitch(0.5f);
            })
            .at(190, c -> {
                Fx.desat(0.6f);
                Fx.vignette(0.5f);
                Fx.tint(Fx.CYAN, 0.3f);
            })
            .shake(60, 2f, 30)
            .shake(140, 2.5f, 30)
            .key(Prop.BARS, 0, 0f, Easing.LINEAR).key(Prop.BARS, 12, 1f, Easing.OUT_CUBIC).key(Prop.BARS, 215, 1f, Easing.LINEAR).key(Prop.BARS, 235, 0f, Easing.IN_CUBIC)
            .key(Prop.FOV, 0, 0f, Easing.LINEAR).key(Prop.FOV, 60, -0.15f, Easing.IN_OUT_SINE).key(Prop.FOV, 120, 0.2f, Easing.OUT_BACK).key(Prop.FOV, 200, 0f, Easing.IN_OUT_SINE)
            .key(Prop.ROLL, 0, 0f, Easing.LINEAR).key(Prop.ROLL, 80, 3f, Easing.IN_OUT_SINE).key(Prop.ROLL, 160, -3f, Easing.IN_OUT_SINE).key(Prop.ROLL, 230, 0f, Easing.IN_OUT_SINE)
            .key(Prop.FADE, 0, 0.8f, Easing.LINEAR).key(Prop.FADE, 14, 0f, Easing.OUT_CUBIC).key(Prop.FADE, 225, 0f, Easing.LINEAR).key(Prop.FADE, 240, 1f, Easing.IN_CUBIC)
            .build();

    private static final Map<String, Sequence> ALL = new LinkedHashMap<>();

    static {
        for (Sequence s : new Sequence[]{SUPERNOVA, COLLAPSE, ROAR, TIMESTOP, SHOWCASE}) ALL.put(s.name, s);
    }

    public static Sequence get(String name) {
        return ALL.get(name);
    }

    public static java.util.Set<String> names() {
        return ALL.keySet();
    }
}
