package com.astralarcana.engine.anim;

/** Funcoes de easing (entrada t em [0,1], saida normalmente em [0,1]). */
public enum Easing {
    LINEAR, IN_QUAD, OUT_QUAD, IN_OUT_QUAD,
    IN_CUBIC, OUT_CUBIC, IN_OUT_CUBIC,
    IN_EXPO, OUT_EXPO, IN_OUT_EXPO,
    IN_SINE, OUT_SINE, IN_OUT_SINE,
    IN_BACK, OUT_BACK,
    OUT_ELASTIC, OUT_BOUNCE, SMOOTHSTEP;

    public float apply(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        final float c1 = 1.70158f, c3 = c1 + 1f, c4 = (float) (2.0 * Math.PI / 3.0);
        switch (this) {
            case IN_QUAD: return t * t;
            case OUT_QUAD: return 1f - (1f - t) * (1f - t);
            case IN_OUT_QUAD: return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
            case IN_CUBIC: return t * t * t;
            case OUT_CUBIC: return 1f - (float) Math.pow(1f - t, 3);
            case IN_OUT_CUBIC: return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
            case IN_EXPO: return (float) Math.pow(2.0, 10.0 * t - 10.0);
            case OUT_EXPO: return 1f - (float) Math.pow(2.0, -10.0 * t);
            case IN_OUT_EXPO:
                return t < 0.5f ? (float) Math.pow(2.0, 20.0 * t - 10.0) / 2f
                        : (2f - (float) Math.pow(2.0, -20.0 * t + 10.0)) / 2f;
            case IN_SINE: return 1f - (float) Math.cos(t * Math.PI / 2.0);
            case OUT_SINE: return (float) Math.sin(t * Math.PI / 2.0);
            case IN_OUT_SINE: return -((float) Math.cos(Math.PI * t) - 1f) / 2f;
            case IN_BACK: return c3 * t * t * t - c1 * t * t;
            case OUT_BACK: return 1f + c3 * (float) Math.pow(t - 1f, 3) + c1 * (float) Math.pow(t - 1f, 2);
            case OUT_ELASTIC: return (float) Math.pow(2.0, -10.0 * t) * (float) Math.sin((t * 10.0 - 0.75) * c4) + 1f;
            case OUT_BOUNCE: return bounce(t);
            case SMOOTHSTEP: return t * t * (3f - 2f * t);
            default: return t;
        }
    }

    private static float bounce(float t) {
        final float n1 = 7.5625f, d1 = 2.75f;
        if (t < 1f / d1) return n1 * t * t;
        if (t < 2f / d1) { t -= 1.5f / d1; return n1 * t * t + 0.75f; }
        if (t < 2.5f / d1) { t -= 2.25f / d1; return n1 * t * t + 0.9375f; }
        t -= 2.625f / d1;
        return n1 * t * t + 0.984375f;
    }
}
