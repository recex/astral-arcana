package com.astralarcana.engine.core;

/** Copia em cache dos valores de configuracao (ler ConfigValue por frame seria caro e falha antes do load). */
public final class Settings {
    private Settings() {}

    public static volatile Quality quality = Quality.AUTO;
    public static volatile boolean adaptive = true;
    public static volatile int targetFps = 60;
    public static volatile boolean postFx = true;
    public static volatile boolean bloom = true;
    public static volatile float bloomIntensity = 0.8f;
    public static volatile boolean volumetrics = true;
    public static volatile boolean dynamicLights = true;
    public static volatile boolean motionBlur = true;
    public static volatile float shakeScale = 1f;
    public static volatile float audioVolume = 1f;

    public static void refresh() {
        if (!EngineConfig.SPEC.isLoaded()) return;
        quality = EngineConfig.QUALITY.get();
        adaptive = EngineConfig.ADAPTIVE.get();
        targetFps = EngineConfig.TARGET_FPS.get();
        postFx = EngineConfig.POST_FX.get();
        bloom = EngineConfig.BLOOM.get();
        bloomIntensity = EngineConfig.BLOOM_INTENSITY.get().floatValue();
        volumetrics = EngineConfig.VOLUMETRICS.get();
        dynamicLights = EngineConfig.DYNAMIC_LIGHTS.get();
        motionBlur = EngineConfig.MOTION_BLUR.get();
        shakeScale = EngineConfig.SHAKE_SCALE.get().floatValue();
        audioVolume = EngineConfig.AUDIO_VOLUME.get().floatValue();
    }
}
