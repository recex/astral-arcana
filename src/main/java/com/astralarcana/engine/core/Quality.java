package com.astralarcana.engine.core;

/** Niveis de qualidade. AUTO nunca e um nivel efetivo: o PerformanceEngine escolhe LOW..ULTRA. */
public enum Quality {
    AUTO(1.0f, 22, 2, 2, 6, 3, 1.0f, 3000),
    LOW(0.35f, 8, 4, 1, 2, 1, 0.5f, 500),
    MEDIUM(0.65f, 14, 4, 2, 4, 2, 0.75f, 1200),
    HIGH(1.0f, 22, 2, 2, 6, 3, 1.0f, 3000),
    ULTRA(1.0f, 32, 2, 3, 8, 4, 1.0f, 6000);

    public final float particleScale;
    public final int raySteps;
    public final int bloomDiv;
    public final int blurPasses;
    public final int maxLights;
    public final int maxOrbs;
    public final float detail;
    public final int particleBudget;

    Quality(float particleScale, int raySteps, int bloomDiv, int blurPasses, int maxLights, int maxOrbs, float detail, int particleBudget) {
        this.particleScale = particleScale;
        this.raySteps = raySteps;
        this.bloomDiv = bloomDiv;
        this.blurPasses = blurPasses;
        this.maxLights = maxLights;
        this.maxOrbs = maxOrbs;
        this.detail = detail;
        this.particleBudget = particleBudget;
    }
}
