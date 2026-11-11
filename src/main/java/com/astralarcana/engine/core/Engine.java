package com.astralarcana.engine.core;

import com.astralarcana.engine.audio.AudioEngine;
import com.astralarcana.engine.cinematic.CinematicEngine;
import com.astralarcana.engine.particle.ParticleSystem;
import com.astralarcana.engine.perf.PerformanceEngine;
import com.astralarcana.engine.shader.FramebufferPool;
import com.astralarcana.engine.vfx.VFXEngine;
import com.astralarcana.engine.world.WorldVisualEngine;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Coordenador do motor grafico (somente cliente). */
public final class Engine {
    private Engine() {}

    public static final Logger LOG = LogUtils.getLogger();
    private static int tick;

    /** Chamado uma vez por tick de cliente (fim do tick). */
    public static void clientTick() {
        if (tick++ % 20 == 0) Settings.refresh();
        PerformanceEngine.tick();
        ParticleSystem.beginTick();
        VFXEngine.tick();
        WorldVisualEngine.tick();
        CinematicEngine.tick();
        AudioEngine.tick();
    }

    /** Ao sair do mundo: descarta estado e libera memoria de GPU. */
    public static void onLevelUnload() {
        VFXEngine.clear();
        WorldVisualEngine.clear();
        CinematicEngine.clear();
        AudioEngine.clear();
        FramebufferPool.clear();
    }
}
