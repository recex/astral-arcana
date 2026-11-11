package com.astralarcana.engine.core;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracao do cliente (config/astralarcana-client.toml). */
public final class EngineConfig {
    private EngineConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<Quality> QUALITY;
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE;
    public static final ForgeConfigSpec.IntValue TARGET_FPS;
    public static final ForgeConfigSpec.BooleanValue POST_FX;
    public static final ForgeConfigSpec.BooleanValue BLOOM;
    public static final ForgeConfigSpec.DoubleValue BLOOM_INTENSITY;
    public static final ForgeConfigSpec.BooleanValue VOLUMETRICS;
    public static final ForgeConfigSpec.BooleanValue DYNAMIC_LIGHTS;
    public static final ForgeConfigSpec.BooleanValue MOTION_BLUR;
    public static final ForgeConfigSpec.DoubleValue SHAKE_SCALE;
    public static final ForgeConfigSpec.DoubleValue AUDIO_VOLUME;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("graphics");
        QUALITY = b.comment("Qualidade grafica. AUTO deixa o motor escolher conforme o FPS.")
                .defineEnum("quality", Quality.AUTO);
        ADAPTIVE = b.comment("Reduz/aumenta a qualidade automaticamente para manter o FPS alvo.")
                .define("adaptiveQuality", true);
        TARGET_FPS = b.comment("FPS alvo da qualidade adaptativa.")
                .defineInRange("targetFps", 60, 20, 240);
        POST_FX = b.comment("Pos-processamento (distorcao, lentes, glitch, volumetricos).")
                .define("postProcessing", true);
        BLOOM = b.comment("Bloom (brilho) durante os efeitos.")
                .define("bloom", true);
        BLOOM_INTENSITY = b.defineInRange("bloomIntensity", 0.8, 0.0, 3.0);
        VOLUMETRICS = b.comment("Efeitos baseados em profundidade: luzes dinamicas, neblina, orbes volumetricos, ondas 3D.")
                .define("volumetrics", true);
        DYNAMIC_LIGHTS = b.define("dynamicLights", true);
        MOTION_BLUR = b.comment("Motion blur por reprojecao de profundidade (apenas durante efeitos).")
                .define("motionBlur", true);
        b.pop();
        b.push("camera");
        SHAKE_SCALE = b.comment("Multiplicador do tremor de camera (0 desliga).")
                .defineInRange("shakeScale", 1.0, 0.0, 2.0);
        b.pop();
        b.push("audio");
        AUDIO_VOLUME = b.comment("Volume dos sons cinematograficos do mod.")
                .defineInRange("audioVolume", 1.0, 0.0, 2.0);
        b.pop();
        SPEC = b.build();
    }
}
